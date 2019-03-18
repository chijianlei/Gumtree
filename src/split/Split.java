package split;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.tools.javah.Util;

import gumtreediff.actions.model.Action;
import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.matchers.Mapping;
import gumtreediff.matchers.MappingStore;
import gumtreediff.matchers.Matcher;
import gumtreediff.matchers.Matchers;
import gumtreediff.tree.ITree;
import gumtreediff.tree.TreeContext;
import gumtreediff.tree.TreeUtils;
import nodecluster.Cluster;
import structure.DTree;
import structure.Migration;
import structure.SubTree;
import structure.Transform;
import utils.Similarity;
import utils.Utils;

public class Split {
	private ArrayList<Transform> trans = new ArrayList<>();
	private int count = 0;
	
	public static void main (String args[]) throws Exception{
		Split sp = new Split();
		String path = "migrations_test";
		ArrayList<Migration> migrats = new ArrayList<>();
		migrats = sp.readMigration(path, "talker");
		sp.storeTrans(migrats);
		sp.splitSnippets();
		
//		String inputPath = "talker.cpp";
//		sp.suggestion(inputPath);
	}
	
	public void suggestion(String input) throws Exception {//每个statement给5个suggestion		
		File cppfile = new File(input);
//		String miName = input.split("/")[input.split("/").length-1];//标记文件名
		String miName = input.split("\\\\")[input.split("\\\\").length-1];//标记文件名
		TreeContext inputT = new SrcmlCppTreeGenerator().generateFromFile(cppfile);
		ArrayList<SubTree> sts = splitSubTree(inputT, miName);
		System.out.println("subSize:"+sts.size());
		System.out.println("allTranSize:"+trans.size());
//		String output = input.substring(0, input.length()-4)+".txt";
		String output = miName.substring(0, miName.length()-4)+".txt";
		BufferedWriter wr = new BufferedWriter(new FileWriter(new File(output)));
		for(SubTree st : sts) {
			HashMap<Transform, Double> sugs = new HashMap<>();
			String inputTree = Similarity.transfer2string(st);
			if(trans.isEmpty())
				throw new Exception("trans null error!");
			for(int i=0;i<trans.size();i++) {
				Transform ts = trans.get(i);				
				SubTree tmpSt = ts.getSTree();
//				String tmpTree = Similarity.transfer2string(tmpSt);
//				System.out.println(inputTree);
//				System.out.println(tmpTree);
				double sim = Similarity.getSimilarity(st, tmpSt);
				if(sugs.size()<5) {
					sugs.put(ts, sim);
				}else if(sugs.size()==5) {
					Transform delTs = null;
					Double delSim = 1.0;
					for(Map.Entry<Transform, Double> entry : sugs.entrySet()) {
						Transform candiTs = entry.getKey();
						Double candiSim = entry.getValue();
						if(candiSim<sim&&candiSim<delSim) {//找到五个suggestion中sim最小的扔了
							delSim = candiSim;
							delTs = candiTs;							
						}
					}
					if(delTs!=null) {
//						System.out.println("putin:"+sim);
						sugs.remove(delTs, delSim);
						sugs.put(ts, sim);					
					}
					
				}else if(sugs.size()>5)
					throw new Exception("Map size error!");
			}
//			System.out.println("====================================");
			for(Map.Entry<Transform, Double> entry : sugs.entrySet()) {
				Transform candiTs = entry.getKey();
				SubTree sTree = candiTs.getSTree();
				SubTree dTree = candiTs.getDTree();
				String stringSTree = Similarity.transfer2string(sTree);
				String stringDTree = "null";
				if(dTree!=null)
					stringDTree = Similarity.transfer2string(dTree);				
				Double candiSim = entry.getValue();
				int lineNum = sTree.getStNum();
				String name = sTree.getMiName();
				wr.append(stringSTree+"\n");
				wr.append(stringDTree+"\n");
				wr.append(inputTree+"\n");
				wr.append(candiSim+","+lineNum+","+name+"\n");
				wr.flush();
//				System.out.println(stringSTree);
//				System.out.println(stringDTree);
//				System.out.println(inputTree);
//				System.out.println(candiSim+","+lineNum+","+name);				
			}
			wr.append(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			wr.newLine();	
			wr.flush();
//			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		}
		wr.close();
	}
	
	public void splitSnippets() throws Exception {//finer-grained split statement into type snippets		
		for(Transform tf : trans) {			
			SubTree srcST = tf.getSTree();
			SubTree dstST = tf.getDTree();
	        HashMap<Integer, Integer> subMap = tf.getSubMap();
			HashMap<String, ArrayList<Action>> actMap = tf.getActMap();
			String miName = tf.getMiName();
			System.out.println("TF:"+miName);
			ArrayList<DTree> srcDTs = getDwarfTrees(srcST);
			ArrayList<DTree> dstDTs = getDwarfTrees(dstST);
			HashMap<String, ArrayList<DTree>> sDTmap = new HashMap<>();
			HashMap<String, ArrayList<DTree>> dDTmap = new HashMap<>();
			for(DTree dt : srcDTs) {
				String typeName = dt.getRootType();
				System.out.println("sDT:"+Utils.printDTree(dt));
				if(!sDTmap.containsKey(typeName)) {
					ArrayList<DTree> dts = new ArrayList<>();
					dts.add(dt);
					sDTmap.put(typeName, dts);
				}else 
					sDTmap.get(typeName).add(dt);								
			}
			for(DTree dt : dstDTs) {
				String typeName = dt.getRootType();
				System.out.println("dDT:"+Utils.printDTree(dt));
				if(!dDTmap.containsKey(typeName)) {
					ArrayList<DTree> dts = new ArrayList<>();
					dts.add(dt);
					dDTmap.put(typeName, dts);
				}else 
					dDTmap.get(typeName).add(dt);								
			}//put into type categories
			
			//find three kinds of actions
			HashMap<DTree, DTree> dtMap = new HashMap<>();
			for(Map.Entry<String, ArrayList<DTree>> entry : sDTmap.entrySet()) {
				String typeName = entry.getKey();
				ArrayList<DTree> sDTs = entry.getValue();
				if(!dDTmap.containsKey(typeName)) {//find Del actions, DT only in src, not dst
					for(DTree sDT : sDTs) {
						String leaves = Utils.printLeaf(sDT);
						System.out.println("Del:"+ leaves);
					}									
				}else {//search change actions
					ArrayList<DTree> dDTs = dDTmap.get(typeName);
					dtMap = getDTmap(sDTs, dDTs, subMap);
				}							
			}			
			for(Map.Entry<String, ArrayList<DTree>> entry : dDTmap.entrySet()) {
				String typeName = entry.getKey();
				ArrayList<DTree> dDTs = entry.getValue();
				if(!sDTmap.containsKey(typeName)) {//DT only in dst, not src
					for(DTree dST : dDTs) {
						String leaves = Utils.printLeaf(dST);
						System.out.println("Add:"+ leaves);
					}	
				}
			}
		}
	}
	
	public HashMap<DTree, DTree> getDTmap(ArrayList<DTree> sDTs, ArrayList<DTree> dDTs, HashMap<Integer, Integer> subMap) throws Exception{
		HashMap<DTree, DTree> dtMap = new HashMap<>();
		for(DTree sDT : sDTs) {//Dwarf-tree level
			List<ITree> leaves = sDT.getLeaves();
			String srcTString = Utils.printLeaf(sDT);//因为结构原因，可能有部分root的子节点不是DT的叶子			
			int size1 = leaves.size();
			for(int i=0;i<dDTs.size();i++) {
				DTree dDT = dDTs.get(i);
				List<ITree> leaves2 = dDT.getLeaves();
				int size2 = leaves2.size();
				
				//two ways of similarity
				int mapNum1 = 0;
				int mapNum2 = 0;
				for(ITree leaf : leaves) {
					int srcId = leaf.getId();
					String value = leaf.getLabel();//maybe some leaves do not have values
					for(ITree leaf2 : leaves2) {
						String value2 = leaf2.getLabel();
						if(value2.equals(value)) {
							mapNum1++;
							break;
						}//search for equivalent value.						
					}
					for(ITree leaf2 : leaves2) {
						int dstId = leaf2.getId();
						if(subMap.get(srcId)!=null) {
							if(dstId==subMap.get(srcId)) {//search mapping
								mapNum2++;
								break;
							}
						}						
					}
					double sim1 = (2*mapNum1)/(size1+size2);
					double sim2 = (2*mapNum2)/(size1+size2);
					if(sim1>0.5||sim2>0.5) {
						String dstTString = Utils.printLeaf(dDT);
						if(sim1>0.5&&sim1!=1) {
							System.out.println("Change1:"+srcTString+"->"+dstTString);
							dtMap.put(sDT, dDT);
							dDTs.remove(dDT);
//							break;//可能有问题
						}
						if(sim2>0.5&&sim1!=1) {
							System.out.println("Change2:"+srcTString+"->"+dstTString);
							dtMap.put(sDT, dDT);
							dDTs.remove(dDT);
//							break;//可能有问题
						}
					}								
				}
			}
		}
		return null;		
	}
	
	public ArrayList<DTree> getDwarfTrees(SubTree st) throws Exception{
		ITree root = st.getRoot();
		TreeContext tc = st.getTC();
		if(root.getHeight()<=2)
			throw new Exception("error subtree, plz check!");		
		List<ITree> leaves = new ArrayList<>();		
		leaves = Utils.traverse2Leaf(root, leaves);
		ArrayList<DTree> dwarfTrees = new ArrayList<>();
		HashMap<ITree, ArrayList<ITree>> parMap = new HashMap<>();
		for(ITree leaf : leaves) {
			ITree par = leaf.getParent();
			if(par==null)
				throw new Exception("error par!!");
			if(parMap.get(par)==null) {
				ArrayList<ITree> leafList = new ArrayList<>();
				leafList.add(leaf);
				parMap.put(par, leafList);
			}else {
				parMap.get(par).add(leaf);
			}											
		}
		
		for(Map.Entry<ITree, ArrayList<ITree>> entry : parMap.entrySet()) {
			ITree par = entry.getKey();
			ArrayList<ITree> leafList = entry.getValue();
			if(leafList.size()>0) {
				DTree dt = new DTree(par, leafList, tc);
				dwarfTrees.add(dt);
			}else 
				throw new Exception("error par!!");			
		}				
		return dwarfTrees;
	}//sub-subtree whose height is 2
	
	public ArrayList<Migration> readMigration(String path, String input) throws Exception {
		ArrayList<Migration> migrats = new ArrayList<>();
		File rootFile = new File(path);
		File[] dirs = rootFile.listFiles();	
		for(File dir : dirs) {
			if(dir.getName().equals(input)) {
				System.out.println("Skip File:"+dir.getName());
				continue;
			}				
			System.out.println("Reading File:"+dir.getName());
//			Thread.sleep(1000);
			File[] pair = dir.listFiles();
			if(pair.length!=2)
				throw new Exception("error pair! "+dir.getName());
			File srcFile = pair[0];
			File dstFile = pair[1];
			TreeContext tc1 = new SrcmlCppTreeGenerator().generateFromFile(srcFile);
			TreeContext tc2 = new SrcmlCppTreeGenerator().generateFromFile(dstFile);
			Migration mi = new Migration(tc1, tc2, dir.getName()+".cpp");
			migrats.add(mi);
		}
		System.out.println("Migration size:"+migrats.size());
		return migrats;
	}
	
	public void storeTrans(ArrayList<Migration> migrats) throws Exception {
		for(Migration migrat : migrats) {
			String miName = migrat.getMiName();
			TreeContext srcT = migrat.getSrcT();
			TreeContext dstT = migrat.getDstT();
			ArrayList<SubTree> sub1 = splitSubTree(srcT, miName);				
			
//			ArrayList<SubTree> sub2 = splitSubTree(dstT, miName);
//	        double similarity = Similarity.getSimilarity(sub1.get(0), sub2.get(0));
//	        System.out.println("sime:"+similarity);//testing			
			ArrayList<Transform> singleTrans = splitTransform(sub1, srcT, dstT, miName);
			trans.addAll(singleTrans);
			System.out.println("TransSize:"+singleTrans.size());
		}	
	}
	
	public ArrayList<Transform> splitTransform(ArrayList<SubTree> tList, TreeContext srcT, TreeContext dstT, String miName) throws Exception {
		System.out.println("Analyse:"+miName);
		ArrayList<Transform> trans = new ArrayList<>();
		ArrayList<SubTree> actSubTree = new ArrayList<>();
		ArrayList<SubTree> sub2 = splitSubTree(dstT, miName);
		Matcher m = Matchers.getInstance().getMatcher(srcT.getRoot(), dstT.getRoot());
        m.match();
        MappingStore mappings = m.getMappings();
		HashMap<String, LinkedList<Action>> actions = Utils.collectAction(srcT, dstT);		
//		HashMap<SubTree, Integer> st2lineNum = new HashMap<>();				
		LinkedList<Action> updates = actions.get("update");
		LinkedList<Action> deletes = actions.get("delete");
		LinkedList<Action> inserts = actions.get("insert");
		LinkedList<Action> moves = actions.get("move");	
		ArrayList<Integer> srcActIds = Utils.collectSrcActNodeIds(srcT, dstT, actions);
//		System.out.println("IdNum:"+srcActIds.size());
		Cluster cl = new Cluster(srcT, dstT);
		for(SubTree st : tList) {
			ITree t = st.getRoot();
			List<ITree> nodeList = new ArrayList<>();
			nodeList = Utils.collectNode(t, nodeList);
        	for(ITree node : nodeList) {
        		int id = node.getId();
        		if(srcActIds.contains(id)) {
        			actSubTree.add(st);
//        			System.out.println("find a action subtree!");
        			break;
        		}
        	}
		}//先找包含action的subtree
//		for(int j=0;j<updates.size();j++) {
//			Action a = updates.get(j);
//			ITree node = a.getNode();
//			int id = node.getId();
//			System.out.println("UPDid:"+id);
//		}
		
        for(int i=0;i<actSubTree.size();i++) {
        	SubTree st = actSubTree.get(i);
        	ITree t = st.getRoot();
        	ArrayList<Action> subUpdates = new ArrayList<>();
        	ArrayList<Action> subDeletes = new ArrayList<>();
        	ArrayList<Action> subInserts = new ArrayList<>();
        	ArrayList<Action> subMoves = new ArrayList<>();
        	List<ITree> nodeList = new ArrayList<>();
        	nodeList = Utils.collectNode(t, nodeList);
//        	System.out.println("nodelistSize:"+nodeList.size());
        	HashMap<String, ArrayList<Action>> subActions = new HashMap<>();
        	
        	for(int j=0;j<updates.size();j++) {
        		Action a = updates.get(j);
    			ITree node = a.getNode();
    			int id = node.getId();
//    			System.out.println("UPDid:"+id);
    			for(ITree tmp : nodeList) {
//    				System.out.println(id+":"+tmp.getId());
    				if(tmp.getId()==id) {
//    					System.out.println("map:"+tmp.getId());
        				subUpdates.add(a);
//        				updates.remove(a);
        			}
    			}
    		} 
        	
    		for(int j=0;j<deletes.size();j++) {
    			Action a = deletes.get(j);
    			ITree node = a.getNode();
    			int id = node.getId();
    			for(ITree tmp : nodeList) {
    				if(tmp.getId()==id) {
        				subDeletes.add(a);
//        				deletes.remove(a);
        			}
    			}
    			
    		}   		
    		for(int j=0;j<inserts.size();j++) {	
    			Action a = inserts.get(j);
    			ITree sRoot = cl.traverseSRoot(a);			
    			if(nodeList.contains(sRoot)) {
    				subInserts.add(a);
//    				inserts.remove(a);
    			}
    		}   		
    		for(int j=0;j<moves.size();j++) {
    			Action a = moves.get(j);
    			ITree sRoot = cl.findMovRoot(a);			
    			if(nodeList.contains(sRoot)) {
    				subMoves.add(a);
//    				moves.remove(a);
    			}
    		}
//    		System.out.println("subupdsize:"+subUpdates.size());    		
//    		System.out.println("subdelsize:"+subDeletes.size());  		
//    		System.out.println("subaddsize:"+subInserts.size());  		
//    		System.out.println("submovsize:"+subMoves.size());
//    		System.out.println("--------------------------------");
    		subActions.put("update", subUpdates);
    		subActions.put("delete", subDeletes);
    		subActions.put("insert", subInserts);
    		subActions.put("move", subMoves);
    		
    		ITree srcStRoot = st.getRoot();  		
    		ITree dstStRoot = mappings.getDst(srcStRoot);
    		SubTree dstSt = null;
    		if(dstStRoot==null) {
    			System.out.println("SID:"+srcStRoot.getId()); //发现有整颗srcSr删除的情况 
    		}else {
    			for(SubTree st2 : sub2) {
    				ITree root = st2.getRoot();
    				if(root.equals(dstStRoot)) {
                		dstSt = st2;
                		break;
    				}      			
    			}
    		}   			       			     		       			
    		List<ITree> nodes = TreeUtils.preOrder(st.getRoot());
    		HashMap<Integer, Integer> subMap = new HashMap<>();
    		for(ITree src : nodes) {
    			ITree dst = mappings.getDst(src);
    			if(dst!=null) {
    				subMap.put(src.getId(), dst.getId());
    			}
    		}//Mapping between srcId and dstId.
    		Transform tf = new Transform(st, dstSt, subMap, subActions, miName);
    		trans.add(tf);
        }      
		System.out.println("updsize:"+updates.size());
		System.out.println("delsize:"+deletes.size());
		System.out.println("addsize:"+inserts.size());
		System.out.println("movsize:"+moves.size());
		System.out.println("stSize:"+tList.size());
		return trans;      
	}
	
	public ArrayList<SubTree> splitSubTree(TreeContext tc, String miName) {//split subtree from AST tree
		count = 0;
		ITree totalRoot = tc.getRoot();
		List<ITree> orderList = TreeUtils.preOrder(totalRoot);
		ArrayList<SubTree> subRootList = new ArrayList<>();
		for(ITree t : orderList) {
			String typeLabel = tc.getTypeLabel(t);
			if(Utils.ifSRoot(typeLabel)) {
				ITree subRoot = t;
				List<ITree> list = TreeUtils.preOrder(subRoot);
				for(ITree tmp : list) {
					String type = tc.getTypeLabel(tmp);
					if(type.equals("block")) {
						subRoot.getChildren().remove(tmp);//断开父亲和所有block node的连接	
						tmp.setParent(null);//是否需要断开block node跟父亲的连接呢?	
					}									
				}
				SubTree st = new SubTree(subRoot, tc, count, miName);
				subRootList.add(st);
				count++;
			}//if是否考虑typeLabel=="return"?			
		}
//		System.out.println("subTreeNum:"+count);
		return subRootList;
	}
	
	public void getSize() {
		System.out.println(trans.size());
		System.out.println(count);
	}
	
}
