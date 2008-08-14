/**

Copyright (C) SYSTAP, LLC 2006-2007.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Dec 19, 2007
 */

package com.bigdata.rdf.store;

import java.util.UUID;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

import com.bigdata.rdf.model.BigdataBNodeImpl;
import com.bigdata.rdf.model.BigdataLiteralImpl;
import com.bigdata.rdf.model.BigdataURIImpl;
import com.bigdata.rdf.model.BigdataValue;
import com.bigdata.search.FullTextIndex;
import com.bigdata.service.IBigdataClient;

/**
 * Test of adding terms with the full text index enabled and of lookup of terms
 * by tokens which appear within those terms.
 * <p>
 * Note: The {@link FullTextIndex} is written to the {@link IBigdataClient} API
 * so it can not be tested against the {@link AbstractLocalTripleStore}s.
 * 
 * @todo test both the term at a time and the batch term insert APIs.
 * 
 * @todo test all term types (uris, bnodes, and literals). only literals are
 *       being indexed right now, but there could be a use case for tokenizing
 *       URIs. There is never going to be any reason to tokenize BNodes.
 * 
 * @todo test data type support (probably do not index data typed terms).
 * 
 * @todo test XML literal indexing (strip out CDATA and index the tokens found
 *       therein).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestFullTextIndex extends AbstractTripleStoreTestCase {

    /**
     * 
     */
    public TestFullTextIndex() {
    }

    /**
     * @param name
     */
    public TestFullTextIndex(String name) {
        super(name);
    }

//    public Properties getProperties() {
//
//        Properties properties = new Properties(super.getProperties());
//        
//        // enable the full text index.
//        properties.setProperty(Options.TEXT_INDEX,"true");
//        
//        return properties;
//
//    }
    
    /**
     * Test helper verifies that the term is not in the lexicon, adds the term
     * to the lexicon, verifies that the term can be looked up by its assigned
     * term identifier, verifies that the term is now in the lexicon, and
     * verifies that adding the term again returns the same term identifier.
     * 
     * @param term
     *            The term.
     */
    protected void doAddTermTest(AbstractTripleStore store, BigdataValue term) {

        assertEquals(NULL, store.getTermId(term));

        final long id = store.addTerm(term);

        assertNotSame(NULL, id);

        assertEquals(id, store.getTermId(term));

        assertEquals(term, store.getTerm(id));

        assertEquals(id, store.addTerm(term));

    }

    public void test_fullTextIndex01() throws InterruptedException {

        AbstractTripleStore store = getStore();

        final BigdataValue[] terms = new BigdataValue[] {//
                new BigdataLiteralImpl("abc"),//
                new BigdataLiteralImpl("abc", new BigdataURIImpl(XMLSchema.DECIMAL)),//
                new BigdataLiteralImpl("abc", "en"),//
                new BigdataLiteralImpl("good day", "en"),//
                new BigdataLiteralImpl("gutten tag", "de"),//
                new BigdataLiteralImpl("tag team", "en"),//
                new BigdataLiteralImpl("the first day", "en"),// // 'the' is a stopword.

                new BigdataURIImpl("http://www.bigdata.com"),//
                new BigdataURIImpl(RDF.TYPE),//
                new BigdataURIImpl(RDFS.SUBCLASSOF),//
                new BigdataURIImpl(XMLSchema.DECIMAL),//

                new BigdataBNodeImpl(UUID.randomUUID().toString()),//
                new BigdataBNodeImpl("a12"),//
        };

        try {

            store.addTerms(terms);

            dumpTerms(store);

            /*
             * re-open the store before search to verify that the data were made
             * restart safe.
             */
            if (store.isStable()) {

                store.commit();

                store = reopenStore(store);

                store.textSearch("", "abc"); // finds plain literals (@todo
                                                // or anytype?)
                store.textSearch("en", "abc");
                store.textSearch("en", "GOOD DAY");
                store.textSearch("de", "gutten tag");
                store.textSearch("de", "tag");
                store.textSearch("en", "tag");
                store.textSearch("de", "team");
                store.textSearch("en", "the"); // 'the' is a stopword.
                
            }
            
        } finally {

            store.closeAndDelete();

        }

    }
    
}
