package com.ram.ds.cds;

import java.io.Serializable;

import java.util.*;

import com.ram.ds.cds.aggregator.Aggregator;
import com.ram.ds.cds.filters.BitSetFilter;
import com.ram.ds.cds.filters.IFilter;
import com.ram.ds.cds.filters.LevelFilter;
import com.ram.ds.cds.filters.StringFilter;
import com.ram.ds.cds.util.Pair;

public class CubeDs implements Serializable {

    private static final long serialVersionUID = -1355818188050136407L;
    private final HashMap<String, Dimension> dimensionMap = new HashMap<>(4);
    private final HashMap<String, Intersection> intersectionMap = new HashMap<>(10);

    /**
     * Container for any optional application specific objects that should be serialized and persisted
     * with this Collector instance.
     */
    private final HashMap<String, Serializable> applicationDataMap= new HashMap<>();

    /**
     * Get application data object in Collector; will be serialized with the Collector when persisted.
     * Application data can be any application-specific object or collection that should be included with the Collector instance
     * in serialization.
     * @param key
     * @return Object in this collector instance's application data that is associated with key.
     */
    public Serializable getApplicationData( String key ) {
        return applicationDataMap.get(key);
    }

    /**
     * Set application data object in Collector; will be serialized with the Collector when persisted.
     * Application data can be any application-specific object or collection that should be included with the Collector instance
     * in serialization.
     * @param key
     * @param value
     * @return  The previous data object associated with this key, or null if none.
     */
    public Serializable setApplicationData( String key, Serializable value ) {
        return applicationDataMap.put( key, value );
    }

    /**
     * Add a dimension to the collector.  Note, you should add all of the dimensions before you add the intersections.
     *
     * @param dimension the new dimension
     */
    public void addDimension(Dimension dimension) {
        dimensionMap.put(dimension.getName(), dimension);
    }

    /**
     *
     * @param dimName
     * @return Reference to the named Dimension, or null if it does not exist.
     */
    public Dimension getDimension(String dimName) {
        return dimensionMap.get(dimName);
    }

    /**
     *
     * @return List of all Dimensions defined in the collector
     */
    public List<Dimension> getDimensions(){
    	return new ArrayList<>(dimensionMap.values());
    }

    /**
     *
     * @param dimName Name of the Dimension to be removed.  Removing a Dimension causes its HierarchyLevels
     *                to be deleted and deletes any references to its level in any Intersections.  A Dimension cannot
     *                be removed if it contains a level that is used as a primary related level in any intersection.
     * @throws CdsException if the dimension is used as one of the primary dimensions on any intersection.
     */
    public void removeDimension( String dimName ) {
        Dimension dim = getDimension( dimName );
        if ( dim == null )
            return; // no such dimension
        for( HierarchyLevel level : dim.getLevels() ) {
            // A Dimension can not be deleted if it is one of the primary dimensions on any intersection
            for( Intersection intersection : getIntersections()) {
                String[] dimensionNames = intersection.getPrimaryKeyDimensions();
                for( String name : dimensionNames ) {
                    if ( name.equals(dimName)) {
                        throw new CdsException("Dimension " + dimName + " cannot be removed ");
                    }
                }
                intersection.removeReferenceTo( level );
            }
        }
        dimensionMap.remove( dimName );
    }

