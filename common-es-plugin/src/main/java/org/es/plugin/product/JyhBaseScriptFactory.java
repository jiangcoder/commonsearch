package org.es.plugin.product;

import java.util.Map;

import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import com.sun.istack.internal.Nullable;

public class JyhBaseScriptFactory implements NativeScriptFactory{

	@Override
	public ExecutableScript newScript(@Nullable Map<String, Object> params) {
		// TODO Auto-generated method stub
		return new CustomScript(params);
	}
}
