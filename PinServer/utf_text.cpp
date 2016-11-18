/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include <arpa/inet.h>
#include <memory.h>

#include "utf_text.hpp"

short int UTF_text :: getLength() {
    return readShortInt(content);
}
const char* UTF_text :: getText() {
    return (const char*)&content[0 + sizeof(short int)];
}
void UTF_text :: setText(const char* text, const int length) {   
    writeShortInt(length);
    memcpy(&content[0 + sizeof(short int)], text, length + 1);
}
char* UTF_text :: getContent() {
    return (char*)content;
}
short int UTF_text :: readShortInt(const byte* bytes) {
    int size = sizeof(short int);
    byte value[size];
    memcpy(value, bytes, size);
    return ntohs(*(short int*)value);
}
void UTF_text :: writeShortInt(const short int value) {
    short int nvalue = htons(value);
    memcpy(content, &nvalue, sizeof(short int));
}