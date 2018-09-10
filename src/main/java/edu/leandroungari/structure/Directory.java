package edu.leandroungari.structure;

import org.json.simple.JSONObject;

public class Directory extends Register {

	public Directory(String value) {
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
		
		return "directory";
	}

	@Override
	public String toString() {
		
		return this.toJSON();
	}

}
