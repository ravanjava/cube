package com.gap.pem.cds.filters;

import com.gap.pem.cds.IAttributeContainer;

/**
 */
public interface IFilter {

    /**
     * Gets the object which holds the data stores for the hierarchy level or intersection on which this
     * filter should be applied.
     * @return Object holding data stores on which filter applies.
     */
    IAttributeContainer getAttributeContainer();

    /**
     * Is the attribute at the specified index a positive match for this filter or not?
     * @param index
     * @return true if the attribute at index matches this filter
     */
    boolean isMatch(int index);

}


