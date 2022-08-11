
package com.ram.ds.cds.util;

import java.util.*;

import com.ram.ds.cds.stores.IIntStore;

/**
 * Replacements for various SmartArrays operations used in original SMDAP code

 */
public class ArrayOps 
{
    private static Random random = null;
   
    private static Random getRandom() {
        if ( random==null )
            random = new Random( randomSeed );
        return random;
    }

    // for repeatable randomness
    private static int randomSeed = 7*7*7*7*7;


    /**
     * Insert the given integer to the given list as the first element.
     * 
     * @param v the value to insert
     * @param list a list of existing values.
     * @return  a list with the given value as its first element, followed by
     * the elements in the given list.
     */
    public static ArrayList<Integer> insertLeading( int v, ArrayList<Integer> list ) {
        ArrayList<Integer> result = new ArrayList<Integer>( 1 + list.size() );
        result.add(v);
        result.addAll( list );
        return result;
    }


    /**
     * Create a String array of the given size, and populate it with values
     * randomly chosen from the given source array.
     * 
     * @param count the size of the array to be created.
     * @param source the collection of possible values the new array will be
     *               populated with.
     * @return a new String array.
     */
    public static String[] randomFromPopulation( int count, String[] source ) {
        Random r = new Random();
        String[] result = new String[count];
        for( int i=0; i<count; i++ )
            result[i] = source[ r.nextInt( source.length ) ];
        return result;
    }

    /**
     * Create an integer array of the given size, and populate it with values
     * randomly chosen from the given source array.
     *
     * @param count the size of the array to be created.
     * @param source the collection of possible values the new array will be
     *               populated with.
     * @return a new array.
     */
    public static int[] randomFromPopulation( int count, int[] source ) {
        Random r = new Random();
        int[] result = new int[count];
        for( int i=0; i<count; i++ )
            result[i] = source[ r.nextInt( source.length )];
        return result;
    }

    /**
     * Collect the values in the first array whose indices are given
     * in the second array.
     * 
     * @param values the values to collect the value from.
     * @param inds an array of indices into the values array.
     * @return a new array of the same size as the given indices.
     */
    public static int[] index( int[] values, int[] inds ){
        int[] result = new int[inds.length];
        for( int i=0; i<inds.length; i++ )
            result[i] = values[inds[i]];
        return result;
    }

    /**
     * Collect the values in the first array whose indices are given
     * in the integer store.
     * 
     * @param values the values to collect the value from.
     * @param inds the store that holds the indices into the values array.
     * @return a new array of the same size as the given store.
     */
    public static int[] index( int[] values, IIntStore inds ) {
        int[] result = new int[inds.size()];
        for( int i=0; i<result.length; i++ )
            result[i] = values[ inds.getElement(i)];
        return result;
    }

    /**
     * Collect the bits in the given BitSet. The indices of the bits
     * are in the integer array.
     * 
     * @param values the BitSet to collect the value from.
     * @param inds an array of indices into the BitSet.
     * @return a new BitSet of the same size as the given indices.
     */
    public static BitSet index( BitSet values, int[] inds ) {
        BitSet result = new BitSet( inds.length );
        for( int i=0; i<inds.length; i++ )
            result.set( i, values.get(inds[i]));
        return result;
    }

    /**
     * Collect the bits in the given BitVector. The indices of the bits
     * are in the integer array.
     *
     * @param values the BitSet to collect the value from.
     * @param inds an array of indices into the BitSet.
     * @return a new BitSet of the same size as the given indices.
     */
    public static BitVector index( BitVector values, int[] inds ) {
        BitSet resultBitSet = index( values.getBitSet(), inds );
        return new BitVector( resultBitSet, inds.length );
    }

    /**
     * Generate an array whose values are distinct but selected randomly  
     * from the range of [0,size].
     * 
     * @param count must be less or equal to size
     * @param size
     * @return integers randomly selected from the range of [0,size] without replacement.
     */
    public static int[] deal ( int count, int size ) {
        if ( count > size ) 
        	throw new RuntimeException("ArrayOps.deal(): count must be <= size ");
        
        int[] result = new int[count];
        int[] sizeitems = Sequence.getSequence( size );
        for( int i=0; i<result.length; i++ ) {
            int item = getRandom().nextInt(size);
            result[i] = sizeitems[item];
            sizeitems[item] = sizeitems[size = size-1 ];
        }
        return result;
    }

