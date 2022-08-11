package com.gap.pem.cds.stores;

import java.util.Arrays;

/**
 * Stores a Boolean[] for each item in the store.
 * @see BitSetStore for packing logical booleans into one bit.
 * @see com.gap.pem.cds.util.BitMatrix for treating as a logical 2-dimensional array.
 */
public class BooleanArrayStore extends GenericStore implements IBooleanArrayStore
{
	private static final long serialVersionUID = -4185536571620915684L;

	private static final Boolean[] EMPTY = null;
    
    private Boolean[][][]     chunks;

    /**
     * Create a store which will store arrays.  This is useful for compact 
     * time-phased data.
     */
    public BooleanArrayStore()
    {
    	super();
    }

    public BooleanArrayStore(int initialChunkCount, int inputChunkSize)
    {
        super(initialChunkCount, inputChunkSize);
    }

	@Override
    protected void allocateStore (int chunkCount, int inputChunkSize)
	{
		super.allocateStore(chunkCount, inputChunkSize);
		chunks = new Boolean[numChunks][][];
	}

	/**
	 * Get the value at the given index. If the index is less than the 
	 * current size of the store, a value will be returned, even if it 
	 * doesn't exist. Use the {@link #isEmptyValue(Boolean[])} method to check 
	 * if the returned value is an indication that the value doesn't 
	 * exist. 
	 * 
	 * @param index non-negative integer less than the size of the store.
	 * @return a value, could be the empty value.
	 * @throws ArrayIndexOutOfBoundsException if the index is equal to or
	 * greater than the current size of the store.
	 */
	@Override
    public Boolean[] getElement(int index)
    {
        int chunkno = index >> chunkSizeLog;
        int pos = index & chunkSizeMask;
        if (index > validIndex)
        {
            throw new ArrayIndexOutOfBoundsException("Index is beyond "
                      + "the current valid position: " + index
                      + ".  Currently the maximums are [" + chunkno + "][" + pos + "]");
        }

        Boolean[][] chunk = chunks[chunkno];
        if (chunk == null)
        {
        	return BooleanArrayStore.EMPTY;
        }

        return chunks[chunkno][pos];
    }

    /**
     * Check if the given value is indicating a non-existent value in
     * the store. This is typically used to validate the value returned
     * by {@link #getElement(int)}.
     * 
     * @param value an integer value
     * @return true or false
     */
	@Override
    public boolean isEmptyValue(Boolean[] value){
    	return (value == BooleanArrayStore.EMPTY);
    }

    /**
     * Get a value that be used to indicates the non-existence of
     * a real value in the store.
     *
     * @return a value that indicates empty.
     */
    @Override
    public Boolean[] getEmptyValue(){
    	return BooleanArrayStore.EMPTY;
    }
    
    /**
     * Add the given value to the data store and place it at the end
     * of existing values. The size of the store will increase by 
     * one.
     * 
     * @param value the value to add to the end of the store
     * @return the index at which the values were placed in the store.
     * @throws IllegalStateException if the store has already reached
     * its maximum capacity, which is Integer.MAX_VALUE.
     */
	@Override
    public int addElement (Boolean[] value)
    {
		int nextIndex = validIndex + 1;
        if (nextIndex >= Integer.MAX_VALUE)
        {
             throw new IllegalStateException("Maximum limit of " +
                     "store reached. Cannot Add Further");
        }
		int chunkno = nextIndex >> chunkSizeLog;
		int pos = nextIndex & chunkSizeMask;
		if ( numChunks <= chunkno )
		{
            int newNumChunks = numChunks + Constants.INITIAL_CHNK_CNT;
            Boolean[][][] newAllocate = new Boolean[newNumChunks][][];
			/* want to reuse the chunks which were allocated earlier.
			   	hence allocating only the outer array and copying the
			   	earlier positions.*/
            System.arraycopy(chunks, 0, newAllocate, 0, numChunks);
			
            chunks = newAllocate;
			numChunks = newNumChunks;
		}

		Boolean[][] chunk = chunks[chunkno];
		if (chunk == null)
		{
		    chunks[chunkno] = BooleanArrayStore.createChunkWithEmptyValues(chunkSize);
        }

        chunks[chunkno][pos] = value;
        ++validIndex;
        return validIndex;
	}
	
