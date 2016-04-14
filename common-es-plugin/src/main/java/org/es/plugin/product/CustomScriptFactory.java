package org.es.plugin.product;

import java.util.Map;

import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

public class CustomScriptFactory implements NativeScriptFactory{

	@Override
	public ExecutableScript newScript(Map<String, Object> params) {
		// TODO Auto-generated method stub
		return new CustomScript(params);
	}

}
