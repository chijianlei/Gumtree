package test;

import java.util.ArrayList;

import gumtreediff.matchers.MappingStore;

public class test1 {

	public static void main(String args[]) throws Exception{
		String test = "MappingStore mappings = m.getMappings()";
		String test1 = "mappings";
		if(test.contains(test1))
			System.out.println(true);
		else
			System.out.println(false);
		
	}
}
