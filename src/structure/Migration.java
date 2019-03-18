package structure;

import gumtreediff.tree.TreeContext;

public class Migration {
	
	private TreeContext srcT;
	private TreeContext dstT;
	private String miName;
	
	public Migration(TreeContext tc1, TreeContext tc2, String name) {
		srcT = tc1;
		dstT = tc2;
		miName = name;
	}

	public TreeContext getSrcT() {
		return srcT;
	}

	public TreeContext getDstT() {
		return dstT;
	}

	public String getMiName() {
		return miName;
	}


}
