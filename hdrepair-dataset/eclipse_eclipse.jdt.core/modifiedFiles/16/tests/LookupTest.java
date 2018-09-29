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

import java.io.File;
import java.util.Hashtable;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.tests.util.Util;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;

import junit.framework.Test;
/**
 * Name Lookup within Inner Classes
 * Creation date: (8/2/00 12:04:53 PM)
 * @author Dennis Conway
 */
public class LookupTest extends AbstractRegressionTest {
public LookupTest(String name) {
	super(name);
}
public static Test suite() {
	return setupSuite(testClass());
}
/**
 * Non-static member class
 */
public void test001() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A {									\n"+
			"	private static int value = 23;					\n"+
			"	class B {										\n"+
			"		private int value;							\n"+
			"		B (int val) {								\n"+
			"			value = (A.value * 2) + val;			\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String args[]) {		\n"+
			"		int result = new A().new B(12).value; 		\n"+
			"		int expected = 58; 							\n"+
			"		System.out.println( 						\n"+
			"			result == expected 						\n"+
			"				? \"SUCCESS\"  						\n"+
			"				: \"FAILED : got \"+result+\" instead of \"+ expected); \n"+
			"	}												\n"+
			"}"
		},
		"SUCCESS"	
	);									
}
/**
 * Attempt to access non-static field from static inner class (illegal)
 */
public void test002() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"class A {											\n"+
			"	private int value;								\n"+
			"	static class B {								\n"+
			"		B () {										\n"+
			"			value = 2;								\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		B result = new B();							\n"+
			"	}												\n"+
			"}"
		},
		"----------\n" + 
		"1. WARNING in p1\\A.java (at line 3)\n" + 
		"	private int value;								\n" + 
		"	            ^^^^^\n" + 
		"The field A.value is never read locally\n" + 
		"----------\n" + 
		"2. ERROR in p1\\A.java (at line 6)\n" + 
		"	value = 2;								\n" + 
		"	^^^^^\n" + 
		"Cannot make a static reference to the non-static field value\n" + 
		"----------\n");									
}
/**
 * Access static field from static inner class
 */
public void test003() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A {									\n"+
			"	private static int value;						\n"+
			"	static class B {								\n"+
			"		B () {										\n"+
			"			value = 2;								\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		B result = new B();							\n"+
			"		System.out.println(\"SUCCESS\");			\n"+
			"	}												\n"+
			"}",
			"SUCCESS"
		}
	);									
}
/**
 * 
 */
public void test004() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A {									\n"+
			"	private String value;							\n"+
			"	private A (String strIn) {						\n"+
			"		value = new B(strIn, \"E\").str;			\n"+
			"	}												\n"+
			"	class B {										\n"+
			"		String str;									\n"+
			"			private B (String strFromA, String strIn)	{\n"+
			"				str = strFromA + strIn + new C(\"S\").str;\n"+
			"			}										\n"+
			"		class C {									\n"+
			"			String str;								\n"+
			"			private C (String strIn) {				\n"+
			"				str = strIn + new D(\"S\").str;		\n"+
			"			}										\n"+
			"			class D {								\n"+
			"				String str;							\n"+
			"				private D (String strIn) {			\n"+
			"					str = strIn;					\n"+
			"				}									\n"+
			"			}										\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		System.out.println(new A(\"SUCC\").value);	\n"+
			"	}												\n"+
			"}"
		},
		"SUCCESS"
	);
}
/**
 * 
 */
public void test005() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A {									\n"+
			"	private static void doSomething(String showThis) {\n"+
			"		System.out.print(showThis);					\n"+
			"		return;										\n"+
			"	}												\n"+
			"	class B {										\n"+
			"		void aMethod () {							\n"+
			"			p1.A.doSomething(\"SUCC\");				\n"+
			"			A.doSomething(\"ES\");					\n"+
			"			doSomething(\"S\");						\n"+
			"		}											\n"+		
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		B foo = new A().new B();					\n"+
			"		foo.aMethod();								\n"+
			"	}												\n"+
			"}"
		},
		"SUCCESS"
	);
}
/**
 * jdk1.2.2 reports: No variable sucess defined in nested class p1.A. B.C.
 * jdk1.3 reports: success has private access in p1.A
 */
public void test006() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"class A {											\n"+
			"	private static String success = \"SUCCESS\";	\n"+
			"	public interface B {							\n"+
			"		public abstract void aTask();				\n"+
			"		class C extends A implements B {			\n"+
			"			public void aTask() {System.out.println(this.success);}\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String[] argv) {		\n"+
			"	}												\n"+
			"}"
		},
		"----------\n" + 
		"1. WARNING in p1\\A.java (at line 3)\n" + 
		"	private static String success = \"SUCCESS\";	\n" + 
		"	                      ^^^^^^^\n" + 
		"The field A.success is never read locally\n" + 
		"----------\n" + 
		"2. ERROR in p1\\A.java (at line 7)\n" + 
		"	public void aTask() {System.out.println(this.success);}\n" + 
		"	                                             ^^^^^^^\n" + 
		"The field A.success is not visible\n" + 
		"----------\n");
}
/**
 * No errors in jdk1.2.2, jdk1.3
 */
public void test007() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A {									\n"+
			"	private static String success = \"SUCCESS\";	\n"+
			"	public interface B {							\n"+
			"		public abstract void aTask();				\n"+	
			"		class C extends A implements B {			\n"+
			"			public void aTask() {System.out.println(A.success);}\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String[] argv) {		\n"+
			"	}												\n"+
			"}"
		}
	);
}
/**
 * jdk1.2.2 reports: Undefined variable: A.this
 * jdk1.3 reports: non-static variable this cannot be referenced from a static context
 */
public void test008() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"class A {											\n"+
			"	private static String success = \"SUCCESS\";	\n"+
			"	public interface B {							\n"+
			"		public abstract void aTask();				\n"+	
			"		class C extends A implements B {			\n"+
			"			public void aTask() {System.out.println(A.this.success);}\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String[] argv) {		\n"+
			"	}												\n"+
			"}"
		},
		"----------\n" + 
		"1. ERROR in p1\\A.java (at line 7)\n" + 
		"	public void aTask() {System.out.println(A.this.success);}\n" + 
		"	                                        ^^^^^^\n" + 
		"No enclosing instance of the type A is accessible in scope\n" + 
		"----------\n" + 
		"2. WARNING in p1\\A.java (at line 7)\n" + 
		"	public void aTask() {System.out.println(A.this.success);}\n" + 
		"	                                               ^^^^^^^\n" + 
		"The static field A.success should be accessed in a static way\n" + 
		"----------\n"
	);
}
/**
 * jdk1.2.2 reports: No variable success defined in nested class p1.A. B.C
 * jdk1.3 reports: success has private access in p1.A
 */
