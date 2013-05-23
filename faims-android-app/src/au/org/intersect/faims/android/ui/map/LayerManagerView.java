package au.org.intersect.faims.android.ui.map;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jsqlite.Database;
import jsqlite.Stmt;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.FileManager;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.CustomGdalMapLayer;
import au.org.intersect.faims.android.nutiteq.CustomSpatialiteLayer;
import au.org.intersect.faims.android.nutiteq.DatabaseLayer;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.nutiteq.GeometryTextStyle;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.dialog.LineStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PointStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PolygonStyleDialog;
import au.org.intersect.faims.android.ui.dialog.TextStyleDialog;
import au.org.intersect.faims.android.ui.form.CustomDragDropListView;

import com.nutiteq.layers.Layer;

public class LayerManagerView extends LinearLayout {
	
	private class LayersAdapter extends BaseAdapter {
		
		private List<Layer> layers;
		private ArrayList<View> itemViews;

		public LayersAdapter(List<Layer> layers) {
			this.layers = layers;
			this.itemViews = new ArrayList<View>();
			
			for (Layer layer : layers) {
				LayerListItem item = new LayerListItem(LayerManagerView.this.getContext());
				item.init(layer, LayerManagerView.this);
				itemViews.add(item);
			} 
		}
		
		@Override
		public int getCount() {
			return layers.size();
		}

		@Override
		public Object getItem(int position) {
			return layers.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View arg1, ViewGroup arg2) {
			return itemViews.get(position);
		}
		
	}

	private CustomMapView mapView;
	private LinearLayout layout;
	private LinearLayout buttonsLayout;
	private CustomDragDropListView listView;
	private TextView selectedFileText;
	private Spinner tableNameSpinner;
	private Spinner labelColumnSpinner;
	protected File rasterFile;
	protected File spatialFile;
	protected GeometryStyle pointStyle;
	protected GeometryStyle lineStyle;
	protected GeometryStyle polygonStyle;
	protected GeometryTextStyle textStyle;
	protected PolygonStyleDialog polygonStyleDialog;
	protected LineStyleDialog lineStyleDialog;
	protected PointStyleDialog pointStyleDialog;
	protected TextStyleDialog textStyleDialog;

	public LayerManagerView(Context context) {
		super(context);
		
		pointStyle = GeometryStyle.defaultPointStyle();
		lineStyle = GeometryStyle.defaultLineStyle();
		polygonStyle = GeometryStyle.defaultPolygonStyle();
		textStyle = GeometryTextStyle.defaultStyle();
		
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		setOrientation(LinearLayout.VERTICAL);
		
		ShowProjectActivity activity = (ShowProjectActivity) this.getContext();
		activity.getFileManager().addListener(ShowProjectActivity.RASTER_FILE_BROWSER_REQUEST_CODE, new FileManager.FileManagerListener() {
			
			@Override
			public void onFileSelected(File file) {
				LayerManagerView.this.rasterFile = file;
				LayerManagerView.this.selectedFileText.setText(file.getName());
			}
		});
		activity.getFileManager().addListener(ShowProjectActivity.SPATIAL_FILE_BROWSER_REQUEST_CODE, new FileManager.FileManagerListener() {
			
			@Override
			public void onFileSelected(File file) {
				LayerManagerView.this.spatialFile = file;
				LayerManagerView.this.selectedFileText.setText(file.getName());
				try {
					setTableSpinner();
				} catch (jsqlite.Exception e) {
					FLog.e("Not a valid spatial layer file", e);
					showErrorDialog("Not a valid spatial layer file");
				}
			}
		});
	}
	
	public void attachToMap(CustomMapView mapView) {
		this.mapView = mapView;
		
		layout = new LinearLayout(this.getContext());
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		addView(layout);
		
		buttonsLayout = new LinearLayout(this.getContext());
		buttonsLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
		layout.addView(buttonsLayout);
		
		createAddButton();
		createOrderButton();
		createListView();
		
		redrawLayers();
	}
	
