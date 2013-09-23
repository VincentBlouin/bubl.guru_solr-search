package org.triple_brain.module.solr_search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.core.CoreContainer;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.triple_brain.module.model.User;
import org.triple_brain.module.search.GraphSearch;
import org.triple_brain.module.solr_search.json.SearchJsonConverter;

import java.net.URI;
import java.util.HashSet;
import java.util.StringTokenizer;

import static org.triple_brain.module.common_utils.Uris.encodeURL;

/*
* Copyright Mozilla Public License 1.1
*/
public class SolrGraphSearch implements GraphSearch {
    private enum SearchParam {
        ONLY_OWN_VERTICES,
        IS_VERTEX
    }

    ;

    private SearchUtils searchUtils;

    public static SolrGraphSearch withCoreContainer(CoreContainer coreContainer) {
        return new SolrGraphSearch(coreContainer);
    }

    private SolrGraphSearch(CoreContainer coreContainer) {
        this.searchUtils = SearchUtils.usingCoreCoreContainer(coreContainer);
    }

    @Override
    public JSONArray searchOwnVerticesAndPublicOnesForAutoCompletionByLabel(String label, User user) {
        return searchForGraphElementForAutoCompletionByLabelUsingSearchParams(
                label,
                user,
                new HashSet<SearchParam>() {{
                    add(SearchParam.IS_VERTEX);
                }}
        );
    }

    @Override
    public JSONArray searchOnlyForOwnVerticesForAutoCompletionByLabel(String label, User user) {
        return searchForGraphElementForAutoCompletionByLabelUsingSearchParams(
                label,
                user,
                new HashSet<SearchParam>() {{
                    add(SearchParam.IS_VERTEX);
                    add(SearchParam.ONLY_OWN_VERTICES);
                }}
        );
    }

    @Override
    public JSONArray searchRelationsForAutoCompletionByLabel(String label, User user) {
        return searchForGraphElementForAutoCompletionByLabelUsingSearchParams(
                label,
                user,
                new HashSet<SearchParam>()
        );
    }

    @Override
    public JSONObject getByUri(URI uri, User user) {
        try {
            SolrServer solrServer = searchUtils.getServer();
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(
                    "uri:" + encodeURL(uri) + " AND " +
                            "(owner_username:" + user.username() +
                            " OR " + "is_public:true)"
            );
            QueryResponse queryResponse = solrServer.query(solrQuery);
            return SearchJsonConverter.documentToJson(
                    queryResponse.getResults().get(0)
            );
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    private JSONArray searchForGraphElementForAutoCompletionByLabelUsingSearchParams(String label, User user, HashSet<SearchParam> searchParams) {
        JSONArray graphElements = new JSONArray();
        try {
            SolrServer solrServer = searchUtils.getServer();
            SolrQuery solrQuery = new SolrQuery();
            label = label.toLowerCase();
            String sentenceMinusLastWord = sentenceMinusLastWord(label);
            String lastWord = lastWordOfSentence(label);
            solrQuery.setQuery(
                    "label_lower_case:" + sentenceMinusLastWord + "* AND " +
                            "is_vertex:" + Boolean.toString(searchParams.contains(SearchParam.IS_VERTEX)) + " AND " +
                            "(owner_username:" + user.username() +
                            (searchParams.contains(SearchParam.ONLY_OWN_VERTICES) ?
                                    ")" :
                                    " OR " + "is_public:true)")
            );
            solrQuery.addFilterQuery("label_lower_case:" + sentenceMinusLastWord + "*" + lastWord + "*");
            QueryResponse queryResponse = solrServer.query(solrQuery);
            for (SolrDocument document : queryResponse.getResults()) {
                graphElements.put(SearchJsonConverter.documentToJson(
                        document
                ));

            }
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }
        return graphElements;
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
