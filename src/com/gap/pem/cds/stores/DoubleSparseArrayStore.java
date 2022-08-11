
package com.gap.pem.cds.stores;


import com.gap.pem.cds.CdsException;

/**
 * <p>Stores double arrays representing a contiguous range of time series values for a subset of a time horizon.
 * For example, if the time window has 208 weeks, of which 156 are history and 52 are forecast, this class can store a
 * forecast measure of only the 52 values, but still allow them to be treated as a time series of length 208 that
 * conforms to the entire time horizon.  For each item, there is an offset into the time horizon (default=0) and an array
 * of data values, which can be shorter than the total time horizon.
 * Compare to DoubleArrayStore, which is slightly more efficient if all items in the store are fully populated with conforming
 * time series.
 *</p>
 *
 * <p>The time horizon, <code>maxTimeSeriesLength</code>, grows as data elements and offsets are added to the store.</p>
 *
 */
public class DoubleSparseArrayStore extends DoubleArrayStore {

    private static final long serialVersionUID = -1029285150130897223L;

    /**
     * Value used to fill leading or trailing empty slots in the time series
     */
    double doubleMissingValue = 0.0;   //

    public double getMissingValue() {
        return doubleMissingValue;
    }

    public void setDoubleMissingValue( double v ) {
        this.doubleMissingValue = v;
    }

    /**
     * Holds the starting position in the time series for each data array in the super DoubleArrayStore
     */
    IntStore timeSeriesOffsets;

    /**
     * Logical length for the time series in this store.  All time series have the same logical length.
     */
    private int maxTimeSeriesLength = 0;
    public int getMaxTimeSeriesLength() {
        return maxTimeSeriesLength;
    }
    public void setMaxTimeSeriesLength( int length ) {
        maxTimeSeriesLength = length;
    }

    public DoubleSparseArrayStore(int initialChunkCount, int inputChunkSize, int maxTimeSeriesLength, double missingValue ) {
        super( initialChunkCount, inputChunkSize );
        timeSeriesOffsets = new IntStore( initialChunkCount, inputChunkSize );
        this.maxTimeSeriesLength = maxTimeSeriesLength;
        this.doubleMissingValue = missingValue;
    }

    @Override
    public double[] getElement(int index) {
        double[] storedValues = super.getElement(index);
        if ( isEmptyValue( storedValues ))
            return storedValues;
        int offset = timeSeriesOffsets.getElement(index);
        double[] result = new double[maxTimeSeriesLength];
        int i = 0;
        for( ; i<offset; i++ ) {
            result[i] = doubleMissingValue;
        }

        for( int j = 0; j<storedValues.length; i++, j++ ) {
            result[i] = storedValues[j];
        }

        for( ; i<maxTimeSeriesLength; i++ ) {
            result[i] = doubleMissingValue;
        }

        return result;
    }


    @Override
    public int addElement(double[] values) {
        return addElement( values, 0 );

    }

    @Override
    public double[] setElementAt(int index, double[] values ) {
        return setElementAt( index, values, 0 );
    }


    /**
     * Append an array and its offset in the time series to the store
     * @param values
     * @param offset
     * @return the index of the newly added array.
     */
    public int addElement( double[] values, int offset ) {
        if ( offset < 0 )
            throw new CdsException("DoubleSparseArrayStore.addElement: Offset must be >= 0");


        // Trim any leading or trailing missing values
        int leadingTrim = 0;
        int trailingTrim = 0;
        for( int i=0; i<values.length; i++ ) {
            if ( values[i] != doubleMissingValue )
                break;
            leadingTrim++;
            offset++;
        }
        if ( leadingTrim < values.length ) {
            for( int i=values.length-1; i>=leadingTrim; i--) {
                if ( values[i] != doubleMissingValue )
                    break;
                trailingTrim++;
            }
        }
        double[] trimmedvalues = new double[values.length-(leadingTrim+trailingTrim)];
        for( int i=0; i<trimmedvalues.length; i++ ) {
            trimmedvalues[i] = values[i+leadingTrim];
        }



        int result = super.addElement(trimmedvalues);
        timeSeriesOffsets.setElementAt(result, offset);
        if ( trimmedvalues.length + offset > maxTimeSeriesLength )
            maxTimeSeriesLength = trimmedvalues.length + offset; // stretch the limit
        return result;
    }

