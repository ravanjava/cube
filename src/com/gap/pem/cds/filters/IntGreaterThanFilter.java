package com.gap.pem.cds.filters;

import com.gap.pem.cds.IAttributeContainer;

/**
 * Tests if a specified index is greater than a specified value.
 */
public class IntGreaterThanFilter extends Filter {

    int value;

    public IntGreaterThanFilter(IAttributeContainer container, int value) {
        super(container);
        this.value = value;
    }

    /**
     * Is the attribute at the specified index a positive match for this filter or not?
     *
     * @param index
     * @return true if index is greater than the value.
     */
    @Override
    public boolean isMatch  (int index) {
        return index > value;
    }

    @Override
    public String toString() {
        return "IntGreaterThanFilter{" +
                "value=" + value +
                "} " + super.toString();
    }
}

