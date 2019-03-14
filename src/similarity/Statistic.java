package similarity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import com.google.common.io.Files;

public class Statistic {
	
	public static void main (String args[]) throws Exception{
		String path = "migrations";
		givenStatistic(path);
	}
	
	public static void givenStatistic(String path) throws Exception {
		String inputName = "";
		File[] dirs = (new File(path)).listFiles();
		for(File dir : dirs) {
			Split sp = new Split();
			sp.getSize();
			ArrayList<Migration> migrats = new ArrayList<>();
			inputName = dir.getName();
			File[] files = dir.listFiles(); 
			String testName = files[0].getAbsolutePath();
			String miName = testName.split("\\\\")[testName.split("\\\\").length-1];//标记文件名
			String output = miName.substring(0, miName.length()-4)+".txt";
			System.out.println(output);
			File[] fileList = (new File("D:\\workspace\\eclipse2018\\gumtree")).listFiles();
			Boolean ifExist = false;
			for(File tmp : fileList) {
				String name = tmp.getName();
				if(name.equals(output)) {
					ifExist = true;
					break;
				}				
			}
			if(ifExist==true)
				continue;								
			migrats = sp.readMigration(path, inputName);
			sp.storeTrans(migrats);			
			if(files.length!=2)
				throw new Exception("error dir!!");			
			sp.suggestion(testName);		
		}			
	}
	
	public static void clearInclude(String path) {
		
	}

}
