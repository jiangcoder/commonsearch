package com.jiangcoder.search.segment;

public class Term {
	public int mStart = 0;//在句子中起始位置
	public int mLen = -1; //预留：词的长度
	public String mPreWord = "";//词汇的前一个词汇， 如果是中文的就写，不是中文的就是空""
	public String mWord = "";//词汇
	public int mType = 0;// 1: 中文   2：英文   3：数字   4:  强制合并结果 例如：小米3  但是优先合并英文数字 例如： 小米3g  标题中为 小米 3 g  小米3 3g   
							//query中为 小米 3g
	public Double mPro = 0.0;//词汇一元概率
}
