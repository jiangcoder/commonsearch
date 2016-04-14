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
//import org.apache.log4j.Logger;
//
//import com.google.common.collect.ImmutableMap;
//import com.google.common.collect.ImmutableMap.Builder;
//import org.apache.lucene.search.Filter;
//import org.apache.lucene.search.Query;
//import org.elasticsearch.ElasticsearchParseException;
//import org.elasticsearch.common.Strings;
//import org.elasticsearch.common.inject.Inject;
//import org.elasticsearch.common.lucene.search.Queries;
//import org.elasticsearch.common.lucene.search.XConstantScoreQuery;
//import org.elasticsearch.common.lucene.search.function.CombineFunction;
//import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
//import org.elasticsearch.common.lucene.search.function.ScoreFunction;
//import org.elasticsearch.common.xcontent.XContentParser;
//import org.elasticsearch.index.query.QueryParseContext;
//import org.elasticsearch.index.query.QueryParser;
//import org.elasticsearch.index.query.QueryParsingException;
//import org.elasticsearch.index.query.functionscore.ScoreFunctionParser;
//import org.elasticsearch.index.query.functionscore.ScoreFunctionParserMapper;
//import org.elasticsearch.index.query.functionscore.factor.FactorParser;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//
//@SuppressWarnings("deprecation")
//public class CustomFunctionScoreQueryParser implements QueryParser {
//    
//    private static Logger logger=Logger.getLogger(CustomFunctionScoreQueryParser.class);
//    
//    public static final String NAME = "function_score";
//    ScoreFunctionParserMapper funtionParserMapper;
//    // For better readability of error message
//    static final String MISPLACED_FUNCTION_MESSAGE_PREFIX = "You can either define \"functions\":[...] or a single function, not both. ";
//    static final String MISPLACED_BOOST_FUNCTION_MESSAGE_SUFFIX = " Did you mean \"boost\" instead?";
////    static final float maxBoost = Float.MAX_VALUE;
//    static final float minScore = 0.00f;
//    
//    
//    @Inject
//    public CustomFunctionScoreQueryParser(ScoreFunctionParserMapper funtionParserMapper) {
//        this.funtionParserMapper = funtionParserMapper;
//    }
//
//    @Override
//    public String[] names() {
//        return new String[] { NAME, Strings.toCamelCase(NAME) };
//    }
//    
//    private static final ImmutableMap<String, CombineFunction> combineFunctionsMap;
//
//    static {
//        CombineFunction[] values = CombineFunction.values();
//        Builder<String, CombineFunction> combineFunctionMapBuilder = ImmutableMap.<String, CombineFunction>builder();
//        for (CombineFunction combineFunction : values) {
//            combineFunctionMapBuilder.put(combineFunction.getName(), combineFunction);
//        }
//        combineFunctionsMap = combineFunctionMapBuilder.build();
//    }
//
//
//    @Override
//    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
//        XContentParser parser = parseContext.parser();
//
//        Query query = null;
//        float boost = 1.0f;
//         String regionId=null;
//         String sCityId=null;
//         boolean fake=true;
//        String catIds=null;
//        
//        int gomeSortType=0;
//        boolean gomeSortAsc=false;
//        
//        FiltersFunctionScoreQuery.ScoreMode scoreMode = FiltersFunctionScoreQuery.ScoreMode.Multiply;
//        ArrayList<FiltersFunctionScoreQuery.FilterFunction> filterFunctions = new ArrayList<FiltersFunctionScoreQuery.FilterFunction>();
//        float maxBoost = Float.MAX_VALUE;
//
//        String currentFieldName = null;
//        XContentParser.Token token;
//        CombineFunction combineFunction = CombineFunction.MULT;
//        // Either define array of functions and filters or only one function
//        boolean functionArrayFound = false;
//        boolean singleFunctionFound = false;
//        String singleFunctionName = null;
//
//        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
//            if (token == XContentParser.Token.FIELD_NAME) {
//                currentFieldName = parser.currentName();
//            } else if ("query".equals(currentFieldName)) {
//                query = parseContext.parseInnerQuery();
//            } else if ("filter".equals(currentFieldName)) {
//                query = new XConstantScoreQuery(parseContext.parseInnerFilter());
//            } else if ("score_mode".equals(currentFieldName) || "scoreMode".equals(currentFieldName)) {
//                scoreMode = parseScoreMode(parseContext, parser);
//            } else if ("boost_mode".equals(currentFieldName) || "boostMode".equals(currentFieldName)) {
//                combineFunction = parseBoostMode(parseContext, parser);
//            } else if ("max_boost".equals(currentFieldName) || "maxBoost".equals(currentFieldName)) {
//                maxBoost = parser.floatValue();
//            } else if ("boost".equals(currentFieldName)) {
//                boost = parser.floatValue();
//            } else if ("fake".equals(currentFieldName)) {
//                fake = parser.booleanValue();
//            } else if ("region_id".equals(currentFieldName)) {
//                try {
//                    regionId = parser.bytes().utf8ToString();
//                } catch (Exception e) {
////                   e.printStackTrace();
//                    logger.info(e);
//                }
//            }else if ("city_id".equals(currentFieldName)) {
//                try {
//                    sCityId = parser.bytes().utf8ToString();
//                } catch (Exception e) {
////                   e.printStackTrace();
//                    logger.info(e);
//                }
//            }else if("cat_id".equals(currentFieldName)){
//                try {
//                    catIds=parser.bytes().utf8ToString();
//                } catch (Exception e) {
////                   e.printStackTrace();
//                    logger.info(e);
//                }
//            }else if("gomeSortType".equals(currentFieldName)){
//                try {
//                    gomeSortType=parser.intValue();
//                } catch (Exception e) {
////                   e.printStackTrace();
//                    logger.info(e);
//                }
//                 
//                
//            }else if("gomeSortAsc".equals(currentFieldName)){
//                
//                try {
//                    gomeSortAsc=parser.booleanValue();
//                } catch (Exception e) {
////                   e.printStackTrace();
//                    logger.info(e);
//                }
//            
//           }else if ("functions".equals(currentFieldName)) {
//                if (singleFunctionFound) {
//                    String errorString = "Found \"" + singleFunctionName + "\" already, now encountering \"functions\": [...].";
//                    handleMisplacedFunctionsDeclaration(errorString, singleFunctionName);
//                }
//                currentFieldName = parseFiltersAndFunctions(parseContext, parser, filterFunctions, currentFieldName);
//                functionArrayFound = true;
//            } else {
//                // we try to parse a score function. If there is no score
//                // function for the current field name,
//                // functionParserMapper.get() will throw an Exception.
//                ScoreFunctionParser currentFunctionParser = funtionParserMapper.get(parseContext.index(), currentFieldName);
//                singleFunctionName = currentFieldName;
//                if (functionArrayFound) {
//                    String errorString = "Found \"functions\": [...] already, now encountering \"" + currentFieldName + "\".";
//                    handleMisplacedFunctionsDeclaration(errorString, currentFieldName);
//                }
//                filterFunctions.add(new FiltersFunctionScoreQuery.FilterFunction(null, currentFunctionParser.parse(parseContext, parser)));
//                singleFunctionFound = true;
//            }
//        }
//        if (query == null) {
//            query = Queries.newMatchAllQuery();
//        }
//        // if all filter elements returned null, just use the query
//        if (filterFunctions.isEmpty()) {
//            return query;
//        }
//        // handle cases where only one score function and no filter was
//        // provided. In this case we create a FunctionScoreQuery.
//        if (filterFunctions.size() == 1 && filterFunctions.get(0).filter == null) {
//            CustomFunctionScoreQuery theQuery = new CustomFunctionScoreQuery(query, filterFunctions.get(0).function);
//            if (combineFunction != null) {
//                theQuery.setCombineFunction(combineFunction);
//            }
//            theQuery.setBoost(boost);
//            theQuery.setMaxBoost(maxBoost);
//            theQuery.setRegionId(regionId);
//            theQuery.setFake(fake);
//            theQuery.setCatIds(catIds);
//            theQuery.setCityId(sCityId);
//            theQuery.setGomeSortType(gomeSortType);
//            theQuery.setGomeSortAsc(gomeSortAsc);
//            return theQuery;
//            // in all other cases we create a FiltersFunctionScoreQuery.
//        } else {
//            FiltersFunctionScoreQuery functionScoreQuery = new FiltersFunctionScoreQuery(query, scoreMode,filterFunctions.toArray(new FiltersFunctionScoreQuery.FilterFunction[filterFunctions.size()]), maxBoost,minScore);
//
//            if (combineFunction != null) {
//                functionScoreQuery.setCombineFunction(combineFunction);
//            }
//            functionScoreQuery.setBoost(boost);
//            return functionScoreQuery;
//        }
//    }
//
//  
//    private void handleMisplacedFunctionsDeclaration(String errorString, String functionName) {
//        errorString = MISPLACED_FUNCTION_MESSAGE_PREFIX + errorString;
//        if (Arrays.asList(FactorParser.NAMES).contains(functionName)) {
//            errorString = errorString + MISPLACED_BOOST_FUNCTION_MESSAGE_SUFFIX;
//        }
//        throw new ElasticsearchParseException(errorString);
//    }
//
//    private String parseFiltersAndFunctions(QueryParseContext parseContext, XContentParser parser,
//            ArrayList<FiltersFunctionScoreQuery.FilterFunction> filterFunctions, String currentFieldName) throws IOException {
//        XContentParser.Token token;
//        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
//            Filter filter = null;
//            ScoreFunction scoreFunction = null;
//            if (token != XContentParser.Token.START_OBJECT) {
//                throw new QueryParsingException(parseContext.index(), NAME + ": malformed query, expected a "
//                        + XContentParser.Token.START_OBJECT + " while parsing functions but got a " + token);
//            } else {
//                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
//                    if (token == XContentParser.Token.FIELD_NAME) {
//                        currentFieldName = parser.currentName();
//                    } else {
//                        if ("filter".equals(currentFieldName)) {
//                            filter = parseContext.parseInnerFilter();
//                        } else {
//                            // do not need to check null here,
//                            // funtionParserMapper throws exception if parser
//                            // non-existent
//                            ScoreFunctionParser functionParser = funtionParserMapper.get(parseContext.index(), currentFieldName);
//                            scoreFunction = functionParser.parse(parseContext, parser);
//                        }
//                    }
//                }
//            }
//            if (filter == null) {
//                filter = Queries.MATCH_ALL_FILTER;
//            }
//            if (scoreFunction == null) {
//                throw new ElasticsearchParseException("function_score: One entry in functions list is missing a function.");
//            }
//            filterFunctions.add(new FiltersFunctionScoreQuery.FilterFunction(filter, scoreFunction));
//
//        }
//        return currentFieldName;
//    }
//
//    private FiltersFunctionScoreQuery.ScoreMode parseScoreMode(QueryParseContext parseContext, XContentParser parser) throws IOException {
//        String scoreMode = parser.text();
//        if ("avg".equals(scoreMode)) {
//            return FiltersFunctionScoreQuery.ScoreMode.Avg;
//        } else if ("max".equals(scoreMode)) {
//            return FiltersFunctionScoreQuery.ScoreMode.Max;
//        } else if ("min".equals(scoreMode)) {
//            return FiltersFunctionScoreQuery.ScoreMode.Min;
//        } else if ("sum".equals(scoreMode)) {
//            return FiltersFunctionScoreQuery.ScoreMode.Sum;
//        } else if ("multiply".equals(scoreMode)) {
//            return FiltersFunctionScoreQuery.ScoreMode.Multiply;
//        } else if ("first".equals(scoreMode)) {
//            return FiltersFunctionScoreQuery.ScoreMode.First;
//        } else {
//            throw new QueryParsingException(parseContext.index(), NAME + " illegal score_mode [" + scoreMode + "]");
//        }
//    }
//
//    private CombineFunction parseBoostMode(QueryParseContext parseContext, XContentParser parser) throws IOException {
//        String boostMode = parser.text();
//        CombineFunction cf = combineFunctionsMap.get(boostMode);
//        if (cf == null) {
//            throw new QueryParsingException(parseContext.index(), NAME + " illegal boost_mode [" + boostMode + "]");
//        }
//        return cf;
//    }
//}
