package edu.leandroungari.refactoring;

import org.json.simple.JSONObject;

public class RefactoringTarget extends Refactoring{


	private String initialEntity;
	private String targetEntity;
	
	public RefactoringTarget(String type, String initialEntity, String targetEntity) {
		super(type);
		
		this.initialEntity = initialEntity;
		this.targetEntity = targetEntity;
	}
	
	public String getInitialEntity() {
		return initialEntity;
	}
	public void setInitialEntity(String initialEntity) {
		this.initialEntity = initialEntity;
	}
	public String getTargetEntity() {
		return targetEntity;
	}
	public void setTargetEntity(String targetEntity) {
		this.targetEntity = targetEntity;
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
		
		obj.put("initial", this.getInitialEntity());
		obj.put("target", this.getTargetEntity());
		
		return obj.toJSONString();
	}
}
