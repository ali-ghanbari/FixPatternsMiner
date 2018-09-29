/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;

public class TypeMismatchQuickFixTests extends QuickFixTest {
	
	private static final Class THIS= TypeMismatchQuickFixTests.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;


	public TypeMismatchQuickFixTests(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return setUpTest(new TestSuite(THIS));
	}
	
	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}
	
	public static Test suite() {
		return allTests();
	}


	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getFormatterOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		
		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	
	public void testTypeMismatchInVarDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        Thread th= o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        Thread th= (Thread) o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        Object th= o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Thread o) {\n");
		buf.append("        Thread th= o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testTypeMismatchInVarDecl2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");		
		buf.append("public class Container {\n");
		buf.append("    public List[] getLists() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Container.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");				
		buf.append("public class E {\n");
		buf.append("    public void foo(Container c) {\n");
		buf.append("         ArrayList[] lists= c.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");				
		buf.append("public class E {\n");
		buf.append("    public void foo(Container c) {\n");
		buf.append("         ArrayList[] lists= (ArrayList[]) c.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");					
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Container c) {\n");
		buf.append("         List[] lists= c.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");		
		buf.append("import java.util.List;\n");		
		buf.append("public class Container {\n");
		buf.append("    public ArrayList[] getLists() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		

	}
	
	public void testTypeMismatchInVarDecl3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Thread th= foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public Thread foo() {\n");
		buf.append("        Thread th= foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });	
	}	
	
	public void testTypeMismatchInVarDecl4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");		
		buf.append("public class Container {\n");
		buf.append("    public List getLists()[] {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Container.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");				
		buf.append("public class E extends Container {\n");
		buf.append("    public void foo() {\n");
		buf.append("         ArrayList[] lists= super.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");				
		buf.append("public class E extends Container {\n");
		buf.append("    public void foo() {\n");
		buf.append("         ArrayList[] lists= (ArrayList[]) super.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");					
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E extends Container {\n");
		buf.append("    public void foo() {\n");
		buf.append("         List[] lists= super.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");		
		buf.append("import java.util.List;\n");		
		buf.append("public class Container {\n");
		buf.append("    public ArrayList[] getLists() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		

	}
	
	
	public void testTypeMismatchForInterface1() throws Exception {
		
		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test0;\n");
		buf.append("public interface PrimaryContainer {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("PrimaryContainer.java", buf.toString(), false, null);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Container {\n");
		buf.append("    public static Container getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Container.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         PrimaryContainer list= Container.getContainer();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         Container list= Container.getContainer();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         PrimaryContainer list= (PrimaryContainer) Container.getContainer();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("\n");
		buf.append("public class Container {\n");
		buf.append("    public static PrimaryContainer getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");	
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("\n");
		buf.append("public class Container implements PrimaryContainer {\n");
		buf.append("    public static Container getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });		

	}
	
	public void testTypeMismatchForInterface2() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test0;\n");
		buf.append("public interface PrimaryContainer {\n");
		buf.append("    PrimaryContainer duplicate(PrimaryContainer container);\n");
		buf.append("}\n");
		pack0.createCompilationUnit("PrimaryContainer.java", buf.toString(), false, null);
		
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Container {\n");
		buf.append("    public static Container getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Container.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.PrimaryContainer;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(PrimaryContainer primary) {\n");
		buf.append("         primary.duplicate(Container.getContainer());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.PrimaryContainer;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(PrimaryContainer primary) {\n");
		buf.append("         primary.duplicate((PrimaryContainer) Container.getContainer());\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("\n");
		buf.append("public class Container {\n");
		buf.append("    public static PrimaryContainer getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");	
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("\n");
		buf.append("public class Container implements PrimaryContainer {\n");
		buf.append("    public static Container getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test0;\n");
		buf.append("\n");	
		buf.append("import test1.Container;\n");
		buf.append("\n");
		buf.append("public interface PrimaryContainer {\n");
		buf.append("    PrimaryContainer duplicate(Container container);\n");
		buf.append("}\n");
		String expected4= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(4);
		String preview5= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test0;\n");
		buf.append("\n");	
		buf.append("import test1.Container;\n");
		buf.append("\n");
		buf.append("public interface PrimaryContainer {\n");
		buf.append("    PrimaryContainer duplicate(PrimaryContainer container);\n");
		buf.append("\n");
		buf.append("    void duplicate(Container container);\n");
		buf.append("}\n");
		String expected5= buf.toString();
				
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5 }, new String[] { expected1, expected2, expected3, expected4, expected5 });		
	}
	
	public void testTypeMismatchForInterfaceInGeneric() throws Exception {
		
		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test0;\n");
		buf.append("public interface PrimaryContainer<A> {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("PrimaryContainer.java", buf.toString(), false, null);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Container<A> {\n");
		buf.append("    public Container<A> getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Container.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Container<String> c) {\n");
		buf.append("         PrimaryContainer<String> list= c.getContainer();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Container<String> c) {\n");
		buf.append("         Container<String> list= c.getContainer();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Container<String> c) {\n");
		buf.append("         PrimaryContainer<String> list= (PrimaryContainer<String>) c.getContainer();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("\n");
		buf.append("public class Container<A> {\n");
		buf.append("    public PrimaryContainer<String> getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");	
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("\n");
		buf.append("public class Container<A> implements PrimaryContainer<String> {\n");
		buf.append("    public Container<A> getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });		

	}
	
	public void testTypeMismatchForInterfaceInGeneric2() throws Exception {
		
		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test0;\n");
		buf.append("public interface PrimaryContainer<A> {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("PrimaryContainer.java", buf.toString(), false, null);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Container<A> {\n");
		buf.append("    public Container<A> getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Container.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Container<List<?>> c) {\n");
		buf.append("         PrimaryContainer<?> list= c.getContainer();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Container<List<?>> c) {\n");
		buf.append("         Container<List<?>> list= c.getContainer();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Container<List<?>> c) {\n");
		buf.append("         PrimaryContainer<?> list= (PrimaryContainer<?>) c.getContainer();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import test0.PrimaryContainer;\n");
		buf.append("\n");
		buf.append("public class Container<A> {\n");
		buf.append("    public PrimaryContainer<?> getContainer() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		

	}


	public void testTypeMismatchInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int time= System.currentTimeMillis();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int time= (int) System.currentTimeMillis();\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    long time= System.currentTimeMillis();\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}	
	
	public void testTypeMismatchInAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        String str;\n");
		buf.append("        str= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        String str;\n");
		buf.append("        str= (String) iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        Object str;\n");
		buf.append("        str= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		

	}
	
	public void testTypeMismatchInAssignment2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        String str, str2;\n");
		buf.append("        str= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        String str, str2;\n");
		buf.append("        str= (String) iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        Object str;\n");
		buf.append("        String str2;\n");
		buf.append("        str= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		

	}
	
	public void testTypeMismatchInAssignment3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public enum E {\n");
		buf.append("    A, B;\n");
		buf.append("    String str, str2;\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        str2= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public enum E {\n");
		buf.append("    A, B;\n");
		buf.append("    String str, str2;\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        str2= (String) iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public enum E {\n");
		buf.append("    A, B;\n");
		buf.append("    String str;\n");
		buf.append("    Object str2;\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        str2= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testTypeMismatchInExpression() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test0;\n");
		buf.append("public class Other {\n");
		buf.append("    public Object[] toArray() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack0.createCompilationUnit("Other.java", buf.toString(), false, null);

		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.Other;\n");		
		buf.append("public class E {\n");
		buf.append("    public String[] foo(Other other) {\n");
		buf.append("        return other.toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.Other;\n");	
		buf.append("public class E {\n");
		buf.append("    public String[] foo(Other other) {\n");
		buf.append("        return (String[]) other.toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test0.Other;\n");	
		buf.append("public class E {\n");
		buf.append("    public Object[] foo(Other other) {\n");
		buf.append("        return other.toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test0;\n");
		buf.append("public class Other {\n");
		buf.append("    public String[] toArray() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}
	
	public void testCastOnCastExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(List list) {\n");
		buf.append("        ArrayList a= (Cloneable) list;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(List list) {\n");
		buf.append("        ArrayList a= (ArrayList) list;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(List list) {\n");
		buf.append("        Cloneable a= (Cloneable) list;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	
	public void testMismatchingReturnType1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Base {\n");
		buf.append("    public String getName() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Base.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Base {\n");
		buf.append("    public char[] getName() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Base {\n");
		buf.append("    public String getName() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Base {\n");
		buf.append("    public char[] getName() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
						
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testMismatchingReturnType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public interface IBase {\n");
		buf.append("    List getCollection();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("IBase.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E implements IBase {\n");
		buf.append("    public String[] getCollection() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class E implements IBase {\n");
		buf.append("    public List getCollection() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public interface IBase {\n");
		buf.append("    String[] getCollection();\n");
		buf.append("}\n");
		String expected2= buf.toString();
						
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testMismatchingReturnTypeOnGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Base<T extends Number> {\n");
		buf.append("    public String getName(T... t) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Base.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Base<Integer> {\n");
		buf.append("    public char[] getName(Integer... i) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Base<Integer> {\n");
		buf.append("    public String getName(Integer... i) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Base<T extends Number> {\n");
		buf.append("    public char[] getName(T... t) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
						
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testMismatchingReturnTypeOnGeneric2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Base {\n");
		buf.append("    public Number getVal() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Base.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E<T> extends Base {\n");
		buf.append("    public T getVal() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E<T> extends Base {\n");
		buf.append("    public Number getVal() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
						
		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });		
	}


	
	public void testMismatchingExceptions1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface IBase {\n");
		buf.append("    String[] getValues();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("IBase.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E implements IBase {\n");
		buf.append("    public String[] getValues() throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public interface IBase {\n");
		buf.append("    String[] getValues() throws IOException;\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E implements IBase {\n");
		buf.append("    public String[] getValues() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
						
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testMismatchingExceptions2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class Base {\n");
		buf.append("    String[] getValues() throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Base.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.EOFException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E extends Base {\n");
		buf.append("    public String[] getValues() throws EOFException, ParseException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class Base {\n");
		buf.append("    String[] getValues() throws IOException, ParseException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.EOFException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E extends Base {\n");
		buf.append("    public String[] getValues() throws EOFException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
						
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testMismatchingExceptions3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class Base {\n");
		buf.append("    /**\n");
		buf.append("     * @param i The parameter\n");
		buf.append("     *                  More about the parameter\n");
		buf.append("     * @return The returned argument\n");
		buf.append("     * @throws IOException IO problems\n");
		buf.append("     * @since 3.0\n");
		buf.append("     */\n");
		buf.append("    String[] getValues(int i) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Base.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.EOFException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E extends Base {\n");
		buf.append("    /**\n");
		buf.append("     * @param i The parameter\n");
		buf.append("     *                  More about the parameter\n");
		buf.append("     * @return The returned argument\n");
		buf.append("     * @throws EOFException EOF problems\n");
		buf.append("     * @throws ParseException Parse problems\n");
		buf.append("     */\n");
		buf.append("    public String[] getValues(int i) throws EOFException, ParseException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class Base {\n");
		buf.append("    /**\n");
		buf.append("     * @param i The parameter\n");
		buf.append("     *                  More about the parameter\n");
		buf.append("     * @return The returned argument\n");
		buf.append("     * @throws IOException IO problems\n");
		buf.append("     * @throws ParseException \n");
		buf.append("     * @since 3.0\n");
		buf.append("     */\n");
		buf.append("    String[] getValues(int i) throws IOException, ParseException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.EOFException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E extends Base {\n");
		buf.append("    /**\n");
		buf.append("     * @param i The parameter\n");
		buf.append("     *                  More about the parameter\n");
		buf.append("     * @return The returned argument\n");
		buf.append("     * @throws EOFException EOF problems\n");
		buf.append("     */\n");
		buf.append("    public String[] getValues(int i) throws EOFException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
						
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}

	public void testMismatchingExceptionsOnGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface IBase<T> {\n");
		buf.append("    T[] getValues();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("IBase.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E implements IBase<String> {\n");
		buf.append("    public String[] getValues() throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public interface IBase<T> {\n");
		buf.append("    T[] getValues() throws IOException;\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E implements IBase<String> {\n");
		buf.append("    public String[] getValues() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
						
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}

	
}
