///*
// * Licensed to Elasticsearch under one or more contributor
// * license agreements. See the NOTICE file distributed with
// * this work for additional information regarding copyright
// * ownership. Elasticsearch licenses this file to you under
// * the Apache License, Version 2.0 (the "License"); you may
// * not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package org.es.plugin.functionquery;
//
//import org.apache.lucene.index.AtomicReaderContext;
//import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.index.IndexableField;
//import org.apache.lucene.index.Term;
//import org.apache.lucene.search.*;
//import org.apache.lucene.util.Bits;
//import org.apache.lucene.util.ToStringUtils;
//import org.elasticsearch.common.lucene.search.function.CombineFunction;
//import org.elasticsearch.common.lucene.search.function.ScoreFunction;
//import org.es.plugin.functionquery.nativeplugin.ESProductScript;
//
//import java.io.IOException;
//import java.util.Set;
//
//public class CustomFunctionScoreQuery extends  Query{
//	
//	final static String path="sku";
//    Query subQuery;
//    final ScoreFunction function;
//    float maxBoost = Float.MAX_VALUE;
//    CombineFunction combineFunction;
//    String regionId=null;
//    boolean fake=true;
//    String catIds;
//    int sCityId=1;
//    int gomeSortType=0;
//    boolean gomeSortAsc=false; 
//    
//    public CustomFunctionScoreQuery(Query subQuery, ScoreFunction function) {
//        this.subQuery = subQuery;
//        this.function = function;
//        this.combineFunction = function.getDefaultScoreCombiner();
//    }
//
//    public void setCombineFunction(CombineFunction combineFunction) {
//        this.combineFunction = combineFunction;
//    }
//    
//    public void setMaxBoost(float maxBoost) {
//        this.maxBoost = maxBoost;
//    }
//
//    public float getMaxBoost() {
//        return this.maxBoost;
//    }
//
//    public Query getSubQuery() {
//        return subQuery;
//    }
//
//    public ScoreFunction getFunction() {
//        return function;
//    }
//    public String setRegionId(String regionId){
//        return this.regionId=regionId;
//        
//    }
//    public String getRegionId(){
//        return this.regionId;
//    }
//    public int setCityId(String cityId){
//    	if(cityId!=null && cityId.length()!=0){
//        this.sCityId=Integer.parseInt(cityId);
//    	}
//    	return  this.sCityId;
//    	
//    }
//    public Boolean setFake(Boolean fake){
//        return this.fake=fake;
//    }
//    public String setCatIds(String catIds){
//        return this.catIds=catIds;
//    }
//    
//    public int setGomeSortType(int gomeSortType) {
//        return this.gomeSortType = gomeSortType;
//    }
//
//
//    public Boolean setGomeSortAsc(boolean gomeSortAsc) {
//        return this.gomeSortAsc = gomeSortAsc;
//    }
//    
//    @Override
//    public Query rewrite(IndexReader reader) throws IOException {
//        Query newQ = subQuery.rewrite(reader);
//        if (newQ == subQuery) {
//            return this;
//        }
//        CustomFunctionScoreQuery bq = (CustomFunctionScoreQuery) this.clone();
//        bq.subQuery = newQ;
//        return bq;
//    }
//
//    @Override
//    public void extractTerms(Set<Term> terms) {
//        subQuery.extractTerms(terms);
//    }
//
//    @Override
//    public Weight createWeight(IndexSearcher searcher) throws IOException {
//        Weight subQueryWeight = subQuery.createWeight(searcher);
//        return new CustomBoostFactorWeight(subQueryWeight);
//    }
//
//    class CustomBoostFactorWeight extends Weight {
//
//        final Weight subQueryWeight;
//
//        public CustomBoostFactorWeight(Weight subQueryWeight) throws IOException {
//            this.subQueryWeight = subQueryWeight;
//        }
//
//        public Query getQuery() {
//            return CustomFunctionScoreQuery.this;
//        }
//
//        @Override
//        public float getValueForNormalization() throws IOException {
//            float sum = subQueryWeight.getValueForNormalization();
//            sum *= getBoost() * getBoost();
//            return sum;
//        }
//
//        @Override
//        public void normalize(float norm, float topLevelBoost) {
//            subQueryWeight.normalize(norm, topLevelBoost * getBoost());
//        }
//
//        @Override
//        public Scorer scorer(AtomicReaderContext context, Bits acceptDocs) throws IOException {
//            // we ignore scoreDocsInOrder parameter, because we need to score in
//            // order if documents are scored with a script. The
//            // ShardLookup depends on in order scoring.
//            Scorer subQueryScorer = subQueryWeight.scorer(context, acceptDocs);
//            if (subQueryScorer == null) {
//                return null;
//            }
//            function.setNextReader(context);
//            return new CustomBoostFactorScorer(this, subQueryScorer, function, maxBoost, combineFunction);
//        }
//
//        @Override
//        public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
//            Explanation subQueryExpl = subQueryWeight.explain(context, doc);
//            if (!subQueryExpl.isMatch()) {
//                return subQueryExpl;
//            }
//            java.util.List<IndexableField> fieldss=context.reader().document(doc).getFields();
//            double promoScore=0;
//            String skuNo="";
//            double weight=0;
////            long salesVolume=0;
//            long areaSalesVolume=0;
//            long productTag=0;
//            boolean ignoreProductTag = false;
//            
//            //是否是海外购商品
//            boolean isMakeup = false;
//            //是否是价格补差商品
//            boolean isMarketTag = false;
//            
//            for(IndexableField indexField:fieldss){
//            
//            	if(indexField.name().equals(path+"."+"promoScore")){
//            		promoScore=indexField.numericValue().doubleValue();
//            	}
//            	
//            	if(indexField.name().equals(path+"."+"skuNo")){
//                  skuNo=indexField.stringValue();
//                }
//            	
//            	if(indexField.name().equals(path+"."+"weight")){
//                  weight=indexField.numericValue().doubleValue();
//                }
//              
//            	if(indexField.name().equals(path+"."+"productTag")){
//                  productTag=indexField.numericValue().longValue();
//            	}
//              
//            	if(indexField.name().equals(path+"."+ESProductScriptConst.FIELD_IGNOREPRODUCTTag)){
//            	  if(!indexField.stringValue().isEmpty() && indexField.stringValue().equals(ESProductScriptConst.FIELD_IGNOREPRODUCTTag_VALUE_Y) ){
//            		  ignoreProductTag=true; 
//            	  }
//            	}
//            	  
//            	if(indexField.name().equals(path+"."+ESProductScriptConst.FIELD_MAKEUP_FLAG_STORE)){
//            	  if(indexField.numericValue()!=null && indexField.numericValue().equals(1) ){
//            		  isMakeup=true;
//            	  }
//            	}
//            	  
//        	    if(indexField.name().equals(path+"."+ESProductScriptConst.FIELD_MARKETTAG_STORE)){
//            	  if(indexField.numericValue()!=null && indexField.numericValue().equals(1) ){
//            		  isMarketTag=true;
//            	  }
//            	}   
//              
//            }
//                  
//            function.setNextReader(context);
//            Explanation functionExplanation = function.explainScore(doc, subQueryExpl);
//            
//            Explanation functionExplanation1 =new Explanation();
//            functionExplanation1.setValue(functionExplanation.getValue());
//            functionExplanation1.setDescription(functionExplanation.getDescription());
//            
//            int stock=ESProductScript.getStock(skuNo, sCityId);
////             stock=1;
//            
//                if(fake){
//                    addExplainDetailFake(weight,ESProductScript.getWWeight(),stock,1000,promoScore,ESProductScript.getProsWeight(),areaSalesVolume,10000,productTag,functionExplanation1,ignoreProductTag,isMakeup,isMarketTag);
//                }else{
//                    addExplainDetail(weight,100,stock,1000,promoScore,80,0,10000,functionExplanation1);
//                }
//           
//            return combineFunction.explain(getBoost(), subQueryExpl, functionExplanation1, maxBoost);
//        }
//    }
//    
//    /**
//     * 
//    * @Title: addExplainDetailFake
//    * @Description: 搜索页的检索排序分析
//    * @param @param weight
//    * @param @param weightFactor
//    * @param @param stock
//    * @param @param stockFactor
//    * @param @param promoScore
//    * @param @param promoScoreFactor
//    * @param @param areasalesVolume
//    * @param @param salesVolumeFactor
//    * @param @param productTag
//    * @param @param explSrc    设定文件
//    * @return void    返回类型
//    * @throws
//     */
//    private void addExplainDetailFake(double weight,
//    				float weightFactor,int stock,int stockFactor,double promoScore ,
//    				double promoScoreFactor,long areasalesVolume,int salesVolumeFactor,
//    				long productTag,Explanation explSrc,boolean ignoreProductTag,
//    				boolean isMakeup,boolean isMarketTag){
//        
//        double factor1=Math.sqrt(weight*weightFactor);
//        String factor2=ESProductScript.mul(stock, stockFactor);
//        String factor3=ESProductScript.mul(promoScore, promoScoreFactor);
//        String factor4=ESProductScript.divl(areasalesVolume, salesVolumeFactor);
//        String factor5="0";
//            
//        double total= explSrc.getValue();
//        
//        if(productTag==1){
//           if(total<1000){
//               total=explSrc.getValue()/(1+ESProductScript.getProdWeight());
//               factor5=String.valueOf(explSrc.getValue()-total);
//            }else {
//                total=(explSrc.getValue()-1000)/(1+ESProductScript.getProdWeight());
//                factor5=String.valueOf(explSrc.getValue()-total-1000);
//            }
//        }else{
//        	if( ignoreProductTag ){
//        		//存在忽略联营和自营排序的字段,且该字段的值为y,那么当产品是联营时,忽略联营这自营的排序处理
//        		if(total<1000){
//                    total=explSrc.getValue()/(1+ESProductScript.getProdWeight());
//                    factor5=String.valueOf(explSrc.getValue()-total);
//                 }else {
//                     total=(explSrc.getValue()-1000)/(1+ESProductScript.getProdWeight());
//                     factor5=String.valueOf(explSrc.getValue()-total-1000);
//                 }
//        	}else if(isMarketTag){
//        		if(total<1000){
//                    total=explSrc.getValue()/(1+ESProductScript.getMarketTaWight());
//                    factor5=String.valueOf(explSrc.getValue()-total);
//                 }else {
//                     total=(explSrc.getValue()-1000)/(1+ESProductScript.getMarketTaWight());
//                     factor5=String.valueOf(explSrc.getValue()-total-1000);
//                 }
//        	}
//
//        }
//        
//        if(isMakeup){
//    		 explSrc.addDetail(new Explanation(10,new StringBuffer().append("the makeup data sorce sum is 10!!!").toString()));
//    	}else{
//    		 float catScore=explSrc.getValue()-Float.valueOf(ESProductScript.sum(factor1, factor2,factor3,factor4,factor5));
//             
//             Explanation expWeight=new Explanation(Float.valueOf(String.valueOf(factor1)),new StringBuffer().append(" Math.sqrt(mul(weight=").append(weight).append(",").append(weightFactor).append(")").toString());
//             Explanation expstock=new Explanation(Float.valueOf(factor2),new StringBuffer().append(" mul(stock=").append(stock).append(",").append(stockFactor).append(")").toString());
//             Explanation exppromoScore=new Explanation(Float.valueOf(factor3),new StringBuffer().append(" mul(promoScore=").append(promoScore).append(",").append(promoScoreFactor).append(")").toString());
//             Explanation expsalesVolume=new Explanation(Float.valueOf(factor4),new StringBuffer().append(" div(").append("areasalesVolume=").append(areasalesVolume).append("").append(",").append(salesVolumeFactor).append(")").toString());
//             Explanation productTagScore=new Explanation(Float.valueOf(factor5),new StringBuffer().append(" mul(nostock=").append(total).append(",").append(ESProductScript.getProdWeight()).append(")").toString());
//             explSrc.addDetail(expWeight);
//             explSrc.addDetail(expstock);
//             explSrc.addDetail(exppromoScore);
//             
//             if(productTag==1){
//                 explSrc.addDetail(productTagScore); 
//             }else{
//             	//存在忽略联营和自营排序的字段,且该字段的值为y,那么当产品是联营时,忽略联营这自营的排序处理
//             	if( ignoreProductTag ){
//             		Explanation ignoreProductTagScore=new Explanation(Float.valueOf(factor5),new StringBuffer().append(" ignore productTag process mul(nostock=").append(total).append(",").append(ESProductScript.getProdWeight()).append(")").toString());
//             		explSrc.addDetail(ignoreProductTagScore); 
//             	}else if(isMarketTag){
//             		//如果是海外购商品，那么要进行一个加权
//             		Explanation maketTagScore=new Explanation(Float.valueOf(factor5),new StringBuffer().append(" market product, process mul(nostock=").append(total).append(",").append(ESProductScript.getMarketTaWight()).append(")").toString());
//                    explSrc.addDetail(maketTagScore);
//             	}
//             }
//             
//             if(areasalesVolume>0){
//                 explSrc.addDetail(expsalesVolume);
//             }
//             
//             if(catScore>10){
//                 explSrc.addDetail(new Explanation(catScore,new StringBuffer().append(" categorieScore ,hotCate:"+catIds).toString()));
//             }
//    	}
//   
//    }
//    private void addExplainDetail(double weight,int weightFactor,int stock,int stockFactor,double promoScore ,double promoScoreFactor,long areasalesVolume,int salesVolumeFactor,Explanation explSrc){
//        
//        double factor1=Math.sqrt(ESProductScript.div(weight, weightFactor));
//        String factor2=ESProductScript.mul(stock, stockFactor);
//        String factor3=ESProductScript.mul(promoScore, promoScoreFactor);
////        String factor4=ESProductScript.divl(areasalesVolume, salesVolumeFactor);
////        float catScore=explSrc.getValue()-Float.valueOf(ESProductScript.sum(factor1, factor2,factor3,factor4));
//        
//        Explanation expWeight=new Explanation(Float.valueOf(String.valueOf(factor1)),new StringBuffer().append(" Math.sqrt(div(weight=").append(weight).append(",").append(weightFactor).append(")").toString());
//        Explanation expstock=new Explanation(Float.valueOf(factor2),new StringBuffer().append(" mul(stock=").append(stock).append(",").append(stockFactor).append(")").toString());
//        Explanation exppromoScore=new Explanation(Float.valueOf(factor3),new StringBuffer().append(" mul(promoScore=").append(promoScore).append(",").append(promoScoreFactor).append(")").toString());
////        Explanation expsalesVolume=new Explanation(Float.valueOf(factor4),new StringBuffer().append(" div(").append("areasalesVolume=").append(areasalesVolume).append("").append(",").append(salesVolumeFactor).append(")").toString());
//        
//        explSrc.addDetail(expWeight);
//        explSrc.addDetail(expstock);
//        explSrc.addDetail(exppromoScore);
////        if(areasalesVolume>0){
////            explSrc.addDetail(expsalesVolume);
////            }
////        if(catScore>10){
////            explSrc.addDetail(new Explanation(catScore,new StringBuffer().append(" categorieScore ,hotCate:"+catIds).toString()));
////            }
//       
//}
//    
//    static class CustomBoostFactorScorer extends Scorer {
//
//        private final float subQueryBoost;
//        private final Scorer scorer;
//        private final ScoreFunction function;
//        private final float maxBoost;
//        private final CombineFunction scoreCombiner;
//
//        private CustomBoostFactorScorer(CustomBoostFactorWeight w, Scorer scorer, ScoreFunction function, float maxBoost, CombineFunction scoreCombiner)
//                throws IOException {
//            super(w);
//            this.subQueryBoost = w.getQuery().getBoost();
//            this.scorer = scorer;
//            this.function = function;
//            this.maxBoost = maxBoost;
//            this.scoreCombiner = scoreCombiner;
//        }
//
//        @Override
//        public int docID() {
//            return scorer.docID();
//        }
//
//        @Override
//        public int advance(int target) throws IOException {
//            return scorer.advance(target);
//        }
//
//        @Override
//        public int nextDoc() throws IOException {
//            return scorer.nextDoc();
//        }
//
//        @Override
//        public float score() throws IOException {
//            float score = scorer.score();
//            return scoreCombiner.combine(subQueryBoost, score,
//                    function.score(scorer.docID(), score), maxBoost);
//        }
//
//        @Override
//        public int freq() throws IOException {
//            return scorer.freq();
//        }
//
//        @Override
//        public long cost() {
//            return scorer.cost();
//        }
//    }
//
//    public String toString(String field) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("function score (").append(subQuery.toString(field)).append(",function=").append(function).append(')');
//        sb.append(ToStringUtils.boost(getBoost()));
//        return sb.toString();
//    }
//
//    public boolean equals(Object o) {
//        if (o == null || getClass() != o.getClass())
//            return false;
//        CustomFunctionScoreQuery other = (CustomFunctionScoreQuery) o;
//        return this.getBoost() == other.getBoost() && this.subQuery.equals(other.subQuery) && this.function.equals(other.function)
//                && this.maxBoost == other.maxBoost;
//    }
//
//    public int hashCode() {
//        return subQuery.hashCode() + 31 * function.hashCode() ^ Float.floatToIntBits(getBoost());
//    }
//}
