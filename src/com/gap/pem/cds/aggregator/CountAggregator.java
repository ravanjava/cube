package com.gap.pem.cds.aggregator;

/**
 * An aggregator that simply counts the number of matching indices as the 
 * aggregation method runs through a level or intersection.
 *
 */
public class CountAggregator implements Aggregator
{
    private int result = 0;

    public CountAggregator()
    {
    }

    @Override
    public void accumulate(int i)
    {
        result++;
    }

    public int getResult()
    {
        return result;
    }

    public void clearResult()
    {
        result = 0;
    }

    @Override
    public String toString() {
        return "CountAggregator{" +
                "result=" + result +
                '}';
    }
}

