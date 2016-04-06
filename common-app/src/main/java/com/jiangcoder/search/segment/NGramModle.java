package com.jiangcoder.search.segment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGramModle {
	protected static Logger logger = LoggerFactory.getLogger(NGramModle.class);
	private void printLog(String str)
	{
		logger.info(str);
	}
	
	private int mNGramNum = 2;
	
	private int mTotalNum = 0;//训练总词汇数  将来计算概率的时候，如果出现为见过的词汇，可以默认平滑概率为: 1／mTotalNum
	/*
	private HashMap<String, Integer> mWordToNum = new HashMap<String, Integer>();//词汇--->出现次数
	private HashMap<String, TermWord > mWordToWordNum = new HashMap<String, TermWord >();//<词汇>---->［(词汇,出现次数）， 总数目］
	private HashMap<String, TermWord > mDoubleWordToWordNum = new HashMap<String, TermWord >();//<词汇 词汇>---->[(词汇,出现次数）, 总数目]
	
	public HashMap<String, PTermWord> mWordToPro = new HashMap<String, PTermWord>();//词汇--->平滑概率
	private HashMap<String, PTermWord> mDWordToPro = new HashMap<String, PTermWord>();//<Wi-1,Wi>--->P(Wi/Wi-1)  
	private HashMap<String, PTermWord> mTWordToPro = new HashMap<String, PTermWord>();//<Wi-2,Wi-1,Wi>--->P(Wi/(Wi-2,Wi-1))
	*/
	//////////////////////////////////////////////////////////////////////////////////
	private StaticHash<WordCount> mStaticWordToNum = new StaticHash<WordCount>();
	public StaticHash<PTermWord> mStaticWordToPro = new StaticHash<PTermWord>();
	private StaticHash<PTermWord> mStaticDWordToPro = new StaticHash<PTermWord>();
	private StaticHash<TermWord> mStaticWordToWordNum = new StaticHash<TermWord>();
	private StaticHash<WordCount> mStaticTupleMap = new StaticHash<WordCount>();
	
	
	public void LoadModleData(String dataDir, int ngram)
	{
		char c = dataDir.charAt(dataDir.length() - 1);
		if(c != '/')
		{
			dataDir += '/';
		}
		
		mNGramNum = 2;
		if(ngram>=3)
		{
			mNGramNum = 3;
		}
		
		
		String wordtonumfile = dataDir + "ngrammodel/wordtonum.data";
		String totalnumberfile = dataDir + "ngrammodel/totalnum.data";
		LoadWordToNumAndTotalNum(wordtonumfile,totalnumberfile);
		
		String destDir = dataDir + "/ngrammodel/";
		
		String fileName = destDir + "wordtonum.data";
		if(!mStaticWordToNum.loadHashFileToMemory(fileName))			
		{
			printLog("加载" + fileName + "失败");
		}				
		fileName = destDir + "dwordtonum.data";
		if(!mStaticWordToPro.loadHashFileToMemory(fileName))
		{
			printLog("加载" + fileName + "失败");
		}	
		
		fileName = destDir + "dwordtopro.data";
		if(!mStaticDWordToPro.loadHashFileToMemory(fileName))
		{
			printLog("加载" + fileName + "失败");
		}	
					
		fileName = destDir + "tuplewordmap.data";
		if(!mStaticTupleMap.loadHashFileToMemory(fileName))
		{
			printLog("加载" + fileName + "失败");
		}	
		
		fileName = destDir + "tupleword.data";
		if(!mStaticWordToWordNum.loadHashFileToMemory(fileName))
		{
			printLog("加载" + fileName + "失败");
		}	
		printLog("分词加载成功......");
	}
	
	//加载统计数据
	private void LoadWordToNumAndTotalNum(String wordtonumfile, String totalnumberfile)
	{
		try 
		{
			String datafile = totalnumberfile;
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
					if(lineTxt.length() > 0)
					{
						mTotalNum = Integer.parseInt(lineTxt); //训练集词汇累计总数
						if(mTotalNum < 10)
						{
							printLog("训练词汇太少");
							mTotalNum = 10;
						}
						
						break;
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
	
	public Double computeSentenceProbility(String sentence)
	{
		if(mNGramNum == 2)
		{
			return computeSentenceProbility_2Gram(sentence);
		}else
		{
			return computeSentenceProbility_3Gram(sentence);
		}
	}
	
	//词汇用空格隔开
	public Double computeSentenceProbility_3Gram(String sentence)
	{
		String[] Arr = sentence.split("\t");
		Double logP = 0.0;
		
		for(int i=0; i<Arr.length; i++)
		{
			logP += transferLogProbility_3Gram(Arr, i);
		}	
		
		return logP;
	}
	
	public Double computeSentenceProbility_2Gram(String sentence)
	{
		String[] Arr = sentence.split("\t");
		Double logP = 0.0;
		
		for(int i=0; i<Arr.length; i++)
		{
			logP += transferLogProbility_2Gram(Arr, i);
		}	
		
		return logP;
	}
	
	private Double transferLogProbility_2Gram(String[] arr, int pos)
	{
		if(pos < 0)
		{
			return Double.MAX_VALUE;
		}
	
		
		if(pos == 0)//单个词的平滑概率
		{	
			return oneWordLogProbility(arr[0]);
		}else 
		{			
			//P(wi/wi-1)
			return twoWordLogProbility(arr[pos-1], arr[pos]);
		}
	}
	
	private Double transferLogProbility_3Gram(String[] arr, int pos)
	{
		if(pos < 0)
		{
			return Double.MAX_VALUE;
		}
		
		if(pos == 0)//单个词的平滑概率
		{
			return oneWordLogProbility(arr[0]);
		}else if(pos == 1)
		{
			return twoWordLogProbility(arr[0], arr[1]);
		}else
		{		
			return threeWordLogProbility(arr[pos-2], arr[pos-1], arr[pos]);
		}
	}
	
	public Double oneWordLogProbility(String word)
	{
		byte[] p = mStaticWordToPro.getVal(word);
		if(p!=null)
		{
			return new PTermWord(p).mLogPro;
		}
		
		return 0.0-Math.log((1.0)/mTotalNum);
	}
	
	public Double oneWordProbility(String word)
	{
		byte[] p = mStaticWordToPro.getVal(word);
		if(p!=null)
		{
			return new PTermWord(p).mPro; 
		}
		
		return  (1.0)/mTotalNum;
	}
	
	public Double twoWordLogProbility(String leftWord, String rightWord)
	{
		String key = leftWord + " " + rightWord;		
		byte[] p1 = mStaticDWordToPro.getVal(key);
		if(p1!=null)
		{
			return new PTermWord(p1).mLogPro;
		}
		
		int CW2 = 0;
		int Nplus = 0;
		int Wiplus = 0;
		byte[] p = mStaticWordToWordNum.getVal(leftWord);
		if(p!=null)
		{
			TermWord TermWord = new TermWord(p);
			Wiplus = TermWord.mTotalCnt;
			Nplus = TermWord.mSubWordNum;
		}
		else
		{
			Nplus = 1;
			Wiplus = 1;			
		}
			
		Double Pwbi = oneWordProbility(rightWord);
		Double Pwb = (CW2 + Nplus*Pwbi + 0.0)/(Wiplus + Nplus + 0.0);
		Double logP = 10000.0;
		if(Pwb > 0)
		{
			logP = 0.0-Math.log(Pwb);
		}
		return logP;
	}
	
	public Double twoWordProbility(String leftWord, String rightWord)
	{
		String key = leftWord + " " + rightWord;
		byte[] p = mStaticDWordToPro.getVal(key);
		if(p!=null)
		{
			return new PTermWord(p).mPro;
		}	
		int CW2 = 0;
		int Nplus = 0;
		int Wiplus = 0;
	
		p = mStaticWordToWordNum.getVal(leftWord);
		if(p!=null)
		{
			TermWord TermWord = new TermWord(p);
			Wiplus = TermWord.mTotalCnt;
			Nplus = TermWord.mSubWordNum;
		}
		else
		{
			Nplus = 1;
			Wiplus = 1;
		}
		
		Double Pwbi = oneWordProbility(rightWord);
		Double Pwb = (CW2 + Nplus*Pwbi + 0.0)/(Wiplus + Nplus + 0.0);
				
		return Pwb;
	}
	
	public Double threeWordLogProbility(String leftWord, String midWord, String endWord)
	{
		Double logP = 0.0;
		return logP;
	}
}
