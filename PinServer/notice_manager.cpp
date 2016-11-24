/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include <stdlib.h>
#include <iostream>

#include "notice_manager.hpp"

using namespace std;

extern pthread_mutex_t mutex;

void print_notice(int type_notice, void* arg) {
    pthread_mutex_lock(&mutex);
    switch(type_notice) {
        case BIND_NOTICE:
            cout << "Bind on " << *((short int*)arg) << " port successfully." << endl;
            break;
        case NOTICE:
            cout << (char*)arg << endl;
            break;
        case ERR:
            cerr << (char*)arg << endl;
            exit(-1);
            break;
        case INI_SUC_CLIENT_NOTICE:
            cout << "The client with key \""
                << *((int*)arg) << "\" was added successfully." << endl;
            break;
        case INI_FAIL_CLIENT_NOTICE:
            cout << "The client with key \""
                << *((int*)arg) << "\" was ignored." << endl;
            break;
        case FINI_CLIENT_NOTICE:
            cout << "The client with key \""
                << *((int*)arg) << "\" was finished successfully." << endl;
            break;
        case CLIENT_HUNG_UP:
            cout << "The client with key \""
                << *((int*)arg) << "\" hung up." << endl;
            break;
        case CONNECTION_RESET:
            cout << "The client with key \"" << *((int*)arg) <<
                "\" has exceeded the limit of incorrect queries. Connection reset." << endl;
            break;
    }
    pthread_mutex_unlock(&mutex);
}