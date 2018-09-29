/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.devtools.j2objc.gen;

import com.google.devtools.j2objc.GenerationTest;
import com.google.devtools.j2objc.Options;

import java.io.IOException;

/**
 * Tests for {@link ObjectiveCHeaderGenerator}.
 *
 * @author Tom Ball
 */
public class ObjectiveCHeaderGeneratorTest extends GenerationTest {

  @Override
  protected void tearDown() throws Exception {
    Options.resetDeprecatedDeclarations();
    super.tearDown();
  }

  public void testInnerEnumWithPackage() throws IOException {
    String translation = translateSourceFile(
        "package mypackage;" +
        "public class Example { MyClass myclass = new MyClass(); }" +
        "enum Abcd { A, B, C; }" +
        "class MyClass {}", "Example", "mypackage/Example.h");
    assertTranslation(translation, "@interface MypackageExample");
    assertTranslation(translation, "} MypackageAbcd;"); // enum declaration
    assertTranslation(translation, "@interface MypackageAbcdEnum");
    assertTranslation(translation, "@interface MypackageMyClass");
    assertTranslation(translation, "MypackageMyClass *myclass_;");
  }

  public void testTypeNameTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class Example {}", "Example", "Example.h");
    assertTranslation(translation, "@interface Example ");
  }

  public void testDeprecatedTypeNameTranslation() throws IOException {
    Options.enableDeprecatedDeclarations();
    String translation = translateSourceFile(
        "public @Deprecated class Example {}", "Example", "Example.h");
    assertTranslation(translation, "__attribute__((deprecated))\n@interface Example ");
  }

  public void testDeprecatedTypeNameTranslationIsTurnedOff() throws IOException {
    String translation = translateSourceFile(
        "public @Deprecated class Example {}", "Example", "Example.h");
    assertFalse(translation.contains("__attribute__((deprecated))"));
  }

  public void testFullyQualifiedDeprecatedTypeNameTranslation() throws IOException {
    Options.enableDeprecatedDeclarations();
    String translation = translateSourceFile(
        "public @java.lang.Deprecated class Example {}", "Example", "Example.h");
    assertTranslation(translation, "__attribute__((deprecated))\n@interface Example ");
  }

  public void testPackageTypeNameTranslation() throws IOException {
    String translation = translateSourceFile(
        "package unit.test; public class Example {}", "Example", "unit/test/Example.h");
    assertTranslation(translation, "@interface UnitTestExample ");
  }

  public void testPackageTypeNameTranslationWithInnerClass() throws IOException {
    String translation = translateSourceFile(
        "package unit.test; public class Example { class Inner {}}",
        "Example", "unit/test/Example.h");
    assertTranslation(translation, "@interface UnitTestExample ");
    assertTranslation(translation, "Example_Inner");
    assertTranslation(translation, "@interface UnitTestExample_Inner ");
  }

  public void testSuperclassTypeTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class MyException extends Exception {}", "MyException", "MyException.h");
    assertTranslation(translation, "@interface MyException : JavaLangException");
  }

  public void testImplementsTypeTranslation() throws IOException {
    String translation = translateSourceFile(
        "import java.io.Serializable; public class Example implements Serializable {}",
        "Example", "Example.h");
    assertTranslation(translation, "@interface Example : NSObject < JavaIoSerializable >");
  }

  public void testImportTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class MyException extends Exception { MyException(Throwable t) {super(t);}}",
        "MyException", "MyException.h");
    assertTranslation(translation, "@class JavaLangThrowable;");
    assertTranslation(translation, "#include \"java/lang/Exception.h\"");
  }

  public void testForwardDeclarationTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class MyException extends Exception { MyException(Throwable t) {super(t);}}",
        "MyException", "MyException.h");
    assertTranslation(translation, "@class JavaLangThrowable;");
  }

  public void testInstanceVariableTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class Example { Exception testException; }",
        "Example", "Example.h");
    assertTranslation(translation, "JavaLangException *testException_;");
  }

  public void testInterfaceTranslation() throws IOException {
    String translation = translateSourceFile(
        "package unit.test; public interface Example {}",
        "Example", "unit/test/Example.h");
    assertTranslation(translation, "@protocol UnitTestExample");
  }

  public void testDeprecatedInterfaceTranslation() throws IOException {
    Options.enableDeprecatedDeclarations();
    String translation = translateSourceFile(
      "package unit.test; public @Deprecated interface Example {}",
      "Example", "unit/test/Example.h");
    assertTranslation(translation, "__attribute__((deprecated))\n@protocol UnitTestExample");
  }

  public void testInterfaceWithMethodTranslation() throws IOException {
    String translation = translateSourceFile(
        "package unit.test; public interface Example { Example getExample(); }",
        "Example", "unit/test/Example.h");
    assertTranslation(translation, "(id<UnitTestExample>)getExample;");
  }

  public void testInterfaceWithDeprecatedMethodTranslation() throws IOException {
    Options.enableDeprecatedDeclarations();
    String translation = translateSourceFile(
        "package unit.test; public interface Example { @Deprecated Example getExample(); }",
        "Example", "unit/test/Example.h");
    assertTranslation(translation,
        "- (id<UnitTestExample>)getExample __attribute__((deprecated));");
  }

  public void testSuperInterfaceTranslation() throws IOException {
    String translation = translateSourceFile(
        "package unit.test; public interface Example extends Bar {} interface Bar {}",
        "Example", "unit/test/Example.h");
    assertTranslation(translation,
        "@protocol UnitTestExample < UnitTestBar, NSObject, JavaObject >");
  }

  public void testConstTranslation() throws IOException {
    String translation = translateSourceFile(
        "package unit.test; public class Example { public static final int FOO=1; }",
        "Example", "unit/test/Example.h");
    assertTranslation(translation, "#define UnitTestExample_FOO 1");
    assertFalse(translation.contains("initialize"));
  }

  public void testStaticVariableTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class Example { public static java.util.Date today; }",
        "Example", "Example.h");
    assertTranslation(translation, "+ (JavaUtilDate *)today;");
    assertTranslation(translation, "+ (void)setToday:(JavaUtilDate *)today;");
    assertFalse(translation.contains("initialize"));
    assertFalse(translation.contains("dealloc"));
  }

  public void testStaticVariableWithInitTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class Example { public static java.util.Date today = new java.util.Date(); }",
        "Example", "Example.h");
    assertTranslation(translation, "+ (JavaUtilDate *)today;");
    assertTranslation(translation, "+ (void)setToday:(JavaUtilDate *)today;");
    assertFalse(translation.contains("+ (void)initialize;"));
    assertFalse(translation.contains("dealloc"));
  }

  public void testInitMessageTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class Example { void init() {} }", "Example", "Example.h");
    assertTranslation(translation, "- (void)init__ OBJC_METHOD_FAMILY_NONE;");
  }

  public void testInitializeMessageTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class Example { void initialize() {} }", "Example", "Example.h");
    assertTranslation(translation, "- (void)initialize__ OBJC_METHOD_FAMILY_NONE;");
  }

  public void testToStringRenaming() throws IOException {
    String translation = translateSourceFile(
      "public class Example { public String toString() { return super.toString(); } }",
      "Example", "Example.h");
    assertTranslation(translation, "- (NSString *)description;");
  }

  public void testMultipleObjectDeclaration() throws IOException {
    String translation = translateSourceFile(
      "public class Example { String one, two, three; }",
      "Example", "Example.h");
    assertTranslation(translation, "NSString *one_, *two_, *three_;");
  }

  public void testMultiplePrimitiveDeclaration() throws IOException {
    String translation = translateSourceFile(
      "public class Example { int one, two, three; }",
      "Example", "Example.h");
    assertTranslation(translation, "int one_, two_, three_;");
  }

  public void testMultipleInterfaceDeclaration() throws IOException {
    String translation = translateSourceFile(
      "public class Example { Comparable one, two, three; }",
      "Example", "Example.h");
    assertTranslation(translation, "id<JavaLangComparable> one_, two_, three_;");
  }

  public void testMultipleClassDeclaration() throws IOException {
    String translation = translateSourceFile(
      "public class Example { Class<?> one, two, three; }",
      "Example", "Example.h");
    assertTranslation(translation, "IOSClass *one_, *two_, *three_;");
  }

  public void testInnerClassDeclaration() throws IOException {
    String translation = translateSourceFile(
      "public class Example { class Inner {} }",
      "Example", "Example.h");
    assertTranslation(translation, "@interface Example_Inner : NSObject");
    assertNotInTranslation(translation, "Example *this");
    assertTranslation(translation, "- (id)initWithExample:(Example *)outer$;");
  }

  public void testInnerClassDeclarationWithOuterReference() throws IOException {
    String translation = translateSourceFile(
      "public class Example { int i; class Inner { void test() { int j = i; } } }",
      "Example", "Example.h");
    assertTranslation(translation, "@interface Example_Inner : NSObject");
    assertTranslation(translation, "Example *this$0_;");
    assertTranslation(translation, "- (id)initWithExample:(Example *)outer$;");
  }

  public void testAnonymousClassDeclaration() throws IOException {
    String translation = translateSourceFile(
      "public class Example { Runnable run = new Runnable() { public void run() {} }; }",
      "Example", "Example.h");
    assertTranslation(translation, "@interface Example_$1 : NSObject < JavaLangRunnable >");
    assertTranslation(translation, "- (void)run;");
    // Outer reference is not required.
    assertNotInTranslation(translation, "Example *this");
    assertNotInTranslation(translation, "- (id)initWithExample:");
  }

  public void testEnum() throws IOException {
    String translation = translateSourceFile(
      "public enum Color { RED, WHITE, BLUE }",
      "Color", "Color.h");
    assertTranslation(translation, "typedef enum {");
    assertTranslation(translation, "Color_RED = 0,");
    assertTranslation(translation, "Color_WHITE = 1,");
    assertTranslation(translation, "Color_BLUE = 2,");
    assertTranslation(translation, "} Color;");
    assertTranslation(translation, "@interface ColorEnum : JavaLangEnum < NSCopying > {");
    assertTranslation(translation, "+ (ColorEnum *)RED;");
    assertTranslation(translation, "+ (ColorEnum *)WHITE;");
    assertTranslation(translation, "+ (ColorEnum *)BLUE;");
    assertTranslation(translation, "+ (IOSObjectArray *)values;");
    assertTranslation(translation, "+ (ColorEnum *)valueOfWithNSString:(NSString *)name;");
  }

  public void testEnumWithParameters() throws IOException {
    String translation = translateSourceFile(
        "public enum Color { RED(0xff0000), WHITE(0xffffff), BLUE(0x0000ff); " +
        "private int rgb; private Color(int rgb) { this.rgb = rgb; } " +
        "public int getRgb() { return rgb; }}",
        "Color", "Color.h");
    assertTranslation(translation, "@interface ColorEnum : JavaLangEnum");
    assertTranslation(translation, "int rgb_;");
    assertTranslatedLines(translation,
        "- (id)initWithInt:(int)rgb",
        "withNSString:(NSString *)__name",
        "withInt:(int)__ordinal;");
  }

  public void testEnumWithMultipleConstructors() throws IOException {
    String translation = translateSourceFile(
      "public enum Color { RED(0xff0000), WHITE(0xffffff, false), BLUE(0x0000ff); " +
      "private int rgb; private boolean primary;" +
      "private Color(int rgb, boolean primary) { this.rgb = rgb; this.primary = primary; } " +
      "private Color(int rgb) { this(rgb, true); } " +
      "public int getRgb() { return rgb; }" +
      "public boolean isPrimaryColor() { return primary; }}",
      "Color", "Color.h");
    assertTranslation(translation, "@interface ColorEnum : JavaLangEnum");
    assertTranslation(translation, "BOOL primary_;");
    assertTranslatedLines(translation,
        "- (id)initWithInt:(int)rgb",
        "withNSString:(NSString *)__name",
        "withInt:(int)__ordinal;");
    assertTranslatedLines(translation,
        "- (id)initWithInt:(int)rgb",
        "withBoolean:(BOOL)primary",
        "withNSString:(NSString *)__name",
        "withInt:(int)__ordinal;");
    translation = getTranslatedFile("Color.m");
    assertTranslation(translation,
        "[self initColorEnumWithInt:rgb withBoolean:YES withNSString:__name withInt:__ordinal]");
    assertTranslatedLines(translation,
        "if ((self = [super initWithNSString:__name withInt:__ordinal])) {",
        "self->rgb_ = rgb;",
        "self->primary_ = primary;");
  }

  public void testArrayFieldDeclaration() throws IOException {
    String translation = translateSourceFile(
      "public class Example { char[] before; char after[]; }",
      "Example", "Example.h");
    assertTranslation(translation, "IOSCharArray *before_;");
    assertTranslation(translation, "IOSCharArray *after_;");
  }

  public void testForwardDeclarationOfInnerType() throws IOException {
    String translation = translateSourceFile(
        "public class Example { Foo foo; class Foo {} }", "Example", "Example.h");
    // Test that Foo is forward declared because Example contains a field of
    // type Foo and Foo is declared after Example.
    assertTranslation(translation, "@class Example_Foo;");
  }

  public void testAnnotationGeneration() throws IOException {
    String translation = translateSourceFile(
      "package foo; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) " +
      "public @interface Compatible { boolean fooable() default false; }",
      "Compatible", "foo/Compatible.h");

    // Test that the annotation was declared as a protocol and a value class.
    assertTranslation(translation, "@protocol FooCompatible < JavaLangAnnotationAnnotation >");
    assertTranslation(translation, "@interface FooCompatible : NSObject < FooCompatible >");

    // Verify that the value is defined as a property instead of a method.
    assertTranslation(translation, "@private\n  BOOL fooable;");
    assertTranslation(translation, "@property (readonly) BOOL fooable;");

    // Verify default value accessor is generated for property.
    assertTranslation(translation, "+ (BOOL)fooableDefault;");

    // Check that constructor was created with the property as parameter.
    assertTranslation(translation, "- (id)initWithFooable:(BOOL)fooable_;");
  }

  public void testCharacterEdgeValues() throws IOException {
    String translation = translateSourceFile(
      "public class Test { " +
      "  public static final char MIN = 0; " +
      "  public static final char MAX = '\uffff'; " +
      "}", "Test", "Test.h");
    assertTranslation(translation, "x00");
    assertTranslation(translation, "0xffff");
  }

  public void testOverriddenFieldTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class Example { int size; } " +
        "class Subclass extends Example { int size; }" +
        "class Subsubclass extends Subclass { int size; }",
        "Example", "Example.h");
    assertTranslation(translation, "int size_;");
    assertTranslation(translation, "int size_Subclass_;");
    assertTranslation(translation, "int size_Subsubclass_;");
  }

  public void testOverriddenNameTranslation() throws IOException {
    String translation = translateSourceFile(
        "public class Example { int size; int size() { return size; }} " +
        "class Subclass extends Example { int size; int size() { return size; }}",
        "Example", "Example.h");
    assertTranslation(translation, "int size__;");
    assertTranslation(translation, "int size_Subclass_;");
  }

  public void testEnumNaming() throws IOException {
    String translation = translateSourceFile(
        "public enum MyEnum { ONE, TWO, THREE }",
        "MyEnum", "MyEnum.h");
    assertTranslation(translation, "} MyEnum;");
    assertTranslation(translation, "@interface MyEnumEnum : JavaLangEnum");
    assertTranslation(translation, "+ (MyEnumEnum *)ONE;");
  }

  public void testNoImportForMappedTypes() throws IOException {
    String translation = translateSourceFile(
        "public class Test extends Object implements Cloneable { " +
        "  public String toString() { return \"\"; }" +
        "  public Class<?> myClass() { return getClass(); }}",
        "Test", "Test.h");
    assertFalse(translation.contains("#include \"java/lang/Class.h\""));
    assertFalse(translation.contains("#include \"java/lang/Cloneable.h\""));
    assertFalse(translation.contains("#include \"java/lang/Object.h\""));
    assertFalse(translation.contains("#include \"java/lang/String.h\""));
    assertFalse(translation.contains("#include \"Class.h\""));
    assertFalse(translation.contains("#include \"NSCopying.h\""));
    assertFalse(translation.contains("#include \"NSObject.h\""));
    assertFalse(translation.contains("#include \"NSString.h\""));
    assertTranslation(translation, "NSCopying");
  }

  public void testAnonymousConcreteSubclassOfGenericAbstractType() throws IOException {
    String translation = translateSourceFile(
        "public class Test {" +
        "  interface FooInterface<T> { public void foo1(T t); public void foo2(); }" +
        "  abstract static class Foo<T> implements FooInterface<T> { public void foo2() { } }" +
        "  Foo<Integer> foo = new Foo<Integer>() {" +
        "    public void foo1(Integer i) { } }; }",
        "Test", "Test.h");
    assertTranslation(translation, "foo1WithId:(JavaLangInteger *)i");
  }

  // Verify that an empty Java enum doesn't define an empty C enum,
  // which is illegal.
  public void testEmptyEnum() throws IOException {
    String header = translateSourceFile("public class A { enum Foo {} }", "A", "A.h");
    String impl = getTranslatedFile("A.m");

    // Verify there's no C enum.
    assertFalse(header.contains("typedef enum {\n} A_Foo;"));

    // Verify there's still a Java enum type.
    assertTranslation(header, "@interface A_FooEnum : JavaLangEnum");
    assertTranslation(impl, "@implementation A_FooEnum");
  }

  public void testEnumWithInterfaces() throws IOException {
    String translation = translateSourceFile(
        "public class A { interface I {} " +
        "enum Foo implements I, Runnable, Cloneable { " +
        "A, B, C; public void run() {}}}", "A", "A.h");
    assertTranslation(translation,
        "@interface A_FooEnum : JavaLangEnum < NSCopying, A_I, JavaLangRunnable >");
    assertTranslation(translation, "#include \"java/lang/Runnable.h\"");
  }

  public void testExternalNativeMethod() throws IOException {
    String translation = translateSourceFile(
        "package foo; class Example { native void external(String s); " +
        "  void test(String str) { external(str); }}", "Example", "foo/Example.h");

    // Verify test() is in main interface.
    assertTranslation(translation,
        "@interface FooExample : NSObject {\n}\n\n- (void)testWithNSString:(NSString *)str;");

    // Verify external() is in native methods interface.
    assertTranslation(translation,
        "@interface FooExample (NativeMethods)\n- (void)externalWithNSString:(NSString *)s;");

    // Verify category method isn't implemented, but is invoked.
    translation = getTranslatedFile("foo/Example.m");
    assertTranslation(translation, "@implementation FooExample\n");
    assertFalse(translation.contains("- (void)externalWithNSString:(NSString *)s"));
    assertTranslation(translation, "[self externalWithNSString:str];");
  }

  public void testPropertiesOfTypeWeakOuter() throws IOException {
    String sourceContent =
        "  import com.google.j2objc.annotations.Weak;"
        + "import com.google.j2objc.annotations.WeakOuter;"
        + "public class FooBar {"
        + "  @Weak private Internal fieldBar;"
        + "  private Internal fieldFoo;"
        + "  @WeakOuter"
        + "  private class Internal {"
        + "  }"
        + "}";
    String translation = translateSourceFile(sourceContent, "FooBar", "FooBar.h");
    assertTranslatedLines(translation,
        "__weak FooBar_Internal *fieldBar_;",
        "FooBar_Internal *fieldFoo_;");
  }

  public void testAddIgnoreDeprecationWarningsPragmaIfDeprecatedDeclarationsIsEnabled()
      throws IOException {
    Options.enableDeprecatedDeclarations();

    String sourceContent = "";
    String translation = translateSourceFile(sourceContent, "FooBar", "FooBar.h");

    assertTranslation(translation, "#pragma clang diagnostic push");
    assertTranslation(translation, "#pragma GCC diagnostic ignored \"-Wdeprecated-declarations\"");
    assertTranslation(translation, "#pragma clang diagnostic pop");
  }

  public void testDoNotAddIgnoreDeprecationWarningsPragmaIfDeprecatedDeclarationsIsDisabled()
      throws IOException {
    String sourceContent = "";
    String translation = translateSourceFile(sourceContent, "FooBar", "FooBar.h");

    assertNotInTranslation(translation, "#pragma clang diagnostic push");
    assertNotInTranslation(translation,
        "#pragma GCC diagnostic ignored \"-Wdeprecated-declarations\"");
    assertNotInTranslation(translation, "#pragma clang diagnostic pop");
  }

  public void testInnerAnnotationGeneration() throws IOException {
    String source = "import java.lang.annotation.*; public abstract class Test { " +
        "@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) " +
        "public @interface Initialize {}}";
    String translation = translateSourceFile(source, "Test", "Test.h");
    assertTranslation(translation, "@protocol Test_Initialize < JavaLangAnnotationAnnotation >");
    assertTranslation(translation, "@interface Test_Initialize : NSObject < Test_Initialize >");
  }

  public void testFieldSetterGeneration() throws IOException {
    String translation = translateSourceFile(
        "import com.google.j2objc.annotations.Weak;" +
        "class Test { Object o; @Weak String s; static Integer i; }", "Test", "Test.h");
    assertTranslation(translation, "J2OBJC_FIELD_SETTER(Test, o_, id)");
    // Make sure the @Weak and static fields don't generate setters.
    assertOccurrences(translation, "J2OBJC_FIELD_SETTER", 1);
  }

  public void testEnumWithNameAndOrdinalParameters() throws IOException {
    String translation = translateSourceFile(
      "public enum Test { FOO(\"foo\", 3), BAR(\"bar\", 5); " +
      "private String name; private int ordinal; " +
      "private Test(String name, int ordinal) { this.name = name; this.ordinal = ordinal; }" +
      "public String getName() { return name; }}",
      "Test", "Test.h");
    assertTranslation(translation, "@interface TestEnum : JavaLangEnum");
    assertTranslation(translation, "NSString *name_Test_;");
    assertTranslation(translation, "int ordinal_Test_;");
    assertTranslatedLines(translation,
        "- (id)initWithNSString:(NSString *)name",
        "withInt:(int)ordinal",
        "withNSString:(NSString *)__name",
        "withInt:(int)__ordinal;");
  }

  public void testDeprecatedEnumType() throws IOException {
    Options.enableDeprecatedDeclarations();
    String translation = translateSourceFile(
        "@Deprecated public enum Test { A, B }", "Test", "Test.h");
    assertTranslation(translation, "__attribute__((deprecated))\n@interface TestEnum");
  }

  public void testLongConstants() throws IOException {
    String translation = translateSourceFile(
        "class Test { static final long FOO = 123; }", "Test", "Test.h");
    assertTranslation(translation, "123LL");
  }
}
