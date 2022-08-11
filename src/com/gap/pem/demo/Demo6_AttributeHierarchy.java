package com.gap.pem.demo;

import java.util.ArrayList;
import java.util.List;

import com.gap.pem.cds.CubeDs;
import com.gap.pem.cds.Dimension;
import com.gap.pem.cds.HierarchyLevel;
import com.gap.pem.cds.Intersection;
import com.gap.pem.cds.LevelAttribute;
import com.gap.pem.cds.aggregation.AttributeHierarchy;
import com.gap.pem.cds.stores.IDataDomainStore;
import com.gap.pem.cds.stores.IStringStore;
import com.gap.pem.cds.util.ArrayOps;

/**
 * Demonstration of MDAP Usage for JDA Application Developers.  To see MDAP in action, trace
 * through this code in the debugger.
 * @author James Wheeler
 */
class Demo6_AttributeHierarchy {

    public static void main( String[] args ) {

        CubeDs collector = Demo1_CollectorInitialization.initializeCollector();

        // Add a "Color" attribute to the product's Item level.
        Dimension productDimension = collector.getDimension("Product");
        HierarchyLevel levelItem = productDimension.getLevel("Item");
        // Randomly assign a color to each item
        String[] colors = ArrayOps.randomFromPopulation(
                levelItem.getMemberCount(),
                new String[]{"Red","Green","Yellow","White"});
        // Add a store to the level for the attribute and populate it.
        IStringStore colorStore = levelItem.addStringAttribute("Color");
        for( String color : colors )
            colorStore.addElement( color );


        // Now let's create a hierarchy of Category > Class > Color > Item.
        // Class LevelAttribute describes an attribute that is associated with either a HierarchyLevel
        // or an Intersection.
        ArrayList<LevelAttribute> attributeList = new ArrayList<LevelAttribute>();
        attributeList.add( new LevelAttribute(
                productDimension.getLevel("Category"), "Category"));
        attributeList.add( new LevelAttribute(
                productDimension.getLevel("Class"), "Class" ));
        attributeList.add( new LevelAttribute(levelItem,"Color"));
        attributeList.add( new LevelAttribute( levelItem, "Item" ));

        // The hierarchy needs a list of Intersections to examine.  The hierarchy building process goes
        // through the intersections and identifies the distinct tuples of attributes that can be mapped
        // to each intersection.
        ArrayList<Intersection> intersections = new ArrayList<Intersection>();
        String intersectionName = Demo1_CollectorInitialization.INTERSECTION_ITEM_STORE;
        intersections.add( collector.getIntersection(intersectionName));

        // Constructor sets up the hierarchy and computes the tuple groupings.
        AttributeHierarchy hierarchy = new AttributeHierarchy(
                attributeList,
                collector,
                intersections);


        // Create a dimension named "Product2" and a hierarchy named "Product2" where each hierarchy level
        // is created and populated with members where each member is a distinct tuple of attributes.

        Dimension newDimension = hierarchy.createDimension("Product2","Product2");

        // For example, we have created a new dimension with a hierarchy of Category > Class > Color > Item.
        // The topmost level in this dimension is named Category and its members are the distinct values of
        // Class that cast a shadow on at least one of the intersections associated with the hierarchy.
        // The second level Class has one member for each distinct pair of (Category,Class) that cast shadows.
        // The third level Color has one member for each distinct tuple of (Category,Class,Color), and the
        // fourth level "Item" has one member for each distinct tuple of (Category,Class,Color,Item).
        System.out.println("New Dimension 'Product2': ");
        for( HierarchyLevel newLevel : newDimension.getHierarchy( newDimension.getName())) {
            System.out.println( "  "+ newLevel.toString() );
        }

        // =========================================
        // Intersection attributes in the Hierarchy
        // =========================================

        // First, we will add an attribute "Turnover" to the [Item,Store] intersection with possible values
        // of "Fast", "Medium", "Slow".
        Intersection item_store = collector.getIntersection(intersectionName);
        int item_store_count = item_store.size();
        String[] turnover_strings = new String[]{"Fast","Medium","Slow"};
        IDataDomainStore attrStore = item_store.addDataDomainAttribute("Turnover");
        // First populate the acceptable values in the data domain store
        for( String value : turnover_strings ) {
            attrStore.addValidValue( value );
        }

        // Then add an attribute value for each intersection item:
        String[] attrValues = ArrayOps.randomFromPopulation( item_store_count, attrStore.getValidValues() );
        for( String value : attrValues ) {
            attrStore.addElement( value );
        }

        // With the intersection attribute in place, we can add it to a hierarchy just as with a dimension attribute

        List<LevelAttribute> locationAttributeList = new ArrayList<LevelAttribute>();

        // We will create a LevelAttribute using the intersection attribute instead of an attribute from a HierarchyLevel.
        locationAttributeList.add( new LevelAttribute(item_store,"Turnover"));
        // Other levels in the hierarchy come from the location dimension:
        Dimension locationDimension = collector.getDimension("Location");
        locationAttributeList.add( new LevelAttribute( locationDimension.getLevel("Country"), "Country"));
        locationAttributeList.add( new LevelAttribute( locationDimension.getLevel("Region"), "Region"));
        locationAttributeList.add( new LevelAttribute( locationDimension.getLevel("Store"), "Store"));

        AttributeHierarchy newLocationHierarchy = new AttributeHierarchy(locationAttributeList, collector, intersections );
        Dimension newLocDimension = newLocationHierarchy.createDimension("Location2", "Location2");

        // Now we have created a new dimension with a hierarchy of Turnover > Country > Region > Store
        System.out.println("New Dimension 'Location2':");
        for( HierarchyLevel newLevel : newLocDimension.getHierarchy( "Location2" )) {
            System.out.println( "  " +newLevel.toString() );
        }



    }
}
