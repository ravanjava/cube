package com.ram.ds.cds.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ram.ds.cds.CdsException;
import com.ram.ds.cds.CubeDs;
import com.ram.ds.cds.Dimension;
import com.ram.ds.cds.HierarchyLevel;
import com.ram.ds.cds.Intersection;
import com.ram.ds.cds.LevelAttribute;
import com.ram.ds.cds.stores.DataDomainAttrStore;
import com.ram.ds.cds.stores.IIntStore;

/**
 *  A hierarchy of attributes that can be used as a facet in a pivot table or for other dimensional operations.
 *  Calculates the distinct tuples of the attribute values that are found in a collection of one or more Intersections,
 *  and creates new Dimensions containing HierarchyLevels where each member corresponds to a distinct tuple.
 *  <p>
 *      An AttributeHierarchy defines an ordered hierarchy of attributes pertaining to a set of Intersections.
 *  </p>
 *  @author  James Wheeler
 *  @author  Last updated by $Author: misong $
 *  @version $Revision: 1.14.6.1 $
 */
public class AttributeHierarchy  {

    List<LevelAttribute> attributeList;
    CubeDs collector;
    AttributeGrouping[] groupings;



    /**
     * The names of the Intersections accessed through this hierarchy and the depth in the hierarchy at which
     * each Intersection is related to the result Dimension.
     */
    HashMap<String,Integer> intersectionDepths = new HashMap<String, Integer>();

    public AttributeHierarchy( List<LevelAttribute> inattributeList, CubeDs collector, List<Intersection> intersections)  {
        this.attributeList = new ArrayList<LevelAttribute>( inattributeList );
        this.collector = collector;
        groupings = new AttributeGrouping[ attributeList.size()];

        // Initialize groupings
        List<LevelAttribute> partialHierarchy = new ArrayList<LevelAttribute>();
        for( int i=0; i<inattributeList.size(); i++ ) {
            partialHierarchy.add( inattributeList.get(i));
            groupings[i] = new AttributeGrouping( partialHierarchy ); // will make a copy of the list
        }

        for( Intersection intersection : intersections ) {
            // Find the tuples of the attributes that exist at this intersection and add to the appropriate groupings.
            boolean[] usableAttributes = findAttributesAccessibleThroughIntersection( intersection );
            int usableDepth = 0;
            for( boolean usable : usableAttributes ) {
                if ( !usable ) break;
                usableDepth++;
            }
            // Record the depth (number of parts in the tuple) for this intersection.  This will allow a new
            // related level to be added to the intersection when a HierarchyLevel is created from the grouping.
            this.intersectionDepths.put( intersection.getName(), usableDepth );

            // A partial hierarchy:  One grouping is needed for each depth of the hierarchy.
            List<LevelAttribute> subsetHierarchy = new ArrayList<LevelAttribute>();

            // Mapping from the items at the intersection to the leaf-level attribute of the current subset hierarchy.
            ArrayList<int[]> levelTargetInds = new ArrayList<int[]>(); // all arrays are of same size as the intersection.
            for( int depth = 0; depth < usableDepth; depth++ ) {

            	// QUESTION: I am confused by the next block of code. Didn't we already create all the possible
            	// groupings in the block commented as "Initialize groupings"? That would cause the next block
            	// of code essentially a no-op.
            	
                // Create or add to the grouping of distinct tuples at this depth in the hierarchy.
                // The attribute groupings can only be computed for attributes that are usable and whose ancestors are
                // all usable attributes.  Thus we truncate the hierarchy at the place where the first unusable attribute.
                subsetHierarchy.add( attributeList.get(depth));
                if ( groupings[depth] == null ) {
                    // Add grouping if this is the first intersection accessible at this depth
                    groupings[depth] = new AttributeGrouping(subsetHierarchy);
                }

                // Get the target inds at this intersection for the tuples at this depth
                LevelAttribute levelAttribute = attributeList.get(depth);
                int[] attributeInds;
                if ( levelAttribute.getIntersection() != null ) {
                	// this is a multi-level attribute
                    attributeInds = new int[ levelAttribute.size() ];
                    for( int i=0; i<attributeInds.length; i++ ) {
                        attributeInds[i] = levelAttribute.getAttributeKeyForMember( i );
                    }
                }
                else {
                	// this is a single-level attribute.
                    int[] levelMemberInds = intersection.getTargetAggregationInds(collector, levelAttribute.getLevel());

                    if ( levelAttribute.getAttrname().equals( levelAttribute.getLevel().getIdentityAttributeName())) {
                        // the attribute ID is the member ID itself
                        attributeInds = levelMemberInds;
                    }
                    else {
                        // the attribute may occur multiple times, so we get ID for this distinct value (from 0..cardinality-1)
                        // of the attribute
                        attributeInds = new int[ levelMemberInds.length];
                        for( int j=0; j<attributeInds.length; j++ ) {
                            attributeInds[j] = levelAttribute.getAttributeKeyForMember( levelMemberInds[j] );
                        }
                    }
                }
                levelTargetInds.add( attributeInds );
                
                groupings[depth].addTuples(levelTargetInds.toArray(new int[levelTargetInds.size()][]), intersection);

            }
        }
    }


