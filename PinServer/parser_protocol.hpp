/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   protocol_parser.hpp
 * Author: master
 *
 * Created on November 15, 2016, 3:01 PM
 */

#ifndef PROTOCOL_PARSER_HPP
#define PROTOCOL_PARSER_HPP

#include "protocol.hpp"
#include "utf_text.hpp"

void parseUTFtext_command(UTF_text* utf_text, char* command, char* text_command);
bool parse_get_argument(const char* text_command,
        char* text_arg, const int num_arg, char delimiter);

#endif /* PROTOCOL_PARSER_HPP */

