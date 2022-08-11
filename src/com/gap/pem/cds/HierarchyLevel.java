package com.gap.pem.cds;

import java.io.Serializable;
import java.util.HashMap;

import com.gap.pem.cds.stores.IIntStore;
import com.gap.pem.cds.stores.IStringStore;

/**
 * A level in a dimension's hierarchy, containing a set of members.  Each member is identified by a
 * distinct name (identity attribute) and unique memberId.  The identity attribute is stored in an
 * IStringStore object, and the unique memberId is stored in an IIntStore whose values are the consecutive
 * integers from 0 to memberCount-1.  Member ids can be used a subscripts to select members from the level.
 * <p>If the level is the child of another level, it also contains an IIntStore whose name is the name of the
 * parent level and whose values are member id in the parent level for the corresponding member id in this
 * level.  Since a level can belong to more than one hierarchy, it may have a parent id store for more than
 * one parent level.
 *  </p>
 *  <p>A level can also contain a number of additional attributes that describe other information about
 *  the members.  For example, a Country level might contain a Language attribute for each member.</p>
 *
 */
public class HierarchyLevel extends AttributeContainer  implements Serializable {

    private static final long serialVersionUID = 3955105454539234782L;
    private String dimensionName;
    private HashMap<String,Integer> identityToIndexMap = new HashMap<String,Integer>();

    /**
     * Name of the attribute that is used as member identity.  Each member's value must be distinct.
     */
    private String identityAttributeName;
    public String getIdentityAttributeName() {
        return identityAttributeName;
    }

    /**
     * Construct using the level's name as its identity attribute name.
     * @param name
     * @param iDimensionName
     */
    public HierarchyLevel(String name, String iDimensionName) {
        super(name);
        this.dimensionName = iDimensionName;
        this.identityAttributeName = name;
        addIdentityAttribute();
    }

    /**
     * Construct using explicitly provided identity attribute name
     * @param name
     * @param iDimensionName
     * @param identityAttributeName
     */
    public HierarchyLevel( String name,  String iDimensionName, String identityAttributeName ) {
        super(name);
        this.dimensionName = iDimensionName;
        this.identityAttributeName = identityAttributeName;
        addIdentityAttribute();
    }

    public String getDimensionName() {
    	return this.dimensionName;
    }

    protected IStringStore addIdentityAttribute() {
        if ( this.identityToIndexMap == null )
            identityToIndexMap = new HashMap<String, Integer>();
        else
            identityToIndexMap.clear();

        return addStringAttribute( identityAttributeName );

    }

    public IIntStore addParentAttribute(String parentName) {
        return addIntAttribute( parentName );
    }


    /**
     * Add a member to the level.  The name must be distinct from any existing members in the level.
     * @param name Name of the member, which will be stored in the identity attribute
     * @return The memberId of the newly added member
     * @throws CdsException If the name is already used by an existing member.
     */
    public int addMember( String name ) {
        if ( identityToIndexMap.keySet().contains(name))
            throw new CdsException("Duplicate member name '" + name + "' cannot be added to level " + this.getName());
        int memberId = identityAttribute().addElement( name );
        identityToIndexMap.put(name,memberId);
        return memberId;
    }

    /**
     * Replace the member name (identity attribute) for the member whose position (memberId) is index.
     * @param index Member Id of the member to rename
     * @param name New name
     * @return The previous name of the member.
     * @throws CdsException If the new name is already used by an existing member.
     */
    public String setMemberAt( int index, String name ) {
        if ( identityToIndexMap.get(name) != null ) {
            // Already have a member with this name, if it's at the same index it's OK.  Otherwise an error
            if ( identityToIndexMap.get(name) != index ) {
                throw new CdsException("Duplicate member name '"
                        + name +  "' cannot be assigned to a different member in level " + this.getName());
            }
        }
        String previousName = identityAttribute().setElementAt( index, name );
        identityToIndexMap.remove(previousName);
        identityToIndexMap.put( name, index );
        return previousName;
    }


    private IStringStore identityAttribute() {
        return getStringAttribute(identityAttributeName);
    }

    /**
     * Get the memberId (index) of a member identified by its identity attribute.
     * @param identityAttributeValue
     * @return The memberId, or -1 if it does not exist in the level.
     */
    public int lookup( String identityAttributeValue ) {
        Integer index =  identityToIndexMap.get(identityAttributeValue);
        return index == null ? -1 : index;
    }

    @Override
    public String toString() {
        return "HierarchyLevel{" +
                "dimensionName='" + dimensionName + '\'' +
                ", levelName=" + name +
                ", memberCount=" + getMemberCount() +
                '}';
    }
}

