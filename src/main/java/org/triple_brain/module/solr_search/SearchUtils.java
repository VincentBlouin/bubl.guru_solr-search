/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.solr_search;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

public class SearchUtils {
    private CoreContainer coreContainer;

    public static SearchUtils usingCoreCoreContainer(CoreContainer coreContainer){
        return new SearchUtils(coreContainer);
    }

    private SearchUtils(CoreContainer coreContainer){
        this.coreContainer = coreContainer;
    }
    public SolrServer getServer(){
        return new EmbeddedSolrServer(
                coreContainer,
                "triple_brain"
        );
    }

    public void close() {
        coreContainer.shutdown();
    }
}
