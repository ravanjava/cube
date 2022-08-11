package com.gap.pem.cds.aggregation;

import com.gap.pem.cds.aggregator.Aggregator;

/**
 * Interface adding cloneability to aggregators for creating multi-thread duplicates.
 */
public interface CloneableAggregator extends Aggregator {

    /**
     * Implementation:  Create a distinct copy of the aggregator.  The copy should use the same
     * input sources as the instance being copied, but accumulate into separate private data storage.
     * Cloned aggregators are used when an aggregation is partitioned into logical segments, for example
     * for thread-parallel aggregation. Using separate aggregator instances allows race conditions to be
     * avoided altogether without using locks.
     * @return  Identical copy of the aggregator with different result storage locations.
     */
    CloneableAggregator clone();
}
