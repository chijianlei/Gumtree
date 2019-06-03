package test;

import java.io.File;
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
import utils.Similarity;

public class SubtreeTest {
	
	public static void main (String args[]) throws Exception{
		String path = "talker.cpp";
		File cppfile = new File(path);
		TreeContext tc1 = new SrcmlCppTreeGenerator().generateFromFile(cppfile);
		String path2 = "talker2.cpp";
		File cppfile2 = new File(path2);
		TreeContext tc2 = new SrcmlCppTreeGenerator().generateFromFile(cppfile2);		
//		HashMap<String, LinkedList<Action>> actions = collectAction(tc1, tc2);
//		printAllActions(tc1, tc2, actions);
		String miName = path2.split("/")[path2.split("/").length-1];//标记文件名
		Split sp = new Split();
		ArrayList<SubTree> sts1 = sp.splitSubTree(tc1, miName);
		ArrayList<SubTree> sts2 = sp.splitSubTree(tc2, miName);
		System.out.println(sts1.size());
		System.out.println(sts2.size());
		Matcher m = Matchers.getInstance().getMatcher(sts1.get(0).getRoot(), sts2.get(0).getRoot());
        m.match();
        MappingStore mappings = m.getMappings();
        for(Mapping map : mappings) {
        	ITree src = map.getFirst();
        	ITree dst = map.getSecond();
        	System.out.println("Mapping:"+src.getId()+"->"+dst.getId());
        }
        
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
