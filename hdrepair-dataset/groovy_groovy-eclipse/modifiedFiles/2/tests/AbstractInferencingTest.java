/*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.jdt.core.groovy.tests.search;

import groovyjarjarasm.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.groovy.search.ITypeRequestor;
import org.eclipse.jdt.groovy.search.TypeInferencingVisitorWithRequestor;
import org.eclipse.jdt.groovy.search.TypeLookupResult;
import org.eclipse.jdt.groovy.search.TypeLookupResult.TypeConfidence;
import org.eclipse.jdt.groovy.search.VariableScope;

/**
 * @author Andrew Eisenberg
 * @created Nov 13, 2009
 */
public abstract class AbstractInferencingTest extends AbstractGroovySearchTest {

    public AbstractInferencingTest(String name) {
        super(name);
    }


    protected void assertType(String contents, String expectedType) {
        assertType(contents, 0, contents.length(), expectedType, false);
    }

    protected void assertType(String contents, int exprStart, int exprEnd,
            String expectedType) {
        assertType(contents, exprStart, exprEnd, expectedType, false);
    }
    
    protected void assertTypeOneOf(String contents, int start, int end,
			String... expectedTypes) throws Throwable {
    	boolean ok = false;
    	Throwable error = null;
    	for (int i = 0; !ok && i < expectedTypes.length; i++) {
    		try {
    			assertType(contents, expectedTypes[i]);
    			ok = true;
    		} catch (Throwable e) {
    			error = e;
    		}
		}
    	if (!ok) {
    		if (error!=null) {
    			throw error;
    		} else {
    			fail("assertTypeOneOf must be called with at least one expectedType");
    		}
    	}
	}

    
    
	public static void assertType(GroovyCompilationUnit contents, int start, int end, String expectedType) {
        assertType(contents, start, end, expectedType, false);
	}
    
	protected void assertType(String contents, String expectedType, boolean forceWorkingCopy) {
        assertType(contents, 0, contents.length(), expectedType, forceWorkingCopy);
    }

    protected void assertType(String contents, int exprStart, int exprEnd, String expectedType, boolean forceWorkingCopy) {
        assertType(contents, exprStart, exprEnd, expectedType, null, forceWorkingCopy);
    }
	public static void assertType(GroovyCompilationUnit contents, int exprStart,
			int exprEnd, String expectedType, boolean forceWorkingCopy) {
        assertType(contents, exprStart, exprEnd, expectedType, null, forceWorkingCopy);
	}
    
	public static void assertType(GroovyCompilationUnit unit,
			int exprStart, int exprEnd, String expectedType, String extraDocSnippet,
			boolean forceWorkingCopy) {
        SearchRequestor requestor = doVisit(exprStart, exprEnd, unit, forceWorkingCopy);
        
        assertNotNull("Did not find expected ASTNode", requestor.node);
        if (! expectedType.equals(printTypeName(requestor.result.type))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected type not found.\n");
            sb.append("Expected: " + expectedType + "\n");
            sb.append("Found: " + printTypeName(requestor.result.type) + "\n");
            sb.append("Declaring type: " + printTypeName(requestor.result.declaringType) + "\n");
            sb.append("ASTNode: " + requestor.node + "\n");
            sb.append("Confidence: " + requestor.result.confidence + "\n");
            fail(sb.toString());
        }
        
        if (extraDocSnippet != null && (requestor.result.extraDoc==null || !requestor.result.extraDoc.contains(extraDocSnippet))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Incorrect Doc found.\n");
            sb.append("Expected doc should contain: " + extraDocSnippet + "\n");
            sb.append("Found: " + requestor.result.extraDoc + "\n");
            sb.append("ASTNode: " + requestor.node + "\n");
            sb.append("Confidence: " + requestor.result.confidence + "\n");
            fail(sb.toString());
        }
        
        // this is from https://issuetracker.springsource.com/browse/STS-1854
        // make sure that the Type parameterization of Object has not been messed up
        assertNull("Problem!!! Object type has type parameters now.  See STS-1854", VariableScope.OBJECT_CLASS_NODE.getGenericsTypes());
	}

