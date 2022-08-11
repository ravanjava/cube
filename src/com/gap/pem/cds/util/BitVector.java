package com.gap.pem.cds.util;


import java.io.Serializable;
import java.util.BitSet;

import com.gap.pem.cds.CdsException;

/**
 * A boolean vector implementation that knows its actual fixed size, unlike BitSet, with bounds checking and all.
 * Provides a useful subset of the bit vector operations of SmartArrays.
 *
 */
public class BitVector implements Serializable {

    private static final long serialVersionUID = 5739628510127161507L;
    
    private BitSet bitSet;
    private int size;

    /**
     * Construct vector of specified size with every bit set to false
     * @param size
     */
    public BitVector( int size ) {
        if ( size<0 )
            throw new IllegalArgumentException();
        this.size = size;
        bitSet = new BitSet(size);
    }

    /**
     * Wrap a BitSet as a BitVector.  Note that it does not make a distinct copy of the source BitSet.
     * @param bits
     */
    public BitVector( BitSet bits ) {
        this( bits == null ? new BitSet() : bits, bits == null ? 0 : bits.length());
    }


    /**
     * Wrap a BitSet as a BitVector.  Note that it does not make a distinct copy of the source BitSet
     * @param bits
     * @param size The fixed size of the bit vector.
     */
    public BitVector( BitSet bits, int size ) {
        // Wrap a BitSet; note that this does NOT make a distinct copy.  Java's BitSet instances are allocated
        // based on the position of the highest "true" bit, so a BitSet that is all "false" has an empty long[]
        // on the inside.
        if ( size < 0 )
            throw new IllegalArgumentException();
        
        this.size = size;
        this.bitSet = bits;
    }

    /**
     * Get the fixed size.
     * @return Number of bits in the BitVector.
     */
    public int size() {
        return size;
    }

    /**
     * Construct from a boolean array. Size of BitVector is same as length of the array.
     * @param bits
     */
    public BitVector( boolean[] bits ) {
        size = bits.length;
        bitSet = new BitSet(bits.length);
        for( int i=0; i<bits.length; i++ )
            bitSet.set( i, bits[i] );
    }

    /**
     * Access the internal BitSet.
     * @return A reference to the internal BitSet
     */
    public BitSet getBitSet() {
        return bitSet;
    }

    /**
     * Get the values of all bits as a boolean array.
     * @return Values as a boolean[].  Note that this will take more memory than the BitVector.
     */
    public boolean[] getBooleans() {
        boolean[] result = new boolean[size];
        for( int i=0; i<size; i++ )
            result[i] = bitSet.get(i);
        return  result;
    }

    /**
     * @param i
     * @return The boolean value at specified index
     */
    public boolean get( int i ) {
        if ( i<0 || i>=size )
            throw new IndexOutOfBoundsException();
        return bitSet.get(i);
    }

    /**
     * Set boolean value at specified index
     * @param i
     * @param b
     */
    public void set( int i, boolean b ) {
        if ( i<0 || i>=size )
            throw new IndexOutOfBoundsException();
        bitSet.set(i,b);
    }

    /**
     * Set all bits to the same value
     * @param b  The value to set all bits to.
     */
    public void setAll( boolean b ) {
        bitSet.set(0, size, b);
    }


    /**
     * Count the number of values of True in the vector
     * @return The number of bits that are true.
     */
    public int sum() {
    	return bitSet.cardinality();
    }


    /**
     * Does the vector contain any bits that are True?
     * @return True if any bit is True
     */
    public boolean any() {
    	return (bitSet.nextSetBit(0) >= 0);
    }

    /**
     * Are all the bits set to True?
     * @return True if every bit is true.
     */
    public boolean all() {
    	return (bitSet.cardinality()== size);
    }

    /**
     * Find the locations of bits whose value is True.
     * @return An int[] with the position of each True value in the vector
     */
    public int[] where() {
        int[] result = new int[bitSet.cardinality()];
        int iresult = 0;
        for( int i=0; i<size; i++ ) {
            if ( bitSet.get(i) )
                result[iresult++]=i;
        }
        return result;
    }

    // ======= Logical and set operations that create a new BitVector as the result

    /**
     * Compute logical AND with another BitVector.
     * @param bv  The other bit vector; must be the same length.
     * @return New BitVector containing the logical AND of this and param bv.
     */
    public BitVector and( BitVector bv ) {
        // TODO: use internal long[]
        BitVector result = copy();
        result.andInto(bv);
        return result;
    }

    /**
     * Computer logical OR with another BitVector.
     * @param bv  The other bit vector; must be the same length.
     * @return  New BitVector containing the logical OR of this and param bv.
     */
    public BitVector or( BitVector bv ) {
        // TODO: use internal long[]
        BitVector result = copy();
        result.orInto(bv);
        return result;
    }

    /**
     * Compute logical exclusive or (XOR) with another BitVector.
     * @param bv  The other bit vector; must be the same length.
     * @return New BitVector containing the logical exclusive-or of this and param bv.
     */
    public BitVector xor( BitVector bv ) {
        // TODO: use internal long[]
        BitVector result = copy();
        result.xorInto(bv);;
        return result;

    }