public void test009() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"class A {											\n"+
			"	private String success = \"SUCCESS\";			\n"+
			"	public interface B {							\n"+
			"		public abstract void aTask();				\n"+
			"		class C extends A implements B {			\n"+
			"			public void aTask() {System.out.println(this.success);}\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String[] argv) {		\n"+
			"	}												\n"+
			"}"
		},
		"----------\n" + 
		"1. WARNING in p1\\A.java (at line 3)\n" + 
		"	private String success = \"SUCCESS\";			\n" + 
		"	               ^^^^^^^\n" + 
		"The field A.success is never read locally\n" + 
		"----------\n" + 
		"2. ERROR in p1\\A.java (at line 7)\n" + 
		"	public void aTask() {System.out.println(this.success);}\n" + 
		"	                                             ^^^^^^^\n" + 
		"The field A.success is not visible\n" + 
		"----------\n");
}
/**
 * jdk1.2.2 reports: Can't make a static reference to nonstatic variable success in class p1.A
 * jdk1.3 reports: non-static variable success cannot be referenced from a static context
 */
public void test010() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"class A {											\n"+
			"	private String success = \"SUCCESS\";			\n"+
			"	public interface B {							\n"+
			"		public abstract void aTask();				\n"+	
			"		class C extends A implements B {			\n"+
			"			public void aTask() {System.out.println(A.success);}\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String[] argv) {		\n"+
			"	}												\n"+
			"}"
		},
		"----------\n" + 
		"1. WARNING in p1\\A.java (at line 3)\n" + 
		"	private String success = \"SUCCESS\";			\n" + 
		"	               ^^^^^^^\n" + 
		"The field A.success is never read locally\n" + 
		"----------\n" + 
		"2. ERROR in p1\\A.java (at line 7)\n" + 
		"	public void aTask() {System.out.println(A.success);}\n" + 
		"	                                        ^^^^^^^^^\n" + 
		"Cannot make a static reference to the non-static field A.success\n" + 
		"----------\n");
}
/**
 * 
 */
public void test011() {
	this.runNegativeTest(
		new String[] {
			/* p2.Aa */
			"p2/Aa.java",
			"package p2;										\n"+
			"class Aa extends p1.A{								\n"+
			"	class B implements p1.A.C {						\n"+
			"	}												\n"+
			"	public static void main (String args[]) {		\n"+
			"	}												\n"+
			"}",
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A {									\n"+
			"   public A() {									\n"+
			"	}												\n"+
			"	class B implements C {							\n"+
			"		public int sMethod() {						\n"+
			"			return 23;								\n"+
			"		}											\n"+
			"	}												\n"+
			"	public interface C {							\n"+
			"		public abstract int sMethod();				\n"+
			"	}												\n"+
			"}",

		},
		"----------\n" + 
		"1. ERROR in p2\\Aa.java (at line 3)\n" + 
		"	class B implements p1.A.C {						\n" + 
		"	      ^\n" + 
		"The type Aa.B must implement the inherited abstract method A.C.sMethod()\n" + 
		"----------\n"
	);
}
/**
 * 
 */
public void test012() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A {									\n"+
			"	public interface B {							\n"+
			"		public abstract void aMethod (int A);		\n"+
			"		public interface C {						\n"+
			"			public abstract void anotherMethod();	\n"+
			"		}											\n"+
			"	}												\n"+
			"	public class aClass implements B, B.C {			\n"+
			"		public void aMethod (int A) {				\n"+
			"		}											\n"+
			"		public void anotherMethod(){}				\n"+
			"	}												\n"+
			"   	public static void main (String argv[]) {	\n"+
			"		System.out.println(\"SUCCESS\");			\n"+
			"	}												\n"+
			"}"
		},
		"SUCCESS"
	);
}
/**
 * 
 */
public void test013() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A {									\n"+
			"	public interface B {							\n"+
			"		public abstract void aMethod (int A);		\n"+
			"		public interface C {						\n"+
			"			public abstract void anotherMethod(int A);\n"+
			"		}											\n"+
			"	}												\n"+
			"	public class aClass implements B, B.C {			\n"+
			"		public void aMethod (int A) {				\n"+
			"			public void anotherMethod(int A) {};	\n"+
			"		}											\n"+
			"	}												\n"+
			"   	public static void main (String argv[]) {	\n"+
			"		System.out.println(\"SUCCESS\");			\n"+
			"	}												\n"+
			"}"
		},
		"----------\n" + 
		"1. ERROR in p1\\A.java (at line 9)\n" + 
		"	public class aClass implements B, B.C {			\n" + 
		"	             ^^^^^^\n" + 
		"The type A.aClass must implement the inherited abstract method A.B.C.anotherMethod(int)\n" + 
		"----------\n" + 
		"2. ERROR in p1\\A.java (at line 11)\n" + 
		"	public void anotherMethod(int A) {};	\n" + 
		"	                         ^\n" + 
		"Syntax error on token \"(\", ; expected\n" + 
		"----------\n" + 
		"3. ERROR in p1\\A.java (at line 11)\n" + 
		"	public void anotherMethod(int A) {};	\n" + 
		"	                               ^\n" + 
		"Syntax error on token \")\", ; expected\n" + 
		"----------\n"
	);
}
/**
 *
 */
public void test014() {
	this.runNegativeTest(
		new String[] {
			/* pack1.First */
			"pack1/First.java",
			"package pack1;										\n"+
			"public class First {								\n"+
			"	public static void something() {}				\n"+
			"		class Inner {}								\n"+	
			"	public static void main (String argv[]) {		\n"+
			"		First.Inner foo = new First().new Inner();	\n"+
			"		foo.something();							\n"+
			"		System.out.println(\"SUCCESS\");			\n"+
			"	}												\n"+
			"}"
		},
		"----------\n" + 
		"1. ERROR in pack1\\First.java (at line 7)\n" + 
		"	foo.something();							\n" + 
		"	    ^^^^^^^^^\n" + 
		"The method something() is undefined for the type First.Inner\n" + 
		"----------\n"
	);
}
/**
 *
 */
public void test015() {
	this.runConformTest(
		new String[] {
			/* pack1.First */
			"pack1/First.java",
			"package pack1;										\n"+
			"public class First {								\n"+
			"		class Inner {								\n"+
			"			public void something() {}				\n"+
			"		}											\n"+	
			"	public static void main (String argv[]) {		\n"+
			"		First.Inner foo = new First().new Inner();	\n"+
			"		foo.something();							\n"+
			"		System.out.println(\"SUCCESS\");			\n"+
			"	}												\n"+
			"}"
		},
		"SUCCESS"
	);
}
/**
 *
 */