    /**
     * Test if two int[] arrays are identical in length and content
     * @param a
     * @param b
     * @return true if arrays have identical length and content.
     */
    public static boolean match( int[] a, int[] b ) {
        if ( a.length != b.length )
            return false;
        
        for( int i=0; i<a.length; i++ ) 
        	if ( a[i] != b[i] ) 
        		return false;
        return true;
    }

    /**
     * Test if two int[] arrays are identical in length and content
     * @param a
     * @param b
     * @return true if arrays have identical length and content.
     */
    public static boolean match( long[] a, long[] b ) {
        if ( a.length != b.length )
            return false;
        
        for( int i=0; i<a.length; i++ ) 
        	if ( a[i] != b[i] ) 
        		return false;
        return true;
    }
    
    /**
     * Test if two double[] arrays are identical in length and content. 
     * Double values are compared by ==.
     * 
     * @param a
     * @param b
     * @return true if arrays match.
     */
    public static boolean match( double[] a, double[] b ) {
        if ( a.length != b.length )
            return false;
        for( int i=0; i<a.length; i++ ) {
            if ( a[i] != b[i] )
                return false;
        }
        return true;
    }

    /**
     * Test if two String arrays are identical in length and content. There
     * is not supposed to be null in the first array.
     * 
     * @param a
     * @param b
     * @return True if the arrays are the same length and have equal values at every position.
     */
    public static boolean match( String[] a, String[] b ) {
        if ( a.length != b.length )
            return false;

        for( int i=0; i<a.length; i++ ) {
            if ( ! a[i].equals( b[i]))
                return false;
        }
        return true;
    }


    /**
     * Test if two boolean arrays are identical in length and values
     * @param a
     * @param b
     * @return True if the arrays are the same length and have equal values at every position.
     */
    public static boolean match( boolean[] a, boolean[] b ) {
        if( a.length != b.length )
            return false;

        for( int i=0; i<a.length; i++ ) {
            if ( ! a[i]==b[i] )
                return false;
        }
        return true;
    }


    /**
     * Remove duplicates from array, returning result as new array with items in their original order.
     * 
     * @param inputs
     * @return Array of distinct values found in inputs, in the order found.
     */
    public static int[] unique( int[] inputs ) {
        // Note:  there may be some advantage to using Trove containers here. This is a crude substitute
        // for the algorithm used in SmartArrays.
        HashSet<Integer> seen = new HashSet<Integer>();
        ArrayList<Integer> uv = new ArrayList<Integer>(); // to maintain stable ordering
        for (int input : inputs) {
            if (seen.contains(input))
                continue; //
            seen.add(input);
            uv.add(input);

        }
        int[] result = new int[uv.size()];
        for( int i=0; i<result.length; i++ )
            result[i] = uv.get(i);
        return result;
    }

    /**
     * Remove duplicates from array, returning result as new array with items in their orignial order.
     * 
     * @param inputs
     * @return Array of distinct values found in inputs, in the order found.
     */
    public static String[] unique( String[] inputs ) {
        HashSet<String> seen = new HashSet<String>();
        ArrayList<String> uv = new ArrayList<String>();
        for ( String input : inputs ) {
            if (seen.contains(input))
                continue;
            seen.add(input);
            uv.add(input);
        }
        String[] result = new String[uv.size()];
        for( int i=0; i<result.length; i++ )
            result[i] = uv.get(i);
        return result;
    }

    /**
     * Reduction:  Smallest value in array
     * @param values
     * @return Smallest value in the array.  For empty arrays returns Integer.MAX_VALUE.
     */
    public static int min( int[] values ) {
        int minValue = Integer.MAX_VALUE;
        for( int a : values ) 
        	if ( a < minValue ) 
        		minValue = a;
        return minValue;
    }

    /**
     * Reduction: Largest value in array
     * @param values
     * @return Largest value in the array. For empty arrays returns Integer.MIN_VALUE.
     */
    public static int max( int[] values ) {
        int maxValue = Integer.MIN_VALUE;
        for( int a : values ) 
        	if ( a > maxValue ) 
        		maxValue = a;
        return maxValue;
    }

