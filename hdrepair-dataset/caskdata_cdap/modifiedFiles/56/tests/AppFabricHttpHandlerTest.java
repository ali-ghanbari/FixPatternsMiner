package com.continuuity.internal.app.services.http.handlers;

import com.continuuity.AppWithSchedule;
import com.continuuity.AppWithWorkflow;
import com.continuuity.DummyAppWithTrackingTable;
import com.continuuity.MultiStreamApp;
import com.continuuity.SleepingWorkflowApp;
import com.continuuity.WordCountApp;
import com.continuuity.api.data.DataSetSpecification;
import com.continuuity.api.data.dataset.KeyValueTable;
import com.continuuity.api.data.dataset.ObjectStore;
import com.continuuity.api.data.stream.StreamSpecification;
import com.continuuity.app.program.ManifestFields;
import com.continuuity.app.services.EntityType;
import com.continuuity.app.services.ProgramId;
import com.continuuity.common.conf.Constants;
import com.continuuity.data2.transaction.Transaction;
import com.continuuity.data2.transaction.TransactionSystemClient;
import com.continuuity.data2.transaction.persist.SnapshotCodecV2;
import com.continuuity.data2.transaction.persist.TransactionSnapshot;
import com.continuuity.internal.app.services.http.AppFabricTestsSuite;
import com.continuuity.test.internal.DefaultId;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.apache.twill.internal.utils.Dependencies;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import javax.annotation.Nullable;


/**
 *
 */
public class AppFabricHttpHandlerTest {

  private static final Gson GSON = new Gson();
  private static final Type MAP_STRING_STRING_TYPE = new TypeToken<Map<String, String>>() { }.getType();
  private static final Type LIST_MAP_STRING_STRING_TYPE = new TypeToken<List<Map<String, String>>>() { }.getType();

  private String getRunnableStatus(String runnableType, String appId, String runnableId) throws Exception {
    HttpResponse response =
      AppFabricTestsSuite.doGet("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/status");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    String s = EntityUtils.toString(response.getEntity());
    Map<String, String> o = GSON.fromJson(s, new TypeToken<Map<String, String>>() { }.getType());
    return o.get("status");
  }

  private int getFlowletInstances(String appId, String flowId, String flowletId) throws Exception {
    HttpResponse response =
      AppFabricTestsSuite.doGet("/v2/apps/" + appId + "/flows/" + flowId + "/flowlets/" + flowletId + "/instances");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    String result = EntityUtils.toString(response.getEntity());
    Map<String, String> reply = new Gson().fromJson(result, new TypeToken<Map<String, String>>() { }.getType());
    return Integer.parseInt(reply.get("instances"));
  }

  private void setFlowletInstances(String appId, String flowId, String flowletId, int instances) throws Exception {
    JsonObject json = new JsonObject();
    json.addProperty("instances", instances);
    HttpResponse response = AppFabricTestsSuite.doPut("/v2/apps/" + appId + "/flows/" + flowId + "/flowlets/" +
                                                        flowletId + "/instances", json.toString());
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }
  private String getDeploymentStatus() throws Exception {
    HttpResponse response =
      AppFabricTestsSuite.doGet("/v2/deploy/status/");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    String s = EntityUtils.toString(response.getEntity());
    Map<String, String> o = new Gson().fromJson(s, new TypeToken<Map<String, String>>() { }.getType());
    return o.get("status");
  }

  private int getRunnableStartStop(String runnableType, String appId, String runnableId, String action)
    throws Exception {
    HttpResponse response =
      AppFabricTestsSuite.doPost("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/" + action);
    return response.getStatusLine().getStatusCode();
  }

  private void testHistory(Class<?> app, String appId, String runnableType, String runnableId,
                           boolean waitStop, int duration)
      throws Exception {
    try {
      deploy(app);
      Assert.assertEquals(200,
          AppFabricTestsSuite.doPost("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/start", null)
              .getStatusLine().getStatusCode()
      );
      if (waitStop) {
        TimeUnit.SECONDS.sleep(duration);
      } else {
        Assert.assertEquals(200,
            AppFabricTestsSuite.doPost("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/stop", null)
                .getStatusLine().getStatusCode()
        );
      }
      Assert.assertEquals(200,
          AppFabricTestsSuite.doPost("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/start", null)
              .getStatusLine().getStatusCode()
      );
      if (waitStop) {
        TimeUnit.SECONDS.sleep(duration);
      } else {
        Assert.assertEquals(200,
            AppFabricTestsSuite.doPost("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/stop", null)
                .getStatusLine().getStatusCode()
        );
      }

      HttpResponse response = AppFabricTestsSuite.doGet("/v2/apps/" + appId + "/" + runnableType + "/" +
          runnableId + "/history");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      String s = EntityUtils.toString(response.getEntity());
      List<Map<String, String>> o = GSON.fromJson(s, new TypeToken<List<Map<String, String>>>() {
      }.getType());

      // We started and stopped twice, so we should have 2 entries.
      // At least twice because it may have been done in other tests too.
      Assert.assertTrue(o.size() >= 2);

      // For each one, we have 4 fields.
      for (Map<String, String> m : o) {
        Assert.assertEquals(4, m.size());
      }
    } finally {
      Assert.assertEquals(200, AppFabricTestsSuite.doDelete("/v2/apps/" + appId).getStatusLine().getStatusCode());
    }
  }

