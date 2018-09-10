package edu.leandroungari.app;

import edu.leandroungari.refactoring.git.GitRepository;

public class Teste {

	public static void main(String[] args) {

		String base = "/home/leandroungari/Documents/Mestrado/";
		String folder = base + "repositorios/refactoring-toy-example";
		String url = "https://github.com/danilofes/refactoring-toy-example.git";

		try {
			GitRepository repo = new GitRepository("refactoring-toy-example", url, folder);
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
