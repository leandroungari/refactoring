package edu.leandroungari.json;

import java.io.IOException;

import org.json.simple.parser.ParseException;

public interface IO {
	
	void write(String filename) throws IOException;
	void read(String filename) throws ParseException, IOException;
}
