package org.mongodb.morphia.query;


import com.mongodb.MongoException;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.logging.Logr;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.testutil.TestEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * @author marcosnils
 */
public class QueryInForKeyTest extends TestBase {
    private static final Logr LOG = MorphiaLoggerFactory.get(QueryInForKeyTest.class);

    private String classpath;

    @Entity
    private static class HasRefs implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        @Reference
        private Key<ReferencedEntity> ref;
    }

    @Entity
    private static class ReferencedEntity extends TestEntity {
        private String foo;

        public ReferencedEntity() {
        }

        public ReferencedEntity(final String s) {
            foo = s;
        }
    }

    @Test
    public void testInQueryByKey() throws Exception {
        final HasRefs hr = new HasRefs();
        List<Key<ReferencedEntity>> refs = new ArrayList<Key<ReferencedEntity>>();
        for (int x = 0; x < 10; x++) {
            final ReferencedEntity re = new ReferencedEntity("" + x);
            getDs().save(re);
            refs.add(new Key<QueryInForKeyTest.ReferencedEntity>(ReferencedEntity.class.getName(), re.getId()));
        }
        hr.ref = refs.get(0);
        
        getDs().save(hr);

        Query<HasRefs> query = getDs().createQuery(HasRefs.class).field("ref").in(refs);
        try {
            final List<HasRefs> res = query.asList();
            Assert.assertEquals(1, res.size());
        } catch (MongoException e) {
            LOG.debug("query = " + query);
            throw e;
        }
    }
}