public void test016() {
	this.runConformTest(
		new String[] {
			/* pack1.Outer */
			"pack1/Outer.java",
			"package pack1;										\n"+
			"import pack2.*;									\n"+
			"public class Outer {								\n"+
			"	int time, distance;								\n"+
			"	public Outer() {								\n"+
			"	}												\n"+
			"	public Outer(int d) {							\n"+
			"		distance = d;								\n"+
			"	}												\n"+
			"	public void aMethod() {							\n"+
			"		this.distance *= 2;							\n"+
			"		return;										\n"+
			"	}												\n"+
			"}",
			/* pack2.OuterTwo */
			"pack2/OuterTwo.java",
			"package pack2;										\n"+
			"import pack1.*;									\n"+
			"public class OuterTwo extends Outer {				\n"+
			"	public OuterTwo(int bar) {						\n"+
			"		Outer A = new Outer(3) {					\n"+
			"			public void bMethod(){					\n"+
			"				final class X {						\n"+
			"					int price;						\n"+
			"					public X(int inp) {				\n"+
			"						price = inp + 32;			\n"+
			"					}								\n"+
			"				}									\n"+
			"			}										\n"+
			"		};											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		System.out.println(\"\");					\n"+
			"		OuterTwo foo = new OuterTwo(12);			\n"+
			"		Outer bar = new Outer(8);					\n"+
			"	}												\n"+
			"}"
		}
	);
}
/**
 *
 */
public void test017() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A	{									\n"+
			"	int value;										\n"+
			"	public A(B bVal) {								\n"+
			"		bVal.sval += \"V\";							\n"+
			"	}												\n"+
			"	static class B {								\n"+
			"		public static String sval;					\n"+
			"		public void aMethod() {						\n"+
			"			sval += \"S\";							\n"+
			"			A bar = new A(this);					\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		B foo = new B();							\n"+
			"		foo.sval = \"U\";							\n"+
			"		foo.aMethod();								\n"+
			"		System.out.println(foo.sval);				\n"+
			"	}												\n"+
			"}"
		},
		"USV"
	);
}
/**
 * member class
 */
public void test018() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A	{									\n"+
			"	private String rating;							\n"+
			"	public class B {								\n"+
			"		String rating;								\n"+
			"		public B (A sth) {							\n"+
			"			sth.rating = \"m\";						\n"+
			"			rating = \"er\";						\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		A foo = new A();							\n"+
			"		foo.rating = \"o\";							\n"+
			"		B bar = foo.new B(foo);						\n"+
			"		System.out.println(foo.rating + bar.rating);\n"+
			"	}												\n"+
			"}"
		},
		"mer"
	);
}
/**
 * member class
 */
public void test019() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A	{									\n"+
			"	private String rating;							\n"+
			"	public void setRating(A sth, String setTo) {	\n"+
			"		sth.rating = setTo;							\n"+
			"		return;										\n"+
			"	}												\n"+
			"	public class B {								\n"+
			"		public B (A sth) {							\n"+
			"			setRating(sth, \"m\");					\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		A foo = new A();							\n"+
			"		foo.rating = \"o\";							\n"+
			"		B bar = foo.new B(foo);						\n"+
			"		System.out.println(foo.rating + bar.other);	\n"+
			"	}												\n"+
			"}"
		},
		"----------\n" + 
		"1. ERROR in p1\\A.java (at line 17)\n" + 
		"	System.out.println(foo.rating + bar.other);	\n" + 
		"	                                ^^^^^^^^^\n" + 
		"bar.other cannot be resolved or is not a field\n" + 
		"----------\n"
	);
}
/**
 * member class
 */
public void test020() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A	{									\n"+
			"	private String rating;							\n"+
			"	public class B {								\n"+
			"		public B (A sth) {							\n"+
			"			sth.rating = \"m\";						\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		A foo = new A();							\n"+
			"		foo.rating = \"o\";							\n"+
			"		B bar = foo.new B(foo);						\n"+
			"		System.out.println(foo.rating + bar.other);	\n"+
			"	}												\n"+
			"}"
		},
		"----------\n" + 
		"1. WARNING in p1\\A.java (at line 6)\n" + 
		"	sth.rating = \"m\";						\n" + 
		"	    ^^^^^^\n" + 
		"Write access to enclosing field A.rating is emulated by a synthetic accessor method. Increasing its visibility will improve your performance\n" + 
		"----------\n" + 
		"2. ERROR in p1\\A.java (at line 13)\n" + 
		"	System.out.println(foo.rating + bar.other);	\n" + 
		"	                                ^^^^^^^^^\n" + 
		"bar.other cannot be resolved or is not a field\n" + 
		"----------\n"
	);
}
/**
 * member class
 */
public void test021() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"public class A	{									\n"+
			"	private String rating;							\n"+
			"	public class B {								\n"+
			"		public B (A sth) {							\n"+
			"			sth.rating = \"m\";						\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		A foo = new A();							\n"+
			"		foo.rating = \"o\";							\n"+
			"		B bar = foo.new B(foo);						\n"+
			"		System.out.println(foo.rating);				\n"+
			"	}												\n"+
			"}"
		}
	);
}
/**
 *
 */
public void test022() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;										\n"+
			"import p2.*;										\n"+
			"public class A {									\n"+
			"	public int aValue;								\n"+
			"	public A() {}									\n"+
			"	public static class C extends A {				\n"+
			"		public String aString;						\n"+
			"		public C() {								\n"+
			"		}											\n"+
			"	}												\n"+
			"}",
			/* p2.B */
			"p2/B.java",
			"package p2;										\n"+
			"import p1.*;										\n"+
			"public class B extends A.C {						\n"+
			"	public B() {}									\n"+
			"	public class D extends A {						\n"+
			"		public D() {								\n"+
			"			C val2 = new C();						\n"+
			"			val2.aString = \"s\";					\n"+
			"			A val = new A();						\n"+
			"			val.aValue = 23;						\n"+
			"		}											\n"+
			"	}												\n"+
			"	public static void main (String argv[]) {		\n"+
			"		D foo = new B().new D();					\n"+
			"	}												\n"+
			"}"
		}
	);
}
/**
 *
 */
public void test023() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;\n"+
			"public class A implements B {						\n"+
			"}													\n"+
			"interface B {										\n"+
			"	public class A implements B {					\n"+
			"		public static void main (String argv[]) {	\n"+
			"			class Ba {								\n"+
			"				int time;							\n"+
			"			}										\n"+
			"			Ba foo = new Ba();						\n"+
			"			foo.time = 3;							\n"+
			"		}											\n"+
			"		interface C {								\n"+
			"		}											\n"+
			"		interface Bb extends C {					\n"+
			"		}											\n"+
			"	}												\n"+
			"}"
		}
	);
}
/**
 *
 */
public void test024() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;									\n"+
			"public class A {								\n"+
			"	protected static String bleh;				\n"+
			"	interface B {								\n"+
			"		public String bleh();					\n"+
			"		class C{								\n"+
			"			public String bleh() {return \"B\";}\n"+
			"		}										\n"+
			"	}											\n"+
			"	class C implements B {						\n"+
			"		public String bleh() {return \"A\";}	\n"+
			"	}											\n"+
			"	public static void main(String argv[]) {	\n"+
			"		C foo = new A().new C();				\n"+
			"	}											\n"+
			"}"
		}
	);
}
/**
 *
 */
