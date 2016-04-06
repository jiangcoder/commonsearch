package com.jiangcoder.search.segment;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.lang.String;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataTypeTransform
{	
	public byte[] toByteArray()
	{
		return null;
	}
	
	public int size()
	{
		return 0;
	}
	
	public  byte[] intToByte(int a)
	{  
	    return new byte[] {  
	        (byte) ((a >> 24) & 0xFF),  
	        (byte) ((a >> 16) & 0xFF),     
	        (byte) ((a >> 8) & 0xFF),     
	        (byte) (a & 0xFF)  
    	};  
    
	}  
	     
	public  byte[] longToByte(long a) 
	{ 
		 return new byte[] {  
				 	(byte) ((a >> 56) & 0xFF),  
			        (byte) ((a >> 48) & 0xFF),     
			        (byte) ((a >> 40) & 0xFF),     
			        (byte) ((a >> 32) & 0xFF), 
			        (byte) ((a >> 24) & 0xFF),  
			        (byte) ((a >> 16) & 0xFF),     
			        (byte) ((a >> 8) & 0xFF),     
			        (byte) (a & 0xFF)  
		    	};  
	} 
	
	public  int byteToInt(byte[] b, int pos) 
	{  
	    return  ((b[pos+3] & 0xFF)   |  
	            ((b[pos+2]<<8) & 0xFF00)  |  
	            ((b[pos+1]<<16) & 0xFF0000) |  
	            ((b[pos]<<24) & 0xFF000000));  
	}  
	
	//byte数组转成long 
	public long byteToLong(byte[] b, int pos) 
	{ 
	    return  ((long)b[pos+7] & 0xFFL) | 
			(((long)b[pos+6]<<8) & 0xFF00L)  | 
			(((long)b[pos+5]<<16) & 0xFF0000L) | 
			(((long)b[pos+4]<<24) & 0xFF000000L) |
			(((long)b[pos+3]<<32) & 0xFF00000000L) | 
			(((long)b[pos+2]<<40) & 0xFF0000000000L) |
			(((long)b[pos+1]<<48) & 0xFF000000000000L) |
			(((long)b[pos+0]<<56) & 0xFF00000000000000L); 
	}
}

public class StaticHash<Vty extends DataTypeTransform> 
{
	protected static Logger logger = LoggerFactory.getLogger(StaticHash.class);
	private void printLog(String str)
	{
		logger.info(str);
	}
	
	private int mBucketNum = 0;
	private int mElementCnt = 0;
	private int mValSize = 0;
	private int mTotalSize = 0;
	
	public DataTypeTransform mTransform = new DataTypeTransform();
	
	private int[] mVecIndex;
	private byte[] mVecInvert;
	
	class MyCompare implements Comparator<Long>
	{
		public int compare(Long l1, Long l2) 
		{			
			if(l1<=l2)
			{
				return -1;
			}
			else
			{
				return 1;
			}
		}
	}
	
	public static final Comparator<Long> CMPVALUE = new Comparator<Long>() 
	{
		public int compare(Long l, Long r)
		{
			return (int)(l-r);
		}
	};
		
	public byte[] getVal(String k)
	{
		byte[] p = null;
		long hasher = hashString(k);
		
		int id = num_to_bucket_id(hasher, mBucketNum);
		if(id>mVecIndex.length)
			return p;
		int invertPos = mVecIndex[id];
		if(invertPos!=-1 && invertPos<mTotalSize)
		{
			//long tmpHasher = mTransform.byteToLong(mVecInvert, invertPos);
			/*
			if(tmpHasher==hasher)
			{
				p = new byte[mValSize];
				System.arraycopy(mVecInvert, invertPos+8, p, 0, mValSize);
				
				return p;
			}
			invertPos += mValSize+8;
			*/
			for(; ;)
			{
				if(invertPos>=mTotalSize)
				{
					break;
				}
				long tmpHasher = mTransform.byteToLong(mVecInvert, invertPos);
				if(num_to_bucket_id(tmpHasher, mBucketNum) != id || tmpHasher>hasher)
				{
					break;
				}
				if(tmpHasher==hasher)
				{
					p = new byte[mValSize];
					System.arraycopy(mVecInvert, invertPos+8, p, 0, mValSize);
					return p;
				}
				invertPos += mValSize+8;
			}		
		}
		return p;
	}
	
	public int getBucketNum(int hashSize)
	{
		int i=0;
		while(hashSize > 1)
		{
			hashSize = hashSize >> 1;
			++i;
		}
		return i;
	}
	
	public boolean loadHashFileToMemory(String fileName)
	{	
		try
		{			
			File file = new File(fileName);  
	        int fileSize = (int)file.length();  
	        mTotalSize = fileSize;
	        FileInputStream fIn = new FileInputStream(file);  
	        mVecInvert = new byte[fileSize];
	        if(fIn.read(mVecInvert, 0, mVecInvert.length)!=fileSize)
	        {  
	        	fIn.close();  
	        	return false;
	        }  
	        mBucketNum = mTransform.byteToInt(mVecInvert, 0);
	        mElementCnt = mTransform.byteToInt(mVecInvert, 4);
	        mValSize = mTransform.byteToInt(mVecInvert, 8);
	        
	        mVecIndex = new int[mBucketNum];
	        int curPos = 12;
	        for(int i=0; i<mBucketNum; ++i)
	        {
	        	mVecIndex[i] = mTransform.byteToInt(mVecInvert, curPos);
	        	curPos += 4;
	        }
            
            fIn.close();  
		}
		catch (IOException e) 
		{
			e.printStackTrace();
			return false;
		} 
		
		return true;
	}
	
