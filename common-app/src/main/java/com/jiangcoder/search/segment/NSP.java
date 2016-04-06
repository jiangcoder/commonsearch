package com.jiangcoder.search.segment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;



public class NSP {
	
	private int mN = 0;//最优n路径
	private HashSet<String> mDic = new HashSet<String>();
	private int mMaxWordLen = 6;//最大中文字长
	
	
	//排除无效路径
	//不允许连续两个单字组合成词
	private HashSet<String> mWordStart = new HashSet<String>();
	private boolean isRightPath(String sentence)
	{
		if(sentence == null || sentence.length() == 0)
			return false;
		
		String[] arr = sentence.split("\t");
		for(int i=0; i<arr.length; i++)
		{
			if(arr[i].length() ==1 && mWordStart.contains(arr[i]))
			{
				String word = arr[i];
				for(int j=i+1; j<arr.length && j<i+6; j++)
				{
					if(arr[j].length()>1)
					{
						break;
					}
					
					word += arr[j];
					if(mDic.contains(word))
					{
						return false;
					}
				}
			}
		}
		
		
		return true;
	}
	
	
	
	NSP(int N)
	{
		mN = N;
	}
	
	public class Path
	{
		int mNPath = 0;
		public HashMap<Integer,Parent> mParent = new HashMap<Integer,Parent>(); //权重——>parent
		
		Path(int N)
		{
			mNPath = N;			
		}
		
		public void SortAndTrimToNPath()
		{
			if(mParent.size() > mN)
			{
				//遍历mParent,只保留mN条路径的链表
				mParent = insertSort();
				
			}
		}
		
		
		private HashMap<Integer,Parent> insertSort()
		{
			Vector<Integer> tmpVec = new Vector<Integer>();
			
			for(Map.Entry<Integer, Parent> entry: mParent.entrySet())
			{
				int key = entry.getKey();
				
				//找到插入位置
				int i=tmpVec.size() - 1;
				for(; i>=0; i--)
				{
					if(tmpVec.elementAt(i) < key)
					{
						//key应该插入到i+1位置index
						break;
					}
				}
				
				tmpVec.insertElementAt(key, i+1);
				if(tmpVec.size() > mN)
				{
					tmpVec.remove(tmpVec.size() - 1);//如果超过mN，删除最后一个
				}
			}
			
			HashMap<Integer, Parent> tmpMap = new HashMap<Integer, Parent>();
			for(int i=0; i<tmpVec.size();i++)
			{
				int key = tmpVec.elementAt(i);
				Parent value = mParent.get(key);
				
				tmpMap.put(key, value);
			}
			
			return tmpMap;
		}
	}
	
	class Parent
	{
		int mParentPoint = -1;//节点
		int mPathWeight = 100;//到该节点的路径权重
		Parent mNext = null;//同一个权重来源
		String mWordPath = ""; //中间用"\t"隔开
	}

	
	class Term
	{
		public int mPos = -1;//句子中的位置
		public int mLen = -1; //词汇的长度
		public String mWord = "";//词汇
	}
	