  private void testRuntimeArgs(Class<?> app, String appId, String runnableType, String runnableId)
      throws Exception {
    deploy(app);

    Map<String, String> args = Maps.newHashMap();
    args.put("Key1", "Val1");
    args.put("Key2", "Val1");
    args.put("Key2", "Val1");

    HttpResponse response;
    String argString = GSON.toJson(args, new TypeToken<Map<String, String>>() { }.getType());
    response = AppFabricTestsSuite.doPut("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/runtimeargs",
        argString);

    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    response = AppFabricTestsSuite.doGet("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/runtimeargs");

    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    Map<String, String> argsRead = GSON.fromJson(EntityUtils.toString(response.getEntity()),
        new TypeToken<Map<String, String>>() { }.getType());

    Assert.assertEquals(args.size(), argsRead.size());

    for (Map.Entry<String, String> entry : args.entrySet()) {
      Assert.assertEquals(entry.getValue(), argsRead.get(entry.getKey()));
    }

    //test empty runtime args
    response = AppFabricTestsSuite.doPut("/v2/apps/" + appId + "/" + runnableType + "/"
        + runnableId + "/runtimeargs", "");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    response = AppFabricTestsSuite.doGet("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/runtimeargs");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    argsRead = GSON.fromJson(EntityUtils.toString(response.getEntity()),
        new TypeToken<Map<String, String>>() { }.getType());
    Assert.assertEquals(0, argsRead.size());

    //test null runtime args
    response = AppFabricTestsSuite.doPut("/v2/apps/" + appId + "/" + runnableType + "/"
        + runnableId + "/runtimeargs", null);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    response = AppFabricTestsSuite.doGet("/v2/apps/" + appId + "/" + runnableType + "/" + runnableId + "/runtimeargs");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    argsRead = GSON.fromJson(EntityUtils.toString(response.getEntity()),
        new TypeToken<Map<String, String>>() { }.getType());
    Assert.assertEquals(0, argsRead.size());
  }

  /**
   * Tests history of a flow.
   */
  @Test
  public void testFlowHistory() throws Exception {
    testHistory(WordCountApp.class, "WordCountApp", "flows", "WordCountFlow", false, 0);
  }

  /**
   * Tests history of a procedure.
   */
  @Test
  public void testProcedureHistory() throws Exception {
    testHistory(WordCountApp.class, "WordCountApp", "procedures", "WordFrequency", false, 0);
  }

  /**
   * Tests history of a mapreduce.
   */
  @Test
  public void testMapreduceHistory() throws Exception {
    testHistory(DummyAppWithTrackingTable.class, "dummy", "mapreduce", "dummy-batch", false, 0);
  }

  /**
   * Tests history of a workflow.
   */
  @Test
  public void testWorkflowHistory() throws Exception {
    testHistory(SleepingWorkflowApp.class, "SleepWorkflowApp", "workflows", "SleepWorkflow", true, 2);
  }