    /**
     * Run the aggregators through those immediate children of the given parent that
     * fits all the filters. The filters may be installed against any descendant 
     * level, not necessarily the immediate children.
     *
     * <p>Alternatively, Dimension.getChildren may be used to enumerate the children of a level member, and is
     * often many times faster.</p>
     * 
     * @param dimName           "Product"
     * @param hierarchyName     "Product Hierarchy"
     * @param parentLevelName   "Category"
     * @param parentAttribute   "Name"
     * @param parentValue       "Category-1"
     * @param aggregators       [MemberAggregator for lower level]
     * @param additionalFilters []
     */
    public void getChildren(String dimName, String hierarchyName, String parentLevelName, String parentAttribute,
                            String parentValue, Aggregator[] aggregators,
                            IFilter[] additionalFilters) {

        Dimension dimension = getDimension(dimName);
        HierarchyLevel parentLevel = dimension.getLevel(parentLevelName);

        List<IFilter> allFilters = new LinkedList<IFilter>();
        if (additionalFilters != null) {
            allFilters.addAll(Arrays.asList(additionalFilters));
        }
        // the next line seems to be useless ... do we really need it?
        allFilters.add(new StringFilter(parentLevel, parentAttribute, parentValue));
        
        List<HierarchyLevel> hierarchy = dimension.getHierarchy(hierarchyName);

        // split the hierarchy into the target level and levels below the target level.
        //
        // Looping from bottom up along the hierarchy until we reach the parent level.
        // For each level, collect the applicable filters and store them in the 
        // level->filters collection.
        boolean foundLevel = false;
        List<Pair<HierarchyLevel, List<IFilter>>> descendantLevels = new ArrayList<Pair<HierarchyLevel, List<IFilter>>>();
        for (int i = hierarchy.size() - 1; i >= 0 ; i--) {
            HierarchyLevel currentLevel = hierarchy.get(i);
            if (currentLevel == parentLevel) {
                foundLevel = true;
                // break out once we reach the parent level, so this loop effectively is only going
                // through the descendant levels in the hierarchy.
                break;
            }
            // For each descendant level, collect the applicable filters and store them in the 
            // level->filters collection.
            addLevelAndFilterToCollection(allFilters, descendantLevels, currentLevel);
        }

        if (!foundLevel) {
        	throw new RuntimeException("Not found");
        }
        
        // At this point, the descendantLevels collection contains one and only one
        // entry for each level below the parent. For each level, there could be 
        // any number of filters. 

        // descendants are in REVERSE order (from the bottom of the tree up)
        int startingLevel = 0;
        for ( ; startingLevel < descendantLevels.size(); startingLevel++) {
            if (!descendantLevels.get(startingLevel).getRight().isEmpty()) {
                break;    
            }
        }
     
        // startingLevel points to the index of the lowest level in the collection 
        // that has at least one applicable filter. Or it may be pointing to the 
        // parent level.

        for (int i = startingLevel; i < descendantLevels.size() - 1; i++) {
            HierarchyLevel currentLevel = descendantLevels.get(i).getLeft();
            String parentAttrName =  descendantLevels.get(i+1).getLeft().getName();
            // Get the upper level bit mapping for the the parents of the members 
            // in the current level that also fits the filters.
            BitSet parentBits = currentLevel.getParentSelector(descendantLevels.get(i).getRight(), parentAttrName);
            
            // Create and install a level filter on the upper level that will
            // match only the members whose descendants matches all the filters
            // against their owner levels.
            Pair<HierarchyLevel, List<IFilter>> nextLevel = descendantLevels.get(i+1);
            nextLevel.getRight().add(new LevelFilter(nextLevel.getLeft(), parentBits));
        }

        HierarchyLevel targetLevel = descendantLevels.get(descendantLevels.size() - 1).getLeft();
        targetLevel.aggregate(descendantLevels.get(descendantLevels.size() - 1).getRight(), aggregators);
    }

    /**
     * Collect the filters targeted at the current level and put them into the collection.
     * If none of the filters is applicable to the current level, an entry with empty filter list
     * will be put into the collection.
     * 
     * Any applicable filters will also be removed from the list of all filters.
     * 
     * @param allFilters		input/output
     * @param ancestorLevels    output
     * @param currentLevel      input
     */
    private static void addLevelAndFilterToCollection(List<IFilter> allFilters,
                                                      List<Pair<HierarchyLevel, List<IFilter>>> ancestorLevels,
                                                      HierarchyLevel currentLevel)
    {
        List<IFilter> currentLevelFilters = new ArrayList<IFilter>();
        for (int j = 0; j < allFilters.size(); j++) {
            if (currentLevel == allFilters.get(j).getAttributeContainer()) {
                currentLevelFilters.add(allFilters.remove(j));
            }
        }
        ancestorLevels.add(new Pair<HierarchyLevel, List<IFilter>>(currentLevel, currentLevelFilters));
    }


