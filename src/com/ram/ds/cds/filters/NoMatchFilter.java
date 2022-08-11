
package com.ram.ds.cds.filters;

import com.ram.ds.cds.IAttributeContainer;

/**
 * A special filter that does not match any of the elements in an attribute 
 * container.
 */
public class NoMatchFilter extends Filter
{
    public NoMatchFilter (IAttributeContainer iAttributeContainer) {
    	super(iAttributeContainer);
    }
    
    /**
     * @return always false
     */
	@Override
	public boolean isMatch(int index) {
		return false;
	}
}




