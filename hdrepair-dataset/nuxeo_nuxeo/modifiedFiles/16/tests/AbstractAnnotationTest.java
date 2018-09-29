/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.io.InputStream;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.hsqldb.jdbc.jdbcDataSource;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.annotations.FakeNuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public abstract class AbstractAnnotationTest extends RepositoryOSGITestCase {

    protected AnnotationsService service;

    protected Annotation annotation;

    protected Annotation annotation1;

    protected final NuxeoPrincipal user = new FakeNuxeoPrincipal("bob");

    protected final AnnotationManager manager = new AnnotationManager();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:jena");
        ds.setUser("sa");
        ds.setPassword("");
        Context context = new InitialContext();
        context.bind("java:/nxrelations-default-jena", ds);
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseType", "HSQL");
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseTransactionEnabled", "false");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.annotations.test");
        deployBundle("org.nuxeo.ecm.annotations.contrib");
        deployBundle("org.nuxeo.ecm.annotations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        service = Framework.getService(AnnotationsService.class);
        assertNotNull(service);
        InputStream is = getClass().getResourceAsStream("/post-rdf.xml");
        assertNotNull(is);
        annotation = manager.getAnnotation(is);
        assertNotNull(annotation);
    }

}
