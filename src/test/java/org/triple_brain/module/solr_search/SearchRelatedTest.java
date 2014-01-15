package org.triple_brain.module.solr_search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.AdaptableGraphComponentTest;
import org.triple_brain.module.model.graph.GraphFactory;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.model.graph.scenarios.TestScenarios;
import org.triple_brain.module.model.graph.scenarios.VerticesCalledABAndC;
import org.triple_brain.module.model.graph.vertex.VertexOperator;
import org.triple_brain.module.search.GraphIndexer;
import org.triple_brain.module.search.GraphSearch;

import javax.inject.Inject;
import java.io.File;

/*
* Copyright Mozilla Public License 1.1
*/
public class SearchRelatedTest extends AdaptableGraphComponentTest{
    @Inject
    GraphFactory graphMaker;

    GraphIndexer graphIndexer;

    GraphSearch graphSearch;

    @Inject
    protected TestScenarios testScenarios;

    protected SearchUtils searchUtils;

    protected VertexOperator vertexA;
    protected VertexOperator vertexB;
    protected VertexOperator vertexC;
    protected VertexOperator pineApple;
    protected User user;
    protected User user2;
    protected Vertex vertexOfUser2;
    protected static CoreContainer coreContainer;

    @BeforeClass
    public static void beforeSearchRelatedClass()throws Exception{
        coreContainer = getCoreContainerForTests();
    }

    protected static CoreContainer getCoreContainerForTests()throws Exception{
        String solrHomePath = "src/main/resources/solr/";
        String solrXMLPath = "conf/solr.xml";
        File solrConfigXml = new File(solrHomePath + solrXMLPath);
        return new CoreContainer(solrHomePath, solrConfigXml);
    }

    @Before
    public void beforeSearchRelatedTest() throws Exception{
        graphIndexer = SolrGraphIndexer.withCoreContainer(coreContainer);
        graphSearch = SolrGraphSearch.withCoreContainer(coreContainer);
        injector.injectMembers(graphIndexer);
        searchUtils = SearchUtils.usingCoreCoreContainer(coreContainer);
        user = User.withUsernameEmailAndLocales(
                "test2",
                "test@2example.org",
                "[fr]"
        );
        user2 = User.withUsernameEmailAndLocales(
                "test",
                "test@example.org",
                "[fr]"
        );
        deleteAllDocsOfUser(user);
        deleteAllDocsOfUser(user2);
        makeGraphHave3SerialVerticesWithLongLabels();
        vertexOfUser2 = graphMaker.createForUser(user2).defaultVertex();
        pineApple = testScenarios.addPineAppleVertexToVertex(vertexC);
    }

    @AfterClass
    public static void afterSearchRelatedClass(){
        coreContainer.shutdown();
    }


    protected void makeGraphHave3SerialVerticesWithLongLabels() throws Exception {
        VerticesCalledABAndC vertexABAndC = testScenarios.makeGraphHave3SerialVerticesWithLongLabels(
                graphMaker.createForUser(user)
        );
        vertexA = vertexABAndC.vertexA();
        vertexB = vertexABAndC.vertexB();
        vertexC = vertexABAndC.vertexC();
    }

    protected void deleteAllDocsOfUser(User user)throws Exception{
        SolrServer solrServer = solrServer();
        solrServer.deleteByQuery("owner_username:" + user.username());
        solrServer.commit();
    }

    protected SolrDocumentList resultsOfSearchQuery(SolrQuery solrQuery)throws Exception{
        SolrServer solrServer = solrServer();
        QueryResponse queryResponse = solrServer.query(solrQuery);
        return queryResponse.getResults() == null ?
                new SolrDocumentList() :
                solrServer.query(solrQuery).getResults();
    }

    protected SolrServer solrServer(){
        return searchUtils.getServer();
    }

    protected void indexGraph(){
        graphIndexer.indexVertex(vertexA);
        graphIndexer.indexVertex(vertexB);
        graphIndexer.indexVertex(vertexC);
        graphIndexer.indexRelation(
                vertexA.connectedEdges().iterator().next()
        );
        graphIndexer.indexRelation(
                vertexC.connectedEdges().iterator().next()
        );
        graphIndexer.commit();
    }

    protected void indexVertex(VertexOperator vertex){
        graphIndexer.indexVertex(vertex);
        graphIndexer.commit();
    }
}