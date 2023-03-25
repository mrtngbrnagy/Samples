/*
 * Serial.hpp
 *
 *  Created on: 2022. márc. 11.
 *      Author: Marci
 */

#ifndef INC_SERIAL_HPP_
#define INC_SERIAL_HPP_

#include <string>

class ISerial
{
    public:
        virtual ~ISerial(){}
        virtual void transmit(std::string text) = 0;
        virtual void transmit(uint8_t* buffer, int size) = 0;
        virtual uint32_t receive(uint8_t* buf, int size) = 0;
        virtual int getBufSize() = 0;

    public:
        static const uint32_t bufferSize;
};

#endif /* INC_SERIAL_HPP_ */
