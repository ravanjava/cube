package com.gap.pem.cds;

import com.gap.pem.cds.stores.IStringStore;

/**
 * Contains the information on a level of a member in a hierarchy, including whether or not it
 * has any children in the next level down.
 */
public class LevelMemberInfo extends LevelMember
{
	private static final long serialVersionUID = 8845261856724744190L;

	protected IStringStore levelNameStore;
    protected boolean hasChildren;

    public LevelMemberInfo(HierarchyLevel level, int memberIndex, boolean hasChildren) {
        super(level,memberIndex);
        levelNameStore = level.getStringAttribute(level.getIdentityAttributeName());
        this.hasChildren = hasChildren;
    }

    public String getMemberName() {
        return levelNameStore.getElement(memberIndex);
    }

    public boolean getHasChildren() {
        return hasChildren;
    }
    
    @Override
    public boolean equals(Object object){
    	if(this == object){
    		return true;
    	}
    	
    	if(super.equals(object) == false){
    		return false;
    	}
    	
    	if((object instanceof LevelMemberInfo) == false){
    		return false;
    	}
    	
    	LevelMemberInfo other = (LevelMemberInfo)object;
    	if(this.hasChildren != other.hasChildren){
    		return false;
    	}
    	
    	// Comparing the content of the data stores will be 
    	// too expensive. We don't need to do it here 
    	// because as long as the two hierarchy levels have
    	// been compared to be equal in the super class,
    	// their identity attribute stores will be equal.
    	
    	return true;
    }
    
    @Override
    public int hashCode(){
    	int hashCode = super.hashCode();
    	hashCode += (this.hasChildren ? 1 : 0);
    	return hashCode;
    }
}

