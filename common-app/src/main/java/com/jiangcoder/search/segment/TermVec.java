package com.jiangcoder.search.segment;
import java.util.HashSet;

public class TermVec {
	public HashSet<Integer> mStartPos = new HashSet<Integer>();//在句子中起始位置
	public int mLen = -1; //预留：词的长度   如果是转化来的同义词，长度为原词的长度，排序的时候用的上
	public String mWord = "";//词汇
	public String mPreWord = "";//针对query概率切分，该词汇是中文，前一个词汇也是中文，前一个词汇记录到这儿
	public int mType = 0; // 1: 中文   2：英文   3：数字 4:  强制合并结果
	public boolean mBeSource = true;// true： 原词汇，  false： 同义词
	public Double mPro = 0.0;//词汇一元概率
	                         //中文： 有的话就是统计概率，  无的话就是默认概率：0.0000001 
	                         //英文或数字： 默认概率 0.0001
	                         //强制  概率： 0.01  
	public int mIntegerPro = 0;//从1-10
	                           //
}
