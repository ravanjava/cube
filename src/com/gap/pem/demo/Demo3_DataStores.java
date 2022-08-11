package com.gap.pem.demo;

import com.gap.pem.cds.CubeDs;
import com.gap.pem.cds.HierarchyLevel;
import com.gap.pem.cds.IAttributeContainer;
import com.gap.pem.cds.Intersection;
import com.gap.pem.cds.stores.IDoubleArrayStore;
import com.gap.pem.cds.stores.IIntStore;
import com.gap.pem.cds.util.ArrayOps;

import java.util.Collection;
import java.util.Map;

/**
 * Demonstration of MDAP Usage for JDA Application Developers.  To see MDAP in action, trace
 * through this code in the debugger.
 * @author James Wheeler
 */
class Demo3_DataStores {

    public static void main( String[] args ) {
        // We will start with a Collector instance that has dimensions and one intersection initialized.
        CubeDs collector = Demo1_CollectorInitialization.initializeCollector();

        // This instance contains a single intersection
        for( Intersection intersection :collector.getIntersections() ){
            System.out.println( intersection );
        }

        // Retrieve the intersection by name:
        Intersection item_store = collector.getIntersection("[Item,Store]");

        // Get the primary key levels that are related at this intersection.  These are tuples
        // that uniquely define each item that appears on the intersection:
        String[] primaryKeyDimensionNames = item_store.getPrimaryKeyDimensions();
        String[] primaryKeyLevelNames = item_store.getPrimaryKeyLevels();
        System.out.println("Intersection name: " + item_store.getName() + " defined with tuples from: ");
        for( int i=0; i<primaryKeyDimensionNames.length; i++ ) {
            System.out.println("Dimension:"+primaryKeyDimensionNames[i]+" Level:"+primaryKeyLevelNames[i]);
        }

        // Get the list of all levels are related at this intersection:
        Collection<HierarchyLevel> related_levels = item_store.getRelatedLevels();

        // The intersection contains an IIntStore for each related level.  The values hold the member Ids
        // (indices) in the level.
        IIntStore item_memberIds = item_store.getIntAttribute("Item");
        IIntStore store_memberIds = item_store.getIntAttribute("Store");

        // For example, the first ten intersection items:
        for( int i=0; i<10; i++ ) {
            System.out.println("Intersection at position " + i
                    + " has data for Product Item " + item_memberIds.getElement(i)
                    + " and Location Store " + store_memberIds.getElement(i) );
        }

        // Other stores in the intersection may contain measure values, or mappings to other levels besides
        // the related levels.  This is how to get the full list of attributes on the intersection.
        Map<String, IAttributeContainer.StoreType> attributeMap = item_store.getAttributes();
        System.out.println( "There are " + attributeMap.keySet().size() + " attributes in the intersection");


        // The number of items in this intersection:
        int item_store_count = item_store.size();

        // Create a new data store containing one integer value for each item on the intersection, and add
        // it to the intersection.
        int[] prices = ArrayOps.randomFromPopulation(item_store_count, new int[]{ 100, 110, 150, 199 });
        IIntStore pricesStore = item_store.addIntAttribute("Price");
        for( int price : prices ) {
            pricesStore.addElement(price);  // these are appended to the store.
        }

        // Create a new data store containing a series of double values for each item on the intersection.
        // Some intersection items might not have data.  This is how we represent time series on Intersections.
        //
        double[] qtySold = new double[]{ 10.0, 11.0, 9.0, 6.0, 10.0, 12.0 };

        // Create a double array store, which stores a double[] at each position of the intersection.  Note
        // that these should all be the same length so they can be aligned to matching time periods.
        IDoubleArrayStore salesHistory = item_store.addDoubleArrayAttribute("Sales History", qtySold.length);

        // We will populate all but the last 10 intersection items, to illustrate how to deal with missing values.
        for( int i=0; i<item_store_count-10; i++ ) {
            salesHistory.setElementAt(i, qtySold ); // we'll give them all the same value.
        }

        // At this point, salesHistory has a different size than the intersection containing it. This state
        // can lead to problems, so we ensure that all measures conform:

        System.out.println("Intersection size: " + item_store.size() + " salesHistory size:"+ salesHistory.size());
        try {
            salesHistory.getElement( item_store.size() - 2 );
        }
        catch (Exception e) {
            System.out.println( e ); // ArrayIndexOutOfBoundsException
        }

        item_store.ensureSize(item_store.size());


        double[] series = salesHistory.getElement(0);
        series = salesHistory.getElement( salesHistory.size()-2); // missing!  Result is null.

        // To avoid null pointer exceptions, use something like this:
        series = getSeries( salesHistory, 0 );
        series = getSeries( salesHistory, salesHistory.size()-2 );


        // Note:  All the time series in an intersection should align, so the corresponding items of each
        // series array are associated with the same time period.  Sometimes this leads to a large number
        // of missing values, for example if an intersection holds a forecast measure with no values prior to
        // a certain date, and history measure with no values after that date.  In this case, we can use
        // a DoubleSparseArrayStore, which trims leading and trailing missing values from the internal array
        // but returns a full-length time series when an item's value is retrieved.


    }

    //  Example of a strategy that an application might use to deal with missing time series:
    static int TIME_SERIES_LENGTH = 6;
    static double DOUBLE_MISSING_VALUE = Double.NEGATIVE_INFINITY;
    static double[] emptySeries;
    static synchronized double[] getSeries( IDoubleArrayStore measure, int index ) {
        double[] series = measure.getElement(index );
        if ( series ==  null ) {
            if ( emptySeries == null ) {
                emptySeries = new double[TIME_SERIES_LENGTH];
                for( int i=0;i<TIME_SERIES_LENGTH; i++ )
                    emptySeries[i]  = DOUBLE_MISSING_VALUE;
            }
            series = emptySeries;
        }
        return series;
    }
}