package com.ram.ds.cds;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ram.ds.cds.aggregator.Aggregator;
import com.ram.ds.cds.filters.Filter;
import com.ram.ds.cds.filters.IFilter;
import com.ram.ds.cds.stores.BitSetStore;
import com.ram.ds.cds.stores.BooleanArrayStore;
import com.ram.ds.cds.stores.BooleanStore;
import com.ram.ds.cds.stores.DataDomainAttrStore;
import com.ram.ds.cds.stores.DoubleArrayStore;
import com.ram.ds.cds.stores.DoubleSparseArrayStore;
import com.ram.ds.cds.stores.DoubleStore;
import com.ram.ds.cds.stores.FixedLengthIntStore;
import com.ram.ds.cds.stores.FixedLengthStringLookupStore;
import com.ram.ds.cds.stores.FloatArrayStore;
import com.ram.ds.cds.stores.FloatStore;
import com.ram.ds.cds.stores.IBitSetStore;
import com.ram.ds.cds.stores.IBooleanArrayStore;
import com.ram.ds.cds.stores.IBooleanStore;
import com.ram.ds.cds.stores.IDataDomainStore;
import com.ram.ds.cds.stores.IDataStore;
import com.ram.ds.cds.stores.IDoubleArrayStore;
import com.ram.ds.cds.stores.IDoubleStore;
import com.ram.ds.cds.stores.IFloatArrayStore;
import com.ram.ds.cds.stores.IFloatStore;
import com.ram.ds.cds.stores.IIntArrayStore;
import com.ram.ds.cds.stores.IIntStore;
import com.ram.ds.cds.stores.ILongLookupStore;
import com.ram.ds.cds.stores.ILongStore;
import com.ram.ds.cds.stores.IStringArrayStore;
import com.ram.ds.cds.stores.IStringLookupStore;
import com.ram.ds.cds.stores.IStringStore;
import com.ram.ds.cds.stores.IntArrayStore;
import com.ram.ds.cds.stores.IntStore;
import com.ram.ds.cds.stores.LongLookupStore;
import com.ram.ds.cds.stores.LongStore;
import com.ram.ds.cds.stores.StringArrayStore;
import com.ram.ds.cds.stores.StringLookupStore;
import com.ram.ds.cds.stores.StringStore;


/**
 * Base container for storing a collection of attributes.  Each attribute is represented by a store of
 * data values for the attribute. Stores are identified by name, and can be dynamically add to and removed
 * from the container.
 * <p>Each attribute in the store is expected to have the same number of data values, although this is not
 * enforced so that data can be added in arbitrary order during data loading.</p>
 *
 *
 */
public class AttributeContainer implements IAttributeContainer, Serializable {

    private static final long serialVersionUID = 5736366203222399964L;
    
    protected String name;

    /**
     * HashMap storing the attribute values.  Each attribute value stores an array of
     * the type of attribute that is the size of the number of members in this level.
     * For example, if this is the class level and there are 20 classes and 3 attributes
     * (color, onhand, and effdate), there will be 3 arrays stored, each of size 20.
     * The type of the first array will be int (since we will identify each color by an ID),
     * second array will be an int array (since onhand is an integer) and the third will be
     * an array of longs (minutes since 1970, for example).  Ideally, the attributes will
     * typically will be stored as int ID's, rather than values to keep things as small as
     * possible.
     */
    private Map<String, IDataStore> attrNameToAttrStorage = new HashMap<String, IDataStore>(40);

