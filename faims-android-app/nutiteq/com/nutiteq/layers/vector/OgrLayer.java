package com.nutiteq.layers.vector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.log.Log;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.DefaultLabel;
import com.nutiteq.ui.Label;
import com.nutiteq.utils.WkbRead;
import com.nutiteq.vectorlayers.GeometryLayer;

public class OgrLayer extends GeometryLayer {
	private int maxObjects;
	private String tableName;
	private StyleSet<PointStyle> pointStyleSet;
	private StyleSet<LineStyle> lineStyleSet;
	private StyleSet<PolygonStyle> polygonStyleSet;
	private DataSource hDataset;
    private String[] fieldNames;
    private float minZoom = Float.MAX_VALUE;

	static {
		ogr.RegisterAll();
	}

	/**
	 * Open OGR datasource. Datasource properties depend on particular data type, e.g. for Shapefile just give file name
	 * This sample tries to read whole layer, you probably need adjustments to optimize reading depending on data specifics
	 * 
	 * @param proj layer projection. NB! data must be in the same projection
	 * @param fileName datasource name: file or connection string
	 * @param tableName table (OGR layer) name, needed for multi-layer datasets. If null, takes the first layer from dataset
	 * @param maxObjects limit number of objects to avoid out of memory. Could be 1000 for points, less for lines/polygons
	 * @param pointStyleSet styleset for point objects
	 * @param lineStyleSet styleset for line objects
	 * @param polygonStyleSet styleset for polygon objects
	 * @throws IOException file not found or other problem opening OGR datasource
	 */
	public OgrLayer(Projection proj, String fileName, String tableName, int maxObjects,
			StyleSet<PointStyle> pointStyleSet, StyleSet<LineStyle> lineStyleSet, StyleSet<PolygonStyle> polygonStyleSet) throws IOException {
		super(proj);
		this.maxObjects = maxObjects;
		this.tableName = tableName;
		this.pointStyleSet = pointStyleSet;
		this.lineStyleSet = lineStyleSet;
		this.polygonStyleSet = polygonStyleSet;

		hDataset = ogr.Open(fileName);
		if (hDataset == null) {
			Log.error("OgrLayer: unable to open dataset '"+fileName+"'");
			throw new IOException("OgrLayer: unable to open dataset '"+fileName+"'");
		}
		this.fieldNames = getFieldNames(tableName);

        if (pointStyleSet != null) {
            minZoom = Math.min(minZoom, pointStyleSet.getFirstNonNullZoomStyleZoom());
        }
        if (lineStyleSet != null) {
            minZoom = Math.min(minZoom, lineStyleSet.getFirstNonNullZoomStyleZoom());
        }
        if (polygonStyleSet != null) {
            minZoom = Math.min(minZoom, polygonStyleSet.getFirstNonNullZoomStyleZoom());
        }
        Log.debug("ogrLayer minZoom = "+minZoom);
	}


    // TODO: finalizer required?
	// TODO: check if map data and layer projections are same. If not, need to convert spatial filter rect (from map->data projection) and data objects (data->map projection).

    @Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
	    long timeStart = System.currentTimeMillis();
	    if (hDataset == null || zoom < minZoom) {
			return;
		}
	    

		org.gdal.ogr.Layer layer = null;
		if (tableName == null) {
			layer = hDataset.GetLayer(0);
		}
		else {
//			layer = hDataset.ExecuteSQL("SELECT * FROM " + tableName);
		    layer = hDataset.GetLayerByName(tableName);
		}
		if (layer == null) {
			Log.error("OgrLayer: could not find layer '"+tableName+"'");
			return;
		}

		MapPos minPos = projection.fromInternal(envelope.getMinX(), envelope.getMinY());
		MapPos maxPos = projection.fromInternal(envelope.getMaxX(), envelope.getMaxY());
		
		layer.SetSpatialFilterRect(
			Math.min(minPos.x, maxPos.x), Math.min(minPos.y, maxPos.y),
			Math.max(minPos.x, maxPos.x), Math.max(minPos.y, maxPos.y)
		);

		List<com.nutiteq.geometry.Geometry> newVisibleElementsList = new LinkedList<com.nutiteq.geometry.Geometry>();

		layer.ResetReading();
		Feature feature = layer.GetNextFeature();
		Geometry poSrcGeom;

		
		
