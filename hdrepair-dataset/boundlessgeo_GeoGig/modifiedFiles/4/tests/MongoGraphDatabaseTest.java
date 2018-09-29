/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.integration.mongo;

import org.geogit.di.GeogitModule;
import org.geogit.storage.GraphDatabaseTest;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

public class MongoGraphDatabaseTest extends GraphDatabaseTest {
    @Override 
    public void setUpInternal() throws Exception {
        super.setUpInternal();
        final IniMongoProperties properties = new IniMongoProperties();
        final String uri = properties.get("mongodb.uri", String.class).or("mongodb://localhost:27017/"); 
        final String database = properties.get("mongodb.database", String.class).or("geogit");
        MongoClient client = new MongoClient(new MongoClientURI(uri));
        DB db = client.getDB(database);
        db.dropDatabase();
    }

    @Override
    protected Injector createInjector() {
        return Guice.createInjector(Modules.override(new GeogitModule())
                .with(new MongoTestStorageModule()));
    }
}
