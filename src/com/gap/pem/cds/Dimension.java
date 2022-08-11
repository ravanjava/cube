package com.gap.pem.cds;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.gap.pem.cds.aggregator.Aggregator;
import com.gap.pem.cds.aggregator.ShadowAggregator;
import com.gap.pem.cds.filters.BitSetFilter;
import com.gap.pem.cds.filters.ElementFilter;
import com.gap.pem.cds.filters.IFilter;
import com.gap.pem.cds.filters.LevelFilter;
import com.gap.pem.cds.stores.IIntStore;
import com.gap.pem.cds.util.ArrayOps;
import com.gap.pem.cds.util.BitVector;

/**
 *  Represents a collection of levels that pertain to a meaningful characteristic of data items, such as
 *  the geographical location, position in a product line, or points in time.  A dimension contains a set
 *  of one or more levels (@see HierarchyLevel), organized into one or more hierarchies.
 *
 */
public class Dimension implements Serializable
{
    private static final long serialVersionUID = -5608719643589553803L;
    
    private String name;
    private HashMap<String, HierarchyLevel> levels = new HashMap<>();
    private HashMap<String, List<HierarchyLevel>> hierarchyMap = new HashMap<>();

    /**
     * Create a dimension with the given name.
     *
     * @param name      the name of the dimension
     *
     */
    public Dimension(String name)
    {
        this.name = name;
    }

    /**
     * Define a hierarchy.  All of the levels should be added to the dimension before adding the hierarchy.
     *
     * @param iHierarchyName        the name of the hierarchy.
     * @param iOrderedHierarchy     the list of the levels.  The first element is the top of the hierarchy, the last
     *                              is the leaf-level.
     */
    public void addHierarchy(String iHierarchyName, List<HierarchyLevel> iOrderedHierarchy)
    {
        for (int i = 0; i < iOrderedHierarchy.size(); i++) {
            HierarchyLevel level = iOrderedHierarchy.get(i);
            if (i < iOrderedHierarchy.size() - 1) {
                HierarchyLevel nextLevel = iOrderedHierarchy.get(i + 1);
                if (nextLevel.getIntAttribute(level.getName()) == null) {       // the parent level might already exist on the child level
                    nextLevel.addParentAttribute(level.getName());
                }
            }
        }
        hierarchyMap.put(iHierarchyName, iOrderedHierarchy);
    }
    

    /**
     * Equivalent to the {@link #getSelector(String, String, List, boolean)} method with the
     * last boolean parameter forced to be false.
     * 
     * @deprecated use {@link #getSelector(String, String, List, boolean)} instead. If all the
     * filters are at or above the target level, you can pass either true or false for the 
     * <code>includeLowerLevel</code> flag. The flag is only significant when there are filters
     * in the list that are defined on levels below the target level. In such use cases, 
     * evaluate the requirement and pass in a right value for the flag.
     */
    public BitSetSelector getSelector(String hierarchyName,
                               String targetLevelName,
                               List<IFilter> levelFilters) {
    	return this.getSelector(hierarchyName, targetLevelName, levelFilters, false);
    }

