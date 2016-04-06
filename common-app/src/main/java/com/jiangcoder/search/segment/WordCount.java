package com.jiangcoder.search.segment;


public class WordCount extends DataTypeTransform{
	public int mCnt = 1;
	public static final int mSize = 4;
	public int size()
	{
		return mSize;
	}
	public byte[] toByteArray()
	{
		return intToByte(mCnt);
	}
	public WordCount(byte[] p)
	{
		mCnt = byteToInt(p, 0);
	}
	
	public WordCount(int i)
	{
		mCnt = i;
	}
}
