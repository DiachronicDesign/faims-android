package au.org.intersect.faims.android.ui.form;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import au.org.intersect.faims.android.ui.form.styling.FaimsStyling;
import au.org.intersect.faims.android.ui.form.styling.StyleUtils;

public class CustomButton extends Button implements FaimsStyling{

	public CustomButton(Context context) {
		super(context);
	}
	
	public CustomButton(Context context, List<Map<String, String>> styleMappings) {
		super(context);
		applyStyle(styleMappings);
	}

	@Override
	public void applyStyle(List<Map<String, String>> styleMappings) {
		if(!styleMappings.isEmpty()){
			LayoutParams layoutParams = this.getLayoutParams() != null ? (LayoutParams) this.getLayoutParams() : 
				new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			for(Map<String, String> styles : styleMappings){
				if(!styles.isEmpty()){
					for (Entry<String, String> attribute : styles.entrySet()) {
						if ("layout_width".equals(attribute.getKey())) {
							layoutParams.width = StyleUtils.getLayoutParamsValue(attribute.getValue());
						} else if ("layout_height".equals(attribute.getKey())) {
							layoutParams.height = StyleUtils.getLayoutParamsValue(attribute.getValue());
						} else if ("layout_weight".equals(attribute.getKey())) {
							layoutParams.weight = StyleUtils.getLayoutParamsValue(attribute.getValue());
						}
					}
					this.setLayoutParams(layoutParams);
				}
			}
		}
	}

}
