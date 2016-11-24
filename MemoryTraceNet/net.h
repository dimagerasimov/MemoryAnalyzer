#ifndef __NET
#define __NET

#include <unistd.h>
#include <memory.h>
#include <arpa/inet.h>

#include "binf_element.h"

#define MIN_PORT 1<<10
#define MAX_PORT 1<<16

#define BEGIN_CONNECTION_SYMBOL 254
#define END_CONNECTION_SYMBOL 255
#define FREQUENCY_SENDING_MS 500

#define BUFFER_SIZE 1<<15

class Net {
	public:
		bool SendBinf(BinfElement& element, long long int current_time);

		Net(int ipv4, int port);
		~Net( void );

		static void ReverseBytes(byte* arr, int size);
	private:
		int sock_fd;
		int offset;
		long long int last_time_in_ms;
		byte buffer[BUFFER_SIZE];
};

Net :: Net(int ipv4, int port)
{
	struct sockaddr_in server_addr;
	sock_fd = -1;
	if(port < MIN_PORT || port >= MAX_PORT) {
		return;
	}
	sock_fd = socket(AF_INET, SOCK_STREAM, 0);
	if(sock_fd == -1) {
		return;
	}
	memset(&server_addr, 0, sizeof(server_addr));
	server_addr.sin_port = htons(port);
	server_addr.sin_family = AF_INET;
	server_addr.sin_addr.s_addr = ipv4;

	// Try to connect for a remote computer
	if(connect(sock_fd, (const struct sockaddr*)&server_addr,
			sizeof(server_addr)) != 0) {
		close(sock_fd);
		sock_fd = -1;
		return;
	}

	// Offset in buffer
	offset = 0;
	// Set zero value as start time
	last_time_in_ms = 0;

	// Send signal - begin of connection (non-blocking)
	byte begin_connection_symbol = BEGIN_CONNECTION_SYMBOL;
	int res = send(sock_fd, &begin_connection_symbol, sizeof(byte), MSG_DONTWAIT);
	if(res <= 0) {
		exit(1);
	}
}
Net :: ~Net( void )
{
	if(sock_fd != -1) {
		int res;
		if(offset > 0) {
			// Then send a data to remote computer (blocking)
			res = send(sock_fd, buffer, offset * sizeof(byte), 0);
			if(res < (int)(offset * sizeof(byte))) {
				exit(1);
			}
		}
		byte end_connection_symbol = END_CONNECTION_SYMBOL;
		// Blocking connection
		res = send(sock_fd, &end_connection_symbol, sizeof(byte), 0);
		if(res <= 0) {
			exit(1);
		}
		close(sock_fd);
	}
}
bool Net :: SendBinf(BinfElement& element, long long int current_time)
{
	if(sock_fd == -1) {
		return false;
	}

	if(((offset > 0) && (current_time - last_time_in_ms >= FREQUENCY_SENDING_MS))
		|| (offset + element.Size() >= BUFFER_SIZE)) {
		// Then send a data to remote computer (non-blocking)
		int res = send(sock_fd, buffer, offset * sizeof(byte), MSG_DONTWAIT);
		if(res <= 0) {
			exit(1);
		}
		offset = 0;
		// Remember last time
		last_time_in_ms = current_time;
	}
	buffer[offset] = element.GetCodeFunction();
	offset += 1;
	buffer[offset] = element.GetCount();
	offset += 1;
	memcpy(&buffer[offset], element.GetTypes(), element.GetCount());
	offset += element.GetCount();
	buffer[offset] = element.GetSizeOfData();
	offset += 1;
	memcpy(&buffer[offset], element.GetData(), element.GetSizeOfData());
	offset += element.GetSizeOfData();
	return true;
}

#endif
