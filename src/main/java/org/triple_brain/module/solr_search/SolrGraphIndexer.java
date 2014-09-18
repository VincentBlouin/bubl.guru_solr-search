/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.solr_search;

import com.google.inject.Inject;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.WholeGraph;
import org.triple_brain.module.model.graph.FriendlyResourcePojo;
import org.triple_brain.module.model.graph.GraphElement;
import org.triple_brain.module.model.graph.GraphElementPojo;
import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.schema.Schema;
import org.triple_brain.module.model.graph.schema.SchemaOperator;
import org.triple_brain.module.model.graph.schema.SchemaPojo;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;
import org.triple_brain.module.model.graph.vertex.VertexOperator;
import org.triple_brain.module.model.json.graph.GraphElementJson;
import org.triple_brain.module.search.GraphIndexer;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import static org.triple_brain.module.common_utils.Uris.encodeURL;

public class SolrGraphIndexer implements GraphIndexer {
    @Inject
    WholeGraph wholeGraph;

    private int INDEX_AFTER_HOW_NB_DOCUMENTS = 100;
    private SearchUtils searchUtils;

    public static SolrGraphIndexer withCoreContainer(CoreContainer coreContainer) {
        return new SolrGraphIndexer(coreContainer);
    }

    private SolrGraphIndexer(CoreContainer coreContainer) {
        this.searchUtils = SearchUtils.usingCoreCoreContainer(coreContainer);
    }

    @Override
    public void indexWholeGraph() {
        indexAllVertices();
        indexAllEdges();
        indexAllSchemas();
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
    public void indexSchema(SchemaPojo schema) {
        addDocument(
                schemaDocument(schema)
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
        document.addField("is_schema", false);
        document.addField("is_public", vertex.isPublic());
        document.addField("comment", vertex.comment());
        return document;
    }

    private SolrInputDocument schemaDocument(SchemaPojo schema) {
        SolrInputDocument document = friendlyResourceToDocument(schema);
        document.addField("is_vertex", true);
        document.addField("is_schema", true);
        document.addField("is_public", true);
        document.addField(
                "properties",
                GraphElementJson.multipleToJson(schema.getProperties())
        );
        return document;
    }

    private void indexAllVertices() {
        Iterator<VertexInSubGraphOperator> vertexIt = wholeGraph.getAllVertices();
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

    private void indexAllSchemas() {
        Iterator<SchemaOperator> schemaIt = wholeGraph.getAllSchemas();
        Set<SolrInputDocument> schemasDocument = new HashSet<>();
        int totalIndexed = 0;
        int nbInCycle = 0;
        while (schemaIt.hasNext()) {
            schemasDocument.add(
                    schemaDocument(
                            new SchemaPojo(schemaIt.next())
                    )
            );
            nbInCycle++;
            if (nbInCycle == INDEX_AFTER_HOW_NB_DOCUMENTS) {
                totalIndexed += INDEX_AFTER_HOW_NB_DOCUMENTS;
                addDocuments(schemasDocument);
                commit();
                System.out.println(
                        "Indexing schemas ... total indexed yet " + totalIndexed
                );
                nbInCycle = 0;
                schemasDocument = new HashSet<>();
            }
        }
        if(schemasDocument.isEmpty()){
            return;
        }
        addDocuments(schemasDocument);
        commit();
        totalIndexed += nbInCycle;
        System.out.println(
                "Indexing schemas ... total indexed " + totalIndexed
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
        SolrInputDocument document = friendlyResourceToDocument(graphElement);
        for (URI identificationUri : graphElement.getIdentifications().keySet()) {
            document.addField(
                    "identification",
                    encodeURL(identificationUri)
            );
        }
        return document;
    }
    private SolrInputDocument friendlyResourceToDocument(FriendlyResource friendlyResource) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("uri", encodeURL(friendlyResource.uri()));
        document.addField("label", friendlyResource.label());
        document.addField("label_lower_case", friendlyResource.label().toLowerCase());
        document.addField("owner_username", friendlyResource.getOwnerUsername());
        return document;
    }
}
