package com.gap.pem.demo;

import com.gap.pem.cds.CubeDs;
import com.gap.pem.cds.Dimension;
import com.gap.pem.cds.HierarchyLevel;
import com.gap.pem.cds.LevelMemberInfo;
import com.gap.pem.cds.filters.IFilter;
import com.gap.pem.cds.stores.IIntStore;
import com.gap.pem.cds.stores.IStringStore;

import java.util.LinkedList;
import java.util.List;

/**
 * Demonstration of MDAP Usage for JDA Application Developers.  To see MDAP in action, trace
 * through this code in the debugger.
 * @author James Wheeler
 */
class Demo2_Metadata {

    public static void main( String[] args ) {
        // We will start with a Collector instance that with dimensions and one intersection initialized.
        CubeDs collector = Demo1_CollectorInitialization.initializeCollector();

        // What Dimensions are in the collector?
        List<Dimension> dimensions = collector.getDimensions();

        // What levels are contained in each dimension?  Note that some of the levels potentially belong to more than one
        // hierarchy, or no hierarchy at all, allthough in this instance there is one hierarchy containing all levels.
        for( Dimension dimension : dimensions ) {
            List<HierarchyLevel> allLevels = dimension.getLevels();
            System.out.println( allLevels.toString() );
        }

        // What hierarchies are in each dimension?
        for( Dimension dimension : dimensions ) {
            List<String> hierarchyNames = dimension.getHierarchyNames();
            System.out.println( hierarchyNames.toString() );
        }

        // Every dimension should have at least one hierarchy. It is common practice for the first hierarchy to
        // be the default, and have the same name as the dimension.
        for( Dimension dimension : dimensions ) {
            String defaultHierarchyName = dimension.getHierarchyNames().get(0);
            // What levels compose this hierarchy?
            List<HierarchyLevel> levels = dimension.getHierarchy( defaultHierarchyName );
            System.out.print("Hierarchy " + defaultHierarchyName + " contains levels ");
            for( HierarchyLevel level : levels ) {
                System.out.print( level.getName() + " " );
            }
            System.out.println();
        }

        // Inspecting a level.  Let's pick the leaf level of the Location dimension
        Dimension locDimension = collector.getDimension("Location");
        List<HierarchyLevel> locHierarchy = locDimension.getHierarchy( "Location" );
        HierarchyLevel storeLevel = locHierarchy.get( locHierarchy.size() - 1 );

        // How many members are in this level?
        int storeCount = storeLevel.getMemberCount();

        // What dimension does this belong to?   Level contains the name of its containing dimension
        String dimensionName = storeLevel.getDimensionName();
        System.out.println("HierarchyLevel " + storeLevel.getName() + " is in Dimemsion " + dimensionName
            + " and has " + storeCount + " members");

        // The members in a level are identified by position, from 0 to memberCount-1.
        // For example, what is the name of the member whose memberId is 0 (in other words, the member at position
        // zero.  First get need to reference the store inside the level that contains the identity attribute.
        IStringStore storeNameStore = storeLevel.getStringAttribute( storeLevel.getIdentityAttributeName() );

        // What's the name of the member at position 0?
        System.out.println( storeNameStore.getElement(0));

        // What's the name of the member at position 20?
        System.out.println( storeNameStore.getElement(20));


        // Linking a member to its parent - an IIntStore in the level that has the same name as the parent.
        // Get the next level up - the parent of store in the hierarchy.  Note that if the dimension contains
        // more than one hierarchy, the level may have more than one parent level.
        HierarchyLevel regionLevel = locHierarchy.get( locHierarchy.size()-2 );
        IIntStore storeToRegion = storeLevel.getIntAttribute(regionLevel.getName());
        IStringStore regionNameStore = regionLevel.getStringAttribute( regionLevel.getIdentityAttributeName());

        // What's the region memberId for the parent of the store at position 20?
        int parentMemberId = storeToRegion.getElement(20);

        // What's the name of the parent member?  Get the corresponding item from the identity attribute
        String parentMember = regionNameStore.getElement(parentMemberId);
        System.out.println("Child at position 20: "+ storeNameStore.getElement(20) + "  Parent: " + parentMember );


        // ============================================
        // Finding Children and Navigating Hierarchies
        // ============================================

        // Find the children of a level member, using Collector.getChildren()

        // Technique 1: Using Filters and Aggregators
        // Find the children of Country="CA"
        HierarchyLevel parentLevel = locHierarchy.get(0);  // top level = "Country"
        int parentIndex = 0;  // member ID of "CA"

        // Run aggregation through the child level to collect the child index and names.
        MemberAggregator[] aggregators = new MemberAggregator[1];
        aggregators[0] = new MemberAggregator(regionLevel, regionLevel.getIdentityAttributeName());

        List<IFilter> allFilters = new LinkedList<IFilter>();
        if (parentLevel != null) {
            allFilters.add(new ChildMemberFilter(locHierarchy, regionLevel, parentLevel, parentIndex));
        }
        regionLevel.aggregate(allFilters, aggregators);
        System.out.println( "Children of CA in level Region: " + aggregators[0].getMemberList());


        // Technique 2: Dimensions.getChildMembers
        // This is a more powerful technique, including the optional ability to exclude results that do
        // not match any intersection items. It is also faster for large dimensions.
        LevelMemberInfo[] childMembers = locDimension.getChildMembers(
                collector,
                parentLevel,
                0,
                storeLevel,
                null,
                null,
                null,
                false);
        System.out.print("Grandchildren of CA: " );
        for( LevelMemberInfo info : childMembers ) {
            System.out.print( info.getMemberName() + " " );
        }
        System.out.println();



    }


    static int lookupMember( HierarchyLevel level, String member ) {
        IStringStore store = level.getStringAttribute( level.getIdentityAttributeName());
        int memberCount = level.getMemberCount();
        for( int i=0; i<memberCount; i++ ) {
            if ( store.getElement(i).equals(member))
                return i;
        }
        return -1;
    }
}
