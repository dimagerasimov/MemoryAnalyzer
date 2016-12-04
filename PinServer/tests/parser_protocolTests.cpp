/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   parser_protocolTests.cpp
 * Author: master
 *
 * Created on December 2, 2016, 12:57 AM
 */

#include <stdlib.h>
#include <stdio.h>
#include <iostream>
#include <string.h>
#include <sys/time.h>

#include "parser_protocol.hpp"

/*
 * Simple C++ Test Suite
 */

struct timeval start_time;

void initTestTime() {
    gettimeofday(&start_time, NULL);
}

double getTestTimePassed() {
    struct timeval current_time;
    gettimeofday(&current_time, NULL);
    int sec = (current_time.tv_sec - start_time.tv_sec);
    int ms = (current_time.tv_usec - start_time.tv_usec) / 1000;
    return  sec + ms / 1000.0;
}

void testParseUTFtext_command() {
    UTF_text utf_text;
    char str_utf_text[MAX_MESSAGE_LENGTH];
    char* true_command = (char*)"command";
    char* true_text_command = (char*)"text_command";
    sprintf(str_utf_text, "%s%c%s", true_command, COM_DELIMITER, true_text_command);
    
    utf_text.setText(str_utf_text, strlen(str_utf_text));
    
    char real_command[MAX_COMMAND_LENGTH];
    char real_text_command[MAX_MESSAGE_LENGTH];
    parseUTFtext_command(&utf_text, real_command, real_text_command);
    if (strcmp(true_command, real_command) != 0
            || strcmp(true_text_command, real_text_command) != 0) {
        std::cout << "%TEST_FAILED% time=" << getTestTimePassed()
            << " testname=testParseUTFtext_command (parser_protocolTests) message=error message sample" << std::endl;
    }
}

void testParse_get_argument() {
    char* test_word = (char*)"arg";
    char tmp_str[10];
    char text_command[MAX_MESSAGE_LENGTH];
    char text_arg[MAX_MESSAGE_LENGTH];
    char delimiter = ARGS_DELIMITER;
    
    const int NUM_4TEST_ARGS = 10;
    text_command[0] = '\0';
    for(int i = 0; i < NUM_4TEST_ARGS - 1; i++) {
        sprintf(tmp_str, "%s%d%c", test_word, i, delimiter);
        strcat(text_command, tmp_str);
    }
    sprintf(tmp_str, "%s%d", test_word, NUM_4TEST_ARGS - 1);
    strcat(text_command, tmp_str);
    
    for(int i = 0; i < NUM_4TEST_ARGS; i++) {
        bool isSuccess = parse_get_argument(text_command, text_arg, i, delimiter);
        sprintf(tmp_str, "%s%d", test_word, i);
        if(strcmp(tmp_str, text_arg) != 0 || !isSuccess) {
            std::cout << "%TEST_FAILED% time=" << getTestTimePassed()
                << " testname=testParse_get_argument (parser_protocolTests) message=error message sample" << std::endl;
        }
    }
}

int main(int argc, char** argv) {
    std::cout << "%SUITE_STARTING% parser_protocolTests" << std::endl;
    std::cout << "%SUITE_STARTED%" << std::endl;

    initTestTime();
        
    std::cout << "%TEST_STARTED% testParseUTFtext_command (parser_protocolTests)" << std::endl;
    testParseUTFtext_command();
    std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
            << " testParseUTFtext_command (parser_protocolTests)" << std::endl;

    std::cout << "%TEST_STARTED% testParse_get_argument (parser_protocolTests)" << std::endl;
    testParse_get_argument();
    std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
            << " testParse_get_argument (parser_protocolTests)" << std::endl;

    std::cout << "%SUITE_FINISHED% time=" << getTestTimePassed() << std::endl;

    return (EXIT_SUCCESS);
}

