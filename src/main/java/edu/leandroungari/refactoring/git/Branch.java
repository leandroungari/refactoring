package edu.leandroungari.refactoring.git;

public class Branch {

	private String branchName;
	
	public Branch(String name) {
		
		this.branchName = name;
		
	}
	
	public String getName() {
		
		return branchName;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "\nBranch: "+ this.getName() +"\n";
	}
}
