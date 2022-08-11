package com.gap.pem.demo;

import com.gap.pem.cds.CubeDs;
import com.gap.pem.cds.Dimension;
import com.gap.pem.cds.HierarchyLevel;
import com.gap.pem.cds.IAttributeContainer;
import com.gap.pem.cds.aggregation.AggregationController;
import com.gap.pem.cds.aggregation.LevelMemberSelector;
import com.gap.pem.cds.aggregator.Aggregator;
import com.gap.pem.cds.aggregator.CountAggregator;
import com.gap.pem.cds.filters.Filter;
import com.gap.pem.cds.filters.IFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstration of MDAP Usage for JDA Application Developers.  To see MDAP in action, trace
 * through this code in the debugger.
 * @author James Wheeler
 */
class Demo4_Aggregation {

    public static void main( String[] args ) {
        // We will start with a Collector instance that with dimensions and one intersection initialized.
        CubeDs collector = Demo1_CollectorInitialization.initializeCollector();

        // ==========================================
        // Aggregation with Collector.aggregate()
        // ==========================================

        // Any combination of levels can be aggregated:

        //------------------------------------------------------------------
        // Location:             *ALL*     Country     Region     Store
        // Product:  *ALL*         X          X          X          X
        //           Category      X          X          X          X
        //           Class         X          X          X          X
        //           Item          X          X          X          X
        // -----------------------------------------------------------------


        // Using a simple aggregator that merely counts the number of times its accumulate() is called.
        // This gives a count of the number of intersection items that match a set of filters.
        Aggregator countAggregator = new CountAggregator();

        Map<String,String> dimensionToHierarchyMap = new HashMap<String,String>();
        dimensionToHierarchyMap.put("Location","Location");
        dimensionToHierarchyMap.put("Product","Product");
        String intersectionName = Demo1_CollectorInitialization.INTERSECTION_ITEM_STORE;

        CountAggregator aggregator = new CountAggregator(); // this aggregator doesn't need to be attached to a measure.
        //  next show use with DoubleArrayAggregator
        Aggregator[] aggregators = new Aggregator[]{aggregator};
        ArrayList<IFilter> filterList = new ArrayList<IFilter>();

        // No filters gives all-all (total count);
        IFilter[] filters = filterList.toArray( new IFilter[filterList.size()] );

        collector.aggregate(
                dimensionToHierarchyMap,
                intersectionName,
                aggregators,
                filters );

        int totalCount = aggregator.getResult();
        System.out.println("Count of all items is " + totalCount);


        // Add a filter for the first Country ("CA"):
        Dimension locationDimension = collector.getDimension("Location");
        HierarchyLevel countryLevel = locationDimension.getLevel("Country");
        IndexFilter filterCA = new IndexFilter(countryLevel, 0);
        filterList.add (filterCA);

        aggregator.clearResult();  // Must do this each time or results continue to accumulate
        collector.aggregate(
                dimensionToHierarchyMap,
                intersectionName,
                aggregators,
                filterList.toArray( new IFilter[filterList.size()] ) );

        totalCount = aggregator.getResult();
        System.out.println("Count of items where Country=CA is " + totalCount);

        // Add a filter for the top level Category of the Product dimension
        Dimension dimProduct = collector.getDimension("Product");
        HierarchyLevel categoryLevel = dimProduct.getLevel("Category");
        IndexFilter filterCategory0 = new IndexFilter(categoryLevel,0);
        filterList.add( filterCategory0 );

        aggregator.clearResult();
        collector.aggregate(
                dimensionToHierarchyMap,
                intersectionName,
                aggregators,
                filterList.toArray( new IFilter[filterList.size()] ) );

        totalCount = aggregator.getResult();
        System.out.println("Count of items where Country=CA and Category=Category-0 is " + totalCount);

        // ==========================================
        // Aggregation with AggregationController
        // ==========================================
        //
        // This approach is much faster when multiple result values are desired.  It precomputes the
        // mapping to multiple results, and uses a separate aggregator for each.

        // LevelMemberSelector:  Holds a selection of one or more members of a level
        LevelMemberSelector categorySelector = new LevelMemberSelector(categoryLevel);
        categorySelector.select(0);  // you can select by member ID
        categorySelector.select( categoryLevel.getIdentityAttributeName(), "Category-1"); // or select by value of an attribute

        LevelMemberSelector countrySelector = new LevelMemberSelector(countryLevel);
        countrySelector.selectAll();  // selects every member of the level

        AggregationController controller = new AggregationController(
                collector,
                collector.getIntersection( Demo1_CollectorInitialization.INTERSECTION_ITEM_STORE ),
                new String[]{"Product","Location"}, //  the Dimensions.
                new HierarchyLevel[]{categoryLevel, countryLevel}, // levels we want to aggregate up to.
                new LevelMemberSelector[]{categorySelector, countrySelector}, // Select which aggregated level members.
                null ); // optional additional filters

        // The number of tuples of (Category,Country) that will appear in the aggregatoin.
        int resultTupleCount = controller.getResultTupleCount();

        // Allocate and initialize an array of aggregators.  We need an aggregator for each of the
        // result tuples.
        aggregators = new CountAggregator[resultTupleCount];
        for( int i=0; i<resultTupleCount; i++ )
            aggregators[i] = new CountAggregator();

        // Perform the aggregation
        controller.aggregate( aggregators );

        // Get the level member IDs for the aggregated levels.  The outer dimension is by tuple,
        // one for each tuple of target level members.  The inner dimension contains the member ids
        // for the target levels.
        int[][] resultTuples = controller.getResultTuples();

        // Get the names of the target levels defined for this controller instance:
        String[] targetLevelNames = controller.getTargetLevelNames();

        for( int i=0; i<resultTupleCount; i++ ) {
        	System.out.println("####RAM####");
            System.out.println(
                    "  "+targetLevelNames[0]+" member " + resultTuples[i][0]
                  + "; "+targetLevelNames[1]+" member " + resultTuples[i][1]
                  + "   Count=" + ((CountAggregator) aggregators[i] ).getResult()  );
            System.out.println("####RAM####");
        }

        // It is not necessary to include all the dimensions in the aggregation.  For example, to aggregate by
        // the Product.category level only we specify just this dimension. The other dimensions are omitted and
        // do not figure in the aggregation.
        categorySelector.selectNone();
        categorySelector.select(1); // just the category at position 1
        AggregationController controller2 = new AggregationController(
                collector,
                collector.getIntersection( Demo1_CollectorInitialization.INTERSECTION_ITEM_STORE ),
                new String[]{"Product"}, //  the Dimensions.
                new HierarchyLevel[]{categoryLevel}, // levels we want to aggregate up to.
                new LevelMemberSelector[]{categorySelector}, // Select which aggregated level members.
                null ); // optional additional filters

        CountAggregator[] aggregators2 = new CountAggregator[ controller2.getResultTupleCount()];
        for(int i=0; i<aggregators2.length; i++ ) {
            aggregators2[i]= new CountAggregator();
        }
        resultTuples = controller2.getResultTuples();
        controller2.aggregate( aggregators2 );
        for( int i=0; i<aggregators2.length; i++ ) {
            System.out.println("Category " + resultTuples[i][0] + "  Count=" + aggregators2[i].getResult() );
        }

        // It is possible to do a Total-Total-Total aggregation by constructing an aggregation controller
        // using empty lists of items and selectors:
        AggregationController controller3 = new AggregationController(
                collector,
                collector.getIntersection( Demo1_CollectorInitialization.INTERSECTION_ITEM_STORE ),
                new String[0], // empty
                new HierarchyLevel[0], // empty
                new LevelMemberSelector[0], // empty
                null);
        CountAggregator[] aggregators3 = new CountAggregator[ controller3.getResultTupleCount()];
        for( int i=0; i<aggregators3.length; i++ ) {
            aggregators3[i]=new CountAggregator();
        }
        controller3.aggregate(aggregators3);
        System.out.println("Total count, all dimensions = " + aggregators3[0].getResult());

    }

    /**
     * Filter that matches a specific target index in an attribute container
     */
    private static class IndexFilter extends Filter {

        private int targetIndex;

        public IndexFilter(IAttributeContainer iAttributeContainer, int targetIndex) {
            super(iAttributeContainer);
            this.targetIndex = targetIndex;
        }

        public boolean isMatch(int iIndex)
        {
            return this.targetIndex == iIndex;
        }
    }
}