package au.org.intersect.faims.android.nutiteq;

import com.nutiteq.layers.vector.SpatialiteLayer;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;

public class CustomSpatialiteLayer extends SpatialiteLayer {

	private String name;
	private int layerId;
	private String dbPath;
	private String tableName;
	private SpatialiteTextLayer textLayer;
	private boolean textVisible = true;

	public CustomSpatialiteLayer(int layerId, String name, Projection proj, String dbPath,
			String tableName, String geomColumnName, String[] userColumns,
			int maxObjects, StyleSet<PointStyle> pointStyleSet,
			StyleSet<LineStyle> lineStyleSet,
			StyleSet<PolygonStyle> polygonStyleSet) {
		super(proj, dbPath, tableName, geomColumnName, userColumns, maxObjects,
				pointStyleSet, lineStyleSet, polygonStyleSet);
		this.name = name;
		this.layerId = layerId;
		this.dbPath = dbPath;
		this.tableName = tableName;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String layerName) {
		this.name = layerName;
	}

	public int getLayerId() {
		return layerId;
	}

	public String getDbPath() {
		return dbPath;
	}

	public String getTableName() {
		return tableName;
	}
	
	public SpatialiteTextLayer getTextLayer() {
		return textLayer;
	}

	public void setTextLayer(SpatialiteTextLayer textLayer) {
		this.textLayer = textLayer;
	}

	public boolean getTextVisible() {
		return textVisible;
	}
	
	public void setTextVisible(boolean visible) {
		this.textVisible = visible;
		updateTextLayer();
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		updateTextLayer();
	}
	
	private void updateTextLayer() {
		if (this.isVisible() && this.textVisible) {
			this.textLayer.setVisible(true);
		} else {
			this.textLayer.setVisible(false);
		}
	}


}
