package edu.leandroungari.app;

import edu.leandroungari.refactoring.Refactoring;
import edu.leandroungari.refactoring.git.Branch;
import edu.leandroungari.refactoring.git.Commit;
import edu.leandroungari.refactoring.git.GitRepository;
import edu.leandroungari.refactoring.techniques.RefDiff;

public class App {

	public static void main(String[] args) {
		
		String folder = "/home/leandroungari/teste-jersey";
		String url = "https://github.com/jersey/jersey.git";
		
		try {
		
			GitRepository repo = new GitRepository(url, folder);
			
			System.out.println(repo.getCommits().get(1).getDescription());
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
