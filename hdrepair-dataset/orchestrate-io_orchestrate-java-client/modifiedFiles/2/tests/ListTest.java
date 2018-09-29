package io.orchestrate.client.integration;

import static org.junit.Assert.assertNotNull;
import io.orchestrate.client.KvList;
import io.orchestrate.client.KvListOperation;
import io.orchestrate.client.OrchestrateFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class ListTest extends OperationTest {

    private <T> KvList<T> result(final KvListOperation<T> listOp)
            throws InterruptedException, ExecutionException, TimeoutException {
        final OrchestrateFuture<KvList<T>> future = client().execute(listOp);
        return future.get(3, TimeUnit.SECONDS);
    }

    @Test
    public void basicList()
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvListOperation<String> listOp = new KvListOperation<String>(TEST_COLLECTION, String.class);
        final KvList<String> results = result(listOp);
        assertNotNull(results);
    }

    @Test
    public void listWithAfterKey()
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvListOperation<String> listOp = new KvListOperation<String>(TEST_COLLECTION, "someKey", 3,
                false, String.class);
        final KvList<String> results = result(listOp);
        assertNotNull(results);
    }

    @Test
    public void listWithStartKey()
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvListOperation<String> listOp = new KvListOperation<String>(TEST_COLLECTION, "someKey", 3,
                true, String.class);
        final KvList<String> results = result(listOp);
        assertNotNull(results);
    }

}
