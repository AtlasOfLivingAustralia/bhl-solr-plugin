package org.ala.bhl.solr;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.search.LuceneQParserPlugin;
import org.apache.solr.search.QParser;

public class TaxonSynonymyComponent extends SearchComponent {

	public static final String COMPONENT_NAME = "taxa";

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {

		ModifiableSolrParams solrParams = new ModifiableSolrParams(rb.req.getParams());

		if (solrParams.getBool(COMPONENT_NAME, false)) {
			String qstr = solrParams.get(CommonParams.Q);
			try {
				NamedList<Double> synonyms = SynonymyHelper.extractSynonyms(qstr, true);
				rb.rsp.add("synonyms", synonyms);
				String newQstr = SynonymyHelper.addQueryTerms(qstr, synonyms);
				// Have a new query string now -- reparse into a query object...
				LuceneQParserPlugin factory = new LuceneQParserPlugin();
				QParser parser = factory.createParser(newQstr, null, solrParams, rb.req);				
				Query query = parser.parse();
				// Set the query object into the request
				rb.setQuery(query);
				// and the highlight query, otherwise only the original query terms will be highlighted.
				rb.setHighlightQuery(query);
				// And for completeness sake, set the query string in the request to this one...
				solrParams.set(CommonParams.Q, newQstr);
				rb.req.setParams(solrParams);

			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

	}

	@Override
	public String getDescription() {
		return "Search component that extracts taxa synonyms and injects them as query terms";
	}

	@Override
	public String getSourceId() {
		return null;
	}

	@Override
	public String getSource() {
		return null;
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

}
