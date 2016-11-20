/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include "protocol.hpp"

void reverseBytes(byte* arr, int size) {
    byte tmp;
    int half_size = size / 2;
    for(int i = 0; i < half_size; i++) {
        tmp = arr[i];
        arr[i] = arr[size - i - 1];
        arr[size - i - 1] = tmp;
    }
}