package com.ram.ds.cds.stores;

/**
 * Stores a scalar double for each item.
 * This class is meant to provide functionality of an array with auto
 * resize feature. The implementation allocates only the outer array
 * with given initial size upfront. The individual chunks are allocated
 * on an as needed basis
 *
 */
public class DoubleStore extends GenericStore implements IDoubleStore {

    private static final long serialVersionUID = 3037993478913964308L;
    
    private static final double EMPTY = Double.NaN;
    
    /**
     * Storage area for all number of this virtual array
     */
    private double[][] chunks;

    /**
     * Default constructor
     */
    public DoubleStore() {
        super();
    }

    /**
     * Constructor which provides control over the sizing of the
     * initial array (i.e. number of chunks and the sizes of the chunks)
     *
     * @param initialChunkCount Initial number of chunks to allow
     *                          (note chunk memory will only be allocated as needed)
     * @param inputChunkSize    The size of the chunks
     */
    public DoubleStore(int initialChunkCount, int inputChunkSize) {
        super(initialChunkCount, inputChunkSize);
    }

    /**
     * Allocates the outer array.
     *
     * @param chunkCount     Initial number of chunks to allow
     *                       (note chunk memory will only be allocated as needed)
     * @param inputChunkSize The size of the chunks
     */
    @Override
    protected void allocateStore(int chunkCount, int inputChunkSize) {
        super.allocateStore(chunkCount, inputChunkSize);
        chunks = new double[numChunks][];
    }

	/**
	 * Get the value at the given index. If the index is less than the 
	 * current size of the store, a value will be returned, even if it 
	 * doesn't exist. Use the {@link #isEmptyValue(double)} method to check 
	 * if the returned value is an indication that the value doesn't 
	 * exist. 
	 * 
	 * @param index non-negative integer less than the size of the store.
	 * @return a value, could be the empty value.
	 * @throws ArrayIndexOutOfBoundsException if the index is equal to or
	 * greater than the current size of the store.
	 */
	@Override
    public double getElement(int index) {
        int chunkno = index >> chunkSizeLog;
        int pos = index & chunkSizeMask;
        if (index > validIndex)
        {
            throw new ArrayIndexOutOfBoundsException("Index is beyond "
                      + "the current valid position: " + index
                      + ".  Currently the maximums are [" + chunkno + "][" + pos + "]");
        }

        double[] chunk = chunks[chunkno];
        if (chunk == null)
        {
        	return DoubleStore.EMPTY;
        }

        return chunks[chunkno][pos];
    }

    /**
     * Check if the given value is indicating a non-existent value in
     * the store. This is typically used to validate the value returned
     * by {@link #getElement(int)}.
     * 
     * @param value a double value
     * @return true or false
     */
	@Override
    public boolean isEmptyValue(double value){
		return Double.isNaN(value);
    }

    /**
     * Get a value that be used to indicates the non-existence of
     * a real value in the store.
     *
     * @return a value that indicates empty.
     */
    @Override
    public double getEmptyValue(){
    	return DoubleStore.EMPTY;
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
    public int addElement(double value){
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
			double[][] newAllocate = new double[newNumChunks][];
			/* want to reuse the chunks which were allocated earlier.
			   	hence allocating only the outer array and copying the
			   	earlier positions.*/
            System.arraycopy(chunks, 0, newAllocate, 0, numChunks);
			
            chunks = newAllocate;
			numChunks = newNumChunks;
		}

		double[] chunk = chunks[chunkno];
		if (chunk == null)
		{
		    chunks[chunkno] = DoubleStore.createChunkWithEmptyValues(chunkSize);
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
     * this method behaves as {@link #addElement(double)}, except that it
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
     * @param value a double value, could be the empty value.
     * @return the previous value at the given index, if it existed.
     */
	@Override
    public double setElementAt(int index, double value){
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
			double[][] newAllocate = new double[newChunkCount][];
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
		    chunks[chunkIndex] = DoubleStore.createChunkWithEmptyValues(chunkSize);
        }

        double oldvalue = chunks[chunkIndex][pos];
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
    			double[][] newAllocate = new double[newChunkCount][];
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
        return "DoubleStore{" +
                "chunks=" + toChunkString(chunks) +
                '}';
    }

    private String toChunkString(double[][] iChunk) {
        if (iChunk == null) return "[[NULL]]";

        int size = size();

        int currPos = 0;
        StringBuilder sb = new StringBuilder(10000);
        for (double[] innerChunk : iChunk) {
            if (innerChunk == null) {
                sb.append("[null],");
                break;
            }

            for (double i : innerChunk) {
                sb.append(i).append(',');
                currPos++;
                if (currPos >= size) {
                    break;
                }
            }
        }
        return sb.toString();
    }

    private static double[] createChunkWithEmptyValues(int size){
    	double[] chunk = new double[size];
    	for(int i = 0; i < size; i ++){
    		chunk[i] = DoubleStore.EMPTY;
    	}
    	return chunk;
    }
}

