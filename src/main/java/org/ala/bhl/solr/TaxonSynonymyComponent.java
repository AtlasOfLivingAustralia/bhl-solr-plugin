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

		ModifiableSolrParams p = new ModifiableSolrParams(rb.req.getParams());

		if (p.getBool(COMPONENT_NAME, false)) {
			String q = p.get(CommonParams.Q);
			try {
				NamedList<Double> synonyms = SynonymyHelper.extractSynonyms(q, true);
				rb.rsp.add("synonyms", synonyms);
				q = SynonymyHelper.addQueryTerms(q, synonyms);

				System.err.println("Creating new query based on these terms: " + q);

				// Have a new query string now -- reparse into a query...
				LuceneQParserPlugin factory = new LuceneQParserPlugin();
				QParser parser = factory.createParser(q, null, p, rb.req);
				Query query = parser.parse();
				rb.setQuery(query);
				rb.setHighlightQuery(query);
				
				p.set(CommonParams.Q, q);
				rb.req.setParams(p);

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
