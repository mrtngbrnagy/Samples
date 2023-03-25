import pandas as pd

import WatersIMGReader.WatersIMGReader as wir
import traceback

import numpy as np
import matplotlib.pyplot as plt

import time
import glob

from sklearn.decomposition import PCA
from sklearn.cluster import KMeans, DBSCAN
from scipy.spatial.distance import euclidean


def binspace(m_min, m_max, bin_size):

    m_range = (m_max - m_min)
    num_bins = int(m_range // bin_size)

    if m_range % bin_size > 0:
        m_max = m_min + num_bins * bin_size

    return np.round(np.linspace(m_min, m_max, num_bins+1), 2)


def batch_reader(image_path, scan_range, bins, lock_mass=None, save_path=None, scan_limit=1000):

    scan_batch_all = binspace(scan_range[0], scan_range[1], scan_limit)

    if scan_batch_all[-1] < scan_range[1]:
        scan_batch_all = np.append(scan_batch_all, scan_range[1])

    for batch_idx in range(0, scan_batch_all.shape[0]-1):

        load_start, load_end = int(scan_batch_all[batch_idx]), int(scan_batch_all[batch_idx+1])

        try:
            print(f"Loading data for batch {batch_idx}, with scan range {load_start}-{load_end}")
            reader = wir.WatersIMGReader(image_path, 1)

            scans = reader.get_total_scans()
            print("Data contains: " + str(scans) + " scans")

            mass_range = reader.get_mass_range()
            print("Mass range: " + str(mass_range[0]) + " to " + str(mass_range[1]))

            reader.load_data(load_start, load_end)

            print("Reading xy spectra")
            spec_df = reader.get_xy_peaks(load_start, load_end, bins=bins, lock_mass=lock_mass)

            if save_path is not None:
                print("Exporting spectra")
                spec_df.reset_index(inplace=True)
                spec_df.to_feather(save_path+f"_batch_{load_start}_{load_end}")

            del reader
        except NameError as err:
            print(err)
            traceback.print_exc()


def combine_batch_files(path, regex):

    all_files = glob.glob(path+regex)

    df_list = []

    for filename in all_files:
        df = pd.read_feather(filename)
        df_list.append(df)

    return pd.concat(df_list, axis=0, ignore_index=True)


def plot_xy_image(df, key, name, figsize=(20, 20), path=''):
    fig = plt.figure(figsize=figsize)
    plt.scatter(df['X_pos'], df['Y_pos'], c=df[key], marker='s')
    plt.grid()
    plt.axis('equal')
    plt.savefig(f'{path}xy_image_{key}_{name}.png')


def plot_spectrum(df, name, path):
    plt.figure()
    plt.plot(df.index.astype('float'), df.values)
    plt.grid()
    plt.savefig(f'{path}plot_spectrum_{name}.png')


def plot_bars(df, name, path):
    plt.figure()
    plt.grid()
    plt.bar(x=df.index.astype('float'), height=df.values)
    plt.savefig(f'{path}plot_bars_{name}.png')


def normalize_spectra(df):
    df_out = df.divide(df.sum(axis=1), axis=0)
    df_out = df_out.fillna(0)

    return df_out


def remove_low_tic(df, q, path):

    df_sum = df.sum(axis=1)
    df_out = df.copy()
    df_out.loc[df_sum < df_sum.quantile(q=q), :] = np.zeros(df.shape[1])

    plt.figure()
    plt.hist(df_sum.loc[df_sum < df_sum.quantile(q=q)], bins=100)
    plt.savefig(f'{path}plot_sum_{q}.png')

    return df_out


def quart_normalize_spectra(df):
    shared_weight = df.sum(axis=1).quantile(q=0.75)
    df = df.divide(shared_weight)

    return df


if __name__ == "__main__":
    image_path = ''

    project_path = ''

    data_path = project_path + 'Data/'

    results_path = project_path + 'Results/'

    batch_export_path = data_path + 'batch_export/'

    batch_export_name = "20211125_143350_Bella"

    reference_line_path = data_path + 'reference_lines/'

    reference_line_file = reference_line_path + "Bella RPMI 600-1000.data.csv"

    mode = 'cluster'
    bin_ID = '699.6'
    reference_spectrum_type = 'external'
    reference_spectrum_ID = 50682
    line_pass = 'P21'

    mask_top_peak_count = 200

    LM = {'lock_mass': 554.2615,
        'tolerance': 0.5,
        'threshold': 10}

    fsize = (40, 40)

    if mode == 'export':
        # 43 sec / 1000 scan -> ~14 mins for 19k scans
        # 22 sec / 1000 scan @ 100 scan / batch
        # 18 sec / 1000 scan @ 500 scan / batch
        # 17 sec / 1000 scan @ 1000 scan / batch
        # 17 MB / 1000 scan
        scan_range = [2500, 79500]  # [100, 18100] [2500, 79500]
        bins = binspace(600, 1000, 0.1)

        t0 = time.time()

        batch_reader(image_path, scan_range, bins, lock_mass=LM, save_path=batch_export_path+batch_export_name, scan_limit=1000)

        t1 = time.time()
        print(t1-t0)

    elif mode == 'cluster':
        t0 = time.time()
        print('Importing data...')
        combined_df = combine_batch_files(batch_export_path, batch_export_name+"*")
        print('Done.')
        combined_df.set_index('index', inplace=True)

        data_df = combined_df.drop(['X_pos', 'Y_pos'], axis=1)

        print('Reading reference spectrum')

        if reference_spectrum_type == 'external':
            reference_line = pd.read_csv(reference_line_file, delimiter=',')
            reference_line_single = reference_line.loc[reference_line['Class'].str.contains(line_pass), :]
            line_single_sum = reference_line_single['Sum.']
            reference_line_single = reference_line_single.drop(columns=['Class', 'File', 'Start scan', 'End scan', 'Sum.'])
            reference_line_single = reference_line_single.multiply(line_single_sum, axis=0)
            reference_spectrum = reference_line_single.mean(axis=0)
            # shift column name convention from center naming to lower edge naming
            reference_spectrum.index = [str(num) for num in np.round(reference_spectrum.index.astype('float')-0.05, 1)]
            data_df = data_df.append(reference_spectrum, ignore_index=True)
            combined_df = combined_df.append(reference_spectrum, ignore_index=True)
            reference_spectrum_ID = data_df.index[-1]
        else:
            reference_spectrum = data_df.loc[reference_spectrum_ID, :]

        print('Masking')
        plot_spectrum(reference_spectrum, 'reference', results_path)

        data_df_sum = data_df.sum(axis=1)
        img_counter_mask = data_df.loc[data_df_sum < data_df_sum.quantile(q=0.5), :].mean(axis=0).sort_values(ascending=False).index[:200]

        ref_spec_mask = reference_spectrum.sort_values(ascending=False).index[:mask_top_peak_count]
        # remove the largest peaks in the image from the reference mask
        ref_spec_mask = ref_spec_mask.difference(img_counter_mask)

        print(ref_spec_mask.shape)

        plot_bars(reference_spectrum.loc[ref_spec_mask].sort_index(), 'masked_reference', results_path)

        masked_data_df = data_df.loc[:, ref_spec_mask]

        masked_data_df = remove_low_tic(masked_data_df, 0.5, results_path)
        masked_data_df = normalize_spectra(masked_data_df)

        print('Calculating PCA')
        pca = PCA(n_components=100)
        data_pca = pca.fit_transform(masked_data_df)

        print('Clustering')
        k_means = KMeans(n_clusters=10)
        labels = k_means.fit_predict(data_pca)

        reference_label = labels[reference_spectrum_ID]
        reference_label_mask = labels == reference_label

        print(f'Reference spectrum cluster no.: {reference_label}')

        print('Plotting')
        for label in np.unique(labels):
            label_mask = labels == label
            xy_masked = combined_df.loc[label_mask, ['X_pos', 'Y_pos']]
            xy_masked['PC0'] = data_pca[label_mask, 0]
            plot_xy_image(xy_masked, 'PC0', f'cluster_{label}', path=results_path, figsize=fsize)
            plot_bars(masked_data_df.loc[label_mask, :].mean(axis=0), f'mean_cluster_{label}', results_path)

        xy_cluster_labels = combined_df.loc[:, ['X_pos', 'Y_pos']]
        xy_cluster_labels['labels'] = labels
        plot_xy_image(xy_cluster_labels, 'labels', 'unweighted', figsize=fsize, path=results_path)
        xy_cluster_labels.loc[reference_label_mask, 'labels'] = xy_cluster_labels.loc[reference_label_mask, 'labels'] + 20
        plot_xy_image(xy_cluster_labels, 'labels', 'weighted', figsize=fsize, path=results_path)

        print('Done')
        t1 = time.time()
        print(t1 - t0)

    elif mode == 'distance':
        t0 = time.time()
        print('Importing data...')
        combined_df = combine_batch_files(batch_export_path, batch_export_name + "*")
        print('Done.')
        combined_df.set_index('index', inplace=True)

        data_df = combined_df.drop(['X_pos', 'Y_pos'], axis=1)

        print('Reading reference spectrum')

        if reference_spectrum_type == 'external':
            reference_line = pd.read_csv(reference_line_file, delimiter=',')
            reference_line_single = reference_line.loc[reference_line['Class'].str.contains(line_pass), :]
            reference_line_single = reference_line_single.drop(columns=['Class', 'File', 'Start scan', 'End scan', 'Sum.'])
            reference_spectrum = reference_line_single.mean(axis=0)
            # shift column name convention from center naming to lower edge naming
            reference_spectrum.index = [str(num) for num in np.round(reference_spectrum.index.astype('float') - 0.05, 1)]
        else:
            reference_spectrum = data_df.loc[reference_spectrum_ID, :]

        plot_spectrum(reference_spectrum, 'reference', results_path)

        ref_spec_mask = reference_spectrum.sort_values(ascending=False).index[:mask_top_peak_count]

        masked_reference = reference_spectrum.loc[ref_spec_mask].sort_index()
        plot_bars(masked_reference, 'masked_reference', results_path)

        masked_data_df = data_df.loc[:, ref_spec_mask]

        masked_data_df = normalize_spectra(masked_data_df)

        # calculate distance
        print('Calculating distance')
        spectrum_distance = np.zeros(masked_data_df.shape[0])

        for spectrum_idx in range(0, masked_data_df.shape[0]):
            spectrum_distance[spectrum_idx] = euclidean(masked_data_df.iloc[spectrum_idx, :], masked_reference)

        spectrum_distance_df = combined_df[['X_pos', 'Y_pos']]
        spectrum_distance_df['similarity'] = 1/spectrum_distance

        print('Plotting')
        plot_xy_image(spectrum_distance_df, 'similarity', 'reference', figsize=fsize, path=results_path)

        print('Done')
        t1 = time.time()
        print(t1 - t0)

    elif mode == "bin_plot":

        combined_df = combine_batch_files(batch_export_path, batch_export_name + "*")

        # make RGB image from a single bin
        plot_xy_image(combined_df, bin_ID, 'single_bin')