    /**
     * Set the element at the given index to be the given value, and get
     * back the previous value.<p>
     * <ol>
     * <li>If the given index is the same as the current size of the store. 
     * this method behaves as {@link #addElement(Boolean[])}, except that it
     * will be returning the empty value as the previous value.</li> 
     * <li>If the given index is beyond the current size of the store, 
     * the gap will be filled by the empty value before the given value 
     * is added at the end to the given index. The returned value will 
     * be the empty value.</li>
     * <li>If there is a value previously at the given index, it will be 
     * replaced by the given value, even if the new value is the empty
     * value. The previous value will be returned in this case.</li>
     * </ol>
     * @param index a non-negative integer less than Integer.MAX_VALUE.
     * @param value a double[] value, could be the empty value.
     * @return the previous value at the given index, if it existed.
     */
	@Override
    public Boolean[] setElementAt(int index, Boolean[] value){
    	if(index >= Integer.MAX_VALUE){
            throw new IllegalArgumentException("Maximum limit of " +
            	"store reached. Cannot set element at " + index);
    	}

    	// The would-be index of the chunk that will hold the value for
    	// the given index.
        int chunkIndex = index >> chunkSizeLog;
        int pos = index & chunkSizeMask;
        
        // Allocate the chunk if it has not been allocated yet.
    	if (chunkIndex >= numChunks)
		{
    		// Get the number of chunks we need to hold the value at the the given index, 
    		// rounded up to the closest multiple of the chunk count.
    		// 
    		// minimal needed chunk count = chunk index + 1;
    		// rounded-up chunk count = (chunkIndex + 1 + (Constants.INITIAL_CHNK_CNT - 1)) / 
    		//                         Constants.INITIAL_CHNK_CNT * Constants.INITIAL_CHNK_CNT;
    		//    = (chunkIndex / Constants.INITIAL_CHNK_CNT + 1) * Constants.INITIAL_CHNK_CNT;
    		//
    		int newChunkCount = (chunkIndex/Constants.INITIAL_CHNK_CNT+1) * Constants.INITIAL_CHNK_CNT;
			Boolean[][][] newAllocate = new Boolean[newChunkCount][][];
			/* want to reuse the chunks which were allocated earlier.
			   	hence allocating only the outer array and copying the
			   	earlier positions.*/
            System.arraycopy(chunks, 0, newAllocate, 0, numChunks);
			
            chunks = newAllocate;
			numChunks = newChunkCount;
		}

    	validIndex = (index > validIndex) ? index : validIndex;
    	
		if (chunks[chunkIndex] == null)
		{
		    chunks[chunkIndex] = BooleanArrayStore.createChunkWithEmptyValues(chunkSize);
        }

		Boolean[] oldvalue = chunks[chunkIndex][pos];
        chunks[chunkIndex][pos] = value;
        return oldvalue;
    }
	
    /**
     * Grow the size of the store, if necessary, to ensure that it holds
     * at least the given number of elements.
     * <p>
     * If the store is grown due to this call, the extra space will 
     * occupied by empty values.
     * <p>
     * Note that the returned size is not always equal to the desired 
     * size. In the case when the desired size is less than the current
     * size of the store, the current size will be returned.
     * 
     * @param minimumSize the desired minimum size
     * @return the updated size of the store. 
     */
    @Override
    public int ensureSize(int minimumSize){
    	// No need to check if size is equal to MAX_VALUE because it
    	// must be. And we are capable of growing to that size if 
    	// needed.

    	int index = minimumSize-1;
    	if(index > validIndex){
    		int chunkIndex = index >> chunkSizeLog;
        	if (chunkIndex >= numChunks)
    		{
        		// Get the number of chunks we need to hold the value at the the given index, 
        		// rounded up to the closest multiple of the chunk count.
        		// 
        		// minimal needed chunk count = chunk index + 1;
        		// rounded-up chunk count = (chunkIndex + 1 + (Constants.INITIAL_CHNK_CNT - 1)) / 
        		//                         Constants.INITIAL_CHNK_CNT * Constants.INITIAL_CHNK_CNT;
        		//    = (chunkIndex / Constants.INITIAL_CHNK_CNT + 1) * Constants.INITIAL_CHNK_CNT;
        		//
        		int newChunkCount = (chunkIndex/Constants.INITIAL_CHNK_CNT+1) * Constants.INITIAL_CHNK_CNT;
        		Boolean[][][] newAllocate = new Boolean[newChunkCount][][];
    			/* want to reuse the chunks which were allocated earlier.
    			   	hence allocating only the outer array and copying the
    			   	earlier positions.*/
                System.arraycopy(chunks, 0, newAllocate, 0, numChunks);
    			
                chunks = newAllocate;
    			numChunks = newChunkCount;
    		}

        	validIndex = index;
    	}
    	
    	return validIndex+1;
    }

    @Override
    public String toString() {
        return "BooleanArrayStore{" +
                "chunks=" + (chunks == null ? null : Arrays.asList(chunks)) +
                '}';
    }

    private static Boolean[][] createChunkWithEmptyValues(int size){
    	Boolean[][] chunk = new Boolean[size][];
    	// no need to set the values individually, the default
    	// is already BooleanArrayStore.EMPTY;
    	return chunk;
    }
}


