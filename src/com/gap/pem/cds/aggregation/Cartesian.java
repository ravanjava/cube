package com.gap.pem.cds.aggregation;


import com.gap.pem.cds.CdsException;

/**
 * Utilities for manipulating Cartesian spaces mapped to a linear array offset.
 *
 * <p>Offsets in each of the dimensions must fit within a positive 32-bit integer, therefore the total size of the
 * state space must be no larger than 2**31-1.  </p>
 * @author James Wheeler
 *  @author  Last updated by $Author: misong $
 *  @version $Revision: 1.6.2.1 $
 */
public class Cartesian {

    /** Private constructor to prevent instantiation. */
    private Cartesian() {
    }


    /**
     * Validate that the state space described by a set of dimensions fits within the 32-bit Integer domain.
     * In other words, the multiplication of the given cardinalities are within the range from 
     * Integer.MIN_VALUE to Integer.MAX_VALUE, inclusive at both ends.
     * 
     * @param cardinalities
     * @return  True if the state space is in the valid domain; false if it may overflow a 32-bit Integer.
     * @throws CdsException if any of the cardinalities passed is negative.
     */
    public static boolean checkDimensionality( int[] cardinalities ) {
        long prod = 1;
        for( int cardinality : cardinalities ) {
            if ( cardinality < 0 )
                throw new CdsException("Cartesian:  Negative value for cardinality is not allowed");
            prod *= cardinality;
            if ( prod > Integer.MAX_VALUE || prod < Integer.MIN_VALUE )
                return false;
        }
        return true;
    }

    /**
     * Given a set of attribute cardinalities and positions, pack each position into a
     * 
     * @param cardinalities   The cardinality of the attribute associated with each array of coefficients
     * @param coefficients  Equal length arrays of coefficients with values between 0 and cardinalities[i]-1
     * 
     * @return packed linear subscripts in the dimensional space for each set of coefficients. The length of
     * the result equals to the number of dimensions. The value for each dimension is the packed value of the
     * coefficients in the dimension.
     */
    public static long[] pack( int[] cardinalities, int[][] coefficients ) {
        if ( cardinalities.length != coefficients.length )
            throw new CdsException("Cartesian.pack: requires equal one coefficient array for each cardinality");
        if ( cardinalities.length == 0 ) {
            throw new CdsException("Cartesian.pack: empty array argument not supported");
        }
        
        /** 
         * Make sure the state space fits within a 32-bit Integer.
         */
        if ( !checkDimensionality( cardinalities ))
            throw new CdsException("Cartesian.pack: Total cardinality overflows 32-bit integer.");


        // All coefficient vectors must be the same length
        int resultLength = -1;
        for( int i=0; i<coefficients.length; i++ ) {
            if ( i==0 ) resultLength = coefficients[i].length;
            else {
                if ( coefficients[i].length != resultLength )
                    throw new CdsException("Cartesian.pack requires all coefficient vectors to have the same length");
            }
        }

        long[] weights = unitPoly( cardinalities );
        long[] result = new long[resultLength];
        for( int i=0; i<resultLength; i++ ) {
            int packed = 0;
            for( int axis=0; axis<cardinalities.length; axis++ ) {
                packed += weights[axis] * coefficients[axis][i];
            }
            result[i] = packed;
        }

        return result;
    }

    /**
     * Compute the packed value of a set of coefficients of the same polynomial.
     * Assume [w1, w2, w3] and [c1, c2, c3] as inputs:
     *
     * result = 1 x c3 + 1 x w3 x c2 + 1 x w3 x w2 x c1.
     *
     * @param cardinalities the cardinalities of the dimensions.
     * @param positions an int for each dimension, i.e. the position in the dimension
     * @return packed coefficient.
     */
    public static long pack( int[] cardinalities, int[] positions ) {
        if (cardinalities.length != positions.length )
            throw new CdsException("Cartesian.pack(): weights and coefficients are not the same length");
        if ( cardinalities.length == 0 )
            throw new CdsException("Cartesian.pack(): weights and coefficients must have length > 0");
        
        long result =  0;
        int weight = 1;
        for( int i= positions.length-1; i >= 0; i -- ) {
            result += positions[i] * weight;
            weight *= cardinalities[i];
        }
        return result;
    }

    /**
     * Unpack an integer value into its indices in a Cartesian space
     * @param cardinalitites The length of each dimension
     * @param p  The packed value
     * @return The unpacked indices. The size of the result is the same as the 
     * given cardinalities.
     */
    public static int[] unpack( int[] cardinalities, long tupleIndex ) {
        int[] result = new int[ cardinalities.length ];
        long[] poly = unitPoly(cardinalities);
        for( int i=0; i<poly.length; i++ ) {
            result[i] = (int)(tupleIndex / poly[i]); // should be safe to convert to int
            tupleIndex = tupleIndex % poly[i];
        }
        return result;
    }


    /**
     * Given the sizes (weights) of a Cartesian space, compute the difference in linear offset for
     * an increment of 1 along each of the dimensions.
     * 
     * @param lengths
     * @return The weighted increment for each dimension. The size of the result equals to the
     * size of the given array.
     */
    public static long[] unitPoly( int[] cardinalities ) {
        long[] offsets = new long[cardinalities.length];
        for( int i = offsets.length-1; i >= 0; i -- ) {
            if ( i== offsets.length-1)
            	offsets[i] = 1;
            else
            	offsets[i] = cardinalities[i+1] * offsets[i+1];
        }
        return offsets;
    }
}
