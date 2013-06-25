package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Polygon;

public class WKBUtil {
	
	public static Geometry[] cleanGeometry(Geometry[] geoms) {
		for (int i = 0; i < geoms.length; i++) {
			geoms[i] = cleanGeometry(geoms[i]);
		}
		return geoms;
	}

	public static Geometry cleanGeometry(Geometry geom) {
		if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			LinkedList<MapPos> list = new LinkedList<MapPos>(p.getVertexList());
			list.removeLast();
			return new Polygon(list, new ArrayList<List<MapPos>>(), p.getLabel(), p.getStyleSet(), p.userData);
		} else {
			return geom;
		}
	}
	
}
