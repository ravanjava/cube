package com.ram.ds.cds;

import java.io.Serializable;
import java.util.Map;

import com.ram.ds.cds.filters.Filter;
import com.ram.ds.cds.stores.*;

/**
 * Interface for attribute containers.  Includes enum StoreType for data store interface types.
 */
public interface IAttributeContainer extends Serializable {

    enum StoreType {
    	IBitSetStore, 
    	IDataDomainStore,
        IBooleanStore,
        IBooleanArrayStore,
        IDoubleStore, 
        IDoubleArrayStore, 
        IFloatStore,
        IFloatArrayStore,
        IIntStore,
        IIntArrayStore,
        ILongStore,
        ILongLookupStore,
        IStringLookupStore, 
        IStringStore,
        IStringArrayStore}

    String getName();

    Map<String, StoreType> getAttributes();
    
    IDataStore getAttributeStore(String attrName);
    
    IBitSetStore getBitSetAttribute(String attrName);

    IDataDomainStore getDataDomainAttribute(String attrName);
    
    IBooleanStore getBooleanAttribute(String attrName);
    
    IBooleanArrayStore getBooleanArrayAttribute(String attrName);

    IDoubleStore getDoubleAttribute(String attrName);

    IDoubleArrayStore getDoubleArrayAttribute(String attrName);

    IFloatStore getFloatAttribute(String attrName);
  
    IFloatArrayStore getFloatArrayAttribute(String attrName);

    IIntStore getIntAttribute(String attrName);

    IIntArrayStore getIntArrayAttribute(String attrName);

	ILongStore getLongAttribute(String attrName);

    ILongLookupStore getLongLookupAttribute(String attrName);

    IStringLookupStore getStringLookupAttribute(String attrName);

    IStringStore getStringAttribute(String attrName);
    
    IStringArrayStore getStringArrayAttribute(String attrName);
    
    Filter getDataDomainSelector(String attrName, String[] values);
}
//