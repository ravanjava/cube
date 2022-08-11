package com.gap.pem.cds.aggregator;

import com.gap.pem.cds.CdsException;


/**
 * A simple array of aggregated measure values from one of a set of available types.  Used in conjunction
 * with MeasureValueSorter to produce sorting of aggregated values by measures.
 * <p>
 *     Internally, the AggregatedMeasure contains an array of the chosen type, and values can be set or
 *     referenced using get<i>Type</i>(i) and set<i>Type</i>(value,i).  Getters and setters throw NullPointerException if
 *     the <i>Type</i> does not match the type of the measure.
 * </p>
 * <p>
 *     Measures can optionally be <b>nullable</b>, in which case an internal boolean[] array tracks which
 *     values are null and which are not.  Null values can be set or detected by setIsNull() and getIsNull(),
 * </p>
 */
public class AggregatedMeasure {

    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_INTEGER = 2;
    public static final int TYPE_DOUBLE = 3;
    public static final int TYPE_STRING = 4;

    // Null sorting rule determines the ordering when one of the values is null and the second is not.

    /**
     * (default) nulls sort AFTER all values, whether ascending or descending sort
     */
    public static final int NULLS_COME_LAST = 0;

    /**
     * nulls sort BEFORE all values, whether ascending or descending sort
     */
    public static final int NULLS_COME_FIRST = 1;

    // Unused at present
    //    public static final int NULLS_ARE_MINIMAL = 2; // nulls are smaller than smallest non-null value
    //    public static final int NULLS_ARE_MAXIMAL = 3; // nulls are greater than largest non-null value

    private int nullSortRule = NULLS_COME_LAST;
    public int getNullSortRule() { return nullSortRule; }
    public void setNullSortRule( int rule ) {
        nullSortRule = rule;  // must we check domain?
    }

    String name;
    public String getName() {
        if ( name == null )
            return "?";
        return name;
    }

    int type;
    int length;
    public int size() { return length; }
    boolean sortAscending = true; // applies only if sortable; true means sort ascending order; false means descending

    boolean isNullable = true;
    boolean[] isNull;


    /**
     *
     * @return True if the measure's sort order is ascending.  False means descending.
     */
    public boolean getSortAscending() {
        return sortAscending;
    }

    /**
     * Set to true to indicate this measure is sorted in ascending order, false for descending order.
     * @param b
     */
    public void setSortAscending( boolean  b ) {
        sortAscending = b;
    }

    // Internal value arrays - only one will be populated, corresponding to the type.
    int[] intValues;
    double[] doubleValues;
    String[] stringValues;
    boolean[] booleanValues;

    /**
     * Return value of integer measure at position i
     * @param i
     * @return The integer value at position i.
     * @throws NullPointerException if the measure is not TYPE_INTEGER.
     */
    public int getIntAt( int i ) { return intValues[i]; }
    public void setAt( int i, int v ) { intValues[i] = v; }
    public double getDoubleAt( int i ) { return doubleValues[i]; }
    public void setAt( int i, double v ) { doubleValues[i] = v; }
    public boolean getBooleanAt( int i ) { return booleanValues[i]; }
    public void setAt( int i, boolean  v ) { booleanValues[i] = v; }
    public String getStringAt( int i ) { return stringValues[i]; }
    public void setAt( int i, String v ) { stringValues[i] = v; }
    public boolean getIsNull( int i ) { return isNullable && isNull[i]; }
    public void setIsNull( int i, boolean b ) {
        if ( isNullable ) isNull[i] = b;
        else throw new CdsException("AggregatedMeasure.setIsNull(): Measure is not nullable");
    }

    /**
     * Initialize String measure without nullability
     * @param name
     * @param values
     */
    public AggregatedMeasure(String name, String[] values ) {
        this(name, TYPE_STRING, values.length, true );
        setValues(values);
    }