    /**
     * Get the bit set selector on a target level in the given hierarchy that will match only the 
     * members matching all the filters.
     * <p>
     * If the filter list as null or empty, all the members in the target level will be returned.
     * <p>
     * If any of the filters is defined on levels lower than the target level in hierarchy, the
     * behavior is decided by the <code>includeLowerLevel</code> flag. When the flag is false, the 
     * method returns an empty selector. Otherwise, the returned selector will have the bit set 
     * for a member whose descendants match the filters on the lower levels.
     * <p>
     * If any of the filters is defined on a level not found in this dimension by the given
     * hierarchy, or if it is defined on an intersection, the method returns an empty selector.
     * 
     * @param hierarchyName    
     *           a hierarchy in this dimension that includes the target level and the levels
     *           the filters are defined on.
     * @param targetLevelName        
     *            the level for which we are getting the selector. 
     * @param levelFilters         
     *            filters for hierarchy levels in this dimension.
     * @param includeLowerLevel
     * 			  flag that controls the behavior when any of the filters in the list is defined
     *            at a level below the target level.
     * 
     * @return 
     *      A bit set selector that is defined on the target level, with the bits set to true for
     *      members matching all the filters.
     *      
     * @since 8.2
     */
    public BitSetSelector getSelector(String hierarchyName, String targetLevelName, List<IFilter> levelFilters,
    		boolean includeLowerLevel){
        List<HierarchyLevel> hierarchy = hierarchyMap.get(hierarchyName);

        Map<IAttributeContainer, List<IFilter>> hierarchyLevelToFiltersMap = new IdentityHashMap<>();
        HierarchyLevel targetLevel = getLevel(targetLevelName);
        
        // group the filters by the levels they are defined on. If there are any filters that
        // is not defined on a level (must be an intersection) or not for a level in the given
        // hierarchy.
        if(levelFilters != null) {
        	boolean allFilterValid = true;
	        for (IFilter levelFilter : levelFilters) {
	            IAttributeContainer filterContainer = levelFilter.getAttributeContainer();
	            
	            // process the hierarchy in order so that we can throw out filters below the target level
	            boolean filterValid = false;
	            for (HierarchyLevel hierarchyLevel : hierarchy) {
	            	// we are here if the level is at or above the target level.
	                if (hierarchyLevel == filterContainer) {
	                	// collect the filter for the level
	            		List<IFilter> filtersForCurrentLevel = hierarchyLevelToFiltersMap.get(filterContainer);
	            		if (filtersForCurrentLevel == null) {
	            		    filtersForCurrentLevel = new ArrayList<IFilter>();
	            		    hierarchyLevelToFiltersMap.put(filterContainer, filtersForCurrentLevel);
	            		}
	                    filtersForCurrentLevel.add(levelFilter);
	                    filterValid = true;
	                    break;
	                }
	                
	                if(hierarchyLevel == targetLevel){
	                	// if we have reached the target level but still didn't find the level 
	                	// where the filter is defined on ...
	                	if(includeLowerLevel == false){
	                		// if the flag is false, then break out (and filterValid
	                		// will be false).
	                		break;
	                	}
	                	// else if the flag is true, keep going down the hierarchy to
	                	// find the level the filter was defined on.
	                }
	            }
	            
	            if(!filterValid){
	            	// reach here if the filter is not defined on a level in this hierarchy. 
	            	// or the filter is defined on a level below target level while the 
	            	// includeLowerLevel flag is false.
	            	allFilterValid = false;
	            	break;
	            }
	        }
	        
	        if(!allFilterValid){
	        	return new BitSetSelector(targetLevelName); // empty selector
	        }
        }
        
        if(includeLowerLevel) {
            // bring the filters at the levels below the target level to the target level
	        for (int i = hierarchy.size()-1; i >= 0; i--) // from bottom to top
	        {
	            HierarchyLevel currentLevel = hierarchy.get(i);
	            if(currentLevel == targetLevel){
	            	// reached the target level, break out.
	            	break;
	            }
	            
	            // reach here if we are at a level below the target level
	            // because we are looping from the bottom level upwards.
	    		List<IFilter> filtersForLevel = hierarchyLevelToFiltersMap.get(currentLevel);
	    		if(filtersForLevel == null || filtersForLevel.isEmpty()){
	    			continue;
	    		}
	    		
	    		// reach here if we have filters on a level below the target level. Translate
	    		// and replace them with a equivalent filter on the parent level.
	            HierarchyLevel parentLevel = hierarchy.get(i-1);
	    		BitSet parentBitSet = currentLevel.getParentSelector(filtersForLevel, parentLevel.getName());
	    		LevelFilter parentFilter = new LevelFilter(parentLevel, parentBitSet);
	    		
	            // add the current selector to the list of filters for the parent level
	            // and prepare for the next iteration of the loop
	            List<IFilter> filtersForParentLevel = hierarchyLevelToFiltersMap.get(parentLevel);
	            if(filtersForParentLevel == null) {
	            	filtersForParentLevel = new ArrayList<IFilter>();
	            	hierarchyLevelToFiltersMap.put(parentLevel, filtersForParentLevel);
	            }
	            filtersForParentLevel.add(parentFilter); 
	            
	            // remove the filters on the lower level because a replacement has been
	            // placed on the parent level in the previous block.
	            hierarchyLevelToFiltersMap.remove(currentLevel);
	        }
        }
        
        // cast all the filters at or above the target level down to the target level.
		BitSetSelector targetSelector = null;
        for (int i = 0; i < hierarchy.size(); i++)
        {
            HierarchyLevel currentLevel = hierarchy.get(i);
            List<IFilter> filtersForLevel = hierarchyLevelToFiltersMap.get(currentLevel);
            
            // gets the selector for all of the members that fits all the 
            // filters on this level.
            BitSetSelector selector = currentLevel.getSelector(filtersForLevel);
            
            if (currentLevel == targetLevel) {
                // we have the selector for the target level, so we're done
            	targetSelector = selector;
                break;
            }
            
            // reach here if we are at a level above the target level
            
            // Build a filter on the immediately lower level, matching by parent indices.
            HierarchyLevel nextLevel = hierarchy.get(i+1);
            BitSetFilter newFilter = new BitSetFilter(nextLevel,
                        currentLevel.getName(),
                        selector.getBitSet());

            // add the new filter to the list of filters for the next level down
            // and prepare for the next iteration of the loop
            List<IFilter> filtersForNextLevel = hierarchyLevelToFiltersMap.get(nextLevel);
            if(filtersForNextLevel == null) {
            	filtersForNextLevel = new ArrayList<IFilter>();
            	hierarchyLevelToFiltersMap.put(nextLevel, filtersForNextLevel);
            }
            filtersForNextLevel.add(newFilter); 
        }
	        
		return targetSelector;
	}
    
    public void addLevel(HierarchyLevel newLevel)
    {
        levels.put(newLevel.getName(), newLevel);
    }

    public HierarchyLevel getLevel(String name)
    {
        return levels.get(name);
    }

    /**
     * List the levels in the dimension.  Result is in arbitrary order and does not correspond to a hierarchy.
     * @return   All levels in the intersection.
     */
    public List<HierarchyLevel> getLevels(){
    	return new ArrayList<HierarchyLevel>(levels.values());
    }

    public String getName()
    {
        return name;
    }

    /**
     *
     * @param hierName Name of the hierarchy
     * @return Ordered list of names of levels in the hierarchy starting with the top level.
     */
    public List<HierarchyLevel> getHierarchy (String hierName)
    {
        return hierarchyMap.get(hierName);
    }

    /**
     *
     * @return Names of hierarchies in the dimension
     */
    public List<String> getHierarchyNames(){
    	return new ArrayList<>(hierarchyMap.keySet());
    }
    
