package org.web;

public class QuickSort {
	 public static void main( String[] args )
	    {
	        System.out.println( "Hello World!" );
	        int a[]=new int[]{5,2,6,8,9};
	       // int a[]=new int[]{9,3,7,1};
	        getQuickSort(a,0,4);
			for(int m:a){
				System.out.println(m);
			}
	    }

	private static int [] getQuickSort(int[] a, int low, int high) {
		int i=low;int j=high;
		if(i>j) return a;
		int value=a[i];
		while(i!=j){
			while(i<j&&a[j]>=value){
				j--;
			}
			while(i<j&&a[i]<=value){
				i++;
			}
			if(i<j){
				int t=a[i];
				a[i]=a[j];
				a[j]=t;
			}
		}
		a[low]=a[i];
		a[i]=value;
		getQuickSort(a, low,i-1 );
		getQuickSort(a, i+1, high);
		return a;
	}
}