    /**
     * Initialize String measure with nullability
     * @param name  Name of the measure
     * @param values The String values
     * @param isNull  Same length as values, true to indicate that the corresponding item of values is null.
     */
    public AggregatedMeasure( String name, String[] values, boolean[] isNull ) {
        this( name, TYPE_STRING, values.length, true );
        setNullability( values.length, isNull );
        setValues( values );
    }

    /**
     * Initialize int measure that cannot contain nulls.
     * @param name
     * @param values
     */
    public AggregatedMeasure(String name, int[] values ) {
        this(name, TYPE_INTEGER, values.length, true );
        setValues(values);
    }

    /**
     * Initialize boolean measure that may contain nulls.
     * @param name
     * @param values
     * @param isNull True where the corresponding value is null.
     */
    public AggregatedMeasure( String name, int[] values, boolean[] isNull ) {
        this( name, TYPE_INTEGER, values.length, true );
        setNullability( values.length, isNull );
        setValues( values );
    }

    /**
     * Initialize double measure that cannot contain nulls.
     * @param name
     * @param values
     */
    public AggregatedMeasure(String name, double[] values ) {
        this(name, TYPE_DOUBLE, values.length, true );
        setValues(values);
    }

    /**
     * Initialize double measure that may contain nulls
     * @param name
     * @param values
     * @param isNull True where the corresponding value is null.
     */
    public AggregatedMeasure( String name, double[] values, boolean[] isNull ) {
        this( name, TYPE_DOUBLE, values.length, true );
        setNullability( values.length, isNull );
        setValues( values );
    }

    /**
     * Initialize boolean measure that is not nullable.
     * @param name
     * @param values
     */
    public AggregatedMeasure( String name, boolean[] values ) {
        this(name, TYPE_BOOLEAN, values.length, true );
        setValues( values );
    }

    /**
     * Initialize boolean measure with nulls
     * @param name
     * @param values
     * @param isNull Trure where the corresponding value is null.
     */
    public AggregatedMeasure( String name, boolean[] values, boolean[] isNull ) {
        this( name, TYPE_BOOLEAN, values.length, true );
        setNullability( values.length, isNull );
        setValues( values );
    }

    /**
     * Initialize measure of specified length with values to be specified later.
     * @param name Name of the measure.
     * @param type One of the defined types.
     * @param length Length of the measure array
     * @param sortAscending True if the measure is to be sorted in ascending order, false for descending.  The sort
     *                      order can be changed later.
     */
    public AggregatedMeasure(String name, int type, int length, boolean sortAscending) {
        this.isNullable = false;
        this.name = name;
        this.length = length;
        this.sortAscending = sortAscending;

        switch ( type ) {
            case TYPE_BOOLEAN: booleanValues = new boolean[length]; // use BitVector, perhaps?
                break;
            case TYPE_DOUBLE: doubleValues = new double[length];
                break;
            case TYPE_INTEGER: intValues = new int[length];
                break;
            case TYPE_STRING: stringValues = new String[length];
                break;
            default:
                throw new CdsException("MultiTupleAggregator.AggregatedMeasure(): Invalid type code");
        }

        this.type = type;
    }

    /**
     * Compare the values at two positions a and b.  Return -1 if a precedes b in the ordering, or
     * 1 if b precedes a.  Note that 0 is NEVER returned.  Every position is included in the ordering.
     * @param a
     * @param b
     * @return Comparison result.
     */
    public int compareItems( int a, int b ) {
        if ( isNullable ) {
            if ( isNull[a] ) {
                if ( isNull[b]) { // both null
                    return 0;
                }
                else { // apply the null sort order rule
                    switch( nullSortRule ) {
                        case NULLS_COME_LAST:  return 1;  // a is null, so comes after any non-null
                        case NULLS_COME_FIRST: return -1; // a is null, so comes before any non-null
                        default: throw new CdsException("AggregatedMeasure: illegal value for nullSortRule");
                    }
                }
            }
            else if ( isNull[b] ) {
                switch( nullSortRule ) {
                    case NULLS_COME_LAST:  return -1;  // b is null, so comes after any non-null
                    case NULLS_COME_FIRST: return 1;  // b is null, so comes before any non-null
                    default: throw new CdsException("AggregatedMeasure: illegal value for nullSortRule");
                }
            }
        }
        // Here when neither value is null:
        if ( sortAscending )
            return compareItemsAscending( a, b );
        else
            return compareItemsDescending( a, b );
    }

