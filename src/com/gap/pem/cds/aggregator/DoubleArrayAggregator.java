package com.gap.pem.cds.aggregator;

import com.gap.pem.cds.stores.IDoubleArrayStore;

/**
 * Factored into separate class for use in unit tests
 */
public class DoubleArrayAggregator implements Aggregator  {
    double[] result;
    IDoubleArrayStore store;

    public DoubleArrayAggregator( IDoubleArrayStore store, int resultSize ) {
        this.store = store;
        result = new double[resultSize];
    }

    public void accumulate( int posn ) {
        double[] storeValues = store.getElement( posn );
        for( int i=0; i<storeValues.length; i++ ) {
            result[i] += storeValues[i];
        }
    }

    public double[] getResult() {
        return result;
    }
}

