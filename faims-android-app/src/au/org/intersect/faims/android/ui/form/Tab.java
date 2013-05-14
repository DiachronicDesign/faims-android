package au.org.intersect.faims.android.ui.form;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.SelectChoice;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputType;
import android.text.format.Time;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.MapLayout;
import au.org.intersect.faims.android.ui.map.tools.MapTool;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

public class Tab implements Parcelable{

	private Context context;
	private ScrollView scrollView;
	private LinearLayout linearLayout;
	private Map<String, String> viewReference;
	private Map<String, List<View>> viewMap;
	private Map<String, Object> valueReference;
	private List<View> viewList;
	private List<CustomMapView> mapViewList;
	private String name;
	private String label;
	private boolean hidden;
	//private boolean scrollable;
	private View view;
	private Arch16n arch16n;
	private String reference;
	private static final String FREETEXT = "freetext";

	public Tab(Parcel source){
		hidden = source.readBundle().getBoolean("hidden");
		reference = source.readString();
	}
	
	public Tab(Context context, String name, String label, boolean hidden, boolean scrollable, Arch16n arch16n, String reference) {
		this.context = context;
		this.name = name;
		this.arch16n = arch16n;
		label = this.arch16n.substituteValue(label);
		this.label = label;
		this.hidden = hidden;
		this.reference = reference;
		//this.scrollable = scrollable;
		
		this.linearLayout = new LinearLayout(context);
		this.viewReference = new HashMap<String, String>();
		this.valueReference = new HashMap<String, Object>();
		this.viewMap = new HashMap<String, List<View>>();
		this.viewList = new ArrayList<View>();
		this.mapViewList = new ArrayList<CustomMapView>();
        linearLayout.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        
        linearLayout.setBackgroundColor(Color.WHITE);
		
        if (scrollable) {
        	this.scrollView = new ScrollView(this.context);
        	scrollView.addView(linearLayout);
        	this.view = scrollView;
        } else {
        	this.view = linearLayout;
        }
	}

	public static final Parcelable.Creator<Tab> CREATOR = new Parcelable.Creator<Tab>() {
		public Tab createFromParcel(Parcel source) {
			return new Tab(source);
		}

		public Tab[] newArray(int size) {
			return new Tab[size];
		}
	};

