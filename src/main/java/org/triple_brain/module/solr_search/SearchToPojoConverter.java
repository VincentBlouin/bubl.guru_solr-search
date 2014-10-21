/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.solr_search;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.codehaus.jettison.json.JSONObject;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.graph.FriendlyResourcePojo;
import org.triple_brain.module.model.graph.GraphElementPojo;
import org.triple_brain.module.model.graph.Identification;
import org.triple_brain.module.model.graph.IdentificationPojo;
import org.triple_brain.module.model.graph.edge.EdgePojo;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphPojo;
import org.triple_brain.module.model.json.IdentificationJson;
import org.triple_brain.module.model.json.graph.GraphElementJson;
import org.triple_brain.module.search.EdgeSearchResult;
import org.triple_brain.module.search.GraphElementSearchResult;
import org.triple_brain.module.search.VertexSearchResult;

import java.net.URI;
import java.util.*;

import static org.triple_brain.module.common_utils.Uris.decodeUrl;

public class SearchToPojoConverter {

    public static List<VertexSearchResult> verticesSearchResultFromDocuments(SolrDocumentList documentList) {
        List<VertexSearchResult> vertices = new ArrayList<>();
        for (SolrDocument document : documentList) {
            vertices.add(vertexSearchResultFromDocument(
                    document
            ));
        }
        return vertices;
    }

    public static List<GraphElementSearchResult> edgesSearchResultFromDocuments(SolrDocumentList documentList) {
        List<GraphElementSearchResult> results = new ArrayList<>();
        for (SolrDocument document : documentList) {
            results.add(graphElementSearchResultFromDocument(
                    document
            ));
        }
        return results;
    }

    public static GraphElementSearchResult graphElementSearchResultFromDocument(SolrDocument document) {
        return document.containsKey("source_vertex_uri") ?
                edgeSearchResultFromDocument(document) :
                vertexSearchResultFromDocument(document);
    }

    private static VertexSearchResult vertexSearchResultFromDocument(SolrDocument document) {
        return new VertexSearchResult(
                graphElementFromDocument(document),
                buildProperties(document)
        );
    }

    private static EdgeSearchResult edgeSearchResultFromDocument(SolrDocument document) {
        VertexInSubGraphPojo sourceVertex = new VertexInSubGraphPojo(
                new FriendlyResourcePojo(
                        Uris.get(decodeUrl(
                                (String) document.get("source_vertex_uri")
                        ))
                )
        );
        VertexInSubGraphPojo destinationVertex = new VertexInSubGraphPojo(
                new FriendlyResourcePojo(
                        Uris.get(decodeUrl(
                                (String) document.get("destination_vertex_uri")
                        ))
                )
        );
        EdgePojo edge = new EdgePojo(
                graphElementFromDocument(document),
                sourceVertex,
                destinationVertex
        );
        return new EdgeSearchResult(
                edge
        );
    }

    private static GraphElementPojo graphElementFromDocument(SolrDocument document) {
        Date creationDate = null;
        Date lastModificationDate = null;
        Map<String, Object> map = document.getFieldValueMap();
        FriendlyResourcePojo friendlyResource = new FriendlyResourcePojo(
                Uris.get(decodeUrl((String) document.get("uri"))),
                map.containsKey("label") ? document.get("label").toString() : "",
                new HashSet<Image>(),
                map.containsKey("comment") ? document.get("comment").toString() : "",
                creationDate,
                lastModificationDate
        );
        return new GraphElementPojo(
                friendlyResource,
                buildIdentifications(document)
        );
    }

    private static Map<URI, GraphElementPojo> buildProperties(SolrDocument document) {
        return GraphElementJson.fromJsonObjectToMap(
                (String) document.get("properties")
        );
    }

    private static Map<URI, IdentificationPojo> buildIdentifications(SolrDocument document) {
        Map<URI, IdentificationPojo> identifications = new HashMap<>();
        if (!document.containsKey("identification")) {
            return identifications;
        }
        List<String> identificationsUris = (ArrayList<String>) document.get(
                "identification"
        );
        for (String identification : identificationsUris) {
            URI uri = Uris.get(decodeUrl(identification));
            identifications.put(
                    uri,
                    new IdentificationPojo(
                            new FriendlyResourcePojo(
                                    uri
                            )
                    )
            );
        }
        return identifications;
    }
}