    /**
     * Get the immediate children of the given element according to the given hierarchy.
     * 
     * @param hierarchyName the name of a hierarchy in this dimension
     * @param element an element in this dimension, could be null.
     * 
     * @return a list of child elements at the level immediately below the given element in
     *         the hierarchy. If the given element is at the leaf level, an empty list will
     *         be returned. If the given element is null,  or if the level name in the given
     *         element is null, then the members at the top level of the hierarchy will be 
     *         returned.
     * 
     * @throws IllegalArgumentException if the given element does not belong to this
     *         dimension, or if its level does not exist in this dimension, or if the
     *         given hierarchy does not exist in this dimension.
     *         
     * @throws IllegalStateException if the hierarchy with the given name exists but it
     *         doesn't contain any levels.
     */
    public List<Element> getChildElements(String hierarchyName, Element element){
    	String parentDimensionName = this.getName();
    	String parentLevelName = null;
    	int parentElementIndex = -1;
    	if(element != null){
    		parentDimensionName = element.getDimensionName();
    		parentLevelName = element.getLevelName();
    		parentElementIndex = element.getElementIndex();
    	}
    	
    	if(this.name.equals(parentDimensionName) == false){
    		throw new IllegalArgumentException("Element dimension " + parentDimensionName +
    				" doesn't match dimension name " + name);
    	}
    	
    	if(hierarchyMap.containsKey(hierarchyName) != true){
    		throw new IllegalArgumentException("Hierarchy " + hierarchyName + 
    				" does not exist in dimension " + name);
    	}

    	List<HierarchyLevel> levels = hierarchyMap.get(hierarchyName);
    	if(levels == null || levels.isEmpty()){
    		throw new IllegalStateException("Hierarchy " + hierarchyName + 
    				" does not have any levels in dimension " + name);
    	}
    	
    	HierarchyLevel parentLevel = null;
    	HierarchyLevel childLevel = null;
    	
    	if(parentLevelName == null){
    		childLevel = levels.get(0);
    	} else {
    		for(int i = 0, n = levels.size(); i < n; i ++){
    			HierarchyLevel level = levels.get(i);
    			if(level.getName().equals(parentLevelName)){
    				parentLevel = level;
    				if(i < n-1){
    					childLevel = levels.get(i+1);
    				}
    				break;
    			}
    		}
        	
        	if(parentLevel == null){
        		throw new IllegalArgumentException("Element level " + parentLevelName +
        				" is not in the hierarchy " + hierarchyName);
        	}
    	}

    	List<Element> childElements = new ArrayList<Element>();
    	if(childLevel == null){
    		// the parent is at the leaf level
    		return childElements; // empty list
    	}
    	
    	String childLevelName = childLevel.getName();
    	
    	if(parentLevel == null){
    		// parent level null means we are retrieving top level members
    		for(int i = 0, n = childLevel.getMemberCount(); i < n; i ++){
    			Element childElement = new Element(this.name, childLevelName, i);
    			childElements.add(childElement);
    		}
    		
    	} else {
	    	IIntStore parentIndexStore = childLevel.getIntAttribute(parentLevelName);
	    	for(int i = 0, n = parentIndexStore.size(); i < n; i ++){
	    		if(parentIndexStore.getElement(i) == parentElementIndex){
	    			Element childElement = new Element(this.name, childLevelName, i);
	    			childElements.add(childElement);
	    		}
	    	}
    	}
    	
    	return childElements;
    }
    
    /**
     * Get the descendants of the given element at the targeted level according to the given hierarchy.
     * 
     * @param hierarchyName the name of a hierarchy in this dimension
     * @param element an element in this dimension, could be null.
     * @param targetLevel  a level in this dimension that is below the given element in the hierarchy.
     * 
     * @return 
     * 
     * Case 1: Element or its level is null, and target level is null. In this case,
     * the method returns the top level members.<p>
     * Case 2: Element or its level is null, but the target level is not null. In this
     * case, the method returns all the members at the target level.<p>
     * Case 3: Element and its level is not null, but the target level is null. In this
     * case, the method returns all the child elements of the given element, or an 
     * empty list. Essentially it degenerates into the same behavior as if the user 
     * called {@link #getChildElements(String, Element)} <p>
     * Case 4: Element and its level is not null, and the target level is not null. In 
     * this case, the method will return the descendant elements at the given target 
     * level for the given element.
     * 
     * @throws IllegalArgumentException if the given element does not belong to this
     *         dimension, or if its level does not exist in this dimension, or if the
     *         given hierarchy does not exist in this dimension, or if the target level
     *         doesn't exist in this dimension, or if the target Level is above the
     *         element level.
     *         
     * @throws IllegalStateException if the hierarchy with the given name exists but it
     *         doesn't contain any levels.
     */
    public List<Element> getDescendantElements(String hierarchyName, Element element, HierarchyLevel targetLevel){
    	// Sanity check on "hierarchyName" parameter
    	if(hierarchyMap.containsKey(hierarchyName) != true){
    		throw new IllegalArgumentException("Hierarchy " + hierarchyName + 
    				" does not exist in dimension " + name);
    	}
    	
    	List<HierarchyLevel> levels = hierarchyMap.get(hierarchyName);
    	if(levels == null || levels.isEmpty()){
    		throw new IllegalStateException("Hierarchy " + hierarchyName + 
    				" does not have any levels in dimension " + name);
    	}

    	// Sanity check on "element" parameter
    	String ancestorDimensionName = this.name;
    	String ancestorLevelName = null;
    	int ancestorElementIndex = -1;
    	if(element != null){
    		ancestorDimensionName = element.getDimensionName();
        	if(this.name.equals(ancestorDimensionName) == false){
        		throw new IllegalArgumentException("Element dimension " + ancestorDimensionName +
        				" doesn't match dimension name " + name);
        	}
    		ancestorLevelName = element.getLevelName();
    		ancestorElementIndex = element.getElementIndex();
    	}

    	// Sanity check on "targetLevel" parameter
    	String targetDimensionName = this.name;
    	String targetLevelName = null;
    	if(targetLevel != null){
    		targetDimensionName = targetLevel.getDimensionName();
        	if(this.name.equals(targetDimensionName) == false){
        		throw new IllegalArgumentException("Element dimension " + targetDimensionName +
        				" doesn't match dimension name " + name);
        	}
        	
    		targetLevelName = targetLevel.getName();
    	}
    	
    	HierarchyLevel ancestorLevel = null;
    	HierarchyLevel descendantLevel = null;
    	
    	// If both level names are null, then set descendant to top level.
    	if(ancestorLevelName == null && targetLevelName == null){
    		descendantLevel = levels.get(0);
    	}
    	else if(ancestorLevelName == null){
    		// If ancestor level name is null, but target level is not,
    		// then get the descendant level.
	    	for(int i = 0, n = levels.size(); i < n; i ++){
	    		HierarchyLevel level = levels.get(i);
	    		if(level.getName().equals(targetLevelName)){
	    			descendantLevel = level;
	    		}
	    	}
	    	if(descendantLevel == null){
	    		throw new IllegalArgumentException("Target level " + targetLevelName +
	    				" is not in the hierarchy " + hierarchyName);
	    	}
    	} 
    	else if(targetLevelName == null){
    		// If ancestor level is not null, but target level is null.
    		// then set the descent level to the level immediately 
    		// below the ancestor level. In essence, this call is degenerated
    		// into getChildElements().
    		for(int i = 0, n = levels.size(); i < n; i ++){
    			HierarchyLevel level = levels.get(i);
    			if(level.getName().equals(ancestorLevelName)){
    				ancestorLevel = level;
    				if(i < n-1){
    					descendantLevel = levels.get(i+1);
    				}
    				break;
    			}
    		}
        	
        	if(ancestorLevel == null){
        		throw new IllegalArgumentException("Element level " + ancestorLevelName +
        				" is not in the hierarchy " + hierarchyName);
        	}
    	}
    	else {
    		// If both are not null, then 
	    	for(int i = 0, n = levels.size(); i < n; i ++){
	    		HierarchyLevel level = levels.get(i);
	    		if(level.getName().equals(ancestorLevelName)){
	    			ancestorLevel = level;
	    		}
	    		if(level.getName().equals(targetLevelName)){
	    			if(ancestorLevel == null){
	    				throw new IllegalArgumentException("Target level " + targetLevelName +
	    					" is above element level " + ancestorLevelName);
	    			}
	    			descendantLevel = level;
	    		}
	    	}
	    	
	    	if(ancestorLevel == null){
	    		throw new IllegalArgumentException("Element level " + ancestorLevelName +
	    				" is not in the hierarchy " + hierarchyName);
	    	}

	    	if(descendantLevel == null){
	    		throw new IllegalArgumentException("Target level " + targetLevelName +
	    				" is not in the hierarchy " + hierarchyName);
	    	}
    	}
    	
    	// This could be true if the element is at leaf level and the target level is null.
    	if(descendantLevel == null){
    		return new ArrayList<Element>();
    	}
    	
    	// If the target level is the same as the level of the given ancestor element,
    	// then return empty list.
    	if(ancestorLevel == descendantLevel){
    		return new ArrayList<Element>();
    	}
    	
    	List<IFilter> filters = new ArrayList<IFilter>(1);
    	if(ancestorLevel != null && ancestorElementIndex >= 0){
        	IFilter ancestorFilter = new ElementFilter(ancestorLevel, ancestorElementIndex);
        	filters.add(ancestorFilter);
    	}
        
    	BitSetSelector descendantBitSelector = getSelector(hierarchyName,
    			descendantLevel.getName(), filters, false);
    	
    	BitSet descendantBits = descendantBitSelector.getBitSet();
    	
    	List<Element> descendantElements = new ArrayList<Element>();
    	for (int i = descendantBits.nextSetBit(0); i >= 0; i = descendantBits.nextSetBit(i+1)) {
			Element childElement = new Element(this.name, descendantLevel.getName(), i);
			descendantElements.add(childElement);
    	 }
    	
    	return descendantElements;
    }
    
