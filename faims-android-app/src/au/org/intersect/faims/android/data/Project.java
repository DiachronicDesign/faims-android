package au.org.intersect.faims.android.data;

import com.google.gson.JsonObject;

public class Project {

	public String name;
	public String id;
	
	public Project() {
		
	}
	
	public Project(String name, String id) {
		this.name = name;
		this.id = id;
	}
	
	public static Project fromJson(JsonObject object) {
		Project p = new Project();
		p.name = object.get("name").getAsString();
		p.id = object.get("id").getAsString();
		return p;
	}
	
}
