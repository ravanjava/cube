package com.gap.pem.cds;

import java.util.Map;

/**
 * Pair - represents a pair of objects (left and right).
 *
 */
public class Pair<KEY, VALUE> implements java.io.Serializable, Comparable<Pair<KEY, VALUE>>
{
    private KEY left;
    private VALUE right;
    private static final long serialVersionUID = 290668541161222088L;


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

    public KEY getLeft()
    {
        return left;
    }

    public KEY getKey()
    {
        return left;
    }


    public void setLeft(KEY left)
    {
        this.left = left;
    }

    public void setKey(KEY left)
    {
        this.left = left;
    }


    public VALUE getRight()
    {
        return right;
    }

    public VALUE getValue()
    {
        return right;
    }

    public void setRight(VALUE right)
    {
        this.right = right;
    }

    public void setValue(VALUE right)
    {
        this.right = right;
    }


    @SuppressWarnings("unchecked")
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<KEY, VALUE> pair = (Pair<KEY, VALUE>) o;

        if (left != null ? !left.equals(pair.left) : pair.left != null) return false;
        if (right != null ? !right.equals(pair.right) : pair.right != null) return false;

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
    * Implements Comparable abstract method.
    * Sorting is done on the left object first and then the right object if necessary.
    * @param other the Pair object to be compared
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

