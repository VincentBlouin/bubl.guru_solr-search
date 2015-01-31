/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.solr_search.neo4j;

import com.google.inject.AbstractModule;
import org.triple_brain.module.search.GraphIndexer;
import org.triple_brain.module.search.GraphSearch;

public class Neo4jSearchModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(GraphIndexer.class).to(Neo4jGraphIndexer.class);
        bind(GraphSearch.class).to(Neo4jGraphSearch.class);
    }
}
