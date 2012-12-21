package au.org.intersect.faims.android.ui.activity;

import org.javarosa.form.api.FormEntryController;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.DialogType;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;
import au.org.intersect.faims.android.util.BeanShellLinker;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.UIRenderer;

public class ShowProjectActivity extends Activity implements IDialogListener {

	public static final int CAMERA_REQUEST_CODE = 1;

	private FormEntryController fem;

	private UIRenderer renderer;

	private ChoiceDialog choiceDialog;

	private String directory;
	
	private BeanShellLinker linker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FAIMSLog.log();
		
		setContentView(R.layout.activity_show_project);
		Intent data = getIntent();
		setTitle(data.getStringExtra("name"));
		directory = data.getStringExtra("directory");
		
		choiceDialog = DialogFactory.createChoiceDialog(ShowProjectActivity.this, 
				DialogType.CONFIRM_RENDER_PROJECT, 
				getString(R.string.render_project_title),
				getString(R.string.render_project_message));
		choiceDialog.show();
	}
	
	/*
	@Override
	protected void onResume() {
		super.onResume();
		FAIMSLog.log();
		this.manager.dispatchResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		FAIMSLog.log();
		this.manager.dispatchPause(isFinishing());
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		FAIMSLog.log();
		// after taking picture using camera
		if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			this.renderer.getCurrentImageView().setImageBitmap(photo);
			this.renderer.clearCurrentImageView();
		}
	}
	*/
	
	@Override
	public void handleDialogResponse(DialogResultCode resultCode, Object data,
			DialogType type, Dialog dialog) {
		if (type == DialogType.CONFIRM_RENDER_PROJECT) {
			if (resultCode == DialogResultCode.SELECT_YES) {
				renderUI();
			}
		}
		
	}
	
	private void renderUI() {
		// Read, validate and parse the xforms
		this.fem = FileUtil.readXmlContent(Environment
				.getExternalStorageDirectory() + directory + "/ui_schema.xml");

		// render the ui definition
		this.renderer = new UIRenderer(this.fem, this);
		this.renderer.createUI();
		this.renderer.showTabGroup(this, 0);
		
		// bind the logic to the ui
		linker = new BeanShellLinker(getAssets(), renderer);
		linker.source("ui_commands.bsh");
		linker.source("test_script.bsh");
	}
}
