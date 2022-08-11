package com.gap.pem.demo;

import com.gap.pem.cds.CubeDs;
import com.gap.pem.cds.Dimension;
import com.gap.pem.cds.HierarchyLevel;
import com.gap.pem.cds.Intersection;
import com.gap.pem.cds.stores.IIntStore;
import com.gap.pem.cds.stores.IStringStore;
import com.gap.pem.cds.util.ArrayOps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Demonstration of MDAP Usage for JDA Application Developers.  To see MDAP in action, trace
 * through this code in the debugger.
 * @author James Wheeler
 */
class Demo1_CollectorInitialization {

    static final String PRODDIM = "Product";
    static final String CATEGORYLEVEL = "Category";
    static final String CLASSLEVEL = "Class";
    static final String ITEMLEVEL = "Item";
    static final String LOCDIM = "Location";
    static final String COUNTRYLEVEL = "Country";
    static final String REGIONLEVEL = "Region";
    static final String STORELEVEL = "Store";
    static final String INTERSECTION_ITEM_STORE="[Item,Store]";

    public static void main( String[] args ) {
        CubeDs collector = initializeCollector();
        System.out.println("Collector initialized: "+collector.toString());
    }

    /** Set up an instance of the Collector and populate it with three sample dimensions.
     * @return New instance.
     */
    static CubeDs initializeCollector() {
        CubeDs collector = new CubeDs();

        // Add the Product Dimension
        Dimension product_dimension = initializeProductDimension();
        collector.addDimension(product_dimension);

        // Add the Location Dimension
        Dimension loc_dimension = initializeLocDimension();
        collector.addDimension( loc_dimension );

        // Add an Intersection at the leaf level of both dimensions
        HierarchyLevel prodLeafLevel = getLeafLevel( product_dimension ); // Item
        HierarchyLevel locLeafLevel = getLeafLevel( loc_dimension ); // Store
        // The intersection name can be anything.  We will base this one on the names of the intersecting levels.
        Intersection item_store = addIntersection( collector, INTERSECTION_ITEM_STORE, 400,  prodLeafLevel, locLeafLevel);

        // At this point the intersection contains only the references to the items at its two intersecting levels.
        // Measures have not been added.
        System.out.println( item_store.toString() );

        return collector;
    }

    static Dimension initializeProductDimension() {
        Dimension dim = new Dimension(PRODDIM);
        
        // Create levels and add to the dimension.  The levels do not need to be populated when added 
        HierarchyLevel levelCat = new HierarchyLevel(CATEGORYLEVEL, PRODDIM);
        // Every level MUST have an identity attribute.
        // By default, the identity attribute is the name of the level.
        String identityAttributeName = levelCat.getIdentityAttributeName();
        levelCat.addStringAttribute( identityAttributeName );
        dim.addLevel(levelCat);

        // Add a Class level, with Category as its parent
        HierarchyLevel levelClass = new HierarchyLevel(CLASSLEVEL, PRODDIM);
        levelClass.addParentAttribute(CATEGORYLEVEL);
        dim.addLevel(levelClass);

        // Add an Item level, with Class as its parent
        HierarchyLevel levelItem = new HierarchyLevel(ITEMLEVEL, PRODDIM);
        levelItem.addParentAttribute(CLASSLEVEL);
        dim.addLevel(levelItem);

        // Populate the hierarchy with 4 categories, 10 classes, and 25 items:
        int category_count = 4;
        int class_count = 10;
        int item_count = 25;

        // Populate Category Level by adding elements to its identity attribute store.
        //   The root level members do not have parents, so items are added one at a time to the
        //   identity attribute store.  Each item is added as a String, and the result of
        //   addElement() is the new member ID.  Note that memberId is simply a synonym for the
        //   index position of a member in the level.
        IStringStore categoryStore = levelCat.getStringAttribute( levelCat.getIdentityAttributeName() );
        for( int i=0; i<category_count; i++ ) {
            int memberId = categoryStore.addElement( CATEGORYLEVEL+ "-" + i );
        }

        System.out.println(levelCat.toString());

        // Use getMemberCount() to find out how many members are in the level:
        int memberCount = levelCat.getMemberCount();
        for( int i=0; i<memberCount; i++ ) {
            System.out.println(categoryStore.getElement(i));
        }


        // Populate Class level:
        // - Add identity attribute.  The level creates the store and returns it.
        IStringStore classStore = levelClass.addStringAttribute( levelClass.getIdentityAttributeName());
        // - Add parent attribute.  This is integer valued, and contains the member Id of the member's parent.
        String parentLevelName = CATEGORYLEVEL;
        levelClass.addParentAttribute( parentLevelName );
        IIntStore classParentStore = levelClass.getIntAttribute( parentLevelName );

        // Add members to the Class level, with arbitrary parents for demo purposes.
        for( int i=0; i<class_count; i++ ) {
            classStore.addElement( CLASSLEVEL+"-"+i );
            classParentStore.addElement( i%category_count );
        }

        // Inspect the level:
        System.out.println( levelClass.toString() );
        for( int i=0; i<levelClass.getMemberCount(); i++ ) {
            String memberName = classStore.getElement(i);
            int parentMemberId = classParentStore.getElement(i);
            String parentName = categoryStore.getElement( parentMemberId );
            System.out.println( memberName + " child of " + parentName );
        }

        // Populate Item level in a similar manner
        IStringStore itemStore = levelItem.addStringAttribute( levelItem.getIdentityAttributeName() );
        levelItem.addParentAttribute( levelClass.getIdentityAttributeName());
        IIntStore itemParentStore = levelItem.getIntAttribute( levelClass.getIdentityAttributeName() );
        for( int i=0; i<item_count; i++ ) {
            itemStore.addElement( ITEMLEVEL+"="+i);
            itemParentStore.addElement( i%class_count );
        }

        // Inspect the level:
        System.out.println( levelItem.toString() );
        for( int i=0; i<levelItem.getMemberCount(); i++ ) {
            String memberName = itemStore.getElement(i);
            int parentMemberId = itemParentStore.getElement(i);
            String parentName = classStore.getElement( parentMemberId );
            System.out.println( memberName + " child of " + parentName );
        }

        //  The Dimension does not yet contain a hierarchy.  We will add a default hierarchy whose
        //  name is the same as the name of the Dimension.  This default dimension should always
        //  be created if the application assumes that Dimensions and Hierarchies are synonymous.

        dim.addHierarchy( dim.getName(), Arrays.asList( levelCat, levelClass, levelItem ));

        // Show the hierarchy
        System.out.print("Default hierarchy in " + dim.getName() + ": ");
        for( HierarchyLevel level : dim.getHierarchy( dim.getName() ) ){
            System.out.print(" > " + level.getName());
        }
        System.out.println();

        return dim;
    }


