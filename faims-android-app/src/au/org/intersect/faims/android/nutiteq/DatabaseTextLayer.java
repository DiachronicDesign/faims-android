package au.org.intersect.faims.android.nutiteq;

import java.util.List;
import java.util.Vector;

import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.SpatialiteUtil;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.Text;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.StyleSet;
import com.nutiteq.style.TextStyle;
import com.nutiteq.vectorlayers.TextLayer;

public class DatabaseTextLayer extends TextLayer {
	
	private DatabaseLayer databaseLayer;
	private StyleSet<TextStyle> styleSet;
	private int minZoom;

	public DatabaseTextLayer(Projection projection, DatabaseLayer databaseLayer, StyleSet<TextStyle> styleSet) {
		super(projection);
		this.databaseLayer = databaseLayer;
		this.styleSet = styleSet;
		if (styleSet != null) {
			this.minZoom = styleSet.getFirstNonNullZoomStyleZoom();
		}
	}
	
	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
	    
	    if (zoom < minZoom) {
	        setVisibleElementsList(null);
	      return;
	    }

	    List<Geometry> geometries = databaseLayer.getVisibleElements();
	    if (geometries == null || geometries.size() == 0) return;
	    
	    Vector<Text> objects = new Vector<Text>();
	    
	    for(Geometry geom: geometries){
	        
			final String[] userData = (String[]) geom.userData;
	        String label = userData[1]; // note: userData for geometry is set in fetchEntity or fetchRelationship
	        
	        MapPos topRight = null;
	        if (geom instanceof Point) {
	        	topRight = ((Point) geom).getMapPos();
	        } else if (geom instanceof Line) {
	        	topRight = ((Line) geom).getVertexList().get(0);
	        } else if (geom instanceof Polygon) {
	        	try {
	        		MapPos center = SpatialiteUtil.computeCentroid((Polygon) GeometryUtil.convertGeometryToWgs84(geom));
	        		topRight = GeometryUtil.convertFromWgs84(center);
	        	} catch (Exception e) {
	        		topRight = new MapPos(0, 0);
	        		FLog.e("error computing centroid of polygon", e);
	        	}
	        } else {
	        	FLog.e("invalid geometry type");
	        }
	        
	        Text newText = new Text(topRight, label, styleSet, null);
	        
	        newText.attachToLayer(this);
	        newText.setActiveStyle(zoom);
	        
	        objects.add(newText);
	    }
	    
	    setVisibleElementsList(objects);

	  }

}
