package au.org.intersect.faims.android.util;

import android.app.Activity;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.ConfirmDialog;
import au.org.intersect.faims.android.ui.dialog.DialogTypes;
import au.org.intersect.faims.android.ui.dialog.DownloadDialog;

public class DialogFactory {
	
	public static ChoiceDialog createChoiceDialog(Activity activity, DialogTypes type, String title, String message) {
		return ChoiceDialog.create(activity, type, title, message);
	}
	
	public static ConfirmDialog createConfirmDialog(Activity activity, DialogTypes type, String title, String message) {
		return ConfirmDialog.create(activity, type, title, message);
	}
	
	public static BusyDialog createBusyDialog(Activity activity, DialogTypes type, String title, String message) {
		return BusyDialog.create(activity, type, title, message);
	}
	
	public static DownloadDialog createDownloadDialog(Activity activity, DialogTypes type, String title, String message) {
		return DownloadDialog.create(activity, type, title, message);
	}
	
}