    static Dimension initializeLocDimension() {
        // Three levels: Country > Region > Store
        Dimension dim = new Dimension( LOCDIM );
        HierarchyLevel country = addLevel(dim,COUNTRYLEVEL,null);
        HierarchyLevel region = addLevel(dim,REGIONLEVEL,COUNTRYLEVEL);
        HierarchyLevel store = addLevel(dim,STORELEVEL,REGIONLEVEL);

        ArrayList<HierarchyLevel> hierarchyLevels = new ArrayList<HierarchyLevel>();
        hierarchyLevels.add( country );
        hierarchyLevels.add( region );
        hierarchyLevels.add( store );
        dim.addHierarchy( dim.getName(), hierarchyLevels );

        //  Note:  the use of helper code following is not the efficient way to populate a large Dimension.
        //  It is written this way for readability as tutorial code.
        addMembers(dim, "CA", "CanadaEast", "Halifax");
        addMembers(dim, "CA", "CanadaEast", "Ottawa");
        addMembers(dim, "CA", "CanadaEast", "Toronto");
        addMembers(dim, "CA", "CanadaEast", "Windsor");
        addMembers(dim, "CA", "CanadaEast", "Quebec");
        addMembers(dim, "CA", "CanadaEast", "Laval");
        addMembers(dim, "CA", "CanadaWest", "Moose Jaw");
        addMembers(dim, "CA", "CanadaWest", "Vancouver");
        addMembers(dim, "CA", "CanadaWest", "Winnipeg");

        addMembers(dim, "US", "Northeast", "Boston");
        addMembers(dim, "US", "Northeast", "New Haven");
        addMembers(dim, "US", "Northeast", "Burlington");
        addMembers(dim, "US", "Northeast", "Portland (ME)");
        addMembers(dim, "US", "Northeast", "Nashua");
        addMembers(dim, "US", "Northeast", "Hartford");
        addMembers(dim, "US", "Northeast", "Springfield");
        addMembers(dim, "US", "Northeast", "Providence");
        addMembers(dim, "US", "Northeast", "Albany");
        addMembers(dim, "US", "Northeast", "Buffalo");
        addMembers(dim, "US", "Northeast", "Trenton");

        addMembers(dim, "US", "Southeast", "Rockville");
        addMembers(dim, "US", "Southeast", "Atlanta");
        addMembers(dim, "US", "Southeast", "Charlotte");
        addMembers(dim, "US", "Southeast", "Jacksonville");
        addMembers(dim, "US", "Southeast", "Miami");
        addMembers(dim, "US", "Southeast", "Baltimore");
        addMembers(dim, "US", "Southeast", "Memphis");
        addMembers(dim, "US", "Southeast", "Huntsville");
        addMembers(dim, "US", "Southeast", "Jackson");
        addMembers(dim, "US", "Southeast", "Birmingham");

        addMembers(dim, "US", "Midwest", "Chicago");
        addMembers(dim, "US", "Midwest", "Detroit");
        addMembers(dim, "US", "Midwest", "Indianapolis");
        addMembers(dim, "US", "Midwest", "Columbus");
        addMembers(dim, "US", "Midwest", "Des Moines");
        addMembers(dim, "US", "Midwest", "Minneapolis");
        addMembers(dim, "US", "Midwest", "Fargo");
        addMembers(dim, "US", "Midwest", "Kansas City");
        addMembers(dim, "US", "Midwest", "Dallas");
        addMembers(dim, "US", "Midwest", "Houston");
        addMembers(dim, "US", "Midwest", "Abilene");
        addMembers(dim, "US", "Midwest", "Omaha");

        addMembers(dim, "US", "West", "Denver");
        addMembers(dim, "US", "West", "Colorado Springs");
        addMembers(dim, "US", "West", "Helena");
        addMembers(dim, "US", "West", "Caspar");
        addMembers(dim, "US", "West", "Billings");
        addMembers(dim, "US", "West", "Salt Lake City");
        addMembers(dim, "US", "West", "Albuquerque");
        addMembers(dim, "US", "West", "Tucson");
        addMembers(dim, "US", "West", "Phoenix");
        addMembers(dim, "US", "West", "San Diego");
        addMembers(dim, "US", "West", "Los Angeles");
        addMembers(dim, "US", "West", "Sacramento");
        addMembers(dim, "US", "West", "San Jose");
        addMembers(dim, "US", "West", "Palo Alto");
        addMembers(dim, "US", "West", "Portlans (OR)");
        addMembers(dim, "US", "West", "Seattle");
        addMembers(dim, "US", "West", "Anchorage");
        addMembers(dim, "US", "West", "Fairbanks");
        addMembers(dim, "US", "West", "Juneau");
        addMembers(dim, "US", "West", "Honolulu");


        return dim;
    }

