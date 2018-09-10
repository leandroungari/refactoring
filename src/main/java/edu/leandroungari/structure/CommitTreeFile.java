package edu.leandroungari.structure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.leandroungari.json.IO;
import edu.leandroungari.json.Jsonable;

public class CommitTreeFile implements Jsonable<CommitTreeFile>, IO{

	private String commitId;
	private ArrayList<Register> list;
	
	public CommitTreeFile(String id, ArrayList<Register> list) {
		
		this.commitId = id;
		this.list = list;
	}
	
	
	public String getCommitId() {
		return commitId;
	}
	public void setCommitId(String commitId) {
		this.commitId = commitId;
	}
	public ArrayList<Register> getList() {
		return list;
	}
	public void setList(ArrayList<Register> list) {
		this.list = list;
	}


	@Override
	public void write(String filename) throws IOException {
		
		FileWriter writer = new FileWriter(new File(filename));
		
		writer.write(this.toString());
		writer.close();
	}

	@Override
	public void read(String filename) throws ParseException, IOException {
		
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		
		fis.close();
		
		CommitTreeFile m = this.fromJSON(new String(data, "UTF-8"));
		this.setCommitId(m.getCommitId());
		this.setList(m.getList());
	}


	@SuppressWarnings("unchecked")
	@Override
	public String toJSON() {
		
		JSONObject obj = new JSONObject();
		obj.put("commitId", this.getCommitId());
		obj.put("list", this.getList());
		
		return obj.toJSONString();
	}


	@SuppressWarnings("unchecked")
	@Override
	public CommitTreeFile fromJSON(String json) throws ParseException {
		
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(json);
		
		String commitId = (String) obj.get("commitId");
		ArrayList<Register> list = (ArrayList<Register>) obj.get("list");
		
		return new CommitTreeFile(commitId, list);
	}
	
	@Override
	public String toString() {
	
		return this.toJSON();
	}
	
	
}
