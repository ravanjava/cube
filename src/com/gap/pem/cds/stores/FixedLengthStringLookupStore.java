package com.gap.pem.cds.stores;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

/**
 */

public class FixedLengthStringLookupStore extends FixedLengthLookupStore implements IStringLookupStore
{
    private static final long serialVersionUID = -3093767221908973607L;
    
    private static final String EMPTY = null;
    
    private TObjectIntMap<String> valueToKeyMap = new TObjectIntHashMap<String>(
    		gnu.trove.impl.Constants.DEFAULT_CAPACITY,
    		gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR,
    		-1);
    
    //Initialize 1st element to value " ". The index position in the array would
    // be used as the key for the stored value.
    private String[] keyToValueArray = new String[]{" "};

    public FixedLengthStringLookupStore(int maximumSize)
    {
		super(maximumSize);
    }

    @Override
    public List<String> getValidValues()
    {
        List<String> validValues = new ArrayList<String>();
        validValues.addAll(Arrays.asList(keyToValueArray));
        return validValues;
    }

    @Override
    public int getKeyForValidValue(String value) {
        return valueToKeyMap.get(value);
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
        int idAtIndex = keyStore.getElement(index);
        if(keyStore.isEmptyValue(idAtIndex)){
        	// the element doesn't exist
        	return FixedLengthStringLookupStore.EMPTY;
        }
        
        return keyToValueArray[idAtIndex];
    }

    /**
     * Check if the given value is indicating a non-existent value in
     * the store. This is typically used to validate the value returned
     * by {@link #getElement(int)}.
     * 
     * @param value a String value
     * @return true or false
     */
	@Override
    public boolean isEmptyValue(String value){
		return value == FixedLengthStringLookupStore.EMPTY;
    }

    /**
     * Get a value that be used to indicates the non-existence of
     * a real value in the store.
     *
     * @return a value that indicates empty.
     */
    @Override
    public String getEmptyValue(){
    	return FixedLengthStringLookupStore.EMPTY;
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
        int keyForValue = getKeyForValue(value);
        int retVal = keyStore.addElement(keyForValue);
        ++validIndex;
        return retVal;
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
     * @param value a long value, could be the empty value.
     * @return the previous value at the given index, if it existed.
     */
	@Override
    public String setElementAt(int index, String value)
    {
		int prevSize = keyStore.size();
		
        int keyForValue = getKeyForValue(value);
        int oldKey = keyStore.setElementAt(index, keyForValue);
        int currSize = keyStore.size();
        if(currSize > prevSize){
        	// the keystore has grown in size.
            validIndex = currSize - 1;
        }
        return keyToValueArray[oldKey];
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
    	int prevSize = keyStore.size();
    	keyStore.ensureSize(minimumSize);
    	int currSize = keyStore.size();
        if(currSize > prevSize){
        	// the keystore has grown in size.
            validIndex = currSize - 1;
    	}
    	return currSize;
    }

    /**
     * Returns existing key from map. Creates a new key for the value if value is not found on the map. Updates ketToValueMap also.
     * @param value
     * @return the key for the value.
     */
    private int getKeyForValue(String value) {
        int indexForValue = valueToKeyMap.get(value);
        if(indexForValue == -1)
        {
            valueToKeyMap.put(value, currentID);
            indexForValue = currentID++;
            //Grow keyToValueArray array if necessary.
            if(indexForValue >= keyToValueArray.length)
            {
                //TODO: Is it okay to use Constants.INITIAL_CHNK_CNT ?
                String[] newKeyToValueArray = new String[keyToValueArray.length + Constants.INITIAL_CHNK_CNT];
                System.arraycopy(keyToValueArray, 0, newKeyToValueArray, 0, keyToValueArray.length);
                keyToValueArray = newKeyToValueArray;
            }
            keyToValueArray[indexForValue] = value;
        }
        return indexForValue;
    }

    @Override
    public String toString() {
        return "FixedLengthStringLookupStore{" +
                "valueToKeyMap=" + valueToKeyMap +
                ", keyToValueArray=" + (keyToValueArray == null ? null : Arrays.asList(keyToValueArray)) +
                '}';
    }
}

