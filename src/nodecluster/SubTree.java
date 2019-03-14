package nodecluster;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class SubTree {
	
	private ITree root;
	private TreeContext tc;
	private int stNum;
	private String miName;
	
	public SubTree(ITree node, TreeContext context, int count, String name) {
		root = node;
		tc = context;
		stNum = count;
		miName = name;
	}

	public ITree getRoot() {
		return root;
	}

	public TreeContext getTC() {
		return tc;
	}

	public int getStNum() {
		return stNum;
	}

	public String getMiName() {
		return miName;
	}


}
