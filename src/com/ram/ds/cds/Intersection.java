package com.ram.ds.cds;

import java.io.Serializable;
import java.util.*;

import com.ram.ds.cds.aggregation.Cartesian;
import com.ram.ds.cds.filters.IFilter;
import com.ram.ds.cds.stores.IDataStore;
import com.ram.ds.cds.stores.IIntStore;
import com.ram.ds.cds.util.ArrayOps;
import com.ram.ds.cds.util.BitMatrix;
import com.ram.ds.cds.util.BitVector;
import com.ram.ds.cds.util.Sequence;

/**
 * <p>Represents the intersection of one or more HierarchyLevels from different dimensions, providing a place to
 * hold measures. An Intersection corresponds to a fact table in a star-schema database.
 *
 * <p>Conceptually, an intersection contains a set of <b>items</b>, where each item is described by tuple of
 * members in the intersection's primary levels.  The tuple can be thought of a primary key, and is unique in
 * the intersection.  Additional attributes can be added to the Intersection, including ones that reference
 * additional dimensions, however these need not be unique.</p>
 *
 * <p>Measures are attached to an intersection by adding data stores.  Any data store added to the intersection
 * must conform to the number of items defined on the intersection.  Also, additional dimensions can be
 * attached and added as related levels. </p>
 *
 */
public class Intersection extends AttributeContainer implements Serializable {

    private static final long serialVersionUID = 3759628501127161507L;

    /**
     * The dimensions that contain the levels that define the primary key.
     */
    private String[] primaryKeyDimensions;
    public String[] getPrimaryKeyDimensions() { return primaryKeyDimensions; }

    /**
     * The levels that define the primary key
     */
    private String[] primaryKeyLevels;
    public String[] getPrimaryKeyLevels() { return primaryKeyLevels; }

    /**
     * The levels associated with all dimensions that are attached to this intersection, including
     * the primary key dimensions plus any other dimensions that have been subsequently added.
     * The key is the dimension name. Because it is using a LinkedHashMap, the entries in the map
     * can be retrieved by their insertion order.
     */
    private LinkedHashMap<String, HierarchyLevel> relatedLevelsMap = new LinkedHashMap<String, HierarchyLevel>();

    /**
     * Used if, and only if, the Intersection has a known fixed size at the time it is created.  This allows
     * fixed size attributes to be added to the intersection, which are somewhat faster and more compact.
     */
    private int memberCount = 0;

    // Caching is enabled in all cases. Change constant to suppress.  This could potentially become an instance member.
    private static final boolean cacheLevelMappings = true;

    /**
     * Cache of mappings from a hierarchy level, which can be a higher level in the dimension than the
     * related level, to the items on the intersection.  Key is the level name, value is int[] of same length
     * as the intersection, where each value is a memberId in the level.
     */
    private HashMap<String,int[]> cache = cacheLevelMappings ? new HashMap<String, int[]>() : null;

    /**
     * Mapping of a level to the store that holds its memberIds.
     */
    private LinkedHashMap<HierarchyLevel,IIntStore> levelToAttributeMap = new LinkedHashMap<HierarchyLevel, IIntStore>();

    /**
     * Holds the memberIds for each of the related levels.
     */
    private IIntStore[] relatedLevelsStores;

    /**
     * Holds the sizes for each related levels
     */
    private int[] relatedLevelsCardinality;

    /**
     * Maps the packed cartesian index for each tuple on the intersection to its position in the intersection.
     */
    private HashMap<Long,Integer> cartesianTupleMap = new HashMap<Long, Integer>();

    /**
     * Get the number of elements in the intersection. Because the intersection may
     * contain multiple attribute stores, in case these attribute stores are of 
     * different size, this method returns the size of the attribute store with
     * largest number of elements.
     *  
     * @return Number of items in the intersection
     */
    public int size() {
        return super.getMemberCount();
    }

    /**
     * Create an intersection with the given name. The intersection is formed by
     * the combinations of the given array of primary levels. The order of the
     * levels in the array is significant. Subsequent {@link #lookup(int[])} or
     * {@link #lookupOrAdd(int[])} calls must pass the member indices in the same
     * order.
     * <p>
     * The levels need to be populated with members before the intersection is
     * created.
     * 
     * @param name a name
     * @param primaryLevels the levels to form the intersection with. The order
     *        of the levels in the array is significant.
     */
    public Intersection(String name, HierarchyLevel[] primaryLevels) {
        super(name);
        initializePrimaryKey(primaryLevels);
    }

