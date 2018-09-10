package edu.leandroungari.app;

import edu.leandroungari.refactoring.git.GitRepository;
import edu.leandroungari.refactoring.techniques.RefDiff;
import edu.leandroungari.refactoring.techniques.RefactoringMiner;

public class App {

	public static void main(String[] args) {
		
		String base = "/home/leandroungari/Documents/Mestrado/";
		String folder = base + "repositorios/refactoring-toy-example";
		String url = "https://github.com/junit-team/junit4.git";
		
		try {
		
			final String PATH = base + "dados/examples-miner/";
			
			RefactoringMiner miner = new RefactoringMiner();
			
			GitRepository repo = new GitRepository("junit4", url, folder);
			repo.export(PATH, miner.getRefactorings(repo));
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		try {
		
			final String PATH = "/home/leandroungari/examples-refdiff/";
			
			//RefactoringMiner miner = new RefactoringMiner();
			RefDiff refdiff = new RefDiff();
			
			GitRepository repo = new GitRepository("junit4", url, folder);
			repo.export(PATH, refdiff.getRefactorings(repo));
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
