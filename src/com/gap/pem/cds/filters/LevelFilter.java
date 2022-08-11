package com.gap.pem.cds.filters;

import java.util.BitSet;

import com.gap.pem.cds.IAttributeContainer;

public class LevelFilter extends Filter {

    private BitSet selectedBits;

    public LevelFilter(IAttributeContainer iAttributeContainer, 
                       BitSet iBitSet)
    {
        super(iAttributeContainer);
        selectedBits = iBitSet;
    }

    @Override
    public boolean isMatch(int iIndex) {
        return (selectedBits.get(iIndex));
    }

    public BitSet getSelectedBits() {
        return selectedBits;
    }

    @Override
    public String toString() {
        return "LevelFilter{" +
                "selectedBits=" + selectedBits +
                "} " + super.toString();
    }
}

