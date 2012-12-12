package au.org.intersect.faims.util;

import android.app.Activity;
import au.org.intersect.faims.android.BusyDialog;
import au.org.intersect.faims.android.ConfirmDialog;
import au.org.intersect.faims.android.LocateServerDialog;

public class DialogCreatorUtil {
	
	public static LocateServerDialog createLocateServerDialog(Activity activity) {
		return LocateServerDialog.create(activity);
	}
	
	public static ConfirmDialog createConfirmDialog(Activity activity, String type, String title, String message) {
		return ConfirmDialog.create(activity, type, title, message);
	}
	
	public static BusyDialog createBusyDialog(Activity activity, String type, String title, String message) {
		return BusyDialog.create(activity, type, title, message);
	}
	
}
