package edu.leandroungari.refactoring.git;

import java.util.Date;

public class Commit {

	
	private String name;
	private String author;
	private Date date;
	private String description;
	
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
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "\nCommit name: " + this.getName() +
			   "\nAuthor: " + this.getAuthor() +
			   "\nDate: " + this.getDate().toString() +
			   "\nDescription: " + this.getDescription() + "\n";
	}
}
