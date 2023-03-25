/*
 * CountersteerEmulator.cpp
 *
 *  Created on: Jul 03, 2022
 *      Author: Marci
 */

 #include <cmath>
 #include <algorithm>

 #include "stm32f4xx_hal.h"

 #include "CountersteerEmulator.hpp"

 CountersteerEmulator::CountersteerEmulator(CascadeFFController* cascade) : cascade_(cascade)
 {
     prevTick_ = HAL_GetTick();     
 }

 CountersteerEmulator::CountersteerEmulator(){}

 void CountersteerEmulator::setParams(float forceSensitivity, float decaySensitivity, float decayCurve, int decayThreshold)
 {
     forceSensitivity_ = forceSensitivity;
     decayCurve_ = decayCurve;
     decayThreshold_ = decayThreshold;
     decaySensitivity_ = decaySensitivity;
 }

 void CountersteerEmulator::update(int forceValue)
 {
    if (cascade_ != nullptr)
    {
        uint32_t currentTick = HAL_GetTick();

        // sigmoid for decay: large weight at low angles, low weight in large lean
        float decay = - 1.0 + 1.0 / (1.0 + std::exp(-decayCurve_ * (float)(std::abs(outValue_) - decayThreshold_)));

        float timeScale = (currentTick - prevTick_) / timeScaler_; 

        outValue_ += forceSensitivity_ * forceValue * timeScale;

        outValue_ += decay * outValue_ * timeScale * decaySensitivity_;

        outValue_ = std::clamp<int>(outValue_, -0x7fff, 0x7fff);

        // 500 is the max angle of the motor
        // This changed significantly, rework
        //cascade_->setPositionTarget((outValue_ * 500) / 0x7fff);

        prevTick_ = currentTick;
    }
 }

 int CountersteerEmulator::getValue()
 {
     return outValue_;
 }