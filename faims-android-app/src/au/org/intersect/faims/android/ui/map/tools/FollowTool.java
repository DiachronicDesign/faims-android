package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;
import au.org.intersect.faims.android.util.SpatialiteUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.VectorElement;

public class FollowTool extends HighlightTool {
	
	private class FollowToolCanvas extends ToolCanvas {
		
		private float distance;
		private float distanceTextX;
		private float distanceTextY;
		private float angle;
		private float angleTextX;
		private float angleTextY;
		
		private boolean showKm;
		
		private MapPos tp1;
		private MapPos tp2;
		private RectF rectF;
		private Geometry geom;
		
		public FollowToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				
				canvas.drawLine((float) tp1.x, (float) tp1.y, (float) tp2.x, (float) tp2.y, paint);
				
				if (showKm) {
					canvas.drawText("Distance: " + MeasurementUtil.displayAsKiloMeters(distance/1000), distanceTextX, distanceTextY, textPaint);
				} else {
					canvas.drawText("Distance: " + MeasurementUtil.displayAsMeters(distance), distanceTextX, distanceTextY, textPaint);
				}
				
				canvas.drawArc(rectF, FollowTool.this.mapView.getRotation()-90, angle, true, paint);
				
				canvas.drawText("Bearing: " + MeasurementUtil.displayAsDegrees(angle), angleTextX, angleTextY, textPaint);
				
				if (geom instanceof Polygon) {
					drawPolygonOverlay((Polygon) geom, canvas);
				}
			}
		}
		
		private void drawPolygonOverlay(Polygon polygon, Canvas canvas) {
			MapPos lp = null;
			for (MapPos p : polygon.getVertexList()) {
				p = transformPoint(p);
				if (lp != null) {
					canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
				}
				lp = p;
			}
			MapPos p = polygon.getVertexList().get(0);
			p = transformPoint(p);
			canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
		}
		
		protected MapPos transformPoint(MapPos p) {
			return GeometryUtil.transformVertex(p, mapView, true);
		}

		public void drawDistanceAndBearing(MapPos currentPoint, MapPos targetPoint) {
			this.isDirty = true;
			
			this.tp1 = GeometryUtil.transformVertex(GeometryUtil.convertFromWgs84(currentPoint), FollowTool.this.mapView, true);
			this.tp2 = GeometryUtil.transformVertex(GeometryUtil.convertFromWgs84(targetPoint), FollowTool.this.mapView, true);
			
			this.distance = (float) SpatialiteUtil.distanceBetween(currentPoint, targetPoint);
			
			float dx = (float) (tp2.x - tp1.x);
			float dy = (float) (tp2.y - tp1.y);
			float d = (float) Math.sqrt(dx * dx + dy * dy) / 2;
			
			this.rectF = new RectF((float) tp1.x - d, (float) tp1.y - d, (float) tp1.x + d, (float) tp1.y + d);
			
			this.angle = SpatialiteUtil.computeAzimuth(currentPoint, targetPoint);
			
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			distanceTextX = (float) tp1.x + offset;
			distanceTextY = (float) tp1.y + offset;
			
			angleTextX = (float) tp1.x + offset;
			angleTextY = (float) tp1.y + 2 * offset;
			
			invalidate();
		}
		
		public void setShowKm(boolean value) {
			showKm = value;
			invalidate();
		}

		public void drawBuffer(Geometry geom) {
			this.isDirty = true;
			
			this.geom = geom;
			
			this.invalidate();
		}
		
	}
	
	public static final String NAME = "Follow Path";
	
	private FollowToolCanvas canvas;

	protected SettingsDialog settingsDialog;

	public FollowTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new FollowToolCanvas(context);
		container.addView(canvas);
	}

	@Override 
	public void activate() {
		super.activate();
		canvas.clear();
	}
	
	@Override
	public void deactivate() {
		super.deactivate();
		canvas.clear();
	}
	
	@Override
	public void onLayersChanged() {
		super.onLayersChanged();
		canvas.clear();
	}
	
	@Override
	public void onMapChanged() {
		super.onMapChanged();
		updateDistanceAndBearing();
	}
	
	@Override
	public void onMapUpdate() {
		super.onMapUpdate();
		updateDistanceAndBearing();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (canvas != null) layout.addView(canvas);
	}
	
	@Override
	protected void clearSelection() {
		super.clearSelection();
		canvas.clear();
		mapView.setGeomToFollow(null);
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			try {
				if (((element instanceof Line) || (element instanceof Point)) && (mapView.getHighlights().size() < 1)) {
					Geometry geom = (Geometry) element;
					
					if (mapView.hasHighlight(geom)) {
						mapView.removeHighlight(geom);
					} else {
						mapView.addHighlight(geom);
					}
					
					mapView.setGeomToFollow(GeometryUtil.convertGeometryToWgs84(geom));
					
					updateDistanceAndBearing();
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void updateDistanceAndBearing() {
		try {
			MapPos pos = mapView.getCurrentPosition();
			if (pos == null) return;
			if (mapView.getGeomToFollow() == null) return;
			
			Geometry buffer = mapView.getGeomToFollowBuffer();
			
			canvas.drawBuffer(buffer);

			MapPos targetPoint = mapView.nextPointToFollow(pos, mapView.getPathBuffer());
			
			canvas.drawDistanceAndBearing(pos, targetPoint);
			canvas.setColor(mapView.getDrawViewColor());
			canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
			canvas.setTextSize(mapView.getDrawViewTextSize());
			canvas.setShowKm(mapView.showKm());
		} catch (Exception e) {
			FLog.e("error updating distance and bearing", e);
			showError(e.getMessage());
		}
	}
	
	@Override
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Style Tool");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				SettingsDialog.Builder builder = new SettingsDialog.Builder(context);
				builder.setTitle("Style Settings");
				
				builder.addTextField("color", "Select Color:", Integer.toHexString(mapView.getDrawViewColor()));
				builder.addSlider("strokeSize", "Stroke Size:", mapView.getDrawViewStrokeStyle());
				builder.addSlider("textSize", "Text Size:", mapView.getDrawViewTextSize());
				builder.addCheckBox("showDegrees", "Show Degrees:", !mapView.showDecimal());
				builder.addCheckBox("showKm", "Show Km:", mapView.showKm());
				builder.addTextField("buffer", "Buffer Size (m):", Float.toString(mapView.getPathBuffer()));
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = settingsDialog.parseColor("color");
							float strokeSize = settingsDialog.parseSlider("strokeSize");
							float textSize = settingsDialog.parseSlider("textSize");
							boolean showDecimal = !settingsDialog.parseCheckBox("showDegrees");
							boolean showKm = settingsDialog.parseCheckBox("showKm");
							float buffer = Float.parseFloat(((EditText)settingsDialog.getField("buffer")).getText().toString());
							
							mapView.setDrawViewColor(color);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
							mapView.setShowDecimal(showDecimal);
							mapView.setShowKm(showKm);
							mapView.setPathBuffer(buffer);
							
							FollowTool.this.updateDistanceAndBearing();
						} catch (Exception e) {
							FLog.e(e.getMessage(), e);
							showError(e.getMessage());
						}
					}
				});
				
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
				
				settingsDialog = builder.create();
				settingsDialog.show();
			}
				
		});
		return button;
	}

}
