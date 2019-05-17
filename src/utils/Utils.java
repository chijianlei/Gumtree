package utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import gumtreediff.actions.ActionGenerator;
import gumtreediff.actions.model.Action;
import gumtreediff.actions.model.Delete;
import gumtreediff.actions.model.Insert;
import gumtreediff.actions.model.Move;
import gumtreediff.actions.model.Update;
import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.matchers.Mapping;
import gumtreediff.matchers.MappingStore;
import gumtreediff.matchers.Matcher;
import gumtreediff.matchers.Matchers;
import gumtreediff.tree.ITree;
import gumtreediff.tree.TreeContext;
import gumtreediff.tree.TreeUtils;
import nodecluster.Cluster;
import split.Split;
import structure.ChangeTuple;
import structure.DTree;
import structure.SubTree;

public class Utils {
	
	public static void main (String args[]) throws Exception{
		String path = "talker.cpp";
		File cppfile = new File(path);
		TreeContext tc1 = new SrcmlCppTreeGenerator().generateFromFile(cppfile);
		String path2 = "talker2.cpp";
		File cppfile2 = new File(path2);
		TreeContext tc2 = new SrcmlCppTreeGenerator().generateFromFile(cppfile2);		
//		HashMap<String, LinkedList<Action>> actions = collectAction(tc1, tc2);
//		printAllActions(tc1, tc2, actions);
		String miName = path2.split("/")[path2.split("/").length-1];//标记文件名
		Split sp = new Split();
		ArrayList<SubTree> sts = sp.splitSubTree(tc2, miName);
		System.out.println(sts.size());
		for(SubTree st : sts) {
			String src = subtree2src(st);
			System.out.println(src);
		}
	}
	
	public static String subtree2src(SubTree st) throws Exception {
		String src = "";
		String loopEnd = "";
		ITree root = st.getRoot();
		TreeContext srcT = st.getTC();
		String sType = srcT.getTypeLabel(root);
		if(sType.equals("while")||sType.equals("for")) {
			if(sType.equals("while"))
				src = src+"while(";
			if(sType.equals("for"))
				src = src+"for(";
			loopEnd = ")";
		}
		
		List<ITree> leaves = new ArrayList<>();
		leaves = traverse2Leaf(root, leaves);
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
		for(int i=0;i<leaves.size()-1;i++) {
			int size = 0;
			ITree leaf1 = leaves.get(i);
			ITree leaf2 = leaves.get(i+1);
			if(leaf1.isRoot()||leaf2.isRoot())//叶子节点为总树根节点，可能么？
				throw new Exception("why is root???");
			ITree sharePar = findShareParent(leaf1, leaf2, srcT);
//			String parType = srcT.getTypeLabel(sharePar);
			List<ITree> childs = sharePar.getChildren();
			if(childs.contains(leaf1)&&childs.contains(leaf2)) {//同一层的两个叶子节点，还原时候直接拼起来就行
				src = src+ leaf2.getLabel();
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
						arguLeaves = traverse2Leaf(node2, arguLeaves);//找到argulist中所有叶子
						src = src + recoverArguList(node2, arguLeaves, srcT);//argulist单独处理
						size = arguLeaves.size();
						i=i+size-1;
					}else if(type2.equals("init")) {
						src = src+"="+leaf2.getLabel();
					}else if(type2.equals("operator")) {
						src = src+leaf2.getLabel();
					}else {
						src = "error situation"; 
						break;
//						throw new Exception("没考虑过的name情况:"+type2);
					}					
				}else if(type1.equals("type")) {
					if(type2.equals("name")) {	
						src = src+" "+leaf2.getLabel();
					}else {
						src = "error situation"; 
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}						
				}else if(type1.equals("operator")) {
					if(type2.equals("call")) {//好像有node2为call的情况
						node2 = node2.getChildren().get(0);
						type2 = srcT.getTypeLabel(node2);
					}						
					if(type2.equals("name")||type2.equals("operator")) {
						src = src+leaf2.getLabel();
					}else {
						src = "error situation";
						break;
//						throw new Exception("没考虑过的operator情况:"+type2);
					}					
				}else if(type1.equals("specifier")) {
					if(type2.equals("name")){
						src = src+" "+leaf2.getLabel();
					}else {
						src = "error situation";
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}						
				}
			}else {
				src = "error situation";
				break;
//				throw new Exception("没考虑过的children情况");
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
			arguSrc = arguSrc+"<";
			end = ">";
		}else if(type.equals("call")||type.equals("decl")) {//call的情况下用()
			arguSrc = arguSrc+"(";
			end = ")";
		}else if(type.equals("constructor")||type.equals("function")) {
			arguSrc = arguSrc+"(";
			end = ")";
		}			
		if(arguLeaves.size()==0) {
			arguSrc = arguSrc+end;
			return 	arguSrc;
		}//返回空括号
		if(arguLeaves.size()==1) {
			arguSrc = arguSrc + arguLeaves.get(0).getLabel()+end;
			return 	arguSrc;
		}//返回单个元素+括号
				
