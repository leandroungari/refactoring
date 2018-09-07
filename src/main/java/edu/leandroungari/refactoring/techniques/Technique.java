package edu.leandroungari.refactoring.techniques;

import java.util.ArrayList;
import java.util.HashMap;

import edu.leandroungari.refactoring.Refactoring;
import edu.leandroungari.refactoring.git.Commit;
import edu.leandroungari.refactoring.git.GitRepository;

public abstract class Technique {
	
	public abstract HashMap<String, ArrayList<Refactoring>> getRefactorings(GitRepository git);
	
	public abstract ArrayList<Refactoring> getRefactorings(GitRepository git, String commitId);
}
