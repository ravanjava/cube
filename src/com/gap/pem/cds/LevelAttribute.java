package com.gap.pem.cds;

import com.gap.pem.cds.stores.IDataDomainStore;
import com.gap.pem.cds.stores.IDataStore;
import com.gap.pem.cds.stores.IStringStore;
import com.gap.pem.cds.util.Sequence;

/**
 * An attribute of a hierarchy level, which may or may not have a one-to-one relation with
 * the members of the level.
 */
public class LevelAttribute {
    /**
     * The number of distinct values of this attribute in the store
     */
    int cardinality;
    
    HierarchyLevel level;
    String attrname;
    
    /**
     * Used if the attribute has a 1:1 relationship with the members of the level, typically for the member name store.
     */
    IStringStore attrStore;

    /**
     * Used if the attribute does not have a 1:1 relationship with the members of the level.  For example, multiple
     * members of a level might have the same value for the Color attribute, so this is stored in a DataDomainStore.
     */
    IDataDomainStore attrDataDomainStore;

    /**
     * The numeric attribute ID for the attribute value for each member of the level.  For example, if color "Red"
     * has a value key of 0 in the DataDomainStore, then memberAttributeInds will have the value zero for members
     * where color is "Red".
     */
    int[] memberAttributeInds;


    Intersection intersection = null;
    public Intersection  getIntersection() {
        return intersection;
    }


    public LevelAttribute( HierarchyLevel level, String attrname ) {
        init( level, attrname );
    }

    void init( HierarchyLevel level, String attrname ) {
        this.level = level;
        this.attrname = attrname;
        
        IDataStore store  = level.getAttribute(attrname);
        if ( store == null )
            throw new CdsException("Level " + level.getName() + " does not contain attribute " + attrname );
        
        if ( store instanceof IDataDomainStore ) {
            attrDataDomainStore = (IDataDomainStore)store;
            cardinality = attrDataDomainStore.getCardinality();
        }
        else if ( store instanceof  IStringStore ) {
            attrStore = (IStringStore)store;
            cardinality = level.getMemberCount();
        }
        else throw new CdsException("LevelAttribute not supported for type " + attrStore.getClass().getName() );
    }

    public int getAttributeKeyForMember( int memberIndex ) {
        if ( attrDataDomainStore != null ) {
            return attrDataDomainStore.getValueKey( memberIndex );
        }
        else 
        	return memberIndex; // Identity attribute, no-op, OK?
    }


    /**
     * Fake a dimension level to allow an Intersection-based attribute to provide items to an attribute
     * hierarchy.  Creates a HierarchyLevel that wraps the data store of the Intersection attribute
     * @param intersection
     * @param attrname
     */
    public LevelAttribute( Intersection intersection, String attrname ) {
        this.intersection = intersection;
        this.attrDataDomainStore = intersection.getDataDomainAttribute(attrname);
        this.attrname = attrname;
        this.cardinality = attrDataDomainStore.getCardinality();
    }

    /**
     * The numeric attribute ID for the attribute value for each member of the level.  For example, if color "Red"
     * has a value key of 0 in the DataDomainStore, then memberAttributeInds will have the value zero for members
     * where color is "Red".
     */
    public int[] getAttributeInds() {
        if ( memberAttributeInds == null ) {
            initializeAttributeInds();
        }
        return memberAttributeInds;
    }

    /**
     * For each member in the level, initialize the ordinal (id) of this attribute
     */
    public void initializeAttributeInds() {

        if ( intersection !=  null ) {
            // This is an intersection-based attribute
            memberAttributeInds = Sequence.getSequence( intersection.size() );
            return;
        }

        int[] inds = new int[level.getMemberCount()];
        for( int memberId=0; memberId<inds.length; memberId++ ) {
            inds[memberId] = getAttributeKeyForMember(memberId);
        }
        memberAttributeInds =  inds;
    }

    /**
     * @param position
     * @return The String value associated with this attribute at a given position (which is also the level's member ID).
     */
    public String getValue( int position ) {
        if ( this.attrDataDomainStore != null )
            return this.attrDataDomainStore.getValidValues()[position];
        else if ( this.attrStore != null )
            return this.attrStore.getElement( position );
        else return null; // should be an impossible state; throwing an exception here might be better
    }

    /**
     * @return The level associated with this attribte
     */
    public HierarchyLevel getLevel() {
        return level;
    }

    /**
     *
     * @return The name of the attribute.
     */
    public String getAttrname() {
        return attrname;
    }

    /**
     *
     * @return The IStringStore containing the attribute values, if this attribute is stored in an IStringStore.
     * Otherwise returns null.  See getAttrDataDomainStore().
     */
    public IStringStore getAttrStore() {
        return attrStore;
    }

    /**
     *
     * @return The IDataDomainStore containing the attribute values, if this attribute is stored in an IDataDomainStore.
     * Otherwise returns null.  See getAttrStore().
     */
    public IDataDomainStore getAttrDataDomainStore() {
        return attrDataDomainStore;
    }

    /**
     *
     * @return The cardinality of an attribute, i.e. the number of distinct values stored in the attribute.
     */
    public int getCardinality() {
        return cardinality;
    }


    /**
     *
     * @return The total count of values for this attribute, same as the member count of the level.
     */
    public int size() {
        return getAttributeInds().length;
    }
}

