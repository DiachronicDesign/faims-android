package au.org.intersect.faims.android.ui.form;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;

import au.org.intersect.faims.android.util.FAIMSLog;

public class Arch16n {

	private Map<String,String> properties;
	private String path;
	private String projectName;

	public Arch16n(String path, String projectName){
		this.properties = new HashMap<String, String>();
		this.path = path;
		this.projectName = projectName;
	}

	public void generatePropertiesMap() {
		try {
			FileInputStream fileInputStream = new FileInputStream(path+"/faims.properties");
			PropertyResourceBundle propertyResourceBundle = new PropertyResourceBundle(fileInputStream);
			for(String s : propertyResourceBundle.keySet()){
				properties.put(s, propertyResourceBundle.getString(s));
			}
		} catch (FileNotFoundException e) {
			FAIMSLog.log("Required faims.properties is not found in the project");
		} catch (IOException e) {
		}
		
		try{
			FileInputStream fileInputStream = new FileInputStream(path+"/faims_"+projectName.replaceAll("\\s", "_")+".properties");
			PropertyResourceBundle propertyResourceBundle = new PropertyResourceBundle(fileInputStream);
			for(String s : propertyResourceBundle.keySet()){
				properties.put(s, propertyResourceBundle.getString(s));
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public String getProperties(String property){
		return this.properties.get(property);
	}

	public String substituteValue(String value){
		if(value.contains("{") && value.contains("}")){
			String toBeSubbed = value.substring(value.indexOf("{"), value.indexOf("}")+1);
			String subs = toBeSubbed.substring(1, toBeSubbed.length()-1);
			return (getProperties(subs) != null) ? value.replace(toBeSubbed, getProperties(subs)) : value;
		}
		return value;
	}
}
