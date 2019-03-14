package cost_matrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes.Name;


public class Node {

	private int Num;
	private int typeNume;
	private String Name;
	private ArrayList<Node> Parent = new ArrayList<Node>();
	private ArrayList<Node> Child = new ArrayList<Node>();
	private Map<Integer, Double> inMap = new HashMap<Integer, Double>();
	private Map<Integer, Double> outMap = new HashMap<Integer, Double>();
	
	public int getNum(){
		return this.Num;
	}
	
	public void setNum(int Num){
		this.Num=Num;
	}
	
	public int getTypeNume() {
		return typeNume;
	}

	public void setTypeNume(int typeNume) {
		this.typeNume = typeNume;
	}

	public String getName(){
		return this.Name;
	}
	
	public void setName(String Name){
		this.Name=Name;
	}
	
	public ArrayList<Node> getParent(){
		return this.Parent;
	}
	
	public void setParent(Node Parent){
		this.Parent.add(Parent);
	}
	
	public ArrayList<Node> getChild(){
		return this.Child;
	}
	
	public void setChild(Node Child){
		this.Child.add(Child);
	}
	
	public Map<Integer, Double> getinMap(){
		return this.inMap;
	}
	
	public void setinMap(int in, double weight){
		this.inMap.put(in, weight);
	}
	
	public Map<Integer, Double> getoutMap(){
		return this.outMap;
	}
	
	public void setoutMap(int out, double weight){
		this.outMap.put(out, weight);
	}
}
