package edu.leandroungari.refactoring.techniques;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import edu.leandroungari.refactoring.git.GitRepository;

public class RefactoringMiner extends Technique {

	private GitHistoryRefactoringMiner miner;

	public RefactoringMiner() {
		super();

		this.miner = new GitHistoryRefactoringMinerImpl();
	}

	@Override
	public ArrayList<edu.leandroungari.refactoring.Refactoring> getRefactorings(GitRepository git, String commitId) {

		ArrayList<edu.leandroungari.refactoring.Refactoring> result = new ArrayList<>();

		try {

			miner.detectAll(git.getRepository(), "master", new RefactoringHandler() {
				@Override
				public void handle(RevCommit commitData, List<Refactoring> refactorings) {

					// System.out.println("Refactorings at " + commitData.getId().getName());
					if (commitData.getId().getName().equals(commitId)) {

						for (Refactoring ref : refactorings) {

							result.add(new edu.leandroungari.refactoring.Refactoring(commitId, ref.getName(),
									ref.toString()));
						}
					}
				}
			});

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public HashMap<String, ArrayList<edu.leandroungari.refactoring.Refactoring>> getRefactorings(GitRepository git) {

		HashMap<String, ArrayList<edu.leandroungari.refactoring.Refactoring>> table = new HashMap<>();

		try {

			miner.detectAll(git.getRepository(), "master", new RefactoringHandler() {
				@Override
				public void handle(RevCommit commitData, List<Refactoring> refactorings) {

					ArrayList<edu.leandroungari.refactoring.Refactoring> result = new ArrayList<>();
					String commitId = commitData.getId().getName();
					for (Refactoring ref : refactorings) {

						result.add(
								new edu.leandroungari.refactoring.Refactoring(
										commitId, 
										ref.getName(), 
										ref.toString()
								)
						);
					}
					
					table.put(commitId, result);

				}
			});

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return table;
	}
}
