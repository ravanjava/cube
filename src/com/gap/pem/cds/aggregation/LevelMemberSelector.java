package com.gap.pem.cds.aggregation;


import com.gap.pem.cds.Element;
import com.gap.pem.cds.HierarchyLevel;
import com.gap.pem.cds.CdsException;
import com.gap.pem.cds.stores.IStringStore;
import com.gap.pem.cds.util.BitVector;

import java.util.Collection;

/**
 * Holds a subset selection of the members of a level (or any attribute) as a BitVector.
 *
 */
public class LevelMemberSelector {
    HierarchyLevel level;
    BitVector selected;

    /**
     * Define a selector on a level
     * @param level
     */
    public LevelMemberSelector( HierarchyLevel level ) {
        this.level = level;
        selected = new BitVector(level.getMemberCount());
        selected.setAll( false );
    }

    public HierarchyLevel getLevel() {
        return level;
    }

    public BitVector getSelectedBits() {
        return selected;
    }

    /**
     * Select every member of the level
     */
    public void selectAll() {
        int size = selected.size();
        // TODO: More efficient if using selected.setAll(true);
        for( int i=0; i<size; i++ ) {
            selected.set(i,true);
        }
    }

    /**
     * Unselect every member of the level
     */
    public void selectNone() {
        selected.getBitSet().clear();
    }

    /**
     * Select the first member of the level where a String valued attribute matches the specified string.
     * Note: should probably select ALL matches.
     *
     * @param attribute
     * @param value
     * @throws IndexOutOfBoundsException if no member contains a matching string.
     */
    public void select( String attribute, String value ) {
        selected.set( lookup( attribute, value ), true );
    }


    /**
     * Unselect the first member of the level where a String valued attribute matches the specified string.
     * Note:  should probably unselect ALL matches.
     * @param attribute
     * @param value
     */
    public void unselect( String attribute, String value ) {
        selected.set( lookup(attribute,value), false );
    }


    /**
     * Select the member at a known position.  Position is used as the member's ID.
     * @param position
     */
    public void select( int position ) {
        selected.set( position, true );
    }


    /**
     * Unselect the member at a known position.  Position is used as the member's ID.
     * @param position
     */
    public void unselect( int position ) {
        selected.set( position, false );
    }

    /**
     * Select from Elements
     * @param elements
     */
    public void select( Collection<Element> elements ) {
        selectNone();
        for( Element element : elements ) {
            select( element.getElementIndex());
        }
    }

    /**
     * Locate the <b>first</b> member in the level that matches a String value attribute.
     * @param attribute
     * @param value
     * @return Index of the first matching member, or -1 if not found.
     */
    int lookup( String attribute, String value ) {
        IStringStore store =level.getStringAttribute(attribute);
        if ( store==null )
            throw new CdsException("String store named " + attribute + " does not occur in level " + level.getName());
        // todo see if we need something faster than search here
        int size = level.getMemberCount();
        for( int i=0; i<size; i++ ) {
            if ( store.getElement(i).equals(value)) {
                return i;
            }
        }
        return -1;
    }


    @Override
    public String toString() {
        return "LevelMemberSelector{" +
                "level=" + level +
                ", selected=" + selected +
                '}';
    }
}
