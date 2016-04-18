package com.jiangcoder.search.interview.dhgate;

public class Y extends X{

	public Y() {
		System.out.println("Y");
	}
	public void print(){
		System.out.println("abc");
	}
	public static void main(String[] args) {
		X x=new X();
		x.print();
		x=new Y();
		x.print();
		Y y=new Y();
		y.print();
	}
}
