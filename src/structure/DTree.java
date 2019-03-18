package structure;

import java.util.List;

import gumtreediff.tree.ITree;
import gumtreediff.tree.TreeContext;

public class DTree {
	
	private ITree root;
	private List<ITree> leaves;
	private TreeContext treeContext;
	
	public DTree(ITree root, List<ITree> leaves, TreeContext treeContext) {
		this.root = root;
		this.leaves = leaves;
		this.treeContext = treeContext;		
	}

	public ITree getRoot() {
		return root;
	}
	
	public String getRootType() {
		String type = treeContext.getTypeLabel(root);
		return type;
	}

	public List<ITree> getLeaves() {
		return leaves;
	}

	public TreeContext getTreeContext() {
		return treeContext;
	}

}
