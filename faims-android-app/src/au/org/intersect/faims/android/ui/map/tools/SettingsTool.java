package au.org.intersect.faims.android.ui.map.tools;

import java.util.Locale;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

public abstract class SettingsTool extends MapTool {
	
	protected LinearLayout layout;
	
	protected MapButton settingsButton;
	
	public SettingsTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		settingsButton = createSettingsButton(context);
		
		updateLayout();
	}
	
	protected void updateLayout() {
		layout.removeAllViews();
		layout.addView(settingsButton);
	}

	@Override
	public View getUI() {
		return layout;
	}
	
	protected int parseColor(String value) throws Exception {
		Integer color = (int) Long.parseLong(value, 16);
		if (color == null) {
			throw new MapException("Invalid color specified");
		}
		return color;
	}
	
	protected float parseSize(int value) throws Exception {
		if (value < 0 || value > 100) {
			throw new MapException("Invalid size");
		}
		
		return ((float) value) / 100;
	}
	
	protected EditText addSetter(Context context, LinearLayout layout, String labelText, String defaultValue) {
		return addSetter(context, layout, labelText, defaultValue, -1);
	}
	
	protected EditText addSetter(Context context, LinearLayout layout, String labelText, String defaultValue, int type) {
		TextView label = new TextView(context);
		label.setText(labelText);
		
		EditText text = new EditText(context);
		text.setText(defaultValue.toUpperCase(Locale.ENGLISH));
		
		if (type >= 0) text.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
		layout.addView(label);
		layout.addView(text);
		
		return text;
	}
	
	protected SeekBar addSlider(Context context, LinearLayout layout, final String labelText, float defaultValue) {
		final TextView label = new TextView(context);
		label.setText(labelText + " " + Float.toString(defaultValue));
		
		final SeekBar seekBar = new SeekBar(context);
		seekBar.setMax(100);
		seekBar.setProgress((int) (defaultValue * 100));
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				label.setText(labelText + " " + Float.toString(((float) seekBar.getProgress()) / 100));
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		layout.addView(label);
		layout.addView(seekBar);
		
		return seekBar;
	}
	
	protected abstract MapButton createSettingsButton(final Context context);

}
