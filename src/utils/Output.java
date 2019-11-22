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
import java.util.regex.Pattern;

import gumtreediff.actions.model.Action;
import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.io.TreeIoUtils;
import gumtreediff.matchers.MappingStore;
import gumtreediff.matchers.Matcher;
import gumtreediff.matchers.Matchers;
import gumtreediff.tree.ITree;
import gumtreediff.tree.TreeContext;
import gumtreediff.tree.TreeUtils;
import nodecluster.Cluster;
import split.Split;
import structure.Definition;
import structure.Migration;
import structure.SubTree;
import structure.Transform;

public class Output {

	public static void main (String args[]) throws Exception{
		String path = args[0];
//		String path = "Absolute3DLocalizationElement.cpp";
//		Output.collectTokens(sp.trans);
//		Output.collectChangePairs(path, "");
//		Output.collectDefUse(path, "", "");
//		Output.printJson(path, null);
//		String path = "talker.cpp";
		Output.tokensFromInputFile(path);
	}	
	
	public static void tokensFromInputFile(String path) throws Exception {
		Split sp = new Split();
		File output = new File("src-test.txt");
		File outLine = new File("lineNum.txt");
		File outFile = new File("src-var.txt");
		BufferedWriter wr = new BufferedWriter(new FileWriter(output));
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(outLine));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(outFile));
		File cppFile = new File(path);
		TreeContext tc = new SrcmlCppTreeGenerator().generateFromFile(cppFile);
		String cppName = cppFile.getName();	
		System.out.println("Reading file: "+cppName);
		Defuse def = new Defuse();
		ArrayList<Definition> defs1 = def.getDef(tc, "src");//�ȼ���action,���ռ�defs
        HashMap<String, ArrayList<Definition>> defMap1 = def.transferDefs(defs1);
        HashMap<ITree, ArrayList<Definition>> blockMap1 = def.transferBlockMap(defs1, tc, "src");
		ArrayList<SubTree> sub1 = sp.splitSubTree(tc, cppName);//Subtree�и��ѹ�block,ע��
		for(SubTree srcT : sub1) {			
			ITree root = srcT.getRoot();			
			List<ITree> candidates = root.getDescendants();
			if(srcT!=null&&srcT.getTC()!=null) {
				String src = subtree2src(srcT);
				
				ArrayList<ITree> leaves1 = new ArrayList<ITree>();				
				Utils.traverse2Leaf(root, leaves1);
				int labelCount = 0;
				for(ITree leaf : leaves1) {
					String label = leaf.getLabel();
					if(!label.equals(""))
						labelCount++;
					String type = tc.getTypeLabel(leaf);
					if(type.equals("literal")) {
						leaf.setLabel(Output.deleteLiteral(leaf, tc));
					}					
					ArrayList<Definition> stringList = defMap1.get(label);
					if(stringList!=null) {
						ITree parBlock = def.searchBlock(leaf, tc);
						ArrayList<Definition> blockList = blockMap1.get(parBlock);
						for(Definition def1 : stringList) {
							if(blockList!=null) {
								if(blockList.contains(def1)) {
									if(leaf.getId()>def1.getDefLabelID()) {
										leaf.setLabel("var");									
									}											
								}
							}							
							if(def1.getDefLabelID()==leaf.getId()) {
								leaf.setLabel("var");
							}
						}
					}
				}
				if(labelCount<=1)
					continue;
				
				wr.append(src);
				wr.newLine();
				wr.flush();	
				src = subtree2src(srcT);
				wr2.append(src);
				wr2.newLine();
				wr2.flush();	
				
				candidates.add(root);
				int beginLine = 0;
				int beginColumn = 0;
				int endLine = 0;
				int endColumn = 0;
				for(ITree node : candidates) {
					int line = node.getLine();
					int column = node.getColumn();
					if(line==0)
						continue;//null
					int lastLine = node.getLastLine();
					int lastColumn = node.getLastColumn();
					if(beginLine==0&&line>beginLine) {
						beginLine = line;
					}else if(line<beginLine&&line!=0){
						beginLine = line;
					}
					if(beginColumn==0&&column>beginColumn) {
						beginColumn = column;
					}else if(column<beginColumn&&column!=0){
						beginColumn = column;
					}
					if(endLine==0&&lastLine>endLine) {
						endLine = lastLine;						
					}else if(lastLine>endLine) {
						endLine = lastLine;
					}
					if(endColumn==0&&lastColumn>endColumn) {
						endColumn = lastColumn;						
					}else if(lastColumn>endColumn) {
						endColumn = lastColumn;
					}
				}				
				wr1.append(String.valueOf(beginLine)+","+String.valueOf(beginColumn)+
						"->"+String.valueOf(endLine)+","+String.valueOf(endColumn));
				wr1.newLine();
				wr1.flush();
			}
		}
		wr.close();	
		wr1.close();
		wr2.close();
	}
	
	public static void printJson(String path, String filter) throws Exception {
		Split sp = new Split();
		ArrayList<Migration> migrats = new ArrayList<>();
		migrats = sp.readMigration(path, filter);
		sp.storeTrans(migrats);
		ArrayList<Transform> trans = sp.trans;
		for(int i=0;i<trans.size();i++) {
			Transform tf = trans.get(i);
			System.out.println("Analyse:"+tf.getMiName());
			SubTree srcT = tf.getSTree();
			SubTree dstT = tf.getDTree();
			if(srcT!=null&&dstT!=null) {
                if(srcT.getTC()!=null&&dstT.getTC()!=null) {
                	TreeContext sub1 = new TreeContext();
        			TreeContext sub2 = new TreeContext();
        			sub1.importTypeLabels(srcT.getTC());
        			sub2.importTypeLabels(dstT.getTC());
        			sub1.setRoot(srcT.getRoot());
        			sub2.setRoot(dstT.getRoot());
        			String out = "jsons\\pair"+String.valueOf(i)+"_src.json";
        			BufferedWriter wr = new BufferedWriter(new FileWriter(new File(out)));
        			wr.append(TreeIoUtils.toJson(sub1).toString());
        			wr.flush();
        			wr.close();
        			String out1 = "jsons\\pair"+String.valueOf(i)+"_tgt.json";
        			BufferedWriter wr1 = new BufferedWriter(new FileWriter(new File(out1)));
        			wr1.append(TreeIoUtils.toJson(sub2).toString());
        			wr1.flush();
        			wr1.close();
				}
			}		
		}
		
	}
	
	public static void collectChangePairs(String path, String filter) throws Exception {
		Split sp = new Split();
		ArrayList<Migration> migrats = new ArrayList<>();
		migrats = sp.readMigration(path, filter);
		sp.storeTrans(migrats);
		String out = "change.txt";
		String out1 = "src-train.txt";
		String out2 = "tgt-train.txt";
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
	}//search���ݴ��룬�ҵ��������tokens���к�(���ܲ�Ψһ)
	
	public static String subtree2src(SubTree st) throws Exception {
//		String src = String.valueOf(st.getRoot().getId());
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
		}else if (sType.equals("return")){
			src = src+"return ";
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
			src = src+leaves.get(0).getLabel();//�Ȱ�0��Ҷ�ӷ���
			return src;
		}
		
		src = src+leaves.get(0).getLabel();//�Ȱ�0��Ҷ�ӷ���
//		System.out.println("leafSize:"+leaves.size());
		for(int i=0;i<leaves.size()-1;i++) {
//			System.out.println(src);
			int size = 0;
			ITree leaf1 = leaves.get(i);
			ITree leaf2 = leaves.get(i+1);
			if(leaf1.getLabel()==""&&leaf2.getLabel()=="")
				continue;
			if(leaf2.getLabel().equals("")&&
					!srcT.getTypeLabel(leaf2).equals("argument_list")&&
					!srcT.getTypeLabel(leaf2).equals("parameter_list")) {
				i++;
				if(i<leaves.size()-1) {					
					leaf2 = leaves.get(i+1);
				}else {
					continue;
				}
			}//�����н�ȡblock��Ķϵ�Ӱ�컹ԭ,����
			if(leaf1.isRoot()||leaf2.isRoot())//Ҷ�ӽڵ�Ϊ�������ڵ㣬����ô��
				throw new Exception("why is root???");
			ITree sharePar = Utils.findShareParent(leaf1, leaf2, srcT);
//			String parType = srcT.getTypeLabel(sharePar);
			List<ITree> childs = sharePar.getChildren();
			if(childs.contains(leaf1)&&childs.contains(leaf2)) {//ͬһ�������Ҷ�ӽڵ㣬��ԭʱ��ֱ��ƴ��������
				src = src+" "+leaf2.getLabel();
			}else if(childs.size()>=2){//��������۲�ͬ��֧�»�ԭ��������
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
				}//��sharePar����һ��leaf1,leaf2��Ӧ���ڵ�(���䱾��)
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
						arguLeaves = Utils.traverse2Leaf(node2, arguLeaves);//�ҵ�argulist������Ҷ��
						src = src + recoverArguList(node2, arguLeaves, srcT);//argulist��������
						if(src.substring(src.length()-1)==" ")
							src = src.substring(0, src.length()-1);//ȥ���ո�
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
						src = String.valueOf(st.getRoot().getId())+"error name situation"; 
						break;
//						throw new Exception("û���ǹ���name���:"+type2);
					}					
				}else if(type1.equals("type")) {
					if(type2.equals("name")) {	
						src = src+" "+leaf2.getLabel();
					}else {
						src = String.valueOf(st.getRoot().getId())+"error type situation"; 
						break;
//						throw new Exception("û���ǹ���type���:"+type2);
					}						
				}else if(type1.equals("operator")) {
					if(type2.equals("call")) {//������node2Ϊcall�����
						node2 = node2.getChildren().get(0);
						type2 = srcT.getTypeLabel(node2);
					}						
					if(type2.equals("name")||type2.equals("operator")) {
						src = src+" "+leaf2.getLabel();
					}else {
						src = String.valueOf(st.getRoot().getId())+"error operator situation"; 
						break;
//						throw new Exception("û���ǹ���operator���:"+type2);
					}					
				}else if(type1.equals("call")) {
					if(type2.equals("operator")) {	
						src = src+" "+leaf2.getLabel();
					}else if(type2.equals("call")) {
						src = src+" , "+leaf2.getLabel();
					}else {
						src = String.valueOf(st.getRoot().getId())+ "error call situation";
						break;
//						throw new Exception("û���ǹ���type���:"+type2);
					}
				}else if(type1.equals("specifier")) {
					if(type2.equals("name")){
						src = src+" "+leaf2.getLabel();
					}else {
						src = String.valueOf(st.getRoot().getId())+"error specifier situation"; 
						break;
//						throw new Exception("û���ǹ���type���:"+type2);
					}						
				}else if(type1.equals("parameter_list")) {
					if(type2.equals("member_init_list")) {
						src = src+" : "+leaf2.getLabel();
					}
				}else if(type1.equals("decl")) {
					if(type2.equals("decl")) {
						src = src+" , "+leaf2.getLabel();
					}
				}else if(type1.equals("init")) {
					if(type2.equals("condition")) {
						src = src+" ; "+leaf2.getLabel();
					}
				}else if(type1.equals("condition")) {
					if(type2.equals("incr")) {
						src = src+" ; "+leaf2.getLabel();
					}
				}else {
					src = src+"error other situation";
					break;
//					throw new Exception("û���ǹ���children���");
				}				
			}				
		}
		src = src+loopEnd;
		if(src.contains("  "))
			src = src.replace("  ", " ");
		src = src.trim();
		return src;		
	}
	
	public static String recoverArguList(ITree root, List<ITree> arguLeaves, TreeContext srcT) throws Exception {
		String arguSrc = "";
		String end = "";
		ITree node = root.getParent();
		String type = srcT.getTypeLabel(node);//�ҵ�argument_list���ڵ�
		if(type.equals("name")) {//name�����<>
			arguSrc = arguSrc+" < ";
			end = " > ";
		}else if(type.equals("call")||type.equals("decl")) {//call���������()
			arguSrc = arguSrc+" ( ";
			end = " ) ";
		}else if(type.equals("constructor")||type.equals("function")) {
			arguSrc = arguSrc+" ( ";
			end = " ) ";
		}			
		if(arguLeaves.size()==0) {
			arguSrc = arguSrc+end;
			return 	arguSrc;
		}//���ؿ�����
		if(arguLeaves.size()==1) {
			arguSrc = arguSrc + deleteLiteral(arguLeaves.get(0), srcT)+end;
			return 	arguSrc;
		}//���ص���Ԫ��+����
				
		arguSrc = arguSrc + deleteLiteral(arguLeaves.get(0), srcT);
		for(int i=0;i<arguLeaves.size()-1;i++) {
			ITree leaf1 = arguLeaves.get(i);
			ITree leaf2 = arguLeaves.get(i+1);
			ITree sharePar = Utils.findShareParent(leaf1, leaf2, srcT);
//			String parType = srcT.getTypeLabel(sharePar);
			List<ITree> childs = sharePar.getChildren();
			if(childs.contains(leaf1)&&childs.contains(leaf2)) {//ͬһ�������Ҷ�ӽڵ㣬��ԭʱ��ֱ��ƴ��������
				arguSrc = arguSrc+" "+deleteLiteral(leaf2, srcT);
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
				}//��sharePar����һ��leaf1,leaf2��Ӧ���ڵ�(���䱾��)
				String type1 = srcT.getTypeLabel(node1);
				String type2 = srcT.getTypeLabel(node2);
				if(type1.equals("name")) {
					if(type2.equals("argument_list")||type2.equals("parameter_list")) {	
						List<ITree> leaves = new ArrayList<>();
						leaves = Utils.traverse2Leaf(node2, leaves);//�ҵ�argulist������Ҷ��
						arguSrc = arguSrc + recoverArguList(node2, leaves, srcT);
					}else if(type2.equals("operator")) {
						arguSrc = arguSrc+" "+deleteLiteral(leaf2, srcT);
					}else if(type2.equals("modifier")) {
						arguSrc = arguSrc+" * ";
					}else {
						arguSrc = String.valueOf(srcT.getRoot().getId())+"error nameArg situation";
						break;
//						throw new Exception("û���ǹ���name���:"+type2);	
					}																						
				}else if(type1.equals("argument")||type1.equals("parameter")) {
					if(type2.equals("argument")||type2.equals("parameter")) {
						arguSrc = arguSrc+" , "+deleteLiteral(leaf2, srcT);
					}else {
						arguSrc = String.valueOf(srcT.getRoot().getId())+"error argumentArg situation";
						break;
//						throw new Exception("û���ǹ���argument���:"+type2);
					}					
				}else if(type1.equals("type")) {
					if(type2.equals("name")) {	
						arguSrc = arguSrc+" "+deleteLiteral(leaf2, srcT);
					}else {
						arguSrc = String.valueOf(srcT.getRoot().getId())+"error typeArg situation";
						break;
//						throw new Exception("û���ǹ���type���:"+type2);
					}						
				}else if(type1.equals("call")) {
					if(type2.equals("operator")) {	
						arguSrc = arguSrc+" "+deleteLiteral(leaf2, srcT);
					}else {
						arguSrc = String.valueOf(srcT.getRoot().getId())+"error callArg situation";
						break;
//						throw new Exception("û���ǹ���type���:"+type2);
					}
				}else if(type1.equals("operator")) {
					if(type2.equals("call")) {//������node2Ϊcall�����
						node2 = node2.getChildren().get(0);
						type2 = srcT.getTypeLabel(node2);
					}						
					if(type2.equals("name")||type2.equals("operator")) {
						arguSrc = arguSrc+" "+deleteLiteral(leaf2, srcT);
					}else {
						arguSrc = String.valueOf(srcT.getRoot().getId())+"error operatorArg situation";
						break;
//						throw new Exception("û���ǹ���operator���:"+type2);
					}						
				}else if(type1.equals("specifier")) {
					if(type2.equals("name")){
						arguSrc = arguSrc+" "+deleteLiteral(leaf2, srcT);
					}else {
						arguSrc = String.valueOf(srcT.getRoot().getId())+"error specifierArg situation";
						break;
//						throw new Exception("û���ǹ���type���:"+type2);
					}						
				}else {
					arguSrc = String.valueOf(srcT.getRoot().getId())+"error otherArg situation";
					break;
//					throw new Exception("û���ǹ���children���");
				}					
			}		
		}
		arguSrc = arguSrc+end;//������β
		return arguSrc;		
	}//argulist�൱��subtree�е�subtree��������ԭ
	
	public static String deleteLiteral(ITree leaf, TreeContext tc) {
		String label = leaf.getLabel();
		String type = tc.getTypeLabel(leaf);
		if(type.equals("literal")) {
//			if(isNumber(label)==true) {
//				if(isInteger(label)==true) {
//					label = "Int";
//				}else if(isDouble(label)==true){
//					label = "Float";
//				}
//			}
			if(label.contains("\"")) 
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
			newLine = newLine+" "+srcs[srcs.length-1];//����β��
		}		
		return newLine;
	}
	
	private static boolean isNumber (Object obj) {
		if (obj instanceof Number) {
			return true;
		} else if (obj instanceof String){
			try{
				Double.parseDouble((String)obj);
				return true;
			}catch (Exception e) {
				return false;
			}
		}
		return false;
	}
	
	/* 
	  * �ж��Ƿ�Ϊ����  
	  * @param str ������ַ���  
	  * @return ����������true,���򷵻�false  
	*/  
	  public static boolean isInteger(String str) {    
	    Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");    
	    return pattern.matcher(str).matches();    
	  }  


	/*  
	  * �ж��Ƿ�Ϊ������������double��float  
	  * @param str ������ַ���  
	  * @return �Ǹ���������true,���򷵻�false  
	*/    
	  public static boolean isDouble(String str) {    
	    Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");    
	    return pattern.matcher(str).matches();    
	  }
}
