//package org.es.plugin.functionquery.nativeplugin;
//
//import org.elasticsearch.action.ActionModule;
//import org.elasticsearch.common.inject.Module;
//import org.elasticsearch.plugins.AbstractPlugin;
//import org.elasticsearch.script.ScriptModule;
///**
// * ScriptPlugin for product
// * @author dinghb
// *
// */
//
//public class ProductScriptPlugin extends AbstractPlugin {
//
//    @Override public String name() {
//        return "product-script";
//    }
//
//
//    @Override public String description() {
//        return "product-script";
//    }
//
//
//    @Override public void processModule(Module module) {
//        if (module instanceof ScriptModule ) {
//        	((ScriptModule) module).registerScript("esproduct",ESProductNativeScriptFactory.class );
//        }
////        if (module instanceof ActionModule) {
////     	   newDragonDict.init();
////         ActionModule actionModule = (ActionModule) module;
////         actionModule.registerAction(MemoryStoreAction.INSTANCE, TransportMemoryStoreAction.class);
////        }
//    }
//}
