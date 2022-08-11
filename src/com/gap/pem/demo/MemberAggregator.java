package com.gap.pem.demo;

import com.gap.pem.cds.HierarchyLevel;
import com.gap.pem.cds.aggregator.Aggregator;
import com.gap.pem.cds.stores.IStringStore;

import java.util.ArrayList;
import java.util.List;

/**
 * An aggregator that collects the names of the members at a certain level.
 * This class is derived from usage in the Demand worksheet source. 
 * See: com.manu.scpoweb.demand.worksheet.node.aggregator.MemberAggregator
 */
class  MemberAggregator implements Aggregator {
    private final List<String> memberList; // The result
    private final IStringStore memberNameStore; // The strings to be searched

    /**
     * Create an aggregator that collects the names of the members from
     * the given data store.
     *
     * @param level  The level containing the member names to be collected.
     * @param levelAttrName  The attribute (IStringStore based) to search
     */
    public MemberAggregator(HierarchyLevel level, String levelAttrName) {
        memberNameStore = level.getStringAttribute(levelAttrName);
        memberList = new ArrayList<String>();
    }

    @Override
    public void accumulate(int i) {
        String memberName = memberNameStore.getElement(i);
        memberList.add(memberName);
    }

    /**
     * Get the aggregation result as a list of member names.
     *
     * @return an ordered list of members.
     */
    public List<String> getMemberList() {
        return memberList;
    }

    /**
     * Discard the result list and initialize a new one.
     */
    public void clear() {
        memberList.clear();
    }

}