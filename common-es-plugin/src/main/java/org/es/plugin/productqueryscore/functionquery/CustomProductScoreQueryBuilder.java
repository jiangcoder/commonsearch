package org.es.plugin.productqueryscore.functionquery;

import java.io.IOException;
import java.util.ArrayList;

import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.index.query.BoostableQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;

public class CustomProductScoreQueryBuilder extends BaseQueryBuilder
		implements BoostableQueryBuilder<CustomProductScoreQueryBuilder> {
	private final QueryBuilder queryBuilder;

	private final FilterBuilder filterBuilder;

	private Float boost;

	private Float maxBoost;

	private String scoreMode;

	private String boostMode;
	private String regionId;
	private Boolean fake;
	private String catIds;
	private String sCityId;

	private int gomeSortType = 0;
	private boolean gomeSortAsc = false;

	private ArrayList<FilterBuilder> filters = new ArrayList<FilterBuilder>();
	private ArrayList<ScoreFunctionBuilder> scoreFunctions = new ArrayList<ScoreFunctionBuilder>();

	public CustomProductScoreQueryBuilder(QueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
		this.filterBuilder = null;
	}

	public CustomProductScoreQueryBuilder(FilterBuilder filterBuilder) {
		this.queryBuilder = null;
		this.filterBuilder = filterBuilder;
	}

	public CustomProductScoreQueryBuilder() {
		this.filterBuilder = null;
		this.queryBuilder = null;
	}

	public CustomProductScoreQueryBuilder(ScoreFunctionBuilder scoreFunctionBuilder) {
        queryBuilder = null;
        filterBuilder = null;
        this.filters.add(null);
        this.scoreFunctions.add(scoreFunctionBuilder);
	}

    public CustomProductScoreQueryBuilder add(FilterBuilder filter, ScoreFunctionBuilder scoreFunctionBuilder) {
        this.filters.add(filter);
        this.scoreFunctions.add(scoreFunctionBuilder);
        return this;
    }

	public CustomProductScoreQueryBuilder add(ScoreFunctionBuilder scoreFunctionBuilder) {
	       this.filters.add(null);
	        this.scoreFunctions.add(scoreFunctionBuilder);
	        return this;
	}

	
	  public CustomProductScoreQueryBuilder scoreMode(String scoreMode) {
	        this.scoreMode = scoreMode;
	        return this;
	    }
	    
	    public CustomProductScoreQueryBuilder boostMode(String boostMode) {
	        this.boostMode = boostMode;
	        return this;
	    }
	    
	    public CustomProductScoreQueryBuilder boostMode(CombineFunction combineFunction) {
	        this.boostMode = combineFunction.getName();
	        return this;
	    }

	    public CustomProductScoreQueryBuilder maxBoost(float maxBoost) {
	        this.maxBoost = maxBoost;
	        return this;
	    }
	    public CustomProductScoreQueryBuilder regionId(String regionId) {
	        this.regionId = regionId;
	        return this;
	    }
	    public CustomProductScoreQueryBuilder cityId(String cityId) {
	        this.sCityId = cityId;
	        return this;
	    }
	    public CustomProductScoreQueryBuilder fake(Boolean fake) {
	        this.fake = fake;
	        return this;
	    }
	    public CustomProductScoreQueryBuilder catIds(String catIds) {
	        this.catIds = catIds;
	        return this;
	    }
	    
	    public CustomProductScoreQueryBuilder gomeSortType(int gomeSortType) {
	        this.gomeSortType = gomeSortType;
	        return this;
	    }
	    public CustomProductScoreQueryBuilder gomeSortAsc(boolean gomeSortAsc) {
	        this.gomeSortAsc = gomeSortAsc;
	        return this;
	    }
	
	@Override
	protected void doXContent(XContentBuilder builder, Params params) throws IOException {
		builder.startObject(CustomProductScoreQueryParser.NAME);
		if (queryBuilder != null) {
			builder.field("query");
			queryBuilder.toXContent(builder, params);
		} else if (filterBuilder != null) {
			builder.field("filter");
			filterBuilder.toXContent(builder, params);
		}
		// If there is only one function without a filter, we later want to
		// create a FunctionScoreQuery.
		// For this, we only build the scoreFunction.Tthis will be translated to
		// FunctionScoreQuery in the parser.
		if (filters.size() == 1 && filters.get(0) == null) {
			scoreFunctions.get(0).toXContent(builder, params);
		} else { // in all other cases we build the format needed for a
					// FiltersFunctionScoreQuery
			builder.startArray("functions");
			for (int i = 0; i < filters.size(); i++) {
				builder.startObject();
				if (filters.get(i) != null) {
					builder.field("filter");
					filters.get(i).toXContent(builder, params);
				}
				scoreFunctions.get(i).toXContent(builder, params);
				builder.endObject();
			}
			builder.endArray();
		}
		if (scoreMode != null) {
			builder.field("score_mode", scoreMode);
		}
		if (boostMode != null) {
			builder.field("boost_mode", boostMode);
		}
		if (maxBoost != null) {
			builder.field("max_boost", maxBoost);
		}
		if (boost != null) {
			builder.field("boost", boost);
		}
		if (regionId != null) {
			builder.field("region_id", regionId);
		}
		if (sCityId != null) {
			builder.field("city_id", sCityId);
		}
		if (fake != null) {
			builder.field("fake", fake);
		}
		if (catIds != null) {
			builder.field("cat_id", catIds);
		}
		builder.field("gomeSortType", gomeSortType);
		builder.field("gomeSortAsc", gomeSortAsc);
		builder.endObject();

	}

	@Override
	public CustomProductScoreQueryBuilder boost(float boost) {
		this.boost = boost;
		return this;
	}

}