		arguSrc = arguSrc + arguLeaves.get(0).getLabel();
		for(int i=0;i<arguLeaves.size()-1;i++) {
			ITree leaf1 = arguLeaves.get(i);
			ITree leaf2 = arguLeaves.get(i+1);
			ITree sharePar = findShareParent(leaf1, leaf2, srcT);
//			String parType = srcT.getTypeLabel(sharePar);
			List<ITree> childs = sharePar.getChildren();
			if(childs.contains(leaf1)&&childs.contains(leaf2)) {//同一层的两个叶子节点，还原时候直接拼起来就行
				arguSrc = arguSrc+ leaf2.getLabel();
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
						leaves = traverse2Leaf(node2, leaves);//找到argulist中所有叶子
						arguSrc = arguSrc + recoverArguList(node2, leaves, srcT);
					}else if(type2.equals("operator")) {
						arguSrc = arguSrc+leaf2.getLabel();
					}else if(type2.equals("modifier")) {
						arguSrc = arguSrc+" *";
					}else {
						arguSrc = "error situation";
						break;
//						throw new Exception("没考虑过的name情况:"+type2);	
					}																						
				}else if(type1.equals("argument")||type1.equals("parameter")) {
					if(type2.equals("argument")||type2.equals("parameter")) {
						arguSrc = arguSrc+","+leaf2.getLabel();
					}else {
						arguSrc = "error situation";
						break;
//						throw new Exception("没考虑过的argument情况:"+type2);
					}					
				}else if(type1.equals("type")) {
					if(type2.equals("name")) {	
						arguSrc = arguSrc+" "+leaf2.getLabel();
					}else {
						arguSrc = "error situation";
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}						
				}else if(type1.equals("operator")) {
					if(type2.equals("call")) {//好像有node2为call的情况
						node2 = node2.getChildren().get(0);
						type2 = srcT.getTypeLabel(node2);
					}						
					if(type2.equals("name")||type2.equals("operator")) {
						arguSrc = arguSrc+leaf2.getLabel();
					}else {
						arguSrc = "error situation";
						break;
//						throw new Exception("没考虑过的operator情况:"+type2);
					}						
				}else if(type1.equals("specifier")) {
					if(type2.equals("name")){
						arguSrc = arguSrc+" "+leaf2.getLabel();
					}else {
						arguSrc = "error situation";
						break;
//						throw new Exception("没考虑过的type情况:"+type2);
					}						
				}else {
					arguSrc = "error situation";
					break;
//					throw new Exception("没考虑过的children情况");
				}					
			}		
		}
		arguSrc = arguSrc+end;//加上收尾
		return arguSrc;		
	}//argulist相当于subtree中的subtree，单独还原
	
	public static ChangeTuple filterChange(DTree sDT, DTree dDT) throws Exception {
		ChangeTuple ct = new ChangeTuple();
		TreeContext sTC = sDT.getTreeContext();
		TreeContext dTC = dDT.getTreeContext();
		ITree sRoot = sDT.getRoot();
		ITree dRoot = dDT.getRoot();
		List<ITree> sLeaves = new ArrayList<ITree>();
		List<ITree> dLeaves = new ArrayList<ITree>();
		sLeaves = traverse2Leaf(sRoot, sLeaves);
		dLeaves = traverse2Leaf(dRoot, dLeaves);
		ArrayList<Integer> sameLocation = new ArrayList<Integer>();
		ArrayList<Integer> diffLocation = new ArrayList<Integer>();
		for(ITree sLeaf : sLeaves) {
			String sType = sTC.getTypeLabel(sLeaf);
			String sValue = sLeaf.getLabel();
			int pos1 = sLeaf.positionInParent();
			for(ITree dLeaf : dLeaves) {
				String dType = dTC.getTypeLabel(dLeaf);
				String dValue = dLeaf.getLabel();
				int pos2 = dLeaf.positionInParent();
				if(sType.equals(dType)&&sValue.equals(dValue)) {										
					if(pos1==pos2) 
						sameLocation.add(pos1);
				}else {
					if(pos1==pos2)
						diffLocation.add(pos1);
				}
			}		
		}
		Collections.sort(sameLocation);
		Collections.sort(diffLocation);
		String sString = "";
		String dString = "";
		ArrayList<ITree> sMoves = new ArrayList<ITree>();
		ArrayList<ITree> dMoves = new ArrayList<ITree>();
		if(diffLocation.size()!=0&&sameLocation.size()!=0) {
			if(diffLocation.get(diffLocation.size()-1)<sameLocation.get(0)||
					diffLocation.get(0)>sameLocation.get(sameLocation.size()-1)) {
				for(int i=0;i<sameLocation.size();i++){
					ITree sLeaf = sLeaves.get(sameLocation.get(i));
					ITree dLeaf = dLeaves.get(sameLocation.get(i));
					sMoves.add(sLeaf);
					dMoves.add(dLeaf);
				}
				for(ITree node : sMoves) {
					sLeaves.remove(node);
				}
				for(ITree node : dMoves) {
					dLeaves.remove(node);
				}
				for(ITree leaf : sLeaves) {
					String value = leaf.getLabel();
					sString = sString + value;
				}
				for(ITree leaf : dLeaves) {
					String value = leaf.getLabel();
					dString = dString + value;
				}
				ct.setSrc(sString);
				ct.setDst(dString);
			//diff的最后一个位置编号也比same小或第一个位置编号就比same大，只保留diff部分，否则不改
			}else {
				sString = Utils.printLeaf(sDT);
				dString = Utils.printLeaf(dDT);
				ct.setSrc(sString);
				ct.setDst(dString);
			}
		}else if(sameLocation.size()==0) {
			sString = Utils.printLeaf(sDT);
			dString = Utils.printLeaf(dDT);
			ct.setSrc(sString);
			ct.setDst(dString);
		}else {
			throw new Exception("error!");
		}
		
		return ct;		
	}//change中包含相同的不修改部分，需要过滤删除
	
	
	
	
	public static ITree findShareParent(ITree leaf1, ITree leaf2, TreeContext tc) throws Exception {
		if(leaf1.isRoot()||leaf2.isRoot()) 
			throw new Exception("check the leaf!");
		ITree sharePar = null;
		if(leaf1.getParent().equals(leaf2.getParent()))//大概率同个父亲
			sharePar = leaf1.getParent();
		else {
			ITree subRoot = traverSubRoot(leaf1, tc);
			Boolean ifSamePar = true;
			while(ifSamePar) {
				List<ITree> children = subRoot.getChildren();
				for(ITree child : children) {
					List<ITree> order = TreeUtils.preOrder(child);
					if(order.contains(leaf1)&&order.contains(leaf2)) {
						ifSamePar = true;
						subRoot = child;
						break;
					}						
					else 
						ifSamePar = false;
				}				
			}
			sharePar = subRoot;
		}
//		System.out.println("find sharePar:"+tc.getTypeLabel(sharePar));
		return sharePar;		
	}//找两个节点共有最低的父亲节点	

	
	public static ITree traverSubRoot(ITree node, TreeContext tc) {
		ITree subRoot = null;
		String typeLabel = tc.getTypeLabel(node);				
		while(!ifSRoot(typeLabel)) {//可能有问题，要注意循环条件
			if(node.isRoot()) {//发现有修改include的情况，subroot为总树根节点
				subRoot = node;
				break;
			}else {
				node = node.getParent();
				typeLabel = tc.getTypeLabel(node);
//				System.out.println("typeLabel:"+typeLabel);
				subRoot = node;
			}		
		}
		return subRoot;
	}//往上追溯某节点在子树内的根节点
	
	public static List<ITree> collectNode(ITree node, List<ITree> nodes) throws Exception {
		if(nodes.isEmpty())
			nodes.add(node);
		if(node.isRoot()&&node.getChildren().isEmpty())
			throw new Exception("Empty root");
		List<ITree> childs = node.getChildren();
		nodes.addAll(node.getChildren());
		for(ITree child : childs) {
			if(!child.getChildren().isEmpty()) {
				collectNode(child, nodes);
			}else continue;
		}
		return nodes;		
	}//收集AST树中所有点
	
	public static Integer collectEdge(ITree node, int num) {
		List<ITree> childs = node.getChildren();
		for(ITree child : childs) {
			num = num+1;
//			System.out.println(child.getParent().getId()+"->"+child.getId());
			collectEdge(child, num);
		}
		return num;		
	}//收集AST树中所有边
	
	public static ArrayList<Integer> collectSrcActNodeIds(TreeContext tc1, TreeContext tc2, HashMap<String, LinkedList<Action>> actMap) throws Exception{
		ArrayList<Integer> srcActIds = new ArrayList<>();
        HashMap<String, LinkedList<Action>> actions = actMap;
        LinkedList<Action> updates = actions.get("update");
        LinkedList<Action> deletes = actions.get("delete");
        LinkedList<Action> inserts = actions.get("insert");
        LinkedList<Action> moves = actions.get("move");
		
		Cluster cl = new Cluster(tc1, tc2);
		for(Action a : updates) {
			int id = a.getNode().getId();
			srcActIds.add(id);
		}
		
		for(Action a : deletes) {
			int id = a.getNode().getId();
			srcActIds.add(id);
		}
		
		for(Action a : inserts) {			
			ITree sRoot = cl.traverseSRoot(a);			
			int id = sRoot.getId();
			srcActIds.add(id);
		}
		
		for(Action a : moves) {
			ITree sRoot = cl.findMovRoot(a);			
			int id = sRoot.getId();
			srcActIds.add(id);
		}
		return srcActIds;        
	}
	
	public static String printToken(SubTree st) throws Exception {
		String tokens = "";
		ITree sRoot = st.getRoot();
		List<ITree> leaves = new ArrayList<ITree>();
		leaves = traverse2Leaf(sRoot, leaves);
		for(int i=0;i<leaves.size();i++) {
			ITree leaf = leaves.get(i);
			String label = leaf.getLabel();
//			if(!label.equals("")) {
//				tokens = tokens+label;
//			}
			tokens = tokens+leaf.getId()+":"+label;
			if(i!=leaves.size()-1) {
				tokens = tokens+" ";
			}
		}	
		return tokens;
	}
	
	public static String printLeaf(DTree dt) throws Exception {
		String values = "";
		List<ITree> leaves = dt.getLeaves();
		for(ITree leaf : leaves) {
			values = values + leaf.getLabel();
		}		
		return values;		
	}
	
	public static String printDTree(DTree dt) {
		String dString = "";
		String typeName = dt.getRootType();
		dString = dString+dt.getRoot().getId()+typeName+"{";
		List<ITree> leaves = dt.getLeaves();
		for(ITree leaf : leaves) {
			dString = dString+leaf.getId()+leaf.getLabel()+",";
		}
		dString = dString.substring(0, dString.length()-1)+"}";
		return dString;
	}
	
	public static String printParents(ITree node, TreeContext tc) {
		List<ITree> pars = node.getParents();
		String parString = "";
		for(ITree par : pars) {
			String parType = tc.getTypeLabel(par);
			parString = parString+parType;
		}
		return parString;
	}
	
	public static List<ITree> traverse2Leaf(ITree node, List<ITree> leafList) throws Exception{//从根节点深度遍历至叶子节点，优先确定path相同的leaf mappings
		List<ITree> childList = node.getChildren();
		if(node.isLeaf()){
			leafList.add(node);
		}else{
			for(ITree child : childList){
				leafList = traverse2Leaf(child, leafList);
			}
		}
		return leafList;
	}
	
	public static HashMap<String, LinkedList<Action>> collectAction(TreeContext tc1, TreeContext tc2, MappingStore mappings) {
		Matcher m = Matchers.getInstance().getMatcher(tc1.getRoot(), tc2.getRoot());
        m.match();
        ActionGenerator g = new ActionGenerator(tc1.getRoot(), tc2.getRoot(), mappings);
        List<Action> actions = g.generate();
        System.out.println("ActionSize:"+actions.size());
        checkTCRoot(tc1);
        checkTCRoot(tc2);
        HashMap<Integer, Integer> mapping1 = new HashMap<Integer, Integer>();
        for(Mapping map : m.getMappings()) {
        	ITree src = map.getFirst();
        	ITree dst = map.getSecond();
        	mapping1.put(src.getId(), dst.getId());
        }
        System.out.println("mapSize:"+mapping1.size());	
        HashMap<String, LinkedList<Action>> actionMap = new HashMap<>();
        HashMap<Integer, Action> moves = new HashMap<Integer, Action>();
        HashMap<Integer, Action> updates = new HashMap<Integer, Action>();
        HashMap<Integer, Action> inserts = new HashMap<Integer, Action>();
        HashMap<Integer, Action> deletes = new HashMap<Integer, Action>();
        ArrayList<Integer> movId = new ArrayList<>();
        ArrayList<Integer> uptId = new ArrayList<>();
        ArrayList<Integer> addId = new ArrayList<>();
        ArrayList<Integer> delId = new ArrayList<>();
        LinkedList<Action> mov = new LinkedList<>();
        LinkedList<Action> upt = new LinkedList<>();
        LinkedList<Action> add = new LinkedList<>();
        LinkedList<Action> del = new LinkedList<>();
        for (Action a : actions) {
            ITree src = a.getNode();
            if (a instanceof Move) {
                moves.put(src.getId(), a);
                movId.add(src.getId());
//                System.out.println(((Move)a).toString());
            } else if (a instanceof Update) {
            	updates.put(src.getId(), a);
            	uptId.add(src.getId());
//                System.out.println(((Update)a).toString());
            } else if (a instanceof Insert) {
            	inserts.put(src.getId(), a);
            	addId.add(src.getId());
//                System.out.println(((Insert)a).toString());
            } else if (a instanceof Delete) {
            	deletes.put(src.getId(), a);
            	delId.add(src.getId());
//            	System.out.println(((Delete)a).toString());
            }           
         }
        for(int n : movId) {
        	Action tmp = moves.get(n);
        	mov.add(tmp);
        }
        for(int n : uptId) {
        	Action tmp = updates.get(n);
        	upt.add(tmp);
        }
        for(int n : addId) {
        	Action tmp = inserts.get(n);
        	add.add(tmp);
        }
        for(int n : delId) {
        	Action tmp = deletes.get(n);
        	del.add(tmp);
        }
        actionMap.put("move", mov);
        actionMap.put("update", upt);
        actionMap.put("insert", add);
        actionMap.put("delete", del);
        
        return actionMap;
	}
	
	public static void printAllActions(TreeContext tc1, TreeContext tc2, HashMap<String, LinkedList<Action>> actionMap) {
		Matcher m = Matchers.getInstance().getMatcher(tc1.getRoot(), tc2.getRoot());
        m.match();
        
        for(Mapping map : m.getMappings()) {
        	ITree src = map.getFirst();
        	ITree dst = map.getSecond();
        	System.out.println("Mapping:"+src.getId()+"->"+dst.getId());
        }                  
        
        LinkedList<Action> moves = actionMap.get("move");
        LinkedList<Action> updates = actionMap.get("update");
        LinkedList<Action> inserts = actionMap.get("insert");
        LinkedList<Action> deletes = actionMap.get("delete");
        for(int i=0;i<moves.size();i++) {
        	Move act = (Move)moves.get(i);
        	System.out.println("Mov:"+act.getNode().getId()+"->"+act.getParent().getId()+","+act.getPosition());
        }
        for(int i=0;i<updates.size();i++) {
        	Update act = (Update)updates.get(i);
        	System.out.println("Upt:"+act.getNode().getId()+","+act.getValue());
        }
        for(int i=0;i<inserts.size();i++) {
        	Action act = inserts.get(i);
        	ITree dst = act.getNode();
        	System.out.println("dstID:"+dst.getId());
//        	ITree src = mappings.getSrc(dst);
        	String label = Integer.toString(dst.getId());
            if (dst.hasLabel()) label = label+","+dst.getLabel();
            if (tc2.hasLabelFor(dst.getType()))
            	label = label+","+tc2.getTypeLabel(dst.getType());
        	System.out.println("Add:"+label+"->"+dst.getParent().getId()+","+dst.getParent().getChildPosition(dst));
        }
        for(int i=0;i<deletes.size();i++) {
        	Delete act = (Delete)deletes.get(i);
        	System.out.println("Del:"+act.getNode().getId());
        }
        	
	}	
	
	public static ITree findNode(TreeContext tc, int id) throws Exception{
		ITree root = tc.getRoot();
		List<ITree> nodes = new ArrayList<>();
		nodes = collectNode(root, nodes);
		ITree target = null;
		for(ITree node : nodes) {
			if(node.getId()==id) {
				target = node;
			}
		}
		return target;
	}	
	
	public static Boolean ifSRoot(String typeLabel) {
		if(typeLabel=="decl_stmt"||typeLabel=="expr_stmt"||typeLabel=="while"||typeLabel=="for"||
					typeLabel=="function"||typeLabel=="constructor"||typeLabel=="if") {
			return true;
		}else
			return false;
	}//SRoot条件可能有遗漏
	
	public static Boolean ifChild(ITree root, ITree target) throws Exception {
		Boolean findNode = null;
//		if(root.getId()==target.getId())
//			throw new Exception("error id! "+target.getId());
		List<ITree> childs = TreeUtils.preOrder(root);
		for(ITree t : childs) {
			if(t.equals(target))
				findNode = true;
		}
		if(findNode==null)
			findNode = false;
		return findNode;
	}
	
	public static void checkTCRoot(TreeContext tc){//发现action后有迷之根节点
		ITree root = tc.getRoot();
		if(root.getParent()!=null) {
			System.err.println("find error root!!");
			root.setParent(null);
		}
	}

}
