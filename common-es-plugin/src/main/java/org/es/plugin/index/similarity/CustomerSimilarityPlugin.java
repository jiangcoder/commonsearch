package org.es.plugin.index.similarity;

import org.elasticsearch.index.similarity.SimilarityModule;
import org.elasticsearch.index.similarity.SimilarityProvider;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.common.inject.Module;
public class CustomerSimilarityPlugin extends AbstractPlugin {
 
  @Override public String name() {
    return "customer-jiangtao";
  }
 
 
  @Override public String description() {
    return "customer description";
  }
 
 
  @Override public void processModule(Module module) {
    if (module instanceof SimilarityModule) {
      SimilarityModule similarityModule = (SimilarityModule) module;
      similarityModule.addSimilarity("customer-jiangtao", CustomSimilarityProvider.class);
    }
  }
}