    private void initializePrimaryKey(HierarchyLevel[] levels) {
        this.primaryKeyDimensions = new String[levels.length];
        this.primaryKeyLevels = new String[levels.length];
        for( int i=0; i<levels.length; i++ ) {
            primaryKeyDimensions[i] = levels[i].getDimensionName();
            primaryKeyLevels[i] = levels[i].getName();
            addRelatedLevel(levels[i]);
        }
        initializeRelatedLevelsStores();
    }

    /**
     * Initialize, or re-initialize, the rapid-access structures for intersection
     * item lookup. This is called only once, while the intersection is being built.
     */
    private void initializeRelatedLevelsStores() {
        int relatedLevelCount = relatedLevelsMap.keySet().size();
        this.relatedLevelsStores = new IIntStore[relatedLevelCount];
        this.relatedLevelsCardinality = new int[relatedLevelCount];
        int irelatedLevel = 0;
        for(Map.Entry<String, HierarchyLevel> entry : relatedLevelsMap.entrySet()) {
            HierarchyLevel level = entry.getValue();
            relatedLevelsStores[irelatedLevel] = getIntAttribute( level.getName());
            relatedLevelsCardinality[irelatedLevel] = level.getMemberCount();
            irelatedLevel++;
        }
    }

    /**
     * Add an additional level that maps to the intersection.  The level must not 
     * belong to a dimension that is one of the primary key dimensions.
     * 
     * @param level
     */
    public void addRelatedLevel(HierarchyLevel level){
        String dimensionName = level.getDimensionName();
        if ( relatedLevelsMap.containsKey( dimensionName )) {
            throw new CdsException("Intersection.addRelatedLevel(): Intersection already has a related level for this Dimension");
        }
    	relatedLevelsMap.put( dimensionName ,level);
    	
    	// Create the index store for the level and put it in the levelToAttributeMap
    	// and realtedLevelsStores.
        IIntStore levelIndexStore = null;
        if(memberCount > 0) {
        	levelIndexStore = addFixedLengthIntAttribute(level.getName(), memberCount);
        }
        else {
        	levelIndexStore = addIntAttribute(level.getName());
        }
        levelToAttributeMap.put(level, levelIndexStore);
        
        // Update the related stores array
        this.relatedLevelsStores = new IIntStore[relatedLevelsMap.size()];
    	int levelIndex = 0;
    	for(Map.Entry<String, HierarchyLevel> entry : relatedLevelsMap.entrySet()){
    		HierarchyLevel relatedLevel = entry.getValue();
        	this.relatedLevelsStores[levelIndex] = this.levelToAttributeMap.get(relatedLevel);
    		levelIndex ++;
    	}
    }
    
    /**
     * Get the related level in the intersection that comes from the given dimension.
     * 
     * @param dimensionName dimension name
     * @return the related level, or null if no such level exists in the intersection.
     */
    public HierarchyLevel getRelatedLevel(String dimensionName){
    	return relatedLevelsMap.get(dimensionName);
    }

    /**
     * Return the levels that form the intersection, in the order they were added 
     * to the intersection.
     * 
     * @return The related levels.  Note that although a Collection, the levels are 
     * in insertion order.
     */
    public Collection<HierarchyLevel> getRelatedLevels() {
        return relatedLevelsMap.values();
    }

    /**
     * Return the levels that form the intersection, in the order they were added 
     * to the intersection.
     * 
     * @return The related levels in insertion order.
     */
    public HierarchyLevel[] relatedLevels() {
        Collection<HierarchyLevel> t = getRelatedLevels();
        return t.toArray( new HierarchyLevel[t.size()] );
    }

    @Override
    public String toString() {
        return "Intersection{" +
        		"name="+this.getName() +
                "relatedLevelsMap=" + relatedLevelsMap.keySet() +
                ", memberCount=" + size() +
                "} " + super.toString();
    }


