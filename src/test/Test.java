package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.tree.ITree;
import gumtreediff.tree.TreeContext;
import gumtreediff.tree.TreeUtils;
import split.Split;
import structure.SubTree;
import utils.Similarity;

public class Test {
	
	public static void main (String args[]) throws Exception{
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
