package edu.leandroungari.refactoring.git;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
	
	public Commit(String name) {
		
		this.setName(name);
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
	
	@SuppressWarnings("unchecked")
	@Override
	public Commit fromJSON(String json) throws ParseException {
		
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(json);
		
		String name = (String) obj.get("name");
		String author = (String) obj.get("author");
		Date date = (Date) obj.get("date");
		String description = (String) obj.get("description");
		
		ArrayList<Refactoring> refactorings = new ArrayList<>();
		Iterator<JSONObject> iterator = ((ArrayList<JSONObject>) obj.get("refactorings")).iterator();
		
		while(iterator.hasNext()) {
			
			refactorings.add(Refactoring.extract(iterator.next()));
		}
		
		Commit m = new Commit(name);
		m.setAuthor(author);
		m.setDate(date);
		m.setDescription(description);
		return m;
	}

	@Override
	public void write(String filename) throws IOException {
		
		FileWriter writer = new FileWriter(new File(filename));
		
		writer.write(this.toString());
		writer.close();
	}

	@Override
	public void read(String filename) throws ParseException, IOException {
		
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		
		fis.close();
		
		Commit m = this.fromJSON(new String(data, "UTF-8"));
		this.setName(m.getName());
		this.setAuthor(m.getAuthor());
		this.setDate(m.getDate());
		this.setDescription(m.getDescription());
	}
}
