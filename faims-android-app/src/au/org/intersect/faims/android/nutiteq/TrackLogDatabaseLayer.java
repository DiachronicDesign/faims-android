package au.org.intersect.faims.android.nutiteq;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import android.graphics.Color;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;

public class TrackLogDatabaseLayer extends DatabaseLayer {

	private Map<User, Boolean> users;

	public TrackLogDatabaseLayer(int layerId, String name, Projection projection, CustomMapView mapView, Type type,
			String queryName, String querySql, DatabaseManager dbmgr, int maxObjects, Map<User, Boolean> users,
			StyleSet<PointStyle> pointStyleSet, StyleSet<LineStyle> lineStyleSet, StyleSet<PolygonStyle> polygonStyleSet) {
		super(layerId, name, projection, mapView, type, queryName, querySql, dbmgr,
				maxObjects, pointStyleSet, lineStyleSet, polygonStyleSet);
		this.users = users;
	}

	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		if (zoom < minZoom) {
	        setVisibleElementsList(null);
	        return;
	    }
		
		try {
			MapPos min = GeometryUtil.convertToWgs84(GeometryUtil.transformVertex(new MapPos(-BOUNDARY_PADDING, -BOUNDARY_PADDING), mapView, false));
			MapPos max = GeometryUtil.convertToWgs84(GeometryUtil.transformVertex(new MapPos(mapView.getWidth() + BOUNDARY_PADDING, mapView.getHeight() + BOUNDARY_PADDING), mapView, false));
			
			if (type == Type.GPS_TRACK) {
				Vector<Geometry> objects = new Vector<Geometry>();
				for(Entry<User, Boolean> user : users.entrySet()){
					if(user.getValue()){
						Vector<Geometry> objectTemp = null;
						String md5Hex = new String(Hex.encodeHex(DigestUtils.md5(user.getKey().getFirstName() + " " + user.getKey().getLastName())));
						int hue =  (int) Long.parseLong(md5Hex.substring(0, 10),16) % 360;
						float[] hsv = new float[3];
						hsv[0] = hue < 0 ? hue + 360 : hue;
						hsv[1] = 1;
						hsv[2] = 1;
						GeometryStyle pointStyle = GeometryStyle.defaultPointStyle();
						pointStyle.pointColor = Color.HSVToColor(hsv);
						pointStyleSet = pointStyle.toPointStyleSet();
						objectTemp = dbmgr.fetchVisibleGPSTrackingForUser(min, max, maxObjects, querySql, user.getKey().getUserId());
						createElementsInLayer(zoom, objectTemp, objects);
					}
				}
				setVisibleElementsList(objects);
			}else {
				super.calculateVisibleElements(envelope, zoom);
			}
		} catch (Exception e) {
			FLog.e("error rendering track log layer", e);
		}
	}

	public void toggleUser(User user, boolean isChecked){
		users.put(user, isChecked);
	}
}
