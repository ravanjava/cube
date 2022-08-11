package com.ram.ds.cds.aggregator;

/**
 * This interface is called by the com.jda.mdap.Collector when an aggregation is performed.  It allows a user of the collector
 * to determine how to accumulate the data.  An implementing class may accumulate more than one measure or may accumulate
 * only a single measure.  That decision is left up to the user.  The purpose of this class is to allow the collector
 * to efficiently iterate over the selected objects and still allow for custom aggregations.
 *
 */
public interface Aggregator {

    /**
     * Apply the aggregation method to position i as defined in the implementing class.
     * @param index The position in the source container of the value to be accumulated.
     */
    void accumulate(int index);
}