package edu.leandroungari.refactoring;

import edu.leandroungari.json.Jsonable;

public abstract class Refactoring implements Jsonable<Refactoring>{

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
}
