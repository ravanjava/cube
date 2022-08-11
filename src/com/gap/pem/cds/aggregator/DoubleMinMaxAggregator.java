package com.gap.pem.cds.aggregator;

import com.gap.pem.cds.stores.IDoubleStore;

/**
 * Aggregates the minimum and maximum of a succession of doubles.  Minimum and maximum are
 * Negative infinity if no values have been aggregated
 */
public class DoubleMinMaxAggregator implements Aggregator {
    double minimum;
    double maximum;
    double missingValue;
    public double[] getResult() { return new double[]{minimum,maximum}; }
    public double getMinimum() { return minimum; }
    public double getMaximum() { return maximum; }

    IDoubleStore source;

    public DoubleMinMaxAggregator(IDoubleStore store) {
        this( store,Double.NEGATIVE_INFINITY);
    }

    public DoubleMinMaxAggregator(IDoubleStore store, double missingValue ) {
        source = store;
        this.missingValue = missingValue;
        reset();
    }

    public void reset() {
        minimum = missingValue;
        maximum = missingValue;
    }

    public void accumulate( int i ) {
        double v = source.getElement(i);
        if ( minimum == missingValue ) {
            minimum = v;
            maximum = v;
        }
        else {
            if ( v < minimum )
                minimum = v;
            if ( v > maximum )
                maximum = v;
        }
    }

    public String toString() { return "DoubleMinMaxAggregator minimum=" +minimum + " maximum=" + maximum; }
}
