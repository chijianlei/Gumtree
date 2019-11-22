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
	private String srcHash;
	private String dstHash;
	
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

	public String getSrcHash() {
		return srcHash;
	}

	public String getDstHash() {
		return dstHash;
	}

	public void setSrcHash(String srcHash) {
		this.srcHash = srcHash;
	}

	public void setDstHash(String dstHash) {
		this.dstHash = dstHash;
	}

}
