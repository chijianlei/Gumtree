package nodecluster;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import similarity.Migration;

public class Cluster {
	private TreeContext tc1;
	private TreeContext tc2;
	private MappingStore mapping;
	private HashMap<ITree, ITree> node2rootMap = new HashMap<>();//insert专用
	private HashMap<ITree, ITree> parMap = new HashMap<>();//insert专用
	
	public static void main (String args[]) throws Exception{
		String path = "talker.cpp";
		File cppfile = new File(path);
		TreeContext tc1 = new SrcmlCppTreeGenerator().generateFromFile(cppfile);
		String path2 = "talker2.cpp";
		File cppfile2 = new File(path2);
		TreeContext tc2 = new SrcmlCppTreeGenerator().generateFromFile(cppfile2);
		Cluster cl = new Cluster(tc1, tc2);
		cl.clusterActions(tc1, tc2);
	}
	
	public Cluster(TreeContext tC1, TreeContext tC2) {
		tc1 = tC1;
		tc2 = tC2;
		Matcher m = Matchers.getInstance().getMatcher(tc1.getRoot(), tc2.getRoot());
        m.match();
        mapping = m.getMappings();
	}
	
	public void clusterActions(TreeContext tC1, TreeContext tC2) throws Exception {
		HashMap<String, LinkedList<Action>> actions = Utils.collectAction(tc1, tc2);
        
        for(Mapping map : mapping) {
        	ITree src = map.getFirst();
        	ITree dst = map.getSecond();
        	System.out.println("Mapping:"+src.getId()+"->"+dst.getId());
        }                  
        		
        LinkedList<Action> updates = actions.get("update");
		System.out.println("updsize:"+updates.size());
		LinkedList<Action> deletes = actions.get("delete");
		System.out.println("delsize:"+deletes.size());
		LinkedList<Action> inserts = actions.get("insert");
		System.out.println("addsize:"+inserts.size());
		LinkedList<Action> moves = actions.get("move");	
		System.out.println("movsize:"+moves.size());
		HashMap<Integer, ArrayList<Action>> uptParentIds = new HashMap<>();
		HashMap<Integer, ArrayList<Action>> uptClusters = new HashMap<>();
		HashMap<Integer, ArrayList<Action>> delParentIds = new HashMap<>();
		HashMap<Integer, ArrayList<Action>> delClusters = new HashMap<>();
		HashMap<Integer, ArrayList<Action>> addParentIds = new HashMap<>();
		HashMap<Integer, ArrayList<Action>> addClusters = new HashMap<>();
		HashMap<Integer, ArrayList<Action>> movParentIds = new HashMap<>();
		HashMap<Integer, ArrayList<Action>> movClusters = new HashMap<>();		
		
		for(Action a : updates) {	
			System.out.println("-----findUptRoot-----");
			ITree sRoot = traverseSRoot(a);
			System.out.println("rootname:"+tc1.getTypeLabel(sRoot));		
			int id = sRoot.getId();
			if(uptParentIds.get(id)==null) {
				ArrayList<Action> acts = new ArrayList<>();
				acts.add(a);
				uptParentIds.put(id, acts);
			}else {
				ArrayList<Action> acts = uptParentIds.get(id);
				acts.add(a);
				uptClusters.put(id, acts);
			}			
		}		
		for(Action a : deletes) {
			System.out.println("-----findDelRoot-----");
			ITree sRoot = traverseSRoot(a);
			System.out.println("rootname:"+tc1.getTypeLabel(sRoot));			
			int id = sRoot.getId();
			if(delParentIds.get(id)==null) {
				ArrayList<Action> acts = new ArrayList<>();
				acts.add(a);
				delParentIds.put(id, acts);
			}else {
				ArrayList<Action> acts = delParentIds.get(id);
				acts.add(a);
				delClusters.put(id, acts);
			}
		}
		for(Action a : inserts) {
			System.out.println("-----findAddRoot-----");
			ITree sRoot = traverseSRoot(a);
			System.out.println("rootname:"+tc1.getTypeLabel(sRoot));			
			int id = sRoot.getId();
			if(addParentIds.get(id)==null) {
				ArrayList<Action> acts = new ArrayList<>();
				acts.add(a);
				addParentIds.put(id, acts);
			}else {
				ArrayList<Action> acts = addParentIds.get(id);
				acts.add(a);
				addClusters.put(id, acts);
			}
		}
		for(Action a : moves) {
			System.out.println("-----findMovRoot-----");
			ITree sRoot = findMovRoot(a);
			System.out.println("rootname:"+tc1.getTypeLabel(sRoot));			
			int id = sRoot.getId();
			if(movParentIds.get(id)==null) {
				ArrayList<Action> acts = new ArrayList<>();
				acts.add(a);
				movParentIds.put(id, acts);
			}else {
				ArrayList<Action> acts = movParentIds.get(id);
				acts.add(a);
				movClusters.put(id, acts);
			}
		}
		
		
		System.out.println("=====UPDCluster====="+uptClusters.size());
		for(Map.Entry<Integer, ArrayList<Action>> entry : uptClusters.entrySet()) {
			ArrayList<Action> acts = entry.getValue();
			ITree newRoot = downRoot(acts);
			System.out.println("downRootname:"+tc1.getTypeLabel(newRoot));
			printPath(newRoot);
		}
		System.out.println("=====DELCluster====="+delClusters.size());
		for(Map.Entry<Integer, ArrayList<Action>> entry : delClusters.entrySet()) {
			ArrayList<Action> acts = entry.getValue();
			ITree newRoot = downRoot(acts);
			System.out.println("downRootname:"+tc1.getTypeLabel(newRoot));
			printPath(newRoot);
		}
		System.out.println("=====ADDCluster====="+addClusters.size());
		for(Map.Entry<Integer, ArrayList<Action>> entry : addClusters.entrySet()) {
			ArrayList<Action> acts = entry.getValue();
			ITree newRoot = downRoot(acts);
			System.out.println("downRootname:"+tc1.getTypeLabel(newRoot));
			printPath(newRoot);
		}
		System.out.println("=====MOVCluster====="+movClusters.size());
		for(Map.Entry<Integer, ArrayList<Action>> entry : movClusters.entrySet()) {
			ArrayList<Action> acts = entry.getValue();
			ITree newRoot = downRoot(acts);
			System.out.println("downRootname:"+tc1.getTypeLabel(newRoot));
			printPath(newRoot);
		}				
	}
	
