package au.org.intersect.faims.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import au.org.intersect.faims.android.data.Project;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class JsonUtil {

	public static String serializeServerPacket(String ip, String port) {
		JsonObject object = new JsonObject();
    	object.addProperty("android_ip", ip);
    	object.addProperty("android_port", port);
    	return object.toString();
	}
	
	public static JsonObject deserializeServerPacket(String json) throws IOException {
		return deserializeJson(json);
	}
	
	public static JsonObject deserializeJson(String json) throws IOException {
		JsonReader reader = new JsonReader(new StringReader(json));
        JsonParser parser = new JsonParser();
        return parser.parse(reader).getAsJsonObject(); 
	}
	
	public static List<Project> deserializeProjects(InputStream stream) throws IOException {
		LinkedList<Project> projects = new LinkedList<Project>();
		JsonArray objects = deserializeJsonArray(stream);
		for (int i = 0; i < objects.size(); i++) {
			projects.push(Project.fromJson(objects.get(i).getAsJsonObject()));
		}
		return projects;
	}
	
	public static Project deserializeProjectArchive(InputStream stream) throws IOException {
		JsonObject object = deserializeJsonObject(stream);
		return Project.fromJson(object);
	}
	
	public static JsonObject deserializeJsonObject(InputStream stream) throws IOException {
        JsonReader reader = new JsonReader(streamToReader(stream));
        JsonParser parser = new JsonParser();
        return parser.parse(reader).getAsJsonObject(); 
	}
	
	public static JsonArray deserializeJsonArray(InputStream stream) throws IOException {
		JsonReader reader = new JsonReader(streamToReader(stream));
	    JsonParser parser = new JsonParser();
	    return parser.parse(reader).getAsJsonArray();
	}
	
	private static StringReader streamToReader(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream);
        StringBuilder sb = new StringBuilder();
        int value;
        while((value = reader.read()) > 0)
            sb.append((char) value);
        return new StringReader(sb.toString());
	}

}
