package edu.leandroungari.refactoring;

public class Refactoring {

	private String type;
	private String commitId;
	
	private String initialEntity;
	private String targetEntity;
	
	private String description;
	
	
	
	public Refactoring(String commitId, String type, String initialEntity, String targetEntity) {
	
		this.commitId = commitId;
		this.type = type;
		
		this.initialEntity = initialEntity;
		this.targetEntity = targetEntity;
		
		this.description = null;
	}
	
	public Refactoring(String commitId, String type, String description) {
	
		this.commitId = commitId;
		this.type = type;
		
		this.description = description;
	}
	
	public String getCommitId() {
		
		return commitId;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getInitialEntity() {
		return initialEntity;
	}
	
	public void setInitialEntity(String initialEntity) {
		this.initialEntity = initialEntity;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTargetEntity() {
		return targetEntity;
	}
	
	public void setTargetEntity(String targetEntity) {
		this.targetEntity = targetEntity;
	}
	
	
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		if (this.description != null) {
			return "\nTipo: " + this.getType() + 
				   "\nDescrição: " + this.getDescription() + "\n";
		}
		else return "\nTipo: " + this.getType() + 
			        "\nInicial: " + this.getInitialEntity() +
			        "\nFinal: " + this.getTargetEntity() + "\n";
	}
}
