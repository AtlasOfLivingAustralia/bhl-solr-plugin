package org.ala.bhl.solr;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.solr.analysis.BaseTokenFilterFactory;

public class SynonymTokenFilterFactory  extends BaseTokenFilterFactory {

	public TokenStream create(TokenStream input) {
		return new SynonymTokenFilter(input);
	}
	
	public static class SynonymTokenFilter extends TokenFilter {

		protected SynonymTokenFilter(TokenStream input) {
			super(input);
			System.err.println("Created new SynonymTokenFilter");
		}

		@Override
		public boolean incrementToken() throws IOException {			
			return false;
		}
		
	}

}
