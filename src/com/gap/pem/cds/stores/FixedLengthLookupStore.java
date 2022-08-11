package com.gap.pem.cds.stores;

/**
 * Base class for lookup stores where the maximum number of keys is known in
 * advance.
 */
public abstract class FixedLengthLookupStore extends GenericStore 
{
    private static final long serialVersionUID = 7173220155193740980L;
    protected FixedLengthIntStore keyStore;
    protected int currentID = 0;

    /**
     * Construct with known limit on number of keys
     * @param maximumSize The maximum number of keys
     */
    public FixedLengthLookupStore(int maximumSize)
    {
        validIndex = -1;
        keyStore = new FixedLengthIntStore(maximumSize);
    }

    @Override
    public String toString() {
        return "FixedLengthLookupStore{" +
                "keyStore=" + keyStore +
                ", currentID=" + currentID +
                '}';
    }
}

