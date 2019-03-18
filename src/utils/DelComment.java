package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
 
/**
 * ɾ��Java�����е�ע��
 * 
 * @author Alive
 * @build 2010-12-23
 */
public class DelComment {
	
	public static void main(String[] args) {
      clearComment("migrations_test"); //ɾ��Ŀ¼������java||cpp�ļ�ע��
      clearInclude("migrations_test"); //ɾ��Ŀ¼������java||cpp�ļ�header
      //ɾ��ĳ�������ļ���ע��
//      clearComment("Absolute3DLocalizationElement2.cpp");
  }
 
    private static int count = 0;
 
    /**
     * ɾ���ļ��еĸ���ע�ͣ�����//��/* * /��
     * @param charset �ļ�����
     * @param file �ļ�
     */
    public static void clearComment(File file, String charset) {
        try {
        	System.out.println("-----��ʼ�����ļ���" + file.getAbsolutePath());
            //�ݹ鴦���ļ���
            if (!file.exists()) {
                return;
            }
 
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    clearComment(f, charset); //�ݹ����
                }
                return;
            } else if (!file.getName().endsWith(".cpp")&&!file.getName().endsWith(".java")) {
                //��cpp or java�ļ�ֱ�ӷ���
                return;
            }           
 
            //���ݶ�Ӧ�ı����ʽ��ȡ
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
            StringBuffer content = new StringBuffer();
            String tmp = null;
            while ((tmp = reader.readLine()) != null) {
                content.append(tmp);
                content.append("\n");
            }
            reader.close();
            String target = content.toString();
            //String s = target.replaceAll("\\/\\/[^\\n]*|\\/\\*([^\\*^\\/]*|[\\*^\\/*]*|[^\\**\\/]*)*\\*\\/", ""); //��������ժ�����ϣ���һ������޷����㣨/* ...**/���������޸�
            String s = target.replaceAll("\\/\\/[^\\n]*|\\/\\*([^\\*^\\/]*|[\\*^\\/*]*|[^\\**\\/]*)*\\*+\\/", "");
            //System.out.println(s);
            //ʹ�ö�Ӧ�ı����ʽ���
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
            out.write(s);
            out.flush();
            out.close();
            count++;
            System.out.println("-----�ļ��������---" + count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
              ���ֲ�һ����include������Ӱ��action���ɣ��Ƿ���û����dependency�Ĺ�ϵ��
    */
    
    public static void clearInclude(File file, String charset) {
        try {
        	System.out.println("-----��ʼ�����ļ���" + file.getAbsolutePath());
            //�ݹ鴦���ļ���
            if (!file.exists()) {
                return;
            }
 
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                	clearInclude(f, charset); //�ݹ����
                }
                return;
            } else if (!file.getName().endsWith(".cpp")&&!file.getName().endsWith(".java")) {
                //��cpp or java�ļ�ֱ�ӷ���
                return;
            }           
 
            //���ݶ�Ӧ�ı����ʽ��ȡ
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
            StringBuffer content = new StringBuffer();
            String tmp = null;
            while ((tmp = reader.readLine()) != null) {
//            	System.out.println(tmp);
            	if(!tmp.contains("#include")&&!tmp.contains("#define")) {
            		content.append(tmp);
                    content.append("\n");
            	}
            }
            reader.close();
            String target = content.toString();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
            out.write(target);
            out.flush();
            out.close();
            count++;
            System.out.println("-----�ļ��������---" + count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    public static void clearComment(String filePath, String charset) {
        clearComment(new File(filePath), charset);
    }
 
    public static void clearComment(String filePath) {
        clearComment(new File(filePath), "UTF-8");
    }
 
    public static void clearComment(File file) {
    	clearComment(file, "UTF-8");
    }
    
    public static void clearInclude(String filePath, String charset) {
    	clearInclude(new File(filePath), charset);
    }
 
    public static void clearInclude(String filePath) {
    	clearInclude(new File(filePath), "UTF-8");
    }
 
    public static void clearInclude(File file) {
    	clearInclude(file, "UTF-8");
    }
 
 
}
