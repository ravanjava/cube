package com.ram.ds.cds;

import java.io.Serializable;

/**
 * Identifies a single member of a HierarchyLevel
 */
public class LevelMember implements Serializable {

    private static final long serialVersionUID = 674958501912716157L;
    
    protected HierarchyLevel level;
    protected int memberIndex;
    
    public LevelMember(CubeDs collector, String dimensionName, String levelName, int memberIndex) {
        this(collector.getDimension(dimensionName).getLevel(levelName), memberIndex );
    }

    public LevelMember(Dimension dimension, String levelName, int memberIndex) {
        this(dimension.getLevel(levelName), memberIndex );
    }

    public LevelMember(HierarchyLevel level, int memberIndex) {
        if (memberIndex <0 || memberIndex >= level.getMemberCount()) {
            throw new CdsException("LevelMember():  memberId is not a valid member of the specified level");
        }
        this.level = level;
        this.memberIndex = memberIndex;
    }
    
    public HierarchyLevel getLevel() {
        return level;
    }

    // FIXME rename this to getMemberIndex to be accurate
    public int getMemberId() {
        return memberIndex;
    }
    
    @Override
    public boolean equals(Object object) {
    	if(object == this){
    		return true;
    	}
    	
    	if((object instanceof LevelMember) == false){
    		return false;
    	}
    	
    	LevelMember other = (LevelMember)object;
    	// FIXME need to implement equals() and hashCode() for HierarchyLevel
    	return (other.memberIndex == this.memberIndex && 
    			other.level.equals(this.level));
    }
    
    @Override
    public int hashCode(){
    	return this.memberIndex + 23 * this.level.hashCode();
    }
    
    @Override
    public String toString() {
    	return String.format("%s[%d]",this.level.getName(), this.memberIndex); 
    }

}

