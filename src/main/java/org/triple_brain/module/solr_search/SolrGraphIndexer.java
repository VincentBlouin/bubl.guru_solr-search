package org.triple_brain.module.solr_search;

import com.google.inject.Inject;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.WholeGraph;
import org.triple_brain.module.model.graph.GraphElement;
import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.vertex.VertexOperator;
import org.triple_brain.module.search.GraphIndexer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.triple_brain.module.common_utils.Uris.encodeURL;

/*
* Copyright Mozilla Public License 1.1
*/
public class SolrGraphIndexer implements GraphIndexer {
    @Inject
    WholeGraph wholeGraph;

    private int INDEX_AFTER_HOW_NB_DOCUMENTS = 100;
    private CoreContainer coreContainer;
    private SearchUtils searchUtils;

    public static SolrGraphIndexer withCoreContainer(CoreContainer coreContainer) {
        return new SolrGraphIndexer(coreContainer);
    }

    private SolrGraphIndexer(CoreContainer coreContainer) {
        this.coreContainer = coreContainer;
        this.searchUtils = SearchUtils.usingCoreCoreContainer(coreContainer);
    }

    @Override
    public void indexWholeGraph() {
        indexAllVertices();
        indexAllEdges();
    }

    @Override
    public void indexVertex(VertexOperator vertex) {
        addDocument(
                vertexDocument(vertex)
        );
    }

    @Override
    public void indexRelation(Edge edge) {
        addDocument(
                edgeDocument(edge)
        );
    }

    @Override
    public void deleteGraphElement(GraphElement graphElement) {
        try {
            SolrServer solrServer = searchUtils.getServer();
            solrServer.deleteByQuery(
                    "uri:" + encodeURL(
                            graphElement.uri()
                    )
            );
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleEdgeLabelUpdated(Edge edge) {
        Set<SolrInputDocument> updatedDocuments = new HashSet<>();
        updatedDocuments.add(
                vertexDocument(
                        (VertexOperator) edge.sourceVertex()
                )
        );
        updatedDocuments.add(
                vertexDocument(
                        (VertexOperator) edge.destinationVertex()
                )
        );
        updatedDocuments.add(
                edgeDocument(
                        edge
                )
        );
        addDocuments(
                updatedDocuments
        );
    }

    @Override
    public void commit() {
        try {
            searchUtils.getServer().commit();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SolrInputDocument edgeDocument(Edge edge) {
        SolrInputDocument document = graphElementToDocument(edge);
        document.addField("is_vertex", false);
        document.addField(
                "source_vertex_uri",
                encodeURL(edge.sourceVertex().uri())
        );
        document.addField(
                "destination_vertex_uri",
                encodeURL(edge.destinationVertex().uri())
        );
        return document;
    }

    private SolrInputDocument vertexDocument(VertexOperator vertex) {
        SolrInputDocument document = graphElementToDocument(vertex);
        document.addField("is_vertex", true);
        document.addField("is_public", vertex.isPublic());
        document.addField("comment", vertex.comment());
        for (Edge edge : vertex.connectedEdges()) {
            document.addField(
                    "relation_name",
                    edge.label()
            );
        }
        return document;
    }

    private void indexAllVertices() {
        Iterator<VertexOperator> vertexIt = wholeGraph.getAllVertices();
        Set<SolrInputDocument> vertexDocuments = new HashSet<>();
        int totalIndexed = 0;
        int nbInCycle = 0;
        while (vertexIt.hasNext()) {
            vertexDocuments.add(
                    vertexDocument(vertexIt.next())
            );
            nbInCycle++;
            if (nbInCycle == INDEX_AFTER_HOW_NB_DOCUMENTS) {
                totalIndexed += INDEX_AFTER_HOW_NB_DOCUMENTS;
                addDocuments(vertexDocuments);
                commit();
                System.out.println(
                        "Indexing vertices ... total indexed yet " + totalIndexed
                );
                nbInCycle = 0;
                vertexDocuments = new HashSet<>();
            }
        }
        addDocuments(vertexDocuments);
        commit();
        totalIndexed += nbInCycle;
        System.out.println(
                "Indexing vertices ... total indexed " + totalIndexed
        );
    }

    private void indexAllEdges() {
        Iterator<EdgeOperator> edgeIt = wholeGraph.getAllEdges();
        Set<SolrInputDocument> edgesDocument = new HashSet<>();
        int totalIndexed = 0;
        int nbInCycle = 0;
        while (edgeIt.hasNext()) {
            edgesDocument.add(
                    edgeDocument(edgeIt.next())
            );
            nbInCycle++;
            if (nbInCycle == INDEX_AFTER_HOW_NB_DOCUMENTS) {
                totalIndexed += INDEX_AFTER_HOW_NB_DOCUMENTS;
                addDocuments(edgesDocument);
                commit();
                System.out.println(
                        "Indexing edges ... total indexed yet " + totalIndexed
                );
                nbInCycle = 0;
                edgesDocument = new HashSet<>();
            }
        }
        addDocuments(edgesDocument);
        commit();
        totalIndexed += nbInCycle;
        System.out.println(
                "Indexing edges ... total indexed " + totalIndexed
        );
    }

    private void addDocument(SolrInputDocument document) {
        try {
            searchUtils.getServer().add(
                    document
            );
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addDocuments(Collection<SolrInputDocument> collection) {
        try {
            searchUtils.getServer().add(
                    collection
            );
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SolrInputDocument graphElementToDocument(GraphElement graphElement) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("uri", encodeURL(graphElement.uri()));
        document.addField("label", graphElement.label());
        document.addField("label_lower_case", graphElement.label().toLowerCase());
        document.addField("owner_username", graphElement.ownerUsername());
        for (FriendlyResource identification : graphElement.getIdentifications()) {
            document.addField(
                    "identification",
                    encodeURL(identification.uri())
            );
        }
        return document;
    }
}
