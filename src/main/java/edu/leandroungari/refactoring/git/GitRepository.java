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
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

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


	public ArrayList<String> getCommits() throws IOException {

		if (this.commits.isEmpty()) {

			Ref head = repository.exactRef("refs/heads/master");

			RevWalk revWalk = new RevWalk(repository);
			RevCommit commit = revWalk.parseCommit(head.getObjectId());			
			
			revWalk.markStart(commit);
			
			for (RevCommit rev: revWalk) {
				this.commits.add(rev.getName());
			}
			
			revWalk.close();
		}

		return this.commits;
	}

	public Repository getRepository() {
		return repository;
	}

	public void export(String basepath, HashMap<String, ArrayList<Refactoring>> table)
			throws MissingObjectException, IncorrectObjectTypeException, NoHeadException, IOException, GitAPIException {

		String path = basepath + this.repositoryName + "/data/";
		File baseFolder = new File(path);

		if (baseFolder.mkdirs()) {

			for (String m : this.getCommits()) {
				System.out.println("Generate file: " + path + m + ".json");

				Commit commit = new Commit(m);
				commit.setRefactorings(table.get(m));
				commit.write(path + m + ".json");
			}
		}
	}

	public void listFilesPerCommit(String id) throws MissingObjectException, IOException {

		RevWalk revWalk = new RevWalk(repository);
		ObjectId commitId = ObjectId.fromString(id);
		
		RevCommit commit = revWalk.parseCommit(commitId);
		
		RevTree tree = commit.getTree();
		System.out.println("Having tree: " + tree);
		
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(false);
		while (treeWalk.next()) {
		    if (treeWalk.isSubtree()) {
		        System.out.println("dir: " + treeWalk.getPathString());
		        treeWalk.enterSubtree();
		    } else {
		        System.out.println("file: " + treeWalk.getPathString());
		    }
		}

		revWalk.close();
		treeWalk.close();

	}
	
}