	public boolean writeToHashFile(HashMap<String, Vty> hashObj, int bucket_pow, String fileName)
	{
		if (bucket_pow < 10)
		{
			bucket_pow = 10;
		}
		else if (bucket_pow > 25) //32000000+
		{
			bucket_pow = 25;
		}

		mBucketNum = (1<<bucket_pow);
		//System.out.println("mBucketNum:\t" + mBucketNum);
	
		long hasher = 0x0L;
		
		HashMap<Integer, Vector<Long> > hashIndex = new HashMap<Integer, Vector<Long> >();
		HashMap<Long, Vty> hashInvert = new HashMap<Long, Vty>();
		
 		for(Entry<String, Vty> entry: hashObj.entrySet())
		{
			++mElementCnt;
			
			String key = entry.getKey();
			Vty val = entry.getValue();
			mValSize = val.size();	
			hasher = hashString(key);			
			
			//打印hash算法冲突的词，后期写文件记录
			if(hashInvert.containsKey(hasher) && hashInvert.get(hasher).equals(val)==false)
			{
				System.out.println(hasher + "\t" + val + "\t" + hashInvert.get(hasher));
			}
			hashInvert.put(hasher, val);
			
			int id = num_to_bucket_id(hasher, mBucketNum);
		
			if(hashIndex.containsKey(id))
			{
				hashIndex.get(id).add(hasher);
			}
			else
			{
				Vector<Long> vTmp = new Vector<Long>();
				vTmp.add(hasher);
				hashIndex.put(id, vTmp);
			}	
		}
 		
		mVecIndex = new int[mBucketNum];
		mVecInvert =  new byte[12 + mBucketNum*4 + mElementCnt*(mValSize+8)];//head + index + invert
		printLog("bufSize:" + mVecInvert.length);
		
		int invertPos = 0;	
		byte[] bTmp = mTransform.intToByte(mBucketNum);
		System.arraycopy(bTmp, 0, mVecInvert, invertPos, 4);
		invertPos += 4;
		
		bTmp = mTransform.intToByte(mElementCnt);
		System.arraycopy(bTmp, 0, mVecInvert, invertPos, 4);
		invertPos += 4;
		
		bTmp = mTransform.intToByte(mValSize);
		System.arraycopy(bTmp, 0, mVecInvert, invertPos, 4);
		invertPos += 4;
		
		//初始化桶，默认-1
		for(int i=0; i<mBucketNum; ++i)
		{
			mVecIndex[i] = -1;
			invertPos += 4;
		}
		int invertStartPos = invertPos;
		for(Entry<Integer, Vector<Long> > entry: hashIndex.entrySet())
		{		
			Integer indexPos = entry.getKey();
			Vector<Long> vTmp  = entry.getValue();
		
			Collections.sort(vTmp, new MyCompare());
			if(vTmp.size()!=0)
			{
				for(Long hash: vTmp)
				{
					byte[] p = mTransform.longToByte(hash);
					System.arraycopy(p, 0, mVecInvert, invertPos, 8);// hash:8
					invertPos += 8;
					
					Vty v = hashInvert.get(hash);
					p = v.toByteArray();//valSize:mSize 
					System.arraycopy(p, 0, mVecInvert, invertPos, mValSize);
					invertPos += mValSize;
				}
				mVecIndex[indexPos] = invertStartPos;
				invertStartPos = invertPos;		
			}
		}
		invertPos = 12;
		for(int i=0; i<mBucketNum; ++i)
		{
			bTmp = mTransform.intToByte(mVecIndex[i]);
			System.arraycopy(bTmp, 0, mVecInvert, invertPos, 4);
			invertPos += 4;
		}

		//System.out.println("mIndexSize:\t" + mVecIndex.length);
		//System.out.println("mInvertSize:\t" + mVecInvert.length);
		//System.out.println("--------------------------------");
		
		try 
		{
			hashIndex.clear();
			hashInvert.clear();
			FileOutputStream fOut = new FileOutputStream(fileName);
			/*
			byte[] buf = mTransform.intToByte(mBucketNum);
            wt.write(buf);
            buf = mTransform.intToByte(mElementCnt);
            wt.write(buf);
            buf = mTransform.intToByte(mValSize);
            wt.write(buf);
            for(int i=0; i<mBucketNum; ++i)
            {
            	buf = mTransform.longToByte(mVecIndex[i]);
                wt.write(buf);
            }
            */
			fOut.write(mVecInvert);
            
			fOut.close();

		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public int num_to_bucket_id(long hasher, int buck_num)
	{
		int id = (int)(hasher & (buck_num - 1));
		return id;
	}
	
	public long hashString(String k)
	{
		byte[] p = k.getBytes();
		return __hash64(p);
	}
	
	public long __hash64(byte[] p)
	{
		long h = __hash(p, 0);
		h <<= 32;
		h |= __hash(p, 1);
		return Math.abs(h);
	}
	
	public long __hash(byte[] p, int hash_type)
	{
		int cKey;
		long seed1 = 0x7FED7FEDL;
		long seed2 = 0xEEEEEEEEL;

		for(byte c: p)
		{
			cKey = (int)(c & 0xFF);
			seed1 = _CRYPT_TABLE[(hash_type << 8) + cKey] ^ (seed1 + seed2);
			seed2 = cKey + seed1 + seed2 + (seed2 << 5) + 3;		
		}
		
		return seed1;	
	}
	
	// 通用hash算法。不过hash值有冲突，暂且不用
	public long BKDRHash(String str)  
	{  
	    int seed = 131; // 31 131 1313 13131 131313 etc..  
	    long hash = 0;  
	    byte[] p = str.getBytes();
	    int i = 0;
	    for(byte c: p)  
	    {  
	    	i = (int)(c&0xFF);
	        hash = hash * seed + (i);  
	    }  
	    return (hash & 0x7FFFFFFF);  
	} 
	
	public long convertLong(long l)
	{
		long unSignedValue = l & Long.MAX_VALUE; 
		unSignedValue |= 0x80000000L;
		return unSignedValue;
	}
	
	public  static final long _CRYPT_TABLE[] = {
		0x55C636E2L,0x2BE0170L,0x584B71D4L,0x2984F00EL,0xB682C809L,0x91CF876BL,0x775A9C24L,0x597D5CA5L,0x5A1AFEB2L,0xD3E9CE0DL,0x32CDCDF8L,0xB18201CDL,0x3CCE05CEL,0xA55D13BEL,0xBB0AFE71L,0x9376AB33L,
		0x848F645EL,0x87E45A45L,0x45B86017L,0x5E656CA8L,0x1B851A95L,0x2542DBD7L,0xAB4DF9E4L,0x5976AE9BL,0x6C317E7DL,0xCDDD2F94L,0x3C3C13E5L,0x335B1371L,0x31A592CAL,0x51E4FC4CL,0xF7DB5B2FL,0x8ABDBE41L,
		0x8BEAA674L,0x20D6B319L,0xDE6C9A9DL,0xC5AC84E5L,0x445A5FEBL,0x94958CB0L,0x1E7D3847L,0xF35D29B0L,0xCA5CCEDAL,0xB732C8B5L,0xFDCC41DDL,0xEDCEC16L,0x9D01FEAEL,0x1165D38EL,0x9EE193C8L,0xBF33B13CL,
		0x61BC0DFCL,0xEF3E7BE9L,0xF8D4D4C5L,0xC79B7694L,0x5A255943L,0xB3DD20AL,0x9D1AB5A3L,0xCFA8BA57L,0x5E6D7069L,0xCB89B731L,0x3DC0D15BL,0xD4D7E7EL,0x97E37F2BL,0xFEFC2BB1L,0xF95B16B5L,0x27A55B93L,
		0x45F22729L,0x4C986630L,0x7C666862L,0x5FA40847L,0xA3F16205L,0x791B7764L,0x386B36D6L,0x6E6C3FEFL,0xC75855DBL,0x4ABC7DC7L,0x4A328F9BL,0xCEF20C0FL,0x60B88F07L,0xF7BB4B8FL,0x830B5192L,0x94F711ECL,
		0x20250752L,0x399D21A3L,0xE5C0840DL,0xE76CFFA5L,0x624FAB29L,0x5DF133E6L,0x83E0B9B8L,0xC5796BFBL,0x4A7AB2D0L,0xBA59A821L,0x3A81E4CL,0xCD3ADFDBL,0x32B26B8CL,0x8E35C533L,0x9E6300E9L,0x8CF92AC5L,
		0x880D18EBL,0x131A53B3L,0x2ED2DC64L,0xB23257C1L,0xA06450C1L,0x1B92CB8EL,0x72ED730EL,0x19A685F0L,0x82836483L,0x42D94E8AL,0xEE9BD6F6L,0x556D0B6AL,0xBA65589AL,0xDE24CCE4L,0x53329F6CL,0xC754FE8BL,
		0x503D2DC7L,0x10027BA4L,0xD3B60A8BL,0x68E68D83L,0xA9128A9L,0x595FA35FL,0xB03B5BEL,0x150A45C4L,0xB1629CCEL,0xE5F7497BL,0x8A7098A4L,0xB8233E69L,0x8EA0F978L,0x5B579970L,0xEAB14318L,0x4B28B263L,
		0xB6766CEFL,0x6782877L,0x155C6DD0L,0xC711333CL,0xF819CEDFL,0xEB1D68L,0xD6FFFA6EL,0x439E5962L,0xD765D6DBL,0xCB0BCEE9L,0x6D3C5647L,0x965466F3L,0xCA983C9L,0x74ECC1CEL,0xFC0563B6L,0x42B08FEEL,
		0xC5B38853L,0xFE502CEBL,0x7B432FAFL,0xC309E610L,0x2C3997D8L,0x43774654L,0x15BD9D2CL,0xED6A420DL,0xC7FF520CL,0xB8A97FD1L,0x5E4D60CCL,0xB9738D11L,0xDA2181FFL,0x73AC2597L,0x3A8EEC8DL,0xAC85E779L,
		0xF3F975D6L,0xB9FE7B91L,0xF155D1EL,0x2860B6DDL,0x835977CBL,0xB0607436L,0x9CAB7F6BL,0x8AB91186L,0xC12B51E9L,0x20084E8BL,0x44BA8EADL,0xA542B130L,0x82BCD5C4L,0xCC747F4EL,0xF1909D8L,0xDA242E1CL,
		0x6F7D1AA0L,0xD2626486L,0x88D0781EL,0xAB695CCDL,0xFA569145L,0xB4FEB55CL,0xBE47E896L,0xE70A7A88L,0xD56185A2L,0xACF4C871L,0x9282332L,0x1DDEEAA8L,0x590C7ADBL,0xF4A97667L,0xBFD85705L,0xEA77CCCL,
		0xA9F85364L,0x83195869L,0x8BFB041AL,0xDB842F5CL,0xD6F0F315L,0xA7756EA7L,0xA51B439L,0xA9EDF8A3L,0xD9084E2FL,0x827407F8L,0xD4AC8284L,0x9739D0DL,0xB3BB6CFCL,0xD539C77DL,0x6BBC9AC0L,0x35C641AAL,
		0x934C96B0L,0xD17AF317L,0x29C6BAEFL,0xB275CDACL,0xD72662DEL,0x9F5C2544L,0xC1A98F75L,0xD98E8F9AL,0x47BD5C86L,0x70C610A6L,0xB5482ED4L,0x23B9C68CL,0x3C1BAE66L,0x69556E7FL,0xD902F5E0L,0x653D195BL,
		0xDE6541FBL,0x7BCC6ACL,0xC6EE7788L,0x801534D4L,0x2C1F35C0L,0xD9DE614DL,0xBDCCAC85L,0xB4D4A0DAL,0x242D549BL,0x9D964796L,0xB9CEB982L,0x59FA99A9L,0xD8986CC1L,0x9E90C1A1L,0x1BBD82FL,0xD7F1C5FDL,
		0xDD847EBAL,0x883D305DL,0x25F13152L,0x4A92694DL,0x77F1E601L,0x8024E6E7L,0x2A5F53DL,0x9C3EF4D9L,0xAF403CCCL,0xE2AD03C0L,0x46EDF6ECL,0x6F9BD3E6L,0xCC24AD7AL,0x47AFAB12L,0x82298DF7L,0x708C9EECL,
		0x76F8C1B1L,0xB39459D2L,0x3F1E26D9L,0xE1811BE7L,0x56ED1C4DL,0xC9D18AF8L,0xE828060EL,0x91CADA2EL,0x5CCBF9B7L,0xF1A552D4L,0x3C9D4343L,0xE1008785L,0x2ADFEEBFL,0xF90240A0L,0x3D08CCE7L,0x426E6FB0L,
		0x573C984FL,0x13A843AEL,0x406B7439L,0x636085D9L,0x5000BA9AL,0xAD4A47ABL,0xAF001D8DL,0x419907AEL,0x185C8F96L,0xE5E9ED4DL,0x61764133L,0xD3703D97L,0xAC98F0C6L,0xDBC3A37CL,0x85F010C4L,0x90491E32L,
		0xF12E18BFL,0xC88C96E1L,0xD3FBD6D9L,0xE3C28B08L,0xD5BF08CCL,0xB1E78859L,0x2546DDCFL,0xB030B200L,0xAAFD2811L,0x55B22D21L,0xD38BF567L,0x469C7A2BL,0x5AD05792L,0xA1A5981EL,0x7DFB8384L,0x34D1CA0AL,
		0x7EB0DBE0L,0xD61CE0F6L,0x398068B7L,0xE6406D1FL,0x95AE6B47L,0xE4281230L,0xB0843061L,0xA70A3A68L,0xE340F625L,0x72DCBFFDL,0x8EB8AFCDL,0x18B6661FL,0x17EF5A5CL,0xC5B22L,0x6BA13836L,0x6165E383L,
		0x74481C5BL,0xE56F0711L,0xA26F5024L,0x5FF22E60L,0x31A5E829L,0xA1094BF0L,0xC680EC6CL,0x8CF327D7L,0xEBF1348AL,0x6A227D2FL,0x74065184L,0x8DF65112L,0x2BBD05EEL,0xE4D00ED6L,0x2980EE1AL,0x6AE1DA73L,
		0xE84614DAL,0x6C9906ABL,0xCF8E02DBL,0xD3723E97L,0x92F66CAFL,0xAC8491C7L,0xAEC65696L,0xB98997CFL,0xFA16C762L,0x6D73C65FL,0x205D22A6L,0x4DD3AAA5L,0x2DEB6BC0L,0x9F37686CL,0x71A5282BL,0x376BB9E0L,
		0x7FFF2A1BL,0xDE67982FL,0x9CBF33CEL,0x2E6DAB37L,0x6E3424B9L,0xEE143BCL,0x832A60D9L,0xBB6329E1L,0x13F6BEFDL,0x5965FB84L,0xF60B233CL,0x3D695183L,0x433224A1L,0xB5D9CAE5L,0x82459BABL,0x9F21B311L,
		0xAF6C5247L,0xB447B13AL,0x7B2676C3L,0xC38979CDL,0x8526AE25L,0xC550AD5BL,0x685099A7L,0x65E9C2BDL,0xE5C6DC36L,0xE10B37A9L,0x88016878L,0xCE81D4E4L,0x24D6FC80L,0x4106152DL,0x6D4F5F90L,0xC4DC74BEL,
		0xDB48676CL,0x6CB569B7L,0xF3BF598FL,0x42B08D9L,0x2CCB2DEL,0xB1056F65L,0x47994AF4L,0xFA141BA4L,0x9376AB2EL,0x7A76737L,0x75E7E6FCL,0x449D80A1L,0x3B7259DL,0xF6DF358AL,0x5A75D5B9L,0x47286923L,
		0x3B1A30EFL,0xEEBE3D6AL,0x9DB1AA00L,0x7A90D9L,0x24667071L,0x19C73CFL,0x69039BCDL,0x95900744L,0x6518B1EBL,0x6905F202L,0xEE3951B2L,0xE141FCA9L,0x797FA832L,0x5A95E55BL,0xD6263B15L,0x5B61F394L,
		0x897ACB1CL,0x5F83A9L,0x22420F71L,0xF495176EL,0x7E138F3DL,0x1392E384L,0x373BF7AAL,0x8E512816L,0xA960B3CAL,0x474D74CL,0xFFACD6D7L,0x2EF5ED9EL,0x60992AAAL,0x7E690E99L,0x23C0749DL,0xD8E29105L,
		0x555D5909L,0x15631BFEL,0xA69C5A1CL,0x501017CAL,0x99438048L,0x38733AC7L,0xE682E2C8L,0xD4655FD6L,0x956E4C04L,0x347DF643L,0x2F4B177BL,0x93ED3AA4L,0xA77E1DD5L,0x7AE55702L,0xD2A52FD9L,0xEF8BA18CL,
		0xB7D3C1EEL,0x8078BA8DL,0xAB5AAADBL,0x752BE08FL,0x68B31C1L,0x78AAE3CL,0xAA5A8343L,0x123D9268L,0x2CEAEE43L,0x8EBDB239L,0x650251F3L,0x4883648L,0x8C62E12EL,0x12B32167L,0xE5112E9AL,0x10002548L,
		0x3E7A818DL,0x77E5327L,0xF140CC21L,0x6CE7D75DL,0x9B99F9A5L,0x3215741CL,0xB6AADBAEL,0x738768DCL,0x82A3742FL,0x76517020L,0xDD872AD8L,0x9D0902B2L,0x7D1A6B04L,0x49381592L,0x63A652A5L,0xC15E626L,
		0xE22F70D6L,0x1E84385L,0xB29DE134L,0x20C5000EL,0xE961F443L,0x2D31662EL,0x3CE6BC28L,0x34F9DD94L,0xFA45DE53L,0x497588BDL,0x9468215BL,0x777FA5CL,0x6F7114C0L,0xE0E82694L,0xE4371986L,0x57112DE2L,
		0xE0CAC289L,0xF2A3CEE0L,0x6A41E1B9L,0xBFCEA77DL,0xF927FD52L,0x69747D98L,0xBEA76CDBL,0x8DD39557L,0x4DB5ECEL,0x2A0885C8L,0x3BE4E8EEL,0x21D785DCL,0x9DE7C0EL,0x3258EA33L,0x51922982L,0xEE8DD024L,
		0x3DF6965DL,0x30C1237BL,0xF7F6686AL,0x9FACA186L,0x7C400076L,0x85ACEF8AL,0xF4B6D220L,0xDDC3481CL,0x439EAEC4L,0x717BBE63L,0x8259FAA7L,0xD682BD68L,0x932A8610L,0x38BF0A7FL,0x6212E2C7L,0x88EE3168L,
		0xB3C27047L,0x6133CB1EL,0x15295506L,0x5AE66246L,0x1D208DDDL,0xA91D3DBAL,0xC315968DL,0x6AA2664BL,0x716D0CCAL,0x891F4956L,0x80866BFFL,0xBD56C847L,0x9093425AL,0x28DD9E87L,0x84EF3E08L,0x690A49D6L,
		0x6A7EFF82L,0xABCFE400L,0x3D3BE5CAL,0x381B650CL,0x4B7C8622L,0x3E0246F3L,0xA3561654L,0x9488865CL,0x3AEF1BF2L,0x5E5D68A2L,0xD32F1DDCL,0x51972BF0L,0x177A213BL,0x469375C2L,0x37640BD0L,0xFC3324C8L,
		0x7091A09L,0x2D63D3FBL,0x2153F023L,0x48223875L,0x61A55826L,0x8C136538L,0x49F71D98L,0x84C7D51EL,0x85551A73L,0x13D604C5L,0xD701A626L,0x87B844CAL,0x741EB29DL,0x2A2C977CL,0xC797CA03L,0x6C4085D7L,
		0x2DACF79BL,0x734FA2EBL,0xCC290557L,0xFA1E75E4L,0x6B29A27L,0xBECE2A7AL,0x70A4554BL,0xC935942EL,0xA764BBC1L,0x1FE391D6L,0x7807F0C2L,0x40606ED9L,0xE5153086L,0xE91D7DD2L,0xED5D3BA9L,0xAA14B64AL,
		0x83B24DD9L,0xEC1FF5CDL,0xBA33EAD3L,0xE4EF735CL,0xBC062438L,0xD8BFD523L,0x473D1E04L,0x2007F8A7L,0xB02903EDL,0x86EA8ADAL,0x95AB69CFL,0xFD1F9809L,0x9CB3D8BBL,0x51F45958L,0x9CDD4276L,0xC245865EL,
		0x8F0C836BL,0x4EE7DC07L,0xF6368D9DL,0xEF2C1DC1L,0xEE56B54BL,0xBD62CE2FL,0xF4916AADL,0xC81CB594L,0x41729F49L,0x24BEF0A4L,0xDEF487A9L,0x222E05B8L,0x8D3BF5C6L,0x11B55009L,0xAD09D2B3L,0x19DB9FD1L,
		0xD7427085L,0x33DBFC8BL,0x526B9378L,0x790E1BC8L,0xB2998A00L,0xA5641703L,0x676D249L,0x6B9185CCL,0x30E4348FL,0x82C52F65L,0x57C7DC24L,0x489C1ECDL,0x9FCAB02AL,0x56D61117L,0xFE869CACL,0x55FC5140L,
		0x7FBBB382L,0x9E5AFC79L,0x10047C99L,0xFC9F5984L,0x56587E2DL,0xB98193F0L,0x98FE5E8EL,0x29B15B6BL,0x9561F055L,0xBB0CAA25L,0x1E4ECC15L,0x23F5393BL,0x845B458L,0xCEFF67CAL,0xB099900CL,0xB1564FL,
		0x39EEF3D1L,0xFCC1BF84L,0xAC8893B5L,0x6484BF0EL,0x91C02AB3L,0x8C0C0C70L,0x686FA8C6L,0xE171BED6L,0xDFAE37DFL,0xD5A1A4E7L,0xE3EB49A1L,0x5E6014E0L,0x205B21ACL,0xFD58B3DAL,0x2E7C07CDL,0xEF2CC85AL,
		0xD7587B46L,0xF417847DL,0x8A30CEC1L,0x70984F6CL,0xF0B63388L,0xC220C98DL,0xEDE62936L,0x92C0A7B3L,0x1EF371E8L,0x2005F7AFL,0x91A47265L,0xB0CF5504L,0xD500ABA8L,0xCB5C4BD3L,0x9B3BCBC3L,0xCF6644B5L,
		0xCE9488EFL,0x3FC96EL,0xAA42222FL,0x4844F3D0L,0x4DB89D77L,0x8681AAEL,0x662F3A28L,0x761552DBL,0x1DF7A17AL,0x93FEED9AL,0xCC496A4FL,0xA217CFCDL,0x3BA3C930L,0x268F7E77L,0x797B4A1L,0x8BEBFC51L,
		0x68930C4L,0x16C874E2L,0xC242DA24L,0xFB229F76L,0xA0795B02L,0x689FC036L,0x17A73732L,0xD21AEC00L,0xAC00A692L,0x5B217F18L,0xAE421624L,0x2BC05CC0L,0x48C1DB7AL,0x4F4E63B4L,0x1667F04EL,0x34020F94L,
		0x972B2555L,0x9A07355BL,0x1665970L,0x7DB60C6FL,0x3AD7103BL,0x5C3D09C0L,0xEEA3DADAL,0x88C21C10L,0x102436D7L,0x6A3B3400L,0xEB523C4CL,0xFB97D896L,0x964CB86BL,0xDD878038L,0x529DA4DL,0xB1468A5L,
		0x18739AC8L,0xF7F26668L,0xF64F4471L,0x5C14F5C3L,0x44A081FBL,0x39AC7E37L,0x8A17C26BL,0x868F5E67L,0x3931978DL,0x6EDF7817L,0x4951CC67L,0x943407F3L,0xCC5E748FL,0x2B7EE729L,0xCBB320F0L,0x11FEC8E7L,
		0xFCCFC658L,0x3454354L,0x373AA1ECL,0x1D58FE9AL,0x64710AEL,0xA88AA0BAL,0xD183A23EL,0x40D150A3L,0xF531B8D1L,0xA7D99F85L,0x11838CD5L,0xB19E64B3L,0x3D67A5E9L,0xB02C5AC6L,0x99B9B9E8L,0x4C202B7AL,
		0x15F261D3L,0xA84C2D0DL,0x50F185A6L,0x33BA41D5L,0x39791013L,0x4BAFF44EL,0xEEEEAA1CL,0xE0488314L,0x559CCD2BL,0xA104F445L,0x636F37C4L,0x264D5E3BL,0x75C17F35L,0x75424131L,0xBB115739L,0x74FE755AL,
		0x7D3A7AA6L,0x2D8BE784L,0x83ED154AL,0xFC2673D8L,0x44DD4A7FL,0x79056CC8L,0x82CC8831L,0x9D3C1B7CL,0xE9453BFAL,0x24315694L,0x661F3253L,0x75549F5CL,0xBB2B63EDL,0x67E00D96L,0xF48966C7L,0xD7BEA56L,
		0xC25F92EFL,0xA947A79DL,0xDE4ADF6FL,0xAC0F0342L,0xD3EB246BL,0xA4AA118EL,0x3C3E6A46L,0x457F4441L,0xA50A406FL,0x6C508D9FL,0xE9AC18E7L,0x1ECDB4BAL,0x39AC7E3AL,0x7FB304FAL,0x6F38F8E8L,0x4AECEA6DL,
		0x61035E73L,0x81708907L,0xEBC07205L,0x90FD7614L,0xB52D217FL,0x6C4DE195L,0x1DD49084L,0x64EE482CL,0x94C7A521L,0x540C09D8L,0x75DF8DD5L,0x414131F7L,0x3698FD76L,0xF784DB4FL,0xF8C97A03L,0x48F39B9L,
		0x3BF4F0BDL,0x8CB50992L,0x9B58D9EEL,0xE5AB79CCL,0x9A5F6052L,0xBD9591B0L,0xFAD2232BL,0x5A632254L,0x286E618L,0x8AD3C8F7L,0xE4060176L,0x754C4617L,0x5C10490BL,0x6F7D6FFFL,0x2187B42AL,0x5775095BL,
		0x2F4C663L,0x5A5DCA06L,0xFE4AD4C7L,0x53E19F7DL,0x59FF46B5L,0xBCC42BA5L,0xFD2F4A97L,0xBED6D905L,0x95629B6BL,0x21A1C0DBL,0xAA10B45DL,0xE6EF6D58L,0x2892CF4DL,0x9FED6C10L,0x1E386BF7L,0x9BE0C6E8L,
		0x2B2F15EFL,0x19F5AC7BL,0x7AFF0E72L,0x31DA576FL,0x30252CB4L,0x577960ACL,0x166E9E5AL,0xA9374A61L,0x71369C96L,0x7FF826AEL,0xE8175326L,0xCABBFD33L,0x191190EL,0x699D3C3EL,0x36B40B22L,0xB3950513L,
		0x9B889BFAL,0xA52A5007L,0xAC290FEDL,0x3B4E4A4FL,0xB753D8D6L,0x3C531F22L,0x582F6427L,0xA9CD93A9L,0x546E39AEL,0x242FAAD2L,0xD2E0F747L,0x9F6325DL,0x59D48719L,0xAD7EB66EL,0xD5512878L,0x56DEBF9DL,
		0x5107E5A5L,0xF1C00AA4L,0x814CCCA8L,0x600D90F0L,0x9BE97619L,0x915FA5F2L,0x2B5628DDL,0xA33D5F5AL,0x595DF7C1L,0x6966215DL,0x50EC8337L,0xF1D21372L,0xEE2EEFBL,0xAD9E70B7L,0xAB0D2FE4L,0xCF277B5DL,
		0x62585A2CL,0x835A7844L,0x74B1FA6BL,0x49BAFFD5L,0x2EA9C864L,0x129311A8L,0xBDFA1867L,0x83CA5997L,0x9D1DB719L,0x84BB79E6L,0x9E3F99F2L,0x313F6101L,0x1B99245BL,0xD15D8FB2L,0xCEF90F81L,0x2945268DL,
		0xDBBCF573L,0xB1021886L,0x9EE7EC1DL,0x1CF824F7L,0x7EAA2E32L,0x69C0A2B5L,0x7494419CL,0xE253D7D3L,0x48DA3D12L,0x45B8B571L,0xDB4D147AL,0xD82D8DDEL,0x265D10A2L,0xB0A6EB9AL,0x7E1C93A6L,0x36FE2F46L,
		0xDCAD6B00L,0x5439191L,0xB0CE5484L,0x61D1C309L,0x8DA62A03L,0x6D0FE2FL,0xBAC6DD3CL,0xCA2006F3L,0x8321B1AFL,0x411A6F3L,0xE8918EACL,0x21A2C152L,0x91C0D54FL,0x6AAA14FAL,0xDD22A440L,0x88CB2075L,
		0x7A4EB813L,0x67AFA071L,0xD8D98C9CL,0x31F10D47L,0x6FF1A8A8L,0x2FAAF0A1L,0x48A221BBL,0x3BE6948BL,0xAA79E79BL,0xEA7278CL,0x7A3857EFL,0x49B7FE55L,0xD51CB931L,0x41C018DL,0xB90501L,0x45EA7881L,
		0x8FC1DBCFL,0xB80B32A9L,0xABACD2E9L,0x677BDC40L,0xECACE542L,0x6D6514EBL,0x31C09FF7L,0x5E6C1ABDL,0x1C391D0FL,0xE9D77F1L,0x7119392DL,0x6BE9B0BAL,0x6194FA77L,0x45E62148L,0x42234AF2L,0xC3239D66L,
		0x939CBDBCL,0x56200D9CL,0x6B275208L,0x1A61F3L,0xCCC2A546L,0x4B722BE0L,0xEE25F2B7L,0x6D86CF9EL,0xAA6BE0CDL,0x4DCDA7B6L,0x78D4AA13L,0x36EA7AD9L,0x3F29D700L,0xDEEA2D84L,0x6A6AF5BDL,0x18AFB81CL,
		0xD8E4E73CL,0x8AA708BAL,0x658B94D9L,0xA676478CL,0xCFA10C22L,0x25593C74L,0x8D962235L,0x5F980270L,0x3DF6EBC0L,0x8E7D92FAL,0xC3EE55E1L,0xD5F72447L,0x2B0FA95L,0x52B0B520L,0x70D2C11FL,0x3A6FDD6CL,
		0x193AA698L,0x5496F7D5L,0x4208931BL,0x7A4106ECL,0x83E86840L,0xF49B6F8CL,0xBA3D9A51L,0x55F54DDDL,0x2DE51372L,0x9AFB571BL,0x3AB35406L,0xAD64FF1FL,0xC77764FEL,0x7F864466L,0x416D9CD4L,0xA2489278L,
		0xE30B86E4L,0xB5231B6L,0xBA67AED6L,0xE5AB2467L,0x60028B90L,0x1D9E20C6L,0x2A7C692AL,0x6B691CDBL,0x9E51F817L,0x9B763DECL,0x3D29323FL,0xCFE12B68L,0x754B459BL,0xA2238047L,0xD9C55514L,0x6BDCFFC1L,
		0x693E6340L,0x82383FE7L,0x1916EA5FL,0xEC7BCD59L,0x72DE165AL,0xE79A1617L,0x8EC86234L,0xA8F0D284L,0x20C90226L,0x7BF98884L,0x28A58331L,0x3EC3FA6EL,0x4CE0895BL,0xC353B4D0L,0x33EF064FL,0x21E5E210L,
		0xC8BB589DL,0xE85DCAB2L,0xAC65829FL,0xA7BF92D0L,0x5A6174DL,0x25A50C2EL,0xE5C78777L,0x3D75021FL,0x4BAA9C98L,0x23BDC884L,0x9653BBD7L,0xBADCE7F5L,0xC283A484L,0xC040DF2EL,0x9370A841L,0x2F316022L,
		0x36EED231L,0xAC2CBC0CL,0x13C0A49BL,0xCDD12997L,0x7FE91B2L,0xCD7EABCDL,0x2C01271DL,0x18432DF8L,0x599C6BC7L,0x75E93D5AL,0xB67A6EE2L,0x8E738E16L,0xFF9073FDL,0xAF77026AL,0xF86EA2FCL,0x91509EA3L,
		0x33A78DC6L,0x4F79234AL,0x3A7535BCL,0x3539FCB1L,0x3103EE52L,0x4F6F1E69L,0x6BB3EBBCL,0x4CB77555L,0x8DD1E999L,0x2ADE439DL,0x11521FAEL,0xB94D2545L,0x8DDE9ABDL,0x1909393FL,0xB792A23DL,0x749C455BL,
		0xB5B60F2CL,0x380459CEL,0xDAD5820L,0xB130845BL,0x291CBD52L,0xDE9A5BB7L,0x51DEF961L,0x515B6408L,0xCA6E823EL,0x382E6E74L,0xEEBE3D71L,0x4C8F0C6AL,0xE676DCEAL,0x14E1DC7CL,0x6F7FC634L,0xCF85A943L,
		0xD39EA96EL,0x136E7C93L,0x7164B304L,0xF32F1333L,0x35C34034L,0xDE39D721L,0x91A87439L,0xC410111FL,0x29F17AACL,0x1316A6FFL,0x12F194EEL,0x420B9499L,0xF72DB0DCL,0x690B9F93L,0x17D14BB2L,0x8F931AB8L,
		0x217500BCL,0x875413F8L,0x98B2E43DL,0xC51F9571L,0x54CEBDCAL,0x719CC79L,0xF3C7080DL,0xE4286771L,0xA3EAB3CDL,0x4A6B00E0L,0x11CF0759L,0x7E897379L,0x5B32876CL,0x5E8CD4F6L,0xCEDFA64L,0x919AC2C7L,
		0xB214F3B3L,0xE89C38CL,0xF0C43A39L,0xEAE10522L,0x835BCE06L,0x9EEC43C2L,0xEA26A9D6L,0x69531821L,0x6725B24AL,0xDA81B0E2L,0xD5B4AE33L,0x80F99FBL,0x15A83DAFL,0x29DFC720L,0x91E1900FL,0x28163D58L,
		0x83D107A2L,0x4EAC149AL,0x9F71DA18L,0x61D5C4FAL,0xE3AB2A5FL,0xC7B0D63FL,0xB3CC752AL,0x61EBCFB6L,0x26FFB52AL,0xED789E3FL,0xAA3BC958L,0x455A8788L,0xC9C082A9L,0xA1BEF0EL,0xC29A5A7EL,0x150D4735L,
		0x943809E0L,0x69215510L,0xEF0B0DA9L,0x3B4E9FB3L,0xD8B5D04CL,0xC7A023A8L,0xB0D50288L,0x64821375L,0xC260E8CFL,0x8496BD2CL,0xFF4F5435L,0xFB5560CL,0x7CD74A52L,0x93589C80L,0x88975C47L,0x83BDA89DL,
		0x8BCC4296L,0x1B82C21L,0xFD821DBFL,0x26520B47L,0x4983E19L,0xD3E1CA27L,0x782C580FL,0x326FF573L,0xC157BCC7L,0x4F5E6B84L,0x44EBFBFBL,0xDA26D9D8L,0x6CD9D08EL,0x1719F1D8L,0x715C0487L,0x2C2D3C92L,
		0x53FAABA9L,0xBC836146L,0x510C92D6L,0xE089F82AL,0x4680171FL,0x369F00DEL,0x70EC2331L,0xE253D55L,0xDAFB9717L,0xE5DD922DL,0x95915D21L,0xA0202F96L,0xA161CC47L,0xEACFA6F1L,0xED5E9189L,0xDAB87684L,
		0xA4B76D4AL,0xFA704897L,0x631F10BAL,0xD39DA8F9L,0x5DB4C0E4L,0x16FDE42AL,0x2DFF7580L,0xB56FEC7EL,0xC3FFB370L,0x8E6F36BCL,0x6097D459L,0x514D5D36L,0xA5A737E2L,0x3977B9B3L,0xFD31A0CAL,0x903368DBL,
		0xE8370D61L,0x98109520L,0xADE23CACL,0x99F82E04L,0x41DE7EA3L,0x84A1C295L,0x9191BE0L,0x30930D02L,0x1C9FA44AL,0xC406B6D7L,0xEEDCA152L,0x6149809CL,0xB0099EF4L,0xC5F653A5L,0x4C10790DL,0x7303286CL
	};
	

	public static  void main(String[] argc) throws ClassNotFoundException
	{
		
		String fileName = "npathword.idx";
		
		//test			
		HashMap<String, WordCount> hs = new HashMap<String, WordCount>();	
        try 
        {
        	File file = new File("/server/gomewordsegment/npathword.data");
        	BufferedReader reader  = new BufferedReader(new FileReader(file));
            String line = new String(); 
            int i = 0;
            while ((line = reader.readLine()) != null) 
            {
            	line = line.trim();
                hs.put(line, new WordCount(i++));
            }
            reader.close();  
        }         
        catch (IOException e) 
        {
            e.printStackTrace();
        } 
        /*
        StaticHash<WordCount> hsWrite = new StaticHash<WordCount>();
		hsWrite.writeToHashFile(hs, 16, fileName);
		System.out.println("valSize:" + hs.size());
		System.out.println("---------------write to file over---------------");
		*/
		StaticHash<WordCount> hsReader = new StaticHash<WordCount>();

		hsReader.loadHashFileToMemory(fileName);
		System.out.println("---------------load to memory over---------------");
		
		/*
		Value<String>[] v = new Value[606854];
		for(int i=0; i<v.length; ++i)
		{
			Value<String> x = new Value<String>();
			x.mVal = "asdasdasdaaaaaaaaaaaaaasdasddddddddd";
			x.mHasher = 1212312312313L;
			v[i] = x;
		}
		*/
		/*
		try
		{
			ByteArrayOutputStream bOut = new ByteArrayOutputStream(); //构造一个字节输出流
	        ObjectOutputStream out  = new ObjectOutputStream(bOut);
	
	        out.writeObject(v);
	        byte[] buf = bOut.toByteArray(); //从这个地层字节流中把传输的数组给一个新的数组
	        out.flush(); 
	        FileOutputStream wt = new FileOutputStream(fileName);
	        wt.write(buf);
	        wt.close();           
	        System.out.println("bufSize:" + buf.length);
		}
		catch(IOException e)
		{
			
		}
		*/
		
		
		//System.out.println(hsReader.getVal("顾真安罗开富常广生吴湘峰赵新先任克明纪为民刘佳民"));
		//System.out.println(hsReader.getVal("渡边美智雄"));
     
		
		for(Map.Entry<String, WordCount>entry: hs.entrySet() )
		{
			String k = entry.getKey();
			WordCount v = entry.getValue();
			byte[] p = hsReader.getVal(k);
			WordCount n = new WordCount(p);
			//System.out.println(k + "|" + v.mCnt + "|" + n.mCnt );
			if(v.mCnt==n.mCnt)
				continue;
			System.out.println(k + "|" + hsReader.hashString(k));
		}
	
		/*
		StaticHash<Integer> mStaticWordToNum = new StaticHash<Integer>();
		String fileName = "wordtonum.data";
		mStaticWordToNum.loadHashFileToMemory(fileName, -1);
		System.out.println("手机:" + mStaticWordToNum.getVal("手机"));
		System.out.println("暴玉怀李金清:" + mStaticWordToNum.getVal("暴玉怀李金清"));
		System.out.println("Lanham:" + mStaticWordToNum.getVal("Lanham"));
		System.out.println("黄省曾:" + mStaticWordToNum.getVal("黄省曾"));
		*/
		System.out.println("-----------------------------------------------------------------");
	}
}




