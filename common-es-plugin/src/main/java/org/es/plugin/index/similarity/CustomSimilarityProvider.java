package org.es.plugin.index.similarity;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.similarity.AbstractSimilarityProvider;
 
/**
 * Simple {@link SimilarityProvider} for a {@link CustomSimilarity}
 * 
 * @author xq
 *
 */
public class CustomSimilarityProvider extends AbstractSimilarityProvider {
 
  private CustomSimilarity similarity;
 
  @Inject
  public CustomSimilarityProvider(@Assisted String name, @Assisted Settings settings) {
    super(name);
    this.similarity = new CustomSimilarity();
  }
 
 
 
  public CustomSimilarity get() {
    return similarity;
  }
}