package com.jiangcoder.search.segment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynonymWord {
	public HashMap<String, String> mSynonym = new HashMap<String, String>();
	protected static Logger logger = LoggerFactory.getLogger(SynonymWord.class);
	void printLog(String str)
	{
		logger.info(str);
	}
	
	public void init(String fileDir)
	{
		String fileName = "synonym.data";
		try 
		{
			String datafile = fileDir + fileName;
			String encoding="utf-8";
			File file=new File(datafile);
			if(file.isFile() && file.exists())
			{ 
				//判断文件是否存在
				InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);//考虑到编码格式
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineTxt = null;
				while((lineTxt = bufferedReader.readLine()) != null){
					lineTxt = lineTxt.trim();
					if(lineTxt.isEmpty())
						continue;
					
					lineTxt = lineTxt.toLowerCase();
					
					String[] arr = lineTxt.split("\t");
					if(arr.length == 2)
					{
						String left = arr[0];
						String right = arr[1];
						if(left.length() == right.length())//免去简拼 同义词本身有问题
							continue;
						
						mSynonym.put(left, right);
						mSynonym.put(right, left);
					}else if(arr.length > 2)
					{
						for(int i=0; i<arr.length; i++)
						{
							String left = arr[i];
							String right = "";
							for(int j=0; j<arr.length; j++)
							{
								if(j != i)
								{
									right += arr[j] + " ";
								}
							}
							
							right = right.trim();
							mSynonym.put(left, right);
						}
					}
				}
				read.close();
				
			}else{
				printLog("找不到指定的文件");
			}
		} catch (Exception e) 
		{
		    printLog("读取文件内容出错");
		    e.printStackTrace();
		}
	}
	
	
	//没有同义词： 返回null
	//
	public String getSynonymWord(String word)
	{
		if(word == null || word.isEmpty())
		{
			return null;
		}
		
		word = word.toLowerCase();
		word = word.trim();
		if(mSynonym.containsKey(word))
		{
			return mSynonym.get(word);
		}		
		
		return null;
	}
	
	public static void main(String[] cmdline)
	{
		String dir = "/server/gomewordsegment/";
		
		SynonymWord sw = new SynonymWord();
		sw.init(dir);
		
		String word = "小米3";
		System.out.println(word + "\t:" + sw.getSynonymWord(word));
	}
}