    /**
     * Get the element that is the immediate parent of the given element according
     * to the given hierarchy.
     * 
     * @param hierarchyName the name of a hierarchy in this dimension
     * @param element an element in this dimension, could be null.
     * 
     * @return the immediate parent element of the given element. If the given
     *         element is at the top level, it returns null. If the given element 
     *         is null or if the level of the given element is null, it will return
     *         null.
     * 
     * @throws IllegalArgumentException if the given element is null, or if it does 
     *         not belong to this dimension, or if its level does not exist in this 
     *         dimension, or if its index does not exist in its level, or if the 
     *         given hierarchy does not exist in this dimension.
     *         
     * @throws IllegalStateException if the hierarchy with the given name exists but it
     *         doesn't contain any levels.
     */
    public Element getParentElement(String hierarchyName, Element element) {
    	if(element == null || element.getLevelName() == null){
    		return null;
    	}
    	
    	String childDimensionName = element.getDimensionName();
    	String childLevelName = element.getLevelName();
    	int childElementIndex = element.getElementIndex();
    	
    	if(this.name.equals(childDimensionName) == false){
    		throw new IllegalArgumentException("Element dimension " + childDimensionName +
    				" doesn't match dimension name " + name);
    	}
    	
    	if(hierarchyMap.containsKey(hierarchyName) != true){
    		throw new IllegalArgumentException("Hierarchy " + hierarchyName + 
    				" does not exist in dimension " + name);
    	}
    	
    	List<HierarchyLevel> levels = hierarchyMap.get(hierarchyName);
    	if(levels == null || levels.isEmpty()){
    		throw new IllegalStateException("Hierarchy " + hierarchyName + 
    				" does not have any levels in dimension " + name);
    	}
    	
    	HierarchyLevel parentLevel = null;
    	HierarchyLevel childLevel = null;
    	
    	for(int i = 0, n = levels.size(); i < n; i ++){
    		HierarchyLevel level = levels.get(i);
    		if(level.getName().equals(childLevelName)){
    			childLevel = level;
    			if(i > 0){
    				parentLevel = levels.get(i-1);
    			}
    			break;
    		}
    	}
    	
    	if(childLevel == null){
    		throw new IllegalArgumentException("Element level " + childLevelName +
    				" is not in the hierarchy " + hierarchyName);
    	}

    	
    	if(parentLevel == null){
    		// the given element is the top level, no parent
    		return null;
    	}

    	String parentLevelName = parentLevel.getName();
    	
    	IIntStore parentIndexStore = childLevel.getIntAttribute(parentLevelName);
    	if(childElementIndex < 0 || childElementIndex >= parentIndexStore.size()){
    		throw new IllegalArgumentException("Element index " + childElementIndex + " is out of bounds");
    	}
    	
    	int parentElementIndex = parentIndexStore.getElement(childElementIndex);
    	Element parentElement = new Element(this.name, parentLevelName, parentElementIndex);
    	return parentElement;
    }
    

