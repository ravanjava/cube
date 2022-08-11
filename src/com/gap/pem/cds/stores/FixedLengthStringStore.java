package com.gap.pem.cds.stores;

import java.util.Arrays;

/**
 * Holds an ordered collection of Strings.  Internally the data is a simple String[].
 */
public class FixedLengthStringStore implements IStringStore
{
    private static final long serialVersionUID = -264251659754551899L;

    private static final String EMPTY = null;
    
    private String[] data;
    int validIndex = -1;

    public FixedLengthStringStore(int maximumSize)
    {
        data = new String[maximumSize];
    }

	/**
	 * Get the value at the given index. If the index is less than the 
	 * current size of the store, a value will be returned, even if it 
	 * doesn't exist. Use the {@link #isEmptyValue(String)} method to check 
	 * if the returned value is an indication that the value doesn't 
	 * exist. 
	 * 
	 * @param index non-negative integer less than the size of the store.
	 * @return a value, could be the empty value.
	 * @throws ArrayIndexOutOfBoundsException if the index is equal to or
	 * greater than the current size of the store.
	 */
	@Override
    public String getElement(int index)
    {
        if (index > validIndex)
        {
            throw new ArrayIndexOutOfBoundsException("Index is beyond "
                      + "the current valid position: " + index);
        }

        return data[index];
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
    public boolean isEmptyValue(String value){
    	return (value == FixedLengthStringStore.EMPTY);
    }

    /**
     * Get a value that be used to indicates the non-existence of
     * a real value in the store.
     *
     * @return a value that indicates empty.
     */
    @Override
    public String getEmptyValue(){
    	return FixedLengthStringStore.EMPTY;
    }

    /**
     * Add the given value to the data store and place it at the end
     * of existing values. The size of the store will increase by 
     * one.
     * 
     * @param value the value to add to the end of the store
     * @return the index at which the values were placed in the store.
     * @throws IllegalStateException if the store has already reached
     * its maximum capacity, which is the length the store is fixed
     * to have.
     */
	@Override
    public int addElement(String value)
    {
		int nextIndex = validIndex + 1;
        if (nextIndex >= data.length)
        {
             throw new IllegalArgumentException("Maximum limit " +
            		 " of store reached. Cannot Add Further");
        }
        data[nextIndex] = value;
        ++validIndex;
        return validIndex;
	}

    /**
     * Set the element at the given index to be the given value, and get
     * back the previous value.<p>
     * <ol>
     * <li>If the given index is the same as the current size of the store. 
     * this method behaves as {@link #addElement(String)}, except that it
     * will be returning the empty value as the previous value.</li> 
     * <li>If the given index is beyond the current size of the store, 
     * the gap will be filled by the empty value before the given value 
     * is added at the end to the given index. The returned value will 
     * be the empty value.</li>
     * <li>If there is a value previously at the given index, it will be 
     * replaced by the given value, even if the new value is the empty
     * value. The previous value will be returned in this case.</li>
     * </ol>
     * @param index a non-negative integer less than the maximum size.
     * @param value an String value, could be the empty value.
     * @return the previous value at the given index, if it existed.
     */
	@Override
    public String setElementAt(int index, String value){
    	if(index >= data.length){
            throw new IllegalArgumentException("Maximum limit of " +
            	"store reached. Cannot set element at " + index);
    	}
    	
    	// fill the gap with empty values if necessary.
    	if(index > validIndex){
    		// Use less or equal to make sure there is an empty
    		// value at the intended index.
    		for(int i = validIndex; i <= index; i ++){
    			data[i] = FixedLengthStringStore.EMPTY;
    		}
    		validIndex = index;
    	}
    	
        String oldValue = data[index];
        data[index] = value;
        return oldValue;
    }

    @Override
    public int size() {
        return validIndex + 1;
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
     * @throws IllegalArgumentException if the given size is greater
     * than the maximum size this store is fixed to have.
     */
    @Override
    public int ensureSize(int minimumSize){
    	if(minimumSize > data.length){
    		throw new IllegalArgumentException("Can not set minmum size " +
    				" to be greater than the maximum size.");
    	}
    	
    	int index = minimumSize-1;
    	if(index > validIndex){
    		// Use less or equal to make sure there is an empty
    		// value at the intended index.
    		for(int i = validIndex; i <= index; i ++){
    			data[i] = FixedLengthStringStore.EMPTY;
    		}
			validIndex = index;
    	}
    	
    	return validIndex + 1;
    }


    @Override
    public String toString() {
        return "FixedLengthStringStore{" +
                "data=" + (data == null ? null : Arrays.asList(data)) +
                ", validIndex=" + validIndex +
                '}';
    }

    @Override
    public long getDataSize() {
        long size = 0;
        for( String s : this.data ) {
            if ( s != null ) {
                size += s.length()*2;
            }
        }
        return size;
    }
}

