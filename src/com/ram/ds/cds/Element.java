package com.ram.ds.cds;

import java.io.Serializable;

/**
 * Identifies a single member in a HierarchyLevel by name and index.
 */
public class Element implements Serializable
{
    private static final long serialVersionUID = 4576335746829693979L;
    private String dimensionName=null;
	private String levelName=null;
	private int elementIndex=-1;

	public Element(String dimensionName, String levelName, int elementIndex){
		this.dimensionName = dimensionName;
		this.levelName = levelName;
		this.elementIndex = elementIndex;
	}

    //for serialization
    protected Element() {
    }

    public String getDimensionName() {
		return dimensionName;
	}

	public String getLevelName() {
		return levelName;
	}

	public int getElementIndex() {
		return elementIndex;
	}
	
	@Override
	public int hashCode(){
		return this.dimensionName.hashCode() + 
				this.levelName.hashCode() + 
				this.elementIndex;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj == null || (obj instanceof Element) == false){
			return false;
		}
		
		Element other = (Element)obj;
		return ((this.elementIndex == other.elementIndex) &&
				(this.levelName.equals(other.levelName)) &&
				(this.dimensionName.equals(other.dimensionName)));
	}
	
	@Override
	public String toString(){
        return "Element {" +
                "dim='" + dimensionName + '\'' +
                ", lvl=" + levelName +
                ", idx=" + elementIndex +
                '}';
	}
}




