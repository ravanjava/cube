package com.ram.ds.cds.filters;

import com.ram.ds.cds.IAttributeContainer;

/**
 */
public abstract class Filter implements IFilter {

    private IAttributeContainer attributeContainer;

    public Filter (IAttributeContainer iAttributeContainer) {
        attributeContainer = iAttributeContainer;
    }
    
    @Override
    public IAttributeContainer getAttributeContainer() {
        return attributeContainer;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "attributeContainer=" + attributeContainer +
                '}';
    }
}

