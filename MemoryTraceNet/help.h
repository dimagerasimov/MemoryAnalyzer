#ifndef __HELP
#define __HELP

typedef unsigned char byte;

class Help {
public:
	static bool isBigEndian() {
		int value = 0;
		((char*)&value)[0] = 1;
		return value != 1;
	}
	static bool isLittleEndian() {
		int value = 0;
		((char*)&value)[0] = 1;
		return value == 1;
	}
	static void hton(byte* arr, int size) {
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
	static void ntoh(byte* arr, int size) {
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
};

#endif