	/**
	 * Checks the compilation unit for the expected type and declaring type.
	 * @param assumeNoUnknowns 
	 * @return null if all is OK, or else returns an error message specifying the problem
	 */
    public static String checkType(GroovyCompilationUnit unit, int exprStart,
            int exprEnd, String expectedType, String expectedDeclaringType, boolean assumeNoUnknowns, boolean forceWorkingCopy) {
        SearchRequestor requestor = doVisit(exprStart, exprEnd, unit, forceWorkingCopy);
        if (requestor.node == null) {
            return "Did not find expected ASTNode.  (Start:" + exprStart + ", End:" + exprEnd + ")\n" +
            		"text:" +  String.valueOf(CharOperation.subarray(unit.getContents(), exprStart, exprEnd)) + "\n";
        }
        if (expectedType != null && !expectedType.equals(printTypeName(requestor.result.type))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected type not found.\n");
            sb.append("Expected: " + expectedType + "\n");
            sb.append("Found: " + printTypeName(requestor.result.type) + "\n");
            sb.append("Declaring type: " + printTypeName(requestor.result.declaringType) + "\n");
            sb.append("ASTNode: " + requestor.node + "\n");
            sb.append("Confidence: " + requestor.result.confidence + "\n");
            sb.append("Line, column: " + requestor.node.getLineNumber() + ", " + requestor.node.getColumnNumber());
            return sb.toString();
        }
        if (expectedDeclaringType != null && !expectedDeclaringType.equals(printTypeName(requestor.result.declaringType))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected declaring type not found.\n");
            sb.append("Expected: " + expectedDeclaringType + "\n");
            sb.append("Found: " + printTypeName(requestor.result.declaringType) + "\n");
            sb.append("Type: " + printTypeName(requestor.result.type) + "\n");
            sb.append("ASTNode: " + requestor.node + " : " +  requestor.node.getText() + "\n");
            sb.append("Confidence: " + requestor.result.confidence + "\n");
            sb.append("Line, column: " + requestor.node.getLineNumber() + ", " + requestor.node.getColumnNumber() + "\n");
            return sb.toString();
        }
        
        if (assumeNoUnknowns && !requestor.unknowns.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The following Unknown nodes were found (line:column):\n");
            for (ASTNode unknown : requestor.unknowns) {
                sb.append("(" + unknown.getLineNumber() + ":" + unknown.getColumnNumber() + ") ");
                sb.append(unknown + "\n");
            }
            return sb.toString();
        }
        
        if (VariableScope.OBJECT_CLASS_NODE.getGenericsTypes() != null) {
            return "Problem!!! Object type has type parameters now.  See STS-1854\n";
        }
        
        return null;
    }

	protected void assertType(String contents, int exprStart, int exprEnd,
            String expectedType, String extraDocSnippet, boolean forceWorkingCopy) {
        GroovyCompilationUnit unit = createUnit("Search", contents);
        assertType(unit, exprStart, exprEnd, expectedType, extraDocSnippet, forceWorkingCopy);
    }
    
    /**
     * Asserts that the declaration returned at the selection is deprecated
     * Checks only for the deprecated flag, (and so will only succeed for deprecated 
     * DSLDs).  Could change this in the future
     * 
     * @param contents
     * @param exprStart
     * @param exprEnd
     */
    protected void assertDeprecated(String contents, int exprStart, int exprEnd) {
        GroovyCompilationUnit unit = createUnit("Search", contents);
        SearchRequestor requestor = doVisit(exprStart, exprEnd, unit, false);
        assertNotNull("Did not find expected ASTNode", requestor.node);
        assertTrue("Declaration should be deprecated: " + requestor.result.declaration, hasDeprecatedFlag((AnnotatedNode) requestor.result.declaration));
    }

    
    private boolean hasDeprecatedFlag(AnnotatedNode declaration) {
        int flags;

        if (declaration instanceof PropertyNode) {
        	declaration = ((PropertyNode) declaration).getField();
        }
        if (declaration instanceof ClassNode) {
            flags = ((ClassNode) declaration).getModifiers();
        } else if (declaration instanceof MethodNode) {
            flags = ((MethodNode) declaration).getModifiers();
        } else if (declaration instanceof FieldNode) {
            flags = ((FieldNode) declaration).getModifiers();
        } else {
            flags = 0;
        }

        return (flags & Opcodes.ACC_DEPRECATED) != 0;
    }
    
