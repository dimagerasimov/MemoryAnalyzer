/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include "map_clients.hpp"

MapClients :: MapClients() {
}
bool MapClients :: addClient(int new_client_fds) {
    if(map_clients.size() >= MAX_NUMBER_CLIENTS) {
        return false;
    }
    pair<map<int, int>::iterator, bool> ret;
    ret = map_clients.insert(pair<int,int>(new_client_fds, new_client_fds));
    return true;
}
MapClients :: ~MapClients() {
    map_clients.clear();
}