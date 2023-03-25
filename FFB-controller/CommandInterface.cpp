/*
 * CommandInterface.cpp
 *
 *  Created on: 2022. July. 31.
 *      Author: Marci
 */

 #include <cmath>

#include "CommandInterface.hpp"
#include "DebugLog.hpp"

ISerial *CommandInterface::serial_;
Mode CommandInterface::currentMode_;
CascadeFFController* CommandInterface::cascade_;
Drive* CommandInterface::drive_;
TMC4671* CommandInterface::motor_;
Controls* CommandInterface::controls_;
uint8_t CommandInterface::datagram_[DATAGRAM_SIZE];

void CommandInterface::assignInterface(ISerial *interface)
{
    serial_ = interface;
}

void CommandInterface::print(std::string text)
{
    serial_->transmit(text);
}

uint32_t CommandInterface::receive(uint8_t* msg, uint32_t size)
{
    uint32_t length = serial_->receive(msg, size);    
    return length;
}

// TODO: Add replies
void CommandInterface::update()
{
    uint32_t recSize = receive(datagram_, DATAGRAM_SIZE);

    if (recSize != 0 && datagram_[0] != 0)
    {
        uint8_t command = datagram_[0];
        int value = *((int32_t*)&(datagram_[1]));

        // Send reply
        serial_->transmit(&command, 1);

        // Set mode
        if (command == 1)
        {
            currentMode_ = (Mode)value;
        }
        else
        {
            CommandType command_type = (CommandType)(command / 10);
            uint8_t command_subtype = command % 10;
            
            switch (command_type)
            {
                case CommandType::setTarget:
                    switch ((TargetType)command_subtype)
                    {
                        case TargetType::flux:
                            cascade_->setPIDMode(CascadePIDMode::torque);
                            break;
                        case TargetType::torque:
                            cascade_->setPIDMode(CascadePIDMode::torque);
                            break;
                        case TargetType::velocity:
                            cascade_->setPIDMode(CascadePIDMode::velocity);
                            cascade_->setVelocityTarget(value/QV);
                            break;
                        case TargetType::position:
                            cascade_->setPIDMode(CascadePIDMode::position);
                            cascade_->setPositionTarget(value/QV);
                            break;
                    }
                    break;
                case CommandType::setParam:
                    switch((PIDParam)command_subtype)
                    {
                        case PIDParam::TP:
                            // Q is fixed here, determined by the motor controller
                            //motor_->writeRegister16BitValue(TMC4671_PID_POSITION_P_POSITION_I, 1, uint16_t(value));
                            break;
                        case PIDParam::TI:
                            // Q is fixed here, determined by the motor controller
                            //motor_->writeRegister16BitValue(TMC4671_PID_POSITION_P_POSITION_I, 0, uint16_t(value));
                            break;
                        case PIDParam::VP:
                            // Decide Q value for the gains, e.g. Q22.10
                            cascade_->setVelocityP((double)value/std::pow(2.0,16));
                            break;
                        case PIDParam::VI:
                            // Decide Q value for the gains, e.g. Q22.10
                            cascade_->setVelocityI((double)value/std::pow(2.0,16));
                            break;
                        case PIDParam::PP:
                            // Decide Q value for the gains, e.g. Q22.10
                            cascade_->setPositionP((double)value/std::pow(2.0,16));
                            break;
                        case PIDParam::FFGain:
                            cascade_->setFFGain((double)value/std::pow(2.0,16));
                            break;
                        case PIDParam::FFEnable:
                            cascade_->setFFEnable(true);
                            break;
                        case PIDParam::FFDisable:
                            cascade_->setFFEnable(false);
                            break;
                        case PIDParam::reset:
                            break;
                    }
                    break;

                case CommandType::setConstant:
                    break;

                case CommandType::setLogging:
                    switch ((LoggingCommands)command_subtype)
                    {
                        case LoggingCommands::dumpLog:
                            serial_->transmit(&command, 1);
                            DebugLog::dump();
                            break;
                        case LoggingCommands::startLogging:
                            serial_->transmit(&command, 1);
                            DebugLog::reset();
                            DebugLog::setEnabled(true);                           
                            break;
                        case LoggingCommands::stopLogging:
                            serial_->transmit(&command, 1);
                            DebugLog::setEnabled(false);
                            break;
                        case LoggingCommands::setSubsample:
                            serial_->transmit(&command, 1);
                            DebugLog::setSubsample(true);
                            DebugLog::setSamplingRate(value);                            
                            break;
                        case LoggingCommands::unsetSubsample:
                            serial_->transmit(&command, 1);
                            DebugLog::setSubsample(false);                         
                            break;
                    }
                    break;

                case CommandType::setCalibrating:
                    drive_->disable();
                    controls_->resetCalibration();
                    controls_->setCalibrating(true);
                    //DebugLog::print("Controls calibration is active.\r\n"); 
                    break;

                case CommandType::disableCalibrating:
                    controls_->setCalibrating(false);
                    //DebugLog::print("Controls calibration is disabled.\r\n");
                    break;

                case CommandType::setCenter:
                    drive_->disable();
                    motor_->setEncoderValue(0);
                    break;

                case CommandType::setForceCenter:
                    drive_->disable();
                    controls_->getForceSteer()->setCenter();
                    break;
            }
        }

        // Flush the command
        datagram_[0] = 0;
    }
}

uint32_t CommandInterface::receive()
{
    uint8_t buffer[ISerial::bufferSize];
    uint32_t length = serial_->receive(buffer, ISerial::bufferSize);
    return length == 0 ? 0 : ((uint32_t*)buffer)[0];
}

void CommandInterface::printChar(std::string text, int32_t value)
{
    char send_str[serial_->getBufSize()];
    uint8_t value_char[4];

    *((uint32_t *)(value_char)) = value;
    sprintf(send_str, (text + "%c%c%c%c\r\n").c_str(), value_char[3],value_char[2],value_char[1],value_char[0]);
    serial_->transmit(send_str);
}