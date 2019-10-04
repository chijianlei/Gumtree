package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.matchers.Mapping;
import gumtreediff.matchers.MappingStore;
import gumtreediff.matchers.Matcher;
import gumtreediff.matchers.Matchers;
import gumtreediff.tree.ITree;
import gumtreediff.tree.TreeContext;
import gumtreediff.tree.TreeUtils;
import split.Split;
import structure.SubTree;
import utils.Output;
import utils.Similarity;

public class SubtreeTest {
	
	public static void main (String args[]) throws Exception{
		String path1 = "talker.cpp";
		File cppfile = new File(path1);
		TreeContext tc1 = new SrcmlCppTreeGenerator().generateFromFile(cppfile);
		String miName = path1.split("/")[path1.split("/").length-1];//标记文件名
//		String path2 = "talker2.cpp";
//		File cppfile2 = new File(path2);
//		TreeContext tc2 = new SrcmlCppTreeGenerator().generateFromFile(cppfile2);
		Split sp = new Split();		
//		Matcher m = Matchers.getInstance().getMatcher(tc1.getRoot(), tc2.getRoot());
//        m.match();
//        MappingStore mappings = m.getMappings();
        ArrayList<SubTree> sts1 = sp.splitSubTree(tc1, miName);
//        ArrayList<SubTree> sts2 = sp.splitSubTree(tc2, miName);
//        BufferedWriter wr = new BufferedWriter(new FileWriter(new File("mapping.txt")));
//        for(Mapping map : mappings) {
//        	ITree src = map.getFirst();
//        	ITree dst = map.getSecond();
//        	wr.append(src.getId()+"->"+dst.getId());
//        	wr.newLine();
////        	System.out.println("Mapping:"+src.getId()+"->"+dst.getId());
////        	if(dst.getId()==553) {
////        		System.err.println("find it!"+src.getId());
////        	}
//        }
//        wr.close();
//        System.out.println("Msize:"+mappings.asSet().size());
        for(SubTree st : sts1) {
        	ITree root = st.getRoot();
        	System.out.println("StID:"+root.getId()+":"+root.getLine()+","+root.getColumn()+
        			"->"+root.getLastLine()+","+root.getLastColumn()+" Len:"+root.getLength());
    		List<ITree> des = root.getDescendants();
        	for(ITree node : des) {
        		System.out.println(node.getId()+":"+node.getLine()+","+node.getColumn()+
        				"->"+node.getLastLine()+","+node.getLastColumn()+" Len:"+node.getLength());
        	}
//        	if(root.getId()==41) {
//        		for(ITree node : root.postOrder()) {
//        			System.out.println("ID:"+node.getId());
//        		}
//        		List<ITree> des = root.getDescendants();
//            	for(ITree node : des) {
//            		System.out.println(node.getId());
//            		ITree dst = mappings.getDst(node);
//            		if(dst!=null)
//            		System.out.println(node.getId()+"->"+dst.getId());
//            	}
//        	}      	       		       	
        }
//        System.out.println("-----");
//        for(SubTree st : sts2) {
//        	ITree root = st.getRoot();
//        	String dst = Output.subtree2src(st);
////        	System.out.println(dst);   	       		       	
//        }
	}
	
	public void breakBlock() throws Exception {
		String path = "talker.cpp";
		File cppfile = new File(path);
		TreeContext tc1 = new SrcmlCppTreeGenerator().generateFromFile(cppfile);
		Split sp = new Split();
		ArrayList<SubTree> sts = sp.splitSubTree(tc1, path);
		System.out.println(sts.size());
		for(SubTree st : sts) {			
			ITree sRoot = st.getRoot();
			if(tc1.getTypeLabel(sRoot).equals("while")) {
				String srcTree = Similarity.transfer2string(st);
				System.out.println(srcTree);
				List<ITree> list = TreeUtils.preOrder(st.getRoot());
				for(ITree tmp : list) {
					String type = tc1.getTypeLabel(tmp);
					if(type.equals("block")) {
						List<ITree> childs = sRoot.getChildren();
						System.out.println(childs.size());
						childs.remove(tmp);
						System.out.println(sRoot.getChildren().size());
						sRoot.setChildren(childs);
						tmp.setParent(null);//断开所有block node和父亲的连接											
					}										
				}
				String srcTree1 = Similarity.transfer2string(st);
				System.out.println(srcTree1);
			}			
		}
	}

}
