package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.matchers.MappingStore;
import gumtreediff.tree.TreeContext;

public class test1 {

	public static void main(String args[]) throws Exception{
		HashMap<Integer, Integer> map1 = new HashMap<Integer, Integer>();
		File file1 = new File("mapping1.txt");
		BufferedReader br1 = new BufferedReader(new FileReader(file1));
		String tmpline = "";
		while((tmpline=br1.readLine())!=null) {
			int src = Integer.parseInt(tmpline.split("->")[0]);
			int dst = Integer.parseInt(tmpline.split("->")[1]);
			map1.put(src, dst);
		}
		HashMap<Integer, Integer> map2 = new HashMap<Integer, Integer>();
		File file2 = new File("mapping.txt");
		BufferedReader br2 = new BufferedReader(new FileReader(file2));
		while((tmpline=br2.readLine())!=null) {
			int src = Integer.parseInt(tmpline.split("->")[0]);
			int dst = Integer.parseInt(tmpline.split("->")[1]);
			map2.put(src, dst);
		}
		for(Map.Entry<Integer, Integer> entry : map1.entrySet()) {
			int src = entry.getKey();
			int dst = entry.getValue();
			if(map2.get(src)==null) {
				System.out.println(src+"->"+dst);
			}else if(map2.get(src)!=dst) {
				System.out.println(src+"->"+dst);
			}
		}
		System.out.println("----------------");
		for(Map.Entry<Integer, Integer> entry : map2.entrySet()) {
			int src = entry.getKey();
			int dst = entry.getValue();
			if(map1.get(src)==null) {
				System.out.println(src+"->"+dst);
			}else if(map1.get(src)!=dst) {
				System.out.println(src+"->"+dst);
			}
		}
	}
	
		


}
