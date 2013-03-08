package au.org.intersect.faims.android.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.DownloadResult;
import au.org.intersect.faims.android.data.FetchResult;
import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.JsonUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// TODO create unit tests by injecting AndroidHttpClient with mock client
@Singleton
public class FAIMSClient {

	private static final int CONNECTION_TIMEOUT = 60 * 1000;
	
	private static final int DATA_TIMEOUT = 60 * 1000;

	private static final String BASE_DIR = "/faims/projects/";

	@Inject
	ServerDiscovery serverDiscovery;
	
	AndroidHttpClient httpClient;
	
	private void initClient() throws UnknownHostException {
		if (httpClient == null) {
			FAIMSLog.log();
			String userAgent = InetAddress.getLocalHost().toString();
			httpClient = AndroidHttpClient.newInstance(userAgent);
			
			FAIMSLog.log("userAgent is " + userAgent);
		}
	}
	
	private void cleanupClient() {
		if (httpClient != null) {
			FAIMSLog.log();
			httpClient.close();
			httpClient = null;
		}
	}
	
	public FAIMSClientResultCode uploadDatabase(Project project, File file, String userId) {
		try {
			HashMap<String, ContentBody> extraParts = new HashMap<String, ContentBody>();
			extraParts.put("user", new StringBody(userId));
			return uploadFile(file, "/android/project/" + project.key + "/upload_db", extraParts);
		} catch (Exception e) {
			Log.e("FAIMS", "Error during uploading database", e);
		} 
		return FAIMSClientResultCode.SERVER_FAILURE;
	}
	
	public FAIMSClientResultCode uploadFile(File file, String path) {
		return uploadFile(file, path, null);
	}
	
