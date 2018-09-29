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
package org.eclipse.jdt.core.tests.compiler.regression;

import junit.framework.Test;

public class StaticImportTest extends AbstractComparableTest {

	public StaticImportTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(testClass());
	}
	
	public static Class testClass() {
		return StaticImportTest.class;
	}


	public void test001() {
		this.runConformTest(
			new String[] {
				"X.java",
				"import static java.lang.Math.*;\n" +
				"import static java.lang.Math.PI;\n" +
				"public class X { double pi = abs(PI); }\n",
			},
			"");
	}

	public void test002() {
		this.runConformTest(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"import static p2.Y.*;\n" +
				"import static p2.Z.Zint;\n" +
				"import static p2.Z.ZMember;\n" +
				"public class X {\n" +
				"	int x = y(1);\n" +
				"	int y = Yint;\n" +
				"	int z = Zint;\n" +
				"	void m1(YMember m) {}\n" +
				"	void m2(ZMember m) {}\n" +
				"}\n",
				"p2/Y.java",
				"package p2;\n" +
				"public class Y {\n" +
				"	public static int Yint = 1;\n" +
				"	public static int y(int y) { return y; }\n" +
				"	public static class YMember {}\n" +
				"}\n",
				"p2/Z.java",
				"package p2;\n" +
				"public class Z {\n" +
				"	public static int Zint = 1;\n" +
				"	public static class ZMember {}\n" +
				"}\n",
			},
			"");
	}

	public void test003() { // test inheritance
		this.runConformTest(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"import static p2.Y.*;\n" +
				"import static p2.Z.Zint;\n" +
				"import static p2.Z.ZMember;\n" +
				"public class X {\n" +
				"	int x = y(1);\n" +
				"	int y = Yint;\n" +
				"	int z = Zint;\n" +
				"	void m1(YMember m) {}\n" +
				"	void m2(ZMember m) {}\n" +
				"}\n",
				"p2/YY.java",
				"package p2;\n" +
				"public class YY {\n" +
				"	public static int Yint = 1;\n" +
				"	public static int y(int y) { return y; }\n" +
				"	public static class YMember {}\n" +
				"}\n",
				"p2/Y.java",
				"package p2;\n" +
				"public class Y extends YY {}\n",
				"p2/ZZ.java",
				"package p2;\n" +
				"public class ZZ {\n" +
				"	public static int Zint = 1;\n" +
				"	public static class ZMember {}\n" +
				"}\n",
				"p2/Z.java",
				"package p2;\n" +
				"public class Z extends ZZ {}\n",
			},
			"");
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.A.C;\n" + 
				"public class X { int i = C; }\n",
				"p/A.java",
				"package p;\n" + 
				"public class A extends B implements I {}\n" +
				"class B implements I {}\n",
				"p/I.java",
				"package p;\n" + 
				"public interface I { public static int C = 1; }\n"
			},
			""
		);
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static p.A.C;\n" + 
				"public class X { int i = C; }\n",
				"p/A.java",
				"package p;\n" + 
				"public class A implements I {}\n" +
				"interface I { public static int C = 1; }\n"
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 2)\n" + 
			"	public class X { int i = C; }\n" + 
			"	                         ^\n" + 
			"The type I is not visible\n" + 
			"----------\n"
			// C in p.I is not defined in a public class or interface; cannot be accessed from outside package
		);
	}

	public void test004() { // test static vs. instance
		this.runNegativeTest(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"import static p2.Y.*;\n" +
				"import static p2.Z.Zint;\n" +
				"import static p2.Z.ZMember;\n" +
				"public class X {\n" +
				"	int x = y(1);\n" +
				"	int y = Yint;\n" +
				"	int z = Zint;\n" +
				"	void m1(YMember m) {}\n" +
				"	void m2(ZMember m) {}\n" +
				"}\n",
				"p2/Y.java",
				"package p2;\n" +
				"public class Y {\n" +
				"	public int Yint = 1;\n" +
				"	public int y(int y) { return y; }\n" +
				"	public class YMember {}\n" +
				"}\n",
				"p2/Z.java",
				"package p2;\n" +
				"public class Z {\n" +
				"	public int Zint = 1;\n" +
				"	public class ZMember {}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in p\\X.java (at line 3)\n" + 
			"	import static p2.Z.Zint;\n" + 
			"	              ^^^^^^^^^\n" + 
			"The import p2.Z.Zint cannot be resolved\n" + 
			"----------\n" + 
			"2. ERROR in p\\X.java (at line 4)\n" + 
			"	import static p2.Z.ZMember;\n" + 
			"	              ^^^^^^^^^^^^\n" + 
			"The import p2.Z.ZMember cannot be resolved\n" + 
			"----------\n" + 
			"3. ERROR in p\\X.java (at line 6)\n" + 
			"	int x = y(1);\n" + 
			"	        ^\n" + 
			"The method y(int) is undefined for the type X\n" + 
			"----------\n" + 
			"4. ERROR in p\\X.java (at line 7)\n" + 
			"	int y = Yint;\n" + 
			"	        ^^^^\n" + 
			"Yint cannot be resolved\n" + 
			"----------\n" + 
			"5. ERROR in p\\X.java (at line 8)\n" + 
			"	int z = Zint;\n" + 
			"	        ^^^^\n" + 
			"Zint cannot be resolved\n" + 
			"----------\n" + 
			"6. ERROR in p\\X.java (at line 9)\n" + 
			"	void m1(YMember m) {}\n" + 
			"	        ^^^^^^^\n" + 
			"YMember cannot be resolved to a type\n" + 
			"----------\n" + 
			"7. ERROR in p\\X.java (at line 10)\n" + 
			"	void m2(ZMember m) {}\n" + 
			"	        ^^^^^^^\n" + 
			"ZMember cannot be resolved to a type\n" + 
			"----------\n");
	}

	public void test005() { // test visibility
		this.runNegativeTest(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"import static p2.Y.*;\n" +
				"import static p2.Z.Zint;\n" +
				"import static p2.Z.ZMember;\n" +
				"public class X {\n" +
				"	int x = y(1);\n" +
				"	int y = Yint;\n" +
				"	int z = Zint;\n" +
				"	void m1(YMember m) {}\n" +
				"	void m2(ZMember m) {}\n" +
				"}\n",
				"p2/Y.java",
				"package p2;\n" +
				"public class Y {\n" +
				"	static int Yint = 1;\n" +
				"	static int y(int y) { return y; }\n" +
				"	static class YMember {}\n" +
				"}\n",
				"p2/Z.java",
				"package p2;\n" +
				"public class Z {\n" +
				"	static int Zint = 1;\n" +
				"	static class ZMember {}\n" +
				"}\n",
			},
		"----------\n" + 
		"1. ERROR in p\\X.java (at line 3)\n" + 
		"	import static p2.Z.Zint;\n" + 
		"	              ^^^^^^^^^\n" + 
		"The field Z.p2.Z.Zint is not visible\n" + 
		"----------\n" + 
		"2. ERROR in p\\X.java (at line 4)\n" + 
		"	import static p2.Z.ZMember;\n" + 
		"	              ^^^^^^^^^^^^\n" + 
		"The type p2.Z.ZMember is not visible\n" + 
		"----------\n" + 
		"3. ERROR in p\\X.java (at line 6)\n" + 
		"	int x = y(1);\n" + 
		"	        ^\n" + 
		"The method y(int) from the type Y is not visible\n" + 
		"----------\n" + 
		"4. ERROR in p\\X.java (at line 7)\n" + 
		"	int y = Yint;\n" + 
		"	        ^^^^\n" + 
		"The field Y.Yint is not visible\n" + 
		"----------\n" + 
		"5. ERROR in p\\X.java (at line 8)\n" + 
		"	int z = Zint;\n" + 
		"	        ^^^^\n" + 
		"Zint cannot be resolved\n" + 
		"----------\n" + 
		"6. ERROR in p\\X.java (at line 9)\n" + 
		"	void m1(YMember m) {}\n" + 
		"	        ^^^^^^^\n" + 
		"The type YMember is not visible\n" + 
		"----------\n" + 
		"7. ERROR in p\\X.java (at line 10)\n" + 
		"	void m2(ZMember m) {}\n" + 
		"	        ^^^^^^^\n" + 
		"ZMember cannot be resolved to a type\n" + 
		"----------\n");
	}

	public void test006() { // test non static member types
		this.runNegativeTest(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"import static p2.Z.ZStatic;\n" +
				"import static p2.Z.ZNonStatic;\n" +
				"import p2.Z.ZNonStatic;\n" +
				"public class X {\n" +
				"	void m2(ZStatic m) {}\n" +
				"	void m3(ZNonStatic m) {}\n" +
				"}\n",
				"p2/Z.java",
				"package p2;\n" +
				"public class Z {\n" +
				"	public static class ZStatic {}\n" +
				"	public class ZNonStatic {}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in p\\X.java (at line 3)\n" + 
			"	import static p2.Z.ZNonStatic;\n" + 
			"	              ^^^^^^^^^^^^^^^\n" + 
			"The import p2.Z.ZNonStatic cannot be resolved\n" + 
			"----------\n");
	}

	public void test007() { // test non static member types vs. static field
		this.runConformTest(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"import static p2.Z.ZFieldOverMember;\n" +
				"public class X {\n" +
				"	int z = ZFieldOverMember;\n" +
				"}\n",
				"p2/Z.java",
				"package p2;\n" +
				"public class Z {\n" +
				"	public static int ZFieldOverMember = 1;\n" +
				"	public class ZFieldOverMember {}\n" +
				"}\n",
			},
			"");
	}

	public void test008() { // test static top level types
		this.runNegativeTest(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"import static java.lang.System;\n" +
				"public class X {}\n",
			},
			"----------\n" + 
			"1. ERROR in p\\X.java (at line 2)\n" + 
			"	import static java.lang.System;\n" + 
			"	              ^^^^^^^^^^^^^^^^\n" + 
			"The static import java.lang.System must be a field or member type\n" + 
			"----------\n");
	}

	public void test009() { // test static top level types
		this.runNegativeTest(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"import static java.lang.reflect.Method.*;\n" +
				"public class X {Method m;}\n",
			},
			"----------\n" + 
			"1. ERROR in p\\X.java (at line 3)\n" + 
			"	public class X {Method m;}\n" + 
			"	                ^^^^^^\n" + 
			"Method cannot be resolved to a type\n" + 
			"----------\n");
	}
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=76174
	public void test010() { 
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static java.lang.System.*;\n" +
				"public class X {\n" +
				"	void foo() { arraycopy(); }\n" +
				"}\n"
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 3)\n" + 
			"	void foo() { arraycopy(); }\n" + 
			"	             ^^^^^^^^^\n" + 
			"The method arraycopy(Object, int, Object, int, int) in the type System is not applicable for the arguments ()\n" + 
			"----------\n");
	}
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=76360
	public void test011() { 
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static p.Y.*;\n" +
				"public class X extends p.Z {}\n" +
				"class XX extends M.N {}\n" +
				"class XXX extends M.Missing {}\n",
				"p/YY.java",
				"package p;\n" +
				"public class YY {\n" +
				"	public static class M {\n" +
				"		public static class N {}\n" +
				"	}\n" +
				"}\n",
				"p/Y.java",
				"package p;\n" +
				"public class Y extends YY {}\n",
				"p/Z.java",
				"package p;\n" +
				"public class Z {}\n"
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 4)\n" + 
			"	class XXX extends M.Missing {}\n" + 
			"	                  ^^^^^^^^^\n" + 
			"M.Missing cannot be resolved to a type\n" + 
			"----------\n");
	}

	public void test012() {
		this.runConformTest(
			new String[] {
				"X.java",
				"import static java.lang.Math.*;\n" +
				"public class X {\n" +
				"	public static void main(String[] s) {\n" +
				"		System.out.println(max(1, 2));\n" +
				"	}\n" +
				"}\n",
			},
			"2");
		this.runConformTest(
			new String[] {
				"X.java",
				"import static java.lang.Math.max;\n" +
				"public class X {\n" +
				"	public static void main(String[] s) {\n" +
				"		System.out.println(max(1, 3));\n" +
				"	}\n" +
				"}\n",
			},
			"3");
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p1.C.F;\n" +
				"import p2.*;\n" +
				"public class X implements F {" +
				"	int i = F();" +
				"}\n",
				"p1/C.java",
				"package p1;\n" +
				"public class C {\n" +
				"	public static int F() { return 0; }\n" +
				"}\n",
				"p2/F.java",
				"package p2;\n" +
				"public interface F {}\n"
			},
			""
		);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=77955
	public void test013() { 
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static p.Y.ZZ;\n" + // found if ZZ is static
				"import static p.Z.ZZ.WW;\n" + // found if WW is static
				"import static p.Z.Zz.WW;\n" + // found if WW is static
				"import static p.Z.Zz.*;\n" + // legal
				"import static p.Z.Zz.Zzz;\n" + // legal

				"import static p.Y.Zz;\n" + // Zz is not static
				"import static p.Z.Zz.WW.*;\n" + // import requires canonical name for p.W.WW

				"import p.Y.ZZ;\n" + // import requires canonical name for p.Z.ZZ
				"import static p.Y.ZZ.*;\n" + // import requires canonical name for p.Z.ZZ
				"import static p.Y.ZZ.WW;\n" + // import requires canonical name for p.Z.ZZ
				"import static p.Y.ZZ.WW.*;\n" + // import requires canonical name for p.W.WW
				"import static p.Y.ZZ.ZZZ;\n" + // import requires canonical name for p.Z.ZZ
				"import static p.Y.ZZ.WW.WWW;\n" + // import requires canonical name for p.W.WW
				"public class X {\n" +
				"	int i = Zzz + Zzzz;\n" +
				"	ZZ z;\n" +
				"	WW w;\n" +
				"}\n",
				"p/Y.java",
				"package p;\n" +
				"public class Y extends Z {}\n",
				"p/Z.java",
				"package p;\n" +
				"public class Z {\n" +
				"	public class Zz extends W { public static final int Zzz = 0; public static final int Zzzz = 1; }\n" +
				"	public static class ZZ extends W { public static final int ZZZ = 0; }\n" +
				"}\n",
				"p/W.java",
				"package p;\n" +
				"public class W {\n" +
				"	public static class WW { public static final int WWW = 0; }\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 6)\r\n" + 
			"	import static p.Y.Zz;\r\n" + 
			"	              ^^^^^^\n" + 
			"The import p.Y.Zz cannot be resolved\n" + 
			"----------\n" + 
			"2. ERROR in X.java (at line 7)\r\n" + 
			"	import static p.Z.Zz.WW.*;\r\n" + 
			"	              ^^^^^^^^^\n" + 
			"The import p.Z.Zz.WW cannot be resolved\n" + 
			"----------\n" + 
			"3. ERROR in X.java (at line 8)\r\n" + 
			"	import p.Y.ZZ;\r\n" + 
			"	       ^^^^^^\n" + 
			"The import p.Y.ZZ cannot be resolved\n" + 
			"----------\n" + 
			"4. ERROR in X.java (at line 9)\r\n" + 
			"	import static p.Y.ZZ.*;\r\n" + 
			"	              ^^^^^^\n" + 
			"The import p.Y.ZZ cannot be resolved\n" + 
			"----------\n" + 
			"5. ERROR in X.java (at line 10)\r\n" + 
			"	import static p.Y.ZZ.WW;\r\n" + 
			"	              ^^^^^^\n" + 
			"The import p.Y.ZZ cannot be resolved\n" + 
			"----------\n" + 
			"6. ERROR in X.java (at line 11)\r\n" + 
			"	import static p.Y.ZZ.WW.*;\r\n" + 
			"	              ^^^^^^\n" + 
			"The import p.Y.ZZ cannot be resolved\n" + 
			"----------\n" + 
			"7. ERROR in X.java (at line 12)\r\n" + 
			"	import static p.Y.ZZ.ZZZ;\r\n" + 
			"	              ^^^^^^\n" + 
			"The import p.Y.ZZ cannot be resolved\n" + 
			"----------\n" + 
			"8. ERROR in X.java (at line 13)\r\n" + 
			"	import static p.Y.ZZ.WW.WWW;\r\n" + 
			"	              ^^^^^^\n" + 
			"The import p.Y.ZZ cannot be resolved\n" + 
			"----------\n"
		);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=78056
	public void test014() { 
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.Z.ZZ.ZZZ;\n" +
				"public class X {}\n",
				"p/Z.java",
				"package p;\n" +
				"public class Z {\n" +
				"	public class ZZ { public static final  int ZZZ = 0; }\n" +
				"}\n",
			},
			""
		);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=78075
	public void test015() { 
		this.runConformTest(
			new String[] {
				"X.java",
				"import p.Z.*;\n" +
				"import static p.Z.*;\n" +
				"public class X { int i = COUNT; }\n",
				"p/Z.java",
				"package p;\n" +
				"public class Z {\n" +
				"	public static final  int COUNT = 0;\n" +
				"}\n",
			},
			""
		);
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.Z.*;\n" +
				"import p.Z.*;\n" +
				"public class X { int i = COUNT; }\n",
				"p/Z.java",
				"package p;\n" +
				"public class Z {\n" +
				"	public static final  int COUNT = 0;\n" +
				"}\n",
			},
			""
		);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=77630
	public void test016() { 
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static java.lang.*;\n" +
				"public class X {}\n"
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 1)\r\n" + 
			"	import static java.lang.*;\r\n" + 
			"	              ^^^^^^^^^\n" + 
			"Only a type can be imported. java.lang resolves to a package\n" + 
			"----------\n"
		);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=81724
	public void test017() {
		this.runConformTest(
			new String[] {
				"bug/A.java",
				"package bug;\n" +
				"import static bug.C.*;\n" +
				"public class A {\n" +
				"   private B b;\n" +
				"}\n",
				"bug/B.java",
				"package bug;\n" +
				"import static bug.C.*;\n" +
				"public class B {\n" +
				"}\n",
				"bug/C.java",
				"package bug;\n" +
				"public class C {\n" +
				"   private B b;\n" +
				"}\n",
			},
			""
		);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=81724 - variation
	public void test018() {
		this.runNegativeTest(
			new String[] {
				"bug/A.java",
				"package bug;\n" +
				"import static bug.C.*;\n" +
				"public class A {\n" +
				"   private B b2 = b;\n" +
				"}\n",
				"bug/B.java",
				"package bug;\n" +
				"import static bug.C.*;\n" +
				"public class B {\n" +
				"}\n",
				"bug/C.java",
				"package bug;\n" +
				"public class C {\n" +
				"   private static B b;\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in bug\\A.java (at line 4)\n" + 
			"	private B b2 = b;\n" + 
			"	               ^\n" + 
			"The field C.b is not visible\n" + 
			"----------\n" + 
			"----------\n" + 
			"1. WARNING in bug\\B.java (at line 2)\n" + 
			"	import static bug.C.*;\n" + 
			"	              ^^^^^\n" + 
			"The import bug.C is never used\n" + 
			"----------\n" + 
			"----------\n" + 
			"1. WARNING in bug\\C.java (at line 3)\n" + 
			"	private static B b;\n" + 
			"	                 ^\n" + 
			"The field C.b is never read locally\n" + 
			"----------\n");
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=81718
	public void test019() {
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static java.lang.Math.PI;\n" + 
				"\n" + 
				"public class X {\n" + 
				"  boolean PI;\n" + 
				"  Zork z;\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 5)\n" + 
			"	Zork z;\n" + 
			"	^^^^\n" + 
			"Zork cannot be resolved to a type\n" + 
			"----------\n");
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=82754
	public void test020() {
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static java.lang.Math.round;\n" + 
				"public class X {\n" + 
				"  void foo() { cos(0); }\n" +
				"}\n"
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 3)\n" + 
			"	void foo() { cos(0); }\n" + 
			"	             ^^^\n" + 
			"The method cos(int) is undefined for the type X\n" + 
			"----------\n"	);
	}

	public void test021() {
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.B.foo;\n" + 
				"public class X {\n" + 
				"  void test() { foo(); }\n" +
				"}\n",
				"p/A.java",
				"package p;\n" + 
				"public class A { public static void foo() {} }\n",
				"p/B.java",
				"package p;\n" + 
				"public class B extends A { }\n"
			},
			""
		);
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static p.B.foo;\n" + 
				"public class X {\n" + 
				"  void test() { foo(); }\n" +
				"}\n",
				"p/A.java",
				"package p;\n" + 
				"public class A { public void foo() {} }\n",
				"p/B.java",
				"package p;\n" + 
				"public class B extends A { static void foo(int i) {} }\n"
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 1)\n" + 
			"	import static p.B.foo;\n" + 
			"	              ^^^^^^^\n" + 
			"The import p.B.foo cannot be resolved\n" + 
			"----------\n" + 
			"2. ERROR in X.java (at line 3)\n" + 
			"	void test() { foo(); }\n" + 
			"	              ^^^\n" + 
			"The method foo() is undefined for the type X\n" + 
			"----------\n"
		);
	}

	public void test022() { // test field/method collisions
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.A.F;\n" + 
				"import static p.B.F;\n" + 
				"public class X {\n" + 
				"	int i = F;\n" +
				"}\n",
				"p/A.java",
				"package p;\n" + 
				"public class A { public static class F {} }\n",
				"p/B.java",
				"package p;\n" + 
				"public class B { public static int F = 2; }\n",
			},
			""
			// no collision between field and member type
		);
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.A.F;\n" + 
				"import static p.B.F;\n" + 
				"public class X {\n" + 
				"	int i = F + F();\n" +
				"}\n",
				"p/A.java",
				"package p;\n" + 
				"public class A { public static int F() { return 1; } }\n",
				"p/B.java",
				"package p;\n" + 
				"public class B { public static int F = 2; }\n",
			},
			""
			// no collision between field and method
		);
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.A.F;\n" + 
				"import static p.B.F;\n" + 
				"public class X {\n" + 
				"	int i = F;\n" +
				"}\n",
				"p/A.java",
				"package p;\n" + 
				"public class A { public static int F = 1; }\n",
				"p/B.java",
				"package p;\n" + 
				"public class B extends A {}\n",
			},
			""
			// no collision between 2 fields that are the same
		);
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static p.A.F;\n" + 
				"import static p.B.F;\n" + 
				"public class X {\n" + 
				"	int i = F;\n" +
				"}\n",
				"p/A.java",
				"package p;\n" + 
				"public class A { public static int F = 1; }\n",
				"p/B.java",
				"package p;\n" + 
				"public class B { public static int F = 2; }\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 2)\n" + 
			"	import static p.B.F;\n" + 
			"	              ^^^^^\n" + 
			"The import p.B.F collides with another import statement\n" + 
			"----------\n"
			// F is already defined in a single-type import
		);
	}

	public void test023() {
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.A.C;\n" + 
				"public class X {\n" + 
				"	public static void main(String[] args) {\n" +
				"		System.out.print(C);\n" +
				"		System.out.print(C());\n" +
				"	}\n" +
				"}\n",
				"p/A.java",
				"package p;\n" + 
				"public class A {\n" +
				"	public static int C = 1;\n" +
				"	public static int C() { return C + 3; }\n" +
				"}\n"
			},
			"14"
		);
		this.runConformTest( // extra inheritance hiccup for method lookup
			new String[] {
				"X.java",
				"import static p.A.C;\n" + 
				"public class X {\n" + 
				"	public static void main(String[] args) {\n" +
				"		System.out.print(C);\n" +
				"		System.out.print(C());\n" +
				"	}\n" +
				"}\n",
				"p/A.java",
				"package p;\n" + 
				"public class A extends B {\n" +
				"	public static int C() { return C + 3; }\n" +
				"}\n",
				"p/B.java",
				"package p;\n" + 
				"public class B {\n" +
				"	public static int C = 1;\n" +
				"}\n"
			},
			"14"
		);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=83376
	public void test024() {
		this.runNegativeTest(
			new String[] {
				"p/B.java",
				"package p;\n" + 
				"import static p.A.m;\n" + 
				"import static p2.C.m;\n" + 
				"class A { static void m() {} }\n" + 
				"public class B { public static void main(String[] args) { m(); } }\n",
				"p2/C.java",
				"package p2;\n" + 
				"public class C { public static void m() {} }\n"
			},
			"----------\n" + 
			"1. ERROR in p\\B.java (at line 5)\r\n" + 
			"	public class B { public static void main(String[] args) { m(); } }\r\n" + 
			"	                                                          ^\n" + 
			"The method m() is ambiguous for the type B\n" + 
			"----------\n"
		);
		this.runConformTest(
			new String[] {
				"p/X.java",
				"package p;\n" + 
				"import static p.A.m;\n" + 
				"import static p.B.m;\n" + 
				"public class X { void test() { m(); } }\n" + 
				"class B extends A {}\n",
				"p/A.java",
				"package p;\n" + 
				"public class A { public static int m() { return 0; } }\n"
			},
			""
		);
	}

	public void test025() {
		this.runConformTest(
			new String[] {
				"X.java",
				"import static java.lang.Math.*;\n" + 
				"public class X {\n" + 
				"	public static void main(String[] s) {\n" + 
				"		System.out.print(max(PI, 4));\n" + 
				"		new Runnable() {\n" + 
				"			public void run() {\n" + 
				"				System.out.println(max(PI, 5));\n" + 
				"			}\n" + 
				"		}.run();\n" + 
				"	}\n" + 
				"}\n"
			},
			"4.05.0"
		);
	}

	public void test026() { // ensure inherited problem fields do not stop package resolution
		this.runConformTest(
			new String[] {
				"X.java",
				"public class X extends Y { static void test() { java.lang.String.valueOf(0); } }\n" + 
				"class Y { private String java; }\n"
			},
			""
		);
	}
	
	public void test027() {
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static p.ST.foo;\n" + 
				"public class X {\n" + 
				"	\n" + 
				"	foo bar;\n" + 
				"}\n", 
				"p/ST.java",
				"package p; \n" + 
				"public class ST {\n" + 
				"	public static int foo;\n" + 
				"}\n"	,			
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 4)\n" + 
			"	foo bar;\n" + 
			"	^^^\n" + 
			"foo cannot be resolved to a type\n" + 
			"----------\n");
	}

	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=87490
	public void test028() {
		this.runConformTest(
			new String[] {
				"p1/Z.java",//====================
				"package p1;\n" + 
				"public class Z {\n" + 
				"	public interface I {\n" + 
				"	}\n" + 
				"}\n",
				"q/Y.java",//====================
				"package q;\n" + 
				"import static p.X.I;\n" + 
				"import static p1.Z.I;\n" + 
				"public class Y implements I {\n" + 
				"}\n",
				"p/X.java",//====================
				"package p;\n" + 
				"public enum X {\n" + 
				"	I, J, K\n" + 
				"}\n"	,			
			},
			"");
		// recompile Y against binaries
		this.runConformTest(
			new String[] {
				"q/Y.java",//====================
				"package q;\n" + 
				"import static p.X.I;\n" + 
				"import static p1.Z.I;\n" + 
				"public class Y implements I {\n" + 
				"}\n",
			},
			"",
			null,
			false,
			null);
	}

	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=93913
	public void test029() {
		this.runNegativeTest(
			new String[] {
				"p1/A.java",
				"package p1;\n" + 
				"import static p2.C.B;\n" + 
				"public class A extends B {\n" + 
				"	void test() {" +
				"		int i = B();\n" +
				"		B b = null;\n" +
				"		b.fooB();\n" +
				"		b.fooC();\n" +
				"		fooC();\n" +
				"	}\n" + 
				"}\n",
				"p1/B.java",
				"package p1;\n" + 
				"public class B {\n" + 
				"	public void fooB() {}\n" + 
				"}\n",
				"p2/C.java",
				"package p2;\n" + 
				"public class C {\n" + 
				"	public static class B { public void fooC() {} }\n" + 
				"	public static int B() { return 0; }\n" + 
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in p1\\A.java (at line 6)\n" + 
			"	b.fooB();\n" + 
			"	  ^^^^\n" + 
			"The method fooB() is undefined for the type C.B\n" + 
			"----------\n"
		);
	}

	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=94262
	public void test030() {
		this.runNegativeTest(
			new String[] {
				"p2/Test.java",
				"package p2;\n" + 
				"import static p1.A.*;\n" + 
				"public class Test {\n" + 
				"	Inner1 i; // not found\n" + 
				"	Inner2 j;\n" + 
				"}\n",
				"p1/A.java",
				"package p1;\n" + 
				"public class A {\n" + 
				"	public class Inner1 {}\n" +
				"	public static class Inner2 {}\n" + 
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in p2\\Test.java (at line 4)\n" + 
			"	Inner1 i; // not found\n" + 
			"	^^^^^^\n" + 
			"Inner1 cannot be resolved to a type\n" + 
			"----------\n"
		);
		this.runConformTest(
			new String[] {
				"p2/Test.java",
				"package p2;\n" + 
				"import p1.A.*;\n" + 
				"import static p1.A.*;\n" + 
				"import static p1.A.*;\n" + 
				"public class Test {\n" + 
				"	Inner1 i;\n" + 
				"	Inner2 j;\n" + 
				"}\n",
				"p1/A.java",
				"package p1;\n" + 
				"public class A {\n" + 
				"	public class Inner1 {}\n" +
				"	public static class Inner2 {}\n" + 
				"}\n",
			},
			""
		);
	}

	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=95909
	public void test031() {
		this.runNegativeTest(
			new String[] {
				"PointRadius.java",
				"import static java.lang.Math.sqrt;\n" + 
				"\n" + 
				"public class PointRadius {\n" + 
				"\n" + 
				"	public static void main(String[] args) {\n" + 
				"		double radius = 0;\n" + 
				"		radius = sqrt(pondArea / Math.PI);\n" + 
				"\n" + 
				"	}\n" + 
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in PointRadius.java (at line 7)\n" + 
			"	radius = sqrt(pondArea / Math.PI);\n" + 
			"	              ^^^^^^^^\n" + 
			"pondArea cannot be resolved\n" + 
			"----------\n");
	}

	//http://bugs.eclipse.org/bugs/show_bug.cgi?id=97809
	public void test032() {
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.A.*;\n" + 
				"import static p.B.*;\n" + 
				"public class X {\n" + 
				"	public static void main(String[] args) {foo();}\n" + 
				"}\n",
				"p/A.java",
				"package p;" +
				"public class A {\n" + 
				"	public static void foo() {System.out.print(false);}\n" + 
				"}\n",
				"p/B.java",
				"package p;" +
				"public class B extends A {\n" + 
				"	public static void foo() {System.out.print(true);}\n" + 
				"}\n"
			},
			"true");
	}

	//http://bugs.eclipse.org/bugs/show_bug.cgi?id=97809
	public void test032b() {
		this.runNegativeTest(
			new String[] {
				"X2.java",
				"import static p2.A.*;\n" + 
				"import static p2.B.*;\n" + 
				"public class X2 { void test() {foo();} }\n",
				"p2/A.java",
				"package p2;" +
				"public class A {\n" + 
				"	public static void foo() {}\n" + 
				"}\n",
				"p2/B.java",
				"package p2;" +
				"public class B {\n" + 
				"	public static void foo() {}\n" + 
				"}\n"
			},
			"----------\n" + 
			"1. ERROR in X2.java (at line 3)\r\n" + 
			"	public class X2 { void test() {foo();} }\r\n" + 
			"	                               ^^^\n" + 
			"The method foo() is ambiguous for the type X2\n" + 
			"----------\n"
			// reference to foo is ambiguous, both method foo() in p.B and method foo() in p.A match
		);
	}

	//http://bugs.eclipse.org/bugs/show_bug.cgi?id=97809
	public void test032c() {
		this.runConformTest(
			new String[] {
				"X3.java",
				"import static p3.A.*;\n" + 
				"import static p3.B.foo;\n" + 
				"public class X3 {\n" + 
				"	public static void main(String[] args) {foo();}\n" + 
				"}\n",
				"p3/A.java",
				"package p3;" +
				"public class A {\n" + 
				"	public static void foo() {System.out.print(false);}\n" + 
				"}\n",
				"p3/B.java",
				"package p3;" +
				"public class B {\n" + 
				"	public static void foo() {System.out.print(true);}\n" + 
				"}\n"
			},
			"true");
	}

	//http://bugs.eclipse.org/bugs/show_bug.cgi?id=97809
	public void test032d() {
		this.runConformTest(
			new String[] {
				"X4.java",
				"import static p4.A.foo;\n" + 
				"import static p4.B.*;\n" + 
				"public class X4 {\n" + 
				"	public static void main(String[] args) {foo();}\n" + 
				"}\n",
				"p4/A.java",
				"package p4;" +
				"public class A {\n" + 
				"	public static void foo() {System.out.print(true);}\n" + 
				"}\n",
				"p4/B.java",
				"package p4;" +
				"public class B extends A {\n" + 
				"	public static void foo() {System.out.print(false);}\n" + 
				"}\n"
			},
			"true");
	}

	public void test033() {
		this.runConformTest(
			new String[] {
				"X.java",
				"import static p.A.*;\n" + 
				"import static p.B.*;\n" + 
				"public class X {\n" + 
				"	public static void main(String[] args) {foo(\"aa\");}\n" + 
				"}\n",
				"p/A.java",
				"package p;" +
				"public class A {\n" + 
				"	public static <U> void foo(U u) {System.out.print(false);}\n" + 
				"}\n",
				"p/B.java",
				"package p;" +
				"public class B extends A {\n" + 
				"	public static <V> void foo(String s) {System.out.print(true);}\n" + 
				"}\n"
			},
			"true");
	}

	public void test033b() {
		this.runConformTest(
			new String[] {
				"X2.java",
				"import static p2.A.*;\n" + 
				"import static p2.B.*;\n" + 
				"public class X2 {\n" + 
				"	public static void main(String[] args) {foo(\"aa\");}\n" + 
				"}\n",
				"p2/A.java",
				"package p2;" +
				"public class A {\n" + 
				"	public static <U> void foo(String s) {System.out.print(true);}\n" + 
				"}\n",
				"p2/B.java",
				"package p2;" +
				"public class B extends A {\n" + 
				"	public static <V> void foo(V v) {System.out.print(false);}\n" + 
				"}\n"
			},
			"true");
	}
	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=104198
	public void test034() {
		this.runConformTest(
			new String[] {
				"test/AbstractTest.java",
				"package test;\n" +
				"public abstract class AbstractTest<Z> {\n" + 
				"  \n" + 
				"  public abstract MyEnum m(Z z);\n" + 
				"  \n" + 
				"  public enum MyEnum {\n" + 
				"    A,B\n" + 
				"  }\n" + 
				"}\n",
				"test/X.java",
				"package test;\n" +
				"import static test.AbstractTest.MyEnum.*;\n" +
				"public class X extends AbstractTest<String> {\n" + 
				"  @Override public MyEnum m(String s) {\n" + 
				"    return A;\n" + 
				"  }\n" + 
				"}\n"
			},
			"");
	}
	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=117861
	public void test035() {
		this.runConformTest(
			new String[] {
				"Bug.java",
				"import static java.lang.String.format;\n" +
				"public class Bug extends p.TestCase {\n" + 
				"	public static void main(String[] args) {\n" + 
				"		String msg = \"test\";\n" + 
				"		System.out.print(format(msg));\n" + 
				"		System.out.print(format(msg, 1, 2));\n" +
				"	}\n" + 
				"}\n",
				"p/TestCase.java",
				"package p;\n" + 
				"public class TestCase {\n" + 
				"	static String format(String message, Object expected, Object actual) {return null;}\n" + 
				"}\n"
			},
			"testtest");
		this.runNegativeTest(
			new String[] {
				"C.java",
				"class A {\n" + 
				"	static class B { void foo(Object o, String s) {} }\n" + 
				"	void foo(int i) {}\n" + 
				"}\n" +
				"class C extends A.B {\n" +
				"	void test() { foo(1); }\n" +
				"}\n"
			},
			"----------\n" + 
			"1. ERROR in C.java (at line 6)\r\n" + 
			"	void test() { foo(1); }\r\n" + 
			"	              ^^^\n" + 
			"The method foo(Object, String) in the type A.B is not applicable for the arguments (int)\n" + 
			"----------\n");
		this.runNegativeTest(
			new String[] {
				"A.java",
				"public class A {\n" + 
				"  void foo(int i, long j) {}\n" + 
				"  class B {\n" + 
				"    void foo() { foo(1, 1); }\n" + 
				"  }\n" + 
				"}",
			}, 
			"----------\n" + 
			"1. ERROR in A.java (at line 4)\n" + 
			"	void foo() { foo(1, 1); }\n" + 
			"	             ^^^\n" + 
			"The method foo() in the type A.B is not applicable for the arguments (int, int)\n" + 
			"----------\n"
		);
	}
	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=126564
	public void _test036() {
		this.runNegativeTest(
			new String[] {
				"X.java",
				"import static p.A.CONSTANT_I;\n" + 
				"import static p.A.CONSTANT_B;\n" + 
				"public class X {\n" + 
				"  static int i = p.A.CONSTANT_I;\n" + 
				"  static int j = p.A.CONSTANT_B;\n" + 
				"  static int m = CONSTANT_I;\n" + 
				"  static int n = CONSTANT_B;\n" + 
				"}",
				"p/A.java",
				"package p;\n" + 
				"public class A extends B implements I {}\n" + 
				"interface I { int CONSTANT_I = 1; }\n" + 
				"class B { int CONSTANT_B = 1; }",
			}, 
			"----------\n" + 
			"1. ERROR in X.java (at line 1)\n" + 
			"	import static p.A.CONSTANT_I;\n" + 
			"	              ^^^^^^^^^^^^^^\n" + 
			"The type I is not visible\n" + 
			"----------\n" + 
			"2. ERROR in X.java (at line 2)\n" + 
			"	import static p.A.CONSTANT_B;\n" + 
			"	              ^^^^^^^^^^^^^^\n" + 
			"The type B is not visible\n" + 
			"----------\n" + 
			"3. ERROR in X.java (at line 4)\n" + 
			"	static int i = p.A.CONSTANT_I;\n" + 
			"	               ^^^^^^^^^^^^^^\n" + 
			"The type I is not visible\n" + 
			"----------\n" + 
			"4. ERROR in X.java (at line 5)\n" + 
			"	static int j = p.A.CONSTANT_B;\n" + 
			"	               ^^^^^^^^^^^^^^\n" + 
			"The type B is not visible\n" + 
			"----------\n" + 
			"5. ERROR in X.java (at line 6)\n" + 
			"	static int m = CONSTANT_I;\n" + 
			"	               ^^^^^^^^^^\n" + 
			"CONSTANT_I cannot be resolved\n" + 
			"----------\n" + 
			"6. ERROR in X.java (at line 7)\n" + 
			"	static int n = CONSTANT_B;\n" + 
			"	               ^^^^^^^^^^\n" + 
			"CONSTANT_B cannot be resolved\n" + 
			"----------\n"
		);
	}
}