public void test025() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;								\n"+
			"import p2.*;								\n"+
			"public class A {							\n"+
			"	public static class B {					\n"+
			"		public static int B;				\n"+
			"	}										\n"+
			"	public static void main(String argv[]) {\n"+
			"		B foo = new A.B();					\n"+
			"		B bar = new B();					\n"+
			"		foo.B = 2;							\n"+
			"		p2.B bbar = new p2.B();				\n"+
			"		if (bar.B == 35) {					\n"+
			"			System.out.println(\"SUCCESS\");\n"+
			"		}									\n"+
			"		else {								\n"+
			"			System.out.println(bar.B);		\n"+
			"		}									\n"+
			"	}										\n"+
			"}",
			"p2/B.java",
			"package p2;								\n"+
			"import p1.*;								\n"+
			"public class B extends A {					\n"+
			"	public B() {							\n"+
			"		A.B bleh = new A.B();				\n"+
			"		bleh.B = 35;						\n"+
			"	}										\n"+
			"}"
		},
		"SUCCESS"
	);
}
/**
 *
 */
public void test026() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;								\n"+
			"public class A {							\n"+
			"	public static class B {					\n"+
			"		protected static int B;				\n"+
			"	}										\n"+
			"	public static void main(String argv[]) {\n"+
			"		B foo = new A.B();					\n"+
			"		B bar = new B();					\n"+
			"		B.B = 2;							\n"+
			"		p2.B bbar = new p2.B();				\n"+
			"		if (B.B == 35) {					\n"+
			"			System.out.println(\"SUCCESS\");\n"+
			"		}									\n"+
			"		else {								\n"+
			"			System.out.println(B.B);		\n"+
			"		}									\n"+
			"	}										\n"+
			"}",
			"p2/B.java",
			"package p2;								\n"+
			"import p1.*;								\n"+
			"public class B extends A {					\n"+
			"	public B() {							\n"+
			"		A.B bleh = new A.B();				\n"+
			"		bleh.B = 35;						\n"+
			"	}										\n"+
			"}"
		},
		"----------\n" + 
		"1. ERROR in p2\\B.java (at line 6)\n" + 
		"	bleh.B = 35;						\n" + 
		"	^^^^^^\n" + 
		"The field A.B.B is not visible\n" + 
		"----------\n");
}
/**
 *
 */
public void test027() {
	this.runNegativeTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;								\n"+
			"public class A {							\n"+
			"	protected static class B {				\n"+
			"		public static int B;				\n"+
			"	}										\n"+
			"	public static void main(String argv[]) {\n"+
			"		B foo = new A.B();					\n"+
			"		B bar = new B();					\n"+
			"		B.B = 2;							\n"+
			"		p2.B bbar = new p2.B();				\n"+
			"		if (B.B == 35) {					\n"+
			"			System.out.println(\"SUCCESS\");\n"+
			"		}									\n"+
			"		else {								\n"+
			"			System.out.println(B.B);		\n"+
			"		}									\n"+
			"	}										\n"+
			"}",
			"p2/B.java",
			"package p2;								\n"+
			"import p1.*;								\n"+
			"public class B extends A {					\n"+
			"	public B() {							\n"+
			"		A.B bleh = new A.B();				\n"+
			"		A.B.B = 35;						\n"+
			"	}										\n"+
			"}"
		},
		"----------\n" + 
		"1. ERROR in p2\\B.java (at line 5)\n" + 
		"	A.B bleh = new A.B();				\n" + 
		"	           ^^^^^^^^^\n" + 
		"The constructor A.B() is not visible\n" + 
		"----------\n"
	);
}
/**
 *
 */
public void test028() {
	this.runConformTest(
		new String[] {
			/* p1.A */
			"p1/A.java",
			"package p1;									\n"+
			"public class A {								\n"+
			"	static class B {							\n"+
			"		public static class C {					\n"+
			"			private static int a;				\n"+
			"			private int b;						\n"+
			"		}										\n"+
			"	}											\n"+
			"	class D extends B {							\n"+
			"		int j = p1.A.B.C.a;						\n"+
			"	}											\n"+
			"	public static void main (String argv[]) {	\n"+
			"		System.out.println(\"SUCCESS\");		\n"+
			"	}											\n"+
			"}"
		},
		"SUCCESS"
	);
}

/*
 * http://bugs.eclipse.org/bugs/show_bug.cgi?id=10634
 */
public void test029() {
	this.runNegativeTest(
		new String[] {
			"p1/X.java",
			"package p1;	\n"+
			"import p2.Top;	\n"+
			"public class X extends Top {	\n"+
			"	Member field;	\n"+
			"}	\n",
			"p2/Top.java",
			"package p2;	\n"+
			"public class Top {	\n"+
			"	class Member {	\n"+
			"		void foo(){}	\n"+
			"	}	\n"	+
			"}	\n"	
		},
		"----------\n" + 
		"1. ERROR in p1\\X.java (at line 4)\n" + 
		"	Member field;	\n" + 
		"	^^^^^^\n" + 
		"The type Member is not visible\n" + 
		"----------\n");
}
/*
 * http://bugs.eclipse.org/bugs/show_bug.cgi?id=11435
 * 1.3 compiler must accept classfiles without abstract method (target >=1.2)
 */
public void test030() {

	Hashtable target1_2 = new Hashtable();
	target1_2.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_2);
	
	this.runConformTest(
		new String[] {
			"p1/A.java",
			"package p1; \n"+
			"public abstract class A implements I {	\n" +
			"  public static void main(String[] args) {	\n" +
			"    System.out.println(\"SUCCESS\");	\n" +			
			"  }	\n" +
			"} \n" +
			"interface I {	\n" +
			"	void foo();	\n" +
			"}	\n",
		},
		"SUCCESS", // expected output
		null, // custom classpath
		true, // flush previous output dir content
		null, // special vm args
		target1_2,  // custom options
		null/*no custom requestor*/);

	this.runConformTest(
		new String[] {
			"p1/C.java",
			"package p1; \n"+
			"public class C {	\n" +
			"	void bar(A a){ \n" +
			"		a.foo();	\n" +
			"	}	\n" +
			"  public static void main(String[] args) {	\n" +
			"    System.out.println(\"SUCCESS\");	\n" +			
			"  }	\n" +
			"} \n"
		},
		"SUCCESS", // expected output
		null, // custom classpath
		false, // flush previous output dir content
		null, // special vm args
		null,  // custom options
		null/*no custom requestor*/);
}

/*
 * http://bugs.eclipse.org/bugs/show_bug.cgi?id=11511
 * variant - must filter abstract methods when searching concrete methods
 */
public void test031() {

	this.runConformTest(
		new String[] {
			"p1/X.java",
			"package p1;	\n"+
			"public class X extends AbstractY {	\n"+
			"	public void init() {	\n"+
			"		super.init();	\n"+
			"	}	\n"+
			"	public static void main(String[] arguments) {	\n"+
			"		new X().init();	\n"+
			"	}	\n"+
			"}	\n"+
			"abstract class AbstractY extends AbstractZ implements I {	\n"+
			"	public void init(int i) {	\n"+
			"	}	\n"+
			"}	\n"+
			"abstract class AbstractZ implements I {	\n"+
			"	public void init() {	\n"+
			"		System.out.println(\"SUCCESS\");	\n"+
			"	}	\n"+
			"}	\n"+
			"interface I {	\n"+
			"	void init();	\n"+
			"	void init(int i);	\n"+
			"}	\n"
		},
		"SUCCESS"); // expected output
}