    /**
     * Calculate the combined result of OR-ing together all of the filters when 
     * projected to the intersection.
     * 
     * @param filters a set of filters that 
     * @return Composite of each level projected to this intersection. The size
     * of the BitVector is the same as the size of the intersection. A bit is 
     * set if and only if the element at the index matches all the filters.
     */
    public BitVector getCompositeFilter( IFilter[] filters )  {
    	BitVector result = null;
        if ( filters != null ) { 
        	int size = this.size();
        	result = new BitVector(size);
        	result.setAll(true);
            for( int i=0; i<size; i++ ) {
                for( IFilter filter : filters ) {
                    if (result.get(i)) {
                        result.set(i, filter.isMatch(i));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Clear the internal cache for level mapping. This is mandatory when the levels for
     * any dimension has been changed.
     */
    public void clearLevelMappingCache(){ 
    	if ( cacheLevelMappings ) {
    		cache.clear();
    	}
    }
    
    /**
     * <b>This is required if the application has made any of the changes below
     * to the related levels.</b>
     * <ol>
     * <li>The number of related levels have changed, through {@link #addRelatedLevel(HierarchyLevel)}.</li>
     * <li>The number of members in any of the related levels changed.</li>
     * </ol>
     * The method will update the internal caches and mappings in the intersection.
     */
    public void updateCacheAndMappings() {
    	int numLevels = this.relatedLevelsMap.size();
    	
    	// update the level cardinalities
    	this.relatedLevelsCardinality = new int[numLevels];
    	int levelIndex = 0;
    	for(Map.Entry<String, HierarchyLevel> entry : relatedLevelsMap.entrySet()){
    		HierarchyLevel level = entry.getValue();
    		this.relatedLevelsCardinality[levelIndex] = level.getMemberCount();
    		levelIndex ++;
    	}

    	// update the tuple index to intersection index map for all the existing
    	// elements in the intersection.
		this.cartesianTupleMap.clear();
    	int[] elementKey = new int[numLevels];
    	for(int i = 0, n = size(); i < n; i ++){
    		for(int k = 0; k < this.relatedLevelsStores.length; k ++){
    			elementKey[k] = this.relatedLevelsStores[k].getElement(i);
    		}
    		long tupleKey = packRelatedLevelsInds(elementKey);
    		this.cartesianTupleMap.put(tupleKey, i);
    	}

    	if ( cacheLevelMappings ) {
    		// we can just clear it, its content will be rebuilt upon the next
    		// aggregation operation.
    		cache.clear();
    	}
    	
    }

    /**
     * Which members in the target level have entries in the intersection?
     * 
     * Create a mapping between the members of the intersection and the members of a level.
     * @param collector  It's actually stupid to have to pass this, but we need it to find the Dimension because
     *                   a HierarchyLevel only holds the dimension's name, not a reference to it
     * @param targetLevel
     * @return target level ancestor indices for all the members in the intersection, length
     * is the number of elements in the intersection.
     */
    public int[] getTargetAggregationInds( CubeDs collector, HierarchyLevel targetLevel )
    {
        // This method really should have a hierarchy name to select how the level maps to the leaf of the dimension
        if ( cacheLevelMappings ) {
            int[] cachedInds = cache.get( targetLevel.getName());
            if ( cachedInds != null )
                return ArrayOps.copy( cachedInds ); // so in-place adjustments to the indices do not clobber the cache
        }

        // Map the bottom level to the leaf level of the dimension
        int[] inds = Sequence.getSequence(targetLevel.getMemberCount());
        Dimension dimension = collector.getDimension(targetLevel.getDimensionName());

        inds = projectToIntersection(dimension, null, targetLevel.name, inds);  // todo:  need explicit hierarchy

        if ( inds == null ){
            // THere is no mapping to this level; return -1 for all positions
            inds = new int[this.size()];
            for( int i=0; i<inds.length; i++ )
                inds[i] = -1;
            return inds;
        }


        // Get the level name on this intersection that's part of the same dimension( future hierarchy ) as targetLevel
        String intersectionAttrName = null;
        for( HierarchyLevel level : getRelatedLevels() ) {
            if ( level.getDimensionName().equals( targetLevel.getDimensionName() ) ) {
                intersectionAttrName = level.getName();
            }
        }
        if ( intersectionAttrName == null ) {
            String errmsg = "Intersection does not contain a level from the same dimension as target level " + targetLevel.getName();
            //Trace.write( Trace.error, errmsg );
            throw new CdsException(errmsg);
        }

        IIntStore intersectionInds = getIntAttribute( intersectionAttrName );
        inds = ArrayOps.index( inds, intersectionInds );

        //  Add this mapping to the intersection so we don't need to traverse the hierarchy next time
        if ( cacheLevelMappings ) {
            cache.put( targetLevel.getName(), ArrayOps.copy(inds) );
        }
        return inds;
    }


    /**
     * Return the attribute id (ordinal) for each item at the intersection
     * @param collector
     * @param attribute
     * @return Mapping from each item at this intersection to the items of the attribute.
     */
    public int[] getTargetAggregationInds( CubeDs collector, LevelAttribute attribute ) {
        int[] intersectionMemberInds = getTargetAggregationInds( collector, attribute.getLevel());
        int[] levelMemberAttributeInds = attribute.getAttributeInds();
        return ArrayOps.index( levelMemberAttributeInds, intersectionMemberInds );
    }




    /**
     * Locate the level that belongs to the given dimension and hierarchy in the intersection, then
     * for each member, return the index of its ancestor at the given upper level.
     * <p>
     * Note that this is for the assumption that a hierarchy is contained in a dimension, which is 
     * likely to change once we add hierarchies that include attributes from multiple 
     * dimensions.
     * 
     * @param dimension the dimension that the hierarchy belongs to
     * @param hierarchyName the name of the hierarchy
     * @param upperLevelName the name of the upper level 
     * @param values the indices of all the upper level members. (0, to length-1)
     * 
     * @return the upper-level ancestor indices of all descendant members in the intersection. 
     * Length is the number of members at the leaf level.
     */
    public int[] projectToIntersection(Dimension dimension, String hierarchyName, String upperLevelName, int[] values) {
        if ( hierarchyName == null ) {
            // use the default hierarchy (first one in the list).
            hierarchyName = dimension.getHierarchyNames().get(0);
            // todo: throw exception here if hierarchy is null
        }
        
        // Find the level in this intersection that belongs to the given dimension.
        String dimensionName = dimension.getName();
        
        HierarchyLevel intersectionLevel = null;
        Collection<HierarchyLevel> intersectionLevels = this.getRelatedLevels();
        for(HierarchyLevel level : intersectionLevels){
        	if(level.getDimensionName().equals(dimensionName)){
        		intersectionLevel = level;
        		break;
        	}
        }

        // If the intersection can't be reached from this level, return null
        if (intersectionLevel==null )
            return null;
        if ( ! isAtOrAbove( dimension.getHierarchy( hierarchyName), upperLevelName, intersectionLevel.getName()))
            return null;

        int[] resultInds = null;
        
        List<HierarchyLevel> levels = dimension.getHierarchy( hierarchyName );
        HierarchyLevel parentLevel = null;
        for(HierarchyLevel level : levels) {  // going from top towards the bottom.
            if (level.getName().equals(upperLevelName))  {
                resultInds = Sequence.getSequence( dimension.getLevel( upperLevelName ).getMemberCount() );
                parentLevel = level;
            } else {
                if (resultInds == null)
                    continue; // haven't found the upper level yet
                
                // at this point, the resultInds holds the indices of the 
                // parent level members

                // get the indices of the parent members in its level.
                int[] parentInds = getParentInds( level, parentLevel );
                resultInds = ArrayOps.index( resultInds, parentInds );
                parentLevel = level;
            }
            
            if(level.getName().equals(intersectionLevel.getName())){
            	break;
            }
        }
        
        int[] result = ArrayOps.index( values, resultInds );
        return result;
    }

    /**
     * Return the subscripts in a parent level of the members of this level, which must already contain an
     * IIntStore attribute whose name equals the name of the parent level
     *
     * TODO: consider moving this into a class of hierarchy manipulation operations
     * 
     * @param level the level from which we are gathering the parent indices of its members.
     * @param parentLevel the parent level
     * 
     * @return indices of the parent in its level for every member in the current level.
     */
    static int[] getParentInds( HierarchyLevel level, HierarchyLevel parentLevel ) {
        IIntStore parentInds = level.getIntAttribute( parentLevel.getName());
        if ( parentInds == null ) {
            String msg =  "Level " + level.getName() + " does not have a link to parent level " + parentLevel.getName() ;
            //Trace.write( Trace.error, msg);
            throw new CdsException( msg );
        }
        int[] result = new int[ parentInds.size() ];
        for( int i=0; i<result.length; i++ ) {
            result[i] = parentInds.getElement(i);
        }
        return result;
    }

    /**
     * Delete the IIntStore attribute, if any, that contains indices into a HierarchyLevel.
     * Note that this assumes that no more than one attribute on the Intersection will 
     * reference an instance of a HierarchyLevel.
     * @param level
     */
    void removeReferenceTo( HierarchyLevel level ) {
        if ( levelToAttributeMap.containsKey( level ) ) {
            levelToAttributeMap.remove( level );
        }

        if ( relatedLevelsMap.containsKey( level.getDimensionName())) {
            relatedLevelsMap.remove( level.getDimensionName());
            recalculateLookupKeys();
        }

        super.removeAttributeStore(level.getIdentityAttributeName());
    }


    /**
     * Returns a BitVector (wrapping a BitSet) that marks the positions on the intersection that are children
     * of the a tuple of members of hierarchy levels.  Each LevelMember in the tuple must belong to a Dimension
     * with a level that intersects on this Intersection.  It is not necessary to specify a LevelMember for every
     * related level on the intersection.  An empty tuple selects empty shadow.
     * @param tuple
     * @return BitVector that is true for every item on the intersection that is a child of param tuple.
     */
    public BitVector getTupleShadow( CubeDs collector, LevelMemberTuple tuple ) {
        Set<LevelMember> members = tuple.getLevelMembers();
        if ( members.size() == 0 )
            return new BitVector( this.size());

        BitVector shadow = null;
        for( LevelMember member : members ) {
            BitVector selected = new BitVector(member.getLevel().getMemberCount());
            selected.set( member.getMemberId(), true );
            int[] mapping = this.getTargetAggregationInds(collector, member.getLevel());
            if ( ArrayOps.eq(mapping,-1).all())
                return new BitVector(this.size()); // all false
            selected = ArrayOps.index( selected, mapping);
            if ( shadow == null )
                shadow = selected;
            else
                shadow.andInto( selected );

        }
        return shadow;
    }

    /**
     * Compute the shadow of a tuple of level members on a time series measure
     * @param collector
     * @param tuple  Levels and members whose shadow is to be calculated.  It is not necessary to specify a
     *               member for every related level on the dimension. An empty tuple returns empty shadow.
     * @param bitSet  The mask that selects the time series members.
     * @return  BitMatrix containing a copy of the input BitSet for each element on the intersection that 
     * are in the shadow of the tuple.
     */
    public BitMatrix getTupleShadowMatrix( CubeDs collector, LevelMemberTuple tuple, BitSet bitSet ) {
        BitMatrix result = new BitMatrix( this.size(), bitSet.length() );
        BitVector shadow = getTupleShadow( collector, tuple );
        int[] rows = shadow.where();
        for( int row : rows ) {
            result.setElementAt(row, bitSet);
        }
        return result;
    }


    /**
     * Returns the total size in bytes occupied by this intersection, including all attributes and 
     * measures stored on this intersection. The size includes the stores for related levels and any 
     * cached mappings to higher levels on the dimensions.
     * 
     * @return  Approximate data size in bytes.
     */
    public long getTotalDataSize() {
    	// FIXME the javadoc claims this will include also the space taken by the caches and maps.
    	// but we are not doing that.
        long total = 0;
        Set<String> allAttributes = getAttributes().keySet();
        for( String attrName : allAttributes ) {
            IDataStore store = getAttribute(attrName);
            total += store.getDataSize();
        }

        return total;
    }


    /**
     * Test if a level in a hierarchy is the same or an ancestor level of another level.
     * 
     * @param hierarchy  Ordered list of levels in a hierarchy, topmost first.
     * @param ancestorLevelName the name of the intended ancestor level.
     * @param descendantLevelName the name of the intended descendant level
     * @return true if the given ancestor level is the same as or an ancestor of the 
     * given descendant level. false if it is not, or if one of the two given levels
     * can not be found in the hierarchy.
     */
    private boolean isAtOrAbove( List<HierarchyLevel> hierarchy, String parentLevelName, String childLevelName ) {
        // This is rather a slow thing to do many many times.  Maybe optimize?
        int iparent = -1;
        int ichild = -1;
        int size = hierarchy.size();
        for( int i=0; i<size; i++ ) {
            if ( hierarchy.get(i).getName().equals(parentLevelName)) iparent = i;
            if ( hierarchy.get(i).getName().equals(childLevelName)) ichild = i;
        }
        if ( iparent == -1 || ichild == -1 || ichild < iparent )
            return false;  // it is not a child of this parent in this hierarchy
        else
            return true;
    }

    // Fast lookup of tuples of related level indices.


    /**
     * Locate the position in the Intersection where the specified related levels member indices occur.
     * @param relatedLevelsIds A tuple of valid member indices (positions in the HierarchyLevel) for each
     *                         related level on this intersection.  The order must correspond to the order
     *                         returned in getRelatedLevels().
     * @return The position in the intersection if the tuple exists, or -1 if it does not.
     */
    public int lookup( int[] relatedLevelsIndices ) {
        long key = packRelatedLevelsInds( relatedLevelsIndices );
        Integer index = cartesianTupleMap.get(key);
        return index == null ? -1 : index;
    }


    private long packRelatedLevelsInds( int[] relatedLevelsIds ) {
        return Cartesian.pack( relatedLevelsCardinality, relatedLevelsIds );
    }

    /**
     * Locate the position in the Intersection where the specified related levels member Ids occur,
     * adding the tuple to the intersection if it is not already there.
     * @param relatedLevelsInds A tuple of valid member ids (positions in the HierarchyLevel) for each
     *                         related level on this intersection.  The order must correspond to the order
     *                         returned in getRelatedLevels().
     * @return The position in the intersection of the existing or newly added tuple.
     */
    public int lookupOrAdd(int[] relatedLevelsInds ) {
        int index = lookup( relatedLevelsInds );
        if ( index == -1 ) {
            // Not found; add it to the intersection
            for( int i=0; i<relatedLevelsInds.length; i++ ) {
                index = relatedLevelsStores[i].addElement( relatedLevelsInds[i] );
            }
            long key = packRelatedLevelsInds( relatedLevelsInds );
            this.cartesianTupleMap.put(key,index);
        }
        return index;
    }

    public int addKey(int index, int[] relatedLevelsInds ) {
        for( int i=0; i<relatedLevelsInds.length; i++ ) {
            relatedLevelsStores[i].setElementAt(index, relatedLevelsInds[i]);
        }
        long key = packRelatedLevelsInds(relatedLevelsInds);
        this.cartesianTupleMap.put(key,index);
        return index;
    }

	/**
	 * Returns key.
	 * 
	 * @param index
	 *            Row index for which to return key.
	 * @param relatedLevelsInds
	 *            Key array. Method will populate elements of the array
	 * @return Returns relatedLevelsInds. It is provided for convenience.
	 */
	public int[] getKey(int index, int[] relatedLevelsInds) {
		for (int i = 0; i < relatedLevelsInds.length; i++) {
			relatedLevelsInds[i] = relatedLevelsStores[i].getElement(index);
		}
		return relatedLevelsInds;
	}

    /**
	 * Recalculate related stores and map used by lookup method. This method
	 * msut be invoked in two cases
	 * <ul>
	 * <li>When we added dimension members after intersection was created.
	 * Intersection gets dimension sizes during construction and creates lookup
	 * map which is based on these sizes. So if a dimension size increased after
	 * intersection was created then we would need to invoke this method. If
	 * dimension size decreased then recalculation is not required.
	 * <li>If intersection stores with indexes to dimensions were populated
	 * directly without using method {@linkplain #addKey(int, int[])}, but we
	 * still want to use lookup methods after intersection is populated. In this
	 * case lookup will not find any entry until this method is invoked.
	 * </ul>
	 */
    public void recalculateLookupKeys() {
        // Recalculate cardinality
        initializeRelatedLevelsStores();
        
        // Recalculate lookup keys
        this.cartesianTupleMap.clear();
        int relatedLevelCount = relatedLevelsMap.size();
        int[] relatedLevelsInds = new int[relatedLevelCount];
        for (int iRow = 0, iRowN = this.getMemberCount(); iRow < iRowN; iRow++) {
            for (int iRelatedLevel = 0; iRelatedLevel < relatedLevelCount; iRelatedLevel++) {
                relatedLevelsInds[iRelatedLevel] = relatedLevelsStores[iRelatedLevel].getElement(iRow);
            }
            long key = packRelatedLevelsInds(relatedLevelsInds);
            this.cartesianTupleMap.put(key, iRow);
        }
    }
}