    /**
     * @return List&gt;LevelAttribute&lt; of the attributes in the hierarchy from highest to lowest.
     */
    public List<LevelAttribute> getAttributeList() {
        return attributeList;
    }

    /**
     *
     * @param depth
     * @return The AttributeGrouping at the given depth.
     */
    public AttributeGrouping getGrouping(int depth) {
        return groupings[depth];
    }


    /**
     * Returns the indices into the attribute for an Intersection that was specified in the constructor.
     * @param targetAttributeName
     * @param intersection
     * @return The indices into the intersection for the members of the target attribute, providing a mapping
     * from the intersection to the attribute member.
     */
    public long[] getTargetAggregationInds(String targetAttributeName, Intersection intersection) {
        for( int i=0; i<attributeList.size(); i++ ) {
            if ( attributeList.get(i).getAttrname().equals( targetAttributeName ) ) {
                return groupings[i].getTupleInds( intersection.getName() );
            }
        }
        throw new CdsException( "AttributeHierarchy.getTargetAggregationInds: Attribute not in hierarchy: " + targetAttributeName );
    }

    /**
     * @param parentName Name of parent attribute
     * @return The name of the child attribute of the parent attribute, or null if this is the bottom level.
     */
    public String getChildAttributeName( String parentName ) {
        LevelAttribute child =  getChildAttribute( parentName );
        return child == null ? null : child.getAttrname();
    }


    /**
     * @param parentName
     * @return Attribute that is the immediate child of parentName in the hierarchy, or null if this is the bottom level.
     */
    public LevelAttribute getChildAttribute( String parentName ) {
        int deepestParent = attributeList.size()-1;
        for( int i=0; i<deepestParent; i++ ) {
            if ( attributeList.get(i).getAttrname().equals(parentName)) {
                return attributeList.get(i+1);
            }
        }
        return null;
    }

    /**
     *
     * @param childName
     * @return Name of the parent attribute of childname in the hierarchy, or null if no parent is defined.
     */
    public String getParentAttributeName( String childName ) {
        LevelAttribute parent = getParentAttribute( childName );
        return parent == null ? null : parent.getAttrname();
    }

    /**
     *
     * @param childName
     * @return Attribute that is the parent of childName, or null if no parent is defined.
     */
    public LevelAttribute getParentAttribute( String childName ) {
        int depth = attributeList.size();
        for( int i=0; i<depth; i++ ) {
            if ( attributeList.get(i).getAttrname().equals( childName ) ) {
                if ( i == 0 ) return null;
                else return attributeList.get(i-1);
            }
        }
        return null;
    }


    /**
     * Primarily for diagnostic use at this point.  Returns the String attribute value for each level in
     * an attribute tuple.
     * @param depth
     * @param tupleIndex
     * @return String[] of the attribute values for the given tuple index at depth in the hierarchy.
     */
    public String[] getTupleValues(int depth, long tupleIndex ) {
        int[] inds = getGrouping(depth).getAttributeInds(tupleIndex);
        String[] values = new String[inds.length];
        for( int i=0; i<inds.length; i++ ) {
            values[i] = this.attributeList.get(i).getValue(inds[i]);
        }
        return values;
    }


    /**
     * Convert the attribute hierarchy to a Dimension in the collector, with each AttributeGrouping exposed as a
     * HierarchyLevel.
     * @param dimensionName
     * @return New Dimension derived from the tuples of the attributes.
     */
    public Dimension createDimension(String dimensionName, String hierarchyName) {
        return createDimension( dimensionName, hierarchyName, null );
    }

