package com.jiangcoder.search.segment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class GomeSegment   {
	protected static Logger logger = LoggerFactory.getLogger(GomeSegment.class);
	public GomeSegment() {
		try {
			init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private String path="/server/";
	private void printLog(String str) {
		logger.info(str);
	}

	private NSP mNSP = null;

	private HashSet<String> mPunctuationSet = new HashSet<String>();// 标点符号集合 排除'-'作为型号的一部分，所以需要当成英文字符
	private HashSet<String> mEglishWord = new HashSet<String>();// 主要用来处理英文品牌词汇或者系列词汇 比如： thinkpad
	private HashSet<String> mEglishQiangzhi = new HashSet<String>();// 只强制切分query中的英文此，这个词汇添加的时候要特别小心，不能随便加 只强制一个英文串的开头部分，将错误引入可能性降到最小
	private HashSet<String> mChineseQiangzhi = new HashSet<String>();// 这个只是预留，不建议中文强制切分，而是建议添加训练语料
	private HashSet<String> mHunHeQZ = new HashSet<String>();// 混合强制词汇， 小米3 3g等

	// 概率计算模型
	NGramModle mNGram = new NGramModle();

	// 同义词词典，构建索引时候用
	SynonymWord mSW = new SynonymWord();

	// 初始化： 加载数据 NPath： 搜集多少级别的路径
	public void init(int NPath, int ngram) {
		printLog("初始化开始............");
		long startInit = System.currentTimeMillis();

		path = path.trim();
		char c = path.charAt(path.length() - 1);
		if (c != '/') {
			path += "/";
		}

		// NSP初始化
		mNSP = new NSP(NPath);
		mNSP.init(path);

		// NGram模型数据加载
		mNGram.LoadModleData(path, ngram);

		// 加载需要注意的数据
		LoadData(path);

		// 加载同义典
		mSW.init(path);

		printLog("加载数据完成！");
		long endInit = System.currentTimeMillis();

		long chaInit = endInit - startInit;
		printLog("初始化需要时间： " + chaInit / 1000 + " 秒");
	}

	private void LoadData(String datapath) {
		// 加载英文词汇集合
		// 海尔(Haier) BCD-205STPH 205升L 三门冰箱(银色) 高光抗菌内胆
		loadFromFiletoSet(datapath, "englishword.data", mEglishWord, 5, 20);

		// 加载标点符号，作为自然且分点
		loadFromFiletoSet(datapath, "punctuation.data", mPunctuationSet, 1, 1);

		// 加载需要强制切分的中文和英文
		loadFromFiletoSet(datapath, "chineserqz.data", mChineseQiangzhi, 1, 20);
		loadFromFiletoSet(datapath, "englishqz.data", mEglishQiangzhi, 1, 20);

		// 加载混合强制词汇
		loadFromFiletoSet(datapath, "hunhe.data", mHunHeQZ, 1, 20);
	}

	private void loadFromFiletoSet(String fileDir, String fileName,
			HashSet<String> oneSet, int minlen, int maxlen) {
		try {
			String datafile = fileDir + fileName;
			String encoding = "utf-8";
			File file = new File(datafile);
			if (file.isFile() && file.exists()) {
				// 判断文件是否存在
				InputStreamReader read = new InputStreamReader(
						new FileInputStream(file), encoding);// 考虑到编码格式
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineTxt = null;
				while ((lineTxt = bufferedReader.readLine()) != null) {
					lineTxt = lineTxt.trim();
					if (lineTxt.length() >= minlen
							&& lineTxt.length() <= maxlen) {
						oneSet.add(lineTxt);
					}
				}
				read.close();

			} else {
				printLog("找不到指定的文件");
			}
		} catch (Exception e) {
			printLog("读取文件内容出错");
			e.printStackTrace();
		}
	}

	// 返回值用"\t"隔开
	private String ProbabilitySegment(String chineseStr) {
		String ret = "";
		// 步骤一： 构图
		Vector<VNode> Graph = mNSP.createGraph(chineseStr);

		// 步骤二： 计算每个节点的N个最优前趋
		Vector<NSP.Path> verPath = mNSP.computeNBestPathForEveryVertex(Graph);

		// 步骤三： 回退计算N条最优路径
		Vector<String> nPathVec = mNSP.getNBestPath(verPath, true);

		// 步骤四： 计算最可能的路径
		double maxLogP = Double.MAX_VALUE;
		String mostPStr = "";// 最可能的切分串

		for (String cStr : nPathVec) {
			double pro = computeProbility(cStr);

			if (pro < maxLogP) {
				maxLogP = pro;
				mostPStr = cStr;
			}
		}
		String testStr = ""; // 测试需要这样的串 有顺序
		String[] wordArr = mostPStr.split("\t");
		HashSet<String> wordSet = new HashSet<String>();
		for (int i = 0; i < wordArr.length; i++) {
			wordSet.add(wordArr[i]);
			testStr += wordArr[i] + "\t";
		}

		for (String tStr : wordSet) {
			ret += tStr + "\t";
		}

		ret = ret.trim();

		testStr = testStr.trim();
		return testStr;
	}

	private double computeProbility(String chineseStr) {
		return mNGram.computeSentenceProbility(chineseStr);
	}

	// N 最优路径 综合切分结果 相当与全切分
	private Vector<String> NShortestPathSplit(String chineseStr, boolean bequery) {
		// 步骤一： 构图
		Vector<VNode> Graph = mNSP.createGraph(chineseStr);

		// 步骤二： 计算每个节点的N个最优前趋
		Vector<NSP.Path> verPath = mNSP.computeNBestPathForEveryVertex(Graph);

		// 步骤三： 回退计算N条最优路径
		Vector<String> nPathVec = mNSP.getNBestPath(verPath, bequery);

		return nPathVec;
	}

	// 默认不带"-"号，在调用函数之前自己处理"-"情况，使得函数具有通用性 不允许中间有标点符号
	private String[] pyQuerySplit_Pos(String pyStr) {
		while (pyStr.contains("  ")) {
			pyStr = pyStr.replace("  ", " ");
		}

		String py = pyStr;

		// 步骤一：计算品牌词的位置
		class Pos {
			public int mStart = 0;// 开始位置
			public int mEnd = 0;// 结束位置
		}

		Vector<Pos> brandPos = new Vector<Pos>();
		// 步骤一：品牌词汇提取
		// thinkpade430 品牌词汇至少是两个字符 hp
		for (int i = 0; i < py.length() - 2; i++) {
			for (int j = py.length(); j >= i + 2; j--) {
				String tmpS = py.substring(i, j);
				if (mEglishWord.contains(tmpS)
						|| (i == 0 && mEglishQiangzhi.contains(tmpS))) {
					Pos p = new Pos();
					p.mEnd = j;
					p.mStart = i;

					brandPos.add(p);
					i = j - 1;
					break;
				}
			}
		}

		int pre = 0;
		Vector<Pos> posVec = new Vector<Pos>();
		int lastIndex = 0;
		for (Pos pos : brandPos) {
			int start = pos.mStart;
			int end = pos.mEnd;
			lastIndex = end;

			if (start != pre) {
				{
					Pos tp = new Pos();
					tp.mStart = pre;
					tp.mEnd = start;

					posVec.add(tp);
				}

				{
					Pos tp = new Pos();
					tp.mStart = start;
					tp.mEnd = end;
					posVec.add(tp);
				}

				pre = end;
			} else {
				posVec.add(pos);
				pre = end;
			}
		}

		if (lastIndex != py.length()) {
			Pos tp = new Pos();
			tp.mStart = lastIndex;
			tp.mEnd = py.length();
			posVec.add(tp);
		}

		String retStr = "";
		for (Pos pos : posVec) {
			int begin = pos.mStart;
			int end = pos.mEnd;
			String tmpStr = py.substring(begin, end);
			retStr += tmpStr + " ";
		}

		retStr = retStr.trim();
		return retStr.split(" ");
	}

	// 依赖结果： 中英文区分
	public class hzPySeg {
		public String mHzStr;
		public String mPyStr;
	}

	public class hzPySegPos {
		public Vector<Term> mHzVec = new Vector<Term>();
		public Vector<Term> mPyVec = new Vector<Term>();
		public Vector<Term> mNumVec = new Vector<Term>();
	}

	// 0: 中文和标点符号 1：数字 2： 英文串
	private int charCat(char c) {
		// 必须保证不为空
		if (c >= '0' && c <= '9')
			return 1;

		if (c >= 'a' && c <= 'z')
			return 2;

		return 0;
	}

	// 0: 中文 1：数字 2：英文
	private int stringCat(String str) {
		// 判断字符串类型
		char c = str.charAt(0);
		return charCat(c);
	}

	private hzPySegPos segHzAndPy_Pos(String query) {
		hzPySegPos ret = new hzPySegPos();
		if (query == null || query.length() == 0) {
			return ret;
		}

		query = query.toLowerCase();

		// 步骤一：将标点符号用空格代替（除了数字中间的.号）
		// 苹果hf.l手机l-10 ---> 苹果hf l手机l 10
		String newQuery = "";
		for (int i = 0; i < query.length(); i++) {
			String tmp = query.substring(i, i + 1);

			// 标点符号的处理
			if (mPunctuationSet.contains(tmp)) {
				if (tmp.equals(".")) {
					if (i == 0 || i == query.length() - 1) {
						newQuery += " ";
					} else {
						char pre = query.charAt(i - 1);
						char next = query.charAt(i + 1);

						if (charCat(pre) == 1 && charCat(next) == 1) {
							newQuery += ".";
						} else {
							newQuery += " ";
						}
					}
				} else if (tmp.equals("-")) {
					if (i == 0 || i == query.length() - 1) {
						newQuery += " ";
					} else {
						char pre = query.charAt(i - 1);
						char next = query.charAt(i + 1);

						if ((charCat(pre) == 2 && charCat(next) == 2)
								|| (charCat(pre) == 1 && charCat(next) == 1)) {
							newQuery += "-";
						} else {
							newQuery += " ";
						}
					}
				} else {
					newQuery += " ";
				}
			} else {
				newQuery += tmp;
			}
		}

		while (newQuery.contains("  ")) {
			newQuery = newQuery.replace("  ", " ");
		}
		// 步骤二：中文、数字和英文字符之间添加','号分割
		String[] Arr = newQuery.split(" ");
		int pos = 0;
		for (int i = 0; i < Arr.length; i++) {
			String curStr = Arr[i];
			String tmpStr = "";
			for (int j = 0; j < curStr.length(); j++) {
				if (j == 0 || curStr.charAt(j) == '-'
						|| curStr.charAt(j) == '.') {
					tmpStr += curStr.substring(j, j + 1);
				} else {
					char pre = curStr.charAt(j - 1);
					char now = curStr.charAt(j);
					if (charCat(pre) != charCat(now)
							&& (pre != '.' && pre != '-')) {
						tmpStr += "," + curStr.substring(j, j + 1);
					} else {
						tmpStr += curStr.substring(j, j + 1);
					}
				}
			}

			// 步骤三： 中文，英文和数字分割开
			if (i > 0) {
				pos += 1;// 加1： 是因为自然切分标志 空格 占据一个位置
			}

			String[] curArr = tmpStr.split(",");
			for (int j = 0; j < curArr.length; j++) {
				// 判断是什么字符串
				if (curArr[j].length() == 0)
					continue;

				if (stringCat(curArr[j]) == 0) {
					// 中文串
					Term term = new Term();
					term.mStart = pos;
					term.mLen = curArr[j].length();
					term.mWord = curArr[j];
					pos += term.mLen;

					ret.mHzVec.add(term);
					continue;
				}

				if (stringCat(curArr[j]) == 1) {
					// 数字串
					Term term = new Term();
					term.mStart = pos;
					term.mLen = curArr[j].length();
					term.mWord = curArr[j];
					pos += term.mLen;

					ret.mNumVec.add(term);
					continue;
				}

				// 英文串
				Term term = new Term();
				term.mStart = pos;
				term.mLen = curArr[j].length();
				term.mWord = curArr[j];
				pos += term.mLen;

				ret.mPyVec.add(term);
			}
		}

		return ret;
	}

	// 查看数字和g是否可以合并
	TermVec merge(TermVec gTerm, TermVec numTerm) {
		TermVec retTerm = new TermVec();
		HashSet<Integer> filtPosSet = new HashSet<Integer>();
		HashSet<Integer> newPosSet = new HashSet<Integer>();

		int len = numTerm.mLen;
		for (int pos : numTerm.mStartPos) {
			int newpos = pos + len;
			if (gTerm.mStartPos.contains(newpos))// 找到一个可以合并的
			{
				filtPosSet.add(newpos);
				newPosSet.add(pos);
				gTerm.mStartPos.remove(newpos);
			}
		}

		if (newPosSet.size() == 0)
			return null;

		for (int pos : newPosSet) {
			numTerm.mStartPos.remove(pos);
		}

		retTerm.mBeSource = true;
		retTerm.mLen = numTerm.mLen + 1;
		retTerm.mStartPos = newPosSet;
		retTerm.mType = 4;
		retTerm.mWord = numTerm.mWord + "g";

		return retTerm;
	}

	private void addOneTermVec(HashMap<String, TermVec> retMap, Term term) {

		String key = term.mWord;

		if (retMap.containsKey(key)) {
			retMap.get(key).mStartPos.add(term.mStart);
		} else {
			TermVec tv = new TermVec();
			tv.mBeSource = true;
			tv.mLen = term.mLen;
			tv.mType = term.mType;
			tv.mStartPos = new HashSet<Integer>();
			tv.mStartPos.add(term.mStart);
			tv.mWord = term.mWord;
			tv.mPreWord = term.mPreWord;

			retMap.put(term.mWord, tv);
		}
	}

	// 强制合并 例如：1g 2g 3g 4g 小米3 3 gs s4102
	HashMap<String, TermVec> mergeMandatorySegmentWordPY(
			HashMap<String, TermVec> ResultMap, boolean bequery) {
		HashMap<String, TermVec> retMap = new HashMap<String, TermVec>();
		HashMap<Integer, Term> posToTerm = new HashMap<Integer, Term>();

		for (Map.Entry<String, TermVec> entry : ResultMap.entrySet()) {
			TermVec value = entry.getValue();

			int pos = 0;
			for (Integer num : value.mStartPos) {
				pos = num;
				Term term = new Term();
				term.mLen = value.mLen;
				term.mStart = pos;
				term.mWord = value.mWord;
				term.mType = value.mType;

				// 重复的一定是不需要合并的，先存下来，因为posToTerm 只存放一个最长的
				if (posToTerm.containsKey(pos)) {
					Term tt = posToTerm.get(pos);
					if (tt.mLen < value.mLen) {
						String tkey = tt.mWord;
						if (retMap.containsKey(tkey)) {
							TermVec tv = retMap.get(tkey);
							tv.mStartPos.add(tt.mStart);
						} else {
							addOneTermVec(retMap, tt);
						}

						// 更新posToTerm pos对应的对象
						posToTerm.remove(pos);

						Term tempT = new Term();
						tempT.mLen = value.mLen;
						tempT.mWord = value.mWord;
						tempT.mStart = pos;
						tempT.mType = value.mType;
						posToTerm.put(pos, tempT);
					} else {
						// posToTerm 不变，修改retMap 加入value的信息
						String tkey = value.mWord;
						if (retMap.containsKey(tkey)) {
							TermVec tv = retMap.get(tkey);
							tv.mStartPos.add(pos);
						} else {
							Term newterm = new Term();
							newterm.mLen = value.mLen;
							newterm.mStart = pos;
							newterm.mType = value.mType;
							newterm.mWord = value.mWord;
							addOneTermVec(retMap, newterm);
						}
					}
				} else {
					posToTerm.put(pos, term);
				}
			}
		}

		// 步骤二：数据排序
		HashSet<Integer> hSet = new HashSet<Integer>();// 添加合并的位置信息
		Term[] tArr = new Term[posToTerm.size()];
		int end = -1;
		for (Map.Entry<Integer, Term> entry : posToTerm.entrySet())// 插入排序
		{
			Term term = entry.getValue();

			int pos = end;
			while (pos >= 0) {
				if (tArr[pos].mStart > term.mStart) {
					tArr[pos + 1] = tArr[pos];
					pos--;
				} else {
					tArr[pos + 1] = term;
					break;
				}
			}

			if (pos == -1) {
				tArr[0] = term;
			}

			end++;
		}

		// 步骤三： 合并强制切分结果query切分
		if (bequery) {
			for (int i = 0; i < tArr.length; i++) {
				int pos = tArr[i].mStart;
				Term term = tArr[i];
				if (hSet.contains(pos) || term.mLen >= 4) {
					addOneTermVec(retMap, term);
					continue;
				}

				int nPos = pos + term.mLen;
				if (posToTerm.containsKey(nPos)) // 有相邻词汇
				{
					Term nTerm = posToTerm.get(nPos);
					String nWord = term.mWord + nTerm.mWord;

					if (nTerm.mLen >= 4) {
						addOneTermVec(retMap, term);
						continue;
					}

					if (term.mType == 1 && nTerm.mType == 1) {
						nTerm.mPreWord = term.mWord;
					}

					if (mHunHeQZ.contains(nWord))// 是强制且分的词汇
					{
						if (term.mType == 1 && nTerm.mType == 1) {
							addOneTermVec(retMap, term);
							continue;
						}

						if (term.mType > 1)// 开始词汇不是中文,添加到词汇集合
						{
							Term newTerm = new Term();
							newTerm.mLen = term.mLen + nTerm.mLen;
							newTerm.mStart = term.mStart;
							newTerm.mType = 4;
							newTerm.mWord = nWord;
							hSet.add(nTerm.mStart);

							addOneTermVec(retMap, newTerm);
							i++;// 越过下一个词汇
						} else {
							int nnPos = nPos + nTerm.mLen;
							if (posToTerm.containsKey(nnPos))// 存在可能第二个可以与第三个合并
							{
								Term nnTerm = posToTerm.get(nnPos);
								String nnWord = nTerm.mWord + nnTerm.mWord;
								if (mHunHeQZ.contains(nnWord)) {
									addOneTermVec(retMap, term);
									continue;
								}
							}

							Term newTerm = new Term();
							newTerm.mLen = term.mLen + nTerm.mLen;
							newTerm.mStart = term.mStart;
							newTerm.mType = 4;
							newTerm.mWord = nWord;
							hSet.add(nTerm.mStart);

							addOneTermVec(retMap, newTerm);
							i++;
						}
					} else {
						addOneTermVec(retMap, term);
					}

				} else {
					addOneTermVec(retMap, term);
				}
			}
		} else {
			for (int i = 0; i < tArr.length; i++) {
				int pos = tArr[i].mStart;
				Term term = tArr[i];

				int nPos = pos + term.mLen;
				if (posToTerm.containsKey(nPos)) // 有相邻词汇
				{
					Term nTerm = posToTerm.get(nPos);
					String nWord = term.mWord + nTerm.mWord;

					if (mHunHeQZ.contains(nWord)
							|| (term.mType != 1 && nTerm.mType != 1))// 是强制且分的词汇
					{
						Term newTerm = new Term();
						newTerm.mLen = term.mLen + nTerm.mLen;
						newTerm.mStart = term.mStart;
						newTerm.mType = 4;
						newTerm.mWord = nWord;
						hSet.add(nTerm.mStart);
						addOneTermVec(retMap, newTerm);

						// 原词汇也要入库
						addOneTermVec(retMap, term);
					} else {
						addOneTermVec(retMap, term);
						continue;
					}

				} else {
					addOneTermVec(retMap, term);
				}
			}
		}

		return retMap;
	}

	// 用户query切分
	public HashMap<String, TermVec> QuerySegment_Pos(String query) {
		HashMap<String, TermVec> tvMap = new HashMap<String, TermVec>();

		if (query == null || query.length() == 0)
			return tvMap;

		if (query.contains("　"))// 不是空格，不知道什么符号
		{
			query = query.replaceAll("　", " ");
		}
		query = query.replace("-", " ");
		query = query.toLowerCase();
		query = full2HalfChange(query);// 全角转半角

		// 步骤一： 中文和英文数字分开
		hzPySegPos ret = segHzAndPy_Pos(query);

		// 步骤二： 中文切分{强制切分先不要}
		for (Term term : ret.mHzVec) {
			String hzStr = term.mWord;
			int pos = term.mStart;
			String pSplitStr = ProbabilitySegment(hzStr);// 概率切分结果
			String[] spArr = pSplitStr.split("\t");
			for (int i = 0; i < spArr.length; i++) {
				String word = spArr[i];

				int len = word.length();
				if (tvMap.containsKey(word)) {
					tvMap.get(word).mStartPos.add(pos);
				} else {
					TermVec tv = new TermVec();
					tv.mLen = len;
					tv.mStartPos.add(pos);
					tv.mWord = word;
					tv.mType = 1;
					tvMap.put(word, tv);
				}

				pos += len;
			}
		}

		// 步骤三： 英文切分结果
		for (Term term : ret.mPyVec) {
			String tPY = term.mWord;
			String[] Arr = tPY.split("-");// 一个英文切分
			Vector<Integer> sVec = new Vector<Integer>();
			int start = term.mStart;
			for (int i = 0; i < Arr.length; i++) {
				sVec.add(start);
				start += Arr[i].length();
			}

			for (int i = 0; i < Arr.length; i++) {
				String[] pySplit = pyQuerySplit_Pos(tPY);
				int pos = sVec.elementAt(i);// 开始位置
				for (int j = 0; j < pySplit.length; j++) {
					String word = pySplit[j];
					int len = word.length();
					if (tvMap.containsKey(word)) {
						tvMap.get(word).mStartPos.add(pos);
					} else {
						TermVec tv = new TermVec();
						tv.mLen = word.length();
						tv.mWord = word;
						tv.mStartPos.add(pos);
						tv.mType = 2;
						tvMap.put(word, tv);
					}

					pos += len;
				}
			}
		}

		// 步骤三：数字切分
		for (Term term : ret.mNumVec) {
			String word = term.mWord;
			int pos = term.mStart;
			if (tvMap.containsKey(word)) {
				tvMap.get(word).mStartPos.add(pos);
			} else {
				TermVec tv = new TermVec();
				tv.mLen = word.length();
				tv.mWord = word;
				tv.mStartPos.add(pos);
				tv.mType = 3;
				tvMap.put(word, tv);
			}
		}

		tvMap = mergeMandatorySegmentWordPY(tvMap, true);
		return OneProbility(tvMap);
	}

	int doubleProToInt(Double pro) {
		if (pro < 5.2575125522948823E-5) {
			return 1;
		} else if (pro < 1.8694198798918275E-4) {
			return 2;
		} else if (pro < 7.557286475398297E-4) {
			return 3;
		} else if (pro < 0.0010764057455503059) {
			return 4;
		} else if (pro < 0.0018336877276679657) {
			return 5;
		} else if (pro < 0.003305376861973891) {
			return 6;
		} else if (pro < 0.004314700401471773) {
			return 7;
		} else if (pro < 0.007476722693942755) {
			return 8;
		} else if (pro < 0.017869134964659857) {
			return 9;
		}

		return 10;
	}

	// 为词汇标注一元概率
	HashMap<String, TermVec> OneProbility(HashMap<String, TermVec> ResultMap) {
		for (Map.Entry<String, TermVec> entry : ResultMap.entrySet()) {
			TermVec term = entry.getValue();
			String word = term.mWord;
			String pre = term.mPreWord;

			if (term.mType == 1) {
				if (pre.isEmpty()) {
					byte[] p = mNGram.mStaticWordToPro.getVal(word);
					if (p != null) {
						term.mPro = new PTermWord(p).mPro;
					} else {
						term.mPro = 0.0000001;
					}
				} else {
					term.mPro = mNGram.twoWordProbility(pre, word);
				}
			} else if (term.mType == 4) {
				term.mPro = 0.01;
			} else {
				term.mPro = 0.0001;
			}

			term.mIntegerPro = doubleProToInt(term.mPro);
		}
		return ResultMap;
	}

	public HashMap<String, TermVec> ProductTitleSegment_Pos(String title) {
		HashMap<String, TermVec> ResultMap = new HashMap<String, TermVec>();
		HashMap<String, TermVec> SynWord = new HashMap<String, TermVec>();// 需要添加到索引中的同义词

		if (title == null || title.length() == 0)
			return ResultMap;

		title = title.toLowerCase();
		title = full2HalfChange(title);// 全角转半角

		// 步骤一： 中文和英文数字分开
		hzPySegPos ret = segHzAndPy_Pos(title);

		// 步骤二： 中文切分{强制切分先不要}
		Vector<Term> hzVec = ret.mHzVec;
		for (Term term : hzVec) {
			String tmpStr = term.mWord;
			Vector<String> pathVec = NShortestPathSplit(tmpStr, false);

			for (String pSplitStr : pathVec) {
				String[] spArr = pSplitStr.split("\t");
				int pos = term.mStart;
				for (int j = 0; j < spArr.length; j++) {
					if (ResultMap.containsKey(spArr[j])) {
						ResultMap.get(spArr[j]).mStartPos.add(pos);
					} else {
						TermVec tv = new TermVec();
						tv.mWord = spArr[j];
						tv.mLen = spArr[j].length();
						tv.mStartPos.add(pos);
						tv.mType = 1;
						ResultMap.put(spArr[j], tv);
					}

					pos += spArr[j].length();
				}
			}
		}

		// 步骤三： 英文切分结果
		Vector<Term> pyVec = ret.mPyVec;

		// 英文自然切分
		for (Term term : pyVec) {
			// 强制切分 品牌词汇
			String tPY = term.mWord;
			int pos = term.mStart;
			String[] Arr = tPY.split("-");
			for (int i = 0; i < Arr.length; i++) {
				if (i > 0) {
					pos++;
				}

				if (ResultMap.containsKey(Arr[i])) {
					ResultMap.get(Arr[i]).mStartPos.add(pos);
				} else {
					TermVec tv = new TermVec();
					tv.mWord = Arr[i];
					tv.mLen = Arr[i].length();
					tv.mStartPos.add(pos);
					tv.mType = 2;
					ResultMap.put(Arr[i], tv);
				}

				pos += Arr[i].length();
			}
		}

		// 步骤四：数字切分结果
		Vector<Term> nVec = ret.mNumVec;
		for (Term term : nVec) {
			String word = term.mWord;
			String tmpStr = word;

			int len = word.length();
			int pos = term.mStart;
			if (word.contains("-")) {

			} else {
				if (ResultMap.containsKey(word)) {
					ResultMap.get(word).mStartPos.add(pos);
				} else {
					TermVec tv = new TermVec();
					tv.mLen = len;
					tv.mStartPos.add(pos);
					tv.mWord = word;
					tv.mType = 3;
					ResultMap.put(word, tv);
				}
			}

			word = tmpStr;
			if (word.contains("-")) {
				word = word.replace("-", "#");
				String[] numArr = word.split("#");
				for (int i = 0; i < numArr.length; i++) {
					if (i > 0) {
						pos++;
					}

					String nnum = numArr[i];
					if (ResultMap.containsKey(nnum)) {
						ResultMap.get(nnum).mStartPos.add(pos);
						pos += nnum.length();
					} else {
						TermVec tv = new TermVec();
						tv.mWord = nnum;
						tv.mLen = nnum.length();
						tv.mStartPos.add(pos);
						tv.mType = 3;
						ResultMap.put(nnum, tv);
						pos += tv.mLen;
					}
				}
			}
		}

		// 步骤五：强制合并
		ResultMap = mergeMandatorySegmentWordPY(ResultMap, false);

		// 步骤六：同义词处理
		for (Map.Entry<String, TermVec> entry : ResultMap.entrySet()) {
			String key = entry.getKey();
			TermVec val = entry.getValue();

			// 获取词汇的同义词
			String synword = mSW.getSynonymWord(key);
			if (synword == null)
				continue;

			// 判断是否同义词已经存在
			if (ResultMap.containsKey(synword))
				continue;

			// 添加同义词
			int type = 1;
			if (synword.length() > 1) {
				String[] Arr = synword.split(" ");

				for (int i = 0; i < Arr.length; i++) {
					if (Arr[i].isEmpty())
						continue;

					char c = Arr[i].charAt(0);
					if (val.mType == 4) {
						type = 4;
					} else if (c >= '0' && c <= '9') {
						type = 3;
					} else if (c >= 'a' && c <= 'z') {
						type = 2;
					}

					TermVec term = new TermVec();
					term.mLen = val.mLen;// 原词汇的长度，将来用来做排序的时候用的着
					term.mStartPos = val.mStartPos;
					term.mType = type;
					term.mWord = Arr[i];

					SynWord.put(Arr[i], term);
				}
			}
		}

		for (Map.Entry<String, TermVec> entry : SynWord.entrySet()) {
			String key = entry.getKey();
			TermVec val = entry.getValue();
			ResultMap.put(key, val);
		}

		// 步骤七：一元概率
		return OneProbility(ResultMap);
	}

	// 对输入进行全角转半角转换
	public String full2HalfChange(String QJstr) {
		if (QJstr == null || QJstr.length() == 0)
			return "";

		StringBuffer outStrBuf = new StringBuffer("");
		String Tstr = "";

		byte[] b = null;
		for (int i = 0; i < QJstr.length(); i++) {
			Tstr = QJstr.substring(i, i + 1);
			if (Tstr.equals(" ")) {
				outStrBuf.append(" ");
				continue;
			}

			try {
				b = Tstr.getBytes("unicode");

				if (b[2] == -1) {
					// 表示全角
					b[3] = (byte) (b[3] + 32);
					b[2] = 0;
					outStrBuf.append(new String(b, "unicode"));
				} else {
					outStrBuf.append(Tstr);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		return outStrBuf.toString();
	}

	// 排序专用结构
	class STerm {
		String mWord = "";
		Double mPro = 0.0;
		int mPos = 0;
		int mIntegerPro = 0;
		int mType = 1;
	}

	// 简单插入排序
	void InsertSort(STerm[] sArr, STerm st, int len) {
		if (len == 0) {
			sArr[0] = st;
			return;
		}

		int pos = len - 1;
		for (; pos >= 0; pos--) {
			if (sArr[pos].mPos > st.mPos
					|| (sArr[pos].mPos == st.mPos && sArr[pos].mWord.length() < st.mWord
							.length())) {
				sArr[pos + 1] = sArr[pos];
			} else {
				break;
			}
		}

		sArr[pos + 1] = st;
	}

	String getStr(HashMap<String, TermVec> tQuery) {
		String str = "";

		int size = 0;
		for (Map.Entry<String, TermVec> entry : tQuery.entrySet()) {
			TermVec val = entry.getValue();
			size += val.mStartPos.size();
		}

		STerm[] sArr = new STerm[size];
		int len = 0;

		for (Map.Entry<String, TermVec> entry : tQuery.entrySet()) {
			TermVec term = entry.getValue();

			Iterator<Integer> i = term.mStartPos.iterator();// 先迭代出来
			while (i.hasNext()) {// 遍历
				int key = i.next();
				STerm st = new STerm();
				st.mPos = key;
				st.mWord = term.mWord;
				st.mPro = term.mPro;
				st.mType = term.mType;
				st.mIntegerPro = term.mIntegerPro;

				InsertSort(sArr, st, len);
				len++;
			}
		}

		for (int i = 0; i < sArr.length; i++) {
			String tstr = "\n";
			if ((i + 1) % 3 == 0 && i != 0) {
				tstr = "\n";
			}

			str += sArr[i].mWord + "/" + sArr[i].mPos + "/" + sArr[i].mType
					+ "/" + sArr[i].mPro + "/" + sArr[i].mIntegerPro + "\t"
					+ tstr;
		}

		str = str.trim();

		return str;
	}

	// 混合：4
	// 其他：1 长度小于等于4 2 长度大于等于5
	private int wordtype(String str) {
		boolean en = false;// 含有英文
		boolean nu = false;// 是否还有数字
		boolean zh = false;// 是否还有中文
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c >= '0' && c <= '9')
				nu = true;
			else if (c >= 'a' && c <= 'z')
				en = true;
			else
				zh = true;
		}

		if (nu && en || nu && zh || en && zh)// 混合模型
		{
			return 4;
		}

		if (en) {
			if (str.length() <= 4) {
				return 1;
			}

			return 2;
		}

		return 3;// 数字
	}

	// 输入结构：
	// {"syn":["media,美的"，"微软,microsoft"],"hh":["thinkpad","ipad","小米3"],"dd":["htc","thinkpad","小米3"]}
	public boolean incWord(BasicDBObject words) {

		if (words.containsField("syn")) {
			BasicDBList sys = (BasicDBList) words.get("syn");
			try {
				String datafile = path+"synonym.data";
				String encoding = "utf-8";
				FileOutputStream f = new FileOutputStream(datafile, true);

				for (int i = 0; i < sys.size(); i++) {
					String lineTxt = (String) sys.get(i);
					lineTxt = lineTxt.trim();
					if (lineTxt.isEmpty())
						continue;

					lineTxt = lineTxt.toLowerCase();
					String[] arr = lineTxt.split(",");
					for (int j = 0; j < arr.length; j++) {
						arr[j] = arr[j].trim();
					}

					// 系统内存添加同义词词典
					if (arr.length == 2) {
						String left = arr[0];
						String right = arr[1];

						if (left.length() == right.length())// 免去简拼 同义词本身有问题
							continue;

						mSW.mSynonym.put(left, right);
						mSW.mSynonym.put(right, left);
					} else if (arr.length > 2) {
						for (int k = 0; k < arr.length; k++) {
							String left = arr[k];
							String right = "";
							for (int j = 0; j < arr.length; j++) {
								if (j != k) {
									right += arr[j] + " ";
								}
							}

							right = right.trim();
							mSW.mSynonym.put(left, right);
						}
					}
					// 文件中添加系统词典
					String line = "";
					for (int j = 0; j < arr.length; j++) {
						line += arr[j] + "\t";
					}

					line = line.trim();
					f.write(line.getBytes(encoding));
					f.write("\n".getBytes(encoding));
				}

				f.close();
			} catch (Exception e) {
				printLog("写文件内容出错");
				e.printStackTrace();
			}
		}

		if (words.containsField("hh")) {
			BasicDBList hh = (BasicDBList) words.get("hh");
			try {
				String hhfile = path+"hunhe.data";// 混合词汇
				String sefile = path+"englishqz.data";// 短英文
				String lefile = path+"englishword.data";// 长英文
				String encoding = "utf-8";
				FileOutputStream hhf = new FileOutputStream(hhfile, true);
				FileOutputStream sef = new FileOutputStream(sefile, true);
				FileOutputStream lef = new FileOutputStream(lefile, true);

				for (int i = 0; i < hh.size(); i++) {
					String lineTxt = (String) hh.get(i);
					lineTxt = lineTxt.trim();
					if (lineTxt.isEmpty())
						continue;

					lineTxt = lineTxt.toLowerCase();

					int type = wordtype(lineTxt);
					if (type == 1)// 短英文
					{
						sef.write(lineTxt.getBytes(encoding));
						sef.write("\n".getBytes(encoding));
						mEglishQiangzhi.add(lineTxt);

					} else if (type == 2)// 长英文
					{
						lef.write(lineTxt.getBytes(encoding));
						lef.write("\n".getBytes(encoding));
						mEglishWord.add(lineTxt);
					} else if (type == 4)// 混合
					{
						hhf.write(lineTxt.getBytes(encoding));
						hhf.write("\n".getBytes(encoding));
						mHunHeQZ.add(lineTxt);
					} else {
						String str = "不要填写数字或空串";
						printLog(str);
					}
				}

				sef.close();
				lef.close();
				hhf.close();
			} catch (Exception e) {
				printLog("写文件内容出错");
				e.printStackTrace();
			}
		}

		if (words.containsField("dd")) {
			BasicDBList dd = (BasicDBList) words.get("dd");
			try {
				String hhfile = path+"hunhe.data";// 混合词汇
				String sefile = path+"englishqz.data";// 短英文
				String lefile = path+"englishword.data";// 长英文
				FileOutputStream hhf = new FileOutputStream(hhfile);
				FileOutputStream sef = new FileOutputStream(sefile);
				FileOutputStream lef = new FileOutputStream(lefile);

				for (int i = 0; i < dd.size(); i++) {
					String lineTxt = (String) dd.get(i);
					lineTxt = lineTxt.trim();
					if (lineTxt.isEmpty())
						continue;

					lineTxt = lineTxt.toLowerCase();

					int type = wordtype(lineTxt);
					if (type == 1 && mEglishQiangzhi.contains(lineTxt))// 短英文
					{
						mEglishQiangzhi.remove(lineTxt);
					} else if (type == 2 && mEglishWord.contains(lineTxt))// 长英文
					{
						mEglishWord.remove(lineTxt);
					} else if (type == 4 && mHunHeQZ.contains(lineTxt))// 混合
					{
						mHunHeQZ.remove(lineTxt);
					} else {
						String str = "不要填写数字或空串";
						printLog(str);
					}
				}

				// 反写到文件中
				String encoding = "utf-8";
				for (String str : mEglishWord) {
					lef.write(str.getBytes(encoding));
					lef.write("\n".getBytes(encoding));
				}

				for (String str : mEglishQiangzhi) {
					sef.write(str.getBytes(encoding));
					sef.write("\n".getBytes(encoding));
				}

				for (String str : mHunHeQZ) {
					hhf.write(str.getBytes(encoding));
					hhf.write("\n".getBytes(encoding));
				}

				sef.close();
				lef.close();
				hhf.close();
			} catch (Exception e) {
				printLog("写文件内容出错");
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public HashMap<String, Integer> getSegResult(String line)
	{
		HashMap<String,TermVec> tQuery = ProductTitleSegment_Pos(line);
		HashMap<String, Integer> segResult = new HashMap<String, Integer>();
		
		int size = 0;
		for(Map.Entry<String, TermVec> entry : tQuery.entrySet())
		{
			TermVec val = entry.getValue();
			size += val.mStartPos.size();
		}
		
		STerm[] sArr = new STerm[size];
		int len = 0;
		
		for(Map.Entry<String, TermVec> entry: tQuery.entrySet())
		{
			TermVec term=entry.getValue();
			
			Iterator<Integer> i = term.mStartPos.iterator();//先迭代出来  
	        while(i.hasNext()){//遍历  
	        	int key = i.next();
	        	STerm st = new STerm();
	        	st.mPos = key;
	        	st.mWord = term.mWord;
	        	st.mPro = term.mPro;
	        	st.mType = term.mType;
	        	st.mIntegerPro = term.mIntegerPro;
	        	
	        	InsertSort(sArr, st, len);
	        	len++;
	        }  
		}
		
		for(int i=0; i<sArr.length; i++)
		{
			if(segResult.containsKey(sArr[i].mWord)){
				int cnt = segResult.get(sArr[i].mWord)+1;
				segResult.put(sArr[i].mWord, cnt);
				continue;
			}
			segResult.put(sArr[i].mWord, 1);
		}
		
		return segResult;
	}
	
	public List<String> getQuerySegResult(String line)
	{
		HashMap<String,TermVec> tQuery = QuerySegment_Pos(line);
		List<String> segResult = new ArrayList<String>();
		
		int size = 0;
		for(Map.Entry<String, TermVec> entry : tQuery.entrySet())
		{
			TermVec val = entry.getValue();
			size += val.mStartPos.size();
		}
		
		STerm[] sArr = new STerm[size];
		int len = 0;
		
		for(Map.Entry<String, TermVec> entry: tQuery.entrySet())
		{
			TermVec term=entry.getValue();
			
			Iterator<Integer> i = term.mStartPos.iterator();//先迭代出来  
	        while(i.hasNext()){//遍历  
	        	int key = i.next();
	        	STerm st = new STerm();
	        	st.mPos = key;
	        	st.mWord = term.mWord;
	        	st.mPro = term.mPro;
	        	st.mType = term.mType;
	        	st.mIntegerPro = term.mIntegerPro;
	        	
	        	InsertSort(sArr, st, len);
	        	len++;
	        }  
		}
		
		for(int i=0; i<sArr.length; i++)
		{
			segResult.add(sArr[i].mWord);
		}
		
		return segResult;
	}


	public void init() throws Exception {
		path ="/server/yysegment";
		init(3, 2);
	}

}
