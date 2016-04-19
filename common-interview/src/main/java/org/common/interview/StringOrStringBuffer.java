package org.common.interview;


public class StringOrStringBuffer {
	public static void main(String[] args) {
		String str="String";
		StringBuffer stringBuffer=new StringBuffer("StringBuffer");
		MethodA(str);
		MethodB(stringBuffer);
		System.out.println(str+"--"+stringBuffer);
	}

	private static void MethodB(StringBuffer stringBuffer) {
		// TODO Auto-generated method stub
		stringBuffer=new StringBuffer("newStringBuffer");
	}

	private static void MethodA(String str) {
		// TODO Auto-generated method stub
		str="newString";
	}
}
