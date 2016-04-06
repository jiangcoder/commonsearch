package com.jiangcoder.search.segment;

public class TermWord extends  DataTypeTransform{
	public int mSubWordNum = 1;
	public int mTotalCnt = 1;
	public static final int mSize = 8;
	public byte[] toByteArray()
	{
		byte[] p1 = intToByte(mSubWordNum);
		byte[] p2 =intToByte(mTotalCnt);	
		byte[] p = new byte[8];
		System.arraycopy(p1, 0, p, 0, p1.length);
		System.arraycopy(p2, 0, p, p1.length, p2.length);
		return p;
	}
	public int size()
	{
		return mSize;
	}
	public TermWord(byte[] p)
	{
		mSubWordNum = byteToInt(p, 0);
		mTotalCnt = byteToInt(p, 4);
	}
	public TermWord()
	{
		mSubWordNum = 1;
		mTotalCnt = 1;
	}
}
