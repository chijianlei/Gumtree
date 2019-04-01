package split;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
	public ArrayList<Transform> trans = new ArrayList<>();
	private int count = 0;
	
	public static void main (String args[]) throws Exception{
		Split sp = new Split();
		String path = "migrations_test";
		ArrayList<Migration> migrats = new ArrayList<>();
		migrats = sp.readMigration(path, "");
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
			ArrayList<DTree> sDTs = getDwarfTrees(srcST);
			ArrayList<DTree> dDTs = getDwarfTrees(dstST);
			
			//find different kinds of actions
			HashMap<DTree, DTree> dtMap = new HashMap<>();
			dtMap = getDTmap(sDTs, dDTs, subMap);			
//			for(int i=0;i<sDTs.size();i++) {
//				DTree sDT = sDTs.get(i);
//				
//			}
			

		}
	}
	
	public HashMap<DTree, DTree> getDTmap(ArrayList<DTree> sDTs, ArrayList<DTree> dDTs, HashMap<Integer, Integer> subMap) throws Exception{
		HashMap<DTree, DTree> dtMap = new HashMap<>();
		for(int i=0;i<sDTs.size();i++) {//Dwarf-tree level
			DTree sDT = sDTs.get(i);
			List<ITree> leaves = sDT.getLeaves();
			String srcTString = Utils.printLeaf(sDT);//因为结构原因，可能有部分root的子节点不是DT的叶子	
			int size1 = leaves.size();
			double sim1 = 0.0;
			double sim2 = 0.0;
			DTree candidateDT1 = null;
			DTree candidateDT2 = null;
			for(int j=0;j<dDTs.size();j++) {
				DTree dDT = dDTs.get(j);
				List<ITree> leaves2 = dDT.getLeaves();
				int size2 = leaves2.size();
				
				//two ways of similarity
				int tmpNum1 = 0;
				int tmpNum2 = 0;
				for(ITree leaf : leaves) {
					int srcId = leaf.getId();
					String value = leaf.getLabel();//maybe some leaves do not have values
					for(ITree leaf2 : leaves2) {
						String value2 = leaf2.getLabel();
						if(value2.equals(value)) {
							tmpNum1++;
							break;
						}//search for equivalent value.						
					}
					for(ITree leaf2 : leaves2) {
						int dstId = leaf2.getId();
						if(subMap.get(srcId)!=null) {
							if(dstId==subMap.get(srcId)) {//search mapping
								tmpNum2++;
								break;
							}
						}						
					}								
				}
				double tmpSim1 = (2.0*tmpNum1)/(size1+size2);
				double tmpSim2 = (2.0*tmpNum2)/(size1+size2);
				if(sim1<tmpSim1) {
					candidateDT1 = dDT;
					sim1=tmpSim1;
				}	
				if(sim2<tmpSim2) {
					candidateDT2 = dDT;
					sim2=tmpSim2;
				}					
				if(sDT.getRoot().getId()==2337) {
					System.out.println("dDT:"+dDT.getRoot().getId());
					System.out.println("sim:"+sim1+","+tmpNum1+","+sim2+","+tmpNum2);	
				}
			}
			if(sim1>=0.5&&sim1!=1) {
				System.out.println(dtMap.size());
				String dstTString = Utils.printLeaf(candidateDT1);
				System.out.println(sDT.getRoot().getId());
				System.out.println("Change1:"+srcTString+"->"+dstTString);
				if(sDT.getRoot().getId()==2314) {
				System.out.println("sim:"+sim1+","+sim2);	
			    }
//				System.out.println("Map:"+sDT.getRoot().getId()+"->"+candidateDT1.getRoot().getId());
				if((dtMap.containsKey(sDT)&&!dtMap.containsValue(candidateDT1))||
						((!dtMap.containsKey(sDT)&&dtMap.containsValue(candidateDT1)))) {
					throw new Exception("not identify mapping!");
				}else {
					dtMap.put(sDT, candidateDT1);
				}				
//				dDTs.remove(candidateDT1);
//				break;//可能有问题
			}				
			if(sim2>=0.5&&sim2!=1) {
				String dstTString = Utils.printLeaf(candidateDT2);
				System.out.println("Change2:"+srcTString+"->"+dstTString);
//				System.out.println("Map:"+sDT.getRoot().getId()+"->"+candidateDT2.getRoot().getId());
				if((dtMap.containsKey(sDT)&&!dtMap.containsValue(candidateDT2))||
						(!(dtMap.containsKey(sDT)&&dtMap.containsValue(candidateDT2)))) {
					throw new Exception("not identify mapping!");
				}else {
					dtMap.put(sDT, candidateDT2);
				}
//				dDTs.remove(candidateDT2);
//				break;//可能有问题
			}
			if(sDT.getRoot().getId()==2335) {
				System.out.println("sim:"+sim1+","+sim2);
				System.out.println("size:"+sDT.getLeaves().size());
			}
		}
		return dtMap;		
	}//Matching sDTs and dDTs
	
	public ArrayList<DTree> getDwarfTrees(SubTree st) throws Exception{
		ITree root = st.getRoot();
		TreeContext tc = st.getTC();
		if(root.getHeight()<=2)
			throw new Exception("error subtree, plz check!");		
		List<ITree> leaves = new ArrayList<>();		
		leaves = Utils.traverse2Leaf(root, leaves);
		ArrayList<DTree> dwarfTrees = new ArrayList<>();
		HashMap<ITree, ArrayList<ITree>> parMap = new HashMap<>();
		for(int i=0;i<leaves.size();i++) {
			ITree leaf = leaves.get(i);
			String type = tc.getTypeLabel(leaf);			
			ITree par = leaf.getParent();
			String parType = tc.getTypeLabel(par);
			if(type.equals("argument_list"))
				continue;//"argument_list"的叶子节点不包含任何信息，还可能扰乱匹配
			//leaf nodes have many situations, need to consider one by one.
			else if(type.equals("name")||type.equals("operator")) {
				if(parType.equals("name"))
					par = par.getParent();
			}//考虑name DTree发现的特殊情况, 不然多个name leaf的par跟单个name leaf会匹配异常
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
			Stack<String> trace = new Stack<String>();
			ITree tmp = par;
			String type = tc.getTypeLabel(tmp);
			while(!Utils.ifSRoot(type)) {
				trace.push(type);
				tmp = tmp.getParent();
				type = tc.getTypeLabel(tmp);
			}
			ArrayList<ITree> leafList = entry.getValue();
			if(leafList.size()>0) {
				DTree dt = new DTree(par, leafList, trace, tc);
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
			
//			ArrayList<SubTree> sub2 = splitSubTree(dstT, miName);
//	        double similarity = Similarity.getSimilarity(sub1.get(0), sub2.get(0));
//	        System.out.println("sime:"+similarity);//testing			
			ArrayList<Transform> singleTrans = splitTransform(srcT, dstT, miName);
			trans.addAll(singleTrans);
			System.out.println("TransSize:"+singleTrans.size());
		}	
	}
	

	
	public ArrayList<Transform> splitTransform(TreeContext srcT, TreeContext dstT, String miName) throws Exception {
		System.out.println("Analyse:"+miName);
		ArrayList<Transform> trans = new ArrayList<>();
		ArrayList<SubTree> actSubTree = new ArrayList<>();
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
		ArrayList<SubTree> sub1 = splitSubTree(srcT, miName);//Subtree中割裂过block,注意
		ArrayList<SubTree> sub2 = splitSubTree(dstT, miName);//先计算action,再split ST
		for(SubTree st : sub1) {
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
		System.out.println("stSize:"+sub1.size());
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
