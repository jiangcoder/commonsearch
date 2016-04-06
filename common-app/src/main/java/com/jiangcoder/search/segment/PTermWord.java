package com.jiangcoder.search.segment;

public class PTermWord extends DataTypeTransform{

	public Double mPro = 1.0;//概率值
	public Double mLogPro = 1.0; //-log(mPro)
	public static final int mSize = 16;
	public byte[] toByteArray()
	{
		byte[] p1 = longToByte(Double.doubleToLongBits(mPro));
		byte[] p2 = longToByte(Double.doubleToLongBits(mLogPro));		
		byte[] p = new byte[16];
	
		System.arraycopy(p1, 0, p, 0, p1.length);
		System.arraycopy(p2, 0, p, p1.length, p2.length);
		
		return p;
	}
	public int size()
	{
		return mSize;
	}
	public PTermWord(byte[] p)
	{
		mPro = Double.longBitsToDouble(byteToLong(p, 0));
		mLogPro = Double.longBitsToDouble(byteToLong(p, 8));
	}
	public PTermWord()
	{
		mPro = 0.0;//概率值
		mLogPro = 1.0; 	
	}

}
