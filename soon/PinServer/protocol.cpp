/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include "protocol.hpp"

bool isBigEndian() {
    int value = 0;
    ((char*)&value)[0] = 1;
    return value != 1;
}
bool isLittleEndian() {
    int value = 0;
    ((char*)&value)[0] = 1;
    return value == 1;
}
void hton(byte* arr, int size) {
    if(isBigEndian()) {
        return;
    }
    byte tmp;
    int half_size = size / 2;
    for(int i = 0; i < half_size; i++) {
        tmp = arr[i];
        arr[i] = arr[size - i - 1];
        arr[size - i - 1] = tmp;
    }
}
void ntoh(byte* arr, int size) {
    if(isBigEndian()) {
        return;
    }
    byte tmp;
    int half_size = size / 2;
    for(int i = 0; i < half_size; i++) {
        tmp = arr[i];
        arr[i] = arr[size - i - 1];
        arr[size - i - 1] = tmp;
    }
}