    public static SearchRequestor doVisit(int exprStart, int exprEnd, GroovyCompilationUnit unit, boolean forceWorkingCopy) {
        try {
            if (forceWorkingCopy) {
                unit.becomeWorkingCopy(null);
            }
            try {
                TypeInferencingVisitorWithRequestor visitor = factory.createVisitor(unit);
                visitor.DEBUG = true;
                SearchRequestor requestor = new SearchRequestor(exprStart, exprEnd);
                visitor.visitCompilationUnit(requestor);
                return requestor;
            } finally {
                if (forceWorkingCopy) {
                    unit.discardWorkingCopy();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    
    protected void assertDeclaringType(String contents, int exprStart, int exprEnd,
            String expectedDeclaringType) {
        assertDeclaringType(contents, exprStart, exprEnd, expectedDeclaringType, false);
    }
    
    protected enum DeclarationKind { FIELD, METHOD, PROPERTY, CLASS }
    protected void assertDeclaration(String contents, int exprStart, int exprEnd,
            String expectedDeclaringType, String declarationName, DeclarationKind kind) {
        assertDeclaringType(contents, exprStart, exprEnd, expectedDeclaringType, false, false);
        GroovyCompilationUnit unit = createUnit("Search", contents);
        SearchRequestor requestor = doVisit(exprStart, exprEnd, unit, false);
        
        switch (kind) {
            case FIELD:
                assertTrue("Expecting field, but was " + requestor.result.declaration, 
                        requestor.result.declaration instanceof FieldNode);
                assertEquals("Wrong field name", declarationName, ((FieldNode) requestor.result.declaration).getName());
                break;
            case METHOD:
                assertTrue("Expecting method, but was " + requestor.result.declaration, 
                        requestor.result.declaration instanceof MethodNode);
                assertEquals("Wrong method name", declarationName, ((MethodNode) requestor.result.declaration).getName());
                break;
            case PROPERTY:
                assertTrue("Expecting property, but was " + requestor.result.declaration, 
                        requestor.result.declaration instanceof PropertyNode);
                assertEquals("Wrong property name", declarationName, ((PropertyNode) requestor.result.declaration).getName());
                break;
            case CLASS:
                assertTrue("Expecting class, but was " + requestor.result.declaration, 
                        requestor.result.declaration instanceof ClassNode);
                assertEquals("Wrong class name", declarationName, ((ClassNode) requestor.result.declaration).getName());
                
        }
        
    }
    
    protected void assertDeclaringType(String contents, int exprStart, int exprEnd,
            String expectedDeclaringType, boolean forceWorkingCopy) {
        
        assertDeclaringType(contents, exprStart, exprEnd, expectedDeclaringType, forceWorkingCopy, false);
    }
    protected void assertDeclaringType(String contents, int exprStart, int exprEnd,
            String expectedDeclaringType, boolean forceWorkingCopy, boolean expectingUnknown) {
        GroovyCompilationUnit unit = createUnit("Search", contents);
        SearchRequestor requestor = doVisit(exprStart, exprEnd, unit, forceWorkingCopy);
        
        assertNotNull("Did not find expected ASTNode", requestor.node);
        if (! expectedDeclaringType.equals(requestor.getDeclaringTypeName())) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected declaring type not found.\n");
            sb.append("Expected: " + expectedDeclaringType + "\n");
            sb.append("Found type: " + printTypeName(requestor.result.type) + "\n");
            sb.append("Found declaring type: " + printTypeName(requestor.result.declaringType) + "\n");
            sb.append("ASTNode: " + requestor.node + "\n");
            fail(sb.toString());
        }
        if (expectingUnknown) {
            if (requestor.result.confidence != TypeConfidence.UNKNOWN) {
                StringBuilder sb = new StringBuilder();
                sb.append("Confidence: " + requestor.result.confidence + " (but expecting UNKNOWN)\n");
                sb.append("Expected: " + expectedDeclaringType + "\n");
                sb.append("Found: " + printTypeName(requestor.result.type) + "\n");
                sb.append("Declaring type: " + printTypeName(requestor.result.declaringType) + "\n");
                sb.append("ASTNode: " + requestor.node + "\n");
                fail(sb.toString());
            }
        } else {
            if (requestor.result.confidence == TypeConfidence.UNKNOWN) {
                StringBuilder sb = new StringBuilder();
                sb.append("Expected Confidence should not have been UNKNOWN, but it was.\n");
                sb.append("Expected declaring type: " + expectedDeclaringType + "\n");
                sb.append("Found type: " + printTypeName(requestor.result.type) + "\n");
                sb.append("Found declaring type: " + printTypeName(requestor.result.declaringType) + "\n");
                sb.append("ASTNode: " + requestor.node + "\n");
                fail(sb.toString());
            }
        }
    }
    
    protected void assertUnknownConfidence(String contents, int exprStart, int exprEnd,
            String expectedDeclaringType, boolean forceWorkingCopy) {
        GroovyCompilationUnit unit = createUnit("Search", contents);
        SearchRequestor requestor = doVisit(exprStart, exprEnd, unit, forceWorkingCopy);
        
        assertNotNull("Did not find expected ASTNode", requestor.node);
        if (requestor.result.confidence != TypeConfidence.UNKNOWN) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expecting unknown confidentce, but was " + requestor.result.confidence + ".\n");
            sb.append("Expected: " + expectedDeclaringType + "\n");
            sb.append("Found: " + printTypeName(requestor.result.type) + "\n");
            sb.append("Declaring type: " + printTypeName(requestor.result.declaringType) + "\n");
            sb.append("ASTNode: " + requestor.node + "\n");
            fail(sb.toString());
        }
    }
    public static String printTypeName(ClassNode type) {
    	if (type == null) {
    		return "null";
    	}
    	String arraySuffix = "";
    	while (type.getComponentType() != null) {
    		arraySuffix+="[]";
    		type = type.getComponentType();
    	}
        String name = type.getName() + arraySuffix;
        return name + printGenerics(type);
    }

    public static String printGenerics(ClassNode type) {
        if (type.getGenericsTypes() == null || type.getGenericsTypes().length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        for (int i = 0; i < type.getGenericsTypes().length; i++) {
            GenericsType gt = type.getGenericsTypes()[i];
            sb.append(printTypeName(gt.getType()));
            if (i < type.getGenericsTypes().length-1) {
                sb.append(',');
            }
        }
        sb.append('>');
        return sb.toString();
    }

    public class UnknownTypeRequestor  implements ITypeRequestor {
        private List<ASTNode> unknownNodes = new ArrayList<ASTNode>();
        
        public List<ASTNode> getUnknownNodes() {
            return unknownNodes;
        }
        
        public VisitStatus acceptASTNode(ASTNode node, TypeLookupResult result,
                IJavaElement enclosingElement) {
            if (result.confidence == TypeConfidence.UNKNOWN && node.getEnd() > 0) {
                unknownNodes.add(node);
            }
            return VisitStatus.CONTINUE;
        }
        
    }
    
    public static class SearchRequestor implements ITypeRequestor {

        private final int start;
        private final int end;
        
        public TypeLookupResult result;
        public ASTNode node;
        
        public final List<ASTNode> unknowns = new ArrayList<ASTNode>();
        
        public SearchRequestor(int start, int end) {
            super();
            this.start = start;
            this.end = end;
        }

        public VisitStatus acceptASTNode(ASTNode visitorNode, TypeLookupResult visitorResult,
                IJavaElement enclosingElement) {
            
        	// might have AST nodes with overlapping locations, so result may not be null
            if (this.result == null && 
            		visitorNode.getStart() == start && visitorNode.getEnd() == end && 
                    !(visitorNode instanceof MethodNode /* ignore the run() method*/) &&
                    !(visitorNode instanceof Statement /* ignore all statements */) &&
                    !(visitorNode instanceof ClassNode && ((ClassNode) visitorNode).isScript() /* ignore the script */ )) {
                if (ClassHelper.isPrimitiveType(visitorResult.type)) {
                    this.result = new TypeLookupResult(ClassHelper.getWrapper(visitorResult.type), visitorResult.declaringType, visitorResult.declaration, visitorResult.confidence, visitorResult.scope, visitorResult.extraDoc);
                } else {
                	this.result = visitorResult;
                }
                this.node = visitorNode;
            }
            
            if (visitorResult.confidence == TypeConfidence.UNKNOWN && visitorNode.getEnd() > 0) {
                unknowns.add(visitorNode);
            }
            // always continue since we need to viist to the end to check consistency of 
            // inferencing engine stacks
            return VisitStatus.CONTINUE;
        }
        
        public String getDeclaringTypeName() {
            return printTypeName(result.declaringType);
        }
        
        public String getTypeName() {
            return result.type.getName();
        }
    }

}