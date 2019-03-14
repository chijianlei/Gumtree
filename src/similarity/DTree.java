package similarity;

import java.util.List;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class DTree {
	
	private ITree root;
	private List<ITree> children;
	private TreeContext treeContext;
	
	public DTree(ITree root, List<ITree> children, TreeContext treeContext) {
		this.root = root;
		this.children = children;
		this.treeContext = treeContext;		
	}

	public ITree getRoot() {
		return root;
	}

	public List<ITree> getChildren() {
		return children;
	}

	public TreeContext getTreeContext() {
		return treeContext;
	}

}