    /**
     * Reduction: Largest value in array
     * @param values
     * @return Largest value in the array. For empty arrays returns Long.MIN_VALUE.
     */
    public static long max( long[] values ) {
        long maxValue = Long.MIN_VALUE;
        for( long a : values ) 
        	if ( a > maxValue ) 
        		maxValue = a;
        return maxValue;
    }


    /**
     * Reduction:  product of values of array.  Caution: Does not detect overflows.
     * @param values
     * @return Product of the integer values in the array, or 1 if the array is empty.
     */
    public static int prod( int[] values ) {
        // simple and dumb, no overflow detection
        int prodValue = 1;
        for( int a : values ) prodValue *= a;
        return prodValue;
    }

    /**
     * Reduction:  product of values of array.  Caution: Does not detect overflows.
     * @param values
     * @return Product of the integer values in the array, or 1 if the array is empty.
     */
    public static int prod( Integer[] values ) {
        // simple and dumb, no overflow detection
        int prodValue = 1;
        for( int a : values ) prodValue *= a;
        return prodValue;
    }

    /**
     * @param values The source array
     * @return Distinct copy of values.
     */
    public static int[] copy( int[] values ) {
        int[] result = new int[values.length];
        for( int i=0; i<values.length; i++ )
            result[i] = values[i];
        return result;
        // Note: it is possible that there may be a slight performance advantage to using System.arraycopy here.
    }

    /**
     * Helper to convert ArrayList&lt;Integer&gt; to int[].
     * @param data
     * @return Primitive int[] array.
     */
    public static int[] getInts( ArrayList<Integer> data ) {
        // grrr...  ArrayList should have a method to demote to primitive array
        int[] result = new int[ data.size() ];
        for( int i=0; i<result.length; i++ )
            result[i] = data.get(i);
        return result;
    }

    /**
     * Helper to convert Integer[] to int[]
     * @param data
     * @return Primitive int[] array.
     */
    public static int[] getInts( Integer[] data ) {
        // grrrr again
        int[] result = new int[ data.length ];
        for( int i=0; i<result.length; i++ ) {
            result[i] = data[i];
        }
        return result;
    }


    /**
     * Helper to convert ArrayList&lt;Long&gt; to long[].
     * @param data
     * @return Primitive long[] array.
     */
    public static long[] getLongs( ArrayList<Long> data ) {
        // grrr...  ArrayList should have a method to demote to primitive array
        long[] result = new long[ data.size() ];
        for( int i=0; i<result.length; i++ )
            result[i] = data.get(i);
        return result;
    }

    /**
     * Helper to convert Long[] to long[]
     * @param data
     * @return Primitive long[] array.
     */
    public static long[] getInts( Long[] data ) {
        // grrrr again
        long[] result = new long[ data.length ];
        for( int i=0; i<result.length; i++ ) {
            result[i] = data[i];
        }
        return result;
    }

    /**
     * Find if array values contains the value x
     * @param x
     * @param values
     * @return true if array values contains the value x
     */
    public static boolean member( int x, int[] values ) {
        // crude brute force substitute for the SmartArrays operation
        // should be implemented more cleverly if any heavy duty use 
    	// is intended.
        for( int v : values ) {
            if ( v==x )
                return true;
        }
        return false;
    }

    /**
     * Find if array values contains the value x
     * @param x
     * @param values
     * @return true if array values contains the value x
     */
    public static boolean member( long x, long[] values ) {
        // crude brute force substitute for the SmartArrays operation
        // should be implemented more cleverly if any heavy duty use 
    	// is intended.
        for( long v : values ) {
            if ( v==x )
                return true;
        }
        return false;
    }


    /**
     * Find the first position in a String[] that equals s.
     * @param s
     * @param values
     * @return Position of first match, or -1 if none is found.
     */
    public static int lookup( String s, String[] values ) {
        // slow way, but not too bad if the arrays are short
        for ( int i=0; i<values.length; i++ ) {
            if ( s.equals( values[i] ) )
                return i;
        }
        return -1;
    }

    /**
     * Get a bit set that was set to true for every element in the 
     * array that equals to the given value.
     * 
     * @param values
     * @param v
     * @return BitVector of true for every array item that matches specified value.
     */
    public static BitVector eq( int[] values, int v ) {
        BitVector result = new BitVector(values.length);
        for( int i=0; i<values.length; i++ )
            if ( values[i] == v )
                result.set(i,true);
        return result;
    }
}


