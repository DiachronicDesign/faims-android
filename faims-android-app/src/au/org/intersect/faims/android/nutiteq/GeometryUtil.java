package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

import com.nutiteq.MapView;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.projections.EPSG3857;

public class GeometryUtil {

	public static Geometry fromGeometry(Geometry geom) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(p.getMapPos(), p.getLabel(), p.getStyleSet(), p.userData);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(l.getVertexList(), l.getLabel(), l.getStyleSet(), l.userData);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(p.getVertexList(), new ArrayList<List<MapPos>>(), p.getLabel(), p.getStyleSet(), p.userData);
		}
		return null;
	}
	
	public static Geometry worldToScreen(Geometry geom, MapView mapView) {
		return transformGeometry(geom, mapView, true);
	}
	
	public static Geometry screenToWorld(Geometry geom, MapView mapView) {
		return transformGeometry(geom, mapView, false);
	}
	
	public static ArrayList<Geometry> transformGeometryList(List<Geometry> geomList, MapView mapView, boolean worldToScreen) {
		ArrayList<Geometry> newGeomList = new ArrayList<Geometry>();
		for (Geometry geom : geomList) {
			newGeomList.add(transformGeometry(geom, mapView, worldToScreen));
		}
		return newGeomList;
	}
	
	public static Geometry transformGeometry(Geometry geom, MapView mapView, boolean worldToScreen) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(transformVertex(p.getMapPos(), mapView, worldToScreen), p.getLabel(), p.getStyleSet(), p.userData);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(transformVertices(l.getVertexList(), mapView, worldToScreen), l.getLabel(), l.getStyleSet(), l.userData);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(transformVertices(p.getVertexList(), mapView, worldToScreen), new ArrayList<List<MapPos>>(), p.getLabel(), p.getStyleSet(), p.userData);
		}
		return null;
	}
	
	public static MapPos transformVertex(MapPos vertex, MapView mapView, boolean worldToScreen) {
		if (worldToScreen)
			return mapView.worldToScreen(vertex.x, vertex.y, vertex.z);
		return mapView.screenToWorld(vertex.x, vertex.y);
	}
	
	public static List<MapPos> transformVertices(List<MapPos> vertices, MapView mapView, boolean worldToScreen) {
		List<MapPos> newVertices = new ArrayList<MapPos>();
		for (MapPos vertex : vertices) {
			newVertices.add(transformVertex(vertex, mapView, worldToScreen));
		}
		return newVertices;
	}
	
	public static Geometry convertGeometryFromWgs84(Geometry geom) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(convertFromWgs84(p.getMapPos()), p.getLabel(), p.getStyleSet(), p.userData);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(convertFromWgs84(l.getVertexList()), l.getLabel(), l.getStyleSet(), l.userData);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(convertFromWgs84(p.getVertexList()), new ArrayList<List<MapPos>>(), p.getLabel(), p.getStyleSet(), p.userData);
		}
		return null;
	}
	
	
	public static ArrayList<Geometry> convertGeometryListFromWgs84(List<Geometry> geomList) {
		ArrayList<Geometry> newGeomList = new ArrayList<Geometry>();
		for (Geometry geom : geomList) {
			newGeomList.add(convertGeometryFromWgs84(geom));
		}
		return newGeomList;
	}
	
	public static Geometry convertGeometryToWgs84(Geometry geom) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(convertToWgs84(p.getMapPos()), p.getLabel(), p.getStyleSet(), p.userData);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(convertToWgs84(l.getVertexList()), l.getLabel(), l.getStyleSet(), l.userData);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(convertToWgs84(p.getVertexList()), new ArrayList<List<MapPos>>(), p.getLabel(), p.getStyleSet(), p.userData);
		}
		return null;
	}
	
	
	public static ArrayList<Geometry> convertGeometryListToWgs84(List<Geometry> geomList) {
		ArrayList<Geometry> newGeomList = new ArrayList<Geometry>();
		for (Geometry geom : geomList) {
			newGeomList.add(convertGeometryToWgs84(geom));
		}
		return newGeomList;
	}
	
	public static double distance(MapPos p1, MapPos p2) {
		float[] results = new float[3];
		Location.distanceBetween(p1.y, p1.x, p2.y, p2.x, results);
		return results[0] / 1000;
	}
	
	public static List<MapPos> convertFromWgs84(List<MapPos> pts) {
		ArrayList<MapPos> list = new ArrayList<MapPos>();
		for (MapPos p : pts) {
			list.add(convertFromWgs84(p));
		}
		return list;
	}
	
	public static MapPos convertFromWgs84(MapPos p) {
		return (new EPSG3857()).fromWgs84(p.x, p.y);
	}
	
	public static List<MapPos> convertToWgs84(List<MapPos> pts) {
		ArrayList<MapPos> list = new ArrayList<MapPos>();
		for (MapPos p : pts) {
			list.add(convertToWgs84(p));
		}
		return list;
	}
	
	public static MapPos convertToWgs84(MapPos p) {
		return (new EPSG3857()).toWgs84(p.x, p.y);
	}

	public static MapPos computeAverage(List<MapPos> list) {
		if (list == null || list.size() == 0) {
			return new MapPos(0, 0);
		}
		double x = 0, y = 0;
		for (MapPos p : list) {
			x = x + p.x;
			y = y + p.y;
		}
		return new MapPos(x / list.size(), y / list.size());
	}
	
	public static MapPos computeMedium(List<MapPos> list) {
		if (list == null || list.size() == 0) {
			return new MapPos(0, 0);
		}
		double minx = 0, maxx= 0, miny = 0, maxy = 0;
		for (MapPos p : list) {
			if (p.x < minx) {
				minx = p.x;
			} else if (p.x > maxx) {
				maxx = p.x;
			}
			if (p.y < miny) {
				miny = p.y;
			} else if (p.y > maxy) {
				maxy = p.y;
			}
		}
		return new MapPos((minx + maxx) / 2, (miny + maxy) / 2);
	}
	
}
