package edu.leandroungari.app;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;

import edu.leandroungari.refactoring.git.GitRepository;
import edu.leandroungari.refactoring.techniques.RefDiff;
import edu.leandroungari.refactoring.techniques.RefactoringMiner;

public class App {

	public static void main(String[] args) {
		
		
		App app = new App();
		app.getRefactorings("jersey", "https://github.com/jersey/jersey.git");
		app.treeFiles("jersey", "https://github.com/jersey/jersey.git");
	}
	
	public void treeFiles(String name, String url) {
		
		String base = "/home/leandroungari/Documents/Mestrado/";
		String folder = base + "repositorios/" + name;

		final String PATH = base + "dados/";

		try {
			
			GitRepository repo = new GitRepository(name, url, folder);
			
			repo.exportRepositoryTrees(PATH);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void getRefactorings(String name, String url) {
		
		String base = "/home/leandroungari/Documents/Mestrado/";
		String folder = base + "repositorios/" + name;

		final String PATH = base + "dados/";
		
		try {
			
			GitRepository repo = new GitRepository(name, url, folder);
			
			this.refminer(PATH, repo);
			this.refdiff(PATH, repo);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void refminer(String PATH, GitRepository repo) throws MissingObjectException, IncorrectObjectTypeException, NoHeadException, IOException, GitAPIException {
		
		RefactoringMiner miner = new RefactoringMiner();
		miner.getRefactorings(repo, PATH, "miner");
	}

	private void refdiff(String PATH, GitRepository repo) throws MissingObjectException, IncorrectObjectTypeException, NoHeadException, IOException, GitAPIException {
		
		RefDiff refdiff = new RefDiff();
		refdiff.getRefactorings(repo, PATH, "refdiff");
	}
}