/**
 * http://dev.eclipse.org/bugs/show_bug.cgi?id=29211
 * http://dev.eclipse.org/bugs/show_bug.cgi?id=29213
 */
public void test032() {
	this.runNegativeTest(
		new String[] {
			"X.java", //--------------------------------
			"public class X {\n" + 
			"	public static void main(String[] arguments) {\n" + 
			"		System.out.println(p.Bar.array[0].length);\n" + 
			"		System.out.println(p.Bar.array.length);\n" + 
			"		System.out.println(p.Bar.array[0].foo());\n" + 
			"	}\n" + 
			"}\n",
			"p/Bar.java", //----------------------------
			"package p;\n" + 
			"public class Bar {\n" + 
			"	public static Z[] array;\n" + 
			"}\n" + 
			"class Z {\n" + 
			"	public String foo(){ \n" + 
			"		return \"\";\n" + 
			"	}\n" + 
			"}\n" 
		},
		"----------\n" + 
		"1. ERROR in X.java (at line 3)\n" + 
		"	System.out.println(p.Bar.array[0].length);\n" + 
		"	                   ^^^^^^^^^^^^^^\n" + 
		"The type Z is not visible\n" + 
		"----------\n" + 
		"2. ERROR in X.java (at line 4)\n" + 
		"	System.out.println(p.Bar.array.length);\n" + 
		"	                   ^^^^^^^^^^^^^^^^^^\n" + 
		"The type Z is not visible\n" + 
		"----------\n" + 
		"3. ERROR in X.java (at line 5)\n" + 
		"	System.out.println(p.Bar.array[0].foo());\n" + 
		"	                   ^^^^^^^^^^^^^^\n" + 
		"The type Z is not visible\n" + 
		"----------\n");
}

// 30805 Abstract non-visible method diagnosis fooled by intermediate declarations
public void test033() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //==================================
			"package p;	\n" +
			"public abstract class X {	\n" +
			"	abstract void foo();	\n" +
			"}	\n",
			"q/Y.java", //==================================
			"package q;	\n" +
			"public class Y extends p.X {	\n" +
			"	void foo(){}	\n" +
			"}	\n",
		},
		"----------\n" + 
		"1. ERROR in q\\Y.java (at line 2)\n" + 
		"	public class Y extends p.X {	\n" + 
		"	             ^\n" + 
		"This class must implement the inherited abstract method X.foo(), but cannot override it since it is not visible from Y. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n" + 
		"2. WARNING in q\\Y.java (at line 3)\n" + 
		"	void foo(){}	\n" + 
		"	     ^^^^^\n" + 
		"The method Y.foo() does not override the inherited method from X since it is private to a different package.\n" + 
		"----------\n");
}

// 30805 Abstract non-visible method diagnosis fooled by intermediate declarations
public void test034() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //==================================
			"package p;	\n" +
			"public abstract class X {	\n" +
			"	abstract void foo();	\n" +
			"}	\n",
			"q/Y.java", //==================================
			"package q;	\n" +
			"public abstract class Y extends p.X {	\n" +
			"	void foo(){}	\n" +
			"}	\n" +
			"class Z extends Y {	\n" +
			"}	\n",
		},
		"----------\n" + 
		"1. WARNING in q\\Y.java (at line 3)\n" + 
		"	void foo(){}	\n" + 
		"	     ^^^^^\n" + 
		"The method Y.foo() does not override the inherited method from X since it is private to a different package.\n" + 
		"----------\n" + 
		"2. ERROR in q\\Y.java (at line 5)\n" + 
		"	class Z extends Y {	\n" + 
		"	      ^\n" + 
		"This class must implement the inherited abstract method X.foo(), but cannot override it since it is not visible from Z. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n"
);
}

// 30805 Abstract non-visible method diagnosis fooled by intermediate declarations
public void test035() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //==================================
			"package p;	\n" +
			"public abstract class X {	\n" +
			"	abstract void foo();	\n" +
			"	abstract void bar();	\n" +
			"}	\n",
			"p/Y.java", //==================================
			"package p;	\n" +
			"public abstract class Y extends X {	\n" +
			"	void foo(){};	\n" +
			"}	\n",
			"q/Z.java", //==================================
			"package q;	\n" +
			"class Z extends p.Y {	\n" +
			"}	\n",
		},
		"----------\n" + 
		"1. ERROR in q\\Z.java (at line 2)\n" + 
		"	class Z extends p.Y {	\n" + 
		"	      ^\n" + 
		"This class must implement the inherited abstract method X.bar(), but cannot override it since it is not visible from Z. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n");
}
// 30805 Abstract non-visible method diagnosis fooled by intermediate declarations
public void test036() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //==================================
			"package p;	\n" +
			"public abstract class X {	\n" +
			"	abstract void foo();	\n" +
			"	public interface I {	\n" +
			"		void foo();	\n" +
			"	}	\n" +
			"}	\n",
			"q/Y.java", //==================================
			"package q;	\n" +
			"public abstract class Y extends p.X {	\n" +
			"	void foo(){}	\n" +
			"}	\n" +
			"class Z extends Y implements p.X.I {	\n" +
			"}	\n",
		},
		"----------\n" + 
		"1. WARNING in q\\Y.java (at line 3)\n" + 
		"	void foo(){}	\n" + 
		"	     ^^^^^\n" + 
		"The method Y.foo() does not override the inherited method from X since it is private to a different package.\n" + 
		"----------\n" + 
		"2. ERROR in q\\Y.java (at line 5)\n" + 
		"	class Z extends Y implements p.X.I {	\n" + 
		"	      ^\n" + 
		"This class must implement the inherited abstract method X.foo(), but cannot override it since it is not visible from Z. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n" + // TODO (philippe) should not have following error due to default abstract?
		"3. ERROR in q\\Y.java (at line 5)\n" + 
		"	class Z extends Y implements p.X.I {	\n" + 
		"	      ^\n" + 
		"The inherited method Y.foo() cannot hide the public abstract method in X.I\n" + 
		"----------\n");
}
// 30805 Abstract non-visible method diagnosis fooled by intermediate declarations
public void test037() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //==================================
			"package p;	\n" +
			"public abstract class X {	\n" +
			"	abstract void foo();	\n" +
			"	void bar(){}	\n" +
			"}	\n",
			"q/Y.java", //==================================
			"package q;	\n" +
			"public abstract class Y extends p.X {	\n" +
			"	void foo(){}	//warn \n" +
			"	void bar(){}	//warn \n" +
			"}	\n" +
			"class Z extends Y {	\n" +
			"	void bar(){}	//nowarn \n" +
			"}	\n",
		},
		"----------\n" + 
		"1. WARNING in q\\Y.java (at line 3)\n" + 
		"	void foo(){}	//warn \n" + 
		"	     ^^^^^\n" + 
		"The method Y.foo() does not override the inherited method from X since it is private to a different package.\n" + 
		"----------\n" + 
		"2. WARNING in q\\Y.java (at line 4)\n" + 
		"	void bar(){}	//warn \n" + 
		"	     ^^^^^\n" + 
		"The method Y.bar() does not override the inherited method from X since it is private to a different package.\n" + 
		"----------\n" + 
		"3. ERROR in q\\Y.java (at line 6)\n" + 
		"	class Z extends Y {	\n" + 
		"	      ^\n" + 
		"This class must implement the inherited abstract method X.foo(), but cannot override it since it is not visible from Z. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n");
}
// 30805 Abstract non-visible method diagnosis fooled by intermediate declarations
public void test038() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //==================================
			"package p;	\n" +
			"public abstract class X {	\n" +
			"	abstract void foo();	\n" +
			"}	\n",
			"q/Y.java", //==================================
			"package q;	\n" +
			"public abstract class Y extends p.X {	\n" +
			"	void foo(){}	//warn \n" +
			"}	\n" +
			"class Z extends Y {	\n" +
			"	void foo(){}	//error \n" +
			"}	\n",
		},
		"----------\n" + 
		"1. WARNING in q\\Y.java (at line 3)\n" + 
		"	void foo(){}	//warn \n" + 
		"	     ^^^^^\n" + 
		"The method Y.foo() does not override the inherited method from X since it is private to a different package.\n" + 
		"----------\n" + 
		"2. ERROR in q\\Y.java (at line 5)\n" + 
		"	class Z extends Y {	\n" + 
		"	      ^\n" + 
		"This class must implement the inherited abstract method X.foo(), but cannot override it since it is not visible from Z. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n");
}

