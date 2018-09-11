package edu.leandroungari.refactoring.techniques;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.leandroungari.refactoring.Refactoring;
import edu.leandroungari.refactoring.RefactoringTarget;
import edu.leandroungari.refactoring.git.GitRepository;
import refdiff.core.rm2.model.refactoring.SDRefactoring;

public class RefDiff extends Technique {

	private refdiff.core.RefDiff refDiff;
	
	public RefDiff() {
		super();
		
		this.refDiff = new refdiff.core.RefDiff();
	}
	
	@Override
	public ArrayList<Refactoring> getRefactorings(GitRepository git, String commitId, String basepath, String technique) {
		
		System.out.println("Processing refactorings from commit: " + commitId);
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
			
		return result;
	}

	@Override
	public void getRefactorings(GitRepository git, String basepath, String technique) {
		
		String value = basepath + git.getRepositoryName() + "/refactorings/" + technique + "/";
		File folder = new File(value);
		if (folder.mkdirs()) {
		
			try {
				
				ArrayList<Refactoring> list;
				for(String m: git.getCommits()) {
					
					list = this.getRefactorings(git, m, basepath, technique);
					Technique.generateCommitFile(value, m, list);
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	
}
