package org.es.plugin.productqueryscore;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.script.ScriptModule;

public class ProductScorePlugin extends AbstractPlugin{

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return "jiangtao-name";
	}

	@Override
	public String description() {
		// TODO Auto-generated method stub
		return "jiangtao description";
	}
	@Override
	public void processModule(Module module) {
		if(module instanceof ScriptModule){
			((ScriptModule)module).registerScript("esproduct", ProductScoreFactory.class);
		}
	}
    public Settings additionalSettings() {
		return ImmutableSettings.settingsBuilder().classLoader(ProductScorePlugin.class.getClassLoader()).build();
	}
}
