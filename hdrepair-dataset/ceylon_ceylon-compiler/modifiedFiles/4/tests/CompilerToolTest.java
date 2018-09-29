/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.redhat.ceylon.tools.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.redhat.ceylon.common.tool.OptionArgumentException;
import com.redhat.ceylon.common.tool.ToolFactory;
import com.redhat.ceylon.common.tool.ToolLoader;
import com.redhat.ceylon.common.tool.ToolModel;
import com.redhat.ceylon.common.tools.CeylonToolLoader;
import com.redhat.ceylon.compiler.CeylonCompileTool;
import com.redhat.ceylon.compiler.CompilerErrorException;
import com.redhat.ceylon.compiler.SystemErrorException;
import com.redhat.ceylon.compiler.java.test.CompilerTest;

public class CompilerToolTest extends CompilerTest {
    
    protected final ToolFactory pluginFactory = new ToolFactory();
    protected final ToolLoader pluginLoader = new CeylonToolLoader(null);
    
    private List<String> options(String... strings){
        List<String> ret = new ArrayList<String>(strings.length+2);
        for(String s : strings)
            ret.add(s);
        ret.add("--out");
        ret.add(destDir);
        ret.add("--javac=-cp="+getClassPathAsPath());
        return ret;
    }
    
    @Test
    public void testCompile()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.ceylon"));
        tool.run();
    }
    
    @Test
    public void testCompileVerbose()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--verbose", "--src=test/src", "com.redhat.ceylon.tools.test.ceylon"));
        tool.run();
    }
    
    @Test
    public void testCompileMultiple()  throws Exception {
        File carFile1 = getModuleArchive("com.redhat.ceylon.tools.test.multiple.sub1", "1.0");
        carFile1.delete();
        File carFile2 = getModuleArchive("com.redhat.ceylon.tools.test.multiple.sûb2", "1.0");
        carFile2.delete();
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.multiple.*"));
        tool.run();
        assertTrue(carFile1.exists() && carFile2.exists());
    }
    
    @Test
    public void testCompileNoSuchModule()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        try {
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--src=test/src", "com.redhat.ceylon.tools.test.nosuchmodule"));
            Assert.fail();
        } catch (OptionArgumentException e) {
            Assert.assertEquals("Module com.redhat.ceylon.tools.test.nosuchmodule not found in source directories: test" + File.separator + "src", e.getMessage());   
        }
    }
    
    @Test
    public void testCompileNoSuchModuleDotJava()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        try {
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--src=test/src", "com.redhat.ceylon.tools.test.nosuchmodule.java"));
            Assert.fail();
        } catch (OptionArgumentException e) {
            Assert.assertEquals("file not found: com.redhat.ceylon.tools.test.nosuchmodule.java", e.getMessage());   
        }
    }
    
    @Test
    public void testCompileNoSuchModuleDotCeylon()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        try {
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--src=test/src", "com.redhat.ceylon.tools.test.nosuchmodule.ceylon"));
            Assert.fail();
        } catch (OptionArgumentException e) {
            Assert.assertEquals("file not found: com.redhat.ceylon.tools.test.nosuchmodule.ceylon", e.getMessage());   
        }
    }
    
    @Test
    public void testCompileValidEncoding()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);        
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "--encoding=UTF-8", "com.redhat.ceylon.tools.test.ceylon"));
    }
    
    @Test
    public void testCompileNoSuchEncoding()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        try {
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--src=test/src", "--encoding=foo", "com.redhat.ceylon.tools.test.ceylon"));
            Assert.fail();
        } catch (OptionArgumentException e) {
            Assert.assertEquals("Unsupported encoding: foo", e.getMessage());   
        }
    }
    
    @Test
    public void testCompileModuleAndVersion()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        try {
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--src=test/src", "com.redhat.ceylon.tools.test.ceylon/1.0"));
            Assert.fail();
        } catch (OptionArgumentException e) {
            Assert.assertEquals("Invalid module name or source file: com.redhat.ceylon.tools.test.ceylon/1.0\n"
                                +"Module names should not contain any version part.\n"
                                +"Source file names should be given relative to the current directory.", e.getMessage());   
        }
        
    }
    
    @Test
    public void testCompileWithSyntaxErrors()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.syntax"));
        try{
            tool.run();
            Assert.fail("Tool should have thrown an exception");
        }catch(CompilerErrorException x){
            Assert.assertEquals("There was 1 error", x.getMessage());
        }catch(Throwable t){
            t.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }
    
    @Test
    public void testCompileWithAnalysisErrors()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.analysis"));
        try{
            tool.run();
            Assert.fail("Tool should have thrown an exception");
        }catch(CompilerErrorException x){
            Assert.assertEquals("There were 3 errors", x.getMessage());
        }catch(Throwable t){
            t.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }
  
    @Test
    public void testCompileWithErroneous()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.erroneous"));
        try{
            tool.run();
            Assert.fail("Tool should have thrown an exception");
        }catch(CompilerErrorException x){
            Assert.assertEquals("There was 1 error", x.getMessage());
        }catch(Throwable t){
            t.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }
    
    @Test
    public void testCompileWithRuntimeException()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.runtimeex"));
        try{
            tool.run();
            Assert.fail("Tool should have thrown an exception");
        }catch(CompilerErrorException x){
            Assert.assertEquals("There was 1 error", x.getMessage());
        }catch(Throwable t){
            t.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }
    
    @Test
    public void testCompileWithOomeException()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.oome"));
        try{
            tool.run();
            Assert.fail("Tool should have thrown an exception");
        }catch(SystemErrorException x){
            Assert.assertEquals("java.lang.OutOfMemoryError", x.getMessage());
        }
    }
    
    @Test
    public void testCompileWithStackOverflowError()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.stackoverflow"));
        try{
            tool.run();
            Assert.fail("Tool should have thrown an exception");
        }catch(SystemErrorException x){
            Assert.assertEquals("java.lang.StackOverflowError", x.getMessage());
        }
    }
    
    @Test
    public void testBug1179()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        try{
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--src=test/src", "3"));
            Assert.fail("Tool should have thrown an exception");
        }catch(OptionArgumentException x){
            Assert.assertEquals("Invalid module name or source file: 3\n"
                                +"Module names should not contain any version part.\n"
                                +"Source file names should be given relative to the current directory.", x.getMessage());
        }
    }
    
    @Test
    public void testValidatingJavaOptions()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        try{
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--javac=-target=foo", "com.example"));
            Assert.fail("Tool should have thrown an exception");
        }catch(OptionArgumentException x){
            Assert.assertEquals("Invalid --javac option: -target: invalid target release: foo", x.getMessage());
        }
        
        try{
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--javac=-source=foo", "com.example"));
            Assert.fail("Tool should have thrown an exception");
        }catch(OptionArgumentException x){
            Assert.assertEquals("Invalid --javac option: -source: invalid source release: foo", x.getMessage());
        }
        
        try{
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--javac=-monkey", "com.example"));
            Assert.fail("Tool should have thrown an exception");
        }catch(OptionArgumentException x){
            Assert.assertEquals("Unknown --javac option: -monkey", x.getMessage());
        }
        
        {
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--javac=-Xlint:cast", 
                            "--src=test/src", "com.redhat.ceylon.tools.test.ceylon"));
        }
        
        try{
            CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                    options("--javac=-Xlint:monkey", 
                            "--src=test/src", "com.redhat.ceylon.tools.test.ceylon"));
            Assert.fail("Tool should have thrown an exception");
        }catch(OptionArgumentException x){
            Assert.assertEquals("Unknown --javac option: -Xlint:monkey", x.getMessage());
        }
    }
    
    @Test
    public void testBug1183()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.bug1183"));
        try {
            tool.run();
            Assert.fail("Tool should have thrown an exception");
        } catch (CompilerErrorException e) {
            // We expect this, not a FatalToolError
        }
        
    }
    
    @Test
    public void testBadIntegerLiteral()  throws Exception {
        ToolModel<CeylonCompileTool> model = pluginLoader.loadToolModel("compile");
        Assert.assertNotNull(model);
        CeylonCompileTool tool = pluginFactory.bindArguments(model, 
                options("--src=test/src", "com.redhat.ceylon.tools.test.badintegerliteral"));
        try {
            tool.run();
            Assert.fail("Tool should have thrown an exception");
        } catch (CompilerErrorException e) {
            // We expect this, not a FatalToolError
        }
        
    }
}
