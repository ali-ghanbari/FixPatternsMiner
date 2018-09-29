/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
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

package org.jetbrains.jet.jvm.compiler;

import com.google.common.base.Predicates;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.compileJavaAndLoadTestNamespaceAndBindingContextFromBinary;

/**
 * @author Pavel Talanov
 */
/*
   LoadJavaTestGenerated should be used instead if possible.
 */
public final class LoadJavaCustomTest extends KotlinTestWithEnvironment {
    @NotNull
    private static final String PATH = "compiler/testData/loadJavaCustom";

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    private void doTest(@NotNull String expectedFileName, @NotNull String... javaFileNames) throws Exception {
        List<File> files = ContainerUtil.map(Arrays.asList(javaFileNames), new Function<String, File>() {
            @Override
            public File fun(String s) {
                return new File(s);
            }
        });
        files.add(ExpectedLoadErrorsUtil.ANNOTATION_SOURCE_FILE);
        File expected = new File(expectedFileName);
        File tmpDir = JetTestUtils.tmpDir(expected.getName());

        Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndBindingContext
                = compileJavaAndLoadTestNamespaceAndBindingContextFromBinary(files, tmpDir, getTestRootDisposable(),
                                                                             ConfigurationKind.JDK_ONLY);

        NamespaceDescriptor javaNamespace = javaNamespaceAndBindingContext.first;
        //NOTE: comparing namespace to file (hack)
        AbstractLoadJavaTest.checkLoadedNamespaces(expected, javaNamespace, javaNamespaceAndBindingContext);
    }

    public void testPackageLocalVisibility() throws Exception {
        String dir = PATH + "/packageLocalVisibility";
        String javaDir = dir + "/java";
        doTest(dir + "/expected.txt",
               javaDir + "/test/JFrame.java",
               javaDir + "/awt/Frame.java");
    }

    public void testInner() throws Exception {
        doSimpleTest();
    }

    public void testStaticFinal() throws Exception {
        String dir = "/staticFinal/";
        doTest(PATH + dir + "expected.txt",
               PATH + dir + "test.java");
    }

    private void doSimpleTest() throws Exception {
        doTest(PATH + "/" + getTestName(true) + ".txt",
               PATH + "/" + getTestName(true) + ".java");
    }

    public void testKotlinSignatureTwoSuperclassesInconsistentGenericTypes() throws Exception {
        String dir = PATH + "/kotlinSignature/";
        doTest(dir + "TwoSuperclassesInconsistentGenericTypes.txt",
               dir + "TwoSuperclassesInconsistentGenericTypes.java");
    }

    public void testKotlinSignatureTwoSuperclassesVarargAndNot() throws Exception {
        String dir = PATH + "/kotlinSignature/";
        doTest(dir + "TwoSuperclassesVarargAndNot.txt",
               dir + "TwoSuperclassesVarargAndNot.java");
    }

    //TODO: move to LoadJavaTestGenerated when possible
    public void testEnum() throws Exception {
        String dir = PATH + "/enum";
        String javaDir = dir + "/java";
        doTest(dir + "/expected.txt",
               javaDir + "/MyEnum.java");
    }

    public void testRawSuperType() throws Exception {
        String dir = PATH + "/rawSuperType/";
        doTest(dir + "RawSuperType.txt",
               dir + "RawSuperType.java");
    }

    public void testSubclassWithRawType() throws Exception {
        String dir = PATH + "/subclassWithRawType/";
        doTest(dir + "SubclassWithRawType.txt",
               dir + "SubclassWithRawType.java");
    }

    public static class SubclassingKotlinInJavaTest extends KotlinTestWithEnvironment {
        @Override
        protected JetCoreEnvironment createEnvironment() {
            File dir = new File(PATH + "/subclassingKotlinInJava");

            CompilerConfiguration configuration = CompileCompilerDependenciesTest.compilerConfigurationForTests(
                    ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, new File(dir, "java"));
            configuration.put(CommonConfigurationKeys.SOURCE_ROOTS_KEY, Arrays.asList(new File(dir, "kotlin").getAbsolutePath()));
            return new JetCoreEnvironment(getTestRootDisposable(), configuration);
        }

        public void testSubclassingKotlinInJava() throws Exception {
            File dir = new File(PATH + "/subclassingKotlinInJava");

            InjectorForJavaSemanticServices injectorForJava = new InjectorForJavaSemanticServices(getProject());

            // we need the same binding trace for resolve from Java and Kotlin
            BindingTrace bindingTrace = injectorForJava.getBindingTrace();

            InjectorForTopDownAnalyzerForJvm injectorForAnalyzer = new InjectorForTopDownAnalyzerForJvm(
                    getProject(),
                    new TopDownAnalysisParameters(Predicates.<PsiFile>alwaysFalse(), false, false, Collections.<AnalyzerScriptParameter>emptyList()),
                    bindingTrace,
                    new ModuleDescriptor(Name.special("<test module>")));

            injectorForAnalyzer.getTopDownAnalyzer().analyzeFiles(getEnvironment().getSourceFiles(), Collections.<AnalyzerScriptParameter>emptyList());

            JavaDescriptorResolver javaDescriptorResolver = injectorForJava.getJavaDescriptorResolver();
            NamespaceDescriptor namespaceDescriptor = javaDescriptorResolver.resolveNamespace(
                    LoadDescriptorUtil.TEST_PACKAGE_FQNAME, DescriptorSearchRule.INCLUDE_KOTLIN);
            assert namespaceDescriptor != null;

            NamespaceComparator.compareNamespaces(namespaceDescriptor, namespaceDescriptor,
                                                  NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT, new File(dir, "expected.txt"));

            ExpectedLoadErrorsUtil.checkForLoadErrors(namespaceDescriptor, bindingTrace.getBindingContext());
        }
    }
}
