package au.org.intersect.faims.android.services;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.FileUtil;

public class UploadDatabaseService extends UploadService {

	public UploadDatabaseService() {
		super("UploadDatabaseService");
	}
	
	@Override
	protected FAIMSClientResultCode doUpload(Intent intent) throws Exception {
		File tempFile = null;
		
		try {
			String database = intent.getStringExtra("database");
			String userId = intent.getStringExtra("userId");
			Bundle extras = intent.getExtras();
			Project project = (Project) extras.get("project");
			
			// create temp database to upload
			databaseManager.init(database);
			
			File outputDir = new File(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.key);
			
	    	tempFile = File.createTempFile("temp_", ".sqlite3", outputDir);
	    	databaseManager.dumpDatabaseTo(tempFile);
	    	
	    	if (uploadStopped) {
	    		Log.d("FAIMS", "cancelled upload");
	    		return null; // note: this doesn't matter as upload is cancelled
	    	}
	    	
	    	// tar file
	    	file = File.createTempFile("temp_", ".tar.gz", outputDir);
	    	FileUtil.tarFile(tempFile.getAbsolutePath(), file.getAbsolutePath());
	    	
	    	if (uploadStopped) {
	    		Log.d("FAIMS", "cancelled upload");
	    		return null;
	    	}
	    	
	    	// upload database
			return faimsClient.uploadDatabase(project, file, userId);
			
		} finally {
			if (tempFile != null) tempFile.delete();
		}
	}

}
