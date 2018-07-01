import java.util.*;

//-----------------------------------------------------------------------------
// Write a user-level test program called Test4.java that conducts disk 
// validity tests and measures the performance. 
//
// Test4.java should first call flush to clear the cache before executing. 
// In this way the cache will not have valid blocks from previous applications 
// or tests. It should receive the following two arguments and perform a 
// different test according to a combination of those arguments.
//------------------------------------------------------------------------------
public class Test4 extends Thread
{
	int testCase;
	private boolean enabled = false;
	private String m_enabled = "";
	private byte[] readBlocks;
	private byte[] writeBlocks;
	private int blockAddress[];
	private static final int BLOCKSIZE = 512;
	private String userSelection = "";
	private long readBegin = 0L;
	private long readFinish = 0L;
	private long writeBegin= 0L;
	private long writeFinish = 0L;
	private long averageReadTime = 0L;
	private long averageWriteTime = 0L;
	private Random rng;
	private boolean equals = false;
	
	//-------------------------------------------------------------------------
	// Constructor
	//-------------------------------------------------------------------------
	public Test4(String args[])
	{
		// Check to see if we will use the disk cache or not
		if(args[0].equals("enabled"))
		{
			m_enabled = "enabled";
			enabled = true;
		}
		else
		{
			m_enabled = "diabled";
		}
		// Get the test case we will run
		testCase = Integer.parseInt(args[1]);
		readBlocks = new byte[BLOCKSIZE];
		writeBlocks = new byte[BLOCKSIZE];
		blockAddress = new int[BLOCKSIZE / 2];
		rng = new Random();
	}
	
	//-------------------------------------------------------------------------
	// Run function 
	//-------------------------------------------------------------------------
	public void run()
	{
		// Test4.java should first call flush to clear the cache before executing.
		SysLib.flush();
		switch(testCase)
		{
			// Perform Random Access
			case 1:
				userSelection = "Random accesses";
				RandomAccesses();
				break;
			// Perform Local Access
			case 2:
				userSelection = "Localized accesses";
				LocalizedAccesses();
				break;
			// Perform Mixed Access
			case 3:
				userSelection = "Mixed accesses";
				MixedAccesses();
				break;
			// Perform Adversarial Accesss
			case 4:
				userSelection = "Adversarial accesses";
				AdversaryAccesses();
				break;
			default:
				SysLib.cout("Invalid disk validity test" + "\n");
				break;
		}
		sync();
		SysLib.exit();
	}
	
	//-------------------------------------------------------------------------
	// Read
	//-------------------------------------------------------------------------
	private void read(int blockId, byte[] buf)
	{
		if(enabled == true)
		{
			SysLib.cread(blockId, buf);
		}
		else
		{
			SysLib.rawread(blockId, buf);
		}
	}
	
	//-------------------------------------------------------------------------
	// Write
	//-------------------------------------------------------------------------
	private void write(int blockId, byte[] buf)
	{
		if(enabled == true)
		{
			SysLib.cwrite(blockId, buf);
		}
		else
		{
			SysLib.rawwrite(blockId, buf);
		}
	}
	
	//-------------------------------------------------------------------------
	// Sync
	//-------------------------------------------------------------------------
	public void sync()
	{
		if(enabled == true)
		{
			SysLib.csync();
		}
		else
		{
			SysLib.sync();
		}
	}
	
	//-------------------------------------------------------------------------
	// Random Access: read and write many blocks randomly across the disk. 
	//				  Verify the correctness of your disk cache.
	//-------------------------------------------------------------------------
	public void RandomAccesses()
	{	
		for(int i = 0; i < blockAddress.length; i++)
		{
			blockAddress[i] = (Math.abs(rng.nextInt()) % BLOCKSIZE);
		}
		
		// Start clocking write time and write
		writeBegin = new Date().getTime();
		for(int i = 0; i < blockAddress.length; i++)
		{
			write(blockAddress[i], writeBlocks);
		}
		// Finish clocking write time and calculate average time 
		writeFinish = new Date().getTime();
		averageWriteTime = ((writeFinish - writeBegin) / blockAddress.length);
		
		// Start clocking read time and read
		readBegin = new Date().getTime();
		for(int i = 0; i < blockAddress.length; i++)
		{
			read(blockAddress[i], readBlocks);
		}
		// Finish clocking read time and calculate average read time
		readFinish = new Date().getTime();
		averageReadTime = ((readFinish - readBegin) / blockAddress.length);
		
		// Test to see if read and write blocks are same or if there is invalid 
		// cached block
		equals = Arrays.equals(writeBlocks, readBlocks);
		if(equals == false)
		{
			SysLib.cout("ERROR: invalid cached block found" + "\n");
		}
		
		// Display output to console
		SysLib.cout("Test case: " + userSelection + ", caching is " + m_enabled
			+ "\n");
		SysLib.cout("Average write time was: " + averageWriteTime + " milliseconds"
			+ "\n");
		SysLib.cout("Average read time was: " + averageReadTime + " milliseconds"
			+ "\n");
	}
	
