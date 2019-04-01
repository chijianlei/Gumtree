package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.matchers.MappingStore;
import gumtreediff.matchers.Matcher;
import gumtreediff.matchers.Matchers;
import gumtreediff.tree.ITree;
import gumtreediff.tree.TreeContext;
import gumtreediff.tree.TreeUtils;
import split.Split;
import structure.DTree;
import structure.SubTree;
import utils.Pruning;
import utils.Similarity;

public class Test {
	
	public static void main (String args[]) throws Exception{
		String path = "talker.cpp";
		File cppfile = new File(path);
		TreeContext tc1 = new SrcmlCppTreeGenerator().generateFromFile(cppfile);
		ITree root1 = tc1.getRoot();
		String path2 = "talker2.cpp";
		File cppfile2 = new File(path2);
		TreeContext tc2 = new SrcmlCppTreeGenerator().generateFromFile(cppfile2);
		ITree root2 = tc2.getRoot();
        Matcher m = Matchers.getInstance().getMatcher(tc1.getRoot(), tc2.getRoot());
        m.match();
        MappingStore mappings = m.getMappings();
		Pruning pr = new Pruning(tc1, tc2, mappings);
		pr.pruneTree();
	} 


}
