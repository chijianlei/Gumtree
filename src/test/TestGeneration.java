package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stringtemplate.v4.compiler.STParser.template_return;

import gumtreediff.actions.ActionGenerator;
import gumtreediff.actions.model.Action;
import gumtreediff.actions.model.Delete;
import gumtreediff.actions.model.Insert;
import gumtreediff.actions.model.Move;
import gumtreediff.actions.model.Update;
import gumtreediff.gen.jdt.JdtTreeGenerator;
import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.io.ActionsIoUtils;
import gumtreediff.io.TreeIoUtils;
import gumtreediff.io.ActionsIoUtils.ActionSerializer;
import gumtreediff.matchers.Mapping;
import gumtreediff.matchers.MappingStore;
import gumtreediff.matchers.Matcher;
import gumtreediff.matchers.Matchers;
import gumtreediff.tree.ITree;
import gumtreediff.tree.TreeContext;
import split.Pruning;
import utils.Utils;

public class TestGeneration {
	
	public static void main(String args[]) throws Exception{
		String input = "";
		String path = "talker.cpp";
//		String path = "migrations_test/Absolute3DLocalizationElement/Absolute3DLocalizationElement.cpp";
		File cppfile = new File(path);
		TreeContext tc = new SrcmlCppTreeGenerator().generateFromFile(cppfile);
		String path2 = "talker2.cpp";
//		String path2 = "migrations_test/Absolute3DLocalizationElement/Absolute3DLocalizationElement2.cpp";
		File cppfile2 = new File(path2);
		TreeContext tc2 = new SrcmlCppTreeGenerator().generateFromFile(cppfile2);       
        System.out.println(tc.getRoot().getSize());
        System.out.println(tc.getRoot().getType());
        List<ITree> childs = tc2.getRoot().getChildren();
        for(ITree tmp : childs) {
        	System.out.println(tmp.getId());
        }
        
        Matcher m = Matchers.getInstance().getMatcher(tc.getRoot(), tc2.getRoot());
        m.match();
        MappingStore mappings = m.getMappings();
        ActionGenerator g = new ActionGenerator(tc.getRoot(), tc2.getRoot(), m.getMappings());
        List<Action> actions = g.generate();
        Pruning pt = new Pruning(tc, tc2, mappings);
        pt.pruneTree();//Prune the tree.
        String out = "testGraph.txt";
        BufferedWriter wr = new BufferedWriter(new FileWriter(out));
        wr.append(TreeIoUtils.toDot(tc, mappings, actions, true).toString());
        wr.flush();
        wr.close();
        String out2 = "testGraph2.txt";
        BufferedWriter wr2 = new BufferedWriter(new FileWriter(out2));
        wr2.append(TreeIoUtils.toDot(tc2, mappings, actions, false).toString());
        wr2.flush();
        wr2.close(); 
        
//        System.out.println(TreeIoUtils.toDot(tc, mappings, actions, true).toString());
//        System.out.println(TreeIoUtils.toDot(tc2, mappings, actions, false).toString());
        System.out.println(TreeIoUtils.toXml(tc).toString());
        
        HashMap<Integer, Integer> mapping = new HashMap<>();       
        for(Mapping map : mappings) {
        	ITree src = map.getFirst();
        	ITree dst = map.getSecond();
//        	System.out.println("Mapping:"+src.getId()+"->"+dst.getId());
        	mapping.put(src.getId(), dst.getId());
        }
//        for(Map.Entry<Integer, Integer> entry : mapping.entrySet()) {
//        	System.out.println(entry.getKey()+"->"+entry.getValue());
//        }
        
        System.out.println("ActionSize:" + actions.size());
//        for (Action a : actions) {
//            ITree src = a.getNode();
//            if (a instanceof Move) {
//                ITree dst = mappings.getDst(src);
//                System.out.println(((Move)a).toString());
//            } else if (a instanceof Update) {
//                ITree dst = mappings.getDst(src);
//                System.out.println(((Update)a).toString());
//            } else if (a instanceof Insert) {
//                ITree dst = a.getNode();
//                System.out.println(((Insert)a).toString());
//            } else if (a instanceof Delete) {
//            	System.out.println(((Delete)a).toString());
//            }
//        }
        
//		System.out.println(ActionsIoUtils.toXml(tc, g.getActions(), m.getMappings()).toString());
        String out1 = "testMapping.txt";
        BufferedWriter wr1 = new BufferedWriter(new FileWriter(out1));
//        System.out.println(ActionsIoUtils.toJson(tc, g.getActions(), m.getMappings()).toString());
        wr1.append(ActionsIoUtils.toXml(tc, g.getActions(), m.getMappings()).toString()); 
        wr1.flush();
        wr1.close();       
        
//		System.out.println(ActionsIoUtils.toText(tc, g.getActions(), m.getMappings()).toString());
//        for(ITree c : t.getChildren()) {
//        	if(c.getLabel()!=null)
//        		System.out.println(c.getLabel());
//        }
		List<ITree> nodes = new ArrayList<>();
		nodes = Utils.collectNode(tc2.getRoot(), nodes);
		System.out.println(nodes.size());

	}
		
		
		      

}
