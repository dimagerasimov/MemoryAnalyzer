/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   utf_text.hpp
 * Author: master
 *
 * Created on November 15, 2016, 2:13 PM
 */

#ifndef UTF_TEXT_HPP
#define UTF_TEXT_HPP

typedef unsigned char byte;

#define UTF_TEXT_LENGTH 1<<16

class UTF_text {
    public:
        UTF_text() { }
        ~UTF_text() { }

        short int getLength();
        const char* getText();
        void setText(const char* text, const int length);
        char* getContent();
        
    private:
        short int readShortInt(const byte* bytes);
        void writeShortInt(const short int value);
        
        byte content[UTF_TEXT_LENGTH];
};

#endif /* UTF_TEXT_HPP */

