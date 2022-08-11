package com.ram.ds.cds.aggregator;

import com.ram.ds.cds.util.BitVector;

/**
 * An aggregator that collects the matched indices as the aggregation method 
 * runs through a level or intersection.
 */
public class ShadowAggregator implements Aggregator
{
	private BitVector shadow;
	
	/**
	 * @param size size of the level or intersection
	 */
	public ShadowAggregator(int size){
		this.shadow = new BitVector(size);
		// all bits are false by default
	}

	@Override
	public void accumulate(int index) {
		// turn the bit to true for a matched index
		this.shadow.set(index, true);
	}
	
	/**
	 * @return a bit vector where the bits are set at 
	 * matches indices.
	 */
	public BitVector getShadow(){
		return this.shadow;
	}
}

