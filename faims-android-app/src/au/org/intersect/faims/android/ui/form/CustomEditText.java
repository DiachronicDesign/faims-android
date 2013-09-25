package au.org.intersect.faims.android.ui.form;

import java.util.List;

import android.content.Context;
import android.widget.EditText;

public class CustomEditText extends EditText implements ICustomView {

	private String attributeName;
	private String attributeType;
	private String ref;
	private String currentValue;
	private float certainty;
	private float currentCertainty;
	private String annotation;
	private String currentAnnotation;
	private boolean dirty;
	private String dirtyReason;
	
	public CustomEditText(Context context) {
		super(context);
	}
	
	public CustomEditText(Context context, String attributeName, String attributeType, String ref) {
		super(context);
		this.attributeName = attributeName;
		this.attributeType = attributeType;
		this.ref = ref;
		reset();
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
	
	public String getValue() {
		return getText().toString();
	}
	
	public void setValue(String value) {
		setText(value);
	}

	public float getCertainty() {
		return certainty;
	}

	public void setCertainty(float certainty) {
		this.certainty = certainty;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	public String getDirtyReason() {
		return dirtyReason;
	}
	
	public void setDirtyReason(String reason) {
		this.dirtyReason = reason;
	}

	public void reset() {
		setValue("");
		setCertainty(1);
		setAnnotation("");
		save();
	}

	public boolean hasChanges() {
		return !Compare.equal(getValue(), currentValue) || 
				!Compare.equal(getAnnotation(), currentAnnotation) || 
				!Compare.equal(getCertainty(), currentCertainty);
	}

	@Override
	public void save() {
		currentValue = getValue();
		currentCertainty = getCertainty();
		currentAnnotation = getAnnotation();
	}

	@Override
	public List<?> getValues() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setValues(List<?> values) {
		// TODO Auto-generated method stub
		
	}
}
