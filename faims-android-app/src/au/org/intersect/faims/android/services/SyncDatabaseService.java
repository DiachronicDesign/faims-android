package au.org.intersect.faims.android.services;

import java.io.File;
import java.util.UUID;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import roboguice.RoboGuice;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.FetchResult;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

import com.google.inject.Inject;

public class SyncDatabaseService extends IntentService {

	@Inject
	FAIMSClient faimsClient;
	
	@Inject
	DatabaseManager databaseManager;
	
	private File tempProject;
	
	private File tempDB;
	
	private File tempDir;
	
	private TarArchiveOutputStream os;
	
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
		if (os != null) {
			try {
				os.close();
			} catch (Exception e) {
				FLog.e("error closing steam", e);
			}
		}
		if (tempProject != null) {
			tempProject.delete();
		}
		if (tempDB != null) {
			tempDB.delete();
		}
		if (tempDir != null) {
			FileUtil.deleteDirectory(tempDir);
		}
		FLog.d("stopping service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		FLog.d("starting service");
		
		// 1. upload database to server
		// 2. download database from server
		
		Result uploadResult = uploadDatabaseToServer(intent);
		if (uploadResult.resultCode != FAIMSClientResultCode.SUCCESS) {
			sendMessage(intent, uploadResult);
			return;
		}
		
		Result downloadResult = downloadDatabaseFromServer(intent);
		sendMessage(intent, downloadResult);
	}
	
	private void sendMessage(Intent intent, Result result) {
		try {
			Bundle extras = intent.getExtras();
			Messenger messenger = (Messenger) extras.get("MESSENGER");
			Message msg = Message.obtain();
			msg.obj = result;
			messenger.send(msg);
		} catch (Exception me) {
			FLog.e("error sending message", me);
		}
	}

	private Result uploadDatabaseToServer(Intent intent) {
		FLog.d("uploading database");
		
		try {
			Bundle extras = intent.getExtras();
			String userId = intent.getStringExtra("userId");
			Project project = (Project) extras.get("project");
			String database = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key + "/db.sqlite3";
			
			// create temp database to upload
			String dumpTimestamp = DateUtil.getCurrentTimestampGMT();
			databaseManager.init(database);
			
			tempDB = File.createTempFile("temp_", ".sqlite3", new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir));
			
			if (project.timestamp == null) {
				databaseManager.dumpDatabaseTo(tempDB);
			} else {
				databaseManager.dumpDatabaseTo(tempDB, project.timestamp); 
			}
			
	    	// check if database is empty
	    	if (databaseManager.isEmpty(tempDB)) {
	    		FLog.d("nothing to upload");
	    		return Result.SUCCESS;
	    	}
	    	
	    	if (syncStopped) {
	    		FLog.d("sync cancelled");
	    		return Result.INTERRUPTED;
	    	} 
	    	
		    // tar file
	    	tempProject = File.createTempFile("temp_", ".tar.gz", new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir));
	    	
	    	os = FileUtil.createTarOutputStream(tempProject.getAbsolutePath());
	    	
		    FileUtil.tarFile(tempDB.getAbsolutePath(), os);
		    
		    if (syncStopped) {
	    		FLog.d("sync cancelled");
	    		return Result.INTERRUPTED;
	    	} 
			    	
		    Result result = faimsClient.uploadDatabase(project, tempProject, userId);
		    
		    if (syncStopped) {
	    		FLog.d("sync cancelled");
	    		return Result.INTERRUPTED;
	    	} 
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				faimsClient.invalidate();
				FLog.d("upload failure");
				return result;
			} 
			
			project = ProjectUtil.getProject(project.key); // get the latest settings
			project.timestamp = dumpTimestamp;
			ProjectUtil.saveProject(project);
			
			FLog.d("upload success");
			return result;
		} catch (Exception e) {
			FLog.e("error syncing database", e);
			return Result.FAILURE;
		} finally {
			if (tempDB != null) {
				tempDB.delete();
			}
			
			if (tempProject != null) {
				tempProject.delete();
			}
			
			// TODO check if this is necessary as file util also closes the stream
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
					FLog.e("error closing steam", e);
				}
			}
		}
	}

	private Result downloadDatabaseFromServer(Intent intent) {
		FLog.d("downloading database");
		
		try {
			Project project = (Project) intent.getExtras().get("project");

			FileInfo info;
			int syncVersion;
				
			// check if there is a new version to download
			FetchResult fetchResult = faimsClient.fetchDatabaseVersion(project);
			
			if (syncStopped) {
				FLog.d("sync cancelled");
				return Result.INTERRUPTED;
			}
			
			if (fetchResult.resultCode == FAIMSClientResultCode.FAILURE) {
				faimsClient.invalidate();
				FLog.d("download failure");
				return fetchResult;
			} else {
				info = (FileInfo) fetchResult.data;
				int serverVersion = Integer.parseInt(info.version == null ? "0" : info.version);
				int projectVersion = Integer.parseInt(project.version == null ? "0" : project.version);
				if (serverVersion == projectVersion) {
					FLog.d("already up to date");
					return Result.SUCCESS;
				}
				syncVersion = projectVersion + 1;
			}
				
			// download database from version
			tempDir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + "temp_" + UUID.randomUUID());
			tempDir.mkdirs();
			
			DownloadResult downloadResult = faimsClient.downloadDatabase(project, String.valueOf(syncVersion), tempDir.getAbsolutePath());
			
			if (syncStopped) {
				FLog.d("sync cancelled");
				return Result.INTERRUPTED;
			}
			
			if (downloadResult.resultCode == FAIMSClientResultCode.FAILURE) {
				faimsClient.invalidate();
				FLog.d("download failure");
				return downloadResult;
			} 
			
			// merge database 
			databaseManager.mergeDatabaseFrom(new File(tempDir.getAbsoluteFile() + "/db.sqlite3"));
				
			// update settings
			project = ProjectUtil.getProject(project.key); // get the latest settings
			project.version = downloadResult.info.version;
			ProjectUtil.saveProject(project);
			
			FLog.d("download success");
			return downloadResult;
		} catch (Exception e) {
			FLog.e("error syncing database", e);
			return Result.FAILURE;
		} finally {
			if (tempDir != null) {
				FileUtil.deleteDirectory(tempDir);
			}
		}
	}

}
