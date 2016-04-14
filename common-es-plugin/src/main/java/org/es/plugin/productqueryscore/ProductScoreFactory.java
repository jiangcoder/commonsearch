package org.es.plugin.productqueryscore;

import java.util.Map;

import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

public class ProductScoreFactory implements NativeScriptFactory{

	@Override
	public ExecutableScript newScript(Map<String, Object> params) {
		// TODO Auto-generated method stub
		return new ProductScoreScript(params);
	}

}
