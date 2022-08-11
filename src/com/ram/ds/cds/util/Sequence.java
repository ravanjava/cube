package com.ram.ds.cds.util;

import java.util.Iterator;

/**
 * Sequence generator: Numbers from 0 to n with a repetition count; cycles until total number of items have been
 * generated, then returns -1.

 */
public class Sequence implements Iterator<Integer> 
{
    private int sequenceCount;  // number of distinct value in the sequence; resets to zero when this is reached.
    private int repeatCount;    // number of times each value is repeated before incrementing
    private int totalCount;     // total number of items to generate before sequence ends
    private int resultCount;    // total number that have been generated
    private int next;			// the next value to be generated for the sequence.
    private int repetitions; 

    /**
     * Create a sequence generator that will produce numbers from 0 to the
     * given sequence count (exclusive), controlled by the given 
     * repeatCount and totalCount. 
     * <p>
     * For example, if sequence count is 3, repeat count is 2, and
     * total count is 9, then the sequence of numbers generated will
     * be [0,0,1,1,2,2,0,0,1,-1,-1,-1,...].
     * 
     * @param sequenceCount
     * @param repeatCount
     * @param totalCount
     */
    public Sequence( int sequenceCount, int repeatCount, int totalCount ) {
        this.sequenceCount = sequenceCount;
        this.repeatCount = repeatCount;
        this.totalCount = totalCount;
        this.resultCount = 0;
        this.next = 0;
        this.repetitions = repeatCount;
    }


    /**
     * Create a sequence generator that will produce numbers from 0 to the
     * given sequence count (exclusive).
     * 
     * @param sequenceCount
     */
    public Sequence( int sequenceCount ) {
        this( sequenceCount, 1, sequenceCount );
    }

    /**
     * Get the next number in sequence. Successive invocations of this
     * method will produce a number from 0 to sequence count, until the
     * number of numbers generated has reached total count. At which 
     * point, the method will return -1 for all subsequent invocations.
     * <p>
     * For example, if sequence count is 3, repeat count is 2, and
     * total count is 9, then the sequence of numbers generated will
     * be [0,0,1,1,2,2,0,0,1,-1,-1,-1,...].
     *  
     * @return Next value in the sequence.
     */
    public int getNext() {
        if ( ++ resultCount > totalCount ) 
        	return -1;
        
        int result = next;
        if ( --repetitions == 0 ) {
            repetitions = repeatCount;
            next++;
        }
        // otherwise still repeating
        if ( next >= sequenceCount )
            next = 0; // cycle back

        return result;
    }

    /**
     * Generate an array containing the entire sequence. The length
     * of the array equals total count.
     * 
     * @return an array that contains the sequence of numbers
     * that are generated as if the client has repeatedly 
     * invoked the next() until hasNext() returns false.
     */
    public int[] toArray() {
        int[] result = new int[totalCount];
        for( int i=0; i<totalCount; i++ )
            result[i] = getNext();
        return result;
    }


    /**
     * Generate array of successive integers starting with zero.
     * 
     * @param size
     * @return an array that holds numbers from 0 to size-1.
     */
    public static int[] getSequence( int size ) {
        int[] result = new int[size];
        for( int i=0; i<size; i++ ) 
        	result[i] = i;
        return result;
    }

    @Override
    public boolean hasNext() {
        return resultCount <= totalCount;
    }

    @Override
    public Integer next() {
        return getNext();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "Sequence{" +
                "sequenceCount=" + sequenceCount +
                ", repeatCount=" + repeatCount +
                ", totalCount=" + totalCount +
                ", resultCount=" + resultCount +
                ", next=" + next +
                ", repetitions=" + repetitions +
                '}';
    }
}