	//加载词典 &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	public void init(String path)
	{
		//加载N最优路径词汇 npathword.data
		try 
		{
			String datafile = path + "npathword.data";
			String encoding="utf-8";
			File file=new File(datafile);
			if(file.isFile() && file.exists())
			{ 
				//判断文件是否存在
				InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);//考虑到编码格式
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineTxt = null;
				bufferedReader.readLine();
				while((lineTxt = bufferedReader.readLine()) != null){
					lineTxt = lineTxt.trim();
					if(lineTxt.length() > 1)
					{
						mDic.add(lineTxt);
					}
				}
				read.close();
				
			}else{
				System.out.println("找不到指定的文件");
			}
		} catch (Exception e) 
		{
		    System.out.println("读取文件内容出错");
		    e.printStackTrace();
		}
		
		
		for(String word: mDic)
		{
			String start = word.substring(0,1);
			mWordStart.add(start);
		}
		
	}
	
	Vector<Term> splitAllWords(String sentence)
	{
		Vector<Term> words = new Vector<Term>();
		for(int i=0; i<sentence.length(); i++)
		{
			int j=sentence.length() - i;
			if(j > mMaxWordLen)
			{
				j = mMaxWordLen;
			}
			
			for( ; j >= 2; j--)
			{
				String subStr = sentence.substring(i, i+j);
				if(mDic.contains(subStr))
				{
					Term  aterm = new Term();
					aterm.mPos = i;
					aterm.mLen = subStr.length();
					aterm.mWord = subStr;
					
					words.add(aterm);
				}
			}
		}
		
		return words;		
	}
		
	//########################################测试程序区域，程序测试成功可以删除#################################
	void testSplit(Vector<Term> words )
	{
		System.out.println("分词结果："); 
		for(Term term : words)
		{
			System.out.print(term.mWord + "\t");
		}
		
		System.out.println("");
	}
	
	void testGraph(String sentence, Vector<VNode> graph )
	{
		System.out.println("原串： " + sentence);
		int n=0;
		for(VNode vnode : graph)
		{
			System.out.println("节点" + n + " 的边有:");
			String word = "";
			ENode enode = vnode.mENode;
			while(enode != null)
			{
				word += enode.mWord;
				word += " :(" + vnode.mPointNum + "," + enode.mEndPoint + ")\t"; 
				enode = enode.mNext;
			}
			
			System.out.println(word);
			n++;
		}
	}
	
	
	void testPrintPath(Path path, int verNum)
	{
		
		System.out.println("节点号：" + verNum);
		HashMap<Integer,Parent> par = path.mParent;
		
		for(Map.Entry<Integer,Parent> entry : par.entrySet()){ 		
			System.out.println("路径长度： " + entry.getKey());
			System.out.println("路径：");
			int key = entry.getKey();
			Parent pp = entry.getValue();
			while(pp != null)
			{
				System.out.println(key + "\t:\t" + pp.mWordPath);
				pp = pp.mNext;
			}
		}
	}
	
	//########################################测试程序区域，程序测试成功可以删除#################################
	
	
	//创建图
	public Vector<VNode>  createGraph(String sentence)
	{
		sentence = sentence.replaceAll("\t", " ");
		
		//步骤一： 构建图
		//初始化图结构
		Vector<VNode> graph = new Vector<VNode>();
		for(int i=0; i<sentence.length(); i++)
		{
			VNode aV = new VNode();
			
			//第一条出边
			ENode e = new ENode();
			e.mNext = null;
			e.mEndPoint = i+1;
			e.mWeight = 1;
			e.mWord = sentence.substring(i, i+1);
			aV.mENode = e;
			
			aV.mPointNum = i;
			
			graph.add(aV);
		}
		
		{
			//末节点
			VNode aV = new VNode();
			aV.mENode = null;
			aV.mPointNum = sentence.length();
			graph.add(aV);
		}
		
		
		
		//添加长距离边（跨两个字符或以上的边）
		Vector<Term> words = splitAllWords(sentence);
		
//		testSplit(words);
		
		for(Term term: words)
		{
			//有一条节点pos到节点pos+len的边
			int len = term.mLen;
			int pos = term.mPos;
			VNode vnode = graph.elementAt(pos);
			
			ENode enode = new ENode();//建一条边
			enode.mNext = null;
			enode.mEndPoint = pos + len;
			enode.mWord = term.mWord;
			
			ENode pNode = vnode.mENode;
			while(pNode.mNext != null)
			{
				pNode = pNode.mNext;
			}
			
			pNode.mNext = enode;			
		}
		
		
		return graph;
//		testGraph(sentence);
	}
		
	
	
	public Vector<Path> computeNBestPathForEveryVertex(Vector<VNode> graph)
	{
		//初始化mPath
		Vector<Path> path = new Vector<Path>();
		for(int i=0; i<graph.size(); i++)
		{
//			Path path = new Path(mN);
			path.add(new Path(mN));
		}
		
		for( VNode vnode: graph)
		{
			ENode enode = vnode.mENode;
			while(enode != null)
			{
				int endVer = enode.mEndPoint;
				int weight = enode.mWeight;
				String word = enode.mWord;
				
				//节点endVer的父路径收集
//				System.out.println("Error position: " + endVer);
				Path childPath = path.elementAt(endVer);
				HashMap<Integer,Parent> parentH = childPath.mParent;
				
				if(vnode.mPointNum == 0)
				{
					Parent pp = new Parent();
					pp.mParentPoint = 0;
					pp.mNext = null;
					pp.mPathWeight = 0;
					pp.mWordPath = word;//到该节点的路径
					
					int curWeight = pp.mPathWeight + weight;//当前路径到此节点的权重
					if(parentH.containsKey(curWeight))
					{
						Parent curP = parentH.get(curWeight);
						pp.mNext = curP;
						curP = pp;
						
						parentH.remove(curWeight);
						parentH.put(curWeight, curP);
						
					}else
					{
						parentH.put(curWeight, pp);
					}
				}else
				{
					Path curPath = path.elementAt(vnode.mPointNum);//当前点的路径
					
//					testPrintPath(curPath, vnode.mPointNum);
					curPath.SortAndTrimToNPath();//insertSort();//保留N最优路径
//					testPrintPath(curPath, vnode.mPointNum);
					
					HashMap<Integer,Parent> curParentH = curPath.mParent;
					for(Map.Entry<Integer,Parent> entry : curParentH.entrySet()){ 						 
						int preWeight = entry.getKey();
						int curWeight = preWeight + weight;//当前路径到此节点的权重  前面节点的权重+边的权重
						
						Parent prePar = entry.getValue();
						while(prePar != null)
						{
							String curWordPath = prePar.mWordPath;
							 
							Parent pp = new Parent();
							pp.mParentPoint = 0;
							pp.mNext = null;
							pp.mPathWeight = preWeight;
							pp.mWordPath = curWordPath + "\t" + word;
							
							if(parentH.containsKey(curWeight))
							{
								Parent curP = parentH.get(curWeight);
								pp.mNext = curP;
								curP = pp;
								
								parentH.remove(curWeight);
								parentH.put(curWeight, curP);
								
							}else
							{
								parentH.put(curWeight, pp);
							}
							
							prePar = prePar.mNext;
						}
					}
				}
				
				enode = enode.mNext;
			}
		}
		
		
		return path;
	}
	
	public Vector<String > getNBestPath(Vector<Path> path, boolean bequery)
	{
		Vector<String> nPath = new Vector<String>();
		int end = path.size() - 1;
		Path endPath = path.elementAt(end);
		endPath.SortAndTrimToNPath();
		HashMap<Integer,Parent> parent = endPath.mParent;
		for(Map.Entry<Integer,Parent> entry : parent.entrySet()){ 
			Parent pp = entry.getValue();
			while(pp!=null)
			{
				if(bequery)
				{
					//query切分去掉 无效路径
					if(isRightPath(pp.mWordPath))
						nPath.add(pp.mWordPath);
				}else
				{
					//title且分  不需要判断
					nPath.add(pp.mWordPath);
				}
				pp = pp.mNext;
			}
		}
		
		return nPath;
	}
	
	public static void main(String[] cmdline)
	{
		NSP nsp = new NSP(10);
		nsp.init("");
		
		int num = 100;
		for(int i=0; i<num; i++)
		{
			//步骤一： 构建查出所有的词汇，computeNBestPathForEveryVectex并构建图
//			String sentence = "他说的确实在理";
			String sentence = "建设中国特色社会主义法制体系";
			Vector<VNode> Graph = nsp.createGraph(sentence);
			
			
			//步骤二： 计算每个节点的N个最优前趋
			Vector<Path> verPath = nsp.computeNBestPathForEveryVertex(Graph);
			
			
			//步骤三： 回退计算N条最有路径
			nsp.getNBestPath(verPath, true);
			
		}
		//“他说的确实在理” ： 十万次：2476ms   0.025微秒
		//"建设中国特色社会主义法制体系，必须坚持立法先行，发挥立法的引领和推动作用"： 100次  7342毫秒    73毫秒
	}
}
