/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   protocol.hpp
 * Author: master
 *
 * Created on November 15, 2016, 1:46 PM
 */

#ifndef PROTOCOL_HPP
#define PROTOCOL_HPP

typedef unsigned char byte;

#define CONNECTION_TIMEOUT 15

#define MAX_COMMAND_LENGTH 16
#define MAX_MESSAGE_LENGTH 256

// Symbol delimiters of protocol
#define COM_DELIMITER '`'
#define ARGS_DELIMITER '~'
    
// Answers of protocol
#define SUCCESS "suc"
#define ERROR "err"
    
// Commands of protocol
#define HI "hi"
#define BYE "bye"
#define PIN_INIT "p_in"
#define PIN_EXEC "p_ex"
#define GET_BINARY "gbin"

bool isBigEndian();
bool isLittleEndian();
void hton(byte* arr, int size);
void ntoh(byte* arr, int size);

#endif /* PROTOCOL_HPP */

