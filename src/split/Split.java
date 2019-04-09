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
import utils.Levenshtein;
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
			if(trans.isEmpty()) {
				wr.close();
				throw new Exception("trans null error!");
			}
				
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
					
				}else if(sugs.size()>5) {
					wr.close();
					throw new Exception("Map size error!");
				}
					
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
			HashMap<DTree, DTree> dtMap = new HashMap<>();
			dtMap = getDTmap(srcST, dstST, tf);	

			
			//find different kinds of actions
		
//			for(int i=0;i<sDTs.size();i++) {
//				DTree sDT = sDTs.get(i);
//				
//			}			
		}
	}
	
	public HashMap<DTree, DTree> getDTmap(SubTree srcST, SubTree dstST, Transform tf) throws Exception{
		System.out.println("----getDTmap----");
		TreeContext srcT = tf.getSrcT();
		TreeContext dstT = tf.getDstT();//单叶子节点，不同value，no matching情况
		HashMap<DTree, DTree> dtMap = new HashMap<>();
        HashMap<Integer, Integer> subMap = tf.getSubMap();
		HashMap<String, ArrayList<Action>> actMap = tf.getActMap();
		ArrayList<Action> updates = actMap.get("update");
		String miName = tf.getMiName();
		System.out.println("TF:"+miName);
		ArrayList<DTree> sDTs = getDwarfTrees(srcST);
		ArrayList<DTree> dDTs = getDwarfTrees(dstST);
		System.out.println("sDTsize:"+sDTs.size());
		System.out.println("dDTsize:"+dDTs.size());
		ArrayList<DTree> mappedsDTs = getDwarfTrees(srcST);
		ArrayList<DTree> mappeddDTs = getDwarfTrees(dstST);
		ArrayList<Integer> updIds = new ArrayList<Integer>();
		for(Action act : updates) {
			ITree node = act.getNode();
			int id = node.getId();
			updIds.add(id);
		}
		
		for(int i=0;i<sDTs.size();i++) {//Dwarf-tree level
			DTree sDT = sDTs.get(i);
			List<ITree> leaves = sDT.getLeaves();
			String sDTString = Utils.printLeaf(sDT);//因为结构原因，可能有部分root的子节点不是DT的叶子
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
						int dstId = leaf2.getId();
						
						if(leaves.size()==1&&leaves2.size()==1) {
							if(updIds.contains(srcId)&&dstId == subMap.get(srcId)) {
								tmpNum1++;
								continue;						
							}//If it contains a UPD action, it should be matched.
						}//In some cases, A DTree only has one leaf. It should be discussed separately.
						
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
//				if(sDT.getRoot().getId()==94) {
//					System.out.println(Utils.printDTree(sDT));
//					System.out.println("dDT:"+dDT.getRoot().getId()+Utils.printDTree(dDT));
//					System.out.println("sim:"+sim1+","+tmpNum1+","+sim2+","+tmpNum2);
//					if(candidateDT1!=null) {
//						System.out.println("CandidateId1:"+candidateDT1.getRoot().getId());
//					}
//					if(candidateDT2!=null) {
//						System.out.println("CandidateId2:"+candidateDT2.getRoot().getId());
//					}				
//				}
			}
			if(sim1==1) {
				if(leaves.size()==1&&candidateDT1.getLeaves().size()==1) {
					int srcId = leaves.get(0).getId();
					int candidateId = candidateDT1.getLeaves().get(0).getId();
					System.out.println("Size1id:"+srcId+","+candidateId);
					if(updIds.contains(srcId)) {
						System.out.println("dtMapsize:"+dtMap.size());
						String dstTString = Utils.printLeaf(candidateDT1);
						System.out.println(sDT.getRoot().getId());
						System.out.println("Change3:"+sDTString+"->"+dstTString);
						dtMap.put(sDT, candidateDT1);
						if(!(mappedsDTs.contains(sDT)||mappeddDTs.contains(candidateDT1))) {
							mappedsDTs.add(sDT);
							mappeddDTs.add(candidateDT1);
						}else if(mappedsDTs.contains(sDT)&&!mappeddDTs.contains(candidateDT1)) {
							throw new Exception("error situation");
						}else if(!mappedsDTs.contains(sDT)&&mappeddDTs.contains(candidateDT1)) {
							throw new Exception("error situation");
						}												
					}
				}
			}//In some cases, A DTree only has one leaf. It should be discussed separately.
			if(sim1>=0.3&&sim1!=1) {
				System.out.println("dtMapsize:"+dtMap.size());
				String dDTString = Utils.printLeaf(candidateDT1);
				System.out.println(sDT.getRoot().getId());
				System.out.println("Change1:"+sDTString+"->"+dDTString);
//				System.out.println("Map:"+sDT.getRoot().getId()+"->"+candidateDT1.getRoot().getId());
				if((dtMap.containsKey(sDT)&&!dtMap.containsValue(candidateDT1))) {
					throw new Exception("error candidateDT!");
				}else if(!dtMap.containsKey(sDT)&&dtMap.containsValue(candidateDT1)){
					throw new Exception("error sDT!");
				}else {
					dtMap.put(sDT, candidateDT1);
				}	
				if(!(mappedsDTs.contains(sDT)||mappeddDTs.contains(candidateDT1))) {
					mappedsDTs.add(sDT);
					mappeddDTs.add(candidateDT1);
				}else if(mappedsDTs.contains(sDT)&&!mappeddDTs.contains(candidateDT1)) {
					throw new Exception("error situation");
				}else if(!mappedsDTs.contains(sDT)&&mappeddDTs.contains(candidateDT1)) {
					throw new Exception("error situation");
				}
			}
			if(sim2==1) {
				String dDTString = Utils.printLeaf(candidateDT2);
				if(!sDTString.equals(dDTString)) {
					System.out.println("Change4:"+sDTString+"->"+dDTString);
					if((dtMap.containsKey(sDT)&&!dtMap.containsValue(candidateDT2))) {
						throw new Exception("error candidateDT!");
					}else if(!dtMap.containsKey(sDT)&&dtMap.containsValue(candidateDT2)){
						throw new Exception("error sDT!");
					}else {
						dtMap.put(sDT, candidateDT2);
					}
					if(!(mappedsDTs.contains(sDT)||mappeddDTs.contains(candidateDT2))) {
						mappedsDTs.add(sDT);
						mappeddDTs.add(candidateDT2);
					}else if(mappedsDTs.contains(sDT)&&!mappeddDTs.contains(candidateDT2)) {
						throw new Exception("error situation");
					}else if(!mappedsDTs.contains(sDT)&&mappeddDTs.contains(candidateDT2)) {
						throw new Exception("error situation");
					}
				}
				else {
					List<ITree> leaves2 = candidateDT2.getLeaves();
					Boolean ifMatch = false;
					for(int j=0;j<leaves.size();j++) {
						ITree srcNode = leaves.get(j);
						String sType = srcT.getTypeLabel(srcNode);
						String sValue = srcNode.getLabel();
						ITree dstNode = leaves2.get(j);
						String dType = dstT.getTypeLabel(dstNode);
						String dValue = dstNode.getLabel();
						if(sType.equals(dType)&&sValue.equals(dValue)) {
							ifMatch = true;
						}else
							ifMatch = false;
					}
					if(ifMatch==true) {
						mappedsDTs.add(sDT);
						mappeddDTs.add(candidateDT2);
					}
				}
			}//全部matching，若叶子完全相同，移除，不然是全部需要改的特殊情况
			if(sim2>=0.5&&sim2!=1) {
				String dstTString = Utils.printLeaf(candidateDT2);
				System.out.println("Change2:"+sDTString+"->"+dstTString);
//				System.out.println("Map:"+sDT.getRoot().getId()+"->"+candidateDT2.getRoot().getId());
				if((dtMap.containsKey(sDT)&&!dtMap.containsValue(candidateDT2))) {
					throw new Exception("error candidateDT!");
				}else if(!dtMap.containsKey(sDT)&&dtMap.containsValue(candidateDT2)){
					throw new Exception("error sDT!");
				}else {
					dtMap.put(sDT, candidateDT2);
				}
				if(!(mappedsDTs.contains(sDT)||mappeddDTs.contains(candidateDT2))) {
					mappedsDTs.add(sDT);
					mappeddDTs.add(candidateDT2);
				}else if(mappedsDTs.contains(sDT)&&!mappeddDTs.contains(candidateDT2)) {
					throw new Exception("error situation");
				}else if(!mappedsDTs.contains(sDT)&&mappeddDTs.contains(candidateDT2)) {
					throw new Exception("error situation");
				}
			}
		}		
		for(DTree dt : mappedsDTs) {
			sDTs.remove(dt);
		}
		for(DTree dt : mappeddDTs) {
			dDTs.remove(dt);
		}//unmapped DTrees
		
		for(DTree sDT: sDTs) {
			Boolean ifMatch = false;
			ITree sRoot = sDT.getRoot();
			List<ITree> leaves = sDT.getLeaves();
			for(ITree leaf : leaves) {
				int id = leaf.getId();
				if(subMap.containsKey(id)) {
					ifMatch = true;
				}
			}
			if(ifMatch==true)
				continue;//若是match上且不需要修改的DTree，不要记录直接continue
			String sRootType = srcT.getTypeLabel(sRoot);
			List<ITree> sPars = sRoot.getParents();
			String sParsString = sRootType;
			for(ITree tmpPar : sPars) {
				String parType = srcT.getTypeLabel(tmpPar);
				sParsString = sParsString + parType;
			}
			float sim = 0;
			String sDTString = Utils.printLeaf(sDT);
			DTree candidateDT = null;
			for(DTree dDT: dDTs) {
				float tmpSim = 0;
				ITree dRoot = dDT.getRoot();
				String dRootType = dstT.getTypeLabel(dRoot);
				if(sRootType.equals(dRootType)) {
					List<ITree> dPars = sRoot.getParents();
					String dParsString = sRootType;
					for(ITree tmpPar : dPars) {
						String parType = srcT.getTypeLabel(tmpPar);
						dParsString = dParsString + parType;
					}
					tmpSim = Levenshtein.getSimilarityRatio(sParsString, dParsString);
					if(tmpSim>sim) {
						candidateDT = dDT;
						sim = tmpSim;
					}
				}			
			}
			if(sim>0.5) {
				String dDTString = Utils.printLeaf(candidateDT);
				if(!sDTString.equals(dDTString)) {
					System.out.println("Change5:"+sDTString+"->"+dDTString);
//					System.out.println("Map:"+sDT.getRoot().getId()+"->"+candidateDT2.getRoot().getId());			
				}				
				if((dtMap.containsKey(sDT)&&!dtMap.containsValue(candidateDT))) {
					throw new Exception("error candidateDT!");
				}else if(!dtMap.containsKey(sDT)&&dtMap.containsValue(candidateDT)){
					throw new Exception("error sDT!");
				}else {
					dtMap.put(sDT, candidateDT);
				}
				if(!(mappedsDTs.contains(sDT)||mappeddDTs.contains(candidateDT))) {
					mappedsDTs.add(sDT);
					mappeddDTs.add(candidateDT);
				}else if(mappedsDTs.contains(sDT)&&!mappeddDTs.contains(candidateDT)) {
					throw new Exception("error situation");
				}else if(!mappedsDTs.contains(sDT)&&mappeddDTs.contains(candidateDT)) {
					throw new Exception("error situation");
				}
			}
		}
		for(DTree dt : mappedsDTs) {
			sDTs.remove(dt);
		}
		for(DTree dt : mappeddDTs) {
			dDTs.remove(dt);
		}//unmapped DTrees
		
		System.out.println("sDTSize:"+sDTs.size());
		System.out.println("dDTSize:"+dDTs.size());
		for(DTree sDT: sDTs) {
			ITree sRoot = sDT.getRoot();
			String sRootType = srcT.getTypeLabel(sRoot);
			String sDTString = Utils.printLeaf(sDT);
			Boolean ifDEL = true;
			for(DTree dDT: dDTs) {
				ITree dRoot = dDT.getRoot();
				String dRootType = dstT.getTypeLabel(dRoot);
				if(sRootType.equals(dRootType)) {
					ifDEL = false;
					throw new Exception("why not matching?"+sDTString);
				}else
					continue;					
			}
			if(ifDEL&&!sDTString.equals("")) {
				System.out.println("Del:"+sDTString);
			}				
		}
		for(DTree dDT: dDTs) {
			ITree dRoot = dDT.getRoot();
			String dRootType = dstT.getTypeLabel(dRoot);
			String dDTString = Utils.printLeaf(dDT);
			Boolean ifADD = true;
			for(DTree sDT: sDTs) {
				ITree sRoot = sDT.getRoot();
				String sRootType = srcT.getTypeLabel(sRoot);
				if(sRootType.equals(dRootType)) {
					ifADD = false;
					throw new Exception("why not matching?"+dDTString);
				}else
					continue;
			}
			if(ifADD&&!(dDTString.equals(""))) {
				System.out.println("ADD:"+dDTString);
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
				while(parType.equals("name")) {
					par = par.getParent();
					parType = tc.getTypeLabel(par);
				}//name或operator的parent不能为name					
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
			Matcher m = Matchers.getInstance().getMatcher(tc1.getRoot(), tc2.getRoot());
	        m.match();
	        MappingStore mappings = m.getMappings();
			Pruning pt = new Pruning(tc1, tc2, mappings);
			pt.pruneTree();//Prune the ContextTree in order to get accurate matching rules.
			Migration mi = new Migration(tc1, tc2, mappings, dir.getName()+".cpp");
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
			MappingStore mappings = migrat.getMappings();
			
//			ArrayList<SubTree> sub2 = splitSubTree(dstT, miName);
//	        double similarity = Similarity.getSimilarity(sub1.get(0), sub2.get(0));
//	        System.out.println("sime:"+similarity);//testing			
			ArrayList<Transform> singleTrans = splitTransform(srcT, dstT, mappings, miName);
			trans.addAll(singleTrans);
			System.out.println("TransSize:"+singleTrans.size());
		}	
	}
	

	
	public ArrayList<Transform> splitTransform(TreeContext srcT, TreeContext dstT, MappingStore mappings, String miName) throws Exception {
		System.out.println("Analyse:"+miName);
		ArrayList<Transform> trans = new ArrayList<>();
		ArrayList<SubTree> actSubTree = new ArrayList<>();
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
