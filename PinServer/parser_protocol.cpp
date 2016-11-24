/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include <string.h>

#include "parser_protocol.hpp"

void parseUTFtext_command(UTF_text* utf_text,
        char* command, char* text_command) {
    const char* full_text = utf_text->getText();
    const char* delimiter = strchr(full_text, COM_DELIMITER);
    if(delimiter == NULL) {
        strcpy(command, full_text);
        return;
    }
    int size = delimiter - full_text;
    memcpy(command, full_text, size);
    command[size] = '\0';
    strcpy(text_command, delimiter + 1);
}
bool parse_get_argument(const char* text_command,
        char* text_arg, const int num_arg, char delimiter) {
    const char* prev_delimiter;
    const char* cur_delimiter = text_command;
    for(int i = 0; i <= num_arg; i++) {
        prev_delimiter = cur_delimiter;
        cur_delimiter = strchr(cur_delimiter + 1, delimiter);
        if((cur_delimiter == NULL) && (num_arg != i)) {
            return false;
        }
    }
    // If argument is last
    if((cur_delimiter == NULL) && (num_arg != 0)) {
        strcpy(text_arg, prev_delimiter + 1);
        return true;
    }
    // If argument in the middle
    if(num_arg != 0) {
        prev_delimiter += 1;
    }
    if(cur_delimiter == NULL) {
        strcpy(text_arg, prev_delimiter);
        return true;
    }
    int size = cur_delimiter - prev_delimiter;
    memcpy(text_arg, prev_delimiter, size);
    text_arg[size] = '\0';
    return true;
}