	private void createListView() {
		listView = new CustomDragDropListView(this.getContext(),null);
		listView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position,
					long arg3) {
				try {
					List<Layer> layers = mapView.getAllLayers();
					int last = layers.size() - 1;
					final Layer layer = layers.get(last - position);
					LayerListItem itemView = (LayerListItem) view;
					itemView.toggle();
					mapView.setLayerVisible(layer, itemView.isChecked());
					mapView.updateTools();
				} catch (Exception e) {
					showErrorDialog("Error setting layer visibility");
				}
			}
			
		});
		
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				List<Layer> layers = mapView.getAllLayers();
				int last = layers.size() - 1;
				final Layer layer = layers.get(last - position);
				
				Context context = LayerManagerView.this.getContext();
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Layer Options");

				LinearLayout layout = new LinearLayout(context);
				layout.setOrientation(LinearLayout.VERTICAL);
				
				builder.setView(layout);
				final Dialog d = builder.create();
				
				Button removeButton = new Button(context);
				removeButton.setText("Remove");
				removeButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						d.dismiss();
						removeLayer(layer);
					}
					
				});
				
				Button renameButton = new Button(context);
				renameButton.setText("Rename");
				renameButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						d.dismiss();
						renameLayer(layer);
					}
					
				});
				
				Button showMetadataButton = new Button(context);
				showMetadataButton.setText("Show Metadata");
				showMetadataButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						d.dismiss();
						showMetadata(layer);
					}
				});
				
				layout.addView(removeButton);
				layout.addView(renameButton);
				layout.addView(showMetadataButton);
				
				if (layer instanceof CustomSpatialiteLayer) {
					final ToggleButton showLabelsButton = new ToggleButton(context);
					showLabelsButton.setTextOn("Hide Labels");
					showLabelsButton.setTextOff("Show Labels");
					showLabelsButton.setChecked(((CustomSpatialiteLayer) layer).getTextVisible());
					showLabelsButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View arg0) {
							((CustomSpatialiteLayer) layer).setTextVisible(showLabelsButton.isChecked());
						}
					});
					layout.addView(showLabelsButton);
				} else if (layer instanceof DatabaseLayer) {
					final ToggleButton showLabelsButton = new ToggleButton(context);
					showLabelsButton.setTextOn("Hide Labels");
					showLabelsButton.setTextOff("Show Labels");
					showLabelsButton.setChecked(((DatabaseLayer) layer).getTextVisible());
					showLabelsButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View arg0) {
							((DatabaseLayer) layer).setTextVisible(showLabelsButton.isChecked());
						}
					});
					layout.addView(showLabelsButton);
				}
				
				d.show();
				return true;
			}
			
		});

		layout.addView(listView);
	}

	private void createAddButton() {
		Button addButton = new Button(this.getContext());
		addButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		addButton.setText("Add");
		addButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				addLayer();
			}
			
		});
		
		buttonsLayout.addView(addButton);
	}
	
	private void createOrderButton(){
		ToggleButton orderButton = new ToggleButton(this.getContext());
		orderButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		orderButton.setTextOn("Order ON");
		orderButton.setTextOff("Order OFF");
		orderButton.setChecked(false);
		orderButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					listView.setDropListener(new CustomDragDropListView.DropListener() {
						public void drop(int from, int to) {
							List<Layer> layers = mapView.getAllLayers();
							int last = layers.size() - 1;
							boolean isFromBaseLayer = layers.get(last - from) == mapView.getLayers().getBaseLayer();
							boolean isToBaseLayer = layers.get(last - to) == mapView.getLayers().getBaseLayer();
							if(!(isFromBaseLayer || isToBaseLayer)){
								Collections.swap(layers, last - from, last - to);
								mapView.setAllLayers(layers);
								redrawLayers();
							}
						}
					});
				}else{
					listView.removeDropListener();
				}
			}
		});
		buttonsLayout.addView(orderButton);
	}
	
	public void redrawLayers() {
		List<Layer> layers = mapView.getAllLayers();
		List<Layer> shownLayer = new ArrayList<Layer>(layers);
		Collections.reverse(shownLayer);
		LayersAdapter layersAdapter = new LayersAdapter(shownLayer);
		listView.setAdapter(layersAdapter);
	}
	
	private void addLayer() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle("Add Layer");

		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		final Dialog d = builder.create();
		
		Button loadRasterLayerButton = new Button(getContext());
		loadRasterLayerButton.setText("Load Raster Layer");
		loadRasterLayerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				addRasterLayer();
			}
			
		});
		
		// TODO Fix the bug with loading shape layer
