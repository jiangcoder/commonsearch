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
//import org.elasticsearch.common.inject.Module;
//import org.elasticsearch.index.query.IndexQueryParserModule;
//import org.elasticsearch.plugins.AbstractPlugin;
//import org.elasticsearch.script.ScriptModule;
//
//public class CustomFuntionScoreQueryPlugin extends AbstractPlugin{
//    @Override
//    public String name() {
//        // TODO Auto-generated method stub
//        return "custom-functionquery";
//    }
//
//    @Override
//    public String description() {
//        // TODO Auto-generated method stub
//        return "custom-functionquery";
//    }
//    
//    @Override public void processModule(Module module) {
//        if(module instanceof IndexQueryParserModule){
//            ((IndexQueryParserModule) module).addQueryParser("custom_functionquery", CustomFunctionScoreQueryParser.class);
//        }
//        if (module instanceof ScriptModule ) {
//            ((ScriptModule) module).registerScript("esproduct",ESProductNativeScriptFactory.class );
//        }
//    }
//}
