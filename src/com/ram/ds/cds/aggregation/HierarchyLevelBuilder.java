package com.ram.ds.cds.aggregation;

import java.util.HashMap;

import com.ram.ds.cds.CdsException;
import com.ram.ds.cds.HierarchyLevel;
import com.ram.ds.cds.stores.IIntStore;
import com.ram.ds.cds.stores.IStringStore;
import com.ram.ds.cds.stores.IntStore;
import com.ram.ds.cds.stores.StringStore;


/**
 * Helper for populating a hierarchy of parent-child levels
 *  @author  James Wheeler
 *  @author  Last updated by $Author: misong $
 *  @version $Revision: 1.5.6.1 $
 *
 */
public class HierarchyLevelBuilder {
    String dimensionName;
    String levelName;
    String identityAttributeName;
    
    HierarchyLevelBuilder parentBuilder;
    IStringStore memberNameStore;
    IIntStore parentIdStore;

    HashMap<Long,Integer> tupleToMemberId = new HashMap<Long, Integer>();
    int nextMemberId = 0;

    /**
     * Builder for collecting values in a HierarchyLevel that has an identity attribute (member name as String) and an optional
     * index into a parent level
     * @param dimensionName
     * @param levelName
     * @param identityAttributeName
     * @param parentBuilder Builder for the parent level, if any.  Null for the top level in the new hierarchy
     */
    public HierarchyLevelBuilder(String dimensionName, String levelName, String identityAttributeName, HierarchyLevelBuilder parentBuilder) {
        this.dimensionName = dimensionName;
        this.levelName = levelName;
        this.identityAttributeName = identityAttributeName;
        this.parentBuilder = parentBuilder;

        memberNameStore = new StringStore();
        if (parentBuilder != null){
            parentIdStore = new IntStore();
        }
    }
    
    public String getLevelName() {
        return levelName;
    }

    /**
     * Given a set of indices into the attributes of the dimension to be created, lookup (or add)the member for this tuple
     * @param grouping Attribute grouping of tuples for this depth.
     * @param memberValue  The String value of the attribute for the new member, if it must be added
     * @param attributeInds One value for each attribute ( 0..cardinality-1 ) starting from the top
     * @return The ordinal position in the list of distinct tuples.
     */
    public int getMemberId(AttributeGrouping grouping, String memberValue, int[] attributeInds) {
        long tupleIndex = grouping.getTupleIndex(attributeInds);
        return getMemberId(memberValue, tupleIndex);
    }

    /**
     * Given a distinct TupleInd (packed index in the Cartesian tuple space of the component attributes), return
     * its ordinal position in the list of distinct tuples.  This will be the member ID in the resultant HierarchyLevel.
     * @param memberValue
     * @param distinctTupleInd
     * @return  The ordinal position of the tuple index in the list of distinct tuples.  This will be the member
     * ID in the resultant HierarchyLevel.
     */
    public int getMemberId(String memberValue, long distinctTupleInd){
        if (tupleToMemberId.containsKey(distinctTupleInd)) {
            return tupleToMemberId.get( distinctTupleInd );
        }

        tupleToMemberId.put(distinctTupleInd, nextMemberId++);
        int memberId = memberNameStore.addElement(memberValue);
        // Diagnostic for breakpoint stop
        //if ( memberId != nextMemberId-1 )
        //    System.out.println("Something is wrong");
        return memberId;
    }


    /**
     * Build the level.
     * @return New hierarchy level built from the hierarchy tuples.
     */
    public HierarchyLevel buildLevel() {
        HierarchyLevel level = new HierarchyLevel( levelName, dimensionName, identityAttributeName);
        level.addAttributeStore(identityAttributeName, memberNameStore );
        if (parentIdStore != null) {
            level.addAttributeStore(parentBuilder.getLevelName(), parentIdStore);
            if (memberNameStore.size() != parentIdStore.size()) {
                throw new CdsException("HierarchyLevelBuilder.buildLevel: Parent attribute store size does not match member count for new level");
            }
        }
        // anything else to do?
        return level;
    }

}

