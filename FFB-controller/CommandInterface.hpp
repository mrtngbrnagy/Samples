/*
 * CommandInterface.hpp
 *
 *  Created on: 2022. márc. 11.
 *      Author: Marci
 */

#pragma once

#define DATAGRAM_SIZE 6

#include "ISerial.hpp"
#include <cstdint>

#include "CascadeFFController.hpp"
#include "Drive.hpp"
#include "TMC4671.h"
#include "Controls.hpp"

enum class Mode {idle = 0, FFB = 1, target = 2, counter = 3};

enum class CommandType {setTarget = 1, setParam = 2, setConstant = 3, setLogging = 4, setCalibrating = 5, disableCalibrating = 6, setCenter = 7, setForceCenter = 8};

enum class TargetType {flux = 0, torque = 1, velocity = 2, position = 3};

enum class PIDParam {TP = 1, TI = 2, VP = 3, VI = 4, PP = 5, FFGain = 6, FFEnable = 7, FFDisable = 8, reset = 0};

enum class ConstantEffect { spring = 1, damper = 2, limiter = 3, reset = 0};

enum class LoggingCommands { stopLogging = 0, startLogging = 1, dumpLog = 2, setSubsample = 3, unsetSubsample = 4};

class CommandInterface
{
    public:
        static void assignInterface(ISerial *interface);
        static void print(std::string text);
        static void printChar(std::string text, int32_t value);
        static uint32_t receive(uint8_t* msg, uint32_t size);
        static uint32_t receive();
        static void update();
        template<typename T> static void printInt(std::string text, T value);

    private:
        static ISerial *serial_;
        static CascadeFFController* cascade_;
        static Drive* drive_;
        static TMC4671* motor_;
        static Controls* controls_;
        static uint8_t datagram_[DATAGRAM_SIZE];
        static Mode currentMode_;
};

template<typename T> void CommandInterface::printInt(std::string text, T value)
{
    serial_->transmit(text+std::to_string(value)+"\r\n");
}