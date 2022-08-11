
package com.gap.pem.cds.filters;

import com.gap.pem.cds.IAttributeContainer;

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
