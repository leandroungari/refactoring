package edu.leandroungari.structure;

import java.util.ArrayList;

import org.json.simple.JSONObject;

public class Directory extends Register {

	private ArrayList<Register> list;
	
	public Directory(String value) {
		super(value);
		
		this.list = new ArrayList<>();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String toJSON() {
		
		JSONObject obj = new JSONObject();
		obj.put("type", this.getType());
		obj.put("value", this.getValue());
		obj.put("list", this.getList());
		
		return obj.toJSONString();
	}
	
	

	public ArrayList<Register> getList() {
		return list;
	}

	public void setList(ArrayList<Register> list) {
		this.list = list;
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
