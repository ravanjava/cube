package com.gap.pem.cds.aggregator;

import com.gap.pem.cds.stores.IDoubleStore;

 /**
  * Aggregates the sum of a single double value for each item on an intersection
  */
public class DoubleSumAggregator implements Aggregator {
    double result;
    public double getResult() { return result; }
    IDoubleStore source;
    public DoubleSumAggregator(IDoubleStore store) { this.source = store; this.result = 0.0; }
    public void accumulate( int i ) {  result += source.getElement(i); }
    public String toString() { return "DoubleSumAggregator result=" + result; }
}
