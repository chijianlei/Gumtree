package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.matchers.MappingStore;
import gumtreediff.tree.TreeContext;

public class test1 {

	public static void main(String args[]) throws Exception{
		String path = "spring-webflow/src/main/java/org/springframework/webflow/mvc/view/AbstractMvcView.java";
		String repoName = path.split("//")[0];
		System.out.println(repoName);
	}
	
		


}