//		Button loadShapeLayerButton = new Button(getContext());
//		loadShapeLayerButton.setText("Load Shape Layer");
//		loadShapeLayerButton.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View arg0) {
//				d.dismiss();
//				addShapeLayer();
//			}
//			
//		});
		
		Button loadSpatialLayerButton = new Button(getContext());
		loadSpatialLayerButton.setText("Load Vector Layer");
		loadSpatialLayerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				addSpatialLayer();
			}
			
		});
		
		Button loadDatabaseLayerButton = new Button(getContext());
		loadDatabaseLayerButton.setText("Load Database Layer");
		loadDatabaseLayerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				addDatabaseLayer();
			}
			
		});
		
		Button createLayerButton = new Button(getContext());
		createLayerButton.setText("Create Canvas Layer");
		createLayerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				createLayer();
			}
			
		});
		
		layout.addView(loadRasterLayerButton);
//		layout.addView(loadShapeLayerButton);
		layout.addView(loadSpatialLayerButton);
		layout.addView(loadDatabaseLayerButton);
		layout.addView(createLayerButton);
		
		d.show();
	}
	
	private void addRasterLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add raster layer:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Raster layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		layout.addView(editText);
		
		Button browserButton = new Button(getContext());
		browserButton.setText("browse");
		browserButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showFileBrowser(ShowProjectActivity.RASTER_FILE_BROWSER_REQUEST_CODE);
			}
		});
		layout.addView(browserButton);
		selectedFileText = new TextView(this.getContext());
		layout.addView(selectedFileText);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					if(rasterFile != null){
						mapView.addRasterMap(editText.getText().toString(), rasterFile.getPath());
						double[][] boundaries = ((CustomGdalMapLayer) mapView.getLayers().getBaseLayer()).getBoundaries();
						mapView.setMapFocusPoint(((float)boundaries[0][0]+(float)boundaries[3][0])/2, ((float)boundaries[0][1]+(float)boundaries[3][1])/2);
						redrawLayers();
					}
				} catch (Exception e) {
					showErrorDialog(e.getMessage());
				}
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		builder.create().show();
	}
	