	public ITree findMovRoot(Action a) throws Exception {
		if(!(a instanceof Move))
			throw new Exception("action is not move!");
		ITree dst = ((Move)a).getParent();
		if(node2rootMap.get(dst)==null) {
			ITree sRoot = mapping.getDst(dst);
			if(sRoot == null)
				throw new Exception("map error!");
			return sRoot;
		}//发现另一种情况,move连接的节点不在insert结果中，直接从mapping中找	
		
		ITree sRoot = node2rootMap.get(dst);
		//move连接的父亲是tc2中节点，直接从insert结果中找，必然因为insert插入到tc1中了		
		return sRoot;	
	}
	
	public void printPath(ITree newRoot) {//打印从newRoot到SRoot
		ITree SRoot = newRoot;
		String typeLabel = tc1.getTypeLabel(newRoot);	
		String allPath = typeLabel;
		while(!Utils.ifSRoot(typeLabel)) {//可能有问题，要注意循环条件
			typeLabel = tc1.getTypeLabel(SRoot.getParent());
			allPath = allPath+"<-"+typeLabel;
			SRoot = SRoot.getParent();
		}
		System.out.println(allPath);
	}
	
	public ITree traverseRealParent(Action a) throws Exception {
//		System.out.println("parMapSize:"+parMap.size());
		ITree dst = a.getNode();
		ITree par1 = null;
		ITree par2 = dst.getParent();;
		for(Mapping map : mapping) {			
			ITree first = map.getFirst();
        	ITree second = map.getSecond();
        	if(second.equals(par2)) {
//        		System.out.println("getMap:"+first.getId()+"->"+second.getId());
        		par1 = first;
        		parMap.put(dst, par1);
        	}
		}
		if(par1 == null) {//仍然为-1说明不在mapping中，action为insert子树中的节点
			if(parMap.get(par2)!=null) {
				par1 = parMap.get(par2);//内部子节点的父亲节点与子树的父亲节点绑定
				parMap.put(dst, par1);
			}else
				throw new Exception("error parMap!");
		}	
		return par1;
	}//搜索该action根语句root,Insert专用	
	
