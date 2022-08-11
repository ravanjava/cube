package com.gap.pem.cds.util;

import java.util.Map;

/**
 * Pair - represents a pair of objects (left and right).
 *
 */
public class Pair<KEY, VALUE> implements java.io.Serializable, Comparable<Pair<KEY, VALUE>>
{
    private static final long serialVersionUID = 290668541161222088L;
    
    private KEY left;
    private VALUE right;

    public Pair(KEY left, VALUE right)
    {
        this.left = left;
        this.right = right;
    }

    public Pair(Map.Entry<KEY, VALUE> entry)
    {
        left = entry.getKey();
        right = entry.getValue();
    }

    /**
     * Get the left side key of the pair. Synonym for the 
     * {@link #getKey() getKey} method.
     * 
     * @return the left side key for the pair
     * @see #getKey()
     */
    public KEY getLeft()
    {
        return left;
    }

    /**
     * Get the left side key of the pair. Synonym for the 
     * {@link #getLeft() getLeft} method.
     * 
     * @return the left side key for the pair
     * @see #getLeft()
     */
    public KEY getKey()
    {
        return left;
    }


    /**
     * Set the left side key of the pair. Synonym for the 
     * {@link #setKey(Object) setKey} method.
     * 
     * @param left the left side key for the pair
     * @see #setKey(Object)
     */
    public void setLeft(KEY left)
    {
        this.left = left;
    }

    /**
     * Set the left side key of the pair. Synonym for the 
     * {@link #setLeft(Object) setLeft} method.
     * 
     * @param key the left side key for the pair
     * @see #setLeft(Object)
     */
    public void setKey(KEY key)
    {
        this.left = key;
    }

    /**
     * Get the right side value of the pair. Synonym for the 
     * {@link #getValue() getValue} method.
     * 
     * @return the right side value for the pair
     * @see #getValue()
     */
    public VALUE getRight()
    {
        return right;
    }

    /**
     * Get the right side value of the pair. Synonym for the 
     * {@link #getRight() getRight} method.
     * 
     * @return the right side value for the pair
     * @see #getRight()
     */
    public VALUE getValue()
    {
        return right;
    }

    /**
     * Set the right side value of the pair. Synonym for the 
     * {@link #setValue(Object right) setValue} method.
     * 
     * @param right the right side value for the pair
     * @see #setValue(Object)
     */
    public void setRight(VALUE right)
    {
        this.right = right;
    }

    /**
     * Set the right side value of the pair. Synonym for the 
     * {@link #setRight(Object right) setRight} method.
     * 
     * @param value the right side value for the pair
     * @see #setRight(Object)
     */
    public void setValue(VALUE value)
    {
        this.right = value;
    }

    /**
     * A pair equals to an object if the given object is also a pair,
     * and their key and value are both equal.
     * 
     * @return true or false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
        	return false;
        }

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (left != null ? !left.equals(pair.left) : pair.left != null) {
        	return false;
        }
        
        if (right != null ? !right.equals(pair.right) : pair.right != null) {
        	return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (left != null ? left.hashCode() : 0);
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }

    /**
    * Two pairs are compared first by their left objects and then the right object if necessary.
    * @param other the other Pair object to be compared
    * 
    * @return 0 if equal.
    */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public int compareTo(Pair<KEY, VALUE> other)
    {
       if (left instanceof Comparable && other.left instanceof Comparable)
       {
		int a = ((Comparable) left).compareTo(other.left);
           if (a == 0) //further comparison is necessary 
           {
               if (right instanceof Comparable && other.right instanceof Comparable)
               {
                  return ((Comparable) right).compareTo(other.right);
               }
           }
           else
           {
               return a;
           }
       }
       return 0;
    }
}

