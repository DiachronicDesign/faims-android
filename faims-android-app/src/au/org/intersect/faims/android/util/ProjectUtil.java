package au.org.intersect.faims.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Environment;
import au.org.intersect.faims.android.data.Project;

import com.google.gson.JsonObject;

public class ProjectUtil {

	public static List<Project> getProjects() {
		File file = new File(Environment.getExternalStorageDirectory() + "/faims/projects");
		if (!file.isDirectory()) return null;
		
		String[] directories = file.list(new FilenameFilter() {

			@Override
			public boolean accept(File file, String arg1) {
				return file.isDirectory();
			}
			
		});
		
		Arrays.sort(directories);
		ArrayList<Project> list = new ArrayList<Project>();
		FileInputStream is = null;
		try {
			for (String dir : directories) {
				is = new FileInputStream(
							Environment.getExternalStorageDirectory() + "/faims/projects/" + dir + "/project.settings");
				String config = FileUtil.convertStreamToString(is);
				if (config == null) {
					FAIMSLog.log("project " + "/faims/projects/" + dir + "/project.settings" + " settings malformed");
					continue;
				}
				JsonObject object = JsonUtil.deserializeJson(config);
				Project project = Project.fromJson(dir, object);	
				list.add(project);
			}
		} catch (IOException e) {
			FAIMSLog.log(e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				FAIMSLog.log(e);
			}
		}
		return list;
	}

	public static Project getProject(
			String projectName) {
		List<Project> projects = getProjects();
		if (projects != null) {
			for (Project p : projects) {
				if (p.name.equals(projectName)) return p;
			}
		}
		return null;
	}
	
}