    /**
     * Get the element that is the ancestor of the given element at the given level 
     * according to the given hierarchy.
     * 
     * 
     * @param hierarchyName the name of a hierarchy in this dimension
     * @param element an element in this dimension, could be null.
     * @param targetLevel  a level in this dimension that is above the given element in the hierarchy.
     * 
     * @return 
     * Case 1: Element (or its level) is null, and target level is null. In this case,
     * the method returns null.<p>
     * Case 2: Element (or its level) is null, but the target level is not null. In this
     * case, the method returns null.<p>
     * Case 3: Element and its level is not null, but the target level is null. In this
     * case, the method returns the immediate parent of the given element, if it exists. It is
     * essentially degenerates into the same behavior as if the user called 
     * {@link #getParentElement(String, Element)}.<p>
     * Case 4: Element and its level is not null, and the target level is not null. If the target
     * level is the same as the level for the requested element, it returns null. Otherwise, the 
     * method will return the ancestor element at the given target level for the given element.
     * 
     * @throws IllegalArgumentException if the given element does not belong to this dimension, or 
     * 		   if its level does not exist in this dimension, or if the given hierarchy does not 
     *         exist in this dimension, or if the  target level doesn't exist in this dimension, or 
     *         if the target Level is below the element level.
     *         
     * @throws IllegalStateException if the hierarchy with the given name exists but it
     *         doesn't contain any levels.
     */
    public Element getAncestorElement(String hierarchyName, Element element, HierarchyLevel targetLevel) {
    	// Sanity check on "hierarchyName" parameter
    	if(hierarchyMap.containsKey(hierarchyName) != true){
    		throw new IllegalArgumentException("Hierarchy " + hierarchyName + 
    				" does not exist in dimension " + name);
    	}

    	List<HierarchyLevel> levels = hierarchyMap.get(hierarchyName);
    	if(levels == null || levels.isEmpty()){
    		throw new IllegalStateException("Hierarchy " + hierarchyName + 
    				" does not have any levels in dimension " + name);
    	}

    	// Sanity check on "element"  parameter
    	String descendantDimensionName = this.name;
    	String descendantLevelName = null;
    	int descendantElementIndex = -1;
    	if(element != null){
    		descendantDimensionName = element.getDimensionName();
        	if(this.name.equals(descendantDimensionName) == false){
        		throw new IllegalArgumentException("Element dimension " + descendantDimensionName +
        				" doesn't match dimension name " + name);
        	}
        	descendantLevelName = element.getLevelName();
        	descendantElementIndex = element.getElementIndex();
    	}
    	
    	// Sanity check on "targetLevel" parameter
    	String targetDimensionName = this.name;
    	String targetLevelName = null;
    	if(targetLevel != null){
    		targetDimensionName = targetLevel.getDimensionName();
        	if(this.name.equals(targetDimensionName) == false){
        		throw new IllegalArgumentException("Element dimension " + targetDimensionName +
        				" doesn't match dimension name " + name);
        	}
        	
    		targetLevelName = targetLevel.getName();
    	}
    	
    	// If the given element is null or if its level names are null, then return null.
    	if(descendantLevelName == null){
    		return null;
    	}

    	HierarchyLevel descendantLevel = null;
    	HierarchyLevel ancestorLevel = null;
    	
    	if(targetLevelName == null){
    		// If descendant level is not null, but target level is null.
    		// then set the ancestor level to the level immediately 
    		// above the descendant level. In essence, this call is degenerated
    		// into getParentElement().
    		for(int i = levels.size()-1; i >= 0; i --){
    			HierarchyLevel level = levels.get(i);
    			if(level.getName().equals(descendantLevelName)){
    				descendantLevel = level;
    				if(i > 0){
    					ancestorLevel = levels.get(i-1);
    				}
    				break;
    			}
    		}
        	
        	if(descendantLevel == null){
        		throw new IllegalArgumentException("Element level " + descendantLevelName +
        				" is not in the hierarchy " + hierarchyName);
        	}
    	}
    	else {
    		// If neither element nor the target level is null, then get the levels.
    		if(descendantLevelName.equals(targetLevelName)){
    			// If the user is asking for ancestors in the same level as the
    			// requested element, return null.
    			return null;
    		}
    		
	    	for(int i = 0, n = levels.size(); i < n; i ++){
	    		HierarchyLevel level = levels.get(i);
	    		if(level.getName().equals(targetLevelName)){
	    			ancestorLevel = level;
	    		}
	    		if(level.getName().equals(descendantLevelName)){
	    			if(ancestorLevel == null){
	    				throw new IllegalArgumentException("Target level " + targetLevelName +
	    					" is below element level " + descendantLevelName);
	    			}
	    			descendantLevel = level;
	    		}
	    	}
	    	
	    	// both levels should exist.
	    	if(descendantLevel == null){
	    		throw new IllegalArgumentException("Element level " + descendantLevelName +
	    				" is not in the hierarchy " + hierarchyName);
	    	}

	    	if(ancestorLevel == null){
	    		throw new IllegalArgumentException("Target level " + targetLevelName +
	    				" is not in the hierarchy " + hierarchyName);
	    	}
    	}
    	
    	
    	// This is true when the user gives a top level element with null target level.
    	if(ancestorLevel == null){
    		return null;
    	}
    	
    	int parentIndex = -1;
    	String parentLevelName = null;
    	for(int i = levels.size()-1; i >= 0; i --){
    		HierarchyLevel level = levels.get(i);
    		if(level.getName().equals(descendantLevelName)){
    			// at the descendant level.
    			parentLevelName = levels.get(i-1).getName();
    			IIntStore parentIndexStore = level.getIntAttribute(parentLevelName);
    			parentIndex = parentIndexStore.getElement(descendantElementIndex);
    			continue;
    		}
    		
    		if(level.getName().equals(ancestorLevel.getName())){
    			// At the targeted ancestor level. We are done.
    			// No need to go up further along the hierarchy.
    			break;
    		}
    		
    		if(parentLevelName == null){
    			// still below the descendant level, keep going up the
    			// hierarchy.
    			continue;
    		}
    		
    		// At a level between descendant and ancestor levels. Keep going up
    		// the hierarchy.
			parentLevelName = levels.get(i-1).getName();
			IIntStore parentIndexStore = level.getIntAttribute(parentLevelName);
			parentIndex = parentIndexStore.getElement(parentIndex);
    	}

    	Element ancestorElement = new Element(this.name, ancestorLevel.getName(), parentIndex);
    	return ancestorElement;
    }
    

