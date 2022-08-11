package com.ram.ds.cds.aggregator;

import com.ram.ds.cds.stores.IBooleanStore;


/**
 * Computes the logical AND of a succession of booleans.  Result will be true if all the values
 * are true or the aggregator has never been applied.
 */
public class BooleanAndAggregator implements Aggregator {
    IBooleanStore source;
    boolean result;
    public BooleanAndAggregator( IBooleanStore source ) {
        this.source = source;
        reset();
    }

    public void reset() {
        result = true;  // Identify value for AND
    }

    public boolean getResult() { return result; }

    public void accumulate( int i ) {
        result = result && source.getElement(i);
    }
}
