package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.*;
import java.util.Map;

import gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import gumtreediff.matchers.MappingStore;
import gumtreediff.tree.TreeContext;

public class test1 {

	public static void main(String args[]) throws Exception{
		Map<String, Integer> map = new TreeMap<String, Integer>();
        map.put("a", 2);
        map.put("b", 3);
        map.put("c", 1);
 
        // ͨ��ArrayList���캯����map.entrySet()ת����list
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        // ͨ���Ƚ���ʵ�ֱȽ�����
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> mapping1, Map.Entry<String, Integer> mapping2) {
                return mapping2.getValue().compareTo(mapping1.getValue());
            }
        });
 
        for(Map.Entry<String,Integer> mapping:list){ 
            System.out.println(mapping.getKey()+":"+mapping.getValue()); 
       } 
	}
	
		


}
