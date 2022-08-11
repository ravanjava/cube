package com.ram.ds.cds;

import java.util.ArrayList;
import java.util.TreeMap;

import com.ram.ds.cds.aggregator.AggregatedMeasure;

/**
 * Helper class for sorting a set of sibling level members by an arbitrary combination of
 * the members' names, and/or their associated values of a set of measures.  Each measure can be sorted
 * in either ascending or descending order, and tuples of multiple measures can be sorted.
 *
 */
public class MeasureValueSorter {
    int ntuples;

    ArrayList<AggregatedMeasure> measures = new ArrayList<AggregatedMeasure>();

    /**
     * Add a measure to the sorter.  Measures can be added in any order.  Use setSortOrder() to specify which
     * measures are to be used calculating the sort order.
     */
    public void addMeasure(AggregatedMeasure measure) {
        int size = measure.size();
        if ( ntuples == 0 )
            ntuples = size;
        else if ( size != ntuples ) {
            throw new CdsException("MeasureValueSorter.addMeasure():  All measures must have same number of values");
        }
        measures.add(measure);
    }


    /**
     * The list of measures to use in sorting, in order from most to least significant.
     * Note that any measure that is to be used in sorting must already be present in measures.
     */
    ArrayList<AggregatedMeasure> sortMeasures = new ArrayList<AggregatedMeasure>();


    /**
     * Define which meausre
     * @param names
     */
    public void setSortMeasures( String[] names ) {
        sortMeasures.clear();
        clearSortOrder();
        for( String name : names ) {
            for ( AggregatedMeasure measure : measures ) {
                if ( measure.getName().equals(name)) {
                    sortMeasures.add( measure );
                    break;
                }
            }
        }
    }


    int[] sortOrder;

    /**
     * Get the ordering of the measures, such that sortOrder[0] is the position in the original of the first value in
     * sorted order,  sortOrder[1] is the position of the second value, etc.
     * @return The sort order.
     */
    public int[] getSortOrder() {
        if ( sortOrder == null )
            sort();
        return sortOrder;
    }

    /**
     * Discard any previously calculated sort order.
     */
    public void clearSortOrder() {
        sortOrder = null;
    }

    /**
     * Compute the sort order given current measure values and defined sort order.
     */
    void sort() {
        TreeMap<ChildRow,Integer> orderedRows = new TreeMap<ChildRow, Integer>();
        for( int i=0; i<ntuples; i++ ) {
            orderedRows.put( new ChildRow(this,i), i );
        }
        sortOrder = new int[ntuples];
        int i=0;
        for( ChildRow row : orderedRows.keySet() ) {
            sortOrder[i++] = row.row;
        }
    }

    /**
     * Represents one row of the collection of measures.
     */
    static class ChildRow implements Comparable<ChildRow> {
        MeasureValueSorter measures;
        int row;

        ChildRow( MeasureValueSorter measures, int row ) {
            this.measures = measures;
            this.row = row;
        }

        @Override
        public int compareTo(ChildRow o) {

            int result = 0;

            for( AggregatedMeasure measure : measures.sortMeasures ) {
                result = measure.compareItems( this.row, o.row );
                if ( result != 0 )
                    break;
            }

            if ( result == 0 ) // all measure values were identical in all rows
                return this.row <= o.row ? -1 : 1;
            else
                return result;
        }


        public boolean equals( Object o) {
            if ( o instanceof  ChildRow )
                return this.row == ((ChildRow)o).row; // suitable defn?  Added method only to silence FindBugs.  Not likely to be called
            else return false;
        }

        public int hashCode() {
            return row;
        }
    }
}