	//-------------------------------------------------------------------------
	// Localized Access: read and write a small selection of blocks many times 
	//				     to get a high ratio of cache hits.
	//-------------------------------------------------------------------------
	public void LocalizedAccesses()
	{
		// Start clocking write time and write
		writeBegin = new Date().getTime();
		for(int i = 0; i < blockAddress.length; i++)
		{
			for(int j = 0; j < 10; j++)
			{
				write(j, writeBlocks);
			}
		}
		
		// Finish clocking write time and calculate average write time
		writeFinish = new Date().getTime();
		averageWriteTime = ((writeFinish - writeBegin) / blockAddress.length);
		
		// Start clocking read time and read
		readBegin = new Date().getTime();
		for(int i = 0; i < blockAddress.length; i++)
		{
			for(int j = 0; j < 10; j++)
			{
				read(j, readBlocks);
			}
		}
		
		// Finish clocking read time and calculate average read time 
		readFinish = new Date().getTime();
		averageReadTime = ((readFinish - readBegin) / blockAddress.length);
		
		// Test to see if read and write blocks are same or if there is invalid 
		// cached block
		equals = Arrays.equals(writeBlocks, readBlocks);
		if(equals == false)
		{
			SysLib.cout("ERROR: invalid cached block found" + "\n");
		}
		
		// Display output to console
		SysLib.cout("Test case: " + userSelection + ", caching is " + m_enabled
			+ "\n");
		SysLib.cout("Average write time was: " + averageWriteTime + " milliseconds"
			+ "\n");
		SysLib.cout("Average read time was: " + averageReadTime + " milliseconds"
			+ "\n");
	}
	
	//-------------------------------------------------------------------------
	// Mixed Access: 90% of the total disk operations should be localized 
	//				 accesses and 10% should be random accesses.
	//-------------------------------------------------------------------------
	public void MixedAccesses()
	{
		for(int i = 0; i < blockAddress.length; i++)
		{
			int random = Math.abs(rng.nextInt() % 10);
			// One one out of ten should be random access
			if(random == 9)
			{
				blockAddress[i] = Math.abs(rng.nextInt() % 512);
			}
			// Other 9/10 should be localized
			else
			{
				blockAddress[i] = Math.abs(rng.nextInt() % 10);
			}
		}
		
		// Start clocking write time and write
		writeBegin = new Date().getTime();
		for(int i = 0; i < blockAddress.length; i++)
		{
			write(blockAddress[i], writeBlocks);
		}
		
		// Finish clocking write time and calculate average write time
		writeFinish = new Date().getTime();
		averageWriteTime = ((writeFinish - writeBegin) / blockAddress.length);
		
		// Start clocking read time and read
		readBegin = new Date().getTime();
		for(int i = 0; i < blockAddress.length; i++)
		{
			read(blockAddress[i], readBlocks);
		}
		
		// Finish clocking read time and calculate average read time
		readFinish = new Date().getTime();
		averageReadTime = ((readFinish - readBegin) / blockAddress.length);
		
		// Test to see if read and write blocks are same or if there is invalid 
		// cached block
		equals = Arrays.equals(writeBlocks, readBlocks);
		if(equals == false)
		{
			SysLib.cout("ERROR: invalid cached block found" + "\n");
		}
		
		// Display output to console
		SysLib.cout("Test case: " + userSelection + ", caching is " + m_enabled
			+ "\n");
		SysLib.cout("Average write time was: " + averageWriteTime + " milliseconds"
			+ "\n");
		SysLib.cout("Average read time was: " + averageReadTime + " milliseconds"
			+ "\n");
	}
	
	//-------------------------------------------------------------------------
	// Adversarial Access: generate disk accesses that do not make good use of 
	//					   the disk cache at all.
	//-------------------------------------------------------------------------
	public void AdversaryAccesses()
	{
		for(int i = 0; i < blockAddress.length; i++)
		{
			blockAddress[i] = (i % 7) + 1;
		}
		// Start clocking write time and write
		writeBegin = new Date().getTime();
		for(int i = 0; i < blockAddress.length; i++)
		{
			write(blockAddress[i], writeBlocks);
		}
		// Finish clocking write time and calculate average time 
		writeFinish = new Date().getTime();
		averageWriteTime = ((writeFinish - writeBegin) / blockAddress.length);
		
		// Start clocking read time and read
		readBegin = new Date().getTime();
		for(int i = 0; i < blockAddress.length; i++)
		{
			read(blockAddress[i], readBlocks);
		}
		// Finish clocking read time and calculate average read time
		readFinish = new Date().getTime();
		averageReadTime = ((readFinish - readBegin) / blockAddress.length);
		
		// Test to see if read and write blocks are same or if there is invalid 
		// cached block
		equals = Arrays.equals(writeBlocks, readBlocks);
		if(equals == false)
		{
			SysLib.cout("ERROR: invalid cached block found" + "\n");
		}
		
		// Display output to console
		SysLib.cout("Test case: " + userSelection + ", caching is " + m_enabled
			+ "\n");
		SysLib.cout("Average write time was: " + averageWriteTime + " milliseconds"
			+ "\n");
		SysLib.cout("Average read time was: " + averageReadTime + " milliseconds"
			+ "\n");
	}
}