    @Override
    public String toString() {
        return "Dimension{" +
                "name='" + name + '\'' +
                ", levels=" + levels +
                ", hierarchyMap=" + hierarchyMap +
                '}';
    }

    /**
     * Compute the mapping between two levels in a hierarchy.
     * <p>
     * tofix  overlaps Intersection.projectToLeaf() - factor and put code in proper class
     *
     * @param hierarchyName name of hierarchy that contains both levels.
     * @param upperLevel the upper level name
     * @param lowerLevel the lower level name
     * 
     * @return an array of integer indices. The length of the array equals to the number of
     * members at the lower level. For a member at index i, the value at same index in the 
     * result array is the index of the ancestor at the given upper level.
     */
    public int[] getLevelMapping( String hierarchyName, String upperLevel, String lowerLevel )  {
        int[] mappedInds = null;
        String parentLevelName = null;

        List<HierarchyLevel> hierarchyLevelList = getHierarchy(hierarchyName);
        // looping from top level to bottom level.
        for(HierarchyLevel level : hierarchyLevelList) {
			String levelName = level.getName();
			if (levelName.equals(upperLevel)) {
				mappedInds = new int[level.getMemberCount()];
				for (int i = 0; i < mappedInds.length; i++) {
					mappedInds[i] = i; // sequence
				}

				if (levelName.equals(lowerLevel)) {
					// this is for the special case when upper level and lower
					// level
					// are the same.
					break; // done
				}

				parentLevelName = level.name;
				continue;
			}

			// mappedInds is null if we haven't reached the upper level.
			if (mappedInds == null) {
				continue;
			}

			// reach here if it is a level below the upper level, but above or
			// at
			// the lower level.
			IIntStore parentIndsStore = level.getIntAttribute(parentLevelName);
			if (parentIndsStore == null) {
				// cannot map down the hierarchy because next level down does
				// not have a link to parent level
				String message = "Hierarchy " + hierarchyName + " can not be traversed because level " + levelName
						+ " does not have a link to parent level " + parentLevelName;
				throw new CdsException(message);
			}
			int[] parentInds = getIntValues(parentIndsStore);
			mappedInds = ArrayOps.index(mappedInds, parentInds);
			parentLevelName = level.name;

			if (levelName.equals(lowerLevel)) {
				break; // done
			}
		}
        return mappedInds;
    }

    /**
     * Export the values of a store as an array.  Probably should be part of IIntStore but added here for now
     * @param store
     * @return  The values of store as an int[].
     */
    private int[] getIntValues( IIntStore store ) {
        int[] values = new int[store.size()];
        for( int i=0; i<values.length; i++ )
            values[i] = store.getElement(i);
        return values;
    }


    /**
     * Add a new LevelMember to the tuple.
     * @param context
     * @param level
     * @param memberId
     * @return  New tuple containing the combination of members in context plus a new LevelMember
     * for the specified level and member.
     */
    LevelMemberTuple combineTuple( LevelMemberTuple context, HierarchyLevel level, int memberId ) {
        ArrayList<LevelMember> tupleMembers = new ArrayList<LevelMember>();
        if ( context != null ) {
            for( LevelMember member : context.getLevelMembers()) {
                if ( member.getLevel().getDimensionName().equals( level.getDimensionName()))
                    continue;
                tupleMembers.add(member);
            }
        }
        tupleMembers.add( new LevelMember(level,memberId));
        LevelMemberTuple tuple = new LevelMemberTuple(tupleMembers);
        return tuple;
    }

    /**
     * Enumerate the children of a single parent level member at a specified level in the default (first) hierarchy
     * on the dimension, and for each child indicate whether or not it has children.
     *
     * @param collector  Collector instance containing the intersections
     * @param parentLevel  Level of the member whose children are requested.
     * @param parentMemberId  Member Id (integer ordinal in parentLevel) of the member.
     * @param childLevel  The level where children are requested.  Note that this need not be the immediate
     *                    descendant of parentLevel in the hierarchy.
     * @param grandchildLevel  The level to be examined to determine if a child has children.  Note that this need
     *                         not be the child level's immediate descendant.
     * @param context  A tuple of level members on other dimensions that applies when excludeEmpty is true.
     * @param intersections  The set of intersections to examine to determine whether a child is empty
     * @param excludeEmpty  If true, child members that do not map to any intersection are excluded.  Likewise, the
     *                      hasChildren flag will be set to false if none of the grandchildren members map to an
     *                      intersection.
     * @return Array of LevelMemberInfo indicating the level and member Id and also whether or not the member
     *    has children at the grandchild level.  If excludeEmpty is true and grandchildren are empty, then
     *    hasChildren will be false.
     */
    public LevelMemberInfo[] getChildMembers( CubeDs collector,
                                              HierarchyLevel parentLevel,
                                              int parentMemberId,
                                              HierarchyLevel childLevel,
                                              HierarchyLevel grandchildLevel,
                                              LevelMemberTuple context,
                                              Collection<Intersection> intersections,
                                              boolean excludeEmpty  ) {
    	// pass null as member filters

        return getChildMembers(
        		collector, 
        		null, 
        		parentLevel, 
        		parentMemberId, 
        		childLevel, 
        		grandchildLevel, 
        		context, 
				null,
        		intersections, 
        		excludeEmpty);
    }

