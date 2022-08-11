package com.gap.pem.cds.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gap.pem.cds.CubeDs;
import com.gap.pem.cds.Dimension;
import com.gap.pem.cds.HierarchyLevel;
import com.gap.pem.cds.IAttributeContainer;
import com.gap.pem.cds.Intersection;
import com.gap.pem.cds.stores.IBitSetStore;
import com.gap.pem.cds.stores.IBooleanArrayStore;
import com.gap.pem.cds.stores.IBooleanStore;
import com.gap.pem.cds.stores.IDataDomainStore;
import com.gap.pem.cds.stores.IDataStore;
import com.gap.pem.cds.stores.IDoubleArrayStore;
import com.gap.pem.cds.stores.IDoubleStore;
import com.gap.pem.cds.stores.IFloatArrayStore;
import com.gap.pem.cds.stores.IFloatStore;
import com.gap.pem.cds.stores.IIntStore;
import com.gap.pem.cds.stores.ILongLookupStore;
import com.gap.pem.cds.stores.ILongStore;
import com.gap.pem.cds.stores.IStringArrayStore;
import com.gap.pem.cds.stores.IStringLookupStore;
import com.gap.pem.cds.stores.IStringStore;

public class CubeDebugger {

	public static String debugIntersections(CubeDs coll, int verboseLevel) {
		StringBuilder sb = new StringBuilder(100000);
		sb.append("\n\n<collector>\n");
		Collection<Intersection> intersections = coll.getIntersections();
		sb.append(indent(1)).append("<intersections>\n");
		for (Intersection intersection : intersections) {
			sb.append(outputIntersection(intersection, verboseLevel, 2));
		}
		sb.append(indent(1)).append("</intersections>\n");
		sb.append("</collector>\n\n");

		return sb.toString();
	}

	public static String debug(CubeDs coll, int verboseLevel) {
		List<Dimension> dims = coll.getDimensions();

		StringBuilder sb = new StringBuilder(100000);
		sb.append("\n\n<collector>\n");
		sb.append(indent(1)).append("<dimensions>\n");
		for (Dimension dim : dims) {
			sb.append(outputDimension(dim, verboseLevel, 2));
		}
		sb.append(indent(1)).append("</dimensions>\n");

		Collection<Intersection> intersections = coll.getIntersections();
		sb.append(indent(1)).append("<intersections>\n");
		for (Intersection intersection : intersections) {
			sb.append(outputIntersection(intersection, verboseLevel, 2));
		}
		sb.append(indent(1)).append("</intersections>\n");
		sb.append("</collector>\n\n");

		return sb.toString();
	}

	private static String outputIntersection(Intersection intersection,
			int verboseLevel, int indent) {
		StringBuilder output = new StringBuilder(10000);
		String name = intersection.getName();
		output.append(indent(indent)).append("<intersection name='")
				.append(name).append("' size='").append(intersection.size())
				.append("'>\n");

		indent++;
		Collection<HierarchyLevel> relatedLevels = intersection
				.getRelatedLevels();
		for (HierarchyLevel level : relatedLevels) {
			String levelName = level.getName();
			String dimName = level.getDimensionName();
			output.append(indent(indent)).append("<level name='")
					.append(levelName).append("' dim='").append(dimName)
					.append("'/>\n");
		}
		indent--;

		indent++;
		List<IDataStore> dataStores = new ArrayList<IDataStore>();
		List<IAttributeContainer.StoreType> types = new ArrayList<IAttributeContainer.StoreType>();

		Map<String, IAttributeContainer.StoreType> attribs = intersection
				.getAttributes();
		for (Map.Entry<String, IAttributeContainer.StoreType> attribute : attribs
				.entrySet()) {
			String attribName = attribute.getKey();
			IAttributeContainer.StoreType type = attribute.getValue();
			IDataStore store = intersection.getAttribute(attribName);
			output.append(indent(indent)).append("<attribute name='")
					.append(attribName).append("' size='").append(store.size())
					.append("' type='").append(type).append("'/>\n");

			dataStores.add(store);
			types.add(type);
		}

		for (int i = 0; i < intersection.size() && i < verboseLevel; i++) {
			output.append(printStoreMembersAt(dataStores, types, i, indent));
			output.append("\n");
		}
		indent--;
		output.append(indent(indent)).append("</intersection>\n");

		return output.toString();
	}

