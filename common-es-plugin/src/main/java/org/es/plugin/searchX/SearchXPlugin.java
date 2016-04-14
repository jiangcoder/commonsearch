package org.es.plugin.searchX;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;
/**
 * ScriptPlugin for product
 * @author dinghb
 *
 */

public class SearchXPlugin extends AbstractPlugin {

    @Override public String name() {
        return "search-x";
    }


    @Override public String description() {
        return "search-x";
    }


    @Override public void processModule(Module module) {
    	 if (module instanceof ActionModule) {
             ActionModule actionModule = (ActionModule) module;
             actionModule. registerAction(SearchActionX.INSTANCE, TransportSearchActionX.class);
            }
    }
    
	public Settings additionalSettings() {
		return ImmutableSettings.settingsBuilder().classLoader(SearchXPlugin.class.getClassLoader()).build();
	}
}
