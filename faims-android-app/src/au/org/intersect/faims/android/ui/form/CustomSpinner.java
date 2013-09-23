package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.Spinner;

public class CustomSpinner extends Spinner {

	private String attributeName;
	private String attributeType;
	private String ref;
	private float certainty = 1;
	private float currentCertainty = 1;
	private String annotation = "";
	private String currentAnnotation = "";
	private boolean dirty;
	private String dirtyReason;
	
	public CustomSpinner(Context context) {
		super(context);
	}
	
	public CustomSpinner(Context context, String attributeName, String attributeType, String ref) {
		super(context);
		this.attributeName = attributeName;
		this.attributeType = attributeType;
		this.ref = ref;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public String getAttributeType() {
		return attributeType;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public float getCertainty() {
		return certainty;
	}

	public void setCertainty(float certainty) {
		this.certainty = certainty;
	}

	public float getCurrentCertainty() {
		return currentCertainty;
	}

	public void setCurrentCertainty(float currentCertainty) {
		this.currentCertainty = currentCertainty;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public String getCurrentAnnotation() {
		return currentAnnotation;
	}

	public void setCurrentAnnotation(String currentAnnotation) {
		this.currentAnnotation = currentAnnotation;
	}

	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean value) {
		this.dirty = value;
	}
	
	public void setDirtyReason(String value) {
		this.dirtyReason = value;
	}

	public String getDirtyReason() {
		return dirtyReason;
	}

	public void setValue(String value) {
		for (int i = 0; i < getAdapter().getCount(); ++i) {
			NameValuePair pair = (NameValuePair) getItemAtPosition(i);
			if (value.equalsIgnoreCase(pair.getValue())) {
				setSelection(i);
				break;
			}
		}
	}

	public String getValue() {
		NameValuePair pair = (NameValuePair) getSelectedItem();
		if (pair == null) return "";
		return pair.getValue();
	}
	
	public void reset() {
		setSelection(0);
	}
}