    /**
     * Construct a new Dimension from the tuples of the attributes in hierarchical order.
     * @param dimensionName
     * @param hierarchyName
     * @param levelNames
     * @return New Dimension derived from the tuples of the attributes.
     */
    public Dimension createDimension( String dimensionName, String hierarchyName, String[] levelNames ) {

        if ( levelNames != null && levelNames.length != groupings.length ) {
            throw new CdsException("AttributeHierarchy.createDimension:  levelNames length does not match groupings length");
        }
        Dimension dimension = new Dimension(dimensionName);

        // Create the level builders
        HierarchyLevelBuilder[] builders = new HierarchyLevelBuilder[this.attributeList.size()];

        for(int i=0; i<groupings.length; i++) {
        	HierarchyLevelBuilder parentBuilder = (i > 0 ? builders[i-1] : null);
            String levelName = null;
            String identityAttributeName = null;
            if ( levelNames == null ) {
                // New level name is the same as the leaf level of the source grouping
                levelName = groupings[i].getLeafAttributeName();
                identityAttributeName = levelName;
            }
            else {
                levelName = levelNames[i];
                identityAttributeName = levelNames[i];
            }
            builders[i] = new HierarchyLevelBuilder(dimensionName, levelName, identityAttributeName, parentBuilder);
        }

        // Starting from the deepest level in the attribute, add an item to the HierarchyLevelBuilder for
        // each distinct tuple in the grouping, plus a reference to the tuple of the parent HierarchyLevelBiulder
        for (int i= attributeList.size()-1; i>=0; i--) {
            // The level that contains this attribute
            LevelAttribute sourceAttr = attributeList.get(i);

            AttributeGrouping grouping = groupings[i];

            // This is the set of distinct tuples that exist at the intersection.  Each is the packed index in
            // the tuple space for that tuple.
            HashMap<Long,Integer> tupleIndToAggregatorInd  = grouping.tupleIndToAggregatorInd;

            for( long tupleInd : tupleIndToAggregatorInd.keySet() ) {

                // Unpack the index, giving the attribute ordinals for this tuple.
                int[] attrInds = grouping.getAttributeInds(tupleInd);
                String member = sourceAttr.getValue(attrInds[i]);
                int id = builders[i].getMemberId(member,tupleInd);

                // Record the new Level's memberId in the grouping
                grouping.setTupleIndToNewMemberId( tupleInd, id );

                if (i > 0) { // this attribute has a parent attribute
                    LevelAttribute parentAttribute = attributeList.get(i-1);
                    String parentAttributeValue = parentAttribute.getValue(attrInds[i-1]);
                    AttributeGrouping parentGrouping = groupings[i-1];
                    int[] parentInds = new int[attrInds.length-1];
                    for(int iparent=0; iparent<parentInds.length; iparent++) {
                        parentInds[iparent] = attrInds[iparent];
                    }
                    int parentMemberId = builders[i-1].getMemberId(parentGrouping, parentAttributeValue, parentInds);
                    builders[i].parentIdStore.setElementAt(id, parentMemberId);

                }
            }
        }

        // build the levels in the new dimension, including the members, parent indices.
        List<HierarchyLevel> levels = new ArrayList<HierarchyLevel>();
        for( int i=0; i<groupings.length; i++ ) {
            HierarchyLevel level = builders[i].buildLevel();
            dimension.addLevel( level );
            levels.add( level );
        }
        // build hierarchy in the new dimension
        dimension.addHierarchy( hierarchyName, levels );

        // Add a related level to each Intersection accessed through this new Dimension
        for( String intersectionName : intersectionDepths.keySet() ) {
            int depth = intersectionDepths.get( intersectionName );
            if ( depth == 0 )
                continue;
            Intersection intersection = collector.getIntersection( intersectionName );
            HierarchyLevel level = levels.get(depth-1); // leaf level of the new dimension
            intersection.addRelatedLevel( level );
            IIntStore intersectionIdStore = intersection.getIntAttribute( level.getName());

            int[] leafToIntersectionMap = groupings[depth-1].getNewLevelIdsForIntersection( intersectionName );
            for( int id : leafToIntersectionMap ) {
                intersectionIdStore.addElement(id);
            }
            // clear the level mapping cache in the intersection because the
            // hierarchy for a dimension has been changed.
            intersection.clearLevelMappingCache();
        }

        return dimension;

    }

