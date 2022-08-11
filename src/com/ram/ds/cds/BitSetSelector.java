package com.ram.ds.cds;

import java.util.BitSet;

import com.ram.ds.cds.aggregator.Aggregator;

/**
 * Aggregates into a BitSet by setting boolean values at index positions.
 *
 */
public class BitSetSelector implements Aggregator
{
    private BitSet bitSet;
    private String containerName;

    /**
     * Create a new selector.
     * @param iAttributeContainerName    The name of the level or intersection from which  the
     *                                   selector was created.  The bit set held by this selector will
     *                                   be the length of the number of members in the container specified
     *                                   by iAttributeContainerName.
     */
    public BitSetSelector(String iAttributeContainerName)
    {
        containerName = iAttributeContainerName;
        bitSet = new BitSet();
    }

    /**
     *
     * @param iAttributeContainerName   Name of the intersection
     * @param iBitSet   BitSet into which boolean values are aggregated.
     */
    public BitSetSelector(String iAttributeContainerName, BitSet iBitSet)
    {
        containerName = iAttributeContainerName;
        bitSet = iBitSet;
    }

    public BitSet getBitSet()
    {
        return bitSet;
    }

    public void setBit(int iIndex)
    {
        bitSet.set(iIndex);
    }

    @Override
    public void accumulate(int iIndex)
    {
        setBit(iIndex);
    }

    public void setBit(int iIndex, boolean iValue)
    {
        bitSet.set(iIndex, iValue);
    }

    @Override
    public String toString() {
        return "BitSetSelector{" +
                "bitSet=" + bitSet +
                ", containerName='" + containerName + '\'' +
                '}';
    }
}
