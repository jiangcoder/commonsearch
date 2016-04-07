package org.web;

public class DeadLock {
	private static String A="A";
	private static String B="B";
	public static void main(String[] args) {
		
		Thread t1=new Thread( new Runnable() {
			public void run() {
				synchronized (A) {
					
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						synchronized (B) {
							System.out.println("nihao");
					}
				}
			}
		});
		Thread t2=new Thread( new Runnable() {
			public void run() {
				synchronized (B) {
					synchronized (A) {
						System.out.println("hello world");
					}
				}
			}
		});
		t1.start();
		t2.start();
	}
	 
}
