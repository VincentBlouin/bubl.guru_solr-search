package org.triple_brain.module.solr_search;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.graph.FriendlyResourcePojo;
import org.triple_brain.module.model.graph.GraphElementPojo;
import org.triple_brain.module.model.graph.edge.EdgePojo;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphPojo;
import org.triple_brain.module.search.EdgeSearchResult;
import org.triple_brain.module.search.GraphElementSearchResult;
import org.triple_brain.module.search.VertexSearchResult;

import java.util.*;

import static org.triple_brain.module.common_utils.Uris.decodeUrl;

/*
* Copyright Mozilla Public License 1.1
*/
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

    public static List<EdgeSearchResult> edgesSearchResultFromDocuments(SolrDocumentList documentList) {
        List<EdgeSearchResult> edges = new ArrayList<>();
        for (SolrDocument document : documentList) {
            edges.add(edgeSearchResultFromDocument(
                    document
            ));
        }
        return edges;
    }

    public static GraphElementSearchResult graphElementSearchResultFromDocument(SolrDocument document) {
        return document.containsKey("source_vertex_uri") ?
                edgeSearchResultFromDocument(document) :
                vertexSearchResultFromDocument(document);
    }

    private static VertexSearchResult vertexSearchResultFromDocument(SolrDocument document) {
        return new VertexSearchResult(
                graphElementFromDocument(document),
                buildRelationsName(document)
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
                map.containsKey("comment") ? document.get("comment").toString(): "",
                creationDate,
                lastModificationDate
        );
        return new GraphElementPojo(
                friendlyResource,
                buildIdentifications(document)
        );
    }

    private static List<String> buildRelationsName(SolrDocument document) {
        return (ArrayList<String>) document.get("relation_name");
    }

    private static Set<FriendlyResourcePojo> buildIdentifications(SolrDocument document) {
        Set<FriendlyResourcePojo> identifications = new HashSet<>();
        if (!document.containsKey("identification")) {
            return identifications;
        }
        List<String> identificationsUris = (ArrayList<String>) document.get(
                "identification"
        );
        for (String identification : identificationsUris) {
            identifications.add(
                    new FriendlyResourcePojo(
                            Uris.get(decodeUrl(identification))
                    )

            );
        }
        return identifications;
    }
}