  @Test
  public void testGetSetFlowletInstances() throws Exception {
    //deploy, check the status and start a flow. Also check the status
    deploy(WordCountApp.class);
    Assert.assertEquals("STOPPED", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));
    Assert.assertEquals(200, getRunnableStartStop("flows", "WordCountApp", "WordCountFlow", "start"));
    Assert.assertEquals("RUNNING", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));

    //Get Flowlet Instances
    Assert.assertEquals(1, getFlowletInstances("WordCountApp", "WordCountFlow", "StreamSource"));

    //Set Flowlet Instances
    setFlowletInstances("WordCountApp", "WordCountFlow", "StreamSource", 3);
    Assert.assertEquals(3, getFlowletInstances("WordCountApp", "WordCountFlow", "StreamSource"));

    // Stop the flow and check its status
    Assert.assertEquals(200, getRunnableStartStop("flows", "WordCountApp", "WordCountFlow", "stop"));
    Assert.assertEquals("STOPPED", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));
  }

  @Test
  public void testChangeFlowletStreamInput() throws Exception {
    deploy(MultiStreamApp.class);

    Assert.assertEquals(200,
                        changeFlowletStreamInput("MultiStreamApp", "CounterFlow", "counter1", "stream1", "stream2"));
    // stream1 is no longer a connection
    Assert.assertEquals(500,
                        changeFlowletStreamInput("MultiStreamApp", "CounterFlow", "counter1", "stream1", "stream3"));
    Assert.assertEquals(200,
                        changeFlowletStreamInput("MultiStreamApp", "CounterFlow", "counter1", "stream2", "stream3"));

    Assert.assertEquals(200,
                        changeFlowletStreamInput("MultiStreamApp", "CounterFlow", "counter2", "stream3", "stream4"));
    // stream1 is no longer a connection
    Assert.assertEquals(500,
                        changeFlowletStreamInput("MultiStreamApp", "CounterFlow", "counter2", "stream3", "stream1"));
    Assert.assertEquals(200,
                        changeFlowletStreamInput("MultiStreamApp", "CounterFlow", "counter2", "stream4", "stream1"));

  }

  private int changeFlowletStreamInput(String app, String flow, String flowlet,
                                                String oldStream, String newStream) throws Exception {
    return AppFabricTestsSuite.doPut(
      String.format("/v2/apps/%s/flows/%s/flowlets/%s/connections/%s", app, flow, flowlet, newStream),
      String.format("{\"oldStreamId\":\"%s\"}", oldStream)).getStatusLine().getStatusCode();
  }


  @Test
  public void testStartStop() throws Exception {
    //deploy, check the status and start a flow. Also check the status
    deploy(WordCountApp.class);
    Assert.assertEquals("STOPPED", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));
    Assert.assertEquals(200, getRunnableStartStop("flows", "WordCountApp", "WordCountFlow", "start"));
    Assert.assertEquals("RUNNING", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));

    //web-app, start, stop and status check.
    Assert.assertEquals(200,
                        AppFabricTestsSuite.doPost("/v2/apps/WordCountApp/webapp/start", null)
                          .getStatusLine().getStatusCode());
    Assert.assertEquals("RUNNING", getWebappStatus("WordCountApp"));
    Assert.assertEquals(200,
                        AppFabricTestsSuite.doPost("/v2/apps/WordCountApp/webapp/stop", null)
                          .getStatusLine().getStatusCode());
    Assert.assertEquals("STOPPED", getWebappStatus("WordCountApp"));

    // Stop the flow and check its status
    Assert.assertEquals(200, getRunnableStartStop("flows", "WordCountApp", "WordCountFlow", "stop"));
    Assert.assertEquals("STOPPED", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));

    // Check the start/stop endpoints for procedures
    Assert.assertEquals("STOPPED", getRunnableStatus("procedures", "WordCountApp", "WordFrequency"));
    Assert.assertEquals(200, getRunnableStartStop("procedures", "WordCountApp", "WordFrequency", "start"));
    Assert.assertEquals("RUNNING", getRunnableStatus("procedures", "WordCountApp", "WordFrequency"));
    Assert.assertEquals(200, getRunnableStartStop("procedures", "WordCountApp", "WordFrequency", "stop"));
    Assert.assertEquals("STOPPED", getRunnableStatus("procedures", "WordCountApp", "WordFrequency"));

    //start map-reduce and check status and stop the map-reduce job and check the status ..
    deploy(DummyAppWithTrackingTable.class);
    Assert.assertEquals(200, getRunnableStartStop("mapreduce", "dummy", "dummy-batch", "start"));
    Assert.assertEquals("RUNNING", getRunnableStatus("mapreduce", "dummy", "dummy-batch"));
    Assert.assertEquals(200, getRunnableStartStop("mapreduce", "dummy", "dummy-batch", "stop"));
    Assert.assertEquals("STOPPED", getRunnableStatus("mapreduce", "dummy", "dummy-batch"));

    //deploy and check status of a workflow
    deploy(SleepingWorkflowApp.class);
    Assert.assertEquals(200, getRunnableStartStop("workflows", "SleepWorkflowApp", "SleepWorkflow", "start"));
    while ("STARTING".equals(getRunnableStatus("workflows", "SleepWorkflowApp", "SleepWorkflow"))) {
      TimeUnit.MILLISECONDS.sleep(10);
    }
    Assert.assertEquals("RUNNING", getRunnableStatus("workflows", "SleepWorkflowApp", "SleepWorkflow"));
  }

  /**
   * Metadata tests through appfabric apis.
   */
  @Test
  public void testGetMetadata() throws Exception {
    try {
      HttpResponse response = AppFabricTestsSuite.doPost("/v2/unrecoverable/reset");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      response = deploy(WordCountApp.class);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      response = deploy(AppWithWorkflow.class);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/flows/WordCountFlow");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("WordCountFlow"));

      // verify procedure
      response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/procedures/WordFrequency");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      result = EntityUtils.toString(response.getEntity());
      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("WordFrequency"));

      //verify mapreduce
      response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/mapreduce/VoidMapReduceJob");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      result = EntityUtils.toString(response.getEntity());
      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("VoidMapReduceJob"));

      // verify single workflow
      response = AppFabricTestsSuite.doGet("/v2/apps/AppWithWorkflow/workflows/SampleWorkflow");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      result = EntityUtils.toString(response.getEntity());
      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("SampleWorkflow"));

      // verify apps
      response = AppFabricTestsSuite.doGet("/v2/apps");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      String s = EntityUtils.toString(response.getEntity());
      List<Map<String, String>> o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(2, o.size());
      Assert.assertTrue(o.contains(ImmutableMap.of("type", "App", "id", "WordCountApp", "name", "WordCountApp",
                                                   "description", "Application for counting words")));
      Assert.assertTrue(o.contains(ImmutableMap.of("type", "App", "id", "AppWithWorkflow", "name",
                                                   "AppWithWorkflow", "description", "Sample application")));

      // verify a single app
      response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      Map<String, String> app = new Gson().fromJson(s, MAP_STRING_STRING_TYPE);
      Assert.assertEquals(ImmutableMap.of("type", "App", "id", "WordCountApp", "name", "WordCountApp",
                                          "description", "Application for counting words"), app);

      // verify flows
      response = AppFabricTestsSuite.doGet("/v2/flows");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      Assert.assertTrue(o.contains(ImmutableMap.of("type", "Flow", "app", "WordCountApp", "id", "WordCountFlow", "name",
                                                   "WordCountFlow", "description", "Flow for counting words")));

      // verify flows by app
      response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/flows");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      Assert.assertTrue(o.contains(ImmutableMap.of("type", "Flow", "app", "WordCountApp", "id", "WordCountFlow", "name",
                                                   "WordCountFlow", "description", "Flow for counting words")));

      // verify procedures
      response = AppFabricTestsSuite.doGet("/v2/procedures");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      Assert.assertTrue(o.contains(ImmutableMap.of("type", "Procedure", "app", "WordCountApp", "id", "WordFrequency",
                                                   "name", "WordFrequency", "description",
                                                   "Procedure for executing WordFrequency.")));

      // verify procedures by app
      response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/procedures");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      Assert.assertTrue(o.contains(ImmutableMap.of("type", "Procedure", "app", "WordCountApp", "id", "WordFrequency",
                                                   "name", "WordFrequency", "description",
                                                   "Procedure for executing WordFrequency.")));


      // verify mapreduces
      response = AppFabricTestsSuite.doGet("/v2/mapreduce");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      Assert.assertTrue(o.contains(ImmutableMap.of("type", "Mapreduce", "app", "WordCountApp", "id", "VoidMapReduceJob",
                                                   "name", "VoidMapReduceJob",
                                                   "description", "Mapreduce that does nothing " +
                                                   "(and actually doesn't run) - it is here for testing MDS")));

      // verify workflows
      response = AppFabricTestsSuite.doGet("/v2/workflows");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      Assert.assertTrue(o.contains(ImmutableMap.of(
        "type", "Workflow", "app", "AppWithWorkflow", "id", "SampleWorkflow",
        "name", "SampleWorkflow", "description",  "SampleWorkflow description")));


      // verify programs by non-existent app
      response = AppFabricTestsSuite.doGet("/v2/apps/NonExistenyApp/flows");
      Assert.assertEquals(404, response.getStatusLine().getStatusCode());
      response = AppFabricTestsSuite.doGet("/v2/apps/NonExistenyApp/procedures");
      Assert.assertEquals(404, response.getStatusLine().getStatusCode());
      response = AppFabricTestsSuite.doGet("/v2/apps/NonExistenyApp/mapreduce");
      Assert.assertEquals(404, response.getStatusLine().getStatusCode());
      response = AppFabricTestsSuite.doGet("/v2/apps/NonExistenyApp/workflows");
      Assert.assertEquals(404, response.getStatusLine().getStatusCode());

      // verify programs by app that does not have that program type
      response = AppFabricTestsSuite.doGet("/v2/apps/AppWithWorkflow/flows");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertTrue(o.isEmpty());
      response = AppFabricTestsSuite.doGet("/v2/apps/AppWithWorkflow/procedures");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertTrue(o.isEmpty());
      response = AppFabricTestsSuite.doGet("/v2/apps/AppWithWorkflow/mapreduce");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertTrue(o.isEmpty());
      response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/workflows");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertTrue(o.isEmpty());

      // verify flows by stream
      response = AppFabricTestsSuite.doGet("/v2/streams/text/flows");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      Assert.assertTrue(o.contains(ImmutableMap.of("type", "Flow", "app", "WordCountApp", "id", "WordCountFlow", "name",
                                                   "WordCountFlow", "description", "Flow for counting words")));

      // verify flows by dataset
      response = AppFabricTestsSuite.doGet("/v2/datasets/mydataset/flows");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      Assert.assertTrue(o.contains(ImmutableMap.of("type", "Flow", "app", "WordCountApp", "id", "WordCountFlow", "name",
                                                   "WordCountFlow", "description", "Flow for counting words")));

      // verify one dataset
      response = AppFabricTestsSuite.doGet("/v2/datasets/mydataset");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      Map<String, String> map = new Gson().fromJson(s, MAP_STRING_STRING_TYPE);
      Assert.assertNotNull(map);
      Assert.assertEquals("mydataset", map.get("id"));
      Assert.assertEquals("mydataset", map.get("name"));
      Assert.assertNotNull(map.get("specification"));
      DataSetSpecification spec = new Gson().fromJson(map.get("specification"), DataSetSpecification.class);
      Assert.assertNotNull(spec);

      // verify all datasets
      response = AppFabricTestsSuite.doGet("/v2/datasets");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(3, o.size());
      Map<String, String> expectedDataSets = ImmutableMap.<String, String>builder()
        .put("input", ObjectStore.class.getName())
        .put("output", ObjectStore.class.getName())
        .put("mydataset", KeyValueTable.class.getName()).build();
      for (Map<String, String> ds : o) {
        Assert.assertTrue("problem with dataset " + ds.get("id"), ds.containsKey("id"));
        Assert.assertTrue("problem with dataset " + ds.get("id"), ds.containsKey("name"));
        Assert.assertTrue("problem with dataset " + ds.get("id"), ds.containsKey("classname"));
        Assert.assertTrue("problem with dataset " + ds.get("id"), expectedDataSets.containsKey(ds.get("id")));
        Assert.assertEquals("problem with dataset " + ds.get("id"),
                            expectedDataSets.get(ds.get("id")), ds.get("classname"));
      }

      // verify datasets by app
      response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/datasets");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      expectedDataSets = ImmutableMap.<String, String>builder()
        .put("mydataset", KeyValueTable.class.getName()).build();
      for (Map<String, String> ds : o) {
        Assert.assertTrue("problem with dataset " + ds.get("id"), ds.containsKey("id"));
        Assert.assertTrue("problem with dataset " + ds.get("id"), ds.containsKey("name"));
        Assert.assertTrue("problem with dataset " + ds.get("id"), ds.containsKey("classname"));
        Assert.assertTrue("problem with dataset " + ds.get("id"), expectedDataSets.containsKey(ds.get("id")));
        Assert.assertEquals("problem with dataset " + ds.get("id"),
                            expectedDataSets.get(ds.get("id")), ds.get("classname"));
      }

      // verify one stream
      response = AppFabricTestsSuite.doGet("/v2/streams/text");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      map = new Gson().fromJson(s, MAP_STRING_STRING_TYPE);
      Assert.assertNotNull(map);
      Assert.assertEquals("text", map.get("id"));
      Assert.assertEquals("text", map.get("name"));
      Assert.assertNotNull(map.get("specification"));
      StreamSpecification sspec = new Gson().fromJson(map.get("specification"), StreamSpecification.class);
      Assert.assertNotNull(sspec);

      // verify all streams
      response = AppFabricTestsSuite.doGet("/v2/streams");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      Set<String> expectedStreams = ImmutableSet.of("text");
      for (Map<String, String> stream : o) {
        Assert.assertTrue("problem with stream " + stream.get("id"), stream.containsKey("id"));
        Assert.assertTrue("problem with stream " + stream.get("id"), stream.containsKey("name"));
        Assert.assertTrue("problem with dataset " + stream.get("id"), expectedStreams.contains(stream.get("id")));
      }

      // verify streams by app
      response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/streams");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      s = EntityUtils.toString(response.getEntity());
      o = new Gson().fromJson(s, LIST_MAP_STRING_STRING_TYPE);
      Assert.assertEquals(1, o.size());
      expectedStreams = ImmutableSet.of("text");
      for (Map<String, String> stream : o) {
        Assert.assertTrue("problem with stream " + stream.get("id"), stream.containsKey("id"));
        Assert.assertTrue("problem with stream " + stream.get("id"), stream.containsKey("name"));
        Assert.assertTrue("problem with dataset " + stream.get("id"), expectedStreams.contains(stream.get("id")));
      }
    } finally {
      Assert.assertEquals(200, AppFabricTestsSuite.doDelete("/v2/apps").getStatusLine().getStatusCode());
    }
  }

  /**
   * Tests procedure instances.
   */
  @Test
  public void testProcedureInstances () throws Exception {
    Assert.assertEquals(200, AppFabricTestsSuite.doDelete("/v2/apps").getStatusLine().getStatusCode());
    Assert.assertEquals(200, AppFabricTestsSuite.doPost("/v2/unrecoverable/reset").getStatusLine().getStatusCode());

    HttpResponse response = deploy(WordCountApp.class);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/procedures/WordFrequency/instances");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    String s = EntityUtils.toString(response.getEntity());
    Map<String, String> result = new Gson().fromJson(s, MAP_STRING_STRING_TYPE);
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(1, Integer.parseInt(result.get("instances")));

    JsonObject json = new JsonObject();
    json.addProperty("instances", 10);

    response = AppFabricTestsSuite.doPut("/v2/apps/WordCountApp/procedures/WordFrequency/instances",
                                           json.toString());
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/procedures/WordFrequency/instances");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    s = EntityUtils.toString(response.getEntity());
    result = new Gson().fromJson(s, MAP_STRING_STRING_TYPE);
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(10, Integer.parseInt(result.get("instances")));

    Assert.assertEquals(200, AppFabricTestsSuite.doDelete("/v2/apps/WordCountApp").getStatusLine().getStatusCode());
  }

  @Test
  public void testStatus() throws Exception {

    //deploy and check the status
    deploy(WordCountApp.class);
    //check the status of the deployment
    Assert.assertEquals("DEPLOYED", getDeploymentStatus());
    Assert.assertEquals("STOPPED", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));

    //start flow and check the status
    ProgramId flowId = new ProgramId(DefaultId.DEFAULT_ACCOUNT_ID, "WordCountApp", "WordCountFlow");
    AppFabricTestsSuite.startProgram(flowId);
    Assert.assertEquals("RUNNING", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));

    //stop the flow and check the status
    AppFabricTestsSuite.stopProgram(flowId);
    Assert.assertEquals("STOPPED", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));

    //check the status for procedure
    ProgramId procedureId = new ProgramId(DefaultId.DEFAULT_ACCOUNT_ID, "WordCountApp", "WordFrequency");
    procedureId.setType(EntityType.PROCEDURE);
    AppFabricTestsSuite.startProgram(procedureId);
    Assert.assertEquals("RUNNING", getRunnableStatus("procedures", "WordCountApp", "WordFrequency"));
    AppFabricTestsSuite.stopProgram(procedureId);

    //start map-reduce and check status and stop the map-reduce job and check the status ..
    deploy(DummyAppWithTrackingTable.class);
    ProgramId mapreduceId = new ProgramId(DefaultId.DEFAULT_ACCOUNT_ID, "dummy", "dummy-batch");
    mapreduceId.setType(EntityType.MAPREDUCE);
    AppFabricTestsSuite.startProgram(mapreduceId);
    Assert.assertEquals("RUNNING", getRunnableStatus("mapreduce", "dummy", "dummy-batch"));

    //stop the mapreduce program and check the status
    AppFabricTestsSuite.stopProgram(mapreduceId);
    Assert.assertEquals("STOPPED", getRunnableStatus("mapreduce", "dummy", "dummy-batch"));

    //deploy and check status of a workflow
    deploy(SleepingWorkflowApp.class);
    ProgramId workflowId = new ProgramId(DefaultId.DEFAULT_ACCOUNT_ID, "SleepWorkflowApp", "SleepWorkflow");
    workflowId.setType(EntityType.WORKFLOW);
    AppFabricTestsSuite.startProgram(workflowId);
    while ("STARTING".equals(getRunnableStatus("workflows", "SleepWorkflowApp", "SleepWorkflow"))) {
      TimeUnit.MILLISECONDS.sleep(10);
    }
    Assert.assertEquals("RUNNING", getRunnableStatus("workflows", "SleepWorkflowApp", "SleepWorkflow"));
    AppFabricTestsSuite.stopProgram(workflowId);
  }

  private String getWebappStatus(String appId) throws Exception {
    HttpResponse response = AppFabricTestsSuite.doGet("/v2/apps/" + appId + "/" + "webapp" + "/status");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    String s = EntityUtils.toString(response.getEntity());
    Map<String, String> o = new Gson().fromJson(s, MAP_STRING_STRING_TYPE);
    return o.get("status");
  }

  @Test
  public void testFlowRuntimeArgs() throws Exception {
    testRuntimeArgs(WordCountApp.class, "WordCountApp", "flows", "WordCountFlow");
  }

  @Test
  public void testWorkflowRuntimeArgs() throws Exception {
    testRuntimeArgs(SleepingWorkflowApp.class, "SleepWorkflowApp", "workflows", "SleepWorkflow");
  }

  @Test
  public void testProcedureRuntimeArgs() throws Exception {
    testRuntimeArgs(WordCountApp.class, "WordCountApp", "procedures", "WordFrequency");
  }

  @Test
  public void testMapreduceRuntimeArgs() throws Exception {
    testRuntimeArgs(DummyAppWithTrackingTable.class, "dummy", "mapreduce", "dummy-batch");
  }

  /**
   * Deploys and application.
   */
  static HttpResponse deploy(Class<?> application) throws Exception {
    return deploy(application, null);
  }
  /**
   * Deploys and application with (optionally) defined app name
   */
  static HttpResponse deploy(Class<?> application, @Nullable String appName) throws Exception {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(ManifestFields.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(ManifestFields.MAIN_CLASS, application.getName());

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final JarOutputStream jarOut = new JarOutputStream(bos, manifest);
    final String pkgName = application.getPackage().getName();

    // Grab every classes under the application class package.
    try {
      ClassLoader classLoader = application.getClassLoader();
      if (classLoader == null) {
        classLoader = ClassLoader.getSystemClassLoader();
      }
      Dependencies.findClassDependencies(classLoader, new Dependencies.ClassAcceptor() {
        @Override
        public boolean accept(String className, URL classUrl, URL classPathUrl) {
          try {
            if (className.startsWith(pkgName)) {
              jarOut.putNextEntry(new JarEntry(className.replace('.', '/') + ".class"));
              InputStream in = classUrl.openStream();
              try {
                ByteStreams.copy(in, jarOut);
              } finally {
                in.close();
              }
              return true;
            }
            return false;
          } catch (Exception e) {
            throw Throwables.propagate(e);
          }
        }
      }, application.getName());

      // Add webapp
      jarOut.putNextEntry(new ZipEntry("webapp/default/netlens/src/1.txt"));
      ByteStreams.copy(new ByteArrayInputStream("dummy data".getBytes(Charsets.UTF_8)), jarOut);
    } finally {
      jarOut.close();
    }

    HttpEntityEnclosingRequestBase request;
    if (appName == null) {
      request = AppFabricTestsSuite.getPost("/v2/apps");
    } else {
      request = AppFabricTestsSuite.getPut("/v2/apps/" + appName);
    }
    request.setHeader(Constants.Gateway.CONTINUUITY_API_KEY, "api-key-example");
    request.setHeader("X-Archive-Name", application.getSimpleName() + ".jar");
    request.setEntity(new ByteArrayEntity(bos.toByteArray()));
    return AppFabricTestsSuite.execute(request);
  }

  /**
   * Tests deploying an application.
   */
  @Test
  public void testDeploy() throws Exception {
    HttpResponse response = deploy(WordCountApp.class);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  /**
   * Tests taking a snapshot of the transaction manager.
   */
  @Test
  public void testTxManagerSnapshot() throws Exception {
    Long currentTs = System.currentTimeMillis();

    HttpResponse response = AppFabricTestsSuite.doGet("/v2/transactions/state");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    InputStream in = response.getEntity().getContent();
    try {
      SnapshotCodecV2 codec = new SnapshotCodecV2();
      TransactionSnapshot snapshot = codec.decodeState(in);
      Assert.assertTrue(snapshot.getTimestamp() >= currentTs);
    } finally {
      in.close();
    }
  }

  /**
   * Tests invalidating a transaction.
   * @throws Exception
   */
  @Test
  public void testInvalidateTx() throws Exception {
    TransactionSystemClient txClient = AppFabricTestsSuite.getTxClient();

    Transaction tx1 = txClient.startShort();
    HttpResponse response = AppFabricTestsSuite.doPost("/v2/transactions/" + tx1.getWritePointer() + "/invalidate");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    Transaction tx2 = txClient.startShort();
    txClient.commit(tx2);
    response = AppFabricTestsSuite.doPost("/v2/transactions/" + tx2.getWritePointer() + "/invalidate");
    Assert.assertEquals(409, response.getStatusLine().getStatusCode());

    Assert.assertEquals(400, AppFabricTestsSuite.doPost("/v2/transactions/foobar/invalidate")
                               .getStatusLine().getStatusCode());
  }

  @Test
  public void testResetTxManagerState() throws Exception {
    HttpResponse response = AppFabricTestsSuite.doPost("/v2/transactions/state");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  /**
   * Tests deploying an application.
   */
  @Test
  public void testDeployInvalid() throws Exception {
    HttpResponse response = deploy(String.class);
    Assert.assertEquals(400, response.getStatusLine().getStatusCode());
    Assert.assertNotNull(response.getEntity());
    Assert.assertTrue(response.getEntity().getContentLength() > 0);
  }

  /**
   * Tests deleting an application.
   */
  @Test
  public void testDelete() throws Exception {
    //Delete an invalid app
    HttpResponse response = AppFabricTestsSuite.doDelete("/v2/apps/XYZ");
    Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    deploy(WordCountApp.class);
    getRunnableStartStop("flows", "WordCountApp", "WordCountFlow", "start");
    //Try to delete an App while its flow is running
    response = AppFabricTestsSuite.doDelete("/v2/apps/WordCountApp");
    Assert.assertEquals(403, response.getStatusLine().getStatusCode());
    getRunnableStartStop("flows", "WordCountApp", "WordCountFlow", "stop");
    //Delete the App after stopping the flow
    response = AppFabricTestsSuite.doDelete("/v2/apps/WordCountApp");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    response = AppFabricTestsSuite.doDelete("/v2/apps/WordCountApp");
    Assert.assertEquals(404, response.getStatusLine().getStatusCode());
  }

  /**
   * Tests for program list calls
   */
  @Test
  public void testProgramList() throws Exception {
    //Test :: /flows /procedures /mapreduce /workflows
    //App  :: /apps/AppName/flows /procedures /mapreduce /workflows
    //App Info :: /apps/AppName
    //All Apps :: /apps

    HttpResponse response = AppFabricTestsSuite.doGet("/v2/flows");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    response = AppFabricTestsSuite.doGet("/v2/procedures");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    response = AppFabricTestsSuite.doGet("/v2/mapreduce");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    response = AppFabricTestsSuite.doGet("/v2/workflows");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    deploy(WordCountApp.class);
    deploy(DummyAppWithTrackingTable.class);
    response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/flows");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    String json = EntityUtils.toString(response.getEntity());
    List<Map<String, String>> flows = new Gson().fromJson(json, LIST_MAP_STRING_STRING_TYPE);
    Assert.assertEquals(1, flows.size());

    response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/procedures");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    json = EntityUtils.toString(response.getEntity());
    List<Map<String, String>> procedures = new Gson().fromJson(json, LIST_MAP_STRING_STRING_TYPE);
    Assert.assertEquals(1, procedures.size());

    response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/mapreduce");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    json = EntityUtils.toString(response.getEntity());
    List<Map<String, String>> mapreduce = new Gson().fromJson(json, LIST_MAP_STRING_STRING_TYPE);
    Assert.assertEquals(1, mapreduce.size());

    response = AppFabricTestsSuite.doGet("/v2/apps/WordCountApp/workflows");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    response = AppFabricTestsSuite.doGet("/v2/apps");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    response = AppFabricTestsSuite.doDelete("/v2/apps/dummy");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  /**
   * Test for schedule handlers.
   */
  @Test
  public void testScheduleEndPoints() throws Exception {
    // Steps for the test:
    // 1. Deploy the app
    // 2. Verify the schedules
    // 3. Verify the history after waiting a while
    // 4. Suspend the schedule
    // 5. Verify there are no runs after the suspend by looking at the history
    // 6. Resume the schedule
    // 7. Verify there are runs after the resume by looking at the history
    HttpResponse response = deploy(AppWithSchedule.class);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    response = AppFabricTestsSuite.doGet("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/schedules");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    String json = EntityUtils.toString(response.getEntity());
    List<String> schedules = new Gson().fromJson(json, new TypeToken<List<String>>() { }.getType());
    Assert.assertEquals(1, schedules.size());
    String scheduleId = schedules.get(0);
    Assert.assertNotNull(scheduleId);
    Assert.assertFalse(scheduleId.isEmpty());

    TimeUnit.SECONDS.sleep(5);
    response = AppFabricTestsSuite.doGet("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/history");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    json = EntityUtils.toString(response.getEntity());
    List<Map<String, String>> history = new Gson().fromJson(json, LIST_MAP_STRING_STRING_TYPE);

    int workflowRuns = history.size();
    Assert.assertTrue(workflowRuns >= 1);

    //Check suspend status
    String scheduleStatus = String.format("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/schedules/%s/status",
                                          scheduleId);
    response = AppFabricTestsSuite.doGet(scheduleStatus);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    json = EntityUtils.toString(response.getEntity());
    Map<String, String> output = new Gson().fromJson(json, MAP_STRING_STRING_TYPE);
    Assert.assertEquals("SCHEDULED", output.get("status"));

    String scheduleSuspend = String.format("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/schedules/%s/suspend",
                                           scheduleId);

    response = AppFabricTestsSuite.doPost(scheduleSuspend);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    //check paused state
    scheduleStatus = String.format("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/schedules/%s/status", scheduleId);
    response = AppFabricTestsSuite.doGet(scheduleStatus);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    json = EntityUtils.toString(response.getEntity());
    output = new Gson().fromJson(json, MAP_STRING_STRING_TYPE);
    Assert.assertEquals("SUSPENDED", output.get("status"));

    TimeUnit.SECONDS.sleep(2); //wait till any running jobs just before suspend call completes.

    response = AppFabricTestsSuite.doGet("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/history");
    json = EntityUtils.toString(response.getEntity());
    history = new Gson().fromJson(json,
                                  LIST_MAP_STRING_STRING_TYPE);
    workflowRuns = history.size();

    //Sleep for some time and verify there are no more scheduled jobs after the suspend.
    TimeUnit.SECONDS.sleep(10);

    response = AppFabricTestsSuite.doGet("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/history");
    json = EntityUtils.toString(response.getEntity());
    history = new Gson().fromJson(json,
                                  LIST_MAP_STRING_STRING_TYPE);
    int workflowRunsAfterSuspend = history.size();
    Assert.assertEquals(workflowRuns, workflowRunsAfterSuspend);

    String scheduleResume = String.format("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/schedules/%s/resume",
                                          scheduleId);

    response = AppFabricTestsSuite.doPost(scheduleResume);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    //Sleep for some time and verify there are no more scheduled jobs after the pause.
    TimeUnit.SECONDS.sleep(3);
    response = AppFabricTestsSuite.doGet("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/history");
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    json = EntityUtils.toString(response.getEntity());
    history = new Gson().fromJson(json,
                                  LIST_MAP_STRING_STRING_TYPE);

    int workflowRunsAfterResume = history.size();
    //Verify there is atleast one run after the pause
    Assert.assertTrue(workflowRunsAfterResume > workflowRunsAfterSuspend + 1);

    //check scheduled state
    scheduleStatus = String.format("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/schedules/%s/status", scheduleId);
    response = AppFabricTestsSuite.doGet(scheduleStatus);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    json = EntityUtils.toString(response.getEntity());
    output = new Gson().fromJson(json, MAP_STRING_STRING_TYPE);
    Assert.assertEquals("SCHEDULED", output.get("status"));

    //Check status of a non existing schedule
    String notFoundSchedule = String.format("/v2/apps/AppWithSchedule/workflows/SampleWorkflow/schedules/%s/status",
                                            "invalidId");

    response = AppFabricTestsSuite.doGet(notFoundSchedule);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    json = EntityUtils.toString(response.getEntity());
    output = new Gson().fromJson(json, MAP_STRING_STRING_TYPE);
    Assert.assertEquals("NOT_FOUND", output.get("status"));
  }

  /**
   * Test for resetting app.
   */
  @Test
  public void testUnRecoverableReset() throws Exception {
    try {
      HttpResponse response = deploy(WordCountApp.class);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      response = AppFabricTestsSuite.doPost("/v2/unrecoverable/reset");
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    } finally {
      Assert.assertEquals(200, AppFabricTestsSuite.doDelete("/v2/apps").getStatusLine().getStatusCode());
    }
    // make sure that after reset (no apps), list apps returns 200, and not 404
    Assert.assertEquals(200, AppFabricTestsSuite.doGet("/v2/apps").getStatusLine().getStatusCode());
  }


  /**
   * Test for resetting app.
   */
  @Test
  public void testUnRecoverableResetAppRunning() throws Exception {

    HttpResponse response = deploy(WordCountApp.class);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    Assert.assertEquals(200, getRunnableStartStop("flows", "WordCountApp", "WordCountFlow", "start"));
    Assert.assertEquals("RUNNING", getRunnableStatus("flows", "WordCountApp", "WordCountFlow"));
    response = AppFabricTestsSuite.doPost("/v2/unrecoverable/reset");
    Assert.assertEquals(400, response.getStatusLine().getStatusCode());
    Assert.assertEquals(200, getRunnableStartStop("flows", "WordCountApp", "WordCountFlow", "stop"));
  }

}