//	private void addShapeLayer(){
//		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
//		
//		builder.setTitle("Layer Manager");
//		builder.setMessage("Add shape layer:");
//		
//		LinearLayout layout = new LinearLayout(getContext());
//		layout.setOrientation(LinearLayout.VERTICAL);
//		
//		builder.setView(layout);
//		
//		TextView textView = new TextView(this.getContext());
//		textView.setText("Shape layer name:");
//		layout.addView(textView);
//		final EditText editText = new EditText(LayerManagerView.this.getContext());
//		layout.addView(editText);
//		
//		Button browserButton = new Button(getContext());
//		browserButton.setText("browse");
//		browserButton.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View arg0) {
//				showFileBrowser(ShowProjectActivity.RASTER_FILE_BROWSER_REQUEST_CODE);
//			}
//		});
//		layout.addView(browserButton);
//		selectedFileText = new TextView(this.getContext());
//		layout.addView(selectedFileText);
//
//		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//
//			@SuppressWarnings("unchecked")
//			@Override
//			public void onClick(DialogInterface arg0, int arg1) {
//				try {
//					if(fm.getSelectedFile() != null){
//						StyleSet<PointStyle> ps = (StyleSet<PointStyle>) createStyleSet(10, createPointStyle(Color.RED, 0.05f, 0.1f));
//						StyleSet<LineStyle> ls = (StyleSet<LineStyle>) createStyleSet(10, createLineStyle(Color.GREEN, 0.01f, 0.01f, null));
//						StyleSet<PolygonStyle> pos = (StyleSet<PolygonStyle>) createStyleSet(10, createPolygonStyle(Color.BLUE, createLineStyle(Color.BLACK, 0.01f, 0.01f, null)));
//						mapView.addShapeLayer(editText.getText().toString(), fm.getSelectedFile().getPath(), ps, ls, pos);
//						fm.setSelectedFile(null);
//						redrawLayers();
//					}
//				} catch (Exception e) {
//					showErrorDialog(e.getMessage());
//				}
//			}
//	        
//	    });
//		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//	        public void onClick(DialogInterface dialog, int id) {
//	           // ignore
//	        }
//	    });
//		
//		builder.create().show();
//	}
	
	private void addSpatialLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add spatial layer:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Spatial layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		layout.addView(editText);
		
		TextView tableTextView = new TextView(this.getContext());
		tableTextView.setText("Spatial table name:");
		layout.addView(tableTextView);
		tableNameSpinner = new Spinner(this.getContext());
		tableNameSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				try {
					String tableName = (String) tableNameSpinner.getAdapter().getItem(index);
					setLabelSpinner(tableName);
				} catch (Exception e) {
					FLog.e("error getting table columns", e);
					showErrorDialog("Error getting table columns");
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		layout.addView(tableNameSpinner);
		
		TextView labelTextView = new TextView(this.getContext());
		labelTextView.setText("Spatial label column:");
		layout.addView(labelTextView);
		labelColumnSpinner = new Spinner(this.getContext());
		
		layout.addView(labelColumnSpinner);
		
		Button browserButton = new Button(getContext());
		browserButton.setText("browse");
		browserButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showFileBrowser(ShowProjectActivity.SPATIAL_FILE_BROWSER_REQUEST_CODE);
			}
		});
		layout.addView(browserButton);
		selectedFileText = new TextView(this.getContext());
		layout.addView(selectedFileText);
		
		LinearLayout styleLayout = new LinearLayout(this.getContext());
		styleLayout.setOrientation(LinearLayout.HORIZONTAL);
		styleLayout.addView(createPointStyleButton());
		styleLayout.addView(createLineStyleButton());
		styleLayout.addView(createPolygonStyleButton());
		styleLayout.addView(createTextStyleButton());
		
		layout.addView(styleLayout);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					if(spatialFile != null){
						String layerName = editText.getText() != null ? editText.getText().toString() : null;
						String tableName = tableNameSpinner.getSelectedItem() != null ? (String) tableNameSpinner.getSelectedItem() : null;
						String labelName = labelColumnSpinner.getSelectedItem() != null ? (String) labelColumnSpinner.getSelectedItem() : null;
						mapView.addSpatialLayer(layerName, spatialFile.getPath(), tableName, new String[] { labelName }, 
								pointStyle.toPointStyleSet(), lineStyle.toLineStyleSet(), polygonStyle.toPolygonStyleSet(), 
								textStyle.toStyleSet());
						redrawLayers();
					}
				} catch (Exception e) {
					showErrorDialog(e.getMessage());
				}
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		builder.create().show();
	}
	
	private void addDatabaseLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add database layer:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Database layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		layout.addView(editText);
		
		TextView typeTextView = new TextView(this.getContext());
		typeTextView.setText("Database layer type:");
		layout.addView(typeTextView);
		final Spinner typeSpinner = new Spinner(this.getContext());
		ArrayList<String> types = new ArrayList<String>();
		types.add("Entity");
		types.add("Relationship");
		typeSpinner.setAdapter(new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_dropdown_item, types));
		layout.addView(typeSpinner);
		
		TextView queryTextView = new TextView(this.getContext());
		queryTextView.setText("Database query:");
		layout.addView(queryTextView);
		final Spinner querySpinner = new Spinner(this.getContext());
		List<String> queryNames = mapView.getDatabaseLayerQueryNames();
		querySpinner.setAdapter(new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_dropdown_item, queryNames));
		layout.addView(querySpinner);
		
		LinearLayout styleLayout = new LinearLayout(this.getContext());
		styleLayout.setOrientation(LinearLayout.HORIZONTAL);
		styleLayout.addView(createPointStyleButton());
		styleLayout.addView(createLineStyleButton());
		styleLayout.addView(createPolygonStyleButton());
		styleLayout.addView(createTextStyleButton());
		
		layout.addView(styleLayout);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					String layerName = editText.getText() != null ? editText.getText().toString() : null;
					String type = typeSpinner.getSelectedItem() != null ? (String) typeSpinner.getSelectedItem() : null;
					String query = querySpinner.getSelectedItem() != null ? (String) querySpinner.getSelectedItem() : null;
					mapView.addDatabaseLayer(layerName, "Entity".equals(type), query, mapView.getDatabaseLayerQuery(query), 
							pointStyle.toPointStyleSet(), lineStyle.toLineStyleSet(), polygonStyle.toPolygonStyleSet(), 
							textStyle.toStyleSet());
					redrawLayers();
				} catch (Exception e) {
					showErrorDialog(e.getMessage());
				}
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		builder.create().show();
	}
	
	public Button createPointStyleButton(){
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Point");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				PointStyleDialog.Builder builder = new PointStyleDialog.Builder(LayerManagerView.this.getContext(), pointStyle);
				pointStyleDialog = (PointStyleDialog) builder.create();
				pointStyleDialog.show();
			}
				
		});
		return button;
	}
	
	public Button createLineStyleButton(){
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Line");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				LineStyleDialog.Builder builder = new LineStyleDialog.Builder(LayerManagerView.this.getContext(), lineStyle);
				lineStyleDialog = (LineStyleDialog) builder.create();
				lineStyleDialog.show();
			}
				
		});
		return button;
	}

	public Button createPolygonStyleButton(){
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Polygon");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				PolygonStyleDialog.Builder builder = new PolygonStyleDialog.Builder(LayerManagerView.this.getContext(), polygonStyle);
				polygonStyleDialog = (PolygonStyleDialog) builder.create();
				polygonStyleDialog.show();
			}
				
		});
		return button;
	}
	
	public Button createTextStyleButton(){
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Text");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				TextStyleDialog.Builder builder = new TextStyleDialog.Builder(LayerManagerView.this.getContext(), textStyle);
				textStyleDialog = (TextStyleDialog) builder.create();
				textStyleDialog.show();
			}
				
		});
		return button;
	}
	
	private void createLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Enter layer name:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		builder.setView(scrollView);
		
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		layout.addView(editText);
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					mapView.addCanvasLayer(editText.getText().toString());
					redrawLayers();
				} catch (Exception e) {
					showErrorDialog(e.getMessage());
				}
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		builder.create().show();
	}

	private void removeLayer(final Layer layer) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Do you want to delete layer?");
		
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					mapView.removeLayer(layer);
					redrawLayers();
				} catch (Exception e) {
					showErrorDialog(e.getMessage());
				}
			}
	        
	    });
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		builder.create().show();
	}

	private void renameLayer(final Layer layer) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Enter layer name:");
		
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		if(layer instanceof CustomGdalMapLayer){
			CustomGdalMapLayer gdalMapLayer = (CustomGdalMapLayer) layer;
			editText.setText(gdalMapLayer.getName());
		}else if(layer instanceof CustomSpatialiteLayer){
			CustomSpatialiteLayer spatialiteLayer = (CustomSpatialiteLayer) layer;
			editText.setText(spatialiteLayer.getName());
		}else if(layer instanceof CanvasLayer){
			CanvasLayer canvasLayer = (CanvasLayer) layer;
			editText.setText(canvasLayer.getName());
		}else if (layer instanceof DatabaseLayer) {
			DatabaseLayer databaseLayer = (DatabaseLayer) layer;
			editText.setText(databaseLayer.getName());
		}
		builder.setView(editText);
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					mapView.renameLayer(layer, editText.getText().toString());
					redrawLayers();
				} catch (Exception e) {
					showErrorDialog(e.getMessage());
				}
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		builder.create().show();
	}
	
	private void showMetadata(Layer layer) {
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);

		TextView layerTypeTextView = new TextView(this.getContext());
		layerTypeTextView.setText("Layer type:");
		layout.addView(layerTypeTextView);

		EditText layerTypeEditText = new EditText(LayerManagerView.this.getContext());
		layerTypeEditText.setEnabled(false);
		if(layer instanceof CustomGdalMapLayer){
			layerTypeEditText.setText("raster layer");
		}else if(layer instanceof CustomSpatialiteLayer){
			layerTypeEditText.setText("spatial layer");
		}else if(layer instanceof CanvasLayer){
			layerTypeEditText.setText("canvas layer");
		}else if (layer instanceof DatabaseLayer) {
			layerTypeEditText.setText("database layer");
		}
		layout.addView(layerTypeEditText);
		
		TextView layerNameTextView = new TextView(this.getContext());
		layerNameTextView.setText("Layer name:");
		layout.addView(layerNameTextView);

		if(layer instanceof CustomGdalMapLayer){
			CustomGdalMapLayer gdalMapLayer = (CustomGdalMapLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerView.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(gdalMapLayer.getName());
			layout.addView(layerNameEditText);

			TextView fileNameTextView = new TextView(this.getContext());
			fileNameTextView.setText("File name:");
			layout.addView(fileNameTextView);

			File file = new File(gdalMapLayer.getGdalSource());
			EditText fileNameEditText = new EditText(LayerManagerView.this.getContext());
			fileNameEditText.setEnabled(false);
			fileNameEditText.setText(file.getName());
			layout.addView(fileNameEditText);

			TextView fileSizeTextView = new TextView(this.getContext());
			fileSizeTextView.setText("File size:");
			layout.addView(fileSizeTextView);

			EditText fileSizeEditText = new EditText(LayerManagerView.this.getContext());
			fileSizeEditText.setEnabled(false);
			fileSizeEditText.setText(file.length()/(1024 * 1024) + " MB");
			layout.addView(fileSizeEditText);

			double[][] originalBounds = gdalMapLayer.getBoundaries();
	        TextView upperLeftTextView = new TextView(this.getContext());
	        upperLeftTextView.setText("Upper left boundary:");
			layout.addView(upperLeftTextView);

			EditText upperLeftEditText = new EditText(LayerManagerView.this.getContext());
			upperLeftEditText.setEnabled(false);
			upperLeftEditText.setText(originalBounds[0][0] + "," + originalBounds[0][1]);
			layout.addView(upperLeftEditText);

			TextView bottomRightTextView = new TextView(this.getContext());
			bottomRightTextView.setText("Bottom right boundary:");
			layout.addView(bottomRightTextView);

			EditText bottomRightEditText = new EditText(LayerManagerView.this.getContext());
			bottomRightEditText.setEnabled(false);
			bottomRightEditText.setText(originalBounds[3][0] + "," + originalBounds[3][1]);
			layout.addView(bottomRightEditText);

		}else if(layer instanceof CustomSpatialiteLayer){
			CustomSpatialiteLayer spatialiteLayer = (CustomSpatialiteLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerView.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(spatialiteLayer.getName());
			layout.addView(layerNameEditText);

			TextView fileNameTextView = new TextView(this.getContext());
			fileNameTextView.setText("File name:");
			layout.addView(fileNameTextView);

			File file = new File(spatialiteLayer.getDbPath());
			EditText fileNameEditText = new EditText(LayerManagerView.this.getContext());
			fileNameEditText.setEnabled(false);
			fileNameEditText.setText(file.getName());
			layout.addView(fileNameEditText);

			TextView fileSizeTextView = new TextView(this.getContext());
			fileSizeTextView.setText("File size:");
			layout.addView(fileSizeTextView);

			EditText fileSizeEditText = new EditText(LayerManagerView.this.getContext());
			fileSizeEditText.setEnabled(false);
			fileSizeEditText.setText(file.length()/(1024 * 1024) + " MB");
			layout.addView(fileSizeEditText);

			TextView tableNameTextView = new TextView(this.getContext());
			tableNameTextView.setText("Table name:");
			layout.addView(tableNameTextView);

			EditText tableNameEditText = new EditText(LayerManagerView.this.getContext());
			tableNameEditText.setEnabled(false);
			tableNameEditText.setText(spatialiteLayer.getTableName());
			layout.addView(tableNameEditText);

		}else if(layer instanceof CanvasLayer){
			CanvasLayer canvasLayer = (CanvasLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerView.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(canvasLayer.getName());
			layout.addView(layerNameEditText);
		}else if(layer instanceof DatabaseLayer){
			DatabaseLayer databaseLayer = (DatabaseLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerView.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(databaseLayer.getName());
			layout.addView(layerNameEditText);
			
			TextView tableNameTextView = new TextView(this.getContext());
			tableNameTextView.setText("Query name:");
			layout.addView(tableNameTextView);

			EditText tableNameEditText = new EditText(LayerManagerView.this.getContext());
			tableNameEditText.setEnabled(false);
			tableNameEditText.setText(databaseLayer.getQueryName());
			layout.addView(tableNameEditText);
		}else{
			showErrorDialog("wrong type of layer");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		builder.setTitle("Layer Metadata");
		builder.setView(scrollView);
		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

			}
	        
	    });
		
		builder.create().show();
	}

	private void showErrorDialog(String message) {
		new ErrorDialog(LayerManagerView.this.getContext(), "Layer Manager Error", message).show();
	}
	
	private void showFileBrowser(int requestCode){
		((ShowProjectActivity) this.getContext()).showFileBrowser(requestCode);
	}
	
	public void setTableSpinner() throws jsqlite.Exception{
			synchronized(DatabaseManager.class) {
				List<String> tableName = new ArrayList<String>();
				Stmt st = null;
				Database db = null;
				try {
					db = new jsqlite.Database();
					db.open(spatialFile.getPath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
					
					String query = "select name from sqlite_master where type = 'table' and sql like '%\"Geometry\"%';";
					st = db.prepare(query);
					
					while(st.step()){
						tableName.add(st.column_string(0));
					}
					st.close();
					st = null;
				} finally {
					try {
						if (st != null) st.close();
					} catch(Exception e) {
						FLog.e("error closing statement", e);
					}
					try {
						if (db != null) {
							db.close();
							db = null;
						}
					} catch (Exception e) {
						FLog.e("error closing database", e);
					}
				}
				if(tableName.isEmpty()){
					throw new jsqlite.Exception("Not tables found");
				}else{
					ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
							this.getContext(),
							android.R.layout.simple_spinner_dropdown_item,
							tableName);
					tableNameSpinner.setAdapter(arrayAdapter);
					tableNameSpinner.setSelection(0);
				}
			}
	}
	
	public void setLabelSpinner(String tableName) throws jsqlite.Exception{
		synchronized(DatabaseManager.class) {
			List<String> columnNames = new ArrayList<String>();
			Stmt st = null;
			Database db = null;
			try {
				db = new jsqlite.Database();
				db.open(spatialFile.getPath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				String query = "pragma table_info(" + tableName + ")";
				st = db.prepare(query);
				
				while(st.step()){
					columnNames.add(st.column_string(1));
				}
				
				st.close();
				st = null;
			} finally {
				try {
					if (st != null) st.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
			if(columnNames.isEmpty()){
				throw new jsqlite.Exception("Not labels found");
			}else{
				ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
						this.getContext(),
						android.R.layout.simple_spinner_dropdown_item,
						columnNames);
				labelColumnSpinner.setAdapter(arrayAdapter);
				labelColumnSpinner.setSelection(0);
			}
		}
}
}
