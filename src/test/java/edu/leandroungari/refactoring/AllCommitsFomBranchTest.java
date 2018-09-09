package edu.leandroungari.refactoring;

import edu.leandroungari.refactoring.git.Branch;
import edu.leandroungari.refactoring.git.Commit;
import edu.leandroungari.refactoring.git.GitRepository;

/**
 * Hello world!
 *
 */
public class AllCommitsFomBranchTest {
	
    public static void main(String[] args) throws Exception {
    	
        
    	GitRepository rep = new GitRepository("junit5", "https://github.com/junit-team/junit5.git", "/home/leandroungari/teste-junit-5");
        
        for (Branch branch: rep.getBranches()) {
        	
        	System.out.println("Branch name: " + branch.getName());
        	
        	for (String commit: rep.getCommits(branch)) {
        		
        		System.out.println(commit);
        	}
        }	
        
        
    }
}