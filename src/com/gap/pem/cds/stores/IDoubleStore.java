package com.gap.pem.cds.stores;

public interface IDoubleStore extends IDataStore{

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
    double getElement(int index);

    /**
     * Check if the given value is indicating a non-existent value in
     * the store. This is typically used to validate the value returned
     * by {@link #getElement(int)}.
     * 
     * @param value a double value
     * @return true or false
     */
    boolean isEmptyValue(double value);
    
    /**
     * Get a value that be used to indicates the non-existence of
     * a real value in the store.
     *
     * @return a value that indicates empty.
     */
    double getEmptyValue();
    
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
    int addElement(double value);

    /**
     * Set the element at the given index to be the given value, and get
     * back the previous value.<p>
     * <ol>
     * <li>If the given index is the same as the current size of the store. 
     * this method behaves as {@link #addElement(double)}, except that it
     * will be returning the empty value as the previous value.</li> 
     * <li>If the given index is beyond the current size of the store, 
     * the gap will be filled by the empty value before the given value is 
     * added at the end to the given index. The returned value will be the
     * empty value.</li>
     * <li>If there is a value previously at the given index, it will be 
     * replaced by the given value, even if the new value is the empty
     * value. The previous value will be returned in this case.</li>
     * </ol>
     * @param index a non-negative integer less than Integer.MAX_VALUE.
     * @param value a double value, could be the empty value.
     * @return the previous value at the given index.
     */
    double setElementAt(int index, double value);
}

