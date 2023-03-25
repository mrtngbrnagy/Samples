/*
 * DebugLog.cpp
 *
 *  Created on: 2022. márc. 11.
 *      Author: Marci
 */

#include "DebugLog.hpp"
#include "tusb.h"

ISerial *DebugLog::serial_;
std::vector<RTSample_t> DebugLog::sampleVector_;
size_t DebugLog::currentSampleIdx_;
uint32_t DebugLog::samplingRate_;
bool DebugLog::subSample_;
bool DebugLog::enabled_;

void DebugLog::assignInterface(ISerial *interface)
{
    serial_ = interface;
}

void DebugLog::print(std::string text)
{
    serial_->transmit(text);
}

void DebugLog::transmit(uint8_t* buffer, int size)
{
    serial_->transmit(buffer, size);
}

void DebugLog::init(size_t sampleVectorLength)
{
    sampleVector_ = std::vector<RTSample_t>(sampleVectorLength);
    currentSampleIdx_ = 0;
    samplingRate_ = 1;
    subSample_ = true;
    enabled_ = false;
}

void DebugLog::reset()
{
    currentSampleIdx_ = 0;
}

void DebugLog::stepSample()
{
    currentSampleIdx_++;
}

RTSample_t* DebugLog::getCurrentSample()
{
    if (currentSampleIdx_ < sampleVector_.size())
    {
        return &(sampleVector_[currentSampleIdx_]);
    }
    // If we reached the end of the vector, use the last element as a "current value"
    // If vector size is 1, then we just read the current values by dumping the vector every time (for non continous monitoring)
    else
    {
        return &(sampleVector_.back());
    }    
}

void DebugLog::dump()
{
    int sampleCount = 0;

    int msg_start = 2147483647; // int max to signal beginning of a sample

    for (const RTSample_t& sample : sampleVector_)
    {
        serial_->transmit((uint8_t*)&msg_start, 4);
        serial_->transmit((uint8_t*)&sampleCount, 4);
        serial_->transmit((uint8_t*)&(sample.data[0]), 4);
        serial_->transmit((uint8_t*)&(sample.data[1]), 4);
        serial_->transmit((uint8_t*)&(sample.data[2]), 4);
        serial_->transmit((uint8_t*)&(sample.data[3]), 4);
        serial_->transmit((uint8_t*)&(sample.deltaTime), 4);
        sampleCount++;

        //Run tud_task to complete the transfer
        tud_task();
    }

    sampleCount = -1;
    serial_->transmit((uint8_t*)&msg_start, 4);
    serial_->transmit((uint8_t*)&sampleCount, 4);
}

void DebugLog::printChar(std::string text, int32_t value)
{
    char send_str[serial_->getBufSize()];
    uint8_t value_char[4];

    *((uint32_t *)(value_char)) = value;
    sprintf(send_str, (text + "%c%c%c%c\r\n").c_str(), value_char[3],value_char[2],value_char[1],value_char[0]);
    serial_->transmit(send_str);
}

uint32_t DebugLog::getSamplingRate()
{
    return samplingRate_;
}

bool DebugLog::isSubsample()
{
    return subSample_;
}

void DebugLog::setSubsample(bool isSubsample)
{
    subSample_ = isSubsample;
}

void DebugLog::setEnabled(bool isEnabled)
{
    enabled_ = isEnabled;
}

void DebugLog::setSamplingRate(uint32_t samplingRate)
{
    samplingRate_ = samplingRate;
}

void DebugLog::setData(uint8_t idx, int value)
{
    if (enabled_ && idx < 4)
    {
        if (currentSampleIdx_ < sampleVector_.size())
        {
            sampleVector_[currentSampleIdx_].data[idx] = value;
        }
        // If we reached the end of the vector, use the last element as a "current value"
        // If vector size is 1, then we just read the current values by dumping the vector every time (for non continous monitoring)
        else
        {
            sampleVector_.back().data[idx] = value;
        }
    }
}

void DebugLog::setDeltaTime(uint32_t deltaTime)
{
    if (enabled_)
    {
        if (currentSampleIdx_ < sampleVector_.size())
        {
            sampleVector_[currentSampleIdx_].deltaTime = deltaTime;
        }
        // If we reached the end of the vector, use the last element as a "current value"
        // If vector size is 1, then we just read the current values by dumping the vector every time (for non continous monitoring)
        else
        {
            sampleVector_.back().deltaTime = deltaTime;
        }   
    }
}