	private static String outputDimension(Dimension dim, int verboseLevel,
			int indent) {
		StringBuilder output = new StringBuilder(10000);
		output.append(indent(indent)).append("<dimension name='")
				.append(dim.getName()).append("'>\n");

		indent++;

		List<String> hierNames = dim.getHierarchyNames();
		for (String hierName : hierNames) {
			output.append(indent(indent)).append("<hierarchy name='")
					.append(hierName).append("'>\n");
			List<HierarchyLevel> levels = dim.getHierarchy(hierName);

			indent++;

			for (HierarchyLevel level : levels) {
				output.append(indent(indent)).append("<level name='")
						.append(level.getName());
				output.append("' idAttrName='").append(
						level.getIdentityAttributeName());
				output.append("' memberCount='").append(level.getMemberCount())
						.append("'>\n");

				List<IDataStore> dataStores = new ArrayList<IDataStore>();
				List<IAttributeContainer.StoreType> types = new ArrayList<IAttributeContainer.StoreType>();

				Map<String, IAttributeContainer.StoreType> attribs = level
						.getAttributes();
				for (Map.Entry<String, IAttributeContainer.StoreType> attribute : attribs
						.entrySet()) {
					String attribName = attribute.getKey();
					IAttributeContainer.StoreType type = attribute.getValue();
					IDataStore store = level.getAttribute(attribName);
					output.append(indent(indent)).append("<attribute name='")
							.append(attribName).append("' type='").append(type)
							.append("' size='").append(store.size())
							.append("'/>\n");

					dataStores.add(store);
					types.add(type);
				}

				indent++;
				for (int i = 0; i < level.getMemberCount() && i < verboseLevel; i++) {
					output.append(printStoreMembersAt(dataStores, types, i,
							indent));
					output.append("\n");
				}
				indent--;
				output.append(indent(indent)).append("</level>\n");
			}
			indent--;
			output.append(indent(indent)).append("</hierarchy>\n");
		}
		indent--;
		output.append(indent(indent)).append("</dimension>\n");

		return output.toString();
	}

	private static String printStoreMembersAt(List<IDataStore> dataStores,
			List<IAttributeContainer.StoreType> types, int index, int indent) {
		if (dataStores == null || dataStores.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder(indent(indent));
		sb.append("[").append(index).append("]=");
		for (int i = 0; i < dataStores.size(); i++) {
			IDataStore dataStore = dataStores.get(i);
			IAttributeContainer.StoreType type = types.get(i);
			switch (type) {
			case IBitSetStore:
				IBitSetStore bitsetStore = (IBitSetStore) dataStore;
				sb.append(bitsetStore.getElement(index));
				break;
			case IDataDomainStore:
				IDataDomainStore ddStore = (IDataDomainStore) dataStore;
				sb.append(ddStore.getValue(index));
				break;
			case IBooleanStore:
				IBooleanStore boolStore = (IBooleanStore) dataStore;
				sb.append(boolStore.getElement(index));
				break;
			case IBooleanArrayStore:
				IBooleanArrayStore boolArrStore = (IBooleanArrayStore) dataStore;
				sb.append(Arrays.toString(boolArrStore.getElement(index)));
				break;
			case IDoubleStore:
				IDoubleStore doubleStore = (IDoubleStore) dataStore;
				sb.append(String.valueOf(doubleStore.getElement(index)));
				break;
			case IDoubleArrayStore:
				IDoubleArrayStore doubleArrStore = (IDoubleArrayStore) dataStore;
				//System.out.println(doubleArrStore.size());
				try {
					double[] actual = doubleArrStore.getElement(index);
					String[] tmp = new String[actual.length];
					for (int j=0;j<actual.length;j++) {
						tmp[j]=new DecimalFormat("0").format(actual[j]);
					}
					//sb.append(Arrays.toString(doubleArrStore.getElement(index)));
					sb.append(Arrays.toString(tmp));
				} catch (Exception e1) {
					// FIXME handle this exception if needed
					e1.printStackTrace();
				}
				break;
			case IFloatStore:
				IFloatStore floatStore = (IFloatStore) dataStore;
				sb.append(floatStore.getElement(index));
				break;
			case IFloatArrayStore:
				IFloatArrayStore fltArrayStore = (IFloatArrayStore) dataStore;
				sb.append(Arrays.toString(fltArrayStore.getElement(index)));
				break;
			case IIntStore:
				IIntStore intStore = (IIntStore) dataStore;
				try {
					sb.append(intStore.getElement(index));
				} catch (Exception e) {
					sb.append("_NV_");
				}
				break;
			case ILongStore:
				ILongStore longStore = (ILongStore) dataStore;
				try {
					sb.append(longStore.getElement(index));
				} catch (Exception e) {
					sb.append("_NV_");
				}
				break;
			// case IIntArrayStore:
			// IIntArrayStore intArrayStore = (IIntArrayStore)dataStore;
			// sb.append(intArrayStore.getElement(index));
			// break;
			// case ILongStore:
			// ILongStore longStore = (ILongStore)dataStore;
			// sb.append(longStore.getElement(index));
			// break;
			case ILongLookupStore:
				ILongLookupStore longLookupStore = (ILongLookupStore) dataStore;
				sb.append(longLookupStore.getElement(index));
				break;
			case IStringLookupStore:
				IStringLookupStore strLookupStore = (IStringLookupStore) dataStore;
				sb.append(strLookupStore.getElement(index));
				break;
			case IStringStore:
				IStringStore strStore = (IStringStore) dataStore;
				sb.append(strStore.getElement(index));
				break;
			case IStringArrayStore:
				IStringArrayStore strArrayStore = (IStringArrayStore) dataStore;
				sb.append(Arrays.toString(strArrayStore.getElement(index)));
				break;
			default:
				sb.append("store not coded - ").append(type.toString())
						.append("\n");
				break;
			}
			if (i < dataStores.size() - 1) {
				sb.append(",\t");
			}
		}

		return sb.toString();
	}

	private static String indent(int indentLevel) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indentLevel; i++) {
			sb.append("  ");
		}
		return sb.toString();
	}
}
