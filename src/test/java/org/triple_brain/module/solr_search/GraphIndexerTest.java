package org.triple_brain.module.solr_search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.triple_brain.module.common_utils.JsonUtils;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;
import org.triple_brain.module.search.GraphSearch;
import org.triple_brain.module.solr_search.json.SearchJsonConverter;

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
        JSONArray results = graphSearch.searchOwnVerticesAndPublicOnesForAutoCompletionByLabel(
                "vertex azure",
                user
        );
        assertThat(results.length(), is(1));
        graphIndexer.deleteGraphElement(vertexA);
        graphIndexer.commit();
        results = graphSearch.searchOwnVerticesAndPublicOnesForAutoCompletionByLabel(
                "vertex azure",
                user
        );
        assertThat(results.length(), is(0));
    }

    @Test
    public void updating_edge_label_updates_connected_vertices_relations_name() throws Exception {
        indexGraph();
        assertFalse(JsonUtils.containsString(
                relationsNameOfVertex(vertexA),
                "updated label"
        ));
        assertFalse(JsonUtils.containsString(
                relationsNameOfVertex(vertexB),
                "updated label"
        ));
        Edge edge = vertexA.connectedEdges().iterator().next();
        edge.label("updated label");
        graphIndexer.handleEdgeLabelUpdated(edge);
        graphIndexer.commit();
        assertTrue(JsonUtils.containsString(
                relationsNameOfVertex(vertexA),
                "updated label"
        ));
        assertTrue(JsonUtils.containsString(
                relationsNameOfVertex(vertexB),
                "updated label"
        ));
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
        JSONArray vertexASearchResults = graphSearch.searchOnlyForOwnVerticesForAutoCompletionByLabel(
                "vertex Azure",
                user
        );
        assertThat(
                vertexASearchResults.length(), is(1)
        );
        //todo uncomment when lucene4 be integrated in noe4j for now it conflicts with solr when I upgrade solr to version 4
//        graphIndexer().updateGraphElementIndex(vertexA, user);
        vertexASearchResults = graphSearch.searchOnlyForOwnVerticesForAutoCompletionByLabel(
                "vertex Azure",
                user
        );
        assertThat(
                vertexASearchResults.length(), is(1)
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

    private JSONArray relationsNameOfVertex(Vertex vertex) {
        JSONArray searchResults = graphSearch.searchOnlyForOwnVerticesForAutoCompletionByLabel(
                vertex.label(),
                user
        );
        try {
            JSONObject result = searchResults.getJSONObject(0);
            return result.getJSONArray(
                    SearchJsonConverter.RELATIONS_NAME
            );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


}