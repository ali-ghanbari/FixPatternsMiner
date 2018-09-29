package it.unibz.krdb.obda.parser;

/*
 * #%L
 * ontop-test
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWL;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLConnection;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLFactory;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.Assert.assertTrue;

/***
 * Tests that the SPARQL ORDER BY statement is correctly translated to ORDER BY in SQL.
 */
public class OracleORDERBYTest {

    private OBDADataFactory fac;
    private QuestOWLConnection conn;

    Logger log = LoggerFactory.getLogger(this.getClass());
    private OBDAModel obdaModel;
    private OWLOntology ontology;
    private QuestOWLFactory factory;

    final String owlFile = "resources/orderby/orderBy.owl";
    final String obdaFile = "resources/orderby/orderBy.obda";
    private QuestOWL reasoner;

    @Before
    public void setUp() throws Exception {


        // Loading the OWL file
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument((new File(owlFile)));

        // Loading the OBDA data
        fac = OBDADataFactoryImpl.getInstance();
        obdaModel = fac.getOBDAModel();





    }

    @After
    public void tearDown() throws Exception{
        conn.close();
        reasoner.dispose();
    }


    private void runQuery(String query) throws OBDAException, OWLException{

        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FULL_METADATA, QuestConstants.FALSE);
        // Creating a new instance of the reasoner
        factory = new QuestOWLFactory();
        factory.setOBDAController(obdaModel);

        factory.setPreferenceHolder(p);

        reasoner = (QuestOWL) factory.createReasoner(ontology, new SimpleConfiguration());

        // Now we are ready for querying
        conn = reasoner.getConnection();

        QuestOWLStatement st = conn.createStatement();
        String sql = st.getUnfolding(query);
        //boolean m = sql.matches("(?ms)(.*)ORDER BY country_name (.*)");
        boolean m = sql.matches("(?ms)(.*)ORDER BY (.*)");
        log.debug(sql);
        assertTrue(m);
    }


    @Test
    public void testOrderBy() throws Exception {

        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        ioManager.load(obdaFile);
        String query = "PREFIX : <http://www.semanticweb.org/ontologies/2013/7/untitled-ontology-150#> " +
                "SELECT ?x ?name " +
                "WHERE { ?x a :Country; :name ?name . } "
                + "ORDER BY ?name"
                ;
        runQuery(query);
    }


}
