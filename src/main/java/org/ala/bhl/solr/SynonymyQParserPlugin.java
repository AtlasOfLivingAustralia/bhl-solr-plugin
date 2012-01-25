package org.ala.bhl.solr;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrQueryParser;

public class SynonymyQParserPlugin extends QParserPlugin {
	
	public static String NAME = "taxa";

	public void init(NamedList args) {
	}

	@Override
	public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		return new SynonymyQParser(qstr, localParams, params, req);
	}

}

class SynonymyQParser extends QParser {

	String sortStr;
	SolrQueryParser lparser;

	public SynonymyQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		super(qstr, localParams, params, req);
	}

	@Override
	public Query parse() throws ParseException {

		System.err.println("Here in my parse!");

		String qstr = getString();

		String defaultField = getParam(CommonParams.DF);
		if (defaultField == null) {
			defaultField = getReq().getSchema().getDefaultSearchFieldName();
		}
		lparser = new SolrQueryParser(this, defaultField);

		// these could either be checked & set here, or in the SolrQueryParser constructor
		String opParam = getParam(QueryParsing.OP);
		if (opParam != null) {
			lparser.setDefaultOperator("AND".equals(opParam) ? QueryParser.Operator.AND : QueryParser.Operator.OR);
		} else {
			// try to get default operator from schema
			QueryParser.Operator operator = getReq().getSchema().getSolrQueryParser(null).getDefaultOperator();
			lparser.setDefaultOperator(null == operator ? QueryParser.Operator.OR : operator);
		}
			
		String newqstr = SynonymyHelper.embellishQueryWithSynonyms(qstr);		
		System.err.println("*** QParser: New Query: " + newqstr);

		Query q = lparser.parse(newqstr);

		return q;
	}

}
