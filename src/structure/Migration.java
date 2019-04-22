package structure;

import gumtreediff.matchers.MappingStore;
import gumtreediff.matchers.Matcher;
import gumtreediff.matchers.Matchers;
import gumtreediff.tree.TreeContext;

public class Migration {
	
	private TreeContext srcT;
	private TreeContext dstT;
	private MappingStore mappings;
	private String miName;
	
	public Migration(TreeContext tc1, TreeContext tc2, MappingStore mappings, String name) {
		this.srcT = tc1;
		this.dstT = tc2;
		this.mappings = mappings;
		this.miName = name;
	}

	public TreeContext getSrcT() {
		return srcT;
	}

	public TreeContext getDstT() {
		return dstT;
	}

	public MappingStore getMappings() {
		return mappings;
	}

	public String getMiName() {
		return miName;
	}

	

}
