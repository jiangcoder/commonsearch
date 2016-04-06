package com.jiangcoder.search.segment;

public class ENode {
	public int mEndPoint = -1;//边的入节点
	public ENode mNext = null; //兄弟边
	public int mWeight = 1;//边的权重
	public String mWord = ""; //边对应的词
}
