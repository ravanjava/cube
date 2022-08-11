package com.gap.pem.cds.filters;

import java.util.Arrays;

import com.gap.pem.cds.IAttributeContainer;

/**
 */
public class DataDomainFilter extends Filter {

    int[] selectedValues;
    int currentIndex = 0;

    public DataDomainFilter(IAttributeContainer iContainer,
                            int numberOfSelectedValues) {
    	super (iContainer);
        selectedValues = new int[numberOfSelectedValues];
    }

    public void setSelectedValue(int selectedValue) {
        selectedValues[currentIndex] = selectedValue;
        currentIndex++;
    }

    @Override
    public boolean isMatch(int iIndex) {
        throw new RuntimeException("DataDomainFilter.isMatch() is not yet implemented");
    }

    @Override
    public String toString() {
        return "DataDomainFilter{" +
                "selectedValues=" + Arrays.toString(selectedValues) +
                ", currentIndex=" + currentIndex +
                "} " + super.toString();
    }
}
