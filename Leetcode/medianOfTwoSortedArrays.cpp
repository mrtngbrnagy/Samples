class Solution {
public:
    double findMedianSortedArrays(vector<int>& nums1, vector<int>& nums2) {
        /*
        // Easy solution is copying the second array into the first, sorting, then selecting
        // the center element. Complexity is O(m) + O(N log(N)) which is worse than the required.
        // Pro: Works with unordered input arrays
        // Con: Doesn't take advantage of the inputs being sorted
        // For some reason this method also passes, even though the complexity requirement
        // is not fulfilled.
        nums1.insert(nums1.end(), nums2.begin(), nums2.end());
        std::sort(nums1.begin(), nums1.end());

        // Calculate modulo of the array size to determine if array is even or odd
        size_t mod2 = nums1.size() % 2;
        // If mod != 0, then this is the center element of the array, if mod == 0
        // then we get the high bound of the median.
        size_t idx = nums1.size() / 2;

        // If length is even, we need to average the two center elements.
        if (mod2 == 0)
        {            
            return (nums1[idx - 1] + nums1[idx]) / 2.0;
        }

        // If length is odd, just return the center element.
        return nums1[idx];
        */

        // Use the sorting of the two arrays by binary searching in them
        // Binary search upper and lower bounds in each array
        // The index difference of the bounds is always 1
        // The exit criteria is the overlapping or equality of the bound values
        // If bound A is below bound B, we step bound A up and bound B down
        // If bound B is below bound A, we step bound B up and bound A down
        // Stepping in either direction is done at the half of the current interval
        // A: --------------- (15) B: --------------------- (21) median index is (15 + 21) / 2 = 13
        // A starts in the center (7):
        // ------[--]-------
        // The median index is always assumed to be 13, so B starts in (13-7=6):
        // -----[--]--------------
        // If the lower bound of A is smaller than the upper bound of B
        // we step to the next half up: 7->15 with center of 11:
        // ----------[--]---
        // shifting with the median index of 13 becomes 2:
        // -[--]------------------
        // If the if is false, then we select the other half
        // Continue splitting each interval to half until the bounds overlap
        // E.g.
        // if the lower bound of A is smaller than the uppper bound of B
        // AND
        // the lower bound of B is smaller than the upper bound of A
        // We take the minimum of the upper bounds and maximum of the lower bounds
        // to calculate the median
        // Complexity is the same as in binary search O(log(N))
        size_t sizeA = nums1.size();
        size_t sizeB = nums2.size();

        if (sizeA > sizeB) return findMedianSortedArrays(nums2, nums1);

        if (sizeA == 0)
        {
            return sizeB % 2 == 0 ? (nums2[sizeB/2-1] + nums2[sizeB/2])/2.0 : nums2[sizeB/2];
        }

        // Start index of the interval
        size_t intBegin = 0;
        // End index of the interval
        size_t intEnd = sizeA;
        // Index of median considering all number in the two arrays
        size_t medianIndex = (sizeA + sizeB + 1) / 2;
        size_t intervalCenter, medianShiftedIntCent;

        int upperA, upperB, lowerA, lowerB;

        while(intBegin <= intEnd)
        {
            // Center of the selected interval in array A
            intervalCenter = (intBegin + intEnd) / 2;
            // Center of the selected interval in array B
            medianShiftedIntCent = medianIndex - intervalCenter;

            // Lower bound of interval in A
            lowerA = intervalCenter > 0 ? nums1.at(intervalCenter - 1) : std::numeric_limits<int>::min();
            // Lower bound of interval in B
            lowerB = medianShiftedIntCent > 0 ? nums2.at(medianShiftedIntCent - 1) : std::numeric_limits<int>::min();

            // Upper bound of interval in A
            upperA = intervalCenter < sizeA ? nums1.at(intervalCenter) : std::numeric_limits<int>::max();
            // Upper bound of interval in B
            upperB = medianShiftedIntCent < sizeB ? nums2.at(medianShiftedIntCent) : std::numeric_limits<int>::max();

            /*
            std::cout << "intervalCenter: " << intervalCenter << std::endl;
            std::cout << "intBegin: " << intBegin << std::endl;
            std::cout << "intEnd: " << intEnd << std::endl;
            std::cout << "lowerA: " << lowerA << std::endl;
            std::cout << "lowerB: " << lowerB << std::endl;
            std::cout << "upperA: " << upperA << std::endl;
            std::cout << "upperB: " << upperB << std::endl;
            */

            // If the bound values of each interval overlap (e.g. [2,5],[4,6]) or 'touch' (e.g. [2,5],[5,6])
            if ( lowerA <= upperB && lowerB <= upperA )
            {
                // If length is even, we need to average the two center elements.
                if ((sizeA + sizeB) % 2 == 0)
                {            
                    return (std::max(lowerA, lowerB) + std::min(upperA, upperB)) / 2.0;
                }

                return std::max(lowerA, lowerB);
            }
            // lowerB > upperA, A interval needs to be shifted up, B down
            else if (lowerA < upperB)
            {
                intBegin = intervalCenter + 1;
            }
            // lowerA > upperB, A interval needs to be shifted down, B up
            else
            {
                intEnd = intervalCenter - 1;
            }
        }

        return 0.0;            
    }
};