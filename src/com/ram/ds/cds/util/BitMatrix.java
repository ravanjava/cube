package com.ram.ds.cds.util;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;

import com.ram.ds.cds.CdsException;
import com.ram.ds.cds.stores.BitSetStore;

/**
 * Represents a 2-dimensional array of bits, stored sparsely using BitSetStore
 */
public class BitMatrix extends BitSetStore implements Serializable 
{
    private static final long serialVersionUID = 3798628502137161577L;
    
    private int nrows;
    private int ncols;
    private int cardinality;
    
    public BitMatrix( int nrows, int ncols ) {
        super();
        ensureSize( nrows ); // may not be necessary at this stage. It does preallocate storage for all rows
        this.nrows = nrows;
        this.ncols = ncols;
        this.cardinality = 0;
    }
    
    public int getRowCount() {
    	return this.nrows;
    }
    
    public int getColCount() {
        return this.ncols;
    }

    public int cardinality(){
    	return this.cardinality;
    }

    /**
     * FIXME consider removing this overridden implementation. It is not memory efficient.
     */
    @Override
    public BitSet getElement(int index) {
        BitSet result = super.getElement(index);
        if ( result == null ) {
            result = new BitSet();
            super.setElementAt(index, result);
        }
        return result;
    }

    /**
     *
     * @param index
     * @return The bitset for an item wrapped as a fixed-length BitVector
     */
    public BitVector getBitVector( int index ) {
        return new BitVector( getElement(index), ncols );
    }
    
    @Override
    public int addElement(BitSet value) {
        if ( value != null && value.length() > ncols )   // note:  we must accept length < ncols because of BitSet implementation
            throw new CdsException("BitMatrix.addElement() - BitSet is not the correct length ");
        
        int index = super.addElement(value);
        int newCount = (value == null ? 0 : value.cardinality());
        this.cardinality += newCount;
        return index;
    }

    @Override
    public BitSet setElementAt(int index, BitSet value) {
        if ( value != null && value.length() > ncols )   // note:  we must accept length < ncols because of BitSet implementation
            throw new CdsException("BitMatrix.setElementAt() - BitSet is not the correct length ");
        
        int newCount = (value == null ? 0 : value.cardinality());
        BitSet oldValue = super.setElementAt(index, value);
        int oldCount = (oldValue == null ? 0 : oldValue.cardinality());
        this.cardinality += (newCount - oldCount);
        return oldValue;
    }

    @Override
    public boolean setElementBit(int elementIndex, int bitIndex, boolean value)
    {
    	boolean oldValue = super.setElementBit(elementIndex, bitIndex, value);
    	if(oldValue == false && value == true){
    		this.cardinality ++;
    	} 
    	else if(oldValue == true && value == false){
    		this.cardinality --;
    	}
    	return oldValue;
    }

    public int addElement( BitVector value ) {
        if( value.size() != ncols ) {
            throw new CdsException("BitMatrix.addElement( BitVector) - vector length does not match number of columns in the matrix");
        }
        return addElement( value.getBitSet());
    }

    public BitVector setElementAt( int index, BitVector value ) {
        if( value.size() != ncols ) {
            throw new CdsException("BitMatrix.addElement( BitVector) - vector length does not match number of columns in the matrix");
        }
        BitSet prev =  setElementAt(index, value.getBitSet());
        return new BitVector( prev, ncols );
    }
    
    public void clear() {
    	for(int i = 0; i < this.nrows; i ++){
    		super.setElementAt(i, this.getEmptyValue());
    	}
    	this.cardinality = 0;
    	// do not change nrows and ncols
    }

    public void setAllBits() {
    	for(int i = 0; i < this.nrows; i ++){
    		getBitVector(i).setAll(true);
    	}
    	this.cardinality = nrows*ncols;
    	// do not change nrows and ncols
    }
    
    public void setAllFirstBits() {
    	for(int i = 0; i < this.nrows; i ++){
    		setElementBit(i, 0, true);
    	}
    }
    
    /**
     * Merge the logical AND of a second conforming matrix into this one
     * @param matrix
     */
    public void andInto( BitMatrix matrix ) {
        checkShape(matrix);
        for( int i=0; i<nrows; i++ ) {
            BitSet thisBitSet = this.getElement( i );
            BitSet thatBitSet = matrix.getElement( i );
            // Null implies all false, so if either is null, we leave a null at the result position
            if ( thisBitSet == null || thatBitSet == null )
                continue;
            BitSet combined = (BitSet)thisBitSet.clone();
            combined.and( thatBitSet );
            setElementAt(i, combined);
        }

    }

