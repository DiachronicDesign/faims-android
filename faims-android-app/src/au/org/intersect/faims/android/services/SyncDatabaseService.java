package au.org.intersect.faims.android.services;

import java.io.File;
import java.util.UUID;

import roboguice.RoboGuice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.DownloadResult;
import au.org.intersect.faims.android.data.FetchResult;
import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

import com.google.inject.Inject;

public class SyncDatabaseService extends MessageIntentService {

	@Inject
	FAIMSClient faimsClient;
	
	@Inject
	DatabaseManager databaseManager;
	
	private boolean syncStopped;
	
	public SyncDatabaseService() {
		super("SyncDatabaseService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		syncStopped = true;
		faimsClient.interrupt();
		FLog.d("stopping service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		FLog.d("starting service");
		
		// 1. upload database to server
		// 2. download database from server
		
		if (!uploadDatabaseToServer(intent)) return;
		if (!downloadDatabaseFromServer(intent)) return;
	}

	private boolean uploadDatabaseToServer(Intent intent) {
		FLog.d("uploading database");
		
		FAIMSClientResultCode result = null;
		File tempDB = null;
		File tempProject = null;
		try {
			Bundle extras = intent.getExtras();
			String userId = intent.getStringExtra("userId");
			Project project = (Project) extras.get("project");
			String database = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key + "/db.sqlite3";
			
			// create temp database to upload
			String dumpTimestamp = DateUtil.getCurrentTimestampGMT();
			databaseManager.init(database);
			File outputDir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key);
			tempDB = File.createTempFile("temp_", ".sqlite3", outputDir);
			if (project.timestamp == null) {
				databaseManager.dumpDatabaseTo(tempDB);
			} else {
				databaseManager.dumpDatabaseTo(tempDB, project.timestamp); 
			}
			
	    	// check if database is empty
	    	if (databaseManager.isEmpty(tempDB)) {
	    		FLog.d("nothing to upload");
	    		result = FAIMSClientResultCode.SUCCESS;
	    		return true;
	    	}
	    	
	    	if (syncStopped) {
	    		result = null;
	    		return false;
	    	} 
	    	
		    // tar file
	    	tempProject = File.createTempFile("temp_", ".tar.gz", outputDir);
		    FileUtil.tarFile(tempDB.getAbsolutePath(), tempProject.getAbsolutePath());
		    
		    if (syncStopped) {
		    	result = null;
		    	return false;
		    }
			    	
		    result = faimsClient.uploadDatabase(project, tempProject, userId);
			
			if (syncStopped) {
				result = null;
				return false;
			}
			
			if (result != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
				FLog.d("upload failure");
				return false;
			} 
			
			project = ProjectUtil.getProject(project.key); // get the latest settings
			project.timestamp = dumpTimestamp;
			ProjectUtil.saveProject(project);
			
			FLog.d("upload success");
			return true;
		} catch (Exception e) {
			FLog.e("error syncing database", e);
			result = FAIMSClientResultCode.SERVER_FAILURE;
		} finally {
			if (tempDB != null) {
				tempDB.delete();
			}
			
			if (tempProject != null) {
				tempProject.delete();
			}
			
			try {
				sendMessage(intent, MessageType.SYNC_DATABASE_UPLOAD, result);
			} catch (Exception me) {
				FLog.e("error sending message", me);
			}
		}
		return false;
	}

	private boolean downloadDatabaseFromServer(Intent intent) {
		FLog.d("downloading database");
		
		FAIMSClientResultCode result = null;
		File tempDir = null;
		try {
			Project project = (Project) intent.getExtras().get("project");

			FileInfo info;
			int syncVersion;
				
			// check if there is a new version to download
			FetchResult fetchResult = faimsClient.fetchDatabaseVersion(project);
			result = fetchResult.code;
			
			if (result != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
				FLog.d("download failure");
				return false;
			} else {
				info = (FileInfo) fetchResult.data;
				int serverVersion = Integer.parseInt(info.version == null ? "0" : info.version);
				int projectVersion = Integer.parseInt(project.version == null ? "0" : project.version);
				if (serverVersion == projectVersion) {
					FLog.d("already up to date");
					return true;
				}
				syncVersion = projectVersion + 1;
			}
				
			// download database from version
			tempDir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key + "/" + UUID.randomUUID());
			tempDir.mkdirs();
			DownloadResult downloadResult = faimsClient.downloadDatabase(project, String.valueOf(syncVersion), tempDir.getAbsolutePath());
			result = downloadResult.code;
			
			if (result != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
				FLog.d("download failure");
				return false;
			} 
			
			// merge database 
			databaseManager.mergeDatabaseFrom(new File(tempDir.getAbsoluteFile() + "/db.sqlite3"));
				
			// update settings
			project = ProjectUtil.getProject(project.key); // get the latest settings
			project.version = downloadResult.info.version;
			ProjectUtil.saveProject(project);
			
			FLog.d("download success");
			return true;
		} catch (Exception e) {
			FLog.e("error syncing database", e);
			result = FAIMSClientResultCode.SERVER_FAILURE;
		} finally {
			if (tempDir != null) {
				FileUtil.deleteDirectory(tempDir);
			}
			
			try {
				sendMessage(intent, MessageType.SYNC_DATABASE_DOWNLOAD, result);
			} catch (Exception me) {
				FLog.e("error sending message", me);
			}
		}
		return false;
	}

}
