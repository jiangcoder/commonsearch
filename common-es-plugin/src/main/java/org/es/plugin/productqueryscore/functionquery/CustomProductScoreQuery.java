package org.es.plugin.productqueryscore.functionquery;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.ScoreFunction;
import org.es.plugin.productqueryscore.ESProductScriptConst;

public class CustomProductScoreQuery extends Query{
	final static String path="sku";
	Query subQuery;
	final ScoreFunction function;
	 CombineFunction combineFunction;
	 boolean gomeSortAsc=false;
	 int gomeSortType=0;
	 float maxBoost = Float.MAX_VALUE;
	public  CustomProductScoreQuery(Query subQuery, ScoreFunction function) {
		this.subQuery=subQuery;
		this.function=function;
		this.combineFunction=function.getDefaultScoreCombiner();
	}
	public int setGomeSortType(int gomeSortType) {
        return this.gomeSortType = gomeSortType;
    }


    public Boolean setGomeSortAsc(boolean gomeSortAsc) {
        return this.gomeSortAsc = gomeSortAsc;
    }
    
    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        Query newQ = subQuery.rewrite(reader);
        if (newQ == subQuery) {
            return this;
        }
        CustomProductScoreQuery bq = (CustomProductScoreQuery) this.clone();
        bq.subQuery = newQ;
        return bq;
    }

    @Override
    public void extractTerms(Set<Term> terms) {
        subQuery.extractTerms(terms);
    }

    @Override
    public Weight createWeight(IndexSearcher searcher) throws IOException {
        Weight subQueryWeight = subQuery.createWeight(searcher);
        return new CustomBoostFactorWeight(subQueryWeight);
    }
    class CustomBoostFactorWeight extends Weight {

        final Weight subQueryWeight;

        public CustomBoostFactorWeight(Weight subQueryWeight) throws IOException {
            this.subQueryWeight = subQueryWeight;
        }

        public Query getQuery() {
            return CustomProductScoreQuery.this;
        }

        @Override
        public float getValueForNormalization() throws IOException {
            float sum = subQueryWeight.getValueForNormalization();
            sum *= getBoost() * getBoost();
            return sum;
        }

        @Override
        public void normalize(float norm, float topLevelBoost) {
            subQueryWeight.normalize(norm, topLevelBoost * getBoost());
        }

        @Override
        public Scorer scorer(AtomicReaderContext context, Bits acceptDocs) throws IOException {
            // we ignore scoreDocsInOrder parameter, because we need to score in
            // order if documents are scored with a script. The
            // ShardLookup depends on in order scoring.
            Scorer subQueryScorer = subQueryWeight.scorer(context, acceptDocs);
            if (subQueryScorer == null) {
                return null;
            }
            function.setNextReader(context);
            return new CustomBoostFactorScorer(this, subQueryScorer, function, maxBoost, combineFunction);
        }

        @Override
        public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
            Explanation subQueryExpl = subQueryWeight.explain(context, doc);
            if (!subQueryExpl.isMatch()) {
                return subQueryExpl;
            }
            java.util.List<IndexableField> fieldss=context.reader().document(doc).getFields();
            double promoScore=0;
            String skuNo="";
            double weight=0;
//            long salesVolume=0;
            long areaSalesVolume=0;
            long productTag=0;
            boolean ignoreProductTag = false;
            
            //是否是海外购商品
            boolean isMakeup = false;
            //是否是价格补差商品
            boolean isMarketTag = false;
            
            for(IndexableField indexField:fieldss){
            
            	if(indexField.name().equals(path+"."+"promoScore")){
            		promoScore=indexField.numericValue().doubleValue();
            	}
            	
            	if(indexField.name().equals(path+"."+"skuNo")){
                  skuNo=indexField.stringValue();
                }
            	
            	if(indexField.name().equals(path+"."+"weight")){
                  weight=indexField.numericValue().doubleValue();
                }
              
            	if(indexField.name().equals(path+"."+"productTag")){
                  productTag=indexField.numericValue().longValue();
            	}
              
            	if(indexField.name().equals(path+"."+ESProductScriptConst.FIELD_IGNOREPRODUCTTag)){
            	  if(!indexField.stringValue().isEmpty() && indexField.stringValue().equals(ESProductScriptConst.FIELD_IGNOREPRODUCTTag_VALUE_Y) ){
            		  ignoreProductTag=true; 
            	  }
            	}
            	  
            	if(indexField.name().equals(path+"."+ESProductScriptConst.FIELD_MAKEUP_FLAG_STORE)){
            	  if(indexField.numericValue()!=null && indexField.numericValue().equals(1) ){
            		  isMakeup=true;
            	  }
            	}
            	  
        	    if(indexField.name().equals(path+"."+ESProductScriptConst.FIELD_MARKETTAG_STORE)){
            	  if(indexField.numericValue()!=null && indexField.numericValue().equals(1) ){
            		  isMarketTag=true;
            	  }
            	}   
              
            }
                  
            function.setNextReader(context);
            Explanation functionExplanation = function.explainScore(doc, subQueryExpl);
            
            Explanation functionExplanation1 =new Explanation();
            functionExplanation1.setValue(functionExplanation.getValue());
            functionExplanation1.setDescription(functionExplanation.getDescription());
            
           
            return combineFunction.explain(getBoost(), subQueryExpl, functionExplanation1, maxBoost);
        }
    }
    static class CustomBoostFactorScorer extends Scorer {

        private final float subQueryBoost;
        private final Scorer scorer;
        private final ScoreFunction function;
        private final float maxBoost;
        private final CombineFunction scoreCombiner;
    private CustomBoostFactorScorer(CustomBoostFactorWeight w, Scorer scorer, ScoreFunction function, float maxBoost, CombineFunction scoreCombiner)
            throws IOException {
        super(w);
        this.subQueryBoost = w.getQuery().getBoost();
        this.scorer = scorer;
        this.function = function;
        this.maxBoost = maxBoost;
        this.scoreCombiner = scoreCombiner;
    }

    @Override
    public int docID() {
        return scorer.docID();
    }

    @Override
    public int advance(int target) throws IOException {
        return scorer.advance(target);
    }

    @Override
    public int nextDoc() throws IOException {
        return scorer.nextDoc();
    }

    @Override
    public float score() throws IOException {
        float score = scorer.score();
        return scoreCombiner.combine(subQueryBoost, score,
                function.score(scorer.docID(), score), maxBoost);
    }

    @Override
    public int freq() throws IOException {
        return scorer.freq();
    }

    @Override
    public long cost() {
        return scorer.cost();
    }
}
	@Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("function score (").append(subQuery.toString(field)).append(",function=").append(function).append(')');
        sb.append(ToStringUtils.boost(getBoost()));
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        CustomProductScoreQuery other = (CustomProductScoreQuery) o;
        return this.getBoost() == other.getBoost() && this.subQuery.equals(other.subQuery) && this.function.equals(other.function)
                && this.maxBoost == other.maxBoost;
    }

    public int hashCode() {
        return subQuery.hashCode() + 31 * function.hashCode() ^ Float.floatToIntBits(getBoost());
    }

}
