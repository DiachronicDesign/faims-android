package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.EditText;

public class CustomEditText extends EditText {

	private String archEntName;
	private String archEntType;
	private String relName;
	private String relType;
	private String ref;
	
	public CustomEditText(Context context) {
		super(context);
	}
	
	public CustomEditText(Context context, String archEntName, String archEntType, String relName, String relType, String ref) {
		super(context);
		this.archEntName = archEntName;
		this.archEntType = archEntType;
		this.relName = relName;
		this.relType = relType;
		this.ref = ref;
	}

	public String getArchEntName() {
		return archEntName;
	}

	public String getArchEntType() {
		return archEntType;
	}

	public String getRelName() {
		return relName;
	}

	public String getRelType() {
		return relType;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}
}
