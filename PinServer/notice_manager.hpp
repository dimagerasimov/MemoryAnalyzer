/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   notice_manager.hpp
 * Author: master
 *
 * Created on November 18, 2016, 5:01 PM
 */

#ifndef NOTICE_MANAGER_HPP
#define NOTICE_MANAGER_HPP

#define BIND_NOTICE 0
#define NOTICE 1
#define ERR 2
#define INI_SUC_CLIENT_NOTICE 3
#define INI_FAIL_CLIENT_NOTICE 4
#define FINI_CLIENT_NOTICE 5
#define CLIENT_HUNG_UP 6
#define CONNECTION_RESET 7

void print_notice(int type_notice, void* arg);

#endif /* NOTICE_MANAGER_HPP */

