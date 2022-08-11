package com.ram.ds.cds.aggregation;



/**
 * Aggregator whose result can be combined (reduced) with that of another aggregator.
 */
public interface MapReduceAggregator extends CloneableAggregator {

    /**
     * Aggregate the values accumulated in another aggregator into the values accumulated in this aggregator.
     * @param aggregator
     */
    void reduceWith( MapReduceAggregator aggregator );

    /**
     * Create a duplicate of an aggregator.  This is used to create a separate set of aggregators
     * for a thread in a map-reduce process.
     * @see CloneableAggregator
     * @return The cloned aggregator.
     */
    MapReduceAggregator clone();
}