    /**
     * Compute the logical negation of this bit vector.
     * @return New BitVector that is True where this is False, and vice versa.
     */
    public BitVector not() {
        BitVector result = copy();
        result.bitSet.flip(0,size);
        return result;
    }


    // === Set operations ================================

    /**
     * Set intersection: Are any bits set in both this and another bit vector?
     * @param bv The other bit vector; must be equal length.
     * @return  True if any set bit in the given vector is also
     * set in this vector.
     */
    public boolean intersects( BitVector bv ) {
    	BitSet thisBitSet = this.bitSet;
    	BitSet thatBitSet = bv.bitSet;
    	
    	return thisBitSet.intersects(thatBitSet);
    }

    /**
     * Subset:  Are the bits in set in this bit vector a superset of those set in another bit vector?
     * @param bv The other bit vector; must be equal length.
     * @return True if every bit set in bv is also set in this.
     */
    public boolean contains( BitVector bv ) {
    	BitSet thisBitSet = this.bitSet;
    	BitSet thatBitSet = bv.bitSet;



		// 0110 and 1110 = 0110
    	BitSet copy = (BitSet)thatBitSet.clone();
    	copy.and(thisBitSet);
    	return copy.equals(thatBitSet);
    }


    /**
     * Set intersection: Bits that are set both in this and another bit vector.
     * @param bv The other bit vector; must be equal length.
     * @return New BitVector that is true for each position where this and param bv are both true, in other
     * words the logical AND.
     */
    public BitVector intersectionWith( BitVector bv ) {
        return this.and(bv); // synonym
    }

    /**
     * Set union: Bits that are set either in this or another bit vector.
     * @param bv The other bit vector, must be equal length.
     * @return New BitVector that is true for each position where either this or param bv are both true, in other
     * words, the logical OR.
     */
    public BitVector unionWith( BitVector bv ) {
        return this.or(bv); //synonym
    }

    /**
     * Set difference:  Bit that are set in this bit vector and NOT set in another bit vector.
     * @param bv The other bit vector, must be equal length.
     * @return  BitVector marking bits that are set in this and are not set in param
     */
    public BitVector differenceWith( BitVector bv ) {
        BitVector result = copy();
        result.andNotInto(bv);
        return result;
    }


    // future: add other boolean ops as needed; take advantage of full word logical operations; etc.

    /**
     * Sets each bit in this BitVector to the logical AND of its current value and the value in param bv.
     * @param bv The other bit vector; must be equal length.
     */
    public void andInto( BitVector  bv ) {
        if ( bv.size != this.size ) {
            throw new CdsException(
                    "BitVector.andInto(): argument has size " + bv.size
                            + "; cannot be combined with bit vector of size " + this.size);
        }
        this.bitSet.and( bv.bitSet);
    }

    /**
     * Sets each bit in this BitVector to the logical OR of its current value and the value in param bv.
     * @param bv  The other bit vector; must be equal length.
     */
    public void orInto( BitVector bv ) {
        if ( bv.size != this.size ) {
            throw new CdsException(
                    "BitVector.andInto(): argument has size " + bv.size
                            + "; cannot be combined with bit vector of size " + this.size);
        }
        this.bitSet.or( bv.bitSet);
    }

    /**
     * Sets each bit in this BitVector to the logical XOR of its current value and the value in param bv.
     * @param bv  The other bit vector; must be equal length.
     */
    public void xorInto( BitVector bv ) {
        if ( bv.size != this.size ) {
            throw new CdsException(
                    "BitVector.andInto(): argument has size " + bv.size
                            + "; cannot be combined with bit vector of size " + this.size);
        }
        this.bitSet.xor( bv.bitSet);
    }

    /**
     * Sets each bit in this BitVector to the logical AND of its current value and the negation of the
     * corresponding value in param bv.
     * @param bv The other bit vector; must be equal length.
     */
    public void andNotInto( BitVector bv ) {
        if ( bv.size != this.size ) {
            throw new CdsException(
                    "BitVector.andInto(): argument has size " + bv.size
                            + "; cannot be combined with bit vector of size " + this.size);
        }
        this.bitSet.andNot( bv.bitSet);
    }


    /**
     * Create a distinct instance with the same values
     * @return New BitVector.
     */
    public BitVector copy() {
        return new BitVector(  (BitSet) this.bitSet.clone(), this.size );
    }

    /**
     * @param o
     * @return True if o is instance of BitVector and every bit in this matches the corresponding bit in param o.
     */
    @Override
    public boolean equals ( Object o ) {
        if ( o instanceof BitVector )
            return equals(  (BitVector)o );
        return false;
    }

    /**
     * Is this bit vector equal in size and content to another?
     * @param that The other bit vector; must be equal length.
     * @return True if both BitVectors have the same length and every bit has the same value.
     */
    public boolean equals( BitVector that ) {

        if ( this.size != that.size )
            return false;

        return bitSet.equals( that.bitSet );
    }


    @Override
    public int hashCode() {
        return bitSet.hashCode() * size;
    }



    @Override
    public String toString() {
        return "BitVector{" +
                "bitSet=" + bitSet +
                ", size=" + size +
                '}';
    }
}


