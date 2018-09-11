package edu.leandroungari.refactoring.techniques;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import edu.leandroungari.refactoring.Refactoring;
import edu.leandroungari.refactoring.git.Commit;
import edu.leandroungari.refactoring.git.GitRepository;

public abstract class Technique {

	public abstract void getRefactorings(GitRepository git, String basepath, String technique);

	public abstract ArrayList<Refactoring> getRefactorings(GitRepository git, String commitId, String basepath,
			String technique);

	public static void generateCommitFile(String path, String m,
			ArrayList<edu.leandroungari.refactoring.Refactoring> list) throws IOException {

		System.out.println("Generate file: " + path + m + ".json");

		Commit commit = new Commit(m);
		commit.setRefactorings(list);
		commit.write(path + m + ".json");

	}

}
