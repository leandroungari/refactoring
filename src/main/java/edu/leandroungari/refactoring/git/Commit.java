package edu.leandroungari.refactoring.git;

import java.util.ArrayList;
import java.util.Date;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import edu.leandroungari.json.IO;
import edu.leandroungari.json.Jsonable;
import edu.leandroungari.refactoring.Refactoring;

public class Commit implements Jsonable<Commit>, IO{

	
	private String name;
	private String author;
	private Date date;
	private String description;
	
	private ArrayList<Refactoring> refactorings;
	
	public Commit(String name, String author, Date date, String description) {
		
		this.setName(name);
		this.setAuthor(author);
		this.setDate(date);
		this.setDescription(description);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	
	
	public ArrayList<Refactoring> getRefactorings() {
		return refactorings;
	}

	public void setRefactorings(ArrayList<Refactoring> refactorings) {
		this.refactorings = refactorings;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.toJSON();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String toJSON() {
		
		JSONObject obj = new JSONObject();
		obj.put("name", this.getName());
		obj.put("author", this.getAuthor());
		obj.put("date", this.getDate());
		obj.put("description", this.getDescription());
		obj.put("refactorings", this.getRefactorings());
		
		return obj.toJSONString();
	}

	@Override
	public Commit fromJSON(String json) throws ParseException {
		
		return null;
	}
}
