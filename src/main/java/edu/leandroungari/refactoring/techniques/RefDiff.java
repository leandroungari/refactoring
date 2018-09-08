package edu.leandroungari.refactoring.techniques;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;

import edu.leandroungari.refactoring.Refactoring;
import edu.leandroungari.refactoring.RefactoringTarget;
import edu.leandroungari.refactoring.git.Commit;
import edu.leandroungari.refactoring.git.GitRepository;
import refdiff.core.rm2.model.refactoring.SDRefactoring;

public class RefDiff extends Technique {

	private refdiff.core.RefDiff refDiff;
	
	public RefDiff() {
		super();
		
		this.refDiff = new refdiff.core.RefDiff();
	}
	
	@Override
	public ArrayList<Refactoring> getRefactorings(GitRepository git, String commitId) {
		
		List<SDRefactoring> refactorings = this.refDiff.detectAtCommit(git.getRepository(), commitId);

		ArrayList<Refactoring> result = new ArrayList<>();
		for (SDRefactoring r : refactorings) {

			result.add(
					new RefactoringTarget(
							r.getRefactoringType().getDisplayName(), 
							r.getEntityBefore().key().toString(), 
							r.getEntityAfter().key().toString()
					)
			);
		}
		
		try {
		
			git.getCommit(commitId).setRefactorings(result);
		
		} catch (IOException | GitAPIException e) {
			
			e.printStackTrace();
		}
		
		return result;
	}

	@Override
	public HashMap<String, ArrayList<Refactoring>> getRefactorings(GitRepository git) {
		
		HashMap<String, ArrayList<Refactoring>> table = new HashMap<>();
		
		try {
			
			for(Commit m: git.getCommits()) {
				
				ArrayList<Refactoring> r = this.getRefactorings(git, m.getName());
				table.put(m.getName(), r);
				m.setRefactorings(r);
			}
			
		} catch (IOException | GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return table;
	}

	
}
