package edu.leandroungari.refactoring;

import java.util.ArrayList;

import edu.leandroungari.refactoring.git.Branch;
import edu.leandroungari.refactoring.git.Commit;
import edu.leandroungari.refactoring.git.GitRepository;

public class RefactoringsFromCommitTest {

	public static void main(String[] args) throws Exception {

		GitRepository rep = new GitRepository("clojure","https://github.com/refdiff-data/clojure.git",
				"/home/leandroungari/teste-clojure");

		for (Branch branch : rep.getBranches()) {

			System.out.println("Branch name: " + branch.getName());
			
			ArrayList<String> commits = rep.getCommits(branch);
			
			/*System.out.println("Number of commits: " + commits.size());
			for (Commit commit : commits) {

				//System.out.println(commit);
			}*/
			
			System.out.println(commits.get(0));
		}

	}
}
