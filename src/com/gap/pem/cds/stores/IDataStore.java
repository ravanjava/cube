package com.gap.pem.cds.stores;

import java.io.Serializable;

/**
 * This interface is a placeholder and is the base interface for all DataStore's.  A DataStore is a growable
 * data container which can store a specific primitive (or array of primitives).  Derived interfaces specify
 * the type of primitive.  Generally, a DataStore can be thought of as a growable array for large data sets.
 *
 */
public interface IDataStore extends Serializable {
	
	/**
	 * Return the current number of elements in the store. 
	 * 
	 * @return a non-negative integer.
	 */
    int size();

    /**
     * Grow the size of the store, if necessary, to ensure that it holds
     * at least the given number of elements.
     * <p>
     * If the store size is grown due to this call, the extra space  
     * may be occupied by empty values.
     * <p>
     * Note that the returned size is not always equal to the desired 
     * size. In the case when the desired size is less than the current
     * size of the store, the current size should be returned.
     * 
     * @param minimumSize the desired minimum size
     * @return the updated size of the store. 
     */
    int ensureSize(int minimumSize);


    /**
     * Return the total size in bytes of the data values contained in the store.  Implementation
     * may be estimated or approximate.
     * @return Total size.
     */
    long getDataSize();
}

