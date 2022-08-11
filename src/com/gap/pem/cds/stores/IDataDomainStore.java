package com.gap.pem.cds.stores;

/**
 * Holds an ordered, growable collection of Strings, each of which belongs to a set of possible valid values.
 */
public interface IDataDomainStore extends IStringStore {    // was based on IDataStore; changed to IStringStore

	String getValue(int memberID);
	
	void setValue(int memberID, String value);
	
	int getValueKey(int memberID);

	void setValueByKey(int memberID, int valueKey);


	/**
	 * Gets the key for the passed in value
	 * 
	 * @param value
	 * @return the key for the value or -1 if not found
	 */
    int getKeyForValidValue(String value);

    /**
     *
     * @param valueKey
     * @return the valid String associated with the key
     */
    String getValidValueForKey(int valueKey);

    /**
     * Add a String value to the list of possible valid values that can be stored.
     * @param value
     * @return the value key for the new valid value
     */
	int addValidValue(String value);

    /**
     * @return the count of distinct valid values
     */
    int getCardinality();


    /**
     * Returns the list of valid String values
     * @return array of valid values.
     */
    String[] getValidValues();
}

