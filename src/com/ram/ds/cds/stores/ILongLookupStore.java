package com.ram.ds.cds.stores;

public interface ILongLookupStore extends ILongStore 
{
    /**
     * Gets the key for the passed in long value
     *
     * @param value
     * @return the key for the value or -1 if not found
     */
    int getKeyForValidValue(long value);

}

