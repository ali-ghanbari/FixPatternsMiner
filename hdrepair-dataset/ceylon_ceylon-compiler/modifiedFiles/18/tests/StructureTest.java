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
package com.redhat.ceylon.compiler.java.test.structure;

import org.junit.Ignore;
import org.junit.Test;

import com.redhat.ceylon.compiler.java.test.CompilerTest;

public class StructureTest extends CompilerTest {
    
    //
    // Packages
    
    @Test
    public void testPkgPackage(){
        compareWithJavaSource("pkg/pkg");
    }

    @Test
    public void testPkgPackageMetadata(){
        compareWithJavaSource("pkg/package");
    }

    //
    // Modules
    
    @Test
    public void testMdlModule(){
        compareWithJavaSource("module/single/module");
    }

    //
    // Attributes
    
    @Test
    public void testAtrClassAttribute(){
        compareWithJavaSource("attribute/ClassAttribute");
    }
    @Test
    public void testAtrClassAttributeWithInitializer(){
        compareWithJavaSource("attribute/ClassAttributeWithInitializer");
    }
    @Test
    public void testAtrClassAttributeGetter(){
        compareWithJavaSource("attribute/ClassAttributeGetter");
    }
    @Test
    public void testAtrClassAttributeGetterSetter(){
        compareWithJavaSource("attribute/ClassAttributeGetterSetter");
    }
    @Test
    public void testAtrClassVariable(){
        compareWithJavaSource("attribute/ClassVariable");
    }
    @Test
    public void testAtrClassVariableWithInitializer(){
        compareWithJavaSource("attribute/ClassVariableWithInitializer");
    }
    @Test
    public void testAtrInnerAttributeGetter(){
        compareWithJavaSource("attribute/InnerAttributeGetter");
    }
    @Test
    public void testAtrInnerAttributeGetterSetter(){
        compareWithJavaSource("attribute/InnerAttributeGetterSetter");
    }
    @Test
    public void testAtrInnerAttributeGetterLateInitialisation(){
        compareWithJavaSource("attribute/InnerAttributeGetterLateInitialisation");
    }
    @Test
    public void testAtrClassAttributeWithConflictingMethods(){
        compareWithJavaSource("attribute/ClassAttributeWithConflictingMethods");
    }
    @Test
    public void testAtrInnerAttributeGetterWithConflictingMethods(){
        compareWithJavaSource("attribute/InnerAttributeGetterWithConflictingMethods");
    }
    
    //
    // Classes
    
