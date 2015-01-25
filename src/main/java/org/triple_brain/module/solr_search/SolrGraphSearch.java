/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.solr_search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.triple_brain.module.model.User;
import org.triple_brain.module.search.GraphElementSearchResult;
import org.triple_brain.module.search.GraphSearch;
import org.triple_brain.module.search.VertexSearchResult;

import java.net.URI;
import java.util.List;
import java.util.StringTokenizer;

import static org.triple_brain.module.common_utils.Uris.encodeURL;

public class SolrGraphSearch implements GraphSearch {

    private SearchUtils searchUtils;

    public static SolrGraphSearch withCoreContainer(CoreContainer coreContainer) {
        return new SolrGraphSearch(coreContainer);
    }

    private SolrGraphSearch(CoreContainer coreContainer) {
        this.searchUtils = SearchUtils.usingCoreCoreContainer(coreContainer);
    }

    @Override
    public List<VertexSearchResult> searchSchemasOwnVerticesAndPublicOnesForAutoCompletionByLabel(String label, User user) {
        return SearchToPojoConverter.verticesSearchResultFromDocuments(
                autoCompletionForPublicOrPrivate(
                        label,
                        user,
                        "(is_vertex:true OR is_schema:true)"
                )
        );
    }

    @Override
    public List<VertexSearchResult> searchOnlyForOwnVerticesOrSchemasForAutoCompletionByLabel(String label, User user) {
        return SearchToPojoConverter.verticesSearchResultFromDocuments(
                autoCompletionForPrivate(
                        label,
                        user,
                        "(is_vertex:true OR is_schema:true)"
                )
        );
    }

    @Override
    public List<VertexSearchResult> searchOnlyForOwnVerticesForAutoCompletionByLabel(String label, User user) {
        return SearchToPojoConverter.verticesSearchResultFromDocuments(
                autoCompletionForPrivate(
                        label,
                        user,
                        "is_vertex:true"
                )
        );
    }

    @Override
    public List<GraphElementSearchResult> searchRelationsPropertiesOrSchemasForAutoCompletionByLabel(String label, User user) {
        return SearchToPojoConverter.edgesSearchResultFromDocuments(
                autoCompletionForPrivate(
                        label,
                        user,
                        "(is_relation:true OR is_schema:true)"
                )
        );
    }

    @Override
    public GraphElementSearchResult getByUri(URI uri, User user) {
        try {
            SolrServer solrServer = searchUtils.getServer();
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(
                    "uri:" + encodeURL(uri) + " AND " +
                            "(owner_username:" + user.username() +
                            " OR " + "is_public:true)"
            );
            QueryResponse queryResponse = solrServer.query(solrQuery);
            return SearchToPojoConverter.graphElementSearchResultFromDocument(
                    queryResponse.getResults().get(0)
            );
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    private SolrDocumentList autoCompletionForPublicOrPrivate(
            String label,
            User user,
            String queryPart
    ) {
        return autoCompletion(
                label,
                user,
                queryPart,
                false
        );
    }

    private SolrDocumentList autoCompletionForPrivate(
            String label,
            User user,
            String queryPart
    ) {
        return autoCompletion(
                label,
                user,
                queryPart,
                true
        );
    }

    private SolrDocumentList autoCompletion(
            String label,
            User user,
            String queryPart,
            Boolean forPrivateOnly
    ) {
        try {
            SolrServer solrServer = searchUtils.getServer();
            SolrQuery solrQuery = new SolrQuery();
            label = label.toLowerCase();
            String sentenceMinusLastWord = ClientUtils.escapeQueryChars(sentenceMinusLastWord(label));
            String lastWord = ClientUtils.escapeQueryChars(lastWordOfSentence(label));
            solrQuery.setQuery(
                    queryPart + " AND " +
                            "(owner_username:" + user.username() +
                            (forPrivateOnly ?
                                    ")" :
                                    " OR " + "is_public:true)")
            );
            String labelQuery = "(" + sentenceMinusLastWord + "*" + lastWord + "*)";
            solrQuery.addFilterQuery(
                    "label_lower_case:" + labelQuery + " OR " +
                            "property_label:" + labelQuery
            );
            QueryResponse queryResponse = solrServer.query(solrQuery);
            return queryResponse.getResults();
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    private String lastWordOfSentence(String sentence) {
        StringTokenizer tokenizer = new StringTokenizer(
                sentence,
                " "
        );
        String lastWord = "";
        while (tokenizer.hasMoreTokens()) {
            lastWord = tokenizer.nextToken();
        }
        return lastWord;
    }

    private String sentenceMinusLastWord(String sentence) {
        if (sentence.contains(" ")) {
            return sentence.substring(0, sentence.lastIndexOf(" "));
        } else {
            return "";
        }
    }
}