    /**
     * Perform an aggregation using the given aggregators.  The aggregation will be performed on 
     * the specified intersection for the given hierarchies and will apply the given filters.
     *
     * <p>When large datasets are in use, using AggregationController
     * (@see com.jda.common.mdap.aggregation.AggregationController) is usually much faster.</p>
     *
     * @param iDimensionNameToHierarchyNameMap
     *            For each dimension, provide the hierarchy name to be used for
     *            aggregation.  Even if there is only one hierarchy used, you
     *            will need to provide the hierarchy name.
     * @param iIntersectionName 
     *            The name of the intersection.  For example, if you have an intersection
     *            at "Class" on the product dimension and "Region" on the location dimension,
     *            you might name that "ClassRegion", whereas the intersection at the leaf
     *            level might be called "ProductStore"
     * @param aggregators       
     *            A list of aggregators to be used in aggregation.  Each aggregator will
     *            be called once for each element which matches the given intersection, 
     *            hierarchies, and filters
     * @param iFilters
     *            An array of filters.  The isMatch() must return true for each and every
     *            filter in order for the element in the intersection to be considered a 
     *            match. Any matching element will be passed to the aggregator.
     */
    public void aggregate(Map<String, String> iDimensionNameToHierarchyNameMap,
                          String iIntersectionName,
                          Aggregator[] aggregators,
                          IFilter[] iFilters) {

        // build a list of filters for each dimension and list of filters for the intersection
        List<IFilter> filtersForIntersection = new ArrayList<IFilter>(iFilters.length);
        
        Map<String, List<IFilter>> dimensionToFiltersMap = new IdentityHashMap<String, List<IFilter>>();
        for (IFilter filter : iFilters) {
            IAttributeContainer container = filter.getAttributeContainer();
            if (container instanceof HierarchyLevel) {
                // the filter is for a dimension, so put it in the correct bucket
                List<IFilter> filterList = dimensionToFiltersMap.get(((HierarchyLevel) container).getDimensionName());
                if (filterList == null) {
                    filterList = new ArrayList<IFilter>(iFilters.length);
                    dimensionToFiltersMap.put(((HierarchyLevel) container).getDimensionName(), filterList);
                }
                filterList.add(filter);
            } else {
                filtersForIntersection.add(filter);
            }
        }

        // get a selector for each dimension and add them as a filter onto the intersection filter list 
        Intersection targetIntersection = intersectionMap.get(iIntersectionName);
        for(Map.Entry<String, List<IFilter>> dimFiltersEntry : dimensionToFiltersMap.entrySet()){
        	String dimensionName = dimFiltersEntry.getKey();
            List<IFilter> filters = dimFiltersEntry.getValue();
            if(filters != null){
            	HierarchyLevel intersectionLevel = targetIntersection.getRelatedLevel(dimensionName);
                if ( intersectionLevel==null ) {
                    // This is a valid case, for example, if the level is on the TIME dimension and there is no related
                    // level for that dimension on the intersection.  The filter in this case has no effect.
                    continue;
                }
                String hierarchyName = iDimensionNameToHierarchyNameMap.get(dimensionName);
            	String intersectionLevelName = intersectionLevel.getName();
                BitSetSelector selector = getDimension(dimensionName).getSelector(hierarchyName, intersectionLevelName, filters, false);
                BitSet bitset = selector.getBitSet();
                if (bitset.isEmpty()) {
                	return;
                }
                filtersForIntersection.add(0, new BitSetFilter(targetIntersection, intersectionLevelName, bitset));
            }
        }
        
        // finally, accumulate using the combined filters of what the user passed in for the intersection and
        // the filters we built for the intersection levels from each dimension
        targetIntersection.aggregate(filtersForIntersection, aggregators);
    }

    /**
     * Perform an aggregation using the given aggregators.  The aggregation will be performed on the specified
     * intersection using all of the elements at this intersection.
     *
     * <p>When large datasets are in use, using AggregationController
     * (@see com.jda.common.mdap.aggregation.AggregationController) is usually much faster.</p>
     *
     * @param iIntersectionName 
     *            The name of the intersection.  For example, if you have an intersection
     *            at "Class" on the product dimension and "Region" on the location dimension,
     *            you might name that "ClassRegion", whereas the intersection at the leaf
     *            level might be called "ProductStore"
     * @param aggregators
     *            a list of aggregators to be used in aggregation.  Each aggregator will
     *            be called once for each element which matches the given intersection, 
     *            hierarchies, and filters
     */
    public void aggregate(String iIntersectionName,
                          Aggregator[] aggregators) {
        Intersection targetIntersection = intersectionMap.get(iIntersectionName);
        // accumulate using all of the elements at this intersection
        targetIntersection.aggregate(aggregators);

    }

    /**
     * Add an intersection between two or more dimensions.  You should add the Dimensions to the collector before adding
     * intersections.
     *
     * @param iIntersectionName the iIntersectionName of the intersection
     * @param iRelatedLevels    the levels for which this intersection exists
     */
    public Intersection addIntersection(String iIntersectionName, HierarchyLevel[] iRelatedLevels) {
        Intersection newIntersection = new Intersection(iIntersectionName, iRelatedLevels);
        intersectionMap.put(iIntersectionName, newIntersection);

        return newIntersection;
    }



    /**
     *
     * @param iIntersectionName
     * @return Intersection with the given name, or null if none exists.
     */
    public Intersection getIntersection(String iIntersectionName) {
        return intersectionMap.get(iIntersectionName);
    }

    /**
     * Removes intersection object
     * @param iIntersectionName
     */
    public void removeIntersection(String iIntersectionName){
    	this.intersectionMap.remove(iIntersectionName);
    }
    /**
     *
     * @return  Collection (ArrayList&lt;Intersection$gt;) of Intersections that exist in this Collector instance.
     */
    public Collection<Intersection> getIntersections(){
    	return new ArrayList<>(intersectionMap.values());
    }

    @Override
    public String toString() {
        return "Collector{" +
                "dimensionMap=" + dimensionMap +
                ", intersectionMap=" + intersectionMap +
                '}';
    }
}
