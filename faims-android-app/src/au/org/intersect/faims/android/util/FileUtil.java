package au.org.intersect.faims.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.util.XFormUtils;

import android.os.Environment;
import android.os.StatFs;

public class FileUtil {
	
	private static String toPath(String path) {
		return Environment.getExternalStorageDirectory() + path;
	}

	public static void makeDirs(String dir) {
		FAIMSLog.log();
		
		File file = new File(toPath(dir));
		if (!file.exists())
			file.mkdirs();
		
		FAIMSLog.log(toPath(dir) + " is present " + String.valueOf(file.exists()));
	}
	
	public static void untarFromStream(String dir, String filename) throws IOException {
		FAIMSLog.log();
		
		TarArchiveInputStream ts = null;
		try {
		 ts = new TarArchiveInputStream(
				 new GZIPInputStream(
						 new FileInputStream(toPath(filename))));
		 
	     TarArchiveEntry e;
	     while((e = ts.getNextTarEntry()) != null) {
	    	 if (e.isDirectory()) {
	    		 makeDirs(dir + "/" + e.getName());
	    	 } else {
	    		 writeTarFile(ts, e, new File(toPath(dir + "/" + e.getName())));
	    	 }
	     }
		} finally {
			if (ts != null) ts.close();
		}
		
		FAIMSLog.log("untared file " + filename);
	}
	
	// from: http://stackoverflow.com/questions/3163045/android-how-to-check-availability-of-space-on-external-storage
	public static long getExternalStorageSpace() throws Exception {
		FAIMSLog.log();
		
	    long availableSpace = -1L;
	    
	    StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
	    stat.restat(Environment.getExternalStorageDirectory().getPath());
	    availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

	    return availableSpace;
	}
	
	public static void saveFile(InputStream input, String filename) throws IOException {
		FAIMSLog.log();
		
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(toPath(filename));
		        
			byte[] buffer = new byte[1024];
	        int bufferLength = 0; //used to store a temporary size of the buffer
	        
	        while ( (bufferLength = input.read(buffer)) > 0) {
	            os.write(buffer, 0, bufferLength);
	        }
		} finally {
			if (os != null) os.close();
		}
        
		FAIMSLog.log("saved file " + filename);
	}
	
	public static String generateMD5Hash(String filename) throws IOException, NoSuchAlgorithmException {
		FAIMSLog.log();
		
		FileInputStream fs = null;
		try {
			fs = new FileInputStream(toPath(filename));
			
			MessageDigest digester = MessageDigest.getInstance("MD5");
			byte[] bytes = new byte[8192];
			int byteCount;
			while ((byteCount = fs.read(bytes)) > 0) {
				digester.update(bytes, 0, byteCount);
			}
			
			FAIMSLog.log("generated md5 for hash for file " + filename);
			
			return new String(Hex.encodeHex(digester.digest()));
		} finally {
			if (fs != null) fs.close();
		}
	}
	
	public static void deleteFile(String filename) throws IOException {
		FAIMSLog.log();
		
		new File(toPath(filename)).delete();
		
		FAIMSLog.log("deleted file " + filename);
	}
	
	private static void writeTarFile(TarArchiveInputStream ts, TarArchiveEntry entry, File file) throws IOException {
		FAIMSLog.log();
		
		FileOutputStream os = null;
		
		try {
			os = new FileOutputStream(file);
		        
			byte[] buffer = new byte[(int)entry.getSize()];
	        int bufferLength = 0; //used to store a temporary size of the buffer
	        
	        while ( (bufferLength = ts.read(buffer)) > 0 ) {
	            os.write(buffer, 0, bufferLength);
	        }
		} finally {
			if (os != null) os.close();
		}
		
		FAIMSLog.log("writing tar file " + file.getName());
	}
	
	public static FormEntryController readXmlContent(String path) {
		FormDef fd = null;
		FileInputStream fis = null;
		String mErrorMsg = null;

		File formXml = new File(path);

		try {
			fis = new FileInputStream(formXml);
			fd = XFormUtils.getFormFromInputStream(fis);
			if (fd == null) {
				mErrorMsg = "Error reading XForm file";
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			mErrorMsg = e.getMessage();
		} catch (XFormParseException e) {
			mErrorMsg = e.getMessage();
			e.printStackTrace();
		} catch (Exception e) {
			mErrorMsg = e.getMessage();
			e.printStackTrace();
		}

		if (mErrorMsg != null) {
			return null;
		}

		// new evaluation context for function handlers
		fd.setEvaluationContext(new EvaluationContext(null));

		// create FormEntryController from formdef
		FormEntryModel fem = new FormEntryModel(fd);
		return new FormEntryController(fem);
	}
	
	public static String readFileIntoString(String path) {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(new File(path));
		    FileChannel fc = stream.getChannel();
		    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		    /* Instead of using default, pass in a decoder. */
		    return Charset.defaultCharset().decode(bb).toString();
		}
		catch(IOException ioe){
			return "";
		}
		finally {
			try{
				stream.close();
			}
			catch(Exception e){
				// continue
			}
		}
	}
	
}


