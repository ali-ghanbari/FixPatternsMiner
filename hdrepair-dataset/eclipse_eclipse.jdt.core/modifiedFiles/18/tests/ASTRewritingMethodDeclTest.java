/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.rewrite.describing;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class ASTRewritingMethodDeclTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingMethodDeclTest.class;

	public ASTRewritingMethodDeclTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new Suite(THIS);
	}
	
	public static Test setUpTest(Test someTest) {
		TestSuite suite= new Suite("one test");
		suite.addTest(someTest);
		return suite;
	}
	
	public static Test suite() {
		if (true) {
			return allTests();
		}
		return setUpTest(new ASTRewritingMethodDeclTest("testMethodDeclChanges"));
	}
	
	public void testMethodDeclChanges() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {}\n");
		buf.append("    public void gee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // convert constructor to method: insert return type
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
		}
		{ // change return type
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			assertTrue("Has no return type: gee", methodDecl.getReturnType() != null);
			
			Type returnType= methodDecl.getReturnType();
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			rewrite.replace(returnType, newReturnType, null);
		}
		{ // remove return type
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			assertTrue("Has no return type: hee", methodDecl.getReturnType() != null);
						
			// from method to constructor
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
		}
		{ // rename method name
			MethodDeclaration methodDecl= findMethodDeclaration(type, "iee");
			
			SimpleName name= methodDecl.getName();
			SimpleName newName= ast.newSimpleName("xii");
			
			rewrite.replace(name, newName, null);
		}				
		{ // rename first param & last throw statement
			MethodDeclaration methodDecl= findMethodDeclaration(type, "jee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			rewrite.replace((ASTNode) parameters.get(0), newParam, null);
						
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			Name newThrownException= ast.newSimpleName("ArrayStoreException");
			rewrite.replace((ASTNode) thrownExceptions.get(1), newThrownException, null);			
		}
		{ // rename first and second param & rename first and last exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			rewrite.replace((ASTNode) parameters.get(0), newParam1, null);
			rewrite.replace((ASTNode) parameters.get(1), newParam2, null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			Name newThrownException1= ast.newSimpleName("ArrayStoreException");
			Name newThrownException2= ast.newSimpleName("InterruptedException");
			rewrite.replace((ASTNode) thrownExceptions.get(0), newThrownException1, null);
			rewrite.replace((ASTNode) thrownExceptions.get(2), newThrownException2, null);
		}		
		{ // rename all params & rename second exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");			
			SingleVariableDeclaration newParam3= createNewParam(ast, "m3");	
			rewrite.replace((ASTNode) parameters.get(0), newParam1, null);
			rewrite.replace((ASTNode) parameters.get(1), newParam2, null);
			rewrite.replace((ASTNode) parameters.get(2), newParam3, null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			Name newThrownException= ast.newSimpleName("ArrayStoreException");
			rewrite.replace((ASTNode) thrownExceptions.get(1), newThrownException, null);
		}				
		
					
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public float E(int p1, int p2, int p3) {}\n");
		buf.append("    public float gee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void xii(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(float m, int p2, int p3) throws IllegalArgumentException, ArrayStoreException {}\n");
		buf.append("    public abstract void kee(float m1, float m2, int p3) throws ArrayStoreException, IllegalAccessException, InterruptedException;\n");
		buf.append("    public abstract void lee(float m1, float m2, float m3) throws IllegalArgumentException, ArrayStoreException, SecurityException;\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());
	}
	
	public void testMethodReturnTypeChanges() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E() {}\n");
		buf.append("    E(int i) {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    /* comment */ E(int i, int j) {}\n");
		buf.append("    public void gee1() {}\n");
		buf.append("    void gee2() {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    /* comment */ void gee3() {}\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		List list= type.bodyDeclarations();
		
		{ // insert return type, add second modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(0);
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(Modifier.PUBLIC | Modifier.FINAL), null);
		
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
			
		}
		{ // insert return type, add (first) modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(1);
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(Modifier.FINAL), null);
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
		}
		
		{ // insert return type, add second modifier with comments
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(2);
			
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(Modifier.FINAL), null);
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
			
		}
		
		{ // remove return type, add second modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(3);
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(Modifier.PUBLIC | Modifier.FINAL), null);
		
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
			
		}
		{ // remove return type, add (first) modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(4);
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(Modifier.FINAL), null);
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
		}
		
		{ // remove return type, add second modifier with comments
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(5);
			
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(Modifier.FINAL), null);
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
		}
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public final float E() {}\n");
		buf.append("    final float E(int i) {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    final /* comment */ float E(int i, int j) {}\n");
		buf.append("    public final gee1() {}\n");
		buf.append("    final gee2() {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    final gee3() {}\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());

	}
	
	public void testMethodReturnTypeChanges2() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public synchronized E() {}\n");
		buf.append("    public E(int i) {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    public /* comment */ E(int i, int j) {}\n");
		buf.append("    public synchronized void gee1() {}\n");
		buf.append("    public void gee2() {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    public /* comment */ void gee3() {}\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		List list= type.bodyDeclarations();
		
		{ // insert return type, remove second modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(0);
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(Modifier.PUBLIC), null);
		
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
			
		}
		{ // insert return type, remove (only) modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(1);
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(0), null);
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
		}
		
		{ // insert return type, remove modifier with comments
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(2);
			
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(0), null);
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
			
		}
		
		{ // remove return type, remove second modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(3);
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(Modifier.PUBLIC), null);
		
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
			
		}
		{ // remove return type, remove (only) modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(4);
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(0), null);
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
		}
		
		{ // remove return type, remove modifier with comments
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(5);
			
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(0), null);
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
		}
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public float E() {}\n");
		buf.append("    float E(int i) {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    /* comment */ float E(int i, int j) {}\n");
		buf.append("    public gee1() {}\n");
		buf.append("    gee2() {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    gee3() {}\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());

	}


	
	public void testMethodReturnTypeChangesAST3() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E() {}\n");
		buf.append("    E(int i) {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    /* comment */ E(int i, int j) {}\n");
		buf.append("    public void gee1() {}\n");
		buf.append("    void gee2() {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    /* comment */ void gee3() {}\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST3(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		List list= type.bodyDeclarations();
		
		{ // insert return type, add second modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(0);
			
			ListRewrite modifiers= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			modifiers.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
			
		}
		{ // insert return type, add (first) modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(1);
			
			ListRewrite modifiers= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			modifiers.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
		}
		
		{ // insert return type, add second modifier with comments
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(2);
			
			ListRewrite modifiers= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			modifiers.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
			
		}
		
		{ // remove return type, add second modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(3);

			ListRewrite modifiers= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			modifiers.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
			
		}
		{ // remove return type, add (first) modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(4);

			ListRewrite modifiers= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			modifiers.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);			
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
		}
		
		{ // remove return type, add second modifier with comments
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(5);
			
			ListRewrite modifiers= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			modifiers.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
		}
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public final float E() {}\n");
		buf.append("    final float E(int i) {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    final float /* comment */ E(int i, int j) {}\n");
		buf.append("    public final gee1() {}\n");
		buf.append("    final gee2() {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    final gee3() {}\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());

	}
	
	public void testMethodReturnTypeChanges2AST3() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public synchronized E() {}\n");
		buf.append("    public E(int i) {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    public /* comment */ E(int i, int j) {}\n");
		buf.append("    public synchronized void gee1() {}\n");
		buf.append("    public void gee2() {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    public /* comment */ void gee3() {}\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST3(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		List list= type.bodyDeclarations();
		
		{ // insert return type, remove second modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(0);
			rewrite.remove((ASTNode) methodDecl.modifiers().get(1), null);
		
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
			
		}
		{ // insert return type, remove (only) modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(1);
			rewrite.remove((ASTNode) methodDecl.modifiers().get(0), null);
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
		}
		
		{ // insert return type, remove modifier with comments
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(2);
			rewrite.remove((ASTNode) methodDecl.modifiers().get(0), null);			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
		}
		
		{ // remove return type, remove second modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(3);
			rewrite.remove((ASTNode) methodDecl.modifiers().get(1), null);
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
			
		}
		{ // remove return type, remove (only) modifier
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(4);
			rewrite.remove((ASTNode) methodDecl.modifiers().get(0), null);
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
		}
		
		{ // remove return type, remove modifier with comments
			MethodDeclaration methodDecl= (MethodDeclaration) list.get(5);
			rewrite.remove((ASTNode) methodDecl.modifiers().get(0), null);
			
			// from method to constructor 
			rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, null, null);
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
		}
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public float E() {}\n");
		buf.append("    float E(int i) {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    float /* comment */ E(int i, int j) {}\n");
		buf.append("    public gee1() {}\n");
		buf.append("    gee2() {}\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    gee3() {}\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());

	}


	
	
	public void testListRemoves() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {}\n");
		buf.append("    public void gee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete first param
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.remove((ASTNode) parameters.get(0), null);
		}
		{ // delete second param & remove exception & remove public
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			// change flags
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(0), null);
			
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.remove((ASTNode) parameters.get(1), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			rewrite.remove((ASTNode) thrownExceptions.get(0), null);
		}		
		{ // delete last param
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.remove((ASTNode) parameters.get(2), null);	
		}				
		{ // delete first and second param & remove first exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "iee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.remove((ASTNode) parameters.get(0), null);
			rewrite.remove((ASTNode) parameters.get(1), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			rewrite.remove((ASTNode) thrownExceptions.get(0), null);	
		}				
		{ // delete first and last param & remove second
			MethodDeclaration methodDecl= findMethodDeclaration(type, "jee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.remove((ASTNode) parameters.get(0), null);
			rewrite.remove((ASTNode) parameters.get(2), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			rewrite.remove((ASTNode) thrownExceptions.get(1), null);			
		}
		{ // delete second and last param & remove first exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.remove((ASTNode) parameters.get(1), null);
			rewrite.remove((ASTNode) parameters.get(2), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			rewrite.remove((ASTNode) thrownExceptions.get(1), null);
		}		
		{ // delete all params & remove first and last exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.remove((ASTNode) parameters.get(0), null);
			rewrite.remove((ASTNode) parameters.get(1), null);
			rewrite.remove((ASTNode) parameters.get(2), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			rewrite.remove((ASTNode) thrownExceptions.get(0), null);
			rewrite.remove((ASTNode) thrownExceptions.get(2), null);				
		}				


		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p2, int p3) {}\n");
		buf.append("    void gee(int p1, int p3) {}\n");
		buf.append("    public void hee(int p1, int p2) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p3) throws IllegalAccessException {}\n");
		buf.append("    public void jee(int p2) throws IllegalArgumentException {}\n");
		buf.append("    public abstract void kee(int p1) throws IllegalArgumentException, SecurityException;\n");
		buf.append("    public abstract void lee() throws IllegalAccessException;\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());

	}
	
	public void testListInserts() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {}\n");
		buf.append("    public void gee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // insert before first param & insert an exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY).insertFirst(newParam, null);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 0 thrown exceptions", thrownExceptions.size() == 0);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertFirst(newThrownException, null);

		}
		{ // insert before second param & insert before first exception & add synchronized
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			// change flags
			int newModifiers= Modifier.PUBLIC | Modifier.SYNCHRONIZED;
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
			
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			ASTNode secondParam= (ASTNode) parameters.get(1);
			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY).insertBefore(newParam, secondParam, null);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			ASTNode firstException= (ASTNode) thrownExceptions.get(0);
			Name newThrownException= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertBefore(newThrownException, firstException, null);
		}		
		{ // insert after last param & insert after first exception & add synchronized, static
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			
			// change flags
			int newModifiers= Modifier.PUBLIC | Modifier.SYNCHRONIZED | Modifier.STATIC;
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
			
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			
			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY).insertLast(newParam, null);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			ASTNode firstException= (ASTNode) thrownExceptions.get(0);
			Name newThrownException= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertAfter(newThrownException, firstException, null);

		}				
		{ // insert 2 params before first & insert between two exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "iee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			ASTNode firstParam= (ASTNode) parameters.get(0);
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			
			ListRewrite listRewrite = rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY);
			listRewrite.insertBefore(newParam1, firstParam, null);
			listRewrite.insertBefore(newParam2, firstParam, null);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			
			ASTNode firstException= (ASTNode) thrownExceptions.get(0);
			Name newThrownException= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertAfter(newThrownException, firstException, null);
		}			
		{ // insert 2 params after first & replace the second exception and insert new after
			MethodDeclaration methodDecl= findMethodDeclaration(type, "jee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			ListRewrite listRewrite = rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY);

			ASTNode firstParam= (ASTNode) parameters.get(0);
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			listRewrite.insertAfter(newParam2, firstParam, null);
			listRewrite.insertAfter(newParam1, firstParam, null);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			
			Name newThrownException1= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException1, null);

			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			rewrite.replace((ASTNode) thrownExceptions.get(1), newThrownException2, null);
		}
		{ // insert 2 params after last & remove the last exception and insert new after
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY);
			ASTNode lastParam= (ASTNode) parameters.get(2);
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");

			listRewrite.insertAfter(newParam2, lastParam, null);
			listRewrite.insertAfter(newParam1, lastParam, null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			
			ASTNode lastException= (ASTNode) thrownExceptions.get(2);
			rewrite.remove(lastException, null);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertBefore(newThrownException, lastException, null);
		}	
		{ // insert at first and last position & remove 2nd, add after 2nd, remove 3rd
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY);
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			listRewrite.insertFirst(newParam1, null);
			listRewrite.insertLast(newParam2, null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			
			ASTNode secondException= (ASTNode) thrownExceptions.get(1);
			ASTNode lastException= (ASTNode) thrownExceptions.get(2);
			rewrite.remove(secondException, null);
			rewrite.remove(lastException, null);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertAfter(newThrownException, secondException, null);

		}				


		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(float m, int p1, int p2, int p3) throws InterruptedException {}\n");
		buf.append("    public synchronized void gee(int p1, float m, int p2, int p3) throws InterruptedException, IllegalArgumentException {}\n");
		buf.append("    public static synchronized void hee(int p1, int p2, int p3, float m) throws IllegalArgumentException, InterruptedException {}\n");
		buf.append("    public void iee(float m1, float m2, int p1, int p2, int p3) throws IllegalArgumentException, InterruptedException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, float m1, float m2, int p2, int p3) throws IllegalArgumentException, ArrayStoreException, InterruptedException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3, float m1, float m2) throws IllegalArgumentException, IllegalAccessException, InterruptedException;\n");
		buf.append("    public abstract void lee(float m1, int p1, int p2, int p3, float m2) throws IllegalArgumentException, InterruptedException;\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());

	}

	public void testListInsert() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // insert at first and last position & remove 2nd, add after 2nd, remove 3rd
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY);
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			listRewrite.insertFirst(newParam1, null);
			listRewrite.insertLast(newParam2, null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			
			rewrite.remove((ASTNode) thrownExceptions.get(1), null);
			rewrite.remove((ASTNode) thrownExceptions.get(2), null);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException, null);
		}				


	
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public abstract void lee(float m1, int p1, int p2, int p3, float m2) throws IllegalArgumentException, InterruptedException;\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());

	}
	
	public void testListCombinations() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {}\n");
		buf.append("    public void gee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete all and insert after & insert 2 exceptions
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
		
			rewrite.remove((ASTNode) parameters.get(0), null);
			rewrite.remove((ASTNode) parameters.get(1), null);
			rewrite.remove((ASTNode) parameters.get(2), null);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY).insertLast(newParam, null);


			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 0 thrown exceptions", thrownExceptions.size() == 0);
			
			Name newThrownException1= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException1, null);

			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException2, null);
			
		}
		{ // delete first 2, replace last and insert after & replace first exception and insert before
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			rewrite.remove((ASTNode) parameters.get(0), null);
			rewrite.remove((ASTNode) parameters.get(1), null);
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			rewrite.replace((ASTNode) parameters.get(2), newParam1, null);
						
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY).insertLast(newParam2, null);


			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			Name modifiedThrownException= ast.newSimpleName("InterruptedException");
			rewrite.replace((ASTNode) thrownExceptions.get(0), modifiedThrownException, null);
						
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException2, null);

		}		
		{ // delete first 2, replace last and insert at first & remove first and insert before
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			rewrite.remove((ASTNode) parameters.get(0), null);
			rewrite.remove((ASTNode) parameters.get(1), null);
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			rewrite.replace((ASTNode) parameters.get(2), newParam1, null);
						
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY).insertFirst(newParam2, null);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			rewrite.remove((ASTNode) thrownExceptions.get(0), null);
						
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException2, null);
		}				


		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(float m) throws InterruptedException, ArrayStoreException {}\n");
		buf.append("    public void gee(float m1, float m2) throws InterruptedException, ArrayStoreException {}\n");
		buf.append("    public void hee(float m2, float m1) throws ArrayStoreException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());

	}
	
	public void testListCombination() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {}\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete all and insert after & insert 2 exceptions
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
		
			rewrite.remove((ASTNode) parameters.get(0), null);
			rewrite.remove((ASTNode) parameters.get(1), null);
			rewrite.remove((ASTNode) parameters.get(2), null);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY).insertLast(newParam, null);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 0 thrown exceptions", thrownExceptions.size() == 0);
			
			Name newThrownException1= ast.newSimpleName("InterruptedException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException1, null);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException2, null);

			
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(float m) throws InterruptedException, ArrayStoreException {}\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());

	}
	
	
	public void testMethodBody() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {}\n");
		buf.append("    public void gee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // replace block
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			
			Block body= methodDecl.getBody();
			assertTrue("No body: E", body != null);
			
			Block newBlock= ast.newBlock();

			rewrite.replace(body, newBlock, null);
		}
		{ // delete block & set abstract
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			// change flags
			int newModifiers= Modifier.PUBLIC | Modifier.ABSTRACT;
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
			
			Block body= methodDecl.getBody();
			assertTrue("No body: gee", body != null);

			rewrite.remove(body, null);
		}
		{ // insert block & set to private
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			
			// change flags
			int newModifiers= Modifier.PRIVATE;
			rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
			
			
			Block body= methodDecl.getBody();
			assertTrue("Has body", body == null);
			
			Block newBlock= ast.newBlock();
			rewrite.set(methodDecl, MethodDeclaration.BODY_PROPERTY, newBlock, null);
		}		

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {\n");
		buf.append("    }\n");
		buf.append("    public abstract void gee(int p1, int p2, int p3) throws IllegalArgumentException;\n");
		buf.append("    public void hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    private void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException {\n");
		buf.append("    }\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());

	}
	
	public void testMethodDeclarationExtraDimensions() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1() { return null; }\n");
		buf.append("    public Object foo2() throws IllegalArgumentException { return null; }\n");
		buf.append("    public Object foo3()[][] { return null; }\n");
		buf.append("    public Object foo4()[][] throws IllegalArgumentException { return null; }\n");
		buf.append("    public Object foo5()[][] { return null; }\n");
		buf.append("    public Object foo6(int i)[][] throws IllegalArgumentException { return null; }\n");
		buf.append("    public Object foo7(int i)[][] { return null; }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // add extra dim, add throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo1");
			
			rewrite.set(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(1), null);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException2, null);

		}
		{ // add extra dim, remove throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo2");
			
			rewrite.set(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(1), null);
			
			rewrite.remove((ASTNode) methodDecl.thrownExceptions().get(0), null);			
		}		
		{ // remove extra dim, add throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo3");

			rewrite.set(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(1), null);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException2, null);

		}
		{ // add extra dim, remove throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo4");
			
			rewrite.set(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(1), null);
			
			rewrite.remove((ASTNode) methodDecl.thrownExceptions().get(0), null);			
		}
		{ // add params, add extra dim, add throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo5");
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY).insertLast(newParam1, null);

			
			rewrite.set(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(4), null);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY).insertLast(newThrownException2, null);
	
		}
		{ // remove params, add extra dim, remove throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo6");
			
			rewrite.remove((ASTNode) methodDecl.parameters().get(0), null);		
			
			rewrite.set(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(4), null);
			
			rewrite.remove((ASTNode) methodDecl.thrownExceptions().get(0), null);			
		}
		{ // remove block
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo7");
			rewrite.remove(methodDecl.getBody(), null);			
		}					
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1()[] throws ArrayStoreException { return null; }\n");
		buf.append("    public Object foo2()[] { return null; }\n");
		buf.append("    public Object foo3()[] throws ArrayStoreException { return null; }\n");
		buf.append("    public Object foo4()[] { return null; }\n");
		buf.append("    public Object foo5(float m1)[][][][] throws ArrayStoreException { return null; }\n");
		buf.append("    public Object foo6()[][][][] { return null; }\n");
		buf.append("    public Object foo7(int i)[][];\n");		
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());

	}
	
	public void testModifiersAST3() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1() { return null; }\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    public Object foo2() { return null; }\n");
		buf.append("    public Object foo3() { return null; }\n");
		buf.append("    /** javadoc comment */\n");	
		buf.append("    public Object foo4() { return null; }\n");
		buf.append("    Object foo5() { return null; }\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    public Object foo6() { return null; }\n");
		buf.append("    public Object foo7() { return null; }\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    public static Object foo8() { return null; }\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    Object foo9() { return null; }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST3(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // insert first and last
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo1");
			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			listRewrite.insertFirst(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
			listRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD), null);
		}
		{ // insert 2x first
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo2");
			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			listRewrite.insertFirst(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
			listRewrite.insertFirst(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD), null);
		}		
		{ // remove and insert first
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo3");
			rewrite.remove((ASTNode) methodDecl.modifiers().get(0), null);
			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			listRewrite.insertFirst(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
		}
		{ // remove and insert last
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo4");
			rewrite.remove((ASTNode) methodDecl.modifiers().get(0), null);
			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			listRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
		}
		{ // insert first and insert Javadoc
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo5");
			Javadoc javadoc= ast.newJavadoc();
			TextElement textElem= ast.newTextElement();
			textElem.setText("Hello");
			TagElement tagElement= ast.newTagElement();
			tagElement.fragments().add(textElem);
			javadoc.tags().add(tagElement);
			rewrite.set(methodDecl, MethodDeclaration.JAVADOC_PROPERTY, javadoc, null);
			
			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			listRewrite.insertFirst(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
		}
		{ // remove modifer and remove javadoc
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo6");
			rewrite.remove(methodDecl.getJavadoc(), null);
			rewrite.remove((ASTNode) methodDecl.modifiers().get(0), null);
		}
		{ // remove modifer and insert javadoc
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo7");
			
			Javadoc javadoc= ast.newJavadoc();
			TextElement textElem= ast.newTextElement();
			textElem.setText("Hello");
			TagElement tagElement= ast.newTagElement();
			tagElement.fragments().add(textElem);
			javadoc.tags().add(tagElement);
			rewrite.set(methodDecl, MethodDeclaration.JAVADOC_PROPERTY, javadoc, null);
			
			rewrite.remove((ASTNode) methodDecl.modifiers().get(0), null);
		}
		{ // remove all
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo8");
			rewrite.remove((ASTNode) methodDecl.modifiers().get(0), null);
			rewrite.remove((ASTNode) methodDecl.modifiers().get(1), null);
		}
		{ // insert (first) with javadoc
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo9");
			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			listRewrite.insertFirst(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
		}	
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    final public synchronized Object foo1() { return null; }\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    static final public Object foo2() { return null; }\n");
		buf.append("    final Object foo3() { return null; }\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    final Object foo4() { return null; }\n");
		buf.append("    /**\n");
		buf.append("     * Hello\n");
		buf.append("     */\n");
		buf.append("    final Object foo5() { return null; }\n");
		buf.append("    Object foo6() { return null; }\n");
		buf.append("    /**\n");
		buf.append("     * Hello\n");
		buf.append("     */\n");
		buf.append("    Object foo7() { return null; }\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    Object foo8() { return null; }\n");
		buf.append("    /** javadoc comment */\n");
		buf.append("    final Object foo9() { return null; }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
	}
	
	
	public void testFieldDeclaration() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    int i1= 1;\n");
		buf.append("    int i2= 1, k2= 2, n2= 3;\n");
		buf.append("    static final int i3= 1, k3= 2, n3= 3;\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "A");
		
		FieldDeclaration[] fieldDeclarations= type.getFields();
		assertTrue("Number of fieldDeclarations not 3", fieldDeclarations.length == 3);
		{	// add modifier, change type, add fragment
			FieldDeclaration decl= fieldDeclarations[0];
			
			// add modifier
			int newModifiers= Modifier.FINAL;
			rewrite.set(decl, FieldDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
			
			PrimitiveType newType= ast.newPrimitiveType(PrimitiveType.BOOLEAN);
			rewrite.replace(decl.getType(), newType, null);
					
			VariableDeclarationFragment frag=	ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName("k1"));
			frag.setInitializer(null);

			rewrite.getListRewrite(decl, FieldDeclaration.FRAGMENTS_PROPERTY).insertLast(frag, null);

		}
		{	// add modifiers, remove first two fragments, replace last
			FieldDeclaration decl= fieldDeclarations[1];
			
			// add modifier
			int newModifiers= Modifier.FINAL | Modifier.STATIC | Modifier.TRANSIENT;
			rewrite.set(decl, FieldDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
			
			List fragments= decl.fragments();
			assertTrue("Number of fragments not 3", fragments.size() == 3);
			
			rewrite.remove((ASTNode) fragments.get(0), null);
			rewrite.remove((ASTNode) fragments.get(1), null);
			
			VariableDeclarationFragment frag=	ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName("k2"));
			frag.setInitializer(null);
			
			rewrite.replace((ASTNode) fragments.get(2), frag, null);
		}
		{	// remove modifiers
			FieldDeclaration decl= fieldDeclarations[2];
			
			// change modifier
			int newModifiers= 0;
			rewrite.set(decl, FieldDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
		}
				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    final boolean i1= 1, k1;\n");
		buf.append("    static final transient int k2;\n");
		buf.append("    int i3= 1, k3= 2, n3= 3;\n");
		buf.append("}\n");	
		
		assertEqualString(preview, buf.toString());

	}
	
	public void testInitializer() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    {\n");
		buf.append("        foo();\n");
		buf.append("    }\n");
		buf.append("    static {\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "A");
		
		List declarations= type.bodyDeclarations();
		assertTrue("Number of fieldDeclarations not 2", declarations.size() == 2);
		{	// change modifier, replace body
			Initializer initializer= (Initializer) declarations.get(0);
			
			// add modifier
			int newModifiers= Modifier.STATIC;
			rewrite.set(initializer, Initializer.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
			
			
			Block block= ast.newBlock();
			block.statements().add(ast.newReturnStatement());
			
			rewrite.replace(initializer.getBody(), block, null);
		}
		{	// change modifier
			Initializer initializer= (Initializer) declarations.get(1);
			
			int newModifiers= 0;
			rewrite.set(initializer, Initializer.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
			
		}
				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    static {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("    {\n");
		buf.append("    }\n");
		buf.append("}\n");	
		
		assertEqualString(preview, buf.toString());

	}
	
	
	public void testMethodDeclarationParamShuffel() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1(int i, boolean b) { return null; }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // add extra dim, add throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo1");
			
			List params= methodDecl.parameters();
			
			SingleVariableDeclaration first= (SingleVariableDeclaration) params.get(0);
			SingleVariableDeclaration second= (SingleVariableDeclaration) params.get(1);
			rewrite.replace(first.getName(), ast.newSimpleName("x"), null);
			rewrite.replace(second.getName(), ast.newSimpleName("y"), null);
				
			ASTNode copy1= rewrite.createCopyTarget(first);
			ASTNode copy2= rewrite.createCopyTarget(second);
			
			rewrite.replace(first, copy2, null);
			rewrite.replace(second, copy1, null);
			
		}
	
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1(boolean y, int x) { return null; }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());

	}	
	

	public void testMethodDeclarationParamShuffel1() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1(int i, boolean b) { return null; }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ 
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo1");
			
			List params= methodDecl.parameters();
			
			SingleVariableDeclaration first= (SingleVariableDeclaration) params.get(0);
			SingleVariableDeclaration second= (SingleVariableDeclaration) params.get(1);
				
			ASTNode copy2= rewrite.createCopyTarget(second);

			rewrite.replace(first, copy2, null);
			rewrite.remove(second, null);
		}
	
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1(boolean b) { return null; }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());

	}
		
	public void testMethodDeclaration_bug24916() throws Exception {
	
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    private int DD()[]{\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "DD");
			
			rewrite.set(methodDecl, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
			rewrite.set(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
		}

		String preview= evaluateRewrite(cu, rewrite);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    private DD(){\n");
		buf.append("    };\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testMethodComments1() throws Exception {
	
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("    private void foo(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			rewrite.remove(methodDecl, null);
		}

		String preview= evaluateRewrite(cu, rewrite);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    /**\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testMethodComments2() throws Exception {
	
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("    private void foo(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo2");
			ASTNode node= rewrite.createCopyTarget(methodDecl);

			ASTNode firstDecl= (ASTNode) type.bodyDeclarations().get(0);
			rewrite.getListRewrite(type, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAfter(node, firstDecl, null);
		}

		String preview= evaluateRewrite(cu, rewrite);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("    private void foo(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");
		buf.append("\n");				
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testMethodComments3() throws Exception {
	
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("\n");		
		buf.append("    private void foo(){\n");
		buf.append("    } // another\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			rewrite.remove(methodDecl, null);
		}

		String preview= evaluateRewrite(cu, rewrite);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("\n");			
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	
	public void testBUG_38447() throws Exception {
		
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("\n");		
		buf.append("    private void foo(){\n");
		buf.append("\n"); // missing closing bracket
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			rewrite.remove(methodDecl, null);
		}

		String preview= evaluateRewrite(cu, rewrite);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("\n");			
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}

	public void testMethodComments4() throws Exception {
	
	
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("\n");		
		buf.append("    private void foo(){\n");
		buf.append("    } // another\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			ASTNode copy= rewrite.createCopyTarget(methodDecl);
			
			rewrite.getListRewrite(type, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertLast(copy, null);
			
			MethodDeclaration newMethodDecl= createNewMethod(astRoot.getAST(), "xoo", false);
			rewrite.replace(methodDecl, newMethodDecl, null);
			
			//MethodDeclaration methodDecl2= findMethodDeclaration(type, "foo1");
			//rewrite.markAsReplaced(methodDecl2, copy);
		}

		String preview= evaluateRewrite(cu, rewrite);

		buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("\n");		
		buf.append("    private void xoo(String str) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");
		buf.append("\n");	
		buf.append("    private void foo(){\n");
		buf.append("    } // another\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testInsertFieldAfter() throws Exception {

		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    private int fCount1;\n");
		buf.append("\n");	
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			VariableDeclarationFragment frag= ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName("fColor"));
			FieldDeclaration newField= ast.newFieldDeclaration(frag);
			newField.setType(ast.newPrimitiveType(PrimitiveType.CHAR));
			newField.setModifiers(Modifier.PRIVATE);
					
			rewrite.getListRewrite(type, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newField, 1, null);
		}

		String preview= evaluateRewrite(cu, rewrite);

		buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    private int fCount1;\n");
		buf.append("    private char fColor;\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	
}