// 31198 - regression after 30805 - Abstract non-visible method diagnosis fooled by intermediate declarations
public void test039() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //==================================
			"package p;	\n" +
			"public abstract class X {	\n" +
			"	abstract void foo();	\n" + // should not complain about this one in Z, since it has a visible implementation
			"	abstract void bar();	\n" +
			"}	\n",
			"p/Y.java", //==================================
			"package p;	\n" +
			"public abstract class Y extends X {	\n" +
			"	public void foo(){};	\n" +
			"}	\n",
			"q/Z.java", //==================================
			"package q;	\n" +
			"class Z extends p.Y {	\n" +
			"}	\n",
		},
		"----------\n" + 
		"1. ERROR in q\\Z.java (at line 2)\n" + 
		"	class Z extends p.Y {	\n" + 
		"	      ^\n" + 
		"This class must implement the inherited abstract method X.bar(), but cannot override it since it is not visible from Z. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n");
}

/*
 * 31398 - non-visible abstract method fooling method verification - should not complain about foo() or bar()
 */
public void test040() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //================================
			"package p;	\n" +
			"public class X extends q.Y.Member {	\n" +
			"		void baz(){}	\n" + // doesn't hide Y.baz()
			"}	\n",
			"q/Y.java", //================================
			"package q;	\n" +
			"public abstract class Y {	\n" +
			"	abstract void foo();	\n" +
			"	abstract void bar();	\n" +
			"	abstract void baz();	\n" +
			"	public static abstract class Member extends Y {	\n" +
			"		public void foo() {}	\n" + 
			"		void bar(){}	\n" +
			"	}	\n" +
			"}	\n",
		},
		"----------\n" + 
		"1. ERROR in p\\X.java (at line 2)\n" + 
		"	public class X extends q.Y.Member {	\n" + 
		"	             ^\n" + 
		"This class must implement the inherited abstract method Y.baz(), but cannot override it since it is not visible from X. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n" + 
		"2. WARNING in p\\X.java (at line 3)\n" + 
		"	void baz(){}	\n" + 
		"	     ^^^^^\n" + 
		"The method X.baz() does not override the inherited method from Y since it is private to a different package.\n" + 
		"----------\n");
}

/*
 * 31450 - non-visible abstract method fooling method verification - should not complain about foo() 
 */
public void test041() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //================================
			"package p;	\n" +
			"public class X extends q.Y.Member {	\n" +
			"	public void foo() {}	\n" +
			"	public static class M extends X {}	\n" +
			"}	\n",
			"q/Y.java", //================================
			"package q;	\n" +
			"public abstract class Y {	\n" +
			"	abstract void foo();	\n" +
			"	abstract void bar();	\n" +
			"	public static abstract class Member extends Y {	\n" +
			"		protected abstract void foo();	\n" + // takes precedence over inherited abstract Y.foo()
			"	}	\n" +
			"}	\n",
		},
		"----------\n" + 
		"1. ERROR in p\\X.java (at line 2)\n" + 
		"	public class X extends q.Y.Member {	\n" + 
		"	             ^\n" + 
		"This class must implement the inherited abstract method Y.bar(), but cannot override it since it is not visible from X. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n" + 
		"2. ERROR in p\\X.java (at line 4)\n" + 
		"	public static class M extends X {}	\n" + 
		"	                    ^\n" + 
		"This class must implement the inherited abstract method Y.bar(), but cannot override it since it is not visible from M. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n");
}

/*
 * 31450 - non-visible abstract method fooling method verification - should not complain about foo() 
 */
public void test042() {
	this.runNegativeTest(
		new String[] {
			"p/X.java", //================================
			"package p;	\n" +
			"public class X extends q.Y.Member {	\n" +
			"	public void foo() {}	\n" +
			"	public static class M extends X {}	\n" +
			"}	\n",
			"q/Y.java", //================================
			"package q;	\n" +
			"public abstract class Y {	\n" +
			"	abstract void foo();	\n" +
			"	abstract void bar();	\n" +
			"	public static abstract class Member extends Y {	\n" +
			"		void foo(){}	\n" + 
			"	}	\n" +
			"}	\n",
		},
		"----------\n" + 
		"1. ERROR in p\\X.java (at line 2)\n" + 
		"	public class X extends q.Y.Member {	\n" + 
		"	             ^\n" + 
		"This class must implement the inherited abstract method Y.bar(), but cannot override it since it is not visible from X. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n" + 
		"2. WARNING in p\\X.java (at line 3)\n" + 
		"	public void foo() {}	\n" + 
		"	            ^^^^^\n" + 
		"The method X.foo() does not override the inherited method from Y.Member since it is private to a different package.\n" + 
		"----------\n" + 
		"3. ERROR in p\\X.java (at line 4)\n" + 
		"	public static class M extends X {}	\n" + 
		"	                    ^\n" + 
		"This class must implement the inherited abstract method Y.bar(), but cannot override it since it is not visible from M. Either make the type abstract or make the inherited method visible.\n" + 
		"----------\n");
}

