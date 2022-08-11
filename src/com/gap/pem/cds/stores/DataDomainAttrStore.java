package com.gap.pem.cds.stores;

import java.util.HashMap;
import java.util.Set;

/**
 * This class manages an attribute storage for attributes which have repeated String values (such as color).  It
 * assigns a key to each possible value, and stores the keys for efficiency.
 * <p>
 * This class can consume A LOT of memory if you have a lot of distinct values.  If that is your use-case,
 * you might be better off using StringStore or even String[]
 *
 */
public class DataDomainAttrStore implements IDataDomainStore {

    private static final long serialVersionUID = 7916346830674382740L;
    private static final String EMPTY = null;
    private IntStore keyStore;

    private HashMap<String, Integer> valueToKeyMap;
    private StringStore keyToValueMap;

    private int currentKey;

    public DataDomainAttrStore() {
    	keyStore = new IntStore();
    	valueToKeyMap = new HashMap<String, Integer>();
    	// keyToValueMap is smaller in size, we are not expecting
    	// many valid values.
    	keyToValueMap = new StringStore(8,32);
    	currentKey = 0;
    }

    /**
     * Adds the specified string as a valid value and returns the 
     * value ID specified by this class.
     *
     * @return the key for the valid value
     */
    @Override
	public int addValidValue(String value) {  
    	int valueKey = currentKey;
        valueToKeyMap.put(value, valueKey);
        keyToValueMap.setElementAt(valueKey, value);
        currentKey++;
        return valueKey;
    }

    @Override
	public int getKeyForValidValue(String value) {
        Integer key = valueToKeyMap.get(value);
        if (key == null) 
        	return -1;
		return key;
    }
    
    @Override
    public String getValidValueForKey(int valueKey){
    	String value = keyToValueMap.getElement(valueKey);
    	return value;
    }


    /**
     * Sets the value for a given member.  this method is slow because 
     * it looks up the value for every call.  If you are setting a lot 
     * of values for a lot members, it's probably better to keep the 
     * values locally and call the other setValue method.
     *
     * @param memberID
     * @param value
     */
    @Override
	public void setValue(int memberID, String value) {
        int valueKey = valueToKeyMap.get(value);
        keyStore.setElementAt(memberID, valueKey);
    }

    @Override
    public String getValue(int memberID){
    	int valueKey = keyStore.getElement(memberID);
    	return keyToValueMap.getElement(valueKey);
    }

    @Override
    public String getElement(int index) {
        return getValue( index );
    }

    @Override
    public boolean isEmptyValue(String value) {
        return value.equals( EMPTY );
    }

    @Override
    public String getEmptyValue() {
        return EMPTY;
    }

    @Override
    public int addElement(String value) {
        int valueKey = valueToKeyMap.get(value);
        return keyStore.addElement(valueKey);
    }

    @Override
    public String setElementAt( int position, String value ) {
        int valueKey = valueToKeyMap.get(value);
        int priorValueKey = keyStore.setElementAt( position, valueKey );
        
        String priorValue = null;
        if(keyStore.isEmptyValue(priorValueKey)){
        	priorValue = null;
        } else {
        	priorValue = this.keyToValueMap.getElement(priorValueKey);
        }
        return priorValue;
    }
    
    /**
     * A faster version of the value setter that allows the user to 
     * lookup the key for the values rather than passing in the 
     * string values.
     *
     * @param memberID
     * @param valueKey
     */
    @Override
	public void setValueByKey(int memberID, int valueKey) {
    	keyStore.setElementAt(memberID, valueKey);
    }

    /**
     * Added new method. Not sure we should have getElement instead.
     * @param memberID the ID (ordinal position of the member in the store).
     * @return the value key for the member.
     */
    @Override
	public int getValueKey(int memberID)
    {
        return keyStore.getElement(memberID);
    }

    @Override
	public int size() {
    	return keyStore.size();
    }
    
    @Override
    public int ensureSize(int minimumSize) {
    	return this.keyStore.ensureSize(minimumSize);
    }

    @Override
    public int getCardinality() {
        Set<String> keys = valueToKeyMap.keySet();
        return  keys.size();  // will this be unique or not??
    }

    /**
     * Enumerate the valid values in key order, such the the key for the first item of the result is 0, etc.
     * This assumes that the keys in the hash map are a dense set from 0..n-1.
     * @return the array of valid String values currently defined for the store.
     */
    public String[] getValidValues() {
        String[] validValues = valueToKeyMap.keySet().toArray( new String[ valueToKeyMap.keySet().size() ] );
        String[] result = new String[ validValues.length ];
        for( String value : validValues ) {
            result[ valueToKeyMap.get(value)] = value;
        }
        return result;
    }



    @Override
    public String toString() {
        return "DataDomainAttrStore{" +
                "storage=" + keyStore +
                ", currentKey=" + currentKey +
                ", valueToKeyMap=" + valueToKeyMap +
                '}';
    }

    @Override
    public long getDataSize() {
        long size = keyStore.getDataSize();
        // add sizes of the String keys?
        return size;
    }
}

