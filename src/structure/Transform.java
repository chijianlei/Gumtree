package structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.checkerframework.checker.units.qual.s;

import gumtreediff.actions.model.Action;
import gumtreediff.tree.*;

public class Transform {
	
	private SubTree sTree;
	private SubTree dTree;
	private HashMap<Integer, Integer> subMap;
	private HashMap<String, ArrayList<Action>> actMap;
	private String miName;

	
	public Transform(SubTree st, SubTree dt, HashMap<Integer, Integer> map,
			HashMap<String, ArrayList<Action>> actions, String name) {
		sTree = st;
		dTree = dt;
		subMap = map;
		actMap = actions;
		miName = name;
	}
	
	public ITree getSRoot() {
		return sTree.getRoot();
	}
	
	public ITree getDRoot() {
		return dTree.getRoot();
	}		

	public SubTree getSTree() {
		return sTree;
	}

	public SubTree getDTree() {
		return dTree;
	}

	public TreeContext getSrcT() {
		return sTree.getTC();
	}

	public TreeContext getDstT() {
		return dTree.getTC();
	}
	
	public int getlineNum() {
		return sTree.getStNum();
	}

	public HashMap<Integer, Integer> getSubMap() {
		return subMap;
	}

	public HashMap<String, ArrayList<Action>> getActMap() {
		return actMap;
	}

	public String getMiName() {
		return miName;
	}


}