    // ====================================
    // Helpers


    static HierarchyLevel addLevel( Dimension dimension, String levelName, String parentLevelName ) {
        HierarchyLevel level = new HierarchyLevel(levelName, dimension.getName());
        dimension.addLevel(level);
        level.addStringAttribute( levelName );
        if ( parentLevelName != null ) {
            level.addParentAttribute(parentLevelName);
        }
        return level;

    }
    static void addMembers( Dimension dimension, String level0Member, String level1Member, String level2Member ) {
        List<HierarchyLevel> hierarchy = dimension.getHierarchy( dimension.getName() );  // default hierarchy
        HierarchyLevel level0 = hierarchy.get(0);
        HierarchyLevel level1 = hierarchy.get(1);
        HierarchyLevel level2 = hierarchy.get(2);

        lookupOrAddMember( level0, level0Member );
        lookupOrAddMember( level1, level1Member, level0, level0Member );
        lookupOrAddMember( level2, level2Member, level1, level1Member );
    }

    static int lookupOrAddMember( HierarchyLevel level, String member ) {
        IStringStore memberNameStore = level.getStringAttribute( level.getIdentityAttributeName() );
        int count = level.getMemberCount();
        for( int i=0; i<count; i++ ) {
            if ( memberNameStore.getElement(i).equals(member))
                return i;
        }
        return memberNameStore.addElement(member);
    }

    static int lookupOrAddMember( HierarchyLevel level, String member, HierarchyLevel parentLevel, String parentMember ) {
        int parentMemberId = lookupOrAddMember( parentLevel, parentMember );
        int memberId = lookupOrAddMember( level, member );
        IIntStore parentAttributeStore = level.getIntAttribute( parentLevel.getName());
        parentAttributeStore.setElementAt(memberId, parentMemberId );
        return memberId;
    }

    static HierarchyLevel getLeafLevel( Dimension dimension ) {
        List<HierarchyLevel> defaultHierarchy = dimension.getHierarchy( dimension.getName() );
        return defaultHierarchy.get( defaultHierarchy.size()-1 );
    }

    /**
     * Create an Intersection, add it to the Collector, and populate it with random member Ids from
     * the related levels.
     * @param collector Collector instance in which to create the
     * @param name  Name for the intersection.
     * @param itemCount The number of items to add to the intersection.
     * @param level0 The first related level; must already exist and be populated.
     * @param level1 Second related level; must already exist and be populated.
     * @return  Newly built Intersection
     */
    static Intersection addIntersection( CubeDs collector, String name, int itemCount, HierarchyLevel level0, HierarchyLevel level1 ){
        Intersection intersection = collector.addIntersection( name, new HierarchyLevel[]{ level0, level1});
        // It is assumed itemCount is no larger than the product of the sizes of the two levels.
        int level0count = level0.getMemberCount();
        int level1count = level1.getMemberCount();
        // Generate a random selection of the possible intersecting memberIds:
        int[] temp = ArrayOps.deal(itemCount, level0count*level1count );

        // Get the containers in the intersection to store the related level member ids.
        IIntStore intersectionLevel0Ids = intersection.getIntAttribute( level0.getName() );
        IIntStore intersectionLevel1Ids = intersection.getIntAttribute( level1.getName() );
        for( int t : temp ) {  // unpack and add members from the random set
            intersectionLevel0Ids.addElement( t/level1count );
            intersectionLevel1Ids.addElement( t%level1count );
        }

        return intersection;
    }


}