    int compareItemsAscending( int a, int b ) {
        switch ( type ) {
            case TYPE_BOOLEAN:  // false < true
                boolean  abool = booleanValues[a];
                boolean  bbool = booleanValues[b];
                if ( abool == bbool )
                    return 0;
                else
                    return abool ? 1 : -1;


            case TYPE_INTEGER:
                int aint = intValues[a];
                int bint = intValues[b];
                if ( aint == bint )
                    return 0;
                else
                    return aint<bint ? -1 : 1;


            case TYPE_DOUBLE:
                double adouble = doubleValues[a];
                double bdouble = doubleValues[b];
                if ( adouble == bdouble )
                    return 0;
                else
                    return Double.compare(adouble,bdouble);


            case TYPE_STRING:
                String astring = stringValues[a];
                String bstring = stringValues[b];
                if ( astring.equals(bstring))
                    return 0;
                else
                    return astring.compareTo( bstring );

        }
        throw new CdsException( "AggregatedMeasure.compareItem():  Measure " + this.getName() + " has invalid type");
    }


    int compareItemsDescending( int a, int b ) {
        switch ( type ) {
            case TYPE_BOOLEAN:  // false < true
                boolean  abool = booleanValues[a];
                boolean  bbool = booleanValues[b];
                if ( abool == bbool )
                    return 0;
                else return abool ? -1 : 1;

            case TYPE_INTEGER:
                int aint = intValues[a];
                int bint = intValues[b];
                if ( aint == bint )
                    return 0;
                // Very odd -- for some reason build script rejects Integer.compare....
                else {
                    return aint < bint ? 1 : -1;
                }

            case TYPE_DOUBLE:
                double adouble = doubleValues[a];
                double bdouble = doubleValues[b];
                if ( adouble == bdouble )
                    return 0;
                else return Double.compare(bdouble,adouble); // note reversed order of params

            case TYPE_STRING:
                String astring = stringValues[a];
                String bstring = stringValues[b];
                if ( astring.equals(bstring) )
                    return 0;
                else return bstring.compareTo(astring); // -astring.compareTo( bstring );
        }
        throw new CdsException( "AggregatedMeasure.compareItem():  Measure " + this.getName() + " has invalid type");

    }


    void setValues( String[] values ) {
        for( int i=0; i<values.length; i++ ) {
            stringValues[i] = values[i];
        }
    }

    void setValues( int[] values ) {
        for( int i=0; i<values.length; i++ ) {
            intValues[i] = values[i];
        }
    }

    void setValues( double[] values ) {
        for( int i=0; i<values.length; i++ ) {
            doubleValues[i] = values[i];
        }
    }

    void setValues( boolean [] values ) {
        for( int i=0; i<values.length; i++ ) {
            booleanValues[i] = values[i];
        }
    }

    void setNullability( int length, boolean [] nulls ) {
        if ( nulls == null )
            isNullable = false;
        else {
            isNullable = true;
            if ( nulls.length != length ) {
                throw new CdsException( "AggregatedMeasure.setNullability: length of nulls does not match data length");
            }
            isNull = new boolean[nulls.length];
            for( int i=0; i<nulls.length; i++ ) {
                isNull[i] = nulls[i];
            }
        }
    }

    /**
     * True if values at two positions are both null
     * @param a
     * @param b
     * @return true if the values at positions a and b are both null.
     */
    boolean bothNull( int a, int b ) {
        if ( isNullable ) {
            return isNull[a] && isNull[b];
        }
        else return false;
    }



}

