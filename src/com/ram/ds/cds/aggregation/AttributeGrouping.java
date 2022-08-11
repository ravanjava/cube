package com.ram.ds.cds.aggregation;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ram.ds.cds.CdsException;
import com.ram.ds.cds.Intersection;
import com.ram.ds.cds.LevelAttribute;
import com.ram.ds.cds.util.ArrayOps;

/**
 * Represents a grouping by distinct tuples of attributes in an attribute based hierarchy.  The grouping can
 * consist of one or more attributes.
 * <p>
 *     THe purpose of a grouping is to identify the distinct tuples of values that occur in a set of attributes.
 *     If there is only one attribute in the hierarchy, the grouping identifies the distinct single values in
 *     that attribute.  If the hierarchy has two attributes, the grouping finds the distinct tuples of (attr1,attr2).
 *     Each tuple is given a distinct tupleId.
 * </p>
 *
 * <p>
 *     Tuples are added one Intersection at a time, and the set of Tuples that appear on each intersection are
 *     recorded on separate lists.  When the HierarchyLevel is generated from the grouping, a related level mapping
 *     is added to each of the Intersections.
 * </p>
 *
 *  @author  James Wheeler
 *  @author  Last updated by $Author: misong $
 *  @version $Revision: 1.9.6.1 $
 */
public class AttributeGrouping 
{
	// Note:
	// In the context of this class, the tuples are formed by the combination of attribute values
	// that will be at successive levels in a common dimension.
	//
    static final boolean trace = false;

    /**
     * Attributes in the hierarchy that make up this grouping
     */
    List<LevelAttribute> hierarchy;

    /**
     * The cardinality for each attribute in the hierarchy
     */
    int[] cardinalities;

    /**
     * The weights for unit polynomial to calculate the linear offset in the attribute tuple space
     */
    private long[] weights;


    /**
     * Maps a packed tuple index in the attribute tuple space to an aggregator for items that aggregate to that tuple.
     * Keys are packed tuples of the Cartesian tuple space of attribute cardinalities.
     * Values are ordinals of each tuple, which can be used to aggregate the intersection to the leaf level of grouping.
     */
    HashMap<Long,Integer> tupleIndToAggregatorInd = new HashMap<Long, Integer>();

    /**
     * Maps a packed tuple index to its member Id on the newly created HierarchyLevel
     */
    HashMap<Long,Integer> tupleIndToNewMemberId = new HashMap<Long, Integer>();

    /**
     * Associate a packed tuple index with the new member Id for a newly created HierarchyLevel
     * @param tupleInd
     * @param newMemberId
     */
    public void setTupleIndToNewMemberId( long tupleInd, int newMemberId ) {
        tupleIndToNewMemberId.put( tupleInd, newMemberId );
    }


    /**
     * 
     * @param intersectionName
     * @return an array whose length equals to the intersection size. Its values are the
     *    index to the attribute values in the newly created hierarchy level.
     */
    public int[] getNewLevelIdsForIntersection( String intersectionName ) {
        ArrayList<Long> tupleInds = intersectionTupleInds.get(intersectionName);
        int[] newIds = new int[tupleInds.size()];
        for( int i=0; i<newIds.length; i++ ) {
            newIds[i] = tupleIndToNewMemberId.get( tupleInds.get(i));
        }
        return newIds;
    }

    int distinctTupleIndCount = 0;

    /**
     * Intersection name  -> tupleId  for the tuples found on an intersection
     */
    HashMap<String,ArrayList<Long>> intersectionTupleInds; // list size is the intersection size! this could be a huge cache.

    /**
     * Create a grouping of one or more attributes.
     * @param hierarchy
     */
    public AttributeGrouping(List<LevelAttribute> hierarchy) {
        // Make a private copy of the list
        this.hierarchy = new ArrayList<LevelAttribute>();
        for( LevelAttribute attribute: hierarchy ) {
            this.hierarchy.add( attribute );
        }
        
        cardinalities = new int[ hierarchy.size() ];
        for( int i=0; i<cardinalities.length; i++ ) {
            cardinalities[i] = hierarchy.get(i).getCardinality();
        }
        weights = Cartesian.unitPoly( cardinalities );

        intersectionTupleInds = new HashMap<String, ArrayList<Long>>();
    }