	public ITree traverseSRoot(Action a) throws Exception {
		ITree target = a.getNode();
		if(a instanceof Update||a instanceof Delete) {
			ITree src = a.getNode();
			String typeLabel = tc1.getTypeLabel(src);				
			while(!Utils.ifSRoot(typeLabel)) {//可能有问题，要注意循环条件
				ITree par = src.getParent();
				typeLabel = tc1.getTypeLabel(par);
//				System.out.println("typeLabel:"+typeLabel);
				src = par;
			}
			target = src;
		}
		if(a instanceof Insert) {
			ITree dst = ((Insert)a).getNode();
			String typeLabel = tc2.getTypeLabel(dst);
//			System.out.println(dst.getId()+"typeLabel:"+typeLabel);
			ITree par1 = null;
			ITree par2 = dst.getParent();
//			System.out.println(par2.getId());
//			int pos = ((Insert)a).getPosition();
	        for(Mapping map : mapping) {
	        	ITree first = map.getFirst();
	        	ITree second = map.getSecond();
	        	if(second.equals(par2)) {
//	        		System.out.println("getMap:"+first.getId()+"->"+second.getId());
	        		par1 = first;
	        		typeLabel = tc1.getTypeLabel(par1);
	        		while(!Utils.ifSRoot(typeLabel)) {//可能有问题，要注意循环条件
//	        			System.out.println("LabelID:"+par1.getId());
	        			if(par1.isRoot())
	        				break;//发现有直接连接在总树根节点的情况
	        			else {
	        				ITree tmpPar = par1.getParent();	        			
		        			typeLabel = tc1.getTypeLabel(tmpPar);
//		        			System.out.println("typeLabel:"+typeLabel);
		        			par1 = tmpPar;
	        			}	        			
	        		}
	        		node2rootMap.put(dst, par1);//绑定子节点用
	        		break;	        		
	        	}
	        }
	        if(par1 == null) {//仍然为-1说明不在mapping中，action为insert子树中的节点
	        	if(node2rootMap.get(par2)!=null) {
	        		par1 = node2rootMap.get(par2); 
	        		node2rootMap.put(dst, par1);
	        	}else
        			throw new Exception("error childAction!");
	        }
			target = par1;			
		}
		if(a instanceof Move) {
			ITree src = ((Move) a).getNode();
			ITree dst = ((Move) a).getParent();
			String typeLabel = tc1.getTypeLabel(src);
//			System.out.println(dst.getId()+"typeLabel:"+typeLabel);
//			System.out.println("dstPar:"+dst.getParent().getId());
			while(!Utils.ifSRoot(typeLabel)) {//可能有问题，要注意循环条件
				ITree par = dst.getParent();
				typeLabel = tc1.getTypeLabel(par);
//				System.out.println(dst.getId()+"typeLabel:"+typeLabel);
				dst = par;
			}
			target = dst;
		}
		return target;		
	}//搜索该action根语句root
	
	public ITree downRoot(ArrayList<Action> actions) throws Exception {//topdown or downtop?
		ITree sRoot = null;
		List<Integer> parents = new ArrayList<>();
		Boolean ifChild = true;
		Action exampleA = actions.get(0);
		if(exampleA instanceof Update) {
			sRoot = traverseSRoot(actions.get(0));
			for(Action a : actions) {
				parents.add(a.getNode().getParent().getId());
				Update act = (Update)a;
				System.out.println("Upt:"+act.getNode().getId()+","+act.getValue());
			}
		}else if(exampleA instanceof Delete) {
			sRoot = traverseSRoot(actions.get(0));
			for(Action a : actions) {
				parents.add(a.getNode().getParent().getId());
				Delete act = (Delete)a;
				System.out.println("Del:"+act.getNode().getId());
			}
		}else if(exampleA instanceof Insert) {
			sRoot = traverseSRoot(actions.get(0));
			for(Action a : actions) {				
				ITree realPar = traverseRealParent(a);
				parents.add(realPar.getId());
				Insert act = (Insert)a;
				String out = "Add:"+act.getNode().getId()+"->"+act.getNode().getParent().getId();
				if(act.getParent().getId()!=act.getNode().getParent().getId())
					out = out+"("+act.getParent().getId()+")";
				out = out+","+act.getPosition();
				System.out.println(out);
			}
		}else if(exampleA instanceof Move) {
			ITree dst = ((Move)actions.get(0)).getParent();
			sRoot = node2rootMap.get(dst);
			String typeLabel = tc1.getTypeLabel(sRoot);
			System.out.println(sRoot.getId()+"sRootLabel:"+typeLabel);
			for(Action a : actions) {
				Move act = (Move)a;
				parents.add(act.getParent().getId());
				System.out.println("Mov:"+act.getNode().getId()+"->"+act.getParent().getId()
						+","+act.getPosition());
			}				
		}			
		
		while(ifChild==true) {//下降根节点必须保证覆盖所有actionNode
			System.out.println("par:"+sRoot.getId());
			if(parents.contains(sRoot.getId())) {
				break;//如果下降到只比action的父节点高一层,break
			}
				
			List<ITree> childs = sRoot.getChildren();
			if(childs.isEmpty()) 
				throw new Exception("Error!");
			else if(childs.size()==1)
				sRoot = childs.get(0);
			else {
				ITree tmpPar = null;
				for(int i=0;i<childs.size();i++) {
					tmpPar = childs.get(i);
					for(Action a : actions) {
						ITree target = a.getNode();
						ifChild = Utils.ifChild(tmpPar, target);
						System.out.println(Boolean.toString(ifChild)+" "+target.getId()+","+tmpPar.getId());
						if(ifChild==false)
							break;
					}
					if(ifChild==true) 
						break;
					else
						continue;						
				}
				if(ifChild==true)
					sRoot = tmpPar;
			}		
		}
		return sRoot;
	}//从根语句root下降rootnode,取所有action的公有最下方root
	
		
		  

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
