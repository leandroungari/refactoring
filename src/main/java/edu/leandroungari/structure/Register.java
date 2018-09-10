package edu.leandroungari.structure;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.leandroungari.json.Jsonable;

public abstract class Register implements Jsonable<Register>{
	
	protected String value;
	
	public Register(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public Register fromJSON(String json) throws ParseException {

		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(json);
		
		String type = (String) obj.get("type");
		String value = (String) obj.get("value");
		
		if (type.equals("directory")) return new Directory(value);
		else return new File(value);
	}
	
	public abstract String getType();
	
	public abstract String toString();
}
