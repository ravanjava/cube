package com.ram.ds.cds.filters;

import java.util.BitSet;

import com.ram.ds.cds.IAttributeContainer;
import com.ram.ds.cds.stores.IIntStore;

/**
 * A filter that matches an integer store (typically a store holding the
 * indices into another attribute store) by a bit set. It will not match
 * an index where the integer is an empty value, which is identified by 
 * the IIntStore.isEmptyValue() call.
 * 
 */
public class BitSetFilter extends Filter {

    private BitSet selectedBits;
    private IIntStore correspondingIDStore;

    public BitSetFilter(IAttributeContainer iAttributeContainer,
                        String iTargetAttributeName,
                        int iCapacity) {
        super(iAttributeContainer);
        correspondingIDStore = iAttributeContainer.getIntAttribute(iTargetAttributeName);
        selectedBits = new BitSet(iCapacity);
    }

    public BitSetFilter(IAttributeContainer iAttributeContainer,
                        String iTargetAttributeName,
                        BitSet iBitSet)
    {
        super(iAttributeContainer);
        correspondingIDStore = iAttributeContainer.getIntAttribute(iTargetAttributeName);
        selectedBits = iBitSet;
    }

    @Override
    public boolean isMatch(int iIndex) {
    	int id = correspondingIDStore.getElement(iIndex);
    	if(correspondingIDStore.isEmptyValue(id)){
    		return false;
    	}
        return (selectedBits.get(id));
    }

    public void setBit(int bitNum) {
        selectedBits.set(bitNum);
    }

    @Override
    public String toString() {
        return "BitSetFilter{" +
                "selectedBits=" + selectedBits +
                ", correspondingIDStore=" + correspondingIDStore + ", " + super.toString() +
                '}';
    }
}

