package edu.leandroungari.refactoring.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import edu.leandroungari.refactoring.Refactoring;
import edu.leandroungari.structure.CommitTreeFile;
import edu.leandroungari.structure.Directory;
import edu.leandroungari.structure.Register;
import refdiff.core.api.GitService;
import refdiff.core.util.GitServiceImpl;

public class GitRepository {

	private String repositoryName;

	private String gitClone;
	private String localFolder;

	private Repository repository;
	private ArrayList<String> commits;

	public GitRepository(String repositoryName, String gitClone, String localFolder) throws Exception {

		this.repositoryName = repositoryName;
		this.gitClone = gitClone;
		this.localFolder = localFolder;

		GitService gitService = new GitServiceImpl();
		this.repository = gitService.cloneIfNotExists(this.localFolder, this.gitClone);

		this.commits = new ArrayList<>();
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

	public void exportRefactorings(String basepath, HashMap<String, ArrayList<Refactoring>> table)
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

	public ArrayList<Register> listFilesPerCommit(String id) throws MissingObjectException, IOException {

		RevWalk revWalk = new RevWalk(repository);
		ObjectId commitId = ObjectId.fromString(id);
		
		RevCommit commit = revWalk.parseCommit(commitId);
		
		RevTree tree = commit.getTree();
		System.out.println("Having tree: " + tree);
		
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(false);
		
		ArrayList<Register> result = new ArrayList<>();
		
		while (treeWalk.next()) {
		    if (treeWalk.isSubtree()) {
		    	result.add(new Directory(treeWalk.getPathString()));
		        treeWalk.enterSubtree();
		    } else {
		        result.add(new edu.leandroungari.structure.File(treeWalk.getPathString()));
		    }
		}

		revWalk.close();
		treeWalk.close();
		
		return result;
	}

	
	public void exportRepositoryTrees(String basepath) throws MissingObjectException, IOException {
		
		String path = basepath + this.repositoryName + "/trees/";
		for(String commitId: this.getCommits()) {
			
			CommitTreeFile m = new CommitTreeFile(commitId, this.listFilesPerCommit(commitId));
			m.write(path + commitId + ".json");
		}
		
	}
	
}
