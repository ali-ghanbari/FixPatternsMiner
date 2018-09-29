package org.apache.accumulo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map.Entry;

import jline.ConsoleReader;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.file.FileOperations;
import org.apache.accumulo.core.file.FileSKVWriter;
import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.accumulo.core.util.shell.Shell;
import org.apache.accumulo.server.trace.TraceServer;
import org.apache.accumulo.test.MiniAccumuloCluster;
import org.apache.accumulo.test.MiniAccumuloConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.tools.DistCp;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ShellServerTest {
  public static class TestOutputStream extends OutputStream {
    StringBuilder sb = new StringBuilder();
    
    @Override
    public void write(int b) throws IOException {
      sb.append((char) (0xff & b));
    }
    
    public String get() {
      return sb.toString();
    }
    
    public void clear() {
      sb.setLength(0);
    }
  }
  
  private static String secret = "superSecret";
  public static TemporaryFolder folder = new TemporaryFolder();
  public static MiniAccumuloCluster cluster;
  public static TestOutputStream output;
  public static Shell shell;
  private static Process traceProcess;
  
  static String exec(String cmd) throws IOException {
    output.clear();
    shell.execCommand(cmd, true, true);
    return output.get();
  }
  
  static String exec(String cmd, boolean expectGoodExit) throws IOException {
    String result = exec(cmd);
    if (expectGoodExit)
      assertGoodExit("", true);
    else
      assertBadExit("", true);
    return result;
  }
  
  static String exec(String cmd, boolean expectGoodExit, String expectString) throws IOException {
    return exec(cmd, expectGoodExit, expectString, true);
  }
  
  static String exec(String cmd, boolean expectGoodExit, String expectString, boolean stringPresent) throws IOException {
    String result = exec(cmd);
    if (expectGoodExit)
      assertGoodExit(expectString, stringPresent);
    else
      assertBadExit(expectString, stringPresent);
    return result;
  }
  
  static void assertGoodExit(String s, boolean stringPresent) {
    Shell.log.debug(output.get());
    assertEquals(shell.getExitCode(), 0);
    if (s.length() > 0)
      assertEquals(s + " present in " + output.get() + " was not " + stringPresent, stringPresent, output.get().contains(s));
  }
  
  static void assertBadExit(String s, boolean stringPresent) {
    Shell.log.debug(output.get());
    assertTrue(shell.getExitCode() > 0);
    if (s.length() > 0)
      assertEquals(s + " present in " + output.get() + " was not " + stringPresent, stringPresent, output.get().contains(s));
    shell.resetExitCode();
  }
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    folder.create();
    MiniAccumuloConfig cfg = new MiniAccumuloConfig(folder.newFolder("miniAccumulo"), secret);
    cluster = new MiniAccumuloCluster(cfg);
    cluster.start();
    
    System.setProperty("HOME", folder.getRoot().getAbsolutePath());
    
    // start the shell
    output = new TestOutputStream();
    shell = new Shell(new ConsoleReader(new FileInputStream(FileDescriptor.in), new OutputStreamWriter(output)));
    shell.setLogErrorsToConsole();
    shell.config("-u", "root", "-p", secret, "-z", cluster.getInstanceName(), cluster.getZooKeepers());
    exec("quit", true);
    shell.start();
    shell.setExit(false);
    traceProcess = cluster.exec(TraceServer.class);
  }
  
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    cluster.stop();
    traceProcess.destroy();
    // folder.delete();
  }
  
  @Test(timeout = 30000)
  public void exporttableImporttable() throws Exception {
    // exporttable / importtable
    exec("createtable t -evc", true);
    make10();
    exec("addsplits row5", true);
    exec("config -t t -s table.split.threshold=345M", true);
    exec("offline t", true);
    String export = folder.newFolder().getName();
    exec("exporttable -t t " + export, true);
    DistCp cp = new DistCp(new Configuration());
    String import_ = folder.newFolder().getName();
    cp.run(new String[] {"-f", export + "/distcp.txt", import_});
    exec("importtable t2 " + import_, true);
    exec("config -t t2 -np", true, "345M", true);
    exec("getsplits -t t2", true, "row5", true);
    exec("constraint --list -t t2", true, "VisibilityConstraint=1", true);
    exec("onlinetable t", true);
    exec("deletetable -f t", true);
    exec("deletetable -f t2", true);
  }
  
  @Test(timeout = 30000)
  public void setscaniterDeletescaniter() throws Exception {
    // setscaniter, deletescaniter
    exec("createtable t");
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    shell.getReader().setInput(new ByteArrayInputStream("true\n\n\nSTRING\n".getBytes()));
    exec("setscaniter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n name", true);
    exec("scan", true, "3", true);
    exec("deletescaniter -n name", true);
    exec("scan", true, "1", true);
    exec("deletetable -f t");
    
  }
  
  @Test(timeout = 30000)
  public void execfile() throws Exception {
    // execfile
    File file = folder.newFile();
    PrintWriter writer = new PrintWriter(file.getAbsolutePath());
    writer.println("about");
    writer.close();
    exec("execfile " + file.getAbsolutePath(), true, Constants.VERSION, true);
    
  }
  
  @Test(timeout = 30000)
  public void egrep() throws Exception {
    // egrep
    exec("createtable t");
    make10();
    String lines = exec("egrep row[123]", true);
    assertTrue(lines.split("\n").length - 1 == 3);
    exec("deletetable -f t");
  }
  
  @Test(timeout = 30000)
  public void du() throws Exception {
    // du
    exec("createtable t");
    make10();
    exec("flush -t t -w");
    exec("du t", true, " [t]", true);
    exec("deletetable -f t");
  }
  
  @Test(timeout = 30000)
  public void user() throws Exception {
    // createuser, deleteuser, user, users, droptable
    shell.getReader().setInput(new ByteArrayInputStream("secret\nsecret\n".getBytes()));
    exec("createuser xyzzy", true);
    exec("users", true, "xyzzy", true);
    exec("grant -u xyzzy -s System.CREATE_TABLE", true);
    shell.getReader().setInput(new ByteArrayInputStream("secret\nsecret\n".getBytes()));
    exec("user xyzzy", true);
    exec("createtable t", true, "xyzzy@", true);
    exec("insert row1 cf cq 1", true);
    exec("scan", true, "row1", true);
    exec("droptable -f t", true);
    exec("deleteuser xyzzy", false, "delete yourself", true);
    shell.getReader().setInput(new ByteArrayInputStream((secret + "\n" + secret + "\n").getBytes()));
    exec("user root", true);
    exec("deleteuser xyzzy", true);
    exec("users", true, "xyzzy", false);
  }
  
  @Test(timeout = 30000)
  public void iter() throws Exception {
    // setshelliter, listshelliter, deleteshelliter
    exec("createtable t");
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    shell.getReader().setInput(new ByteArrayInputStream("true\n\n\nSTRING\n".getBytes()));
    exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -pn sum -n name", true);
    shell.getReader().setInput(new ByteArrayInputStream("true\n\n\nSTRING\n".getBytes()));
    exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -pn sum -n xyzzy", true);
    exec("scan -pn sum", true, "3", true);
    exec("listshelliter", true, "Iterator name", true);
    exec("listshelliter", true, "Iterator xyzzy", true);
    exec("listshelliter", true, "Profile : sum", true);
    exec("deleteshelliter -pn sum -n name", true);
    exec("listshelliter", true, "Iterator name", false);
    exec("listshelliter", true, "Iterator xyzzy", true);
    exec("deleteshelliter -pn sum -a", true);
    exec("listshelliter", true, "Iterator xyzzy", false);
    exec("listshelliter", true, "Profile : sum", false);
    exec("deletetable -f t");
    // list iter
    exec("createtable t");
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    shell.getReader().setInput(new ByteArrayInputStream("true\n\n\nSTRING\n".getBytes()));
    exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n name", true);
    shell.getReader().setInput(new ByteArrayInputStream("true\n\n\nSTRING\n".getBytes()));
    exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -n xyzzy", true);
    exec("scan", true, "3", true);
    exec("listiter -scan", true, "Iterator name", true);
    exec("listiter -scan", true, "Iterator xyzzy", true);
    exec("listiter -minc", true, "Iterator name", false);
    exec("listiter -minc", true, "Iterator xyzzy", false);
    exec("deleteiter -scan -n name", true);
    exec("listiter -scan", true, "Iterator name", false);
    exec("listiter -scan", true, "Iterator xyzzy", true);
    exec("deletetable -f t");
    
  }
  
  @Test(timeout = 30000)
  public void notable() throws Exception {
    // notable
    exec("createtable xyzzy", true);
    exec("scan", true, " xyzzy>", true);
    assertTrue(output.get().contains(" xyzzy>"));
    exec("notable", true);
    exec("scan", false, "Not in a table context.", true);
    assertFalse(output.get().contains(" xyzzy>"));
    exec("deletetable -f xyzzy");
  }
  
  @Test(timeout = 30000)
  public void sleep() throws Exception {
    // sleep
    long now = System.currentTimeMillis();
    exec("sleep 0.2", true);
    long diff = System.currentTimeMillis() - now;
    assertTrue(diff >= 200);
    assertTrue(diff < 400);
  }
  
  @Test(timeout = 30000)
  public void addauths() throws Exception {
    // addauths
    exec("createtable xyzzy -evc");
    exec("insert a b c d -l foo", true, "does not have authorization", true);
    exec("addauths -s foo,bar", true);
    exec("getauths", true, "foo,bar", true);
    exec("insert a b c d -l foo");
    exec("scan", true, "[foo]");
    exec("scan -s bar", true, "[foo]", false);
    exec("deletetable -f xyzzy");
  }
  
  @Test(timeout = 30000)
  public void byeQuitExit() throws Exception {
    // bye, quit, exit
    for (String cmd : "bye quit exit".split(" ")) {
      assertFalse(shell.getExit());
      exec(cmd);
      assertTrue(shell.getExit());
      shell.setExit(false);
    }
  }
  
  @Test(timeout = 30000)
  public void classpath() throws Exception {
    // classpath
    exec("classpath", true, "Level 2 URL classpath items are", true);
  }
  
  @Test(timeout = 30000)
  public void clearCls() throws Exception {
    // clear/cls
    exec("cls", true, "[1;1H");
    exec("clear", true, "[2J");
  }
  
  @Test(timeout = 30000)
  public void clonetable() throws Exception {
    // clonetable
    exec("createtable orig -evc");
    exec("config -t orig -s table.split.threshold=123M");
    exec("addsplit -t orig a b c");
    exec("insert a b c value");
    exec("scan", true, "value", true);
    exec("clonetable orig clone");
    // verify constraint, config, and splits were cloned
    exec("constraint --list -t clone", true, "VisibilityConstraint=1", true);
    exec("config -t clone -np", true, "123M", true);
    String out = exec("getsplits -t clone"); // , true, "a\nb\nc\n");
    // compact
    exec("createtable c");
    // make two files
    exec("insert a b c d");
    exec("flush -w");
    exec("insert x y z v");
    exec("flush -w");
    int oldCount = countFiles();
    // merge two files into one
    exec("compact -t c -w");
    assertTrue(countFiles() < oldCount);
    exec("addsplits -t c f");
    // make two more files:
    exec("insert m 1 2 3");
    exec("flush -w");
    exec("insert n 1 2 3");
    exec("flush -w");
    oldCount = countFiles();
    // at this point there are 3 files in the default tablet
    // compact some data:
    exec("compact -b g -e z -w");
    assertTrue(countFiles() == oldCount - 2);
    exec("compact -w");
    assertTrue(countFiles() == oldCount - 2);
    exec("merge --all -t c");
    exec("compact -w");
    assertTrue(countFiles() == oldCount - 3);
    exec("deletetable orig");
    exec("deletetable clone");
    exec("deletetable c");
  }
  
  @Test(timeout = 30000)
  public void constraint() throws Exception {
    // constraint
    exec("constraint -l -t !METADATA", true, "MetadataConstraints=1", true);
    exec("createtable c -evc");
    exec("constraint -l -t c", true, "VisibilityConstraint=1", true);
    exec("constraint -t c -d 1", true, "Removed constraint 1 from table c");
    exec("constraint -l -t c", true, "VisibilityConstraint=1", false);
    exec("deletetable -f c");
  }
  
  @Test(timeout = 30000)
  public void deletemany() throws Exception {
    // deletemany
    exec("createtable t");
    make10();
    assertEquals(10, countkeys("t"));
    exec("deletemany -f -b row8");
    assertEquals(8, countkeys("t"));
    exec("scan -t t -np", true, "row8", false);
    make10();
    exec("deletemany -f -b row4 -e row5");
    assertEquals(8, countkeys("t"));
    make10();
    exec("deletemany -f -c cf:col4,cf:col5");
    assertEquals(8, countkeys("t"));
    make10();
    exec("deletemany -f -r row3");
    assertEquals(9, countkeys("t"));
    make10();
    exec("deletemany -f -r row3");
    assertEquals(9, countkeys("t"));
    make10();
    exec("deletemany -f -b row3 -be -e row5 -ee");
    assertEquals(9, countkeys("t"));
    exec("deletetable -f t");
  }
  
  @Test(timeout = 30000)
  public void deleterows() throws Exception {
    // deleterows
    int base = countFiles();
    exec("createtable t");
    exec("addsplits row5 row7");
    make10();
    exec("flush -w -t t");
    assertTrue(base + 3 == countFiles());
    exec("deleterows -t t -b row5 -e row7", true);
    assertTrue(base + 2 == countFiles());
    exec("deletetable -f t");
  }
  
  @Test(timeout = 30000)
  public void groups() throws Exception {
    exec("createtable t");
    exec("setgroups -t t alpha=a,b,c num=3,2,1");
    exec("getgroups -t t", true, "alpha=a,b,c", true);
    exec("getgroups -t t", true, "num=1,2,3", true);
    exec("deletetable -f t");
  }
  
  @Test(timeout = 30000)
  public void grep() throws Exception {
    exec("createtable t", true);
    make10();
    exec("grep row[123]", true, "row1", false);
    exec("grep row5", true, "row5", true);
    exec("deletetable -f t", true);
  }
  
  @Test(timeout = 30000)
  public void help() throws Exception {
    exec("help -np", true, "Help Commands", true);
  }
  
  // @Test(timeout = 30000)
  public void history() throws Exception {
    exec("history -c", true);
    exec("createtable unusualstring");
    exec("deletetable -f unusualstring");
    exec("history", true, "unusualstring", true);
    exec("history", true, "history", true);
  }
  
  @Test(timeout = 30000)
  public void importDirectory() throws Exception {
    Configuration conf = new Configuration();
    FileSystem fs = FileSystem.get(conf);
    File importDir = folder.newFolder("import");
    String even = new File(importDir, "even.rf").toString();
    String odd = new File(importDir, "odd.rf").toString();
    File errorsDir = folder.newFolder("errors");
    fs.mkdirs(new Path(errorsDir.toString()));
    AccumuloConfiguration aconf = AccumuloConfiguration.getDefaultConfiguration();
    FileSKVWriter evenWriter = FileOperations.getInstance().openWriter(even, fs, conf, aconf);
    evenWriter.startDefaultLocalityGroup();
    FileSKVWriter oddWriter = FileOperations.getInstance().openWriter(odd, fs, conf, aconf);
    oddWriter.startDefaultLocalityGroup();
    long ts = System.currentTimeMillis();
    Text cf = new Text("cf");
    Text cq = new Text("cq");
    Value value = new Value("value".getBytes());
    for (int i = 0; i < 100; i += 2) {
      Key key = new Key(new Text(String.format("%8d", i)), cf, cq, ts);
      evenWriter.append(key, value);
      key = new Key(new Text(String.format("%8d", i + 1)), cf, cq, ts);
      oddWriter.append(key, value);
    }
    evenWriter.close();
    oddWriter.close();
    exec("createtable t", true);
    exec("importdirectory " + importDir + " " + errorsDir + " true", true);
    exec("scan -r 00000000", true, "00000000", true);
    exec("scan -r 00000099", true, "00000099", true);
    exec("deletetable -f t");
  }
  
  @Test(timeout = 30000)
  public void info() throws Exception {
    exec("info", true, Constants.VERSION, true);
  }
  
  @Test(timeout = 30000)
  public void interpreter() throws Exception {
    exec("createtable t", true);
    exec("interpreter -l", true, "HexScan", false);
    exec("insert \\x02 cf cq value", true);
    exec("scan -b 02", true, "value", false);
    exec("interpreter -i org.apache.accumulo.core.util.interpret.HexScanInterpreter", true);
    exec("interpreter -l", true, "HexScan", true);
    exec("scan -b 02", true, "value", true);
    exec("deletetable -f t", true);
  }
  
  @Test(timeout = 30000)
  public void listcompactions() throws Exception {
    exec("createtable t", true);
    exec("config -t t -s table.iterator.minc.slow=30,org.apache.accumulo.test.functional.SlowIterator", true);
    exec("config -t t -s table.iterator.minc.slow.opt.sleepTime=100", true);
    exec("insert a cf cq value", true);
    exec("insert b cf cq value", true);
    exec("insert c cf cq value", true);
    exec("insert d cf cq value", true);
    exec("flush -t t", true);
    exec("sleep 0.2", true);
    exec("listcompactions", true, "default_tablet");
    String[] lines = output.get().split("\n");
    String last = lines[lines.length - 1];
    String[] parts = last.split("\\|");
    assertEquals(12, parts.length);
    exec("deletetable -f t", true);
  }
  
  @Test(timeout = 30000)
  public void maxrow() throws Exception {
    exec("createtable t", true);
    exec("insert a cf cq value", true);
    exec("insert b cf cq value", true);
    exec("insert ccc cf cq value", true);
    exec("insert zzz cf cq value", true);
    exec("maxrow", true, "zzz", true);
    exec("delete zzz cf cq", true);
    exec("maxrow", true, "ccc", true);
    exec("deletetable -f t", true);
  }
  
  @Test(timeout = 30000)
  public void merge() throws Exception {
    exec("createtable t");
    exec("addsplits a m z");
    exec("getsplits", true, "z", true);
    exec("merge -f", true);
    exec("getsplits", true, "z", false);
    exec("deletetable -f t");
    exec("getsplits -t !METADATA", true);
    assertEquals(3, output.get().split("\n").length);
    exec("merge -f -t !METADATA");
    exec("getsplits -t !METADATA", true);
    assertEquals(2, output.get().split("\n").length);
  }
  
  @Test(timeout = 30000)
  public void ping() throws Exception {
    exec("ping", true, "OK", true);
    assertEquals(3, output.get().split("\n").length);
  }
  
  @Test(timeout = 30000)
  public void renametable() throws Exception {
    exec("createtable aaaa");
    exec("insert this is a value");
    exec("renametable aaaa xyzzy");
    exec("tables", true, "xyzzy", true);
    exec("tables", true, "aaaa", false);
    exec("scan -t xyzzy", true, "value", true);
    exec("deletetable -f xyzzy", true);
  }
  
  @Test(timeout = 30000)
  public void systempermission() throws Exception {
    exec("systempermissions");
    assertEquals(8, output.get().split("\n").length - 1);
  }
  
  @Test(timeout = 30000)
  public void listscans() throws Exception {
    exec("createtable t", true);
    exec("config -t t -s table.iterator.scan.slow=30,org.apache.accumulo.test.functional.SlowIterator", true);
    exec("config -t t -s table.iterator.scan.slow.opt.sleepTime=100", true);
    exec("insert a cf cq value", true);
    exec("insert b cf cq value", true);
    exec("insert c cf cq value", true);
    exec("insert d cf cq value", true);
    Thread thread = new Thread() {
      public void run() {
        try {
          ZooKeeperInstance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
          Connector connector = instance.getConnector("root", new PasswordToken(secret));
          Scanner s = connector.createScanner("t", Constants.NO_AUTHS);
          for (@SuppressWarnings("unused") Entry<Key,Value> kv : s)
            ;
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    };
    thread.start();
    exec("sleep 0.1", true);
    System.out.println(exec("listscans", true));
    String lines[] = output.get().split("\n");
    String last = lines[lines.length - 1];
    assertTrue(last.contains("RUNNING"));
    String parts[] = last.split("\\|");
    assertEquals(13, parts.length);
    thread.join();
    exec("deletetable -f t", true);
  }
  
  //@Test(timeout = 60000)
  public void trace() throws Exception {
    exec("sleep 1", true);
    exec("trace on", true);
    exec("createtable t", true);
    System.out.println(exec("trace off"));
    exec("table trace");
    System.out.println(exec("scan -np"));
    exec("sleep 10");
    System.out.println(exec("scan -np"));
    UtilWaitThread.sleep(60*1000);
  }
  
  private int countkeys(String table) throws IOException {
    exec("scan -np -t " + table);
    return output.get().split("\n").length - 1;
  }
  
  private void make10() throws IOException {
    for (int i = 0; i < 10; i++) {
      exec(String.format("insert row%d cf col%d value", i, i));
    }
  }
  
  private int countFiles() throws IOException {
    exec("scan -t !METADATA -np -c file");
    // System.out.println(output.get());
    return output.get().split("\n").length - 1;
  }
  
}
