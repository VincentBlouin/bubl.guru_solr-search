/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package learning;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class SolrJTest {
    String solrHomeRelativePath = "src/test/resources/learning/solr/";
    String solrXMLHomeRelativePath = "conf/solr.xml";
    String solrConfigHomeRelativePath = "conf/solrconfig.xml";
    String solrSchemaHomeRelativePath = "conf/schema.xml";
    String defaultCollectionName = "collection1";
    CoreContainer coreContainer;
    SolrServer solrServer;

    @Before
    public void before()throws Exception{
        solrServer = startUpSolrServer();
        solrServer.deleteByQuery("*:*");
        solrServer.commit();
    }

    @After
    public void after(){
        coreContainer.shutdown();
    }

    private SolrServer startUpSolrServer()throws Exception{
        File solrConfigXml = new File(solrHomeRelativePath + solrXMLHomeRelativePath);
        String solrHome = solrHomeRelativePath;
        coreContainer = new CoreContainer(solrHome, solrConfigXml);
        SolrServer solrServer = new EmbeddedSolrServer(coreContainer, defaultCollectionName);
        return solrServer;
    }

    @Test
    @Ignore("it used to work because segments were there. It shouldnt depend on that.")
    public void can_add_document()throws Exception{
        Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        docs.add(doc1Example());
        docs.add(doc2Example());
        solrServer.add(docs);
        solrServer.commit();
    }

    @Test
    @Ignore("it used to work because segments were there. It shouldnt depend on that.")
    public void can_query_documents()throws Exception{
        add1Document();
        SolrDocumentList docs = resultsOfQuery(
                new SolrQuery().setQuery("*:*")
        );
        assertThat(docs.size(), is(1));
    }

    @Test
    @Ignore("it used to work because segments were there. It shouldnt depend on that.")
    public void can_query_for_field_value()throws Exception{
        add2Documents();
        SolrDocumentList docs = resultsOfQuery(
                new SolrQuery().setQuery("*:*")
        );
        assertThat(docs.size(), is(2));
        docs = resultsOfQuery(
                new SolrQuery().setQuery("name:doc1")
        );

        assertThat(docs.size(), is(1));
    }

    @Test
    @Ignore("it used to work because segments were there. It shouldnt depend on that.")
    public void can_search()throws Exception{
        add2Documents();
        SolrDocumentList docs = resultsOfQuery(
                new SolrQuery().setQuery("*:*")
        );
        assertThat(docs.size(), is(2));
        docs = resultsOfQuery(
                new SolrQuery().setQuery("name:*doc*")
        );
        assertThat(docs.size(), is(2));
    }


    @Test
    @Ignore("it used to work because segments were there. It shouldnt depend on that.")
    public void can_get_auto_complete_suggestions()throws Exception{
        add2Documents();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setParam(CommonParams.QT, "/suggest");
        solrQuery.setParam(CommonParams.Q, "do");
        List<String> suggestions = suggestionsOfQuery(solrQuery);
        assertThat(suggestions.size(), is(2));
    }
    @Test
    @Ignore("it used to work because segments were there. It shouldnt depend on that.")
    public void can_persist_documents()throws Exception{
        assertTrue(coreContainer.isPersistent());
        add2Documents();
        assertFalse(getAllDocsUsingSolr().isEmpty());
        coreContainer.shutdown();
        try{
            getAllDocsUsingSolr().isEmpty();
            fail();
        }catch(SolrServerException e){

        }
        solrServer = startUpSolrServer();
        assertFalse(getAllDocsUsingSolr().isEmpty());
    }

    @Test
    @Ignore("its taking memory to create multicores and we also dont need that feature")
    public void can_create_another_core()throws Exception{
        QueryResponse queryResponse = createCoreWithName(
                UUID.randomUUID().toString()
        );
        assertThat(
                (Integer) queryResponse.getResponseHeader().get("status"),
                is(0)
        );
    }

    @Test
    @Ignore("its taking memory to create multicores and we also dont need that feature")
    public void can_query_a_new_core()throws Exception{
        String coreName = UUID.randomUUID().toString();
        SolrCore solrCore = coreContainer.create(new CoreDescriptor(
                coreContainer,
                coreName,
                "."
        ));
        coreContainer.register(solrCore, true);
        solrServer = new EmbeddedSolrServer(coreContainer, coreName);
        solrServer.add(doc1Example());
        solrServer.commit();
        SolrDocumentList documents = getAllDocsUsingSolr();
        assertThat(documents.size(), is(1));
        solrServer = new EmbeddedSolrServer(coreContainer, defaultCollectionName);
        documents = getAllDocsUsingSolr();
        assertThat(documents.size(), is(0));
    }

    @Test
    public void can_search_on_a_multivalue_field()throws Exception{
        SolrInputDocument doc1 = new SolrInputDocument();
        doc1.addField( "id", "foo");
        doc1.addField( "multi_value", "apple");
        doc1.addField( "multi_value", "meat");
        solrServer.add(doc1);
        solrServer.commit();
        SolrDocumentList docs = resultsOfQuery(
                new SolrQuery().setQuery("multi_value:meat")
        );
        assertThat(docs.size(), is(1));
    }

    private QueryResponse createCoreWithName(String name)throws Exception{
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setParam(CommonParams.QT, "/admin/cores");
        solrQuery.setParam(
                CoreAdminParams.ACTION,
                CoreAdminParams.CoreAdminAction.CREATE.name()
        );
        solrQuery.setParam(
                CoreAdminParams.NAME,
                name
        );
        solrQuery.setParam(
                CoreAdminParams.INSTANCE_DIR,
                "./" + name
        );
        solrQuery.setParam(
                CoreAdminParams.CONFIG,
                solrHomeRelativePath + solrConfigHomeRelativePath
        );
        solrQuery.setParam(
                CoreAdminParams.SCHEMA,
                solrHomeRelativePath + solrSchemaHomeRelativePath
        );
        solrQuery.setParam(
                CoreAdminParams.DATA_DIR,
                "."
        );
        return solrServer.query(solrQuery);
    }

    private SolrDocumentList getAllDocsUsingSolr()throws Exception{
        return resultsOfQuery(
                new SolrQuery().setQuery("*:*")
        );
    }

    private SolrDocumentList resultsOfQuery(SolrQuery solrQuery)throws Exception{
        return solrServer.query(solrQuery).getResults();
    }

    private List<String> suggestionsOfQuery(SolrQuery solrQuery)throws Exception{
        return solrServer.query(solrQuery).getSpellCheckResponse().getSuggestions().get(0).getAlternatives();
    }

    private void add1Document()throws Exception{
        solrServer.add(doc1Example());
        solrServer.commit();
    }

    private void add2Documents()throws Exception{
        solrServer.add(new ArrayList<SolrInputDocument>(){{
            add(doc1Example());
            add(doc2Example());
        }});
        solrServer.commit();
    }

    private SolrInputDocument doc1Example(){
        SolrInputDocument doc1 = new SolrInputDocument();
        doc1.addField( "id", "id1", 1.0f );
        doc1.addField( "name", "doc1", 1.0f );
        doc1.addField( "price", 10 );
        return doc1;
    }

    private SolrInputDocument doc2Example(){
        SolrInputDocument doc2 = new SolrInputDocument();
        doc2.addField( "id", "id2", 1.0f );
        doc2.addField( "name", "doc2", 1.0f );
        doc2.addField( "price", 20 );
        return doc2;
    }
}