    @Test
    public void testKlsAbstractFormal(){
        compareWithJavaSource("klass/AbstractFormal");
    }
    @Test
    public void testKlsCaseTypes(){
        compareWithJavaSource("klass/CaseTypes");
    }
    @Test
    public void testKlsDefaultedInitializerParameter(){
        compareWithJavaSource("klass/DefaultedInitializerParameter");
    }
    @Test
    public void testKlsExtends(){
        compareWithJavaSource("klass/Extends");
    }
    @Test
    public void testKlsExtendsGeneric(){
        compareWithJavaSource("klass/ExtendsGeneric");
    }
    @Test
    public void testKlsInitializerParameter(){
        compareWithJavaSource("klass/InitializerParameter");
    }
    @Test
    public void testKlsInitializerVarargs(){
        compareWithJavaSource("klass/InitializerVarargs");
    }
    @Test
    public void testKlsInnerClass(){
        compareWithJavaSource("klass/InnerClass");
    }
    @Test
    public void testKlsInterface(){
        compareWithJavaSource("klass/Interface");
    }
    @Ignore("M3")
    @Test
    public void testKlsInterfaceWithConcreteMembers(){
        compareWithJavaSource("klass/InterfaceWithConcreteMembers");
    }
    @Test
    public void testKlsInterfaceWithMembers(){
        compareWithJavaSource("klass/InterfaceWithMembers");
    }
    @Test
    public void testKlsClass(){
        compareWithJavaSource("klass/Klass");
    }
    @Test
    public void testKlsKlassMethodTypeParams(){
        compareWithJavaSource("klass/KlassMethodTypeParams");
    }
    @Test
    public void testKlsKlassTypeParams(){
        compareWithJavaSource("klass/KlassTypeParams");
    }
    @Test
    public void testKlsKlassTypeParamsSatisfies(){
        compareWithJavaSource("klass/KlassTypeParamsSatisfies");
    }
    @Test
    public void testKlsKlassWithObjectMember(){
        compareWithJavaSource("klass/KlassWithObjectMember");
    }
    @Test
    public void testKlsLocalClass(){
        compareWithJavaSource("klass/LocalClass");
    }
    @Test
    public void testKlsDoublyLocalClass(){
        compareWithJavaSource("klass/DoublyLocalClass");
    }
    @Test
    public void testKlsLocalClassWithLocalObject(){
        compareWithJavaSource("klass/LocalClassWithLocalObject");
    }
    @Test
    public void testKlsPublicClass(){
        compareWithJavaSource("klass/PublicKlass");
    }
    @Test
    public void testKlsSatisfies(){
        compareWithJavaSource("klass/Satisfies");
    }
    @Test
    public void testKlsSatisfiesErasure(){
        compareWithJavaSource("klass/SatisfiesErasure");
    }
    @Test
    public void testKlsSatisfiesGeneric(){
        compareWithJavaSource("klass/SatisfiesGeneric");
    }
    @Test
    public void testKlsSatisfiesWithMembers(){
        compareWithJavaSource("klass/SatisfiesWithMembers");
    }
    @Test
    public void testKlsRefinedVarianceInheritance_fail(){
        // See https://github.com/ceylon/ceylon-compiler/issues/319
        //compareWithJavaSource("klass/RefinedVarianceInheritance");
        compileAndRun("com.redhat.ceylon.compiler.java.test.structure.klass.rvi_run", "klass/RefinedVarianceInheritance.ceylon");
    }
    @Test
    public void testKlsRefinedVarianceInheritance2(){
        // See https://github.com/ceylon/ceylon-compiler/issues/354
        compareWithJavaSource("klass/RefinedVarianceInheritance2");
    }
    @Test
    public void testKlsVariance(){
        compareWithJavaSource("klass/Variance");
    }
    @Test
    public void testKlsObjectInMethod(){
        compareWithJavaSource("klass/ObjectInMethod");
    }
    @Test
    public void testKlsObjectInStatement(){
        compareWithJavaSource("klass/ObjectInStatement");
    }
    @Test
    public void testKlsInitializerObjectInStatement(){
        compareWithJavaSource("klass/InitializerObjectInStatement");
    }
    @Test
    public void testKlsKlassInStatement(){
        compareWithJavaSource("klass/KlassInStatement");
    }
    @Test
    public void testKlsInitializerKlassInStatement(){
        compareWithJavaSource("klass/InitializerKlassInStatement");
    }
    @Test
    public void testKlsObjectInGetter(){
        compareWithJavaSource("klass/ObjectInGetter");
    }
    @Test
    public void testKlsObjectInSetter(){
        compareWithJavaSource("klass/ObjectInSetter");
    }
    @Test
    public void testKlsClassInGetter(){
        compareWithJavaSource("klass/KlassInGetter");
    }
    @Test
    public void testKlsClassInSetter(){
        compareWithJavaSource("klass/KlassInSetter");
    }
    @Test
    public void testKlsInnerClassUsingOutersTypeParam(){
        compareWithJavaSource("klass/InnerClassUsingOutersTypeParam");
    }
    @Test
    public void testKlsInnerClassUsingOutersTypeParam2(){
        compareWithJavaSource("klass/InnerClassUsingOutersTypeParam2");
    }
    @Test
    public void testKlsUninitializedMethod(){
        compareWithJavaSource("klass/UninitializedMethod");
    }
    
    @Test
    public void testKlsDeferredMethodInitialization(){
        compareWithJavaSource("klass/DeferredMethodInitialization");
    }
    
    @Test
    public void testKlsDeferredMethodInitializationMultipleSpecification(){
        compareWithJavaSource("klass/DeferredMethodInitializationMultipleSpecification");
    }
    
    @Test
    public void testKlsDeferredFunctionInitialization(){
        compareWithJavaSource("klass/DeferredFunctionInitialization");
    }
    @Test
    public void testKlsTypeParamRename(){
        compareWithJavaSource("klass/TypeParamRename");
    }
    
    //
    // Methods
    
