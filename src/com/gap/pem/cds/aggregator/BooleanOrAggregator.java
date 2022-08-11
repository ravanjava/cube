package com.gap.pem.cds.aggregator;

import com.gap.pem.cds.stores.IBooleanStore;


/**
 * Computes the logical OR of a succession of booleans.  Result will be true if any of the values
 * are true or false if none are true or the aggregator has never been applied.
 */
public class BooleanOrAggregator implements Aggregator {
    IBooleanStore source;
    boolean result;
    public BooleanOrAggregator( IBooleanStore source ) {
        this.source = source;
        reset();
    }

    public void reset() {
        result = false;  // Identify value for OR
    }

    public boolean getResult() { return result; }

    public void accumulate( int i ) {
        result = result || source.getElement(i);
    }
}

