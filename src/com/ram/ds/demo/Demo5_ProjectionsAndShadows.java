package com.ram.ds.demo;

import java.util.ArrayList;

import com.ram.ds.cds.*;
import com.ram.ds.cds.stores.IStringStore;
import com.ram.ds.cds.util.BitVector;
import com.ram.ds.cds.util.Sequence;

/**
 * Demonstration of MDAP Usage for JDA Application Developers.  To see MDAP in action, trace
 * through this code in the debugger.
 * @author James Wheeler
 */
class Demo5_ProjectionsAndShadows {


    public static void main( String[] args ) {
        // We will start with a Collector instance that with dimensions and one intersection initialized.
        CubeDs collector = Demo1_CollectorInitialization.initializeCollector();

        demoProjection(collector);
        demoTupleShadow(collector);
    }

    /**
     * Demonstrates projecting an array that conforms to a hierarchy level onto an intersection.
     * @param collector
     */
    static void demoProjection( CubeDs collector ) {

        String intersectionName = Demo1_CollectorInitialization.INTERSECTION_ITEM_STORE;
        Intersection itemStore = collector.getIntersection( intersectionName );

        // Calculate a mapping from this level to the intersection:
        HierarchyLevel regionLevel = collector.getDimension("Location").getLevel("Region");
        int memberCount = regionLevel.getMemberCount();
        IStringStore regionMemberNames = regionLevel.getStringAttribute( regionLevel.getIdentityAttributeName());

        int[] mapping = itemStore.projectToIntersection(
                collector.getDimension("Location"), // dimension
                "Location",                         // hierarchy name
                "Region",                           // upper level name
                Sequence.getSequence(memberCount)   // int[] from 0..memberCount-1
        );

        // Now mapping.length == intersection.size(), and each value in mapping is the
        // associated memberId in level Region.
        for( int i=0; i<10; i++ ) {
            System.out.println("Intersection position " + i + " descends from Region " +
                regionMemberNames.getElement( mapping[i] ));
        }
    }

    //  Finds the "shadow" of a tuple of level members on an intersection
    static void demoTupleShadow( CubeDs collector ) {

        String intersectionName = Demo1_CollectorInitialization.INTERSECTION_ITEM_STORE;
        Intersection itemStore = collector.getIntersection( intersectionName );

        LevelMember category2 = new LevelMember(collector,"Product","Category",0);
        LevelMember region3 = new LevelMember(collector,"Location","Region",3);
        ArrayList<LevelMember> members = new ArrayList<LevelMember>();
        members.add(category2);
        members.add(region3);
        LevelMemberTuple tuple = new LevelMemberTuple(members);

        BitVector shadow = itemStore.getTupleShadow(collector, tuple);
        // shadow is true for every item on the intersection that is in the shadow of this tuple.

       System.out.println("Tuple shadows " + shadow.sum() + " items on intersection");

        // BitVector has a number of set operation methods.  See also BitMatrix, which
        // can be used to keep track of items in a time series class like DoubleArrayStore.


    }

}