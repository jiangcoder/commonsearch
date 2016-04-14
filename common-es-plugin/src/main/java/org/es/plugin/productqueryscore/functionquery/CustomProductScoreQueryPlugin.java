package org.es.plugin.productqueryscore.functionquery;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.index.query.IndexQueryParserModule;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.script.ScriptModule;
import org.es.plugin.productqueryscore.ProductScoreFactory;

public class CustomProductScoreQueryPlugin extends AbstractPlugin{

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return "jiangtao-customquery";
	}

	@Override
	public String description() {
		// TODO Auto-generated method stub
		return "jiangtao-custom-description";
	}
	@Override
	public void processModule(Module module){
		if(module instanceof IndexQueryParserModule){
			((IndexQueryParserModule)module).addQueryParser("jiangtao-customquery", CustomProductScoreQueryParser.class);
		}
		else if(module instanceof ScriptModule){
			((ScriptModule)module).registerScript("esproduct", ProductScoreFactory.class);
		}
	}
}