    /**
     * Helper for populating DataDomainAttrStore
     * @param store
     * @param value
     * @param memberId
     */
    @SuppressWarnings("unused")
	private void putString( DataDomainAttrStore store, String value, int memberId ) {
        int key = store.getKeyForValidValue(value);
        if ( key == -1 )
            key = store.addValidValue( value );
        store.setValueByKey( memberId, key );
    }


    /**
     * Helper:  find the level on the intersection, if any, that is from the same dimension as the attribute.
     * 
     * @param attribute a single level attribute
     * @param intersection an intersection
     * @return The level on the Intersection that relates to the Dimension that contains attribute.
     * @throws CdsException if the attribute does not belong to any of the related Dimensions on the intersection.
     */
    private HierarchyLevel getIntersectionLevelFromSameDimensionAs( LevelAttribute attribute, Intersection intersection ) {
        HierarchyLevel attrlevel = attribute.getLevel();
        HierarchyLevel levelOnDimension = null;
        String dimname = attrlevel.getDimensionName();
        for( HierarchyLevel level : intersection.getRelatedLevels() ) {
            if ( level.getDimensionName().equals( dimname )) {
                levelOnDimension = level;
                break;
            }
        }
        if ( levelOnDimension == null ) {
            throw new CdsException("AttributeHierarchy:  LevelAttribute for " + attribute.getAttrname() +
                    " does not correspond to any of the dimension in intersection " + intersection.getName() );
        }

        return levelOnDimension;

    }

    /**
     * Helper: Check if the given single-level attribute is defined at a level that is on the same as the given
     * level or above it in its dimension. The given pair of attribute and level must have been known to be in 
     * the same dimension.
     *  
     * @param collector
     * @param attr
     * @param level
     * @return
     */
    private boolean attributeIsOnSameLevelOrAncestor( CubeDs collector, LevelAttribute attr, HierarchyLevel level ) {
        Dimension dimension = collector.getDimension( level.getDimensionName() );
        // FIXME using the first hierarchy as the only hierarchy.
        List<HierarchyLevel> defaultHierarchy = dimension.getHierarchy( dimension.getHierarchyNames().get(0));
        
        int levelPosition = -1;  // depth of the given level
        for( levelPosition=0; levelPosition<defaultHierarchy.size(); levelPosition++ ) {
            if ( defaultHierarchy.get(levelPosition).getName().equals( level.getName()))
                break;
        }
        if ( levelPosition == -1 ) {
            throw new CdsException("Attribute does not occur on level"); // foo
        }

        // starting from the given level, loop upwards along the hierarchy to compare the hierarchy level name with 
        // the attribute level name. If found, then the attribute must be on or above the given level.
        HierarchyLevel attrlevel = attr.getLevel();
        while ( levelPosition >= 0 ) {
            if ( attrlevel.getName().equals( defaultHierarchy.get(levelPosition).getName() ) ) {
                return true;
            }
            levelPosition--;
        }
        
        // If not found, then the attribute must be defined on a level that is below the given level.
        return false;
    }

    /**
     * Find the attributes that are on levels that are at or above an Intersection's associated level for the
     * source dimension. Lower level attributes can't be mapped to the Intersection.
     * 
     * @param intersection
     * @return Boolean array containing true where the corresponding attribute in <em>attributeList</em> is
     * capable of being mapped to this intersection.
     */
    private boolean[] findAttributesAccessibleThroughIntersection( Intersection intersection ) {

        boolean[] usableAttributes = new boolean[attributeList.size()]; // default all false

        for( int i=0; i<attributeList.size(); i++ ) {
            LevelAttribute attribute = attributeList.get(i);
            if ( attribute.getIntersection() != null ) {
            	// this is a multi-level attribute
            	// QUESTION, what if this attribute is defined in an intersection above the given
            	// intersection? Guess we are not support this use case because the solution could
            	// be complex. We need to weed out the intersections that are crossing each other
            	// in their levels, with the related levels higher in one dimension but lower in
            	// another. Also, the dimensionalities of the two intersections may not match.
                usableAttributes[i] = attribute.getIntersection().getName().equals(intersection.getName());
                continue;
            }
            // this is a single-level attribute.
            HierarchyLevel levelOnDimension = getIntersectionLevelFromSameDimensionAs( attribute, intersection );
            usableAttributes[i] = attributeIsOnSameLevelOrAncestor( collector, attribute, levelOnDimension );
        }
        return usableAttributes;
    }

}