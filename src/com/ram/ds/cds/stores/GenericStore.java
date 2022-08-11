package com.ram.ds.cds.stores;

/**
 * Base class for holding a data store composed of multiple chunks.  Each chunk's size is a power of
 * 2.  The store is growable, and ordered.
 */
public abstract class GenericStore implements IDataStore {

    private static final long serialVersionUID = 1974366288246131347L;
    protected int            numChunks;  // how many chunks have been allocated
    protected int            chunkSize;  // the number of elements each chunk can hold, power of 2.
    protected int            validIndex; // index of the last element in the store, initially -1.
    protected int            chunkSizeLog;
    protected int            chunkSizeMask;

    public GenericStore()
    {
		allocateStore (Constants.INITIAL_CHNK_CNT, Constants.DEF_CHNK_SIZE);
    }

    public GenericStore(int initialChunkCount, int inputChunkSize)
    {
		allocateStore (initialChunkCount, inputChunkSize);
    }

    /**
     * Base implementation.  Initializes size parameters but does not allocate storage.
     * 
     * @param chunkCount
     * @param inputChunkSize
     */
	protected void allocateStore (int chunkCount, int inputChunkSize)
	{
        validIndex = -1;
        numChunks = 0;
		chunkSize = inputChunkSize;
		// if the given chunk size is not a power of 2, then the next three lines will
		// adjust it to the next power of 2. 
		chunkSize--;
		chunkSizeLog = logBase2(chunkSize);
		chunkSize = 1 << chunkSizeLog;
		chunkSizeMask = chunkSize - 1;
		if (chunkCount == 0)
		{
			numChunks = Constants.INITIAL_CHNK_CNT;
		}
		else
		{
			numChunks = chunkCount;
		}
	}

    private static int logBase2(int chunkSize) {
        int chunkSizeLog = 0;
        do {
            chunkSize >>= 1;
            if (chunkSize != 0)
                chunkSizeLog += 1;
            else
                break;
        } while (true);

        chunkSizeLog += 1;

        return chunkSizeLog;
    }

    @Override
    public int size()
    {
        return validIndex + 1;
    }

    @Override
    public String toString() {
        return "GenericStore{" +
                "numChunks=" + numChunks +
                ", chunkSize=" + chunkSize +
                ", validIndex=" + validIndex +
                ", chunkSizeLog=" + chunkSizeLog +
                ", chunkSizeMask=" + chunkSizeMask +
                '}';
    }

    @Override
    public long getDataSize() {
        return 0;  // derived classes should compute their sizes.
    }
}