    public AttributeContainer(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets a bit set for the parents that will be enabled after applying the given filters to this level.
     *
     * @param iFilters  the filters to apply to THIS level
     * @param parentAttrName the name of the attribute on this level which refers to the parent
     * @return   a BitSetSelector of the length of the specified parent.
     */
    public BitSet getParentSelector(List<IFilter> iFilters,
                                            String parentAttrName) {
        ParentAggregator parentAggregator = new ParentAggregator(parentAttrName);
        aggregate(iFilters, new Aggregator[]{parentAggregator});
        return parentAggregator.getParentBits();
    }

    /**
     * Aggregates parent member ids into a BitSet.
     */
    public class ParentAggregator implements Aggregator
    {
        IIntStore parentIndexStore;
        BitSet parentBits = new BitSet();
        
        public ParentAggregator(String parentAttrName)
        {
        	parentIndexStore = getIntAttribute(parentAttrName);
        }

        @Override
        public void accumulate(int i)
        {
            parentBits.set(parentIndexStore.getElement(i));
        }

        public BitSet getParentBits()
        {
            return parentBits;
        }

        @Override
        public String toString() {
            return "ParentAggregator{" +
                    "parentIndexStore=" + parentIndexStore +
                    ", parentBits=" + parentBits +
                    '}';
        }
    }

    /**
     * Gets the selector for all of the members that fits all the 
     * filters in the list.
     *
     * @param iFilters   the filters to apply
     * @return BitSetSelector for the combination of the filters.
     */
    public BitSetSelector getSelector(List<IFilter> iFilters) {
        BitSetSelector selector = new BitSetSelector(name);
        aggregate(iFilters, new Aggregator []{ selector });
        return selector;
    }


    /**
     * Remove the store for the attribute with the given name.
     * 
     * @param iAttrName
     * @return The removed store, or null if the attribute is not in this container.
     */
    public IDataStore removeAttributeStore(String iAttrName){
    	IDataStore store = this.getAttribute(iAttrName);
    	this.attrNameToAttrStorage.remove(iAttrName);
    	return store;
    }
    
    /**
     * Add the given store to this attribute container.  This method allows the caller to build their own store in
     * case one of the factory methods provided doesn't provide the right kind of store.  In particular, this was added to
     * provide the ability to create fixed-size stores in order to determine how much we could improve performance using
     * single-dimension arrays rather than double-dimension arrays used by the GenericStore and it's derived classes.
     * @param iAttrName the name of the attribute loaded
     * @param iNewStore the store to add
     */
    public void addAttributeStore(String iAttrName, IDataStore iNewStore) {
        putStoreIntoMap(iAttrName, iNewStore);
    }
    
    @Override
	public IDataStore getAttributeStore(String attrName) {
    	return this.attrNameToAttrStorage.get(attrName);
	}

    // ---------------------- Add and get BitSetStore  ---------------------------

    public IBitSetStore addBitSetAttribute(String attrName) {
    	BitSetStore attrStore = new BitSetStore();
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }
    
	@Override
	public IBitSetStore getBitSetAttribute(String attrName) {
		return (IBitSetStore)this.attrNameToAttrStorage.get(attrName);
	}

    // ---------------------- Add and get BooleanStore  --------------------------

    public IBooleanStore addBooleanAttribute(String attrName) {
    	BooleanStore attrStore = new BooleanStore();
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }
    
	@Override
	public IBooleanStore getBooleanAttribute(String attrName) {
		return (IBooleanStore)this.attrNameToAttrStorage.get(attrName);
	}

    // ---------------------- Add and get BooleanArrayStore  ----------------------

    public IBooleanArrayStore addBooleanAttribute(String attrName, int secondDimCapacity) {
    	BooleanArrayStore attrStore = new BooleanArrayStore(0, secondDimCapacity);
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }
    
	@Override
	public IBooleanArrayStore getBooleanArrayAttribute(String attrName) {
		return (IBooleanArrayStore)this.attrNameToAttrStorage.get(attrName);
	}

    // ---------------------- Add and get FloatStore  ---------------------------

    public IFloatStore addFloatAttribute(String attrName, int chunkSize) {
        FloatStore attrStore = new FloatStore(0, chunkSize);
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }

	@Override
	public IFloatStore getFloatAttribute(String attrName) {
		return (IFloatStore)this.attrNameToAttrStorage.get(attrName);
	}

    // ---------------------- Add and get IntStore  ---------------------------

    public IIntStore addIntAttribute(String attrName) {
        IIntStore intStoreForAttr = new IntStore();
        putStoreIntoMap(attrName, intStoreForAttr);
        return intStoreForAttr;
    }

    public IntStore addIntAttribute(String attrName, int chunkSize) {
        IntStore attrStore = new IntStore(0, chunkSize);
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }
    
	@Override
    public IIntStore getIntAttribute(String attrName) {
        return (IIntStore) attrNameToAttrStorage.get(attrName);
    }

    
    public IIntStore addFixedLengthIntAttribute(String attrName, int maximumSize) {
        IIntStore intStoreForAttr = new FixedLengthIntStore(maximumSize);
        putStoreIntoMap(attrName, intStoreForAttr);
        return intStoreForAttr;
    }


    public IStringLookupStore addFixedLengthStringLookupAttribute(String attrName, int maximumSize) {
        IStringLookupStore stringLookupStoreForAttr = new FixedLengthStringLookupStore(maximumSize);
        attrNameToAttrStorage.put(attrName, stringLookupStoreForAttr);
        return stringLookupStoreForAttr;
    }

    // ---------------------- Add and get IntArrayStore  ---------------------------

    public IIntArrayStore addIntArrayAttribute(String attrName, int secondDimCapacity) {
        IntArrayStore attrStore = new IntArrayStore(0, secondDimCapacity);
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }
    
    @Override
    public IIntArrayStore getIntArrayAttribute(String attrName) {
        return (IntArrayStore) attrNameToAttrStorage.get(attrName);
    }

    // ---------------------- Add and get StringStore  ----------------------------
    /**
     * This method should be used sparingly.  The ONLY time it should be used is when the attribute is one-to-one
     * with a member in the dimension (examples:  productname on the product level, description on a category)
     *
     *
     * @param iAttrName the attribute name
     * @return The newly created StringStore.
     */
    public IStringStore addStringAttribute(String iAttrName) {
        IStringStore stringStoreForAttr = new StringStore();
        putStoreIntoMap(iAttrName, stringStoreForAttr);
        return stringStoreForAttr;
    }

    public StringStore addStringAttribute(String attrName, int chunkSize) {
        StringStore attrStore = new StringStore(0, chunkSize);
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }

    @Override
    public IStringStore getStringAttribute(String attrName) {
        return (IStringStore) attrNameToAttrStorage.get(attrName);
    }

    // ---------------------- Add and get StringArrayStore  -----------------------
    
    public IStringArrayStore addStringArrayAttribute(String attrName, int secondDimCapacity) {
        StringArrayStore attrStore = new StringArrayStore(0, secondDimCapacity);
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }

    @Override
    public IStringArrayStore getStringArrayAttribute(String attrName) {
        return (IStringArrayStore) attrNameToAttrStorage.get(attrName);
    }

    // ---------------------- Add and get FloatArrayStore  -----------------------
    
    public IFloatArrayStore addFloatArrayAttribute(String attrName, int bucketCount)
    {
        IFloatArrayStore floatArrayStore = new FloatArrayStore(bucketCount);
        putStoreIntoMap(attrName, floatArrayStore);
        return floatArrayStore;
    }
    
    @Override
    public IFloatArrayStore getFloatArrayAttribute(String attrName) {
        return (IFloatArrayStore) attrNameToAttrStorage.get(attrName);
    }

    // ---------------------- Add and get DoubleStore  ----------------------------
    
    public IDoubleStore addDoubleAttribute(String attrName) {
        IDoubleStore doubleStoreForAttr = new DoubleStore();
        attrNameToAttrStorage.put(attrName, doubleStoreForAttr);
        return doubleStoreForAttr;
    }

    public IDoubleStore addDoubleAttribute(String attrName, int chunkSize) {
        DoubleStore attrStore = new DoubleStore(0, chunkSize);
        attrNameToAttrStorage.put(attrName, attrStore);
        return attrStore;
    }

    @Override
    public IDoubleStore getDoubleAttribute(String attrName) {
        return (IDoubleStore) attrNameToAttrStorage.get(attrName);
    }

    // ---------------------- Add and get DoubleArrayStore  ----------------------------
    
    /*
     * creates a double dimension array.  An example of when you would want to use this is when you are storing a fixed
     * number of forecast periods or fixed number of prices.  This can be more space-efficient than creating a new
     * dimension for the time dimension if the time dimension is not sparse.  For sparse data on the time dimension,
     * you should probably create a dimension
     * @param attrName
     * @return
     *
     */
    public IDoubleArrayStore addDoubleArrayAttribute(String attrName, int secondDimCapacity) {
        DoubleArrayStore attrStore = new DoubleArrayStore(0, secondDimCapacity);
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }


    public IDoubleArrayStore addSparseDoubleArrayAttribute(String attrName, int secondDimCapacity, int defaultTimeSeriesLength, double missingValue) {
        DoubleSparseArrayStore attrStore = new DoubleSparseArrayStore(0, secondDimCapacity, defaultTimeSeriesLength, missingValue );
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }

    @Override
    public IDoubleArrayStore getDoubleArrayAttribute(String attrName) {
        return (IDoubleArrayStore) attrNameToAttrStorage.get(attrName);
    }

    // ---------------------- Add and get DataDomainAttrStore  ----------------------

    public IDataDomainStore addDataDomainAttribute(String attrName) {
        IDataDomainStore datadomainStoreForAttr = new DataDomainAttrStore();
        attrNameToAttrStorage.put(attrName, datadomainStoreForAttr);
        return datadomainStoreForAttr;
    }

    @Override
    public IDataDomainStore getDataDomainAttribute(String attrName) {
        return (IDataDomainStore) attrNameToAttrStorage.get(attrName);
    }

    // ---------------------- Add and get StringLookupStore  ------------------------

    public IStringLookupStore addStringLookupAttribute(String attrName) {
        IStringLookupStore stringLookupStoreForAttr = new StringLookupStore();
        putStoreIntoMap(attrName, stringLookupStoreForAttr);
        return stringLookupStoreForAttr;
    }

    @Override
    public IStringLookupStore getStringLookupAttribute(String attrName) {
        return (IStringLookupStore) attrNameToAttrStorage.get(attrName);
    }

    // ----------------- Add and get LongLookupStore -------------------------------
    
    public ILongLookupStore addLongLookupAttribute(String attrName) {
        ILongLookupStore longLookupStoreForAttr = new LongLookupStore();
        attrNameToAttrStorage.put(attrName, longLookupStoreForAttr);
        return longLookupStoreForAttr;
    }

    @Override
    public ILongLookupStore getLongLookupAttribute(String attrName) {
        return (ILongLookupStore) attrNameToAttrStorage.get(attrName);
    }

    // ---------------------- Add and get LongStore  -------------------------------
    
    public LongStore addLongAttribute(String attrName, int chunkSize) {
        LongStore attrStore = new LongStore(0, chunkSize);
        putStoreIntoMap(attrName, attrStore);
        return attrStore;
    }

    @Override
    public ILongStore getLongAttribute(String attrName) {
        return (ILongStore) attrNameToAttrStorage.get(attrName);
    }


    private void putStoreIntoMap(String attrName, IDataStore attrStore)
    {
        attrNameToAttrStorage.put(attrName, attrStore);
    }

    
    @Override
    public Map<String, StoreType> getAttributes() {
        Map<String, StoreType> retVal = new HashMap<String, StoreType>(attrNameToAttrStorage.size());
        for (Map.Entry<String, IDataStore> entry : attrNameToAttrStorage.entrySet()) {
            String storeName = entry.getKey();
            StoreType type = null;
            IDataStore storeClass = entry.getValue();
            if (storeClass instanceof IBitSetStore) {
                type = StoreType.IBitSetStore;
            } else if (storeClass instanceof IDataDomainStore) {
                type = StoreType.IDataDomainStore;
            } else if (storeClass instanceof IBooleanStore) {
                type = StoreType.IBooleanStore;
            } else if (storeClass instanceof IBooleanArrayStore) {
                type = StoreType.IBooleanArrayStore;
            } else if (storeClass instanceof IDoubleStore) {
                type = StoreType.IDoubleStore;
            } else if (storeClass instanceof IDoubleArrayStore) {
                type = StoreType.IDoubleArrayStore;
            } else if (storeClass instanceof IFloatStore) {
                type = StoreType.IFloatStore;
            } else if (storeClass instanceof IFloatArrayStore) {
                type = StoreType.IFloatArrayStore;
            } else if (storeClass instanceof IIntStore) {
                type = StoreType.IIntStore;
            } else if (storeClass instanceof IIntArrayStore) {
                type = StoreType.IIntArrayStore;
            } else if (storeClass instanceof ILongStore) {
                type = StoreType.ILongStore;
            } else if (storeClass instanceof ILongLookupStore) {
                type = StoreType.ILongLookupStore;
            } else if (storeClass instanceof IStringLookupStore) {
                type = StoreType.IStringLookupStore;
            } else if (storeClass instanceof IStringStore) {
                type = StoreType.IStringStore;
            } else if (storeClass instanceof IStringArrayStore) {
                type = StoreType.IStringArrayStore;
            } else {
                throw new RuntimeException("Unknown store type: " + storeClass.getClass().getName());
            }
           
            retVal.put(storeName, type);
        }

        return retVal;
    }

    @Override
    public Filter getDataDomainSelector(String attrName, String[] values) {
//        IDataDomainStore store = (IDataDomainStore) attrNameToAttrStorage.get(attrName);
//        DataDomainFilter selector = new DataDomainFilter(this, values.length);
//        for (String value : values) {
//            selector.setSelectedValue(store.getKeyForValidValue(value)); // need to add this method and then add the integer to the selector
//        }
        return null;
    }

    /**
     * Returns the number of elements in the store with the largest size.
     * 
     * @return the number of members or 0 if no members have been added.
     */
    public int getMemberCount()
    {
    	int maxSize = 0;
    	
        Iterator<IDataStore> iterator = attrNameToAttrStorage.values().iterator();
        while (iterator.hasNext()) {
            maxSize = Math.max(maxSize, iterator.next().size());
        }

        return maxSize;
    }


    /**
     * Pad every data store in this container with missing values to a minimum size.  This is typically used
     * to deal with the case where the some stores are of uneven size because database fields were null in
     * the trailing rows retrieved from database.
     * @param minimumSize
     */
    public void ensureSize(int minimumSize)
    {
    	for(IDataStore dataStore : attrNameToAttrStorage.values()){
    		dataStore.ensureSize(minimumSize);
    	}
    }

    /**
     * Iterate over the items in this container and apply each filter to each item.  If an item at position i
     * matches all filters, call accumulate(i) on each of the aggregators.
     * <p>
     * <b>Note that the filters must be defined for this container.</b>
     * 
     * @param iFilters  List of filters to apply. The filters must be defined for this container. 
     * 					The list can be null, in which case every item in the container is accumulated.
     * @param aggregators  Array of aggregators to accumulate aggregated result(s). Cannot be null.
     */
    public void aggregate(List<IFilter> iFilters, Aggregator[] aggregators) {
        int memberCount = getMemberCount();
        if (iFilters != null) {
            IFilter[] filters = new IFilter[iFilters.size()];
            filters = iFilters.toArray(filters);
            for (int index = 0; index < memberCount; index++) {
                boolean match = true;
                for (IFilter filter : filters) {
                    if (!filter.isMatch(index)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    for (Aggregator aggregator : aggregators) {
                        aggregator.accumulate(index);
                    }
                }
            }
        }
        else {
            for (int index = 0; index < memberCount; index++) {
                for (Aggregator aggregator : aggregators) {
                    aggregator.accumulate(index);
                }
            }
        }
    }

    /**
     * Perform aggregation over all the items in this container without filtering.
     * 
     * @param aggregators array of aggregators, cannot be null.
     */
    public void aggregate(Aggregator[] aggregators) {
        int numberOfMembers = this.getMemberCount();
        for (int index = 0; index < numberOfMembers; index++) {
            for (Aggregator aggregator : aggregators) {
                aggregator.accumulate(index);
            }
        }
    }

    @Override
    public String toString() {
        return "AttributeContainer{" +
                "name='" + name + '\'' +
                ", attrNameToAttrStorage=" + attrNameToAttrStorage +
                '}';
    }


    /**
     * Get an attribute of any type
     * @param attrName
     * @return The store associated with attrName.
     */
    public IDataStore getAttribute( String attrName ) {
        return attrNameToAttrStorage.get( attrName );
    }
}

