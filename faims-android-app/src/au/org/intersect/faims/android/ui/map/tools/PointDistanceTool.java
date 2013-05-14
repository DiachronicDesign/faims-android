package au.org.intersect.faims.android.ui.map.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.location.Location;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CustomPoint;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class PointDistanceTool extends SelectTool {
	
	private class PointDistanceToolCanvas extends ToolCanvas {
		
		private float textX;
		private float textY;
		private MapPos tp1;
		private MapPos tp2;
		private float distance;
		
		private boolean showKm;
		
		public PointDistanceToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (tp1 != null && tp2 != null) {
				canvas.drawLine((float) tp1.x, (float) tp1.y, (float) tp2.x, (float) tp2.y, paint);
				
				if (showKm) {
					canvas.drawText(MeasurementUtil.displayAsKiloMeters(distance/1000), textX, textY, textPaint);
				} else {
					canvas.drawText(MeasurementUtil.displayAsMeters(distance), textX, textY, textPaint);
				}
				
			}
		}
		
		@Override
		public void clear() {
			tp1 = tp2 = null;
			invalidate();
		}

		public void drawDistanceBetween(MapPos p1, MapPos p2) {
			this.tp1 = GeometryUtil.transformVertex(p1, PointDistanceTool.this.mapView, true);
			this.tp2 = GeometryUtil.transformVertex(p2, PointDistanceTool.this.mapView, true);
			this.distance = PointDistanceTool.this.computeDistance(GeometryUtil.convertToWgs84(p1), GeometryUtil.convertToWgs84(p2));
			
			if (tp1.x > tp2.x) {
				MapPos t = tp2;
				tp2 = tp1;
				tp1 = t;
			}
			
			float midX = (float) (tp1.x + tp2.x) / 2;
			float midY = (float) (tp1.y + tp2.y) / 2;
			
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			textX = midX + offset;
			
			if (tp1.y > tp2.y){
				textY = midY + offset;
			} else {
				textY = midY - offset;
			}
			
			invalidate();
		}
		
		public void setShowKm(boolean value) {
			showKm = value;
			invalidate();
		}
		
	}
	
	public static final String NAME = "Point Distance";
	
	private PointDistanceToolCanvas canvas;

	public PointDistanceTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new PointDistanceToolCanvas(context);
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
		drawDistanceTo();
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
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			try {
				if ((element instanceof CustomPoint) && (mapView.getSelection().size() < 2)) {
					CustomPoint p = (CustomPoint) element;
					
					if (mapView.hasSelection(p)) {
						mapView.removeSelection(p);
					} else {
						mapView.addSelection(p);
					}
					
					drawDistanceTo();
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void drawDistanceTo() {
		if (mapView.getSelection().size() < 2) return;
		
		MapPos p1 = ((CustomPoint) mapView.getSelection().get(0)).getMapPos();
		MapPos p2 = ((CustomPoint) mapView.getSelection().get(1)).getMapPos();
		
		canvas.drawDistanceBetween(p1, p2);
		canvas.setColor(mapView.getDrawViewColor());
		canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
		canvas.setTextSize(mapView.getDrawViewTextSize());
		canvas.setShowKm(mapView.showKm());
	}
	
	public float computeDistance(MapPos p1, MapPos p2) {
		float[] results = new float[3];
		Location.distanceBetween(p1.y, p1.x, p2.y, p2.x, results);
		return results[0];
	}
	
	@Override
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Style Tool");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Style Settings");
				
				LinearLayout layout = new LinearLayout(context);
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				
				final EditText colorSetter = addSetter(context, layout, "Select Color:", Integer.toHexString(mapView.getDrawViewColor()));
				final SeekBar strokeSizeBar = addSlider(context, layout, "Stroke Size:", mapView.getDrawViewStrokeStyle());
				final SeekBar textSizeBar = addSlider(context, layout, "Text Size:", mapView.getDrawViewTextSize());
				final CheckBox decimalBox = addCheckBox(context, layout, "Show Degrees:", !mapView.showDecimal());
				final CheckBox kmBox = addCheckBox(context, layout, "Show Km:", mapView.showKm());
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							float strokeSize = parseSize(strokeSizeBar.getProgress());
							float textSize = parseSize(textSizeBar.getProgress());
							boolean showDecimal = !decimalBox.isChecked();
							boolean showKm = kmBox.isChecked();
							
							mapView.setDrawViewColor(color);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
							mapView.setShowDecimal(showDecimal);
							mapView.setShowKm(showKm);
							
							PointDistanceTool.this.drawDistanceTo();
						} catch (Exception e) {
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
				
				builder.create().show();
			}
				
		});
		return button;
	}
	
}