public void test043() {
	this.runConformTest(
		new String[] {
			"X.java", //================================
			"public class X {\n" + 
			"	public interface Copyable extends Cloneable {\n" + 
			"		public Object clone() throws CloneNotSupportedException;\n" + 
			"	}\n" + 
			"\n" + 
			"	public interface TestIf extends Copyable {\n" + 
			"	}\n" + 
			"\n" + 
			"	public static class ClassA implements Copyable {\n" + 
			"		public Object clone() throws CloneNotSupportedException {\n" + 
			"			return super.clone();\n" + 
			"		}\n" + 
			"	}\n" + 
			"\n" + 
			"	public static class ClassB implements TestIf {\n" + 
			"		public Object clone() throws CloneNotSupportedException {\n" + 
			"			return super.clone();\n" + 
			"		}\n" + 
			"	}\n" + 
			"\n" + 
			"	public static void main(String[] args) throws Exception {\n" + 
			"		Copyable o1 = new ClassA();\n" + 
			"		ClassB o2 = new ClassB();\n" + 
			"		TestIf o3 = o2;\n" + 
			"		Object clonedObject;\n" + 
			"		clonedObject = o1.clone();\n" + 
			"		clonedObject = o2.clone();\n" + 
			"		clonedObject = o3.clone();\n" + 
			"		System.out.println(\"SUCCESS\");\n" + 
			"	}\n" + 
			"}"
		},
		"SUCCESS");
}
/*
 * 62639 - check that missing member type is not noticed if no direct connection with compiled type
 */
public void test044() {
	this.runConformTest(
		new String[] {
			"p/Dumbo.java",
			"package p;\n" +
			"public class Dumbo {\n" +
			"  public class Clyde { }\n" +
			"	public static void main(String[] args) {\n" + 
			"		  System.out.println(\"SUCCESS\");\n" + 
			"	}\n" + 
			"}\n",
		},
		"SUCCESS");
	// delete binary file Dumbo$Clyde (i.e. simulate removing it from classpath for subsequent compile)
	new File(OUTPUT_DIR, "p" + File.separator + "Dumbo$Clyde.class").delete();
	
	this.runConformTest(
		new String[] {
			"q/Main.java",
			"package q;\n" +
			"public class Main extends p.Dumbo {\n" +
			"	public static void main(String[] args) {\n" +
			"		  p.Dumbo d;\n" +
			"		  System.out.println(\"SUCCESS\");\n" + 
			"	}\n" +
			"}\n",
		},
		"SUCCESS",
		null,
		false,
		null);
}
/*
 * ensure that can still found binary member types at depth >=2 (enclosing name Dumbo$Clyde $ Fred)
 */
public void test045() {
	this.runConformTest(
		new String[] {
			"p/Dumbo.java",
			"package p;\n" +
			"public class Dumbo {\n" +
			"  public class Clyde {\n" +
			"  	  public class Fred {\n" +
			"	  }\n" + 
			"	}\n" + 
			"	public static void main(String[] args) {\n" + 
			"		  System.out.println(\"SUCCESS\");\n" + 
			"	}\n" + 
			"}\n",
		},
		"SUCCESS");
	
	this.runConformTest(
		new String[] {
			"q/Main.java",
			"package q;\n" +
			"public class Main extends p.Dumbo {\n" +
			"	public static void main(String[] args) {\n" +
			"		  p.Dumbo.Clyde.Fred f;\n" +
			"		  System.out.println(\"SUCCESS\");\n" + 
			"	}\n" +
			"}\n",
		},
		"SUCCESS",
		null,
		false,
		null);
}
public void test046() {
	this.runNegativeTest(
		new String[] {
			"X.java", //================================
			"public class X {\n" + 
			"     private XY foo(XY t) {\n" + 
			"        System.out.println(t);\n" + 
			"        return t;\n" + 
			"    }\n" + 
			"    public static void main(String[] args) {\n" + 
			"        new X() {\n" + 
			"            void run() {\n" + 
			"                foo(new XY());\n" + 
			"            }\n" + 
			"        }.run();\n" + 
			"    }\n" + 
			"}\n" + 
			"class XY {\n" + 
			"    public String toString() {\n" + 
			"        return \"SUCCESS\";\n" + 
			"    }\n" + 
			"}\n"
		}, 
			"----------\n" + 
			"1. ERROR in X.java (at line 9)\n" + 
			"	foo(new XY());\n" + 
			"	^^^\n" + 
			"Cannot make a static reference to the non-static method foo(XY) from the type X\n" + 
			"----------\n");
}
public void test047() {
	this.runConformTest(
		new String[] {
			"X.java", //================================
			"public class X extends SuperTest\n" + 
			"{\n" + 
			"    public X()\n" + 
			"    {\n" + 
			"        super();\n" + 
			"    }\n" + 
			"  \n" + 
			"    static void print(Object obj)\n" + 
			"    {\n" + 
			"        System.out.println(\"Object:\" + obj.toString());\n" + 
			"    }\n" + 
			"    \n" + 
			"    public static void main(String[] args)\n" + 
			"    {\n" + 
			"        print(\"Hello world\");\n" + 
			"    }\n" + 
			"}\n" + 
			"class SuperTest\n" + 
			"{\n" + 
			"    SuperTest(){};\n" + 
			"    static void print(String s)\n" + 
			"    {\n" + 
			"        System.out.println(\"String: \" + s);\n" + 
			"    }\n" + 
			"}\n"	},
		"String: Hello world");
}
// 73740 - missing serialVersionUID diagnosis shouldn't trigger load of Serializable
public void test048() {
	this.runConformTest(
		new String[] {
			"X.java", //---------------------------
			"public class X {\n" + 
			"   public static void main(String[] args) {\n"+
			"		System.out.println(\"SUCCESS\");\n"+
			"   }\n"+
			"}\n",
		},
		"SUCCESS",
		Util.concatWithClassLibs(OUTPUT_DIR, true/*output in front*/),
		false, // do not flush output
		null,  // vm args
		null, // options
		new ICompilerRequestor() {
			public void acceptResult(CompilationResult result) {
				assertNotNull("missing reference information",result.simpleNameReferences);
				char[] serializable = TypeConstants.JAVA_IO_SERIALIZABLE[2];
				for (int i = 0, length = result.simpleNameReferences.length; i < length; i++) {
					char[] name = result.simpleNameReferences[i];
					if (CharOperation.equals(name, serializable))
						assertTrue("should not contain reference to Serializable", false);
				}
			}
		});		
}
// 76682 - ClassCastException in qualified name computeConversion
public void test049() {
	this.runNegativeTest(
		new String[] {
			"X.java", //---------------------------
			"public class X\n" + 
			"{\n" + 
			"    private String foo() {\n" + 
			"        return \"Started \" + java.text.DateFormat.format(new java.util.Date());\n" + 
			"    }\n" + 
			"}\n" ,
		},
		"----------\n" + 
		"1. ERROR in X.java (at line 4)\r\n" + 
		"	return \"Started \" + java.text.DateFormat.format(new java.util.Date());\r\n" + 
		"	                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n" + 
		"Cannot make a static reference to the non-static method format(Date) from the type DateFormat\n" + 
		"----------\n");
}
public void test050() {
	this.runConformTest(
		new String[] {
			"X.java", //---------------------------
			"public class X {\n" + 
			"\n" + 
			"    public static void main(String argv[]) {\n" + 
			"    	X.Y.Z.foo();\n" + 
			"    }\n" + 
			"    static class Y {\n" + 
			"    	static class Z {\n" + 
			"    		static void foo() {\n" + 
			"    			System.out.println(\"SUCCESS\");\n" + 
			"    		}\n" + 
			"    	}\n" + 
			"    }\n" + 
			"}\n",
		},
		"SUCCESS");
}

