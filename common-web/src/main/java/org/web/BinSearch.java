package org.web;

/**
 * Hello world!
 *
 */
public class BinSearch 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        int a[]=new int[]{1,2,6,8,9};
        System.out.println(getMiddle(a,0,4,9));
    }
    public static int  getMiddle(int a[],int start,int end,int value){
    	if(start>end)return -1;
    	int mid=(start+end)/2;
    		if(value<a[mid]){
    			return getMiddle(a,start,mid-1, value);
    		}
    		else if(value>a[mid]){
    			return getMiddle(a,mid+1,end, value);
    		}
    		else {
				return mid;
			}
    }
}
