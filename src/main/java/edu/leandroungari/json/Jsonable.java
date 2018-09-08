package edu.leandroungari.json;

import org.json.simple.parser.ParseException;

public interface Jsonable <T> {

	String toJSON();
	T fromJSON(String json) throws ParseException;
}