    /**
     * Given indices into the grouped attributes that occur on an Intersection, add any that have not already
     * been seen to the set of distinct tuples.  Also record which tuples occur at this intersection.
     * 
     * @param attrInds two dimension array. The size of the first dimension equals to the number of attributes
     * in the grouping. The size of the section dimension equals to the size of the intersection.
     * 
     * @param intersection an intersection
     */
    public void addTuples( int[][] attrInds, Intersection intersection ) {
        String intersectionName = intersection.getName();
        if ( attrInds.length != this.hierarchy.size()) {
            throw new CdsException("AttributeGrouping.addTuples():  attribute indices array does not match grouping depth");
        }

        ArrayList<Long> tupleInds = new ArrayList<Long>();
        int n = attrInds[0].length; // same as intersection size

        for( int i = 0; i < n; i ++ ) {
            long tupleIndex = getTupleIndex( attrInds, i );
            // have we seen this before on any intersection?
            if ( !this.tupleIndToAggregatorInd.containsKey(tupleIndex)) {
                this.tupleIndToAggregatorInd.put( tupleIndex, this.distinctTupleIndCount++ );
            }

            tupleInds.add( tupleIndex );
        }

        this.intersectionTupleInds.put( intersectionName, tupleInds );
    }


    /**
     *
     * @return The name of the leaf level attribute in the hierarchy.
     */
    public String getLeafAttributeName() {
        return hierarchy.get( hierarchy.size()-1 ).getAttrname();
    }

    /**
     *
     * @return the cardinalities of each of the attributes in the grouping.
     */
    public int[] getCardinalities() {
        return cardinalities;
    }


    /**
     *
     * @return The cardinality, or cumulative number of distinct tuples of attribute values found for all Intersections.
     * This will be the number of distinct members that will be created in a HiearchyLevel built from this grouping.
     */
    public int getCardinality(  ) {
        return tupleIndToAggregatorInd.size();
    }

    /**
     * @return The unique packed tuple indices that were found on an intersection.
     */
    public long[] getTupleInds( String intersectionName ) {
        ArrayList<Long> tuplesOnIntersection = this.intersectionTupleInds.get( intersectionName );
        return ArrayOps.getLongs(tuplesOnIntersection);
    }

    /**
     *
     * @param intersectionName
     * @return  The mapping between the items on the Intersection and the distinct tuples of this grouping.
     */
    public int[] getTupleMapping(String intersectionName){
        ArrayList<Long> tuplesOnIntersection = this.intersectionTupleInds.get( intersectionName );
        int[] distinctTupleInds = new int[tuplesOnIntersection.size()];
        for(int i = 0; i < tuplesOnIntersection.size(); i ++){
            long tupleIndex = tuplesOnIntersection.get(i);
            int aggregatorInd = tupleIndToAggregatorInd.get(tupleIndex);
            distinctTupleInds[i] = aggregatorInd;
        }
        return distinctTupleInds;
    }




    /**
     * Calculate the tuple index of a combination of attribute values. The attribute values are identified
     * by their indices. Its opposite operation is {@link #getAttributeInds(long)}.
     * 
     * @param attrIds An index into each of the LevelAttributes. The length of the array should be the
     * same as the number of attributes in the grouping.
     * @return  The packed linear offset in the tuple space selected by an index into each attribute
     */
    public long getTupleIndex( int[] attrIds ) {
        if ( attrIds.length != cardinalities.length )
            throw new CdsException("AttributeGrouping.getTupleIndex():  attrIds length must match number of levels in the attribute hierarchy");
        long result = 0;
        for( int i=0; i<attrIds.length; i++ )
            result += attrIds[i]*weights[i];
        return result;
    }


    /**
     * Calculate the tuple index of a combination of attribute values. The attribute values are identified
     * by their indices, stored at a slice of the given two dimension array at the given position.
     * 
     * @param attrIdsArrays Indices into the level attributes, one array for each level attribute.  All must be equal length.
     * @param posn The offset in the index arrays for the tuple selected.
     * @return  The packed linear index in the tuple space selected by an index in an array
     */
    public long getTupleIndex( int[][] attrIdsArrays, int posn ) {
        long result = 0;
        for( int i=0; i<attrIdsArrays.length; i++ ) {
            result += (attrIdsArrays[i][posn] * weights[i]);
        }
        return result;
    }

    /**
     * Unpack a linear index in the tuple space, returning the position in each of the level attributes.
     * This is the opposite of {@link #getTupleIndex(int[])}.
     * 
     * @param tupleIndex
     * @return  The position in each of the level attributes.
     */
    public int[] getAttributeInds( long tupleIndex ) {
        int[] result = new int[weights.length];
        for( int i=0; i<result.length; i++ ) {
            result[i] = (int)(tupleIndex/weights[i]); // should be safe to convert to int
            tupleIndex -= result[i]*weights[i];
        }
        return result;
    }


    /**
     * @param tupleIndex
     * @return  The attribute values separated by "-" for the packed tuple index.  Primarily for diagnostic use.
     */
    public String getAttributeLabel( long tupleIndex ) {
        int[] inds = getAttributeInds( tupleIndex );
        StringBuilder labelBuilder = new StringBuilder();
        for( int i=0; i<inds.length; i++ ) {
            if ( i>0 ) 
            	labelBuilder.append("-");
            labelBuilder.append(this.hierarchy.get(i).getValue(inds[i]));
        }
        return labelBuilder.toString();
    }



}
