#ifndef __BIN_JOURNAL
#define __BIN_JOURNAL

#include <stdio.h>
#include <vector>
#include <string>

#include "help.h"
#include "binf_element.h"

#define MFREE_SECTION 0x01

class BinJournal {
	public:
		bool AddBinf(const BinfElement& element, const int number_of_section);
		bool Push( void );

		BinJournal(const char* name_journal);
		~BinJournal( void );

	private:
		char name_journal[64];
		FILE* TraceFile;

		type_long mfree_size;
		std :: vector<BinfElement> mfree_elements;

		bool WriteBinf(BinfElement& binfElement);
		bool WriteSectionMFree( void );
};

BinJournal :: BinJournal(const char* name_journal)
{
	mfree_size = 0;
	strcpy(this->name_journal, name_journal);
}
BinJournal :: ~BinJournal( void )
{
	mfree_elements.clear();
}
bool BinJournal :: AddBinf(const BinfElement& element, const int number_of_section)
{
	bool addOk;
	switch(number_of_section)
	{
		case MFREE_SECTION: {
			mfree_size += (type_long)((BinfElement&)element).Size();
			mfree_elements.push_back(element);
			addOk = true;
			break;
		}
		default: {
			addOk = false; break; }
	}
	return addOk;
}
bool BinJournal :: WriteBinf(BinfElement& binfElement)
{
	int offset, written_items;
	byte buffer[256];

	offset = 0;
	buffer[offset] = binfElement.GetCodeFunction();
	offset += 1;
	buffer[offset] = binfElement.GetCount();
	offset += 1;
	memcpy(&buffer[offset], binfElement.GetTypes(), binfElement.GetCount());
	offset += binfElement.GetCount();
	buffer[offset] = binfElement.GetSizeOfData();
	offset += 1;
	memcpy(&buffer[offset], binfElement.GetData(), binfElement.GetSizeOfData());
	offset += binfElement.GetSizeOfData();

	written_items = fwrite(buffer,
		sizeof(byte), offset, TraceFile);
	if(written_items != offset)
		{return false; }

	return true;
}
bool BinJournal :: WriteSectionMFree( void )
{
	int written_items;

	byte name_of_section = MFREE_SECTION;
	written_items = fwrite(&name_of_section, sizeof(byte), 1, TraceFile);
	if(written_items != 1)
		{return false; }

	type_long size_of_section = (type_long)mfree_size;
	Help :: hton((byte*)&size_of_section, sizeof(type_long));
	written_items = fwrite(&size_of_section, sizeof(type_long), 1, TraceFile);
	if(written_items != 1)
		{return false; }

	bool writeOk;
	int size = mfree_elements.size();
	for(int i = 0; i < size; i++)
	{
		writeOk = WriteBinf(mfree_elements[i]);
		if(!writeOk) { return false; }
	}
	return true;
}
bool BinJournal :: Push( void )
{
	TraceFile = fopen(name_journal, "wb");
	if(TraceFile == NULL) { return false; }

	bool writeSectionOk = true;
	if(writeSectionOk)
		{ writeSectionOk = WriteSectionMFree(); }
	//else if for next sections

	fflush(TraceFile);
	fclose(TraceFile);

	return writeSectionOk;
}

#endif
