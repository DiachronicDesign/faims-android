package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.view.View;
import au.org.intersect.faims.android.ui.map.CustomMapView;

public abstract class MapTool extends CustomMapView.CustomMapListener {
	
	protected String name;
	protected CustomMapView mapView;

	public MapTool(CustomMapView mapView, String name) {
		this.name = name;
		this.mapView = mapView;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public void activate() {
		
	}
	
	public void deactivate() {
		
	}
	
	// define tool context ui
	public abstract View getUI(final Context context);
	
}
