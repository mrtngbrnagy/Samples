/*
 * CountersteerEmulator.hpp
 *
 *  Created on: Jul 03, 2022
 *      Author: Marci
 */

 #pragma once

 #include <cstdint>
 #include <memory>

 #include "CascadeFFController.hpp"

class CountersteerEmulator
{
    public:
    CountersteerEmulator(CascadeFFController* cascade);
    CountersteerEmulator();

    void setParams(float forceSensitivity, float decaySensitivity_, float decayCurve, int decayThreshold);

    void update(int forceValue);

    int getValue();

    private:
    uint32_t prevTick_;
    int decayThreshold_;
    float decayCurve_;
    float forceSensitivity_ = 1.0;
    float decaySensitivity_ = 1.0;
    float timeScaler_ = 1000.0;
    int outValue_ = 0;   
    CascadeFFController* cascade_;  
};