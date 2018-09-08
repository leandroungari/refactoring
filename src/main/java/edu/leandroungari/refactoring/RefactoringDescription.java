package edu.leandroungari.refactoring;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RefactoringDescription extends Refactoring {

	private String description;
	
	public RefactoringDescription(String type, String description) {
		super(type);
		
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		
		return this.toJSON();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String toJSON() {
		
		JSONObject obj = new JSONObject();
		obj.put("type", this.getType());
		
		obj.put("description", this.getDescription());
		
		return obj.toJSONString();
	}

	@Override
	public RefactoringDescription fromJSON(String json) throws ParseException {
		
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(json);
		
		String type = (String) obj.get("type");
		String description = (String) obj.get("description");
		
		return new RefactoringDescription(type, description);
	}
	
	
	
}