    /**
     * Store an array and its offset at an index position in the store
     * @param index
     * @param values
     * @param offset
     * @return the previous array stored internally at index.  Note that this is the internal array with missing
     * values trimmed, and is not adjusted to the logical size of the array.
     */
    public double[] setElementAt( int index, double[] values, int offset ) {

        // Trim any leading or trailing missing values
        int leadingTrim = 0;
        int trailingTrim = 0;
        for( int i=0; i<values.length; i++ ) {
            if ( values[i] != doubleMissingValue )
                break;
            leadingTrim++;
            offset++;
        }
        if ( leadingTrim < values.length ) {
            for( int i=values.length-1; i>=leadingTrim; i--) {
                if ( values[i] != doubleMissingValue )
                    break;
                trailingTrim++;
            }
        }
        double[] trimmedvalues = new double[values.length-(leadingTrim+trailingTrim)];
        for( int i=0; i<trimmedvalues.length; i++ ) {
            trimmedvalues[i] = values[i+leadingTrim];
        }

        double[] result = super.setElementAt( index, trimmedvalues ); // or should we return the stretched array?  Is this ever used?
        timeSeriesOffsets.setElementAt( index, offset );
        if ( trimmedvalues.length + offset > maxTimeSeriesLength )
            maxTimeSeriesLength = values.length + offset; // stretch the limit
        return result;
    }

    /**
     * Return the value at a specific offset in the time series.  Missing value will be returned if the
     * offset is outside the data array.
     * @param index
     * @param timeSeriesOffset
     * @return The value at position timeSeriesOffset in the array at index in the store, or the missing value
     * if the no data is stored at this index or timeSeriesOutset is outside the logical data range of the
     * sparse data store.
     */
    public double getElementValueAt( int index, int timeSeriesOffset ) {
        int currentOffset = timeSeriesOffsets.getElement(index);
        if ( timeSeriesOffset < currentOffset )
            return doubleMissingValue;
        double[] currentValues = super.getElement(index);
        if ( timeSeriesOffset <  currentOffset + currentValues.length )
            return currentValues[timeSeriesOffset-currentOffset];
        else
            return doubleMissingValue;
    }

    /**
     * Set a value at a specified offset in a time series.  The internal array will be lengthened
     * if necessary to be able to hold a value at this offset.
     * @param index
     * @param timeSeriesOffset
     * @param value
     */
    public void setElementValueAt( int index, int timeSeriesOffset, double value ) {
        if ( timeSeriesOffset < 0 )
            throw new IndexOutOfBoundsException();
        int currentOffset = timeSeriesOffsets.getElement(index);
        double[] currentValues = super.getElement(index);
        if ( timeSeriesOffset < currentOffset ) {
            // Prepend empty values
            int prepend = currentOffset - timeSeriesOffset;
            double[] newValues = new double[currentValues.length + prepend];
            int inew = 0;
            for( ; inew < prepend; inew++ )
                newValues[inew] = doubleMissingValue;
            for( int iold = 0; iold < currentValues.length; iold++, inew++ ) {
                newValues[inew] = currentValues[iold];
            }
            super.setElementAt( index, newValues );
            this.timeSeriesOffsets.setElementAt( index, timeSeriesOffset );
            currentOffset = timeSeriesOffset;
            currentValues = super.getElement( index );
        }
        else if ( timeSeriesOffset >= currentOffset + currentValues.length ) {
            // Append empty values
            int append = timeSeriesOffset - ( currentOffset+currentValues.length-1);
            double[] newValues = new double[ currentValues.length + append ];
            int inew = 0;
            for( ; inew < currentValues.length; inew++ ) {
                newValues[inew] = currentValues[inew];
            }
            for( ; inew < newValues.length; inew++ ) {
                newValues[inew] =  doubleMissingValue;
            }
            super.setElementAt( index, newValues );
            currentValues = newValues;
            if ( currentOffset + currentValues.length > maxTimeSeriesLength )
                maxTimeSeriesLength = currentOffset + newValues.length;
        }

        currentValues[ timeSeriesOffset-currentOffset ] = value;

    }


    @Override
    public long getDataSize() {
        return super.getDataSize() + timeSeriesOffsets.getDataSize();
    }


    /**
     * Return a mutable reference to the internal array in the store.  Caller is responsible
     * for synchronization and knowing the offset.
     * @param index
     * @return   The double[] stored at the given index, or null if none is defined.
     */
    public double[] refBaseArray(int index) {
        return super.getElement(index);
    }


    /**
     * Add an array of values to the values already in the store.  Missing values are treated as zero,
     * so adding a 5.0 to a doubleMissingValue will place 5.0 in the store.  Similarly, if the new value is
     * doubleMissingValue, the store's current value is left unchanged.
     * <p>
     *     The internal array is extended as necessary to hold the array of new values at the specified
     *     offset. Maximum time series length for this store will be increated as needed to hold the
     *     appended values.
     *
     * </p>
     * @param index  The index of the time series in the store
     * @param timeSeriesOffset  The offset into the time series where the first new value is added.
     * @param values The values to be summed into the data, if any, already present for the index.
     */
    public void addValuesToElement( int index, int timeSeriesOffset, double[] values ) {
        // Quick and dirty implementation - come back and optimize this later
        for( int i=0; i<values.length; i++ ) {
            double newvalue = values[i];
            if ( newvalue == doubleMissingValue )
                continue;
            double current = getElementValueAt( index, timeSeriesOffset+i );
            if ( current != this.doubleMissingValue )
                newvalue += current;
            setElementValueAt( index, timeSeriesOffset+i, newvalue );
        }
    }

}