    /**
     * Enumerate the children of a single parent level member at a specified level in the default (first) hierarchy
     * on the dimension, and for each child indicate whether or not it has children.
     *
     * @param collector  Collector instance containing the intersections
     * @param intersectionViews Additional filters for intersections
     * @param parentLevel  Level of the member whose children are requested.
     * @param parentMemberId  Member Id (integer ordinal in parentLevel) of the member.
     * @param childLevel  The level where children are requested.  Note that this need not be the immediate
     *                    descendant of parentLevel in the hierarchy.
     * @param grandchildLevel  The level to be examined to determine if a child has children.  Note that this need
     *                         not be the child level's immediate descendant.
     * @param context  A tuple of level members on other dimensions that applies when excludeEmpty is true.
     * @param intersections  The set of intersections to examine to determine whether a child is empty
     * @param filters extra filters to be taken into consideration.
     * @param excludeEmpty  If true, child members that do not map to any intersection are excluded.  Likewise, the
     *                      hasChildren flag will be set to false if none of the grandchildren members map to an
     *                      intersection.
     * @return Array of LevelMemberInfo indicating the level and member Id and also whether or not the member
     *    has children at the grandchild level.  If excludeEmpty is true and grandchildren are empty, then
     *    hasChildren will be false.
     */
    public LevelMemberInfo[] getChildMembers( CubeDs collector,
            Map<Intersection, BitSet> intersectionViews,
            HierarchyLevel parentLevel,
            int parentMemberId,
            HierarchyLevel childLevel,
            HierarchyLevel grandchildLevel,
            LevelMemberTuple context,
            List<IFilter> levelFilters, // filter on levels in the same dimension
            Collection<Intersection> intersections,
            boolean excludeEmpty  ) {

        if ( childLevel == null ) {
        	// just in case, should never happen.
            return new LevelMemberInfo[0];
        }
        
        // Some of the extra filters could be defined on a level within this dimension, 
        // while the others could be defined on a level in the other dimensions.
        //
        // So separate the extra filters into two groups. The filers for this dimension
        // will be taken into consideration when the try to find out the child or
        // grandchild members, regardless whether excludeEmpty is requested. The filters
        // for other dimensions, however, will only be taken into consideration when
        // excludeEmpty is true.
        List<IFilter> filtersForThisDimension = new ArrayList<IFilter>();
        List<IFilter> filtersForOtherDimensions = new ArrayList<IFilter>();
        if(levelFilters != null){
        	for(IFilter levelFilter : levelFilters){
        		IAttributeContainer container = levelFilter.getAttributeContainer();
        		boolean forThisDimension = false;
        		for(Map.Entry<String, HierarchyLevel> entry : this.levels.entrySet()){
        			HierarchyLevel level = entry.getValue();
        			if(level == container){
        				forThisDimension = true;
        				break;
        			}
        		}
        		if(forThisDimension){
    				filtersForThisDimension.add(levelFilter);
        		} else {
        			filtersForOtherDimensions.add(levelFilter);
        		}
        	}
        }
        
        // if a parent member is given, then add it to the filter.
        if(parentLevel != null && parentMemberId >= 0){
        	filtersForThisDimension.add(new ElementFilter(parentLevel, parentMemberId));
        }
        
        // FIX- can't assume the first one.
        String hierarchyName = this.getHierarchyNames().get(0);
        
        // collect the children that match all the filters into an index->member map.
        HashMap<Integer, LevelMemberInfo> childMap = new HashMap<Integer, LevelMemberInfo>();
    	BitSetSelector childSelector = this.getSelector(hierarchyName, childLevel.getName(), filtersForThisDimension, true);
    	BitSet childBitset = childSelector.getBitSet();
    	for(int ci = childBitset.nextSetBit(0); ci >= 0; ci = childBitset.nextSetBit(ci+1)){
    		childMap.put(ci, new LevelMemberInfo(childLevel, ci, false)); // pass false to assume no children.
    	}
        
    	// get the grand children that match all the filters and the index of their parents
    	// at the child level.
    	if(grandchildLevel != null) {
            BitSetSelector gChildSelector = this.getSelector(hierarchyName, grandchildLevel.getName(), filtersForThisDimension, true);
            BitSet gChildBitset = gChildSelector.getBitSet(); // indices of the grand children that match the filters.
            
            // set the parents of the matched grand children to "have children".
            int[] grandchildMapping = getLevelMapping( hierarchyName, childLevel.getName(), grandchildLevel.getName());
            for(int gi = gChildBitset.nextSetBit(0); gi >= 0; gi = gChildBitset.nextSetBit(gi+1)){
            	int ci = grandchildMapping[gi];
            	if(childMap.containsKey(ci) == false){
            		childMap.put(ci, new LevelMemberInfo(childLevel, ci, true));
            	} else {
            		childMap.get(ci).hasChildren = true;
            	}
            }
        }
        
        LevelMemberInfo[] children = childMap.values().toArray(new LevelMemberInfo[childMap.size()]);

        // excludeEmpty means that we should not return any children that do not map to a item on any
        // intersection.  Similarly, the hasChildren should be false if none the grandchildren under a
        // child map to an intersection. In this process, we also need to take into consideration the 
        // extra filters that are placed on other dimensions. 
        if ( excludeEmpty ) {

            // FIX- can't assume the first hierarchy.
        	Map<String, String> dimensionHierarchyMap = new HashMap<>();
        	for(Dimension dimension : collector.getDimensions()){
        		String dimensionName = dimension.getName();
        		String hierName = dimension.getHierarchyNames().get(0);
        		dimensionHierarchyMap.put(dimensionName, hierName);
        	}
        	
        	// gather all the filters on other dimensions we need to take into consideration when deciding 
        	// if the children or grand children have a shadow on the intersections. This includes:
        	// 1. The filters on the level members in the context.
        	// 2. The extra level filters that were defined on other dimensions.
            if (context != null) {
            	for(LevelMember contextLevelMember : context.getLevelMembers()){
            		HierarchyLevel contextLevel  = contextLevelMember.getLevel();
            		int contextMemberId = contextLevelMember.getMemberId();
            		filtersForOtherDimensions.add(new ElementFilter(contextLevel, contextMemberId));
            	}
            }
        	
            // Calculate the shadow of these context filters (i.e. the filters on other dimensions) on the 
            // intersections.
            Map<String, BitVector> contextShadows = new HashMap<>();
            if(!filtersForOtherDimensions.isEmpty()){
                for( Intersection intersection: intersections ) {
                	String intersectionName = intersection.getName();

                    ShadowAggregator aggr = new ShadowAggregator(intersection.size());
                    Aggregator[] aggregators = new Aggregator[1];
                    aggregators[0] = aggr;
                    
                    IFilter[] filters = filtersForOtherDimensions.toArray(new IFilter[filtersForOtherDimensions.size()]);
                    collector.aggregate(dimensionHierarchyMap, intersectionName, aggregators, filters);
                    
                	BitVector shadow = aggr.getShadow();
                    if(contextShadows.containsKey(intersectionName)){
                    	contextShadows.get(intersectionName).and(shadow);
                    } else {
                    	contextShadows.put(intersectionName, shadow);
                    }
                }
            }

            // Get the depth of the child level and grandchild level in the hierarchy.
            List<HierarchyLevel> hierarchy = this.getHierarchy(hierarchyName);
            int childDepth =  hierarchy.indexOf(childLevel);
            int grandChildDepth = grandchildLevel==null ? 999999 : hierarchy.indexOf(grandchildLevel); // 999999 = effectively infinity for hierarchy depth
            
            // Get the depth of the related level for this dimension in all the intersections.
            HashMap<String,Integer> intersectionDepths = new HashMap<String, Integer>();
            for( Intersection intersection : intersections ) {
                HierarchyLevel intersectionLevel = intersection.getRelatedLevel( this.getName());
                int intersectionDepth = hierarchy.indexOf(intersectionLevel);
                intersectionDepths.put( intersection.getName(), intersectionDepth );
            }

            // Find out if intersections are at the same level or deeper
            boolean[] nonEmpty = new boolean[children.length];
            boolean[] nonEmptyGrandchildren = new boolean[children.length];
            for( int i = 0; i < children.length; i ++ ) {
                // if true, then this member appears on at least one intersection where child is a related level
                boolean childPresentOnIntersection = false;
                // if true, then there are descendants of this child on at least one intersection that may aggregate to child level
                boolean hasDeeperShadow = false;
                // if true, then there descendants at or below grandchild level
                boolean hasNonEmptyGrandchildren = false;

                for( Intersection intersection : intersections ) {
                    BitVector contextShadow  = contextShadows.get(intersection.getName());
                    if(contextShadow == null) {
                        // FIX: this can be optimized away; no need to materialize this bit vector
                        contextShadow = new BitVector( intersection.size());
                        contextShadow.setAll(true);
                    }
                    
                 // apply intersection views if defined.
                    if (intersectionViews != null) {
                        BitSet bitSet = intersectionViews.get(intersection);
                        if (bitSet != null) {
                            contextShadow.getBitSet().and(bitSet);
                        }
                    }

                    BitVector childShadow = intersection.getTupleShadow( collector, makeTuple( children[i]));

                    boolean intersects = contextShadow.intersects(childShadow);

                    if ( !intersects ) {
                    	// If the shadow casted by the child has nothing in common with the shadow
                    	// casted by the context, then together, they don't have any matching entries
                    	// in the intersection.
                        continue;
                    }

                    // reach here if there are entries in the intersection that are formed by the 
                    // descendants of the child and the descendants of the members in the context.
                    int intersectionDepth = intersectionDepths.get( intersection.getName());
                    childPresentOnIntersection = childPresentOnIntersection || intersectionDepth == childDepth;
                    hasDeeperShadow = hasDeeperShadow ||  intersectionDepth > childDepth;
                    hasNonEmptyGrandchildren = hasNonEmptyGrandchildren || intersectionDepth >= grandChildDepth;

                }
                
                // For this child, record whether we have found its descendants in any of the intersections,
                // taking into consideration of the context.
                nonEmpty[i] = childPresentOnIntersection || hasDeeperShadow;
                // For this child, record whether we have found in any of the intersections its descendants
                // that are lower in hierarchy, taking into consideration of the context.
                nonEmptyGrandchildren[i] = hasNonEmptyGrandchildren;
            }

            List<LevelMemberInfo> subset = new ArrayList<LevelMemberInfo>( children.length );
            for( int i=0; i<children.length; i++ ) {
                if ( nonEmpty[i] ) {
                    subset.add( children[i] );
                }
                if ( !nonEmptyGrandchildren[i] ) {
                    children[i].hasChildren = false;
                }
            }
            children = subset.toArray( new LevelMemberInfo[ subset.size() ] );
        }

        return children;
    }

//    /**
//     * Returns all members at a child level that are descended from a single ancestor member in a hierarchy
//     * @param hierarchyName Name of the hierarchy
//     * @param parentName Name of the parent member's level
//     * @param parentMemberId Member Id of the parent member
//     * @param childName Name of the child level
//     * @return Array of all members in the child level, without regard to whether they are "empty" at intersections.
//     */
//    private LevelMemberInfo[] getChildMembers( String hierarchyName, String parentLevelName, int parentMemberId, String childLevelName ) {
//        HierarchyLevel childLevel = getLevel(childLevelName);
//        int[] mapping = this.getLevelMapping( hierarchyName, parentLevelName, childLevelName );
//        ArrayList<LevelMemberInfo> childList = new ArrayList<LevelMemberInfo>( childLevel.getMemberCount());
//        for( int i=0; i<mapping.length; i++ ){
//            if ( mapping[i] == parentMemberId )
//                childList.add( new LevelMemberInfo( childLevel, i, false ));
//        }
//        return childList.toArray( new LevelMemberInfo[childList.size()]);
//    }

    // Create a member tuple with only one level member
    private LevelMemberTuple makeTuple( LevelMemberInfo child ) {
        LevelMember member = new LevelMember(child.getLevel(), child.getMemberId());
        ArrayList<LevelMember> members = new ArrayList<LevelMember>();
        members.add(member);
        LevelMemberTuple childTuple = new LevelMemberTuple(members);
        return childTuple;
    }

    /**
     * Return the lowest level in the default hierarchy (the one with the same name as the dimension)
     * @return The leaf level
     */
    public HierarchyLevel getLeafLevel() {
        List<HierarchyLevel> levels = getHierarchy( getName());
        return levels.get( levels.size()-1);

    }

}


//