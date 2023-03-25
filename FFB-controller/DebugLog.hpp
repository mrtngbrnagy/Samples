/*
 * DebugLog.hpp
 *
 *  Created on: 2022. márc. 11.
 *      Author: Marci
 */

#ifndef INC_DEBUGLOG_HPP_
#define INC_DEBUGLOG_HPP_

// Switch on or off runtime log printing blocks for various modules
#define DEBUG_LOG_HIDFFB 0
#define DEBUG_LOG_USBCALLBACKS 0

#include "ISerial.hpp"
#include <cstdint>
#include <vector>

const double QV = std::pow(2.0,16);

// struct for holding the sampler log
typedef struct RTSample
{
    uint32_t deltaTime = 0;
    int data[4] = {0, 0, 0, 0};
} RTSample_t;

class DebugLog
{
    public:
        static void assignInterface(ISerial *interface);
        static void print(std::string text);
        static void printChar(std::string text, int32_t value);
        template<typename T> static void printInt(std::string text, T value); // e.g.: DebugLog::printInt<uint16_t>("some text", some_integer);
        static void transmit(uint8_t* buffer, int size);

        static void dump();
        static void init(size_t sampleVectorLength);
        static void reset();

        static void stepSample();
        static RTSample_t* getCurrentSample();

        static uint32_t getSamplingRate();
        static void setSamplingRate(uint32_t samplingRate);
        static bool isSubsample();
        static void setSubsample(bool isSubsample);

        static void setEnabled(bool isEnabled);

        static void setData(uint8_t idx, int value);
        static void setDeltaTime(uint32_t deltaTime);

    private:
        static size_t currentSampleIdx_;
        static ISerial *serial_;
        static std::vector<RTSample_t> sampleVector_;
        static uint32_t samplingRate_; // default sampling rate of 1ms
        static bool subSample_;
        static bool enabled_;
};

template<typename T> void DebugLog::printInt(std::string text, T value)
{
    serial_->transmit(text+std::to_string(value)+"\r\n");
}

#endif /* INC_DEBUGLOG_HPP_ */
