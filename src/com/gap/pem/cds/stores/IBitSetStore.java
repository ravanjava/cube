package com.gap.pem.cds.stores;

import java.util.BitSet;

public interface IBitSetStore extends IDataStore 
{
	/**
	 * Get the value at the given index. If the index is less than the 
	 * current size of the store, a value will be returned, even if it 
	 * doesn't exist. Use the {@link #isEmptyValue(BitSet)} method to check 
	 * if the returned value is an indication that the value doesn't 
	 * exist. 
	 * 
	 * @param index non-negative integer less than the size of the store.
	 * @return a value, could be the empty value.
	 * @throws ArrayIndexOutOfBoundsException if the index is equal to or
	 * greater than the current size of the store.
	 */
    BitSet getElement(int index);

    /**
     * Check if the given value is indicating a non-existent value in
     * the store. This is typically used to validate the value returned
     * by {@link #getElement(int)}.
     * 
     * @param value a BitSet value
     * @return true or false
     */
    boolean isEmptyValue(BitSet value);
    
    /**
     * Get a value that be used to indicates the non-existence of
     * a real value in the store.
     *
     * @return a value that indicates empty.
     */
    BitSet getEmptyValue();
    
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
    int addElement(BitSet value);

    /**
     * Set the element at the given index to be the given value, and get
     * back the previous value.<p>
     * <ol>
     * <li>If the given index is the same as the current size of the store. 
     * this method behaves as {@link #addElement(BitSet)}, except that it
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
     * @param value a BitSet value, could be the empty value.
     * @return the previous value at the given index.
     */
    BitSet setElementAt(int index, BitSet value);
    
    /**
     * Get the value of the bit with the specified bit index, in the bit 
     * set stored at the given element index.
     * 
     * @param elementIndex index of the bit set to get the value from
     * @param bitIndex index of the bit whose value to retrieve.
     * 
     * @return true if the bit is set, otherwise false.
	 * @throws ArrayIndexOutOfBoundsException if the index is equal to or
	 * larger than the current size of the store.
     */
    boolean getElementBit(int elementIndex, int bitIndex);
	
	
    /**
     * Set the bit with the specified bit index to the given value, in the bit 
     * set stored at the given element index.
     * 
     * @param elementIndex index of the bit set to set the value for
     * @param bitIndex index of the bit whose value to be updated.
     * 
     * @return the previous value of the bit.
     * 
     * @see #setElementAt(int, BitSet)
     */
    boolean setElementBit(int elementIndex, int bitIndex, boolean value);
}


