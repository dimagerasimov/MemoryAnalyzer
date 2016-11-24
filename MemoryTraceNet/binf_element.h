#ifndef __BINF_ELEMENT
#define __BINF_ELEMENT

#include <memory.h>

#define FCODE_MALLOC 0
#define FCODE_FREE 1

#define TCODE_PTR 0
#define TCODE_SIZE_T 1
#define TCODE_LONG 2

typedef unsigned long long int type_ptr;
typedef unsigned long long int type_size_t;
typedef unsigned long long int type_long;

typedef unsigned char byte;

class BinfElement {
	public:
		BinfElement(const byte __code_function, const byte __count,
			const byte* __types, const byte __size_of_data, const byte* __data);
		BinfElement(const BinfElement& obj);
		~BinfElement( void );

		byte GetCodeFunction( void );
		byte GetCount( void );
		byte* GetTypes( void );
		byte GetSizeOfData( void );
		byte* GetData( void );
		byte Size( void );

	private:
		byte code_function;
		byte count;
		byte* types;
		byte size_of_data;
		byte* data;
};

BinfElement :: BinfElement(const byte __code_function, const byte __count,
	const byte* __types, const byte __size_of_data, const byte* __data) :
		code_function(__code_function), count(__count), size_of_data(__size_of_data)
{
	types = new byte[__count];
	data = new byte[__size_of_data];
	memcpy(types, __types, __count * sizeof(byte));
	memcpy(data, __data, __size_of_data);
}
BinfElement :: BinfElement(const BinfElement& obj) :
	code_function(obj.code_function), count(obj.count), size_of_data(obj.size_of_data)
{
	this->types = new byte[obj.count];
	this->data = new byte[obj.size_of_data];
	memcpy(this->types, obj.types, obj.count * sizeof(byte));
	memcpy(this->data, obj.data, obj.size_of_data);
}
BinfElement :: ~BinfElement( void )
{
	delete[] types;
	delete[] data;
}
byte BinfElement :: GetCodeFunction( void )
{
	return code_function;
}
byte BinfElement :: GetCount( void )
{
	return count;
}
byte* BinfElement :: GetTypes( void )
{
	return types;
}
byte BinfElement :: GetSizeOfData( void )
{
	return size_of_data;
}
byte* BinfElement :: GetData( void )
{
	return data;
}
byte BinfElement :: Size( void )
{
	return sizeof(code_function) + sizeof(count) +
		sizeof(size_of_data) + count * sizeof(byte) + size_of_data;
}

#endif

