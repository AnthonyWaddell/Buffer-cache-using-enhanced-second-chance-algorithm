import java.util.*;


//-----------------------------------------------------------------------------
// Cache class
//-----------------------------------------------------------------------------
public class Cache
{	
	
	private int blockSize;
	private int victim;
	private CacheBlock[] cacheTable = null;
	private Vector <byte[]> byteBuffer;
	private static int INVALID = -1;
	//-------------------------------------------------------------------------
	// CacheBlock subclass
	//-------------------------------------------------------------------------
	private class CacheBlock
	{
		public int blockFrame;
		public boolean referenceBit;
		public boolean dirtyBit;
		
		public CacheBlock()
		{
			blockFrame = -1;
			referenceBit = false;
			dirtyBit = false;
		}
	}
	
	//-------------------------------------------------------------------------
	// The constructor: allocates a cacheBlocks number of cache blocks, each 
	// containing blockSize-byte data, on memory
	//-------------------------------------------------------------------------
	public Cache(int block_size, int cacheBlocks)
	{
		blockSize = block_size;					  // Set block size
		victim = 0;								  // For blockId of victim block
		cacheTable = new CacheBlock[cacheBlocks]; // Create the CacheBlock
		byteBuffer = new Vector<byte[]>();
		
		// Initialize CacheBlock[] and buffer for byte data
		for(int i = 0; i < cacheBlocks; i++)
		{
			byte[] temp = new byte[blockSize];
			cacheTable[i] = new CacheBlock();
			byteBuffer.add(temp);
		}
	}
	
	//-------------------------------------------------------------------------
	// Reads into the buffer[ ] array the cache block specified by blockId from
	// the disk cache if it is in cache, otherwise reads the corresponding disk
	// block from the disk device. Upon an error, it should return false,
	// otherwise return true.
	//-------------------------------------------------------------------------
	public synchronized boolean read(int blockId, byte buffer[])
	{
		boolean ret_val = true;
		int exists;
		byte[] temp;
		
		// If blockId is invalid, return
		if(blockId < 0)
		{
			ret_val = false;
			return ret_val;
		}
		
		for(int i = 0; i < cacheTable.length; i++)
		{
			if(cacheTable[i].blockFrame == blockId)
			{
				temp = byteBuffer.elementAt(i);
				System.arraycopy(temp, 0, buffer, 0, blockSize);
				cacheTable[i].referenceBit = true;
				return ret_val;
			}
		}
		
		// Find next victim
		exists = find();
		if(exists == INVALID)
		{
			exists = secondChanceAlgorithm();
		}
		if(cacheTable[exists].dirtyBit == true)
		{
				if(cacheTable[exists].blockFrame != INVALID)
				{
					SysLib.rawwrite(cacheTable[exists].blockFrame, byteBuffer.elementAt(exists));
					cacheTable[exists].dirtyBit = false;
				}
		}
		
		// Read from disk
		SysLib.rawread(blockId, buffer);
		temp = new byte[blockSize];
		System.arraycopy(buffer, 0, temp, 0, blockSize);
		byteBuffer.set(exists, temp);
		cacheTable[exists].referenceBit = true;
		cacheTable[exists].blockFrame = blockId;
		return ret_val;
		
	}
	
	//-------------------------------------------------------------------------
	// Writes the buffer[ ]array contents to the cache block specified by
	// blockId from the disk cache if it is in cache, otherwise finds a free 
	// cache block and writes the buffer [ ] contents on it. No write through. 
	// Upon an error, it should return false, otherwise return true.
	//-------------------------------------------------------------------------
	public synchronized boolean write(int blockId, byte buffer[])
	{
		boolean ret_val = true;
		int exists;
		byte[] temp;
		
		// If blockId is invalid, return
		if(blockId < 0)
		{
			ret_val = false;
			return ret_val;
		}
		
		for(int i = 0; i < cacheTable.length; i++)
		{
			if(cacheTable[i].blockFrame == blockId)
			{
				temp = byteBuffer.elementAt(i);
				System.arraycopy(temp, 0, buffer, 0, blockSize);
				byteBuffer.set(i, temp);
				cacheTable[i].dirtyBit = true;
				cacheTable[i].referenceBit = true;
			}
		}
		
		// Find next victim
		exists = find();
		if(exists == INVALID)
		{
			exists = secondChanceAlgorithm();
		}
		if(cacheTable[exists].dirtyBit == true)
		{
				if(cacheTable[exists].blockFrame != INVALID)
				{
					SysLib.rawwrite(cacheTable[exists].blockFrame, byteBuffer.elementAt(exists));
					cacheTable[exists].dirtyBit = false;
				}
		}
		
		// Write to disk
		SysLib.rawwrite(blockId, buffer);
		temp = new byte[blockSize];
		System.arraycopy(buffer, 0, temp, 0, blockSize);
		byteBuffer.set(exists, temp);
		// Update dirtyBit
		cacheTable[exists].dirtyBit = true;
		cacheTable[exists].referenceBit = true;
		cacheTable[exists].blockFrame = blockId;
		return ret_val;
		
	}
	
	//-------------------------------------------------------------------------
	// Writes back all dirty blocks to the DISK file. The sync( ) method 
	// maintains valid clean copies of the block in Cache.java. Call sync() 
	// when shutting down ThreadOS.
	//-------------------------------------------------------------------------
	public synchronized void sync()
	{
		for (int i = 0; i < cacheTable.length; i++)
		{
			if(cacheTable[i].dirtyBit == true)
			{
				if(cacheTable[i].blockFrame != INVALID)
				{
					SysLib.rawwrite(cacheTable[i].blockFrame, byteBuffer.elementAt(i));
					cacheTable[i].dirtyBit = false;
				}
			}
		}
		// Sync disk
		SysLib.sync();
	}
	
	//-------------------------------------------------------------------------
	// Writes back all dirty blocks to the DISK file. The flush( ) method
	// invalidates all cached blocks. Call flush between the running of 
	// performance tests so each test starts with an empty cache.
	//-------------------------------------------------------------------------
	public synchronized void flush()
	{
		for(int i = 0; i < cacheTable.length; i++)
		{
			if(cacheTable[i].dirtyBit == true)
			{
				if(cacheTable[i].blockFrame != INVALID)
				{
					SysLib.rawwrite(cacheTable[i].blockFrame, byteBuffer.elementAt(i));
					cacheTable[i].dirtyBit = false;
				}
			}
			cacheTable[i].referenceBit = false;
			cacheTable[i].blockFrame = INVALID;
		}
		// Sync disk
		SysLib.sync();
	}
	
	//-------------------------------------------------------------------------
	// Return the position of an invalid page
	//-------------------------------------------------------------------------
	public int find()
	{
		// Iterate over page table looking for invalid index
		int ret_val = INVALID;
		for(int i = 0; i < cacheTable.length; i++)
		{
			if(cacheTable[i].blockFrame == INVALID)
			{
				ret_val = i;
				return ret_val;
			}
		}
		return ret_val;
	}
	
	//-------------------------------------------------------------------------
	// Enhanced Second Chance Algorithm
	//-------------------------------------------------------------------------
	public int secondChanceAlgorithm()
	{
		// Iterate over cacheTable
		while(true)
		{
			// Advance the victim
			victim++;
			victim = victim % cacheTable.length;
			if(cacheTable[victim].referenceBit == false)
			{
				return victim;
			}
			// Reset it to 0 or false by the second-chance algorithm when 
			// searching a next victim.
			cacheTable[victim].referenceBit = false;
		}
	}
}