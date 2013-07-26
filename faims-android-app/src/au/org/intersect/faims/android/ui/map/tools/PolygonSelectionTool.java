package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapText;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.ToolBarButton;

import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.VectorElement;

public class PolygonSelectionTool extends HighlightSelectionTool {

	public static final String NAME = "Polygon Selection";
	private MapButton settingsButton;
	protected float distance = 0;
	protected SettingsDialog settingsDialog;
	private MapButton clearButton;
	private MapText selectedDistance;

	public PolygonSelectionTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		settingsButton = createSettingsButton(context);
		clearButton = createClearButton(context);
		selectedDistance = new MapText(context);
		selectedDistance.setBackgroundColor(Color.WHITE);
		selectedDistance.setText("Current Distance: " + distance + " m");
		
		updateLayout();
	}
	
	@Override
	public void activate() {
		super.activate();
		if (!mapView.isProperProjection()) {
			showError("This tool will not function properly as projection is not a projected coordinate system.");
		}
	}
	
	@Override
	protected void updateLayout() {
		if (settingsButton != null) {
			layout.addView(settingsButton);
			layout.addView(selectSelection);
			layout.addView(restrictSelection);
			layout.addView(clearRestrictSelection);
			layout.addView(clearButton);
			layout.addView(selectedSelection);
			layout.addView(restrictedSelection);
			layout.addView(selectionCount);
		}
	}
	
	private MapButton createClearButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Clear");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearSelection();
			}
			
		});
		return button;
	}
	
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Set Distance");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				SettingsDialog.Builder builder = new SettingsDialog.Builder(context);
				builder.setTitle("Set Distance");
				
				builder.addTextField("distance", "Distance (m):", Float.toString(distance));
				builder.addCheckBox("remove", "Remove Selection:", false);
				
				builder.setPositiveButton("Run Query", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							float distance = Float.parseFloat(((EditText) settingsDialog.getField("distance")).getText().toString());
							boolean remove = settingsDialog.parseCheckBox("remove");
							
							PolygonSelectionTool.this.distance = distance;
							
							if (mapView.getHighlights().size() == 0) {
								showError("Please select a polygon");
								return;
							}
							
							Polygon polygon = (Polygon) mapView.getHighlights().get(0);
							
							mapView.runPolygonSelection((Polygon) GeometryUtil.convertGeometryToWgs84(polygon), distance, remove);
							selectedDistance.setText("Current Distance: " + distance + " m");
						} catch (Exception e) {
							FLog.e("error running polygon selection query", e);
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
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if ((element instanceof Polygon) && (mapView.getHighlights().size() < 1)) {
			super.onVectorElementClicked(element, arg1, arg2, arg3);
		}
	}
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Polygon");
		button.setSelectedState(R.drawable.tools_select_s);
		button.setNormalState(R.drawable.tools_select);
		return button;
	}

}
