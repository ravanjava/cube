package com.ram.ds.cds.filters;

import com.ram.ds.cds.IAttributeContainer;
import com.ram.ds.cds.stores.IStringStore;

/**
 */
public class StringFilter extends Filter {

    private IStringStore attrStore;
    private boolean isTargetEmpty;
    private String target;

    public StringFilter(IAttributeContainer iAttributeContainer,
                        String iAttrName,
                        String iTargetString) {
        super(iAttributeContainer);
        attrStore = iAttributeContainer.getStringAttribute(iAttrName);
        target = iTargetString;
        isTargetEmpty = attrStore.isEmptyValue(target);
    }

    @Override
    public boolean isMatch(int iIndex)
    {
    	boolean isMatch = false;
    	
    	String value = attrStore.getElement(iIndex);
    	if(isTargetEmpty){
    		isMatch = attrStore.isEmptyValue(value);
    	} else {
    		if(attrStore.isEmptyValue(value)){
    			isMatch = false;
    		} else {
    			isMatch = value.equals(target);
    		}
    	}
    	
    	return isMatch;
    }

    /**
     * replaces the current target with the new target
     * @param iNewTarget   the new target string
     */
    public void setTarget(String iNewTarget)
    {
        target = iNewTarget;
        isTargetEmpty = attrStore.isEmptyValue(target);
    }

    @Override
    public String toString() {
        return "StringFilter{" +
                "attrStore=" + attrStore +
                ", target='" + target + '\'' +
                "} " + super.toString();
    }
}

