package edu.leandroungari.refactoring.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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

import refdiff.core.api.GitService;
import refdiff.core.util.GitServiceImpl;

public class GitRepository {

	private String gitClone;
	private String localFolder;

	private Repository repository;
	private ArrayList<Branch> branches;
	private ArrayList<Commit> commits;

	public GitRepository(String gitClone, String localFolder) throws Exception {

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
	
	public ArrayList<Commit> getCommits() throws MissingObjectException, IncorrectObjectTypeException, IOException, NoHeadException, GitAPIException {
		
		return this.getCommits(new Branch("refs/heads/master"));
	}

	public ArrayList<Commit> getCommits(Branch branch) throws MissingObjectException, IncorrectObjectTypeException, IOException, NoHeadException, GitAPIException {
		
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
					
					this.commits.add(new Commit(commit.getName(), commit.getAuthorIdent().getName(), new Date(commit.getCommitTime() * 1000L), commit.getFullMessage()));
				}
			}

			walk.close();
			git.close();			
		}
		
		return this.commits;
	}
	
	public Commit getCommit(String name) throws MissingObjectException, IncorrectObjectTypeException, NoHeadException, IOException, GitAPIException {
		
		return this.getCommit(name, new Branch("refs/heads/master"));
	}

	public Commit getCommit(String name, Branch branch) throws MissingObjectException, IncorrectObjectTypeException, NoHeadException, IOException, GitAPIException {
		
		for (Commit m : this.getCommits(branch)) {
			if (m.getName().equals(name)) return m;
		}
		
		return null; 
	}

	public Repository getRepository() {
		return repository;
	}
	
	
	
	
}