	public FAIMSClientResultCode uploadFile(File file, String path, HashMap<String, ContentBody> extraParts) {
		synchronized(FAIMSClient.class) {
			try {
				initClient();
				
				MultipartEntity entity = new MultipartEntity();
				entity.addPart("file", new FileBody(file, "binary/octet-stream"));
				entity.addPart("md5", new StringBody(FileUtil.generateMD5Hash(file.getPath())));
				
				if (extraParts != null) {
					for (Entry<String, ContentBody> entry : extraParts.entrySet()) {
						entity.addPart(entry.getKey(), entry.getValue());
					}
				}
				
				HttpPost post = new HttpPost(new URI(getUri(path)));
				post.setEntity(entity);
				
				HttpResponse response = httpClient.execute(post);
				
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					Log.d("FAIMS", "upload failed");
					
					return FAIMSClientResultCode.SERVER_FAILURE;
				}
				
				Log.d("FAIMS", "uploaded file!");
				
				return FAIMSClientResultCode.SUCCESS;
				
			} catch (Exception e) {
				Log.e("FAIMS", "Error during uploading file", e);
				
				return FAIMSClientResultCode.SERVER_FAILURE;
				
			} finally {
				cleanupClient();
			}
		}
	}
	
	public FAIMSClientResultCode fetchProjectList(LinkedList<Project> projects) {
		synchronized(FAIMSClient.class) {
			FAIMSLog.log();
	
			InputStream stream = null;
			try {			
				initClient();
				
				HttpEntity entity = getRequest(getUri("/android/projects"));
				
				stream = entity.getContent();
				
				List<Project> ps = JsonUtil.deserializeProjects(stream);
				
				for (Project p : ps) {
					projects.push(p);
				}
				
				FAIMSLog.log("fetched projects!");
		        
				return FAIMSClientResultCode.SUCCESS;
			} catch(Exception e) {
				FAIMSLog.log(e);
				
				return FAIMSClientResultCode.SERVER_FAILURE;
				
			} finally {
				
				try {
					if (stream != null) stream.close();
				} catch (IOException e) {
					FAIMSLog.log(e);
				}
				
				cleanupClient();
			}
		}
	}
	
	public FetchResult fetchDatabaseVersion(Project project) {
		synchronized(FAIMSClient.class) {
	
			InputStream stream = null;
			try {			
				initClient();
				
				FileInfo info = new FileInfo();
				
				getFileInfo("/android/project/" + project.key + "/archive_db", info);
				
				FAIMSLog.log("fetched database version!");
				
				FetchResult result = new FetchResult();
				result.code = FAIMSClientResultCode.SUCCESS;
				result.data = info;
		        
				return result;
			} catch(Exception e) {
				FAIMSLog.log(e);
				
				FetchResult result = new FetchResult();
				result.code = FAIMSClientResultCode.SERVER_FAILURE;
				return result;
				
			} finally {
				
				try {
					if (stream != null) stream.close();
				} catch (IOException e) {
					FAIMSLog.log(e);
				}
				
				cleanupClient();
			}
		}
	}
	
	public DownloadResult downloadProject(Project project) {
		FileInfo info = new FileInfo();
		FAIMSClientResultCode code = downloadFile("/android/project/" + project.key + "/archive", 
				"/android/project/" + project.key + "/download", 
				Environment.getExternalStorageDirectory() + BASE_DIR, info);
		DownloadResult result = new DownloadResult();
		result.code = code;
		result.info = info;
		return result;
	}
	
	public DownloadResult downloadDatabase(Project project) {
		FileInfo info = new FileInfo();
		FAIMSClientResultCode code =  downloadFile("/android/project/" + project.key + "/archive_db", 
				"/android/project/" + project.key + "/download_db", 
				Environment.getExternalStorageDirectory() + BASE_DIR + project.key, info);
		
		DownloadResult result = new DownloadResult();
		result.code = code;
		result.info = info;
		return result;
	}
	
	public DownloadResult downloadDatabase(Project project, String version, String dir) {
		FileInfo info = new FileInfo();
		FAIMSClientResultCode code =  downloadFile("/android/project/" + project.key + "/archive_db?version=" + version, 
				"/android/project/" + project.key + "/download_db?version=" + version, 
				dir, info);
		DownloadResult result = new DownloadResult();
		result.code = code;
		result.info = info;
		return result;
	}
	
	public FAIMSClientResultCode downloadFile(String infoPath, String downloadPath, String dir) {
		return downloadFile(infoPath, downloadPath, dir, new FileInfo());
	}
	
	public FAIMSClientResultCode downloadFile(String infoPath, String downloadPath, String dir, FileInfo info) {
		synchronized(FAIMSClient.class) {
			FAIMSLog.log();
			
			InputStream stream = null;
			File file = null;
			
			try {
				initClient();
				
				getFileInfo(infoPath, info);
				
		        long freeSpace = FileUtil.getExternalStorageSpace();
		        
		        FAIMSLog.log("freespace: " + String.valueOf(freeSpace));
		        FAIMSLog.log("filesize: " + String.valueOf(info.size));
		        
		        if (info.size > freeSpace) {
		        	return FAIMSClientResultCode.STORAGE_LIMIT_ERROR;
		        } 
		        
		        file = downloadArchive(downloadPath, info);
				
				if (file == null) {
					return FAIMSClientResultCode.DOWNLOAD_CORRUPTED;
				}
				
				FileUtil.untarFile(dir, file.getAbsolutePath());
				
				file.delete();
				
				FAIMSLog.log("downloaded file!");
				
				return FAIMSClientResultCode.SUCCESS;
		        
			} catch (Exception e) {
				FAIMSLog.log(e);
				
				// remove downloaded file
				if (file != null) {
					file.delete();
				}
					
				return FAIMSClientResultCode.SERVER_FAILURE;
				
			} finally {
				
				try {
					if (stream != null) stream.close();
				} catch (IOException e) {
					FAIMSLog.log(e);
				}
				
				cleanupClient();
			}
		}
	}
	
	private void getFileInfo(String path, FileInfo info) throws Exception {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpEntity entity = getRequest(getUri(path));
			
			stream = entity.getContent();
			
			JsonObject object = JsonUtil.deserializeJsonObject(stream);
			
			info.parseJson(object);
			
		} finally {
			if (stream != null) stream.close();
		}
		
	}
	
	private File downloadArchive(String path, FileInfo archive) throws Exception {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, DATA_TIMEOUT);
			
			HttpEntity entity = getRequest(getUri(path), params);
			
			stream = entity.getContent();
			
	    	FileUtil.makeDirs(Environment.getExternalStorageDirectory() + BASE_DIR); // make sure directory exists
			
	    	File tempFile = File.createTempFile("temp_", ".tar.gz", new File(Environment.getExternalStorageDirectory() + BASE_DIR));
	    	
			FileUtil.saveFile(stream, tempFile.getAbsolutePath());
			
			String md5 = FileUtil.generateMD5Hash(tempFile.getAbsolutePath());
			
			FAIMSLog.log("filename.md5Hash: " + md5);
			FAIMSLog.log("archive.md5Hash:  " + archive.md5);
			
			if (!archive.md5.equals(md5)) {
				
				tempFile.delete();
				
				return null;
			}
			
			return tempFile;
		} finally {
			// TODO check if file needs to be deleted
			if (stream != null) stream.close();
		}
	}
	
	private String getUri(String path) throws Exception {
		FAIMSLog.log(serverDiscovery.getServerHost() + path);
		
		return serverDiscovery.getServerHost() + path;
	}
	
	private HttpEntity getRequest(String uri) throws IOException {
		return getRequest(uri, null);
	}
	
	private HttpEntity getRequest(String uri, HttpParams params) throws IOException {
		FAIMSLog.log(uri);
		
		HttpGet get = new HttpGet(uri);
		
		if (params != null) {
			get.setParams(params);
		}

		HttpResponse response = httpClient.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			Log.d("FAIMS", "failed request: " + uri);
			return null;
		}
		
		HttpEntity entity = response.getEntity();
		
		return entity;
	}
	
	public void invalidate() {
		serverDiscovery.invalidateServerHost();
	}

	public void interrupt() {
		synchronized(FAIMSClient.class) {
			if (httpClient != null) {
				httpClient.getConnectionManager().shutdown();
			}
		}
	}

	public FAIMSClientResultCode uploadDirectory(String projectDir, String uploadDir, String requestExcludePath, String uploadPath) {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			File file = null;
			try {
				initClient();
				
				String uploadDirPath = projectDir + "/" + uploadDir;
				
				if (!new File(uploadDirPath).isDirectory()) {
					Log.d("FAIMS", "no new files to upload");
					return FAIMSClientResultCode.SUCCESS;
				}
				
				List<String> localFiles = FileUtil.listDir(uploadDirPath);
				
				Log.d("FAIMS", "local Files: " + localFiles.toString());
				
				if (localFiles.size() == 0) {
					Log.d("FAIMS", "no new files to upload");
					return FAIMSClientResultCode.SUCCESS;
				}
				
				HttpEntity entity = getRequest(getUri(requestExcludePath));
				stream = entity.getContent();
				JsonObject object = JsonUtil.deserializeJsonObject(stream);
				
				ArrayList<String> files = new ArrayList<String>();
				JsonArray filesArray = object.getAsJsonArray("files");
				for (int i = 0; i < filesArray.size(); i++) {
					files.add(filesArray.get(i).getAsString());
				}
				
				Log.d("FAIMS", "server Files: " + files.toString());
				
				// check if new files to upload
				boolean canUpload = !isFileListInFileList(localFiles, files);
				
				if (!canUpload) {
					Log.d("FAIMS", "no new files to upload");
					return FAIMSClientResultCode.SUCCESS;
				}
				
				file = new File(projectDir + "/" + UUID.randomUUID());
				FileUtil.tarFile(uploadDirPath, "", file.getAbsolutePath(), files);
				
				return uploadFile(file, uploadPath);
			} catch (Exception e) {
				Log.e("FAIMS", "Error during uploading directory", e);
			} finally {
				try {
					if (stream != null) stream.close();
				} catch (IOException ioe) {
					Log.e("FAIMS", "Error during uploading directory", ioe);
				}
				
				if (file != null) file.delete();
				
				cleanupClient();
			}
			return FAIMSClientResultCode.SERVER_FAILURE;
		}
	}

	public FAIMSClientResultCode downloadDirectory(String projectDir, String downloadDir, String requestExcludePath, String infoPath, String downloadPath) {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			try {
				initClient();
				
				HttpEntity entity = getRequest(getUri(requestExcludePath));
				stream = entity.getContent();
				JsonObject object = JsonUtil.deserializeJsonObject(stream);
				
				ArrayList<String> files = new ArrayList<String>();
				JsonArray filesArray = object.getAsJsonArray("files");
				for (int i = 0; i < filesArray.size(); i++) {
					files.add(filesArray.get(i).getAsString());
				}
				
				Log.d("FAIMS", "server Files: " + files.toString());
				
				if (files.size() == 0) {
					Log.d("FAIMS", "no new files to download");
					return FAIMSClientResultCode.SUCCESS;
				}
				
				String downloadDirPath = projectDir + "/" + downloadDir;
				
				// make sure dir exists
				FileUtil.makeDirs(downloadDirPath);
				
				List<String> localFiles = FileUtil.listDir(downloadDirPath);
				
				// check if new files to download
				boolean canDownload = !isFileListInFileList(files, localFiles);
				
				if (!canDownload) {
					Log.d("FAIMS", "no new files to download");
					return FAIMSClientResultCode.SUCCESS;
				}
				
				// construct info path with exclude files
				StringBuilder sb = new StringBuilder(infoPath + "?");
				for (String lf : localFiles) {
					sb.append("files[]=");
					sb.append(lf);
					sb.append("&");
				}
				
				return downloadTempFile(sb.toString(), downloadPath, downloadDirPath);
			} catch (Exception e) {
				Log.e("FAIMS", "Error during downloading directory", e);
			} finally {
				try {
					if (stream != null) stream.close();
				} catch (IOException ioe) {
					Log.e("FAIMS", "Error during downloading directory", ioe);
				}
				
				cleanupClient();
			}
			return FAIMSClientResultCode.SERVER_FAILURE;
		}
	}
	
	private boolean isFileListInFileList(List<String> subFiles, List<String> files) {
		for (String sf : subFiles) {
			boolean fileInList = false;
			for (String f : files) {
				if (sf.equals(f)) {
					fileInList = true;
					break;
				}
			}
			if (!fileInList) {
				return false;
			}
		}
		return true;
	}
	
	public FAIMSClientResultCode downloadTempFile(String infoPath, String downloadPath, String dir) {
		return downloadTempFile(infoPath, downloadPath, dir, new FileInfo());
	}
	
	public FAIMSClientResultCode downloadTempFile(String infoPath, String downloadPath, String dir, FileInfo info) {
		synchronized(FAIMSClient.class) {
			FAIMSLog.log();
			
			InputStream stream = null;
			File file = null;
			
			try {
				initClient();
				
				getFileInfo(infoPath, info);
				
		        long freeSpace = FileUtil.getExternalStorageSpace();
		        
		        FAIMSLog.log("freespace: " + String.valueOf(freeSpace));
		        FAIMSLog.log("filesize: " + String.valueOf(info.size));
		        
		        if (info.size > freeSpace) {
		        	return FAIMSClientResultCode.STORAGE_LIMIT_ERROR;
		        } 
		        
		        file = downloadArchive(downloadPath + "?file=" + URLEncoder.encode(info.filename, "UTF-8"), info);
				
				if (file == null) {
					return FAIMSClientResultCode.DOWNLOAD_CORRUPTED;
				}
				
				FileUtil.untarFile(dir, file.getAbsolutePath());
				
				file.delete();
				
				FAIMSLog.log("downloaded file!");
				
				return FAIMSClientResultCode.SUCCESS;
		        
			} catch (Exception e) {
				FAIMSLog.log(e);
				
				// remove downloaded file
				if (file != null) {
					file.delete();
				}
					
				return FAIMSClientResultCode.SERVER_FAILURE;
				
			} finally {
				
				try {
					if (stream != null) stream.close();
				} catch (IOException e) {
					FAIMSLog.log(e);
				}
				
				cleanupClient();
			}
		}
	}
	
}
