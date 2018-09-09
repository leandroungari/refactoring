package edu.leandroungari.refactoring.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import edu.leandroungari.refactoring.Refactoring;
import refdiff.core.api.GitService;
import refdiff.core.util.GitServiceImpl;

public class GitRepository {

	private String repositoryName;
	
	private String gitClone;
	private String localFolder;

	private Repository repository;
	private ArrayList<Branch> branches;
	private ArrayList<String> commits;

	public GitRepository(String repositoryName, String gitClone, String localFolder) throws Exception {

		this.repositoryName = repositoryName;
		this.gitClone = gitClone;
		this.localFolder = localFolder;

		GitService gitService = new GitServiceImpl();
		this.repository = gitService.cloneIfNotExists(this.localFolder, this.gitClone);

		this.commits = new ArrayList<>();
		
		this.initialize();
	}

	private void initialize() throws GitAPIException {

		Git git = new Git(repository);
		RevWalk walk = new RevWalk(repository);

		List<Ref> branches = git.branchList().call();
		System.out.println("Number of branches:" + branches.size());

		this.branches = new ArrayList<>();
		for (Ref branch : branches) {

			this.branches.add(new Branch(branch.getName()));
		}

		git.close();
		walk.close();
	}

	public ArrayList<Branch> getBranches() {

		return this.branches;
	}
	
	public ArrayList<String> getCommits() throws MissingObjectException, IncorrectObjectTypeException, IOException, NoHeadException, GitAPIException {
		
		return this.getCommits(new Branch("refs/heads/master"));
	}

	public ArrayList<String> getCommits(Branch branch) throws MissingObjectException, IncorrectObjectTypeException, IOException, NoHeadException, GitAPIException {
		
		if (this.commits.isEmpty()) {
			
			Git git = new Git(repository);
			RevWalk walk = new RevWalk(repository);
			
			Iterable<RevCommit> commits = git.log().all().call();
		
			for (RevCommit commit : commits) {
				
				boolean foundInThisBranch = false;
				RevCommit targetCommit = walk.parseCommit(repository.resolve(commit.getName()));

				for (Map.Entry<String, Ref> e : repository.getAllRefs().entrySet()) {

					if (e.getKey().startsWith(Constants.R_HEADS)) {

						if (walk.isMergedInto(targetCommit, walk.parseCommit(e.getValue().getObjectId()))) {

							String foundInBranch = e.getValue().getName();

							if (branch.getName().equals(foundInBranch)) {
								foundInThisBranch = true;
								break;
							}
						}
					}
				}

				if (foundInThisBranch) {
					
					this.commits.add(commit.getName());
				}
			}

			walk.close();
			git.close();			
		}
		
		return this.commits;
	}


	public Repository getRepository() {
		return repository;
	}
	
	
	public void export(String basepath, HashMap<String, ArrayList<Refactoring>> table) throws MissingObjectException, IncorrectObjectTypeException, NoHeadException, IOException, GitAPIException {
				
		String path = basepath + this.repositoryName + "/data/";
		File baseFolder = new File(path);
		
		if (baseFolder.mkdirs()) {
			
			for(String m: this.getCommits()) {
				System.out.println("Generate file: " + path + m + ".json");
				
				Commit commit = new Commit(m);
				commit.setRefactorings(table.get(m));
				commit.write(path + m + ".json");
			}
		}
	}
	
	
	
}