    @Test
    public void testMthActualMethodShortcut(){
        compareWithJavaSource("method/ActualMethodShortcut");
    }
    @Test
    public void testMthLocalMethod(){
        compareWithJavaSource("method/LocalMethod");
    }
    @Test
    public void testMthMethod(){
        compareWithJavaSource("method/Method");
    }
    @Test
    public void testMthMethodErasure(){
        compareWithJavaSource("method/MethodErasure");
    }
    @Test
    public void testMthMethodTypeParams(){
        compareWithJavaSource("method/MethodTypeParams");
    }
    @Test
    public void testMthMethodWithDefaultParams(){
        compareWithJavaSource("method/MethodWithDefaultParams");
    }
    @Test
    public void testMthMethodWithLocalObject(){
        compareWithJavaSource("method/MethodWithLocalObject");
    }
    @Test
    public void testMthMethodWithParam(){
        compareWithJavaSource("method/MethodWithParam");
    }
    @Test
    public void testMthMethodWithVarargs(){
        compareWithJavaSource("method/MethodWithVarargs");
    }
    @Test
    public void testMthPublicMethod(){
        compareWithJavaSource("method/PublicMethod");
    }
    @Test
    public void testMthFunctionInStatement(){
        compareWithJavaSource("method/FunctionInStatement");
    }
    @Test
    public void testMthFunctionInGetter(){
        compareWithJavaSource("method/FunctionInGetter");
    }
    @Test
    public void testMthFunctionInSetter(){
        compareWithJavaSource("method/FunctionInSetter");
    }
    @Test
    public void testMthMethodSpecifyingNullaryTopLevel(){
        compareWithJavaSource("method/MethodSpecifyingNullaryTopLevel");
    }
    @Test
    public void testMthMethodSpecifyingUnaryTopLevel(){
        compareWithJavaSource("method/MethodSpecifyingUnaryTopLevel");
    }
    @Test
    public void testMthMethodSpecifyingTopLevelWithResult(){
        compareWithJavaSource("method/MethodSpecifyingTopLevelWithResult");
    }
    @Test
    public void testMthMethodSpecifyingCallable(){
        compareWithJavaSource("method/MethodSpecifyingCallable");
    }
    @Test
    public void testMthMethodSpecifyingInitializer(){
        compareWithJavaSource("method/MethodSpecifyingInitializer");
    }
    
    @Test
    public void testMthMethodSpecifyingTopLevelWithTypeParam(){
        compareWithJavaSource("method/MethodSpecifyingTopLevelWithTypeParam");
    }
    @Test
    public void testMthMethodSpecifyingTopLevelWithTypeParamMixed(){
        compareWithJavaSource("method/MethodSpecifyingTopLevelWithTypeParamMixed");
    }
    @Test
    public void testMthMethodSpecifyingMethod(){
        compareWithJavaSource("method/MethodSpecifyingMethod");
    }
    @Test
    public void testMthMethodSpecifyingGetter(){
        compareWithJavaSource("method/MethodSpecifyingGetter");
    }
    @Test
    public void testMthMethodSpecifyingInitParam(){
        compareWithJavaSource("method/MethodSpecifyingInitParam");
    }
    @Test
    public void testMthMethodDefaultedParamCaptureInitParam(){
        compareWithJavaSource("method/MethodDefaultedParamCaptureInitParam");
    }
    @Test
    public void testMthRefinedMethodSpecifyingTopLevel(){
        compareWithJavaSource("method/RefinedMethodSpecifyingTopLevel");
    }
    @Test
    public void testMthLocalMethodSpecifyingMethod(){
        compareWithJavaSource("method/LocalMethodSpecifyingMethod");
    }
    @Test
    public void testMthLocalMethodSpecifyingParam(){
        compareWithJavaSource("method/LocalMethodSpecifyingParam");
    }
    @Test
    public void testMthVarargsMethodSpecifyingMethodWithSequence(){
        compareWithJavaSource("method/VarargsMethodSpecifyingMethodWithSequence");
    }
    @Test
    public void testMthVarargsMethodSpecifyingMethodWithVarargs(){
        compareWithJavaSource("method/VarargsMethodSpecifyingMethodWithVarargs");
    }
    @Test
    public void testMthSequenceMethodSpecifyingMethodWithVarargs(){
        compareWithJavaSource("method/SequenceMethodSpecifyingMethodWithVarargs");
    }
    @Test
    public void testTwoParamLists(){
        compareWithJavaSource("method/TwoParamLists");
    }
    @Test
    public void testTwoParamListsDefaulted(){
        compareWithJavaSource("method/TwoParamListsDefaulted");
    }
    @Test
    public void testThreeParamLists(){
        compareWithJavaSource("method/ThreeParamLists");
    }
    @Test
    public void testTwoParamListsVoid(){
        compareWithJavaSource("method/TwoParamListsVoid");
    }
    @Test
    public void testTwoParamListsTP(){
        compareWithJavaSource("method/TwoParamListsTP");
    }
    @Test
    public void testCallableEscaping(){
        compareWithJavaSource("method/CallableEscaping");
    }
    
}