    /**
     * @param matrix
     * @return  New BitMatrix containing the logical product ("AND") of the two matrices
     */
    public BitMatrix and( BitMatrix matrix ) {
        BitMatrix result = this.copy();
        result.andInto(matrix);
        return result;
    }

    /**
     * Merge the logical OR of a second conforming matrix into this one.
     * @param matrix
     */
    public void orInto( BitMatrix matrix ) {
        checkShape(matrix);
        for( int i=0; i<nrows; i++ ) {
            BitSet thisBitSet = this.getElement( i );
            BitSet thatBitSet = matrix.getElement( i );
            BitSet combined = null;
            // Null implies all false, so if both are null
            if ( thisBitSet == null ) {
                if ( thatBitSet == null )
                    continue;  // all false + all false -> all false
                else
                    combined = thatBitSet;
            }
            else {
                if ( thatBitSet == null ) {
                    combined = thisBitSet;
                }
                else {
                    combined = (BitSet) thisBitSet.clone();
                    combined.or( thatBitSet );
                }
            }
            this.setElementAt(i, combined);
        }

    }


    /**
     * @param matrix
     * @return New BitMatrix containing the logical sum ("OR") of the two matrices
     */
    public BitMatrix or( BitMatrix matrix ) {
        BitMatrix result = this.copy();
        result.orInto(matrix);
        return result;
    }

    /**
     * Set difference
     * @param matrix
     * @return  BitMatrix marking bits that are set in this and are not set in param
     */
    public BitMatrix differenceWith( BitMatrix matrix ) {
        checkShape( matrix );
        BitMatrix result = new BitMatrix( nrows, ncols );
        for( int i=0; i<nrows; i++ ) {
            BitSet thisBitset = getElement(i);
            BitSet thatBitset = matrix.getElement(i);
            // if this row is null, then no bits are set and we leave it alone.
            // Likewise, if the param matrix's row is null, it cannot affect the set difference.
            if ( thisBitset == null || thatBitset == null )
                continue;
            BitVector thisbv = new BitVector(thisBitset, ncols );
            BitVector thatbv = new BitVector(thatBitset, ncols );
            BitSet resultBitset = thisbv.differenceWith(thatbv).getBitSet();
            result.setElementAt(i, resultBitset );
        }
        return result;
    }

    public BitMatrix copy() {
        // Make a deep copy
        BitMatrix result = new BitMatrix( nrows, ncols );
        for( int i=0; i<nrows; i++ ) {
            BitSet set = this.getElement(i);
            //if ( set == null )
            //    set = new BitSet();
            result.setElementAt( i, (BitSet)set.clone());
        }
        return result;
    }

    public boolean intersects( BitMatrix matrix ) {
        checkShape(matrix);
        for( int i=0; i<nrows; i++ ) {
            if ( this.getElement(i).intersects(matrix.getElement(i)))
                return true;
        }
        return false;
    }

    /**
     * @param matrix
     * @return True if every bit set in this is also set in param matrix
     */
    public boolean subsetOf( BitMatrix matrix ) {
        checkShape(matrix);
        for( int i=0; i<nrows; i++ ) {
            if ( matrix.getBitVector(i).contains(getBitVector(i)))
                continue;
            return false;
        }
        return true;
    }

    public boolean contains( BitMatrix matrix ) {
        checkShape(matrix);
        for( int i=0; i<nrows; i++ ) {
            if ( getBitVector(i).contains(matrix.getBitVector(i)))
                continue;
            return false;
        }
        return true;
    }

    public static BitMatrix unionOf( Collection<BitMatrix> matrixes ) {
        BitMatrix result = null;
        for( BitMatrix m : matrixes ) {
            if ( result == null )
                result = m;
            else
                result.orInto(m);
        }
        return result;
    }

    public BitMatrix intersection( BitMatrix matrix ) {
        return this.and(matrix);
    }



    /**
     * Validate that a matrix has the same shape as this
     * @param matrix
     */
    void checkShape( BitMatrix matrix ) {
        if ( matrix.size() != this.nrows || matrix.getColCount() != this.ncols ) {
            throw new CdsException("BitMatrix arguments must have the same number of rows and columns");
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BitMatrix bitMatrix = (BitMatrix) o;

        if (ncols != bitMatrix.ncols) return false;
        if (nrows != bitMatrix.nrows) return false;

        for( int i=0; i<nrows; i++ ) {
            if ( this.getBitVector(i).equals( bitMatrix.getBitVector(i)))
                continue;
            else return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
		// FIXME Inconsistent semantics between hashCode() and equals().
        int result = nrows;
        result = 31 * result + ncols;
        return result;
    }
}

