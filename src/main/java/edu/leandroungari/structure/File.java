package edu.leandroungari.structure;

import org.json.simple.JSONObject;

public class File extends Register {

	public File(String value) {
		super(value);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String toJSON() {
		
		JSONObject obj = new JSONObject();
		obj.put("type", this.getType());
		obj.put("value", this.getValue());
		
		return obj.toJSONString();
	}

	@Override
	public String getType() {
		
		return "file";
	}

	@Override
	public String toString() {

		return this.toJSON();
	}

}
