package com.gap.pem.cds.stores;

import java.util.List;

/**
 */
public interface IStringLookupStore extends IStringStore
{
    /**
     * Gets the key for the passed in string value
     * @param value
     * @return the key for the value or -1 if not found
     */
    int getKeyForValidValue(String value);

    /**
     * Gets the list of valid value Strings
     * @return List&lt;String&gt; containing the current list of valid Strings in the store.
     */
    List<String> getValidValues();
}

