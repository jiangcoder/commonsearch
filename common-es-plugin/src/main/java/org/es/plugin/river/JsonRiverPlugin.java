//package org.es.plugin.river;
//
//import org.elasticsearch.plugins.AbstractPlugin;
//import org.elasticsearch.river.RiversModule;
//
//public class JsonRiverPlugin extends AbstractPlugin {
//
//    @Override
//    public String name() {
//        return "json";
//    }
//
//    @Override
//    public String description() {
//        return "River Streaming JSON Plugin";
//    }
//
//    public void onModule(RiversModule module) {
//        module.registerRiver("json", JsonRiverModule.class);
//    }
//}
