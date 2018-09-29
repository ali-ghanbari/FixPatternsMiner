package org.jetbrains.idea.maven.dom;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.idea.maven.MavenImportingTestCase;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

import java.io.File;
import java.util.Properties;

public class PropertyResolverTest extends MavenImportingTestCase {
  public void testResolvingProjectAttributes() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    assertEquals("test", resolve("${project.groupId}", myProjectPom));
    assertEquals("test", resolve("${pom.groupId}", myProjectPom));
  }

  public void testResolvingProjectParentAttributes() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<parent>" +
                  "  <groupId>parent.test</groupId>" +
                  "  <artifactId>parent.project</artifactId>" +
                  "  <version>parent.1</version>" +
                  "</parent>");

    assertEquals("parent.test", resolve("${project.parent.groupId}", myProjectPom));
    assertEquals("parent.test", resolve("${pom.parent.groupId}", myProjectPom));
  }

  public void testResolvingAbsentProperties() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    assertEquals("${project.parent.groupId}", resolve("${project.parent.groupId}", myProjectPom));
  }

  public void testResolvingProjectDirectories() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    assertEquals(new File(getProjectPath(), "target").getPath(),
                 resolve("${project.build.directory}", myProjectPom));
    assertEquals(new File(getProjectPath(), "src/main/java").getPath(),
                 resolve("${project.build.sourceDirectory}", myProjectPom));
  }

  public void testResolvingProjectAndParentProperties() throws Exception {
    createProjectPom("<groupId>test</groupId>" +
                     "<artifactId>project</artifactId>" +
                     "<version>1</version>" +

                     "<properties>" +
                     " <parentProp>parent.value</parentProp>" +
                     "</properties>" +

                     "<modules>" +
                     "  <module>m</module>" +
                     "</modules>");

    VirtualFile f = createModulePom("m",
                                    "<groupId>test</groupId>" +
                                    "<artifactId>m</artifactId>" +
                                    "<version>1</version>" +

                                    "<properties>" +
                                    " <moduleProp>module.value</moduleProp>" +
                                    "</properties>" +

                                    "<parent>" +
                                    "  <groupId>test</groupId>" +
                                    "  <artifactId>project</artifactId>" +
                                    "  <version>1</version>" +
                                    "</parent>");

    importProject();

    assertEquals("parent.value", resolve("${parentProp}", f));
    assertEquals("module.value", resolve("${moduleProp}", f));

    assertEquals("parent.value", resolve("${project.parentProp}", f));
    assertEquals("parent.value", resolve("${pom.parentProp}", f));
    assertEquals("module.value", resolve("${project.moduleProp}", f));
    assertEquals("module.value", resolve("${pom.moduleProp}", f));
  }

  public void testProjectPropertiesRecursively() throws Exception {
    createProjectPom("<groupId>test</groupId>" +
                     "<artifactId>project</artifactId>" +
                     "<version>1</version>" +

                     "<properties>" +
                     " <prop1>value</prop1>" +
                     " <prop2>${prop1}-2</prop2>" +
                     " <prop3>${prop2}-3</prop3>" +
                     "</properties>");

    importProject();

    assertEquals("value", resolve("${prop1}", myProjectPom));
    assertEquals("value-2", resolve("${prop2}", myProjectPom));
    assertEquals("value-2-3", resolve("${prop3}", myProjectPom));
  }

  public void testDoNotGoIntoInfiniteRecursion() throws Exception {
    createProjectPom("<groupId>test</groupId>" +
                     "<artifactId>project</artifactId>" +
                     "<version>1</version>" +

                     "<properties>" +
                     " <prop1>${prop1}</prop1>" +

                     " <prop2>${prop3}</prop2>" +
                     " <prop3>${prop2}</prop3>" +

                     " <prop4>${prop5}</prop4>" +
                     " <prop5>${prop6}</prop5>" +
                     " <prop6>${prop4}</prop6>" +

                     "</properties>");

    importProject();
    assertEquals("${prop1}", resolve("${prop1}", myProjectPom));
    assertEquals("${prop3}", resolve("${prop2}", myProjectPom));
    assertEquals("${prop5}", resolve("${prop4}", myProjectPom));
  }

  public void testSophisticatedPropertyNameDoesNotBreakResolver() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    assertEquals("${~!@#$%^&*()}", resolve("${~!@#$%^&*()}", myProjectPom));
    assertEquals("${#ARRAY[@]}", resolve("${#ARRAY[@]}", myProjectPom));
  }

  public void testProjectPropertiesWithProfiles() throws Exception {
    createProjectPom("<groupId>test</groupId>" +
                     "<artifactId>project</artifactId>" +
                     "<version>1</version>" +

                     "<properties>" +
                     " <prop>value1</prop>" +
                     "</properties>" +

                     "<profiles>" +
                     "  <profile>" +
                     "    <id>one</id>" +
                     "    <properties>" +
                     "      <prop>value2</prop>" +
                     "    </properties>" +
                     "  </profile>" +
                     "  <profile>" +
                     "    <id>two</id>" +
                     "    <properties>" +
                     "      <prop>value3</prop>" +
                     "    </properties>" +
                     "  </profile>" +
                     "</profiles>");

    importProject();
    assertEquals("value1", resolve("${prop}", myProjectPom));

    importProjectWithProfiles("one");
    assertEquals("value2", resolve("${prop}", myProjectPom));

    importProjectWithProfiles("two");
    assertEquals("value3", resolve("${prop}", myProjectPom));
  }

  public void testResolvingBasedirProperties() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    assertEquals(getProjectPath(), resolve("${basedir}", myProjectPom));
    assertEquals(getProjectPath(), resolve("${project.basedir}", myProjectPom));
    assertEquals(getProjectPath(), resolve("${pom.basedir}", myProjectPom));
  }

  public void testResolvingSystemProperties() throws Exception {
    String javaHome = System.getProperty("java.home");
    String tempDir = System.getenv("TEMP");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    assertEquals(javaHome, resolve("${java.home}", myProjectPom));
    assertEquals(tempDir, resolve("${env.TEMP}", myProjectPom));
  }

  public void testAllProperties() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    assertEquals("foo test-project bar",
                 resolve("foo ${project.groupId}-${project.artifactId} bar", myProjectPom));
  }

  public void testIncompleteProperties() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    assertEquals("${project.groupId", resolve("${project.groupId", myProjectPom));
    assertEquals("$project.groupId}", resolve("$project.groupId}", myProjectPom));
    assertEquals("{project.groupId}", resolve("{project.groupId}", myProjectPom));
  }

  public void testUncomittedProperties() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    Document doc = FileDocumentManager.getInstance().getDocument(myProjectPom);
    doc.setText(createPomXml("<groupId>test</groupId>" +
                             "<artifactId>project</artifactId>" +
                             "<version>2</version>" +

                             "<properties>" +
                             "  <uncomitted>value</uncomitted>" +
                             "</properties>"));
    PsiDocumentManager.getInstance(myProject).commitDocument(doc);

    assertEquals("value", resolve("${uncomitted}", myProjectPom));
  }

  public void testEscapingProperties() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");

    assertEquals("foo ^project bar",
                 PropertyResolver.resolve(getModule("project"), "foo ^${project.artifactId} bar", new Properties(), "/", null));
    assertEquals("foo ${project.artifactId} bar",
                 PropertyResolver.resolve(getModule("project"), "foo ^^${project.artifactId} bar", new Properties(), "^^", null));
    assertEquals("project ${project.artifactId} project ${project.artifactId}",
                 PropertyResolver.resolve(getModule("project"),
                                          "${project.artifactId} ^${project.artifactId} ${project.artifactId} ^${project.artifactId}",
                                          new Properties(), "^", null));
  }

  public void testEscapingCharacters() throws Exception {
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +
                  "" +
                  "<properties>" +
                  "  <foo>abc:def\\ghi</foo>" +
                  "</properties>");

    assertEquals("abc\\:def\\\\ghi", PropertyResolver.resolve(getModule("project"), "${foo}", new Properties(), null, ":\\"));
  }

  private String resolve(String text, VirtualFile f) {
    Document d = FileDocumentManager.getInstance().getDocument(f);
    PsiFile psi = PsiDocumentManager.getInstance(myProject).getPsiFile(d);

    DomManager domManager = DomManager.getDomManager(myProject);
    DomFileElement<MavenDomProjectModel> dom = domManager.getFileElement((XmlFile)psi, MavenDomProjectModel.class);

    return PropertyResolver.resolve(text, dom);
  }
}
