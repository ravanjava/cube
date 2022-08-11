package com.ram.ds.demo;

import java.util.LinkedList;
import java.util.List;

import com.ram.ds.cds.HierarchyLevel;
import com.ram.ds.cds.filters.Filter;
import com.ram.ds.cds.stores.IIntStore;

/**
 * A filter that is capable of finding out the child members of
 * a parent member.
 */
class ChildMemberFilter extends Filter {

    // the name of the parent member.
    private final int parentIndex;

    private final List<IIntStore> ancestorFkStores;

    /**
     * Create a filter that matches only the child members of a
     * given parent member.
     *
     */
    public ChildMemberFilter(List<HierarchyLevel> allLevels,
                             HierarchyLevel childLevel,
                             HierarchyLevel parentLevel,
                             int parentIndex) {
        super(childLevel);

        ancestorFkStores = new LinkedList<IIntStore>();

        boolean cachingStarted = false;
        for (int i = allLevels.size() - 1; i > 0; i--) {
            HierarchyLevel levelCurrent = allLevels.get(i);
            HierarchyLevel levelAbove = allLevels.get(i - 1);

            if (levelCurrent == childLevel) {
                cachingStarted = true;
            }
            if (levelCurrent == parentLevel) {
                cachingStarted = false;
            }
            if (cachingStarted) {
                ancestorFkStores.add(levelCurrent.getIntAttribute(levelAbove.getName()));
            }
        }

        this.parentIndex = parentIndex;
    }

    // iIndex - index of an element in the attribute container we are
    // aggregating.
    @Override
    public boolean isMatch(int iIndex) {
        int index = iIndex;
        int parentFkIndex = -1;
        for (IIntStore ancestorFkStore : ancestorFkStores) {
            parentFkIndex = ancestorFkStore.getElement(index);
            index = parentFkIndex;
        }
        return (parentFkIndex == parentIndex);
    }

    @Override
    public String toString() {
        return "ChlidMemberFilter{" +
                "parentIndex=" + parentIndex +
                ", ancestorFkStores=" + ancestorFkStores +
                "} " + super.toString();
    }
}
