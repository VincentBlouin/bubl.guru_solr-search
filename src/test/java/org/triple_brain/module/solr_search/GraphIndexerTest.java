package org.triple_brain.module.solr_search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Ignore;
import org.junit.Test;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.search.GraphSearch;
import org.triple_brain.module.search.VertexSearchResult;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.triple_brain.module.common_utils.Uris.encodeURL;

/*
* Copyright Mozilla Public License 1.1
*/

public class GraphIndexerTest extends SearchRelatedTest {

    @Test
    public void can_index_vertex() throws Exception {
        SolrDocumentList documentList = queryVertex(vertexA);
        assertThat(documentList.size(), is(0));
        graphIndexer.indexVertex(vertexA);
        graphIndexer.commit();
        documentList = queryVertex(vertexA);
        assertThat(documentList.size(), is(1));
        assertThat(
                labelOfGraphElementSearchResult(documentList.get(0)),
                is("vertex Azure")
        );
    }

    @Test
    public void can_remove_graph_element_from_index() {
        indexGraph();
        GraphSearch graphSearch = SolrGraphSearch.withCoreContainer(coreContainer);
        List<VertexSearchResult> results = graphSearch.searchOwnVerticesAndPublicOnesForAutoCompletionByLabel(
                "vertex azure",
                user
        );
        assertThat(results.size(), is(1));
        graphIndexer.deleteGraphElement(vertexA);
        graphIndexer.commit();
        results = graphSearch.searchOwnVerticesAndPublicOnesForAutoCompletionByLabel(
                "vertex azure",
                user
        );
        assertThat(results.size(), is(0));
    }

    @Test
    public void updating_edge_label_updates_connected_vertices_relations_name() throws Exception {
        indexGraph();
        assertFalse(
                relationsNameOfVertex(vertexA).contains(
                        "updated label"
                )
        );
        assertFalse(
                relationsNameOfVertex(vertexB).contains(
                        "updated label"
                )
        );
        EdgeOperator edge = vertexA.connectedEdges().iterator().next();
        edge.label("updated label");
        graphIndexer.handleEdgeLabelUpdated(edge);
        graphIndexer.commit();
        assertTrue(
            relationsNameOfVertex(vertexA).contains(
                    "updated label"
            )
        );
        assertTrue(
            relationsNameOfVertex(vertexB).contains(
                    "updated label"
            )
        );
    }

    @Test
    @Ignore(
            "todo when lucene4 be integrated in noe4j " +
                    "for now it conflicts with solr " +
                    "when I upgrade solr to version 4"
    )
    public void indexing_graph_element_doesnt_erase_vertex_specific_fields() {
        indexGraph();
        GraphSearch graphSearch = SolrGraphSearch.withCoreContainer(coreContainer);
        List<VertexSearchResult> vertexASearchResults = graphSearch.searchOnlyForOwnVerticesForAutoCompletionByLabel(
                "vertex Azure",
                user
        );
        assertThat(
                vertexASearchResults.size(), is(1)
        );
        //todo uncomment when lucene4 be integrated in noe4j for now it conflicts with solr when I upgrade solr to version 4
//        graphIndexer().updateGraphElementIndex(vertexA, user);
        vertexASearchResults = graphSearch.searchOnlyForOwnVerticesForAutoCompletionByLabel(
                "vertex Azure",
                user
        );
        assertThat(
                vertexASearchResults.size(), is(1)
        );
    }

    private String labelOfGraphElementSearchResult(SolrDocument solrDocument) {
        return (String) solrDocument.getFieldValue("label");
    }

    private SolrDocumentList queryVertex(Vertex vertex) throws Exception {
        return resultsOfSearchQuery(
                new SolrQuery().setQuery(
                        "uri:" + encodeURL(vertex.uri().toString())
                )
        );
    }

    private List<String> relationsNameOfVertex(Vertex vertex) {
        List<VertexSearchResult> searchResults = graphSearch.searchOnlyForOwnVerticesForAutoCompletionByLabel(
                vertex.label(),
                user
        );
        VertexSearchResult result = searchResults.iterator().next();
        return result.getRelationsName();
    }


}