		for (int n = 0; feature != null && n < maxObjects; n++) {

		    poSrcGeom = feature.GetGeometryRef();
		    if (poSrcGeom == null) {
		    	Log.error("unknown src geom " + n);
		    	android.util.Log.e("FAIMS", "unknown src geom " + n);
		    	continue;
		    }
			int eType = poSrcGeom.GetGeometryType();
			if (eType == ogr.wkbUnknown) {
			    Log.error("unknown object type "+eType);
			    android.util.Log.e("FAIMS", "unknown object type "+eType);
				continue;
			}

            final Map<String, String> userData = new HashMap<String, String>();

            for(int field=0; field<feature.GetFieldCount();field++){
                userData.put(this.fieldNames[field], feature.GetFieldAsString(field));
            }
            
            Label label = createLabel(userData);

            byte[] geomWkb = poSrcGeom.ExportToWkb();
            com.nutiteq.geometry.Geometry[] geoms = WkbRead.readWkb(new ByteArrayInputStream(geomWkb), userData);
            
            // add stylesets, new objects are needed for this
            for(int i = 0; i<geoms.length; i++){
                com.nutiteq.geometry.Geometry object = geoms[i];           
                com.nutiteq.geometry.Geometry newObject = null;
                
                if(object instanceof com.nutiteq.geometry.Point){
                    newObject = new com.nutiteq.geometry.Point(((Point) object).getMapPos(), label, pointStyleSet, object.userData);
                }else if(object instanceof Line){
                    newObject = new com.nutiteq.geometry.Line(((Line) object).getVertexList(), label, lineStyleSet, object.userData);
                }else if(object instanceof Polygon){
                    newObject = new com.nutiteq.geometry.Polygon(((Polygon) object).getVertexList(), ((Polygon) object).getHolePolygonList(), label, polygonStyleSet, object.userData);
                }
                
                newObject.attachToLayer(this);
                newObject.setActiveStyle(zoom);
                newVisibleElementsList.add(newObject);
                
			 feature = layer.GetNextFeature();
            }
		}
		long timeEnd = System.currentTimeMillis();
		Log.debug("OgrLayer loaded "+tableName+" N:"+ newVisibleElementsList.size()+" time ms:"+(timeEnd-timeStart));
		android.util.Log.d("FAIMS", "OgrLayer loaded "+tableName+" N:"+ newVisibleElementsList.size()+" time ms:"+(timeEnd-timeStart));
		setVisibleElementsList(newVisibleElementsList);
	}

	protected Label createLabel(Map<String, String> userData) {
	    StringBuffer labelTxt = new StringBuffer();
	    for(Map.Entry<String, String> entry : userData.entrySet()){
	        labelTxt.append(entry.getKey() + ": " + entry.getValue()+"\n");
	    }
	    
		return new DefaultLabel(labelTxt.toString());
	}
	
	
    private String[] getFieldNames(String table) {
        Layer poLayer;
        if(tableName != null){
            poLayer = hDataset.GetLayerByName(tableName);
        }else{
            poLayer = hDataset.GetLayer(0);
        }
        if(poLayer == null){
            Log.error("Layer not found "+table);
            return null;
        }
        FeatureDefn poDefn = poLayer.GetLayerDefn();
        
        String[] names = new String[poDefn.GetFieldCount()];
        
        for (int iAttr = 0; iAttr < poDefn.GetFieldCount(); iAttr++) {
            FieldDefn poField = poDefn.GetFieldDefn(iAttr);
            names[iAttr] = poField.GetNameRef();
        }
        return names;
    }
	
	// print layer details for troubleshooting. Code from ogrinfo.java
	public void printLayerDetails(String table) {

        Layer poLayer;
        if(tableName != null){
            poLayer = hDataset.GetLayerByName(table);
        }else{
            poLayer = hDataset.GetLayer(0);
        }
        if(poLayer == null){
            Log.error("Layer not found "+table);
            return;
        }
        FeatureDefn poDefn = poLayer.GetLayerDefn();
        Log.debug("Layer name: " + poDefn.GetName());
        Log.debug("Geometry: " + ogr.GeometryTypeToName(poDefn.GetGeomType()));

        Log.debug("Feature Count: " + poLayer.GetFeatureCount());

        double oExt[] = poLayer.GetExtent(true);
        if (oExt != null)
            Log.debug("Extent: (" + oExt[0] + ", " + oExt[2] + ") - ("
                    + oExt[1] + ", " + oExt[3] + ")");

        String pszWKT;

        if (poLayer.GetSpatialRef() == null)
            pszWKT = "(unknown)";
        else {
            pszWKT = poLayer.GetSpatialRef().ExportToPrettyWkt();
        }

        Log.debug("Layer SRS WKT:\n" + pszWKT);

        if (poLayer.GetFIDColumn().length() > 0)
            Log.debug("FID Column = " + poLayer.GetFIDColumn());

        if (poLayer.GetGeometryColumn().length() > 0)
            Log.debug("Geometry Column = " + poLayer.GetGeometryColumn());

        for (int iAttr = 0; iAttr < poDefn.GetFieldCount(); iAttr++) {
            FieldDefn poField = poDefn.GetFieldDefn(iAttr);

            Log.debug(poField.GetNameRef() + ": "
                    + poField.GetFieldTypeName(poField.GetFieldType()) + " ("
                    + poField.GetWidth() + "." + poField.GetPrecision() + ")");
        }
    }

	public void printSupportedDrivers() {
	    Log.debug("Supported drivers:");
        for( int iDriver = 0; iDriver < ogr.GetDriverCount(); iDriver++ )
        {
            Log.debug( "  -> " + ogr.GetDriver(iDriver).GetName() );
        }
    }

}
