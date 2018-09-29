/*
 * Copyright 2008 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.struts2.dom.struts;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Tests for &lt;include&gt;.
 *
 * @author Yann C&eacute;bron
 */
public class StrutsIncludeTest extends BasicStrutsHighlightingTestCase<JavaModuleFixtureBuilder> {

  @NotNull
  protected String getTestDataLocation() {
    return "strutsXmlInclude";
  }

  protected void configureModule(final JavaModuleFixtureBuilder moduleBuilder) throws Exception {
    final String path = myFixture.getTempDirPath();
    moduleBuilder.addContentRoot(path);
    new File(path + SOURCE_PATH).mkdir();
    moduleBuilder.addSourceRoot(SOURCE_PATH);
  }

  public void testInclude() throws Throwable {
    performHighlightingTest("src/struts.xml",
                            "src/struts-include.xml",
                            "src/com/test/struts-sub.xml");
  }

  public void testIncludeNotInFileset() throws Throwable {
    performHighlightingTest("src/struts-notinfileset.xml",
                            "src/com/test/struts-sub.xml");
  }

}