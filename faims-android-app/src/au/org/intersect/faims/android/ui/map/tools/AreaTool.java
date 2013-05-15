package au.org.intersect.faims.android.ui.map.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CustomPolygon;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;
import au.org.intersect.faims.android.util.SpatialiteUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG4326;

public class AreaTool extends SelectTool {
	
	private class AreaToolCanvas extends ToolCanvas {
		
		private float textX;
		private float textY;
		
		private boolean showKm;
		
		public AreaToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				
				if (showKm) {
					canvas.drawText(MeasurementUtil.displayAsKiloMeters(AreaTool.this.area/(1000*1000)) + "\u00B2", textX, textY, textPaint);
				} else {
					canvas.drawText(MeasurementUtil.displayAsMeters(AreaTool.this.area) + "\u00B2", textX, textY, textPaint);
				}
				
			}
		}
		
		public void drawArea(CustomPolygon polygon) {
			this.isDirty = true;
			
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			MapPos p = GeometryUtil.transformVertex(polygon.getVertexList().get(polygon.getVertexList().size()-1), AreaTool.this.mapView, true);
			
			textX = (float) p.x + offset;
			textY = (float) p.y + offset;
			
			invalidate();
		}
		
		public void setShowKm(boolean value) {
			showKm = value;
			invalidate();
		}
		
	}
	
	public static final String NAME = "Area";
	
	private AreaToolCanvas canvas;

	private float area;

	public AreaTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new AreaToolCanvas(context);
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
		drawArea();
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
				if ((element instanceof CustomPolygon) && (mapView.getSelection().size() < 1)) {
					CustomPolygon p = (CustomPolygon) element;
					
					if (mapView.hasSelection(p)) {
						mapView.removeSelection(p);
					} else {
						mapView.addSelection(p);
					}
					
					computeArea();
					drawArea();
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void computeArea() {
		if (mapView.getSelection().size() < 1) return;
		
		CustomPolygon polygon = (CustomPolygon) mapView.getSelection().get(0);
		
		this.area = computePolygonArea(polygon);
	}
	
	private void drawArea() {
		if (mapView.getSelection().size() < 1) return;
		
		CustomPolygon polygon = (CustomPolygon) mapView.getSelection().get(0);
		
		canvas.drawArea(polygon);
		canvas.setColor(mapView.getDrawViewColor());
		canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
		canvas.setTextSize(mapView.getDrawViewTextSize());
		canvas.setShowKm(mapView.showKm());
	}
	
	public float computePolygonArea(CustomPolygon polygon) {
		try {
			polygon = (CustomPolygon) GeometryUtil.projectGeometry(new EPSG4326(), polygon);
			return (float) SpatialiteUtil.computeArea(polygon);
		} catch (Exception e) {
			FLog.e("error computing area of polygon", e);
			showError("Error computing area of polygon");
		}
		
		return 0;
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
				
				ScrollView scrollView = new ScrollView(context);
				LinearLayout layout = new LinearLayout(context);
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				scrollView.addView(layout);
				
				final EditText colorSetter = addSetter(context, layout, "Select Color:", Integer.toHexString(mapView.getDrawViewColor()));
				final SeekBar strokeSizeBar = addSlider(context, layout, "Stroke Size:", mapView.getDrawViewStrokeStyle());
				final SeekBar textSizeBar = addSlider(context, layout, "Text Size:", mapView.getDrawViewTextSize());
				final CheckBox decimalBox = addCheckBox(context, layout, "Show Degrees:", !mapView.showDecimal());
				final CheckBox kmBox = addCheckBox(context, layout, "Show Km:", mapView.showKm());
				
				builder.setView(scrollView);
				
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
							
							AreaTool.this.drawArea();
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
