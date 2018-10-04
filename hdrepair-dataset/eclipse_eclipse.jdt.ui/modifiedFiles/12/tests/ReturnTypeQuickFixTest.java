/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;

public class ReturnTypeQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= ReturnTypeQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ReturnTypeQuickFixTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return setUpTest(new TestSuite(THIS));
	}
	
	public static Test suite() {
		return allTests();
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getFormatterOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		
		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	
	public void testSimpleTypeReturnDeclMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        return (new Vector()).elements();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;		
		
				String preview= getPreviewContent(proposal);
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Enumeration;\n");		
				buf.append("import java.util.Vector;\n");
				buf.append("public class E {\n");
				buf.append("    public Enumeration foo() {\n");
				buf.append("        return (new Vector()).elements();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Vector;\n");				
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        return (new Vector()).elements();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}
	}


	public void testVoidTypeReturnDeclMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        //do nothing\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;		
		
				String preview= getPreviewContent(proposal);
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public void foo() {\n");
				buf.append("        //do nothing\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        //do nothing\n");	
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}	
		
	}
	
	
	public void testReturnTypeDeclMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E {\n");
		buf.append("    public foo();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E {\n");
		buf.append("    public void foo();\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	
	public void testVoidTypeReturnDeclMissing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("           return;\n");
		buf.append("        }\n");
		buf.append("        return;\n");				
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;		
		
				String preview= getPreviewContent(proposal);
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public void foo() {\n");
				buf.append("        if (true) {\n");
				buf.append("           return;\n");
				buf.append("        }\n");
				buf.append("        return;\n");				
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        if (true) {\n");
				buf.append("           return;\n");
				buf.append("        }\n");
				buf.append("        return;\n");				
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}	
	}
	
	public void testVoidMissingInAnonymousType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");
		buf.append("            public foo() {\n");
		buf.append("            }\n");		
		buf.append("        };\n");				
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");
		buf.append("            public void foo() {\n");
		buf.append("            }\n");		
		buf.append("        };\n");				
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	
	public void testNullTypeReturnDeclMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;		
		
				String preview= getPreviewContent(proposal);
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public Object foo() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}	
		}		
	}
	
	public void testArrayTypeReturnDeclMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        return new int[][] { { 1, 2 }, { 2, 3 } };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public int[][] foo() {\n");
				buf.append("        return new int[][] { { 1, 2 }, { 2, 3 } };\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        return new int[][] { { 1, 2 }, { 2, 3 } };\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}	
	}
	
	public void testVoidMethodReturnsStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    Vector fList= new Vector();\n");
		buf.append("    public void elements() {\n");
		buf.append("        return fList.toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
			
		{	
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
			String preview= getPreviewContent(proposal);
		
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E {\n");
			buf.append("    Vector fList= new Vector();\n");
			buf.append("    public Object[] elements() {\n");
			buf.append("        return fList.toArray();\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}
		{	
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
			String preview= getPreviewContent(proposal);
		
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E {\n");
			buf.append("    Vector fList= new Vector();\n");
			buf.append("    public void elements() {\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}		
	}
	
	public void testVoidMethodReturnsAnonymClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getOperation() {\n");
		buf.append("        return new Runnable() {\n");
		buf.append("            public void run() {}\n");
		buf.append("        };\n");				
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
			
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public Runnable getOperation() {\n");
		buf.append("        return new Runnable() {\n");
		buf.append("            public void run() {}\n");
		buf.append("        };\n");				
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
			
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getOperation() {\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testConstructorReturnsValue() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");	
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        return System.getProperties();\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
			String preview= getPreviewContent(proposal);
	
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.util.Properties;\n");					
			buf.append("\n");
			buf.append("public class E {\n");
			buf.append("    public Properties E() {\n");
			buf.append("        return System.getProperties();\n");		
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
			String preview= getPreviewContent(proposal);
	
			buf= new StringBuffer();
			buf.append("package test1;\n");	
			buf.append("public class E {\n");
			buf.append("    public E() {\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}		
	}
	
	
	public void testCorrectReturnStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public Runnable getOperation() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public Runnable getOperation() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getOperation() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testCorrectReturnStatementForArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getArray() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}	
	
	public void testMethodWithConstructorName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] E() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	
	
	public void testMissingReturnStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getArray() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testMissingReturnStatementWithCode() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] fArray;\n");		
		buf.append("    public int getArray()[] {\n");
		buf.append("        if (true) {\n");		
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] fArray;\n");	
		buf.append("    public int getArray()[] {\n");
		buf.append("        if (true) {\n");		
		buf.append("        }\n");
		buf.append("        return fArray;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] fArray;\n");		
		buf.append("    public void getArray()[] {\n");
		buf.append("        if (true) {\n");		
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testMissingReturnStatementWithCode2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean isVisible() {\n");
		buf.append("        boolean res= false;\n");			
		buf.append("        if (true) {\n");
		buf.append("            res= false;\n");			
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean isVisible() {\n");
		buf.append("        boolean res= false;\n");			
		buf.append("        if (true) {\n");
		buf.append("            res= false;\n");			
		buf.append("        }\n");
		buf.append("        return res;\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void isVisible() {\n");
		buf.append("        boolean res= false;\n");			
		buf.append("        if (true) {\n");
		buf.append("            res= false;\n");			
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}
	
	public void testMissingReturnStatementWithCode3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public String getDebugInfo() {\n");
		buf.append("        getClass().getName();\n");			
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public String getDebugInfo() {\n");
		buf.append("        return getClass().getName();\n");			
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}
	
	public void testMissingReturnStatementWithCode4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public String getDebugInfo() {\n");
		buf.append("        getClass().notify();\n");			
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public String getDebugInfo() {\n");
		buf.append("        getClass().notify();\n");			
		buf.append("        return null;\n");			
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}
	
}