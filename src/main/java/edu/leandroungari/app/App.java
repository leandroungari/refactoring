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
			
			for (Branch branch: repo.getBranches()) {
				
				System.out.println(branch);
			}
			
			Commit m = repo.getCommit("d94ca2b27c9e8a5fa9fe19483d58d2f2ef024606");
			System.out.println(m);
			
			RefDiff ref = new RefDiff();
			
			for (Refactoring r: ref.getRefactorings(repo, m.getName())) {
				System.out.println(r);
			}
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
