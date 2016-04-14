package org.es.plugin.product;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.script.ScriptModule;

public class NativeScriptPlugin extends AbstractPlugin{

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return "native-script";
	}

	@Override
	public String description() {
		// TODO Auto-generated method stub
		return "native scripts";
	}
	public void onModule(ScriptModule module){
		module.registerScript("native-script", JyhBaseScriptFactory.class);
	}
}