	public View addInput(FormAttribute attribute, String ref, String viewName, String directory, boolean isArchEnt, boolean isRelationship) {
    	Button certaintyButton = null;
    	Button annotationButton = null;
    	
		if (attribute.controlType != Constants.CONTROL_TRIGGER) {
			LinearLayout fieldLinearLayout = new LinearLayout(this.context);
	    	fieldLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
	    	
            TextView textView = createLabel(attribute);
            fieldLinearLayout.addView(textView);
            linearLayout.addView(fieldLinearLayout);
                
    		if(attribute.certainty && (isArchEnt || isRelationship)){
    			certaintyButton = createCertaintyButton();
    			fieldLinearLayout.addView(certaintyButton);
    		}
    		
    		if(attribute.annotation && !FREETEXT.equals(attribute.type)){
    			annotationButton = createAnnotationButton();
    			fieldLinearLayout.addView(annotationButton);
    		}
        }
		
		viewReference.put(viewName, ref);
		View view = null;
		
		// check the control type to know the type of the question
        switch (attribute.controlType) {
            case Constants.CONTROL_INPUT:
            	switch (attribute.dataType) {
	                case Constants.DATATYPE_INTEGER:
	                	view = createIntegerTextField(attribute, ref);
	                	setupView(view, certaintyButton, annotationButton, ref);
	                    break;
	                case Constants.DATATYPE_DECIMAL:
	                	view = createDecimalTextField(attribute, ref);
	                	setupView(view, certaintyButton, annotationButton, ref);
	                    break;
	                case Constants.DATATYPE_LONG:
	                	view = createLongTextField(attribute, ref);
	                	setupView(view, certaintyButton, annotationButton, ref);
	                    break;
	                // set input type as date picker
	                case Constants.DATATYPE_DATE:
	                	view = createDatePicker(attribute, ref);
	                	setupView(view, certaintyButton, annotationButton, ref, DateUtil.getDate((CustomDatePicker) view));
	                    break;
	                // get the text area
	                case Constants.DATATYPE_TEXT:
	                	view = createTextArea(attribute, ref);
	                	setupView(view, certaintyButton, annotationButton, ref);
	                    break;
	                // set input type as time picker
	                case Constants.DATATYPE_TIME:
	                	view = createTimePicker(attribute, ref);
	    				setupView(view, certaintyButton, annotationButton, ref, DateUtil.getTime((CustomTimePicker) view));
	                    break;
	                // default is edit text
	                default:
	                	// check if map type
	                	if (attribute.map) {
	                		MapLayout mapLayout = new MapLayout(this.context);
	                		final CustomMapView mapView = mapLayout.getMapView();
	                		
	                		Button layerButton = createLayerButton();
	                		linearLayout.addView(layerButton);
	                		
	                		Spinner toolsDropDown = createToolsDropDown(mapView);
	                		linearLayout.addView(toolsDropDown);
	                		
	                		layerButton.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View arg0) {
									mapView.showLayersDialog();
								}
	                			
	                		});
	                		
	                		toolsDropDown.setOnItemSelectedListener(new OnItemSelectedListener() {

								@Override
								public void onItemSelected(AdapterView<?> arg0,
										View arg1, int arg2, long arg3) {
									mapView.selectToolIndex(arg2);
								}

								@Override
								public void onNothingSelected(
										AdapterView<?> arg0) {
									// ignore
								}
	                			
	                		});
	                		
	                		linearLayout.addView(mapLayout);
	                		
	                		mapView.selectDefaultTool();
	                		
	                		mapViewList.add(mapView);
	                		view = mapView;
	                	} else {
	                		view = createTextField(-1, attribute, ref);
	                		setupView(view, certaintyButton, annotationButton, ref);
	                	}
	                    break;
            	}
                break;
            // uploading image by using camera
            /*
            case Constants.CONTROL_IMAGE_CHOOSE:
                Button imageButton = new Button(this.context);
                imageButton.setText("Choose Image");

                final ImageView imageView = new ImageView(this.context);
                imageButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent cameraIntent = new Intent(
                                MediaStore.ACTION_IMAGE_CAPTURE);
                        UIRenderer.this.imageView = imageView;
                        ((ShowProjectActivity) UIRenderer.this.context)
                                .startActivityForResult(cameraIntent,
                                        ShowProjectActivity.CAMERA_REQUEST_CODE);
                    }
                });
               
                layout.addView(imageButton);
                layout.addView(imageView);
                break;
            */
            // create control for select one showing it as drop down
            case Constants.CONTROL_SELECT_ONE:
                switch (attribute.dataType) {
                    case Constants.DATATYPE_CHOICE:
                    	// check if the type if image to create image slider
                        if ("image".equalsIgnoreCase(attribute.questionType)) {
                            view = renderImageSliderForSingleSelection(attribute, directory, ref);
                            setupView(view, certaintyButton, annotationButton, ref);
                        }
                        // Radio Button
                        else if ("full".equalsIgnoreCase(attribute.questionAppearance)) {
                        	view = createRadioGroup(attribute, ref);
                        	setupView(view, certaintyButton, annotationButton, ref);
                        // List
                        } else if ("compact".equalsIgnoreCase(attribute.questionAppearance) ) {
                        	view = createList(attribute);
                            linearLayout.addView(view); // TODO does this need certainty and annotation buttons
                        // Default is single select dropdown
                        } else {
                        	view = createDropDown(attribute, ref);
                        	NameValuePair pair = (NameValuePair) ((CustomSpinner) view).getSelectedItem();
                        	setupView(view, certaintyButton, annotationButton, ref, pair.getValue());
                        }
                        break;
                }
                break;
            // create control for multi select, showing it as checkbox
            case Constants.CONTROL_SELECT_MULTI:
                switch (attribute.dataType) {
                    case Constants.DATATYPE_CHOICE_LIST:
                    	view = createCheckListGroup(attribute, ref);
                    	setupView(view, certaintyButton, annotationButton, ref, new ArrayList<NameValuePair>());
                }
                break;
            // create control for trigger showing as a button
            case Constants.CONTROL_TRIGGER:
                view = createTrigger(attribute);
                linearLayout.addView(view);
                break;
        }
        
        if(attribute.name != null){
        	addViewMappings(attribute.name, view);
        }
        
        return view;
	}
	
	private TextView createLabel(FormAttribute attribute) {
		TextView textView = new TextView(this.context);
        String inputText = attribute.questionText;
        inputText = arch16n.substituteValue(inputText);
        textView.setText(inputText);
        return textView;
	}
	
	private Button createCertaintyButton() {
		Button button = new Button(this.context);
		button.setBackgroundResource(R.drawable.square_button);
		int size = (int) ScaleUtil.getDip(context, 30);
		LayoutParams layoutParams = new LayoutParams(size, size);
		layoutParams.topMargin = 10;
		button.setLayoutParams(layoutParams);
		button.setText("C");
		button.setTextSize(10);
		return button;
	}
	
	private Button createAnnotationButton() {
		Button button = new Button(this.context);
		button.setBackgroundResource(R.drawable.square_button);
		int size = (int) ScaleUtil.getDip(context, 30);
		LayoutParams layoutParams = new LayoutParams(size, size);
		layoutParams.topMargin = 10;
		button.setLayoutParams(layoutParams);
		button.setText("A");
		button.setTextSize(10);
		return button;
	}
	
	private void setupView(View view, Button certaintyButton, Button annotationButton, String ref) {
		setupView(view, certaintyButton, annotationButton, ref, "");
	}
	
	private void setupView(View view, Button certaintyButton, Button annotationButton, String ref, Object value) {
		if (certaintyButton != null) onCertaintyButtonClicked(certaintyButton, view);
        if (annotationButton != null) onAnnotationButtonClicked(annotationButton, view);
        linearLayout.addView(view);
        valueReference.put(ref, value);
	}
	
	private CustomEditText createTextField(int type, FormAttribute attribute, String ref) {
		CustomEditText text = new CustomEditText(this.context, attribute.name, attribute.type, ref);
    	if (attribute.readOnly) {
    		text.setEnabled(false);
    	}
    	if (type >= 0) text.setInputType(type);
    	return text;
	}
	
	private CustomEditText createIntegerTextField(FormAttribute attribute, String ref) {
    	return createTextField(InputType.TYPE_CLASS_NUMBER, attribute, ref);
	}
	
	private CustomEditText createDecimalTextField(FormAttribute attribute, String ref) {
        return createTextField(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, attribute, ref);
	}
	
	private CustomEditText createLongTextField(FormAttribute attribute, String ref) {
        return createTextField(InputType.TYPE_CLASS_NUMBER, attribute, ref);
	}
	
	private CustomDatePicker createDatePicker(FormAttribute attribute, String ref) {
		CustomDatePicker date = new CustomDatePicker(this.context, attribute.name, attribute.type, ref);
    	Time now = new Time();
		now.setToNow();
		date.updateDate(now.year, now.month, now.monthDay);
		if (attribute.readOnly) {
    		date.setEnabled(false);
    	}
    	return date;
	}
	
	private CustomEditText createTextArea(FormAttribute attribute, String ref) {
		CustomEditText text = new CustomEditText(this.context, attribute.name, attribute.type, ref);
    	if (attribute.readOnly) {
    		text.setEnabled(false);
    	}
    	text.setLines(5);
    	return text;
	}
	
	private CustomTimePicker createTimePicker(FormAttribute attribute, String ref) {
		CustomTimePicker time = new CustomTimePicker(this.context, attribute.name, attribute.type, ref);
    	Time timeNow = new Time();
        timeNow.setToNow();
		time.setCurrentHour(timeNow.hour);
		time.setCurrentMinute(timeNow.minute);
		if (attribute.readOnly) {
    		time.setEnabled(false);
    	}
		return time;
	}
	
	private CustomLinearLayout createRadioGroup(FormAttribute attribute, String ref) {
		CustomLinearLayout selectLayout = new CustomLinearLayout(this.context, attribute.name, attribute.type, ref);
        selectLayout.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        selectLayout.setOrientation(LinearLayout.VERTICAL);
        RadioGroup radioGroupLayout = new RadioGroup(this.context);
        radioGroupLayout.setOrientation(LinearLayout.HORIZONTAL);
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	CustomRadioButton radioButton = new CustomRadioButton(this.context);
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
            radioButton.setText(innerText);
            radioButton.setValue(selectChoice.getValue());
            radioGroupLayout.addView(radioButton);
        }
        selectLayout.addView(radioGroupLayout);
        return selectLayout;
	}
	
	private CustomListView createList(FormAttribute attribute) {
		CustomListView list = new CustomListView(this.context);
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
            choices.add(pair);
        }
        ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                this.context,
                android.R.layout.simple_list_item_1,
                choices);
        list.setAdapter(arrayAdapter);
        return list;
	}
	
	private CustomSpinner createDropDown(FormAttribute attribute, String ref) {
		CustomSpinner spinner = new CustomSpinner(this.context, attribute.name, attribute.type, ref);
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
            choices.add(pair);
        }
        ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                this.context,
                android.R.layout.simple_spinner_dropdown_item,
                choices);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(0);
        return spinner;
	}
	
	private CustomLinearLayout createCheckListGroup(FormAttribute attribute, String ref) {
		CustomLinearLayout selectLayout = new CustomLinearLayout(
                this.context, attribute.name, attribute.type, ref);
        selectLayout.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        selectLayout.setOrientation(LinearLayout.VERTICAL);
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	CustomCheckBox checkBox = new CustomCheckBox(this.context);
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
            checkBox.setText(innerText);
            checkBox.setValue(selectChoice.getValue());
            selectLayout.addView(checkBox);
        }
        return selectLayout;
	}
	
	private Button createTrigger(FormAttribute attribute) {
		 Button button = new Button(this.context);
         String questionText = arch16n.substituteValue(attribute.questionText);
         button.setText(questionText);
         return button;
	}
	
	private Button createLayerButton() {
		 Button button = new Button(this.context);
         button.setText("Layers");
         return button;
	}
	
	private Spinner createToolsDropDown(CustomMapView mapView) {
		Spinner spinner = new Spinner(this.context);
		List<MapTool> tools = mapView.getTools();
		ArrayAdapter<MapTool> arrayAdapter = new ArrayAdapter<MapTool>(
				this.context,
				android.R.layout.simple_spinner_dropdown_item,
				tools);
		spinner.setAdapter(arrayAdapter);
		spinner.setSelection(0);
		return spinner;
	}

	private void onAnnotationButtonClicked(Button annotationButton, final View view) {
		annotationButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final EditText editText = new EditText(v.getContext());
				if (view instanceof CustomEditText){
	        		CustomEditText customEditText = (CustomEditText) view;
	        		editText.setText(customEditText.getCurrentAnnotation());
	        	}else if (view instanceof CustomLinearLayout){
	        		CustomLinearLayout customLinearLayout = (CustomLinearLayout) view;
	        		editText.setText(customLinearLayout.getCurrentAnnotation());
	        	}else if (view instanceof CustomHorizontalScrollView){
	        		CustomHorizontalScrollView customHorizontalScrollView = (CustomHorizontalScrollView) view;
	        		editText.setText(customHorizontalScrollView.getCurrentAnnotation());
	        	}else if (view instanceof CustomSpinner){
	        		CustomSpinner customSpinner = (CustomSpinner) view;
	        		editText.setText(customSpinner.getCurrentAnnotation());
	        	}
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				
				builder.setTitle("Annotation");
				builder.setMessage("Set the annotation text for the field");
				builder.setView(editText);
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				        	if (view instanceof CustomEditText){
				        		CustomEditText customEditText = (CustomEditText) view;
				        		customEditText.setCurrentAnnotation(editText.getText().toString());
				        	}else if (view instanceof CustomLinearLayout){
				        		CustomLinearLayout customLinearLayout = (CustomLinearLayout) view;
				        		customLinearLayout.setCurrentAnnotation(editText.getText().toString());
				        	}else if (view instanceof CustomHorizontalScrollView){
				        		CustomHorizontalScrollView customHorizontalScrollView = (CustomHorizontalScrollView) view;
				        		customHorizontalScrollView.setCurrentAnnotation(editText.getText().toString());
				        	}else if (view instanceof CustomSpinner){
				        		CustomSpinner customSpinner = (CustomSpinner) view;
				        		customSpinner.setCurrentAnnotation(editText.getText().toString());
				        	}
				        }
				    });
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				            // User cancelled the dialog
				        }
				    });
				
				builder.create().show();
			}
		});
	}

	private void onCertaintyButtonClicked(Button certaintyButton,final View view) {
		certaintyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				LinearLayout layout = new LinearLayout(v.getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				final SeekBar seekBar = new SeekBar(v.getContext());
				float certainty = 0;
				seekBar.setMax(100);
				seekBar.setMinimumWidth((int) ScaleUtil.getDip(Tab.this.context, 400));
				if (view instanceof CustomEditText){
	        		CustomEditText customEditText = (CustomEditText) view;
	        		certainty = customEditText.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomDatePicker){
	        		CustomDatePicker customDatePicker = (CustomDatePicker) view;
	        		certainty = customDatePicker.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomTimePicker){
	        		CustomTimePicker customTimePicker = (CustomTimePicker) view;
	        		certainty = customTimePicker.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomLinearLayout){
	        		CustomLinearLayout customLinearLayout = (CustomLinearLayout) view;
	        		certainty = customLinearLayout.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomHorizontalScrollView){
	        		CustomHorizontalScrollView customHorizontalScrollView = (CustomHorizontalScrollView) view;
	        		certainty = customHorizontalScrollView.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomSpinner){
	        		CustomSpinner customSpinner = (CustomSpinner) view;
	        		certainty = customSpinner.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}
				
				final TextView text = new TextView(v.getContext());
				text.setText("    Certainty: " + certainty);
				seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}
					
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}
					
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						text.setText("    Certainty: " + ((float) progress)/100);
					}
				});
				layout.addView(text);
				layout.addView(seekBar);
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				
				builder.setTitle("Certainty");
				builder.setMessage("Set the certainty value for the question");
				builder.setView(layout);
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				        	if (view instanceof CustomEditText){
				        		CustomEditText customEditText = (CustomEditText) view;
				        		customEditText.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomDatePicker){
				        		CustomDatePicker customDatePicker = (CustomDatePicker) view;
				        		customDatePicker.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomTimePicker){
				        		CustomTimePicker customTimePicker = (CustomTimePicker) view;
				        		customTimePicker.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomLinearLayout){
				        		CustomLinearLayout customLinearLayout = (CustomLinearLayout) view;
				        		customLinearLayout.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomHorizontalScrollView){
				        		CustomHorizontalScrollView customHorizontalScrollView = (CustomHorizontalScrollView) view;
				        		customHorizontalScrollView.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomSpinner){
				        		CustomSpinner customSpinner = (CustomSpinner) view;
				        		customSpinner.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}
				        }
				    });
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				            // User cancelled the dialog
				        }
				    });
				
				builder.create().show();
				
			}
		});
	}

	public TabSpec createTabSpec(TabHost tabHost) {
		TabSpec tabSpec = tabHost.newTabSpec(name);
		
		tabSpec.setContent(new TabContentFactory() {

            @Override
            public View createTabContent(String tag) {
            	return view;
            }
        });
        
        tabSpec.setIndicator(label);
        
		return tabSpec;
	}
	
	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}
	
	public boolean getHidden() {
		return hidden;
	}
	
	public void setHidden(boolean hidden){
		this.hidden = hidden;
	}

	public String getReference() {
		return reference;
	}

	public boolean hasView(String name){
		return this.viewMap.containsKey(name);
	}

	public String getPath(String viewName){
		return this.viewReference.get(viewName);
	}

	private void addViewMappings(String name, View view){
		if(this.viewMap.containsKey(name)){
			this.viewMap.get(name).add(view);
		}else{
			List<View> views = new ArrayList<View>();
			views.add(view);
			this.viewMap.put(name, views);
		}
		viewList.add(view);
	}
	
	public List<View> getAllChildrenViews(){
		List<View> views = new ArrayList<View>();
		for(int i = 0; i < linearLayout.getChildCount(); i++){
			views.add(linearLayout.getChildAt(i));
		}
		return views;
	}
	public List<View> getAllViews(){
		return this.viewList;
	}

	public List<View> getViews(String name) {
		return this.viewMap.get(name);
	}
	
	public Object getStoredValue(String ref){
		return this.valueReference.get(ref);
	}

	public Map<String, String> getViewReference() {
		return viewReference;
	}

	public void setValueReference(String ref, Object value){
		this.valueReference.put(ref, value);
	}

	/**
     * Rendering image slide for select one
     * 
     * @param layout
     * @param questionPrompt
	 * @param path2 
	 * @param attributeType 
	 * @param attributeName 
     */
    private CustomHorizontalScrollView renderImageSliderForSingleSelection(final FormAttribute attribute, String directory, String ref) {
    	final CustomHorizontalScrollView horizontalScrollView = new CustomHorizontalScrollView(this.context, attribute.name, attribute.type, ref);
        LinearLayout galleriesLayout = new LinearLayout(this.context);
        galleriesLayout.setOrientation(LinearLayout.HORIZONTAL);
        final List<CustomImageView> galleryImages = new ArrayList<CustomImageView>();
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	final String picturePath = Environment.getExternalStorageDirectory() + directory + "/" + selectChoice.getValue();
        	File pictureFolder = new File(picturePath);
        	if(pictureFolder.exists()){
	        	for(final String name : pictureFolder.list()){
	        		LinearLayout galleryLayout = new LinearLayout(this.context);
	        		galleryLayout.setOrientation(LinearLayout.VERTICAL);
	        		CustomImageView gallery = new CustomImageView(this.context);
	        		int size = (int) ScaleUtil.getDip(context, 400);
	        		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(size, size);
	                gallery.setImageURI(Uri.parse(ref+"/"+name));
	                gallery.setBackgroundColor(Color.RED);
	                gallery.setPadding(10, 10, 10, 10);
	                gallery.setLayoutParams(layoutParams);
	                gallery.setPicture(null);
	                gallery.setOnClickListener(new OnClickListener() {
	
	                    @Override
	                    public void onClick(View v) {
	                    	CustomImageView selectedImageView = (CustomImageView) v;
	                        horizontalScrollView.setSelectedImageView(selectedImageView);
	                        for (CustomImageView view : galleryImages) {
	                            if (view.equals(selectedImageView)) {
	                                view.setBackgroundColor(Color.GREEN);
	                            } else {
	                                view.setBackgroundColor(Color.RED);
	                            }
	                        }
	                    }
	                });
	                TextView textView = new TextView(this.context);
	                textView.setText(name);
	                textView.setGravity(Gravity.CENTER_HORIZONTAL);
	                textView.setTextSize(20);
	                galleryLayout.addView(textView);
	                galleryImages.add(gallery);
	                galleryLayout.addView(gallery);
	                galleriesLayout.addView(galleryLayout);
	        	}
	        	horizontalScrollView.setImageViews(galleryImages);
	        }
        }
        horizontalScrollView.addView(galleriesLayout);
        return horizontalScrollView;
    }

	/*
    public ImageView getCurrentImageView() {
        return this.imageView;
    }

    public void clearCurrentImageView() {
        this.imageView = null;
    }
    */

	public void clearViews() {
		for (View v : viewList) {
			if (v instanceof CustomEditText) {
				CustomEditText text = (CustomEditText) v;
				text.setText("");
				text.setCertainty(1);
				text.setAnnotation("");
				text.setCurrentCertainty(1);
				text.setCurrentAnnotation("");
				valueReference.put(text.getRef(), "");
			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker date = (CustomDatePicker) v;
				Time now = new Time();
				now.setToNow();
				date.updateDate(now.year, now.month, now.monthDay);
				date.setCertainty(1);
				date.setCurrentCertainty(1);
				valueReference.put(date.getRef(), DateUtil.getDate(date));
			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker time = (CustomTimePicker) v;
				Time now = new Time();
				now.setToNow();
				time.setCurrentHour(now.hour);
				time.setCurrentMinute(now.minute);
				time.setCertainty(1);
				time.setCurrentCertainty(1);
				valueReference.put(time.getRef(), DateUtil.getTime(time));
			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout layout = (CustomLinearLayout) v;
				layout.setCertainty(1);
				layout.setAnnotation("");
				layout.setCurrentCertainty(1);
				layout.setCurrentAnnotation("");
				View child0 = layout.getChildAt(0);
				
				if (child0 instanceof RadioGroup){
					RadioGroup rg = (RadioGroup) child0;
					rg.clearCheck();
					valueReference.put(layout.getRef(), "");
				}else if (child0 instanceof CheckBox){
					for(int i = 0; i < layout.getChildCount(); ++i){
						View view = layout.getChildAt(i);
						if (view instanceof CustomCheckBox){
							CustomCheckBox cb = (CustomCheckBox) view;
							cb.setChecked(false);
						}
					}
					valueReference.put(layout.getRef(), new ArrayList<NameValuePair>());
				}
			} else if (v instanceof CustomSpinner) {
				CustomSpinner spinner = (CustomSpinner) v;
				spinner.setSelection(0);
				spinner.setCertainty(1);
				spinner.setAnnotation("");
				spinner.setCurrentCertainty(1);
				spinner.setCurrentAnnotation("");
				NameValuePair pair = (NameValuePair) spinner.getSelectedItem();
				valueReference.put(spinner.getRef(), pair.getValue());
			} else if(v instanceof CustomHorizontalScrollView){
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) v;
				horizontalScrollView.setCertainty(1);
				horizontalScrollView.setAnnotation("");
				horizontalScrollView.setCurrentCertainty(1);
				horizontalScrollView.setCurrentAnnotation("");
				for(CustomImageView customImageView : horizontalScrollView.getImageViews()){
					customImageView.setBackgroundColor(Color.RED);
				}
				horizontalScrollView.setSelectedImageView(null);
			}
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Bundle tabBundle = new Bundle();
		tabBundle.putBoolean("hidden", hidden);
		dest.writeBundle(tabBundle);
		dest.writeString(reference);
	}

	public void onShowTab() {
		for (CustomMapView mapView : mapViewList) {
			mapView.restartThreads();
		}
	}

	public void onHideTab() {
		for (CustomMapView mapView : mapViewList) {
			mapView.killThreads();
		}
	}
	
	public List<CustomMapView> getMapViewList(){
		return mapViewList;
	}

}
