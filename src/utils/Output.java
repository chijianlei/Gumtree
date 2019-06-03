package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import gumtreediff.actions.model.Action;
import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.matchers.MappingStore;
import gumtreediff.matchers.Matcher;
import gumtreediff.matchers.Matchers;
import gumtreediff.tree.ITree;
import gumtreediff.tree.TreeContext;
import gumtreediff.tree.TreeUtils;
import nodecluster.Cluster;
import split.Split;
import structure.Migration;
import structure.SubTree;
import structure.Transform;

public class Output {

	public static void main (String args[]) throws Exception{
		String path = args[0];
//		String path = "migrations_test";
//		Output.collectTokens(sp.trans);
//		Output.collectChangePairs(path, "");
//		Output.collectDefUse(path, "", "");
//		String path = "talker.cpp";
		Output.tokensFromInputFile(path);
	}
	
	public static void tokensFromInputFile(String path) throws Exception {
		Split sp = new Split();
//		DelComment.clearComment(path);
//		DelComment.clearInclude(path);
		File output = new File("src-test.txt");
		File outLine = new File("lineNum.txt");
		BufferedWriter wr = new BufferedWriter(new FileWriter(output));
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(outLine));
		File cppFile = new File(path);
		TreeContext tc = new SrcmlCppTreeGenerator().generateFromFile(cppFile);
		String cppName = cppFile.getName();	
		System.out.println("Reading file: "+cppName);
		ArrayList<SubTree> sub1 = sp.splitSubTree(tc, cppName);//Subtree中割裂过block,注意
		int lastLineNum = 0;
		for(SubTree srcT : sub1) {
			if(srcT!=null) {
				if(srcT.getTC()!=null) {					
					String src = subtree2src(srcT);
					wr.append(src);
					wr.newLine();
					wr.flush();
					ArrayList<Integer> nums = locateLineNum(srcT, path);
					int candidate = 0;
					if(nums.size()>1) {	
						int diffValue = Integer.MAX_VALUE;
						for(int tmp : nums) {
							int tmpdiff = Math.abs(tmp-lastLineNum);
							if(tmpdiff<diffValue) {
								candidate = tmp;
								diffValue = tmpdiff;
							}
						}
					}else if(nums.size()==1) {
						candidate = nums.get(0);
					}else {
						wr.close();	
						wr1.close();
						throw new Exception("Why no lineNum???");
					}						
					wr1.append(String.valueOf(candidate));
					wr1.newLine();
					wr1.flush();
					lastLineNum = candidate;
				}							
			}
		}
		wr.close();	
		wr1.close();
	}
	
	public static void collectChangePairs(String path, String filter) throws Exception {
		Split sp = new Split();
		ArrayList<Migration> migrats = new ArrayList<>();
		migrats = sp.readMigration(path, filter);
		sp.storeTrans(migrats);
		String out = "change.txt";
		String out1 = "src-train.txt";
		String out2 = "dst-train.txt";
		BufferedWriter wr = new BufferedWriter(new FileWriter(new File(out)));
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(new File(out1)));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(new File(out2)));
		for(Transform tf : sp.trans) {
			System.out.println("===============================");
			SubTree srcT = tf.getSTree();
			SubTree dstT = tf.getDTree();
			if(srcT!=null&&dstT!=null) {
				if(srcT.getTC()!=null&&dstT.getTC()!=null) {
					String src = subtree2src(srcT);				
					String dst = subtree2src(dstT);
					if(!(src.contains("error situation")||dst.contains("error situation"))) {
//						System.out.println(src);
						wr.append(src+"\t");
						wr1.append(src);
						wr1.newLine();
						wr1.flush();
//						System.out.println(dst);
						wr.append(dst);
						wr.newLine();
						wr.flush();
						wr2.append(dst);
						wr2.newLine();
						wr2.flush();
					}	
				}							
			}
		}
		wr.close();
		wr1.close();
		wr2.close();
	}
	
	public static void collectTokens(ArrayList<Transform> trans) throws Exception {
		String out1 = "srcDiffToken.txt";
		String out2 = "dstDiffToken.txt";
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(new File(out1)));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(new File(out2)));
		for(Transform tf : trans) {
			System.out.println("===============================");
			SubTree srcT = tf.getSTree();
			SubTree dstT = tf.getDTree();
			if(srcT!=null&&dstT!=null) {
				List<ITree> leaves1 = new ArrayList<ITree>();
				List<ITree> leaves2 = new ArrayList<ITree>();
				if(srcT.getTC()!=null&&dstT.getTC()!=null) {
					leaves1 = Utils.traverse2Leaf(srcT.getRoot(), leaves1);				
					leaves2 = Utils.traverse2Leaf(dstT.getRoot(), leaves2);	
					for(int i=0;i<leaves1.size();i++) {
						ITree leaf = leaves1.get(i);
						if(leaf.getLabel()!=null) {
							wr1.append(leaf.getLabel());
							if(i!=leaves1.size()-1)
								wr1.append(" ");
						}
					}
					wr1.append("\t");
//					wr1.newLine();
//					wr1.flush();
					for(int i=0;i<leaves2.size();i++) {
						ITree leaf = leaves2.get(i);
						if(leaf.getLabel()!=null) {
							wr1.append(leaf.getLabel());
							if(i!=leaves2.size()-1)
								wr1.append(" ");
						}
					}
					wr1.newLine();
					wr1.flush();
//					wr2.append(s2);
//					wr2.newLine();
//					wr2.flush();
				}							
			}
		}
		wr1.close();
		wr2.close();
	}
	
	public static void collectDefUse(String path, String mode, String filter) throws Exception {//获取DefUse	
		Split sp = new Split();		
		ArrayList<Migration> migrats = sp.readMigration(path, filter);
		File output = new File("defuse.txt");
		File output1 = new File("src-val.txt");
		File output2 = new File("tgt-val.txt");
		BufferedWriter wr = new BufferedWriter(new FileWriter(output));
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(output1));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(output2));
		for(Migration migrat : migrats) {
			HashMap<String, ArrayList<Integer>> sDefMap = new HashMap<String, ArrayList<Integer>>();
			HashMap<String, ArrayList<Integer>> dDefMap = new HashMap<String, ArrayList<Integer>>();
			HashMap<Integer, SubTree> stMap = new HashMap<Integer, SubTree>();
			HashMap<Integer, SubTree> dtMap = new HashMap<Integer, SubTree>();
			String miName = migrat.getMiName();
			TreeContext sTC = migrat.getSrcT();
			TreeContext dTC = migrat.getDstT();
			MappingStore mappings = migrat.getMappings();
			
			System.out.println("Analyse:"+miName);
			Matcher m = Matchers.getInstance().getMatcher(sTC.getRoot(), dTC.getRoot());
	        m.match();
			ArrayList<SubTree> changedSTree = new ArrayList<>();
			HashMap<String, LinkedList<Action>> actions = Utils.collectAction(sTC, dTC, mappings);					
			ArrayList<Integer> srcActIds = Utils.collectSrcActNodeIds(sTC, dTC, actions);
			ArrayList<SubTree> sub1 = sp.splitSubTree(sTC, miName);//Subtree中割裂过block,注意
			ArrayList<SubTree> sub2 = sp.splitSubTree(dTC, miName);//先计算action,再split ST

			if(mode.equals("allDef")) {//allDef输出全集即可
				changedSTree = sub1;
			}else {
				for(SubTree st : sub1) {
					ITree t = st.getRoot();
					List<ITree> nodeList = new ArrayList<>();
					nodeList = Utils.collectNode(t, nodeList);
		        	for(ITree node : nodeList) {
		        		int id = node.getId();
		        		if(srcActIds.contains(id)) {
		        			changedSTree.add(st);
//		        			System.out.println("find a action subtree!");
		        			break;
		        		}
		        	}
				}//先找包含action的subtree
			}					
			
			for(SubTree st : sub1) {
				ITree sRoot = st.getRoot();
				String sType = sTC.getTypeLabel(sRoot);
				if(sType.equals("decl_stmt")) {
					List<ITree> children = sRoot.getChildren();
					for(ITree root : children) {
						if(sTC.getTypeLabel(root).equals("decl")) {
							List<ITree> childs = root.getChildren();
							for(ITree child : childs) {
								String type = sTC.getTypeLabel(child);
								if(type.equals("name")) {//只有decl下的name节点的value才被认为是一条def
									String label = child.getLabel();
									int sID = sRoot.getId();
									stMap.put(sID, st);
									if(sDefMap.get(label)==null) {
										ArrayList<Integer> ids = new ArrayList<Integer>();
										ids.add(sID);
										sDefMap.put(label, ids);
									}else {
										sDefMap.get(label).add(sID);										
									}
								}
							}
						}else {
							wr.close();
							wr1.close();
							wr2.close();
							throw new Exception("error type!"+sTC.getTypeLabel(root));
						}						
					}			
				}
			}
			for(SubTree dt : sub2) {
				ITree dRoot = dt.getRoot();
				String dType = sTC.getTypeLabel(dRoot);
				if(dType.equals("decl_stmt")) {	
					List<ITree> children = dRoot.getChildren();
					for(ITree root : children) {
						if(sTC.getTypeLabel(root).equals("decl")) {
							List<ITree> childs = root.getChildren();
							for(ITree child : childs) {
								String type = dTC.getTypeLabel(child);
								if(type.equals("name")) {//只有decl下的name节点的value才被认为是一条def
									String label = child.getLabel();
									int dID = dRoot.getId();
									dtMap.put(dID, dt);
									if(dDefMap.get(label)==null) {
										ArrayList<Integer> ids = new ArrayList<Integer>();
										ids.add(dID);
										dDefMap.put(label, ids);
									}else {
										dDefMap.get(label).add(dID);
									}
								}
							}
						}
					}														
				}
			}
						
			System.out.println("changeSize:"+changedSTree.size());			
			for(SubTree srcT : changedSTree) {
				HashMap<String, SubTree> sMap = new HashMap<String, SubTree>();
				HashMap<String, SubTree> dMap = new HashMap<String, SubTree>();
				ArrayList<SubTree> sDef = new ArrayList<SubTree>();
				ArrayList<SubTree> dDef = new ArrayList<SubTree>();
				ArrayList<String> commonDef = new ArrayList<String>();//change pair的def交集
				HashMap<String, String> varMap = new HashMap<String, String>();//change pair的def总集
				System.out.println("===================");
				ITree sRoot = srcT.getRoot();
	    		ITree dRoot = mappings.getDst(sRoot);
	    		SubTree dstT = null;
	    		if(dRoot==null) {
	    			System.out.println("SID:"+sRoot.getId()); //发现有整颗srcST删除的情况 
	    			continue;
	    		}else {//根据mapping来找dt
	    			for(SubTree dt : sub2) {
	    				ITree root = dt.getRoot();
	    				if(root.equals(dRoot)) {
	    					dstT = dt;
	                		break;
	    				}      			
	    			}
	    		}
	    		if(dstT==null) {
	    			wr.close();
	    			wr1.close();
					wr2.close();
	    			throw new Exception("why is null?");
	    		}
	    		System.out.println(sRoot.getId()+"->"+dRoot.getId());
	    		
	    		String src = subtree2src(srcT);
	    		String tar = subtree2src(dstT);	    		
	    		if(((float)src.length()/(float)tar.length())<0.25||((float)tar.length()/(float)src.length())<0.25) {
	    			continue;
	    		}//长度相差太多的句子直接跳过
	    			
				String sType = sTC.getTypeLabel(sRoot);
				String dType = dTC.getTypeLabel(dRoot);
				if(!sType.equals("decl_stmt")&&!dType.equals("decl_stmt")) {					
					List<ITree> sLeaves = new ArrayList<ITree>();
					Utils.traverse2Leaf(sRoot, sLeaves);
					List<ITree> dLeaves = new ArrayList<ITree>();
					Utils.traverse2Leaf(dRoot, dLeaves);
					for(ITree leaf : sLeaves) {												
						String type = sTC.getTypeLabel(leaf);
						if(type.equals("name")) {
							String label = leaf.getLabel();
							ArrayList<Integer> map = sDefMap.get(label);
							if(map!=null) {
								if(map.size()==1) {
									int defID = map.get(0);
									SubTree st = stMap.get(defID);
									sMap.put(label, st);
									System.out.println("1Line");
								}else {//发现有多个def line同一个关键字的情况，可能发生在不同的method
									Collections.sort(map);
									ArrayList<Integer> subMap = new ArrayList<Integer>();
									for(int id : map) {//只取该条语句之前的subtree,抛弃掉之后的
										if(id<sRoot.getId())
											subMap.add(id);
									}
									if(subMap.size()==0)
										continue;//好像有defid全在rootid之后的情况，跳过
									int defID = subMap.get(subMap.size()-1);//取离该def最近的
									SubTree st = stMap.get(defID);
									sMap.put(label, st);
									System.out.println("mLine");
								}							
							}
						}
					}					
					for(ITree leaf : dLeaves) {
						String type = dTC.getTypeLabel(leaf);
						if(type.equals("name")) {
							String label = leaf.getLabel();
							ArrayList<Integer> map = dDefMap.get(label);
							if(map!=null) {
								if(map.size()==1) {
									int defID = map.get(0);
									SubTree st = dtMap.get(defID);
									dMap.put(label, st);
									System.out.println("1Line");
								}else {
									Collections.sort(map);
									ArrayList<Integer> subMap = new ArrayList<Integer>();
									for(int id : map) {//只取该条语句之前的subtree,抛弃掉之后的
										if(id<dRoot.getId())
											subMap.add(id);
									}
									if(subMap.size()==0)
										continue;//好像有defid全在rootid之后的情况，跳过
									int defID = subMap.get(subMap.size()-1);//取离该def最近的
									SubTree st = dtMap.get(defID);
									dMap.put(label, st);
									System.out.println("mLine");
								}							
							}
						}
					}
					if(sMap.size()>dMap.size()) {
						for(Map.Entry<String, SubTree> entry : dMap.entrySet()) {
							String keyword = entry.getKey();
							SubTree dt = entry.getValue();
							if(sMap.containsKey(keyword)) {//sMap和dMap取keyword交集
								sDef.add(sMap.get(keyword));
								dDef.add(dt);
								commonDef.add(keyword);//共有的keyword放到commonDef
								String absVarName = "var";
								varMap.put(keyword, absVarName);//共有的keyword放到varMap
							}
						}
					}else {
						for(Map.Entry<String, SubTree> entry : sMap.entrySet()) {
							String keyword = entry.getKey();
							SubTree st = entry.getValue();
							if(dMap.containsKey(keyword)) {//sMap和dMap取keyword交集
								dDef.add(dMap.get(keyword));
								sDef.add(st);
								commonDef.add(keyword);//共有的keyword放到commonDef
								String absVarName = "var";
								varMap.put(keyword, absVarName);//共有的keyword放到varMap
							}
						}
					}//change pair的DefMap取交集
					for(Map.Entry<String, SubTree> entry : dMap.entrySet()) {
						String keyword = entry.getKey();
						if(!varMap.containsKey(keyword)) {
							String absVarName = "var";
							varMap.put(keyword, absVarName);//共有的keyword放到varMap
						}
					}
					for(Map.Entry<String, SubTree> entry : sMap.entrySet()) {
						String keyword = entry.getKey();
						if(!varMap.containsKey(keyword)) {//sMap和dMap取keyword交集
							String absVarName = "var";
							varMap.put(keyword, absVarName);//共有的keyword放到varMap
						}
					}//私有def分别放入varMap										
					
					System.out.println("varSize:"+varMap.size());
					for(Map.Entry<String, String> entry : varMap.entrySet()) {
						String keyword = entry.getKey();
						String value = entry.getValue();
						System.out.println(keyword+","+value);
					}
					if(mode.equals("allDef")) {
						System.out.println("------s-------");
						for(SubTree st : sDef) {
							String sLine = subtree2src(st);
							sLine = absVariable(sLine, varMap);
							System.out.println(st.getRoot().getId()+" DefLine: "+sLine);
							wr.append(sLine+" ; ");
							wr1.append(sLine+" ; ");
						}
						if(sDef.size()!=0) {//allDef时候输出了use就不输出def那行了
							String sLine = subtree2src(srcT);
							sLine = absVariable(sLine, varMap);
							wr.append(sLine+"\t");
							wr1.append(sLine+" ; ");
							wr1.newLine();
							wr1.flush();
							System.out.println("CurrentLine: "+sLine);
						}	
						System.out.println("------d-------");
						for(SubTree dt : dDef) {
							String dLine = subtree2src(dt);
							dLine = absVariable(dLine, varMap);
							System.out.println(dt.getRoot().getId()+" DefLine: "+dLine);
							wr.append(dLine+" ; ");
							wr2.append(dLine+" ; ");
						}
						if(sDef.size()!=0) {
							String dLine = subtree2src(dstT);
							dLine = absVariable(dLine, varMap);
							System.out.println("CurrentLine: "+dLine);
							wr.append(dLine);
							wr.newLine();
							wr.flush();
							wr2.append(dLine);
							wr2.newLine();
							wr2.flush();
						}						
					}else {																		
						System.out.println("------s-------");
						for(SubTree st : sDef) {
							String sLine = subtree2src(st);
							sLine = absVariable(sLine, varMap);
							System.out.println(st.getRoot().getId()+" DefLine: "+sLine);
//							wr.append(sLine+" ; ");
//							wr1.append(sLine+" ; ");
						}
						String sLine = subtree2src(srcT);
						sLine = absVariable(sLine, varMap);
						wr.append(sLine+"\t");
						wr1.append(sLine);
						wr1.newLine();
						wr1.flush();
						System.out.println("CurrentLine: "+sLine);
//						System.out.println(Utils.printToken(srcT));
						System.out.println("------d-------");
						for(SubTree dt : dDef) {
							String dLine = subtree2src(dt);
							dLine = absVariable(dLine, varMap);
							System.out.println(dt.getRoot().getId()+" DefLine: "+dLine);
//							wr.append(dLine+" ; ");
//							wr2.append(dLine+" ; ");
						}
						String dLine = subtree2src(dstT);
						dLine = absVariable(dLine, varMap);
						System.out.println("CurrentLine: "+dLine);						
						wr.append(dLine);
						wr.newLine();
						wr.flush();
						wr2.append(dLine);
						wr2.newLine();
						wr2.flush();
					}					
				}else {
					List<ITree> sLeaves = new ArrayList<ITree>();
					Utils.traverse2Leaf(sRoot, sLeaves);
					List<ITree> dLeaves = new ArrayList<ITree>();
					Utils.traverse2Leaf(dRoot, dLeaves);
					for(ITree leaf : sLeaves) {
						String label = leaf.getLabel();
						if(sDefMap.containsKey(label)) {
							ArrayList<Integer> map = sDefMap.get(label);
							if(map!=null) {
								if(map.size()==1) {
									int defID = map.get(0);
									SubTree st = stMap.get(defID);
									sMap.put(label, st);
								}else {//发现有多个def line同一个关键字的情况，可能发生在不同的method
									for(int id : map) {//只取该条语句的subtree
										if(id==sRoot.getId()) {
											SubTree st = stMap.get(id);
											sMap.put(label, st);
											break;
										}																						
									}
								}							
							}
						}
					}
					for(ITree leaf : dLeaves) {
						String label = leaf.getLabel();
						if(dDefMap.containsKey(label)) {
							ArrayList<Integer> map = dDefMap.get(label);
							if(map!=null) {
								if(map.size()==1) {
									int defID = map.get(0);
									SubTree dt = dtMap.get(defID);
									dMap.put(label, dt);
								}else {//发现有多个def line同一个关键字的情况，可能发生在不同的method
									for(int id : map) {//只取该条语句的subtree
										if(id==dRoot.getId()) {
											SubTree dt = dtMap.get(id);
											dMap.put(label, dt);
											break;
										}																						
									}
								}							
							}
						}
					}
					for(Map.Entry<String, SubTree> entry : sMap.entrySet()) {
						String keyword = entry.getKey();
						if(!varMap.containsKey(keyword)) {//sMap和dMap取keyword交集
							String absVarName = "var";
							varMap.put(keyword, absVarName);//共有的keyword放到varMap
						}
					}
					for(Map.Entry<String, SubTree> entry : dMap.entrySet()) {
						String keyword = entry.getKey();
						if(!varMap.containsKey(keyword)) {
							String absVarName = "var";
							varMap.put(keyword, absVarName);//共有的keyword放到varMap
						}
					}//私有def分别放入varMap						
					
					System.out.println("------s-------");
					String sLine = subtree2src(srcT);
					sLine = absVariable(sLine, varMap);
					System.out.println("CurrentLine: "+sLine);
					wr.append(sLine+"\t");
					wr1.append(sLine+"");
					wr1.newLine();
					wr1.flush();
					System.out.println("------d-------");
					String dLine = subtree2src(dstT);
					dLine = absVariable(dLine, varMap);
					System.out.println("CurrentLine: "+dLine);
					wr.append(dLine);
					wr.newLine();
					wr.flush();
					wr2.append(dLine);
					wr2.newLine();
					wr2.flush();				
				}
			}			
		}	
		wr.close();
		wr1.close();
		wr2.close();
	}
	
	public static ArrayList<Integer> locateLineNum(SubTree st, String path) throws Exception {
		ArrayList<Integer> candidates = new ArrayList<Integer>();
		List<ITree> leaves = new ArrayList<ITree>();
		leaves = Utils.traverse2Leaf(st.getRoot(), leaves);
//		System.out.println("tokens:"+Utils.printToken(st));
		ArrayList<String> labels = new ArrayList<String>();
		ArrayList<String> codes = new ArrayList<String>();
		for(ITree tmp : leaves) {
			String label = tmp.getLabel();
			labels.add(label);
		}
		File file = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String tmpline = "";
		while((tmpline = br.readLine())!=null) {
			codes.add(tmpline);
		}
		br.close();
		float sim = Float.MIN_VALUE;
		for(String code : codes) {
			int count = 0;
			for(String label : labels) {
				if(code.contains(label)) {
					count++;
				}
			}
			float tmpsim = (float)count/(float)(labels.size());
			if(tmpsim>sim) {
				candidates.clear();
				sim = tmpsim;
				int lineNum = codes.indexOf(code)+1;
				candidates.add(lineNum);
			}else if(tmpsim==sim) {
				int lineNum = codes.indexOf(code)+1;
				candidates.add(lineNum);
			}
		}
		return candidates;
	}//search整份代码，找到包含最多tokens的行号(可能不唯一)
	
	public static String subtree2src(SubTree st) throws Exception {
		String src = "";
		String loopEnd = "";
		ITree root = st.getRoot();
		TreeContext srcT = st.getTC();
		String sType = srcT.getTypeLabel(root);
		if(sType.equals("while")||sType.equals("for")||sType.equals("if")) {
			if(sType.equals("while"))
				src = src+"while ( ";
			if(sType.equals("for"))
				src = src+"for ( ";
			if(sType.equals("if"))
				src = src+"if ( ";
			loopEnd = " ) ";
		}
		
		List<ITree> leaves = new ArrayList<>();
		leaves = Utils.traverse2Leaf(root, leaves);
//		System.out.println(leaves.size());
//		for(ITree leaf : leaves) {
//			String type = srcT.getTypeLabel(leaf);
//			String label = leaf.getLabel();
//			System.out.println(type+":"+label);
//		}
		if(leaves.size()==0)
			throw new Exception("null leaves");
		else if(leaves.size()==1) {
			src = src+leaves.get(0).getLabel();//先把0号叶子放入
			return src;
		}
		
		src = src+leaves.get(0).getLabel();//先把0号叶子放入
//		System.out.println("leafSize:"+leaves.size());
		for(int i=0;i<leaves.size()-1;i++) {
//			System.out.println(src);
			int size = 0;
			ITree leaf1 = leaves.get(i);
			ITree leaf2 = leaves.get(i+1);
			if(leaf2.getLabel().equals(""))
				continue;//发现有截取block后的断点影响还原,跳过
			if(leaf1.isRoot()||leaf2.isRoot())//叶子节点为总树根节点，可能么？
				throw new Exception("why is root???");
			ITree sharePar = Utils.findShareParent(leaf1, leaf2, srcT);
//			String parType = srcT.getTypeLabel(sharePar);
			List<ITree> childs = sharePar.getChildren();
			if(childs.contains(leaf1)&&childs.contains(leaf2)) {//同一层的两个叶子节点，还原时候直接拼起来就行
				src = src+" "+leaf2.getLabel();
			}else if(childs.size()>=2){//分情况讨论不同分支下还原代码问题
				ITree node1 = null;
				ITree node2 = null;
				for(ITree child : childs) {
					if(child.isLeaf()) {
						if(child.equals(leaf1))
							node1 = child;
						if(child.equals(leaf2))
							node2 = child;
					}else {
						List<ITree> list = TreeUtils.preOrder(child);
						if(list.contains(leaf1))
							node1 = child;
						if(list.contains(leaf2))
							node2 = child;
//						if(list.contains(leaf1)&&list.contains(leaf1))
//							throw new Exception("wrong sharePar!");
					}				
				}//找sharePar的下一个leaf1,leaf2对应父节点(或其本身)
				String type1 = "";
				String type2 = "";
				if(node1!=null&&node2!=null) {
					type1 = srcT.getTypeLabel(node1);
					type2 = srcT.getTypeLabel(node2);
				}else 
					System.out.println("why node is null?"+st.getMiName()+","+st.getRoot().getId());
				
				if(type1.equals("name")) {
					if(type2.equals("argument_list")||type2.equals("parameter_list")) {						
						List<ITree> arguLeaves = new ArrayList<>();
						arguLeaves = Utils.traverse2Leaf(node2, arguLeaves);//找到argulist中所有叶子
						src = src + recoverArguList(node2, arguLeaves, srcT);//argulist单独处理
						size = arguLeaves.size();
						i=i+size-1;
					}else if(type2.equals("init")) {
						src = src+" = "+leaf2.getLabel();
					}else if(type2.equals("operator")) {
						src = src+" "+leaf2.getLabel();
					}else if(type2.equals("modifier")) {
						src = src+" * ";
					}else if(type2.equals("index")) {
						src = src+" [ "+leaf2.getLabel()+" ] ";
					}else {
						src = "error name situation"; 
						break;
//						throw new Exception("没考虑过的name情况:"+type2);
					}					
				}else if(type1.equals("type")) {
					if(type2.equals("name")) {	
						src = src+" "+leaf2.getLabel();
					}else {
						src = "error type situation"; 
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}						
				}else if(type1.equals("operator")) {
					if(type2.equals("call")) {//好像有node2为call的情况
						node2 = node2.getChildren().get(0);
						type2 = srcT.getTypeLabel(node2);
					}						
					if(type2.equals("name")||type2.equals("operator")) {
						src = src+" "+leaf2.getLabel();
					}else {
						src = "error operator situation"; 
						break;
//						throw new Exception("没考虑过的operator情况:"+type2);
					}					
				}else if(type1.equals("call")) {
					if(type2.equals("operator")) {	
						src = src+" "+leaf2.getLabel();
					}else if(type2.equals("call")) {
						src = src+" , "+leaf2.getLabel();
					}else {
						src = src+ "error call situation";
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}
				}else if(type1.equals("specifier")) {
					if(type2.equals("name")){
						src = src+" "+leaf2.getLabel();
					}else {
						src = "error specifier situation"; 
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}						
				}else if(type1.equals("parameter_list")) {
					if(type2.equals("member_init_list")) {
						src = src+" : "+leaf2.getLabel();
					}
				}else if(type1.equals("decl")) {
					if(type2.equals("decl")) {
						src = src+" , "+leaf2.getLabel();
					}
				}else {
					src = "error other situation";
					break;
//					throw new Exception("没考虑过的children情况");
				}
			}				
		}
		src = src+loopEnd;
		return src;		
	}
	
	public static String recoverArguList(ITree root, List<ITree> arguLeaves, TreeContext srcT) throws Exception {
		String arguSrc = "";
		String end = "";
		ITree node = root.getParent();
		String type = srcT.getTypeLabel(node);//找到argument_list父节点
		if(type.equals("name")) {//name情况用<>
			arguSrc = arguSrc+" < ";
			end = " > ";
		}else if(type.equals("call")||type.equals("decl")) {//call的情况下用()
			arguSrc = arguSrc+" ( ";
			end = " ) ";
		}else if(type.equals("constructor")||type.equals("function")) {
			arguSrc = arguSrc+" ( ";
			end = " ) ";
		}			
		if(arguLeaves.size()==0) {
			arguSrc = arguSrc+end;
			return 	arguSrc;
		}//返回空括号
		if(arguLeaves.size()==1) {
			arguSrc = arguSrc + deleteLiteral(arguLeaves.get(0).getLabel())+end;
			return 	arguSrc;
		}//返回单个元素+括号
				
		arguSrc = arguSrc + deleteLiteral(arguLeaves.get(0).getLabel());
		for(int i=0;i<arguLeaves.size()-1;i++) {
			ITree leaf1 = arguLeaves.get(i);
			ITree leaf2 = arguLeaves.get(i+1);
			ITree sharePar = Utils.findShareParent(leaf1, leaf2, srcT);
//			String parType = srcT.getTypeLabel(sharePar);
			List<ITree> childs = sharePar.getChildren();
			if(childs.contains(leaf1)&&childs.contains(leaf2)) {//同一层的两个叶子节点，还原时候直接拼起来就行
				arguSrc = arguSrc+" "+deleteLiteral(leaf2.getLabel());
			}else if(childs.size()>=2){
				ITree node1 = null;
				ITree node2 = null;
				for(ITree child : childs) {
					if(child.isLeaf()) {
						if(child.equals(leaf1))
							node1 = child;
						if(child.equals(leaf2))
							node2 = child;
					}else {
						List<ITree> list = TreeUtils.preOrder(child);
						if(list.contains(leaf1))
							node1 = child;
						if(list.contains(leaf2))
							node2 = child;
//						if(list.contains(leaf1)&&list.contains(leaf1))
//							throw new Exception("wrong sharePar!");
					}				
				}//找sharePar的下一个leaf1,leaf2对应父节点(或其本身)
				String type1 = srcT.getTypeLabel(node1);
				String type2 = srcT.getTypeLabel(node2);
				if(type1.equals("name")) {
					if(type2.equals("argument_list")||type2.equals("parameter_list")) {	
						List<ITree> leaves = new ArrayList<>();
						leaves = Utils.traverse2Leaf(node2, leaves);//找到argulist中所有叶子
						arguSrc = arguSrc + recoverArguList(node2, leaves, srcT);
					}else if(type2.equals("operator")) {
						arguSrc = arguSrc+" "+deleteLiteral(leaf2.getLabel());
					}else if(type2.equals("modifier")) {
						arguSrc = arguSrc+" * ";
					}else {
						arguSrc = "error nameArg situation";
						break;
//						throw new Exception("没考虑过的name情况:"+type2);	
					}																						
				}else if(type1.equals("argument")||type1.equals("parameter")) {
					if(type2.equals("argument")||type2.equals("parameter")) {
						arguSrc = arguSrc+" , "+deleteLiteral(leaf2.getLabel());
					}else {
						arguSrc = "error argumentArg situation";
						break;
//						throw new Exception("没考虑过的argument情况:"+type2);
					}					
				}else if(type1.equals("type")) {
					if(type2.equals("name")) {	
						arguSrc = arguSrc+" "+deleteLiteral(leaf2.getLabel());
					}else {
						arguSrc = "error typeArg situation";
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}						
				}else if(type1.equals("call")) {
					if(type2.equals("operator")) {	
						arguSrc = arguSrc+" "+deleteLiteral(leaf2.getLabel());
					}else {
						arguSrc = "error callArg situation";
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}
				}else if(type1.equals("operator")) {
					if(type2.equals("call")) {//好像有node2为call的情况
						node2 = node2.getChildren().get(0);
						type2 = srcT.getTypeLabel(node2);
					}						
					if(type2.equals("name")||type2.equals("operator")) {
						arguSrc = arguSrc+" "+deleteLiteral(leaf2.getLabel());
					}else {
						arguSrc = "error operatorArg situation";
						break;
//						throw new Exception("没考虑过的operator情况:"+type2);
					}						
				}else if(type1.equals("specifier")) {
					if(type2.equals("name")){
						arguSrc = arguSrc+" "+deleteLiteral(leaf2.getLabel());
					}else {
						arguSrc = "error specifierArg situation";
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}						
				}else {
					arguSrc = "error otherArg situation";
					break;
//					throw new Exception("没考虑过的children情况");
				}					
			}		
		}
		arguSrc = arguSrc+end;//加上收尾
		return arguSrc;		
	}//argulist相当于subtree中的subtree，单独还原
	
	public static String deleteLiteral(String label) {
		if(label.contains("\"")) {
			label = "\"\"";
		}
		return label;
	}	
	
	public static String absVariable(String src, HashMap<String, String> varMap) {
		String[] srcs = src.split(" ");
		String newLine = "";
		if(srcs.length==1) {
			String token = srcs[0];
			if(varMap.containsKey(token)) {
				if(newLine.equals("")) {
					newLine = varMap.get(token);
				}else
					newLine = newLine+" "+varMap.get(token);
			}else {
				if(newLine.equals("")) {
					newLine = token;
				}else
					newLine = newLine+" "+token;
			}
		}else {
			for(int i=0;i<srcs.length-1;i++) {
				String token = srcs[i];
				String nextToken = srcs[i+1];
				if(varMap.containsKey(token)) {
					if(newLine.equals("")) {
						newLine = varMap.get(token);
					}else {
						if(nextToken.equals("(")) {
							newLine = newLine+" "+token;
						}else
							newLine = newLine+" "+varMap.get(token);
					}						
				}else {
					if(newLine.equals("")) {
						newLine = token;
					}else
						newLine = newLine+" "+token;
				}
			}
		}		
		return newLine;
	}
		
}
