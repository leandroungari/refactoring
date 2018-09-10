package edu.leandroungari.refactoring;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.leandroungari.json.Jsonable;

public abstract class Refactoring implements Jsonable<Refactoring> {

	private String type;

	public Refactoring(String type) {

		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public abstract String toString();

	@Override
	public Refactoring fromJSON(String json) throws ParseException {

		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(json);

		return Refactoring.extract(obj);
	}

	public static Refactoring extract(JSONObject obj) {
		
		String type = (String) obj.get("type");
		String description = (String) obj.get("description");

		if (description == null) {

			String initial = (String) obj.get("initial");
			String target = (String) obj.get("target");

			return new RefactoringTarget(type, initial, target);
		} 
		else return new RefactoringDescription(type, description);

	}
}