public void test051() {
	this.runNegativeTest(
		new String[] {
			"X.java", //---------------------------
			"public class X {\n" + 
			"\n" + 
			"    public static void main(String[] args) {\n" + 
			"        args.finalize();\n" + 
			"    }\n" + 
			"}\n",
		},
		"----------\n" + 
		"1. ERROR in X.java (at line 4)\n" + 
		"	args.finalize();\n" + 
		"	     ^^^^^^^^\n" + 
		"The method finalize() from the type Object is not visible\n" + 
		"----------\n");
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=87463
public void test052() {
	this.runConformTest(
		new String[] {
			"X.java", //---------------------------
			"public class X {\n" + 
			"	public void test() {\n" + 
			"		class C {\n" + 
			"			public C() {\n" + 
			"			}\n" + 
			"			public void foo() {\n" + 
			"				System.out.println(\"hello\");\n" + 
			"			}\n" + 
			"		}\n" + 
			"		int n = 0;\n" + 
			"		switch (n) {\n" + 
			"			case 0 :\n" + 
			"				if (true) {\n" + 
			"					C c2 = new C();\n" + 
			"				}\n" + 
			"		}\n" + 
			"	}\n" + 
			"}\n",
		},
		"");
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=87463 - variation
public void test053() {
	this.runConformTest(
		new String[] {
			"X.java", //---------------------------
			"public class X {\n" + 
			"	public void test() {\n" + 
			"		int l = 1;\n" + 
			"		switch(l) {\n" + 
			"			case 1: \n" + 
			"				class C {\n" + 
			"					public C() {\n" + 
			"					}\n" + 
			"					public void foo() {\n" + 
			"						System.out.println(\"hello\");\n" + 
			"					}\n" + 
			"				}\n" + 
			"				int n = 0;\n" + 
			"				switch (n) {\n" + 
			"					case 0 :\n" + 
			"						if (true) {\n" + 
			"							C c2 = new C();\n" + 
			"						}\n" + 
			"				}\n" + 
			"		}\n" + 
			"	}\n" + 
			"}\n",
		},
		"");
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=93486
public void test054() {
    this.runConformTest(
        new String[] {
            "X.java", //---------------------------
            "import java.util.LinkedHashMap;\n" + 
            "import java.util.Map.Entry;\n" + 
            "\n" + 
            "public class X {\n" + 
            "    \n" + 
            "    private LinkedHashMap fCache;\n" + 
            "    \n" + 
            "    public X(final int cacheSize) {\n" + 
            "        // start with 100 elements but be able to grow until cacheSize\n" + 
            "        fCache= new LinkedHashMap(100, 0.75f, true) {\n" + 
            "            /** This class is not intended to be serialized. */\n" + 
            "            private static final long serialVersionUID= 1L;\n" + 
            "            protected boolean removeEldestEntry(Entry eldest) {\n" + 
            "                return size() > cacheSize;\n" + 
            "            }\n" + 
            "        };\n" + 
            "    }\n" + 
            "}\n",
        },
        "");
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=106140
public void test055() {
    this.runNegativeTest(
        new String[] {
            "A.java",
            "import p.*;\n" + 
            "public class A {\n" + 
            "    public void errors() {\n" + 
	            "    B b = new B();\n" + 
            "        String s1 = b.str;\n" + 
            "        String s2 = B.str;\n" + 
            "    }\n" + 
            "}\n",
            "p/B.java",
            "package p;\n" + 
            "class B {\n" + 
            "    public static String str;\n" + 
            "}\n",
        },
		"----------\n" + 
		"1. ERROR in A.java (at line 4)\r\n" + 
		"	B b = new B();\r\n" + 
		"	^\n" + 
		"The type B is not visible\n" + 
		"----------\n" + 
		"2. ERROR in A.java (at line 4)\r\n" + 
		"	B b = new B();\r\n" + 
		"	          ^\n" + 
		"The type B is not visible\n" + 
		"----------\n" + 
		"3. ERROR in A.java (at line 6)\r\n" + 
		"	String s2 = B.str;\r\n" + 
		"	            ^\n" + 
		"The type B is not visible\n" + 
		"----------\n"
	);
}
// final method in static inner class still found in extending classes
public void test056() {
    this.runConformTest(
        new String[] {
            "X.java",
			"public class X {\n" + 
			"  public static void main(String[] args) {\n" + 
			"    I x = new Z();\n" + 
			"    x.foo();\n" + 
			"  }\n" + 
			"  static interface I {\n" + 
			"    Y foo();\n" + 
			"  }\n" + 
			"  static class Y {\n" + 
			"    public final Y foo() { \n" + 
			"        System.out.println(\"SUCCESS\");\n" + 
			"        return null; \n" + 
			"    }\n" + 
			"  }\n" + 
			"  static class Z extends Y implements I {\n" + 
			"      // empty\n" + 
			"  }\n" + 
			"}",
        },
        "SUCCESS");
}
// unresolved type does not fool methods signature comparison
public void test057() {
    this.runNegativeTest(
        new String[] {
            "X.java",
			"import java.awt.*;\n" + 
			"public class X {\n" + 
			"    public void foo(Window w) {\n" + 
			"        // empty\n" + 
			"    }\n" + 
			"    public void foo(Applet a) {\n" + 
			"        // empty\n" + 
			"    }\n" + 
			"}"},
		"----------\n" + 
		"1. ERROR in X.java (at line 6)\n" + 
		"	public void foo(Applet a) {\n" + 
		"	                ^^^^^^\n" + 
		"Applet cannot be resolved to a type\n" + 
		"----------\n"
		);
}
public void test058() {
    this.runConformTest(
        new String[] {
        		"p/X.java", // =================
        		"package p;\n" + 
        		"\n" + 
        		"import p.q.Z;\n" + 
        		"public class X { \n" + 
        		"  public static void main(String argv[]) {\n" + 
        		"     System.out.println(Z.z);\n" + 
        		"  }\n" + 
        		"}", // =================
        		"p/q/Z.java", // =================
        		"package p.q;\n" + 
        		"\n" + 
        		"public class Z extends Y implements I { \n" + 
        		"}\n" + 
        		"class Y {\n" + 
        		"    protected static int z = 1;\n" + 
        		"}\n" + 
        		"interface I {\n" + 
        		"    int z = 0;\n" + 
        		"}", // =================
		},
		"0");
}
public static Class testClass() {
	return LookupTest.class;
}
}
