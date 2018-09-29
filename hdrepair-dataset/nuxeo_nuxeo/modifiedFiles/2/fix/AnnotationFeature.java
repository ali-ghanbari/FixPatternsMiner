package org.nuxeo.ecm.platform.annotations.repository.service;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.url.core", "org.nuxeo.ecm.relations.api", "org.nuxeo.ecm.relations",
        "org.nuxeo.ecm.relations.jena", "org.nuxeo.ecm.platform.types.api", "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.annotations", "org.nuxeo.ecm.annotations.contrib", "org.nuxeo.ecm.annotations.repository",
        "org.nuxeo.ecm.annotations.repository.test", "org.nuxeo.runtime.jtajca", "org.nuxeo.runtime.datasource" })
@LocalDeploy({ "org.nuxeo.runtime.datasource:anno-ds.xml" })
public class AnnotationFeature extends SimpleFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        Framework.addListener(new AnnotationsJenaSetup());
    }
}
