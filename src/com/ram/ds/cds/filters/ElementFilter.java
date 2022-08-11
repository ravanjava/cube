
package com.ram.ds.cds.filters;

import com.ram.ds.cds.IAttributeContainer;

/**
 * A filter that will match an element in an attribute container.
 * 
 */
public class ElementFilter extends Filter 
{
	private int elementIndex; 
	
	public ElementFilter(IAttributeContainer attributeContainer, int elementIndex){
		super(attributeContainer);
		this.elementIndex = elementIndex;
	}

	@Override
	public boolean isMatch(int index) {
		return (elementIndex == index);
	}
}
