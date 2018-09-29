package org.jetbrains.jet.codegen;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import gnu.trove.THashSet;
import jet.Range;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethod;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.*;

/**
 * @author max
 * @author yole
 */
public class ExpressionCodegen extends JetVisitor<StackValue, StackValue> {
    private static final String CLASS_OBJECT = "java/lang/Object";
    private static final String CLASS_STRING = "java/lang/String";
    public static final String CLASS_STRING_BUILDER = "java/lang/StringBuilder";
    private static final String CLASS_COMPARABLE = "java/lang/Comparable";
    private static final String CLASS_ITERABLE = "java/lang/Iterable";
    private static final String CLASS_ITERATOR = "java/util/Iterator";

    private static final String CLASS_RANGE = "jet/Range";
    private static final String CLASS_NO_PATTERN_MATCHED_EXCEPTION = "jet/NoPatternMatchedException";
    private static final String CLASS_TYPE_CAST_EXCEPTION = "jet/TypeCastException";

    private static final String ITERABLE_ITERATOR_DESCRIPTOR = "()Ljava/util/Iterator;";
    private static final String ITERATOR_HASNEXT_DESCRIPTOR = "()Z";
    private static final String ITERATOR_NEXT_DESCRIPTOR = "()Ljava/lang/Object;";

    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type INTEGER_TYPE = Type.getType(Integer.class);
    private static final Type ITERATOR_TYPE = Type.getType(Iterator.class);
    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private static final Type STRING_TYPE = Type.getObjectType(CLASS_STRING);

    private static final Type RANGE_TYPE = Type.getType(Range.class);

    private final Stack<Label> myContinueTargets = new Stack<Label>();
    private final Stack<Label> myBreakTargets = new Stack<Label>();

    private int myLastLineNumber = -1;

    private static final String[] PRIMITIVE_TYPE_INFO_FIELDS = {
            null,
            "BOOL_TYPE_INFO",
            "CHAR_TYPE_INFO",
            "BYTE_TYPE_INFO",
            "SHORT_TYPE_INFO",
            "INT_TYPE_INFO",
            "FLOAT_TYPE_INFO",
            "LONG_TYPE_INFO",
            "DOUBLE_TYPE_INFO"
    };

    private final InstructionAdapterEx v;
    private final FrameMap myMap;
    private final JetTypeMapper typeMapper;
    private final GenerationState state;
    private final Type returnType;
    private final BindingContext bindingContext;
    private final Map<TypeParameterDescriptor, StackValue> typeParameterExpressions = new HashMap<TypeParameterDescriptor, StackValue>();
    private final ClassContext context;
    private final IntrinsicMethods intrinsics;

    public ExpressionCodegen(MethodVisitor v,
                             FrameMap myMap,
                             Type returnType,
                             ClassContext context,
                             GenerationState state) {
        this.myMap = myMap;
        this.typeMapper = state.getTypeMapper();
        this.returnType = returnType;
        this.state = state;
        this.v = new InstructionAdapterEx(v);
        this.bindingContext = state.getBindingContext();
        this.context = context;
        this.intrinsics = state.getIntrinsics();
    }

    public JetTypeMapper getTypeMapper() {
        return state.getTypeMapper();
    }

    public void addTypeParameter(TypeParameterDescriptor typeParameter, StackValue expression) {
        typeParameterExpressions.put(typeParameter, expression);
    }

    static void loadTypeInfo(JetTypeMapper typeMapper, ClassDescriptor descriptor, InstructionAdapter v) {
        String owner = typeMapper.jvmName(descriptor, OwnerKind.IMPLEMENTATION);
        if (descriptor.getTypeConstructor().getParameters().size() > 0) {
            v.load(0, JetTypeMapper.TYPE_OBJECT);
            v.getfield(owner, "$typeInfo", "Ljet/typeinfo/TypeInfo;");
        }
        else {
            v.getstatic(owner, "$typeInfo", "Ljet/typeinfo/TypeInfo;");
        }
    }

    public StackValue genQualified(StackValue receiver, JetElement selector) {
        markLineNumber(selector);
        return selector.visit(this, receiver);
    }

    public StackValue gen(JetElement expr) {
        return genQualified(StackValue.none(), expr);
    }

    public void gen(JetElement expr, Type type) {
        StackValue value = gen(expr);
        value.put(type, v);
    }

    public void genToJVMStack(JetExpression expr) {
        gen(expr, expressionType(expr));
    }

    @Override
    public StackValue visitExpression(JetExpression expression, StackValue receiver) {
        throw new UnsupportedOperationException("Codegen for " + expression + " is not yet implemented");
    }

    @Override
    public StackValue visitParenthesizedExpression(JetParenthesizedExpression expression, StackValue receiver) {
        return genQualified(receiver, expression.getExpression());
    }

    @Override
    public StackValue visitAnnotatedExpression(JetAnnotatedExpression expression, StackValue receiver) {
        return genQualified(receiver, expression.getBaseExpression());
    }

    @Override
    public StackValue visitIfExpression(JetIfExpression expression, StackValue receiver) {
        Type asmType = expressionType(expression);
        StackValue condition = gen(expression.getCondition());

        JetExpression thenExpression = expression.getThen();
        JetExpression elseExpression = expression.getElse();

        if (thenExpression == null && elseExpression == null) {
            throw new CompilationException();
        }

        if (thenExpression == null) {
            return generateSingleBranchIf(condition, elseExpression, false);
        }

        if (elseExpression == null) {
            return generateSingleBranchIf(condition, thenExpression, true);
        }


        Label elseLabel = new Label();
        condition.condJump(elseLabel, true, v);   // == 0, i.e. false

        gen(thenExpression, asmType);

        Label endLabel = new Label();
        v.goTo(endLabel);
        v.mark(elseLabel);

        gen(elseExpression, asmType);

        v.mark(endLabel);

        return StackValue.onStack(asmType);
    }

    @Override
    public StackValue visitWhileExpression(JetWhileExpression expression, StackValue receiver) {
        Label condition = new Label();
        myContinueTargets.push(condition);
        v.mark(condition);

        Label end = new Label();
        myBreakTargets.push(end);

        final StackValue conditionValue = gen(expression.getCondition());
        conditionValue.condJump(end, true, v);

        gen(expression.getBody(), Type.VOID_TYPE);
        v.goTo(condition);

        v.mark(end);
        myBreakTargets.pop();
        myContinueTargets.pop();

        return StackValue.onStack(Type.VOID_TYPE);
    }

    @Override
    public StackValue visitDoWhileExpression(JetDoWhileExpression expression, StackValue receiver) {
        Label condition = new Label();
        v.mark(condition);
        myContinueTargets.push(condition);

        Label end = new Label();
        myBreakTargets.push(end);

        gen(expression.getBody(), Type.VOID_TYPE);

        final StackValue conditionValue = gen(expression.getCondition());
        conditionValue.condJump(condition, false, v);

        v.mark(end);

        myBreakTargets.pop();
        myContinueTargets.pop();
        return StackValue.onStack(Type.VOID_TYPE);
    }

    @Override
    public StackValue visitForExpression(JetForExpression expression, StackValue receiver) {
        final JetExpression loopRange = expression.getLoopRange();
        final JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, loopRange);
        Type loopRangeType = typeMapper.mapType(expressionType);
        if (loopRangeType.getSort() == Type.ARRAY) {
            new ForInArrayLoopGenerator(expression, loopRangeType).invoke();
            return StackValue.none();
        }
        else {
            final DeclarationDescriptor descriptor = expressionType.getConstructor().getDeclarationDescriptor();
            final PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
            if (declaration instanceof PsiClass) {
                final Project project = declaration.getProject();
                final PsiClass iterable = JavaPsiFacade.getInstance(project).findClass("java.lang.Iterable", ProjectScope.getAllScope(project));
                if (((PsiClass) declaration).isInheritor(iterable, true)) {
                    generateForInIterable(expression, loopRangeType);
                    return StackValue.none();
                }
            }
            if (isClass(descriptor, "IntRange")) {       // TODO IntRange subclasses
                new ForInRangeLoopGenerator(expression, loopRangeType).invoke();
                return StackValue.none();
            }
            throw new UnsupportedOperationException("for/in loop currently only supported for arrays and Iterable instances");
        }
    }

    private void generateForInIterable(JetForExpression expression, Type loopRangeType) {
        final JetParameter loopParameter = expression.getLoopParameter();
        final VariableDescriptor parameterDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, loopParameter);
        JetType paramType = parameterDescriptor.getOutType();
        Type asmParamType = typeMapper.mapType(paramType);

        int iteratorVar = myMap.enterTemp();
        gen(expression.getLoopRange(), loopRangeType);
        v.invokeinterface(CLASS_ITERABLE, "iterator", ITERABLE_ITERATOR_DESCRIPTOR);
        v.store(iteratorVar, ITERATOR_TYPE);

        Label begin = new Label();
        Label end = new Label();
        myContinueTargets.push(begin);
        myBreakTargets.push(end);

        v.mark(begin);
        v.load(iteratorVar, ITERATOR_TYPE);
        v.invokeinterface(CLASS_ITERATOR, "hasNext", ITERATOR_HASNEXT_DESCRIPTOR);
        v.ifeq(end);

        myMap.enter(parameterDescriptor, asmParamType.getSize());
        v.load(iteratorVar, ITERATOR_TYPE);
        v.invokeinterface(CLASS_ITERATOR, "next", ITERATOR_NEXT_DESCRIPTOR);
        // TODO checkcast should be generated via StackValue
        if (asmParamType.getSort() == Type.OBJECT && !"java.lang.Object".equals(asmParamType.getClassName())) {
            v.checkcast(asmParamType);
        }
        v.store(lookupLocal(parameterDescriptor), asmParamType);

        gen(expression.getBody(), Type.VOID_TYPE);

        v.goTo(begin);
        v.mark(end);

        int paramIndex = myMap.leave(parameterDescriptor);
        v.visitLocalVariable(loopParameter.getName(), asmParamType.getDescriptor(), null, begin, end, paramIndex);
        myMap.leaveTemp();
        myBreakTargets.pop();
        myContinueTargets.pop();
    }

    private DeclarationDescriptor contextType() {
        return context.getContextClass();
    }

    private OwnerKind contextKind() {
        return context.getContextKind();
    }

    private StackValue thisExpression() {
        return context.getThisExpression();
    }

    private abstract class ForLoopGenerator {
        protected JetForExpression expression;
        protected Type loopRangeType;
        protected VariableDescriptor parameterDescriptor;

        public ForLoopGenerator(JetForExpression expression, Type loopRangeType) {
            this.expression = expression;
            this.loopRangeType = loopRangeType;
            final JetParameter loopParameter = expression.getLoopParameter();
            this.parameterDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, loopParameter);
        }

        public void invoke() {
            JetType paramType = parameterDescriptor.getOutType();
            Type asmParamType = typeMapper.mapType(paramType);

            myMap.enter(parameterDescriptor, asmParamType.getSize());
            generatePrologue();

            Label condition = new Label();
            Label increment = new Label();
            Label end = new Label();
            v.mark(condition);
            myContinueTargets.push(increment);
            myBreakTargets.push(end);

            generateCondition(asmParamType, end);

            gen(expression.getBody(), Type.VOID_TYPE);

            v.mark(increment);
            generateIncrement();
            v.goTo(condition);
            v.mark(end);

            cleanupTemp();
            final int paramIndex = myMap.leave(parameterDescriptor);
            v.visitLocalVariable(expression.getLoopParameter().getName(), asmParamType.getDescriptor(), null, condition, end, paramIndex);
            myBreakTargets.pop();
            myContinueTargets.pop();
        }

        protected void generatePrologue() {
        }

        protected abstract void generateCondition(Type asmParamType, Label end);

        protected abstract void generateIncrement();

        protected void cleanupTemp() {
        }
    }

    private class ForInArrayLoopGenerator extends ForLoopGenerator {
        private int myLengthVar;
        private int myIndexVar;

        public ForInArrayLoopGenerator(JetForExpression expression, Type loopRangeType) {
            super(expression, loopRangeType);
        }

        @Override
        protected void generatePrologue() {
            myLengthVar = myMap.enterTemp();
            gen(expression.getLoopRange(), loopRangeType);
            v.arraylength();
            v.store(myLengthVar, Type.INT_TYPE);
            myIndexVar = myMap.enterTemp();
            v.aconst(0);
            v.store(myIndexVar, Type.INT_TYPE);
        }

        protected void generateCondition(Type asmParamType, Label end) {
            v.load(myIndexVar, Type.INT_TYPE);
            v.load(myLengthVar, Type.INT_TYPE);
            v.ificmpge(end);

            gen(expression.getLoopRange(), loopRangeType);  // array
            v.load(myIndexVar, Type.INT_TYPE);
            v.aload(loopRangeType.getElementType());
            StackValue.onStack(loopRangeType.getElementType()).put(asmParamType, v);
            v.store(lookupLocal(parameterDescriptor), asmParamType);
        }

        protected void generateIncrement() {
            v.iinc(myIndexVar, 1);
        }

        protected void cleanupTemp() {
            myMap.leaveTemp(2);
        }
    }

    private class ForInRangeLoopGenerator extends ForLoopGenerator {
        private int myRangeVar;
        private int myEndVar;

        public ForInRangeLoopGenerator(JetForExpression expression, Type loopRangeType) {
            super(expression, loopRangeType);
        }

        @Override
        protected void generatePrologue() {
            myRangeVar = myMap.enterTemp();
            myEndVar = myMap.enterTemp();
            gen(expression.getLoopRange(), loopRangeType);
            v.dup();
            v.dup();
            v.store(myRangeVar, loopRangeType);

            v.invokevirtual("jet/IntRange", "getStartValue", "()I");
            v.store(lookupLocal(parameterDescriptor), Type.INT_TYPE);
            v.invokevirtual("jet/IntRange", "getEndValue", "()I");
            v.store(myEndVar, Type.INT_TYPE);
        }

        @Override
        protected void generateCondition(Type asmParamType, Label end) {
            v.load(lookupLocal(parameterDescriptor), Type.INT_TYPE);
            v.load(myEndVar, Type.INT_TYPE);
            v.ificmpgt(end);
        }

        @Override
        protected void generateIncrement() {
            v.iinc(lookupLocal(parameterDescriptor), 1);  // TODO support decreasing order
        }

        @Override
        protected void cleanupTemp() {
            myMap.leaveTemp(2);
        }
    }

    @Override
    public StackValue visitBreakExpression(JetBreakExpression expression, StackValue receiver) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();

        Label label = labelElement == null ? myBreakTargets.peek() : null; // TODO:

        v.goTo(label);
        return StackValue.none();
    }

    @Override
    public StackValue visitContinueExpression(JetContinueExpression expression, StackValue receiver) {
        String labelName = expression.getLabelName();

        Label label = labelName == null ? myContinueTargets.peek() : null; // TODO:

        v.goTo(label);
        return StackValue.none();
    }

    private StackValue generateSingleBranchIf(StackValue condition, JetExpression expression, boolean inverse) {
        Label endLabel = new Label();

        condition.condJump(endLabel, inverse, v);

        gen(expression, Type.VOID_TYPE);

        v.mark(endLabel);
        return StackValue.none();
    }

    @Override
    public StackValue visitConstantExpression(JetConstantExpression expression, StackValue receiver) {
        CompileTimeConstant<?> compileTimeValue = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression);
        assert compileTimeValue != null;
        return StackValue.constant(compileTimeValue.getValue(), expressionType(expression));
    }

    @Override
    public StackValue visitStringTemplateExpression(JetStringTemplateExpression expression, StackValue receiver) {
        StringBuilder constantValue = new StringBuilder("");
        for (JetStringTemplateEntry entry : expression.getEntries()) {
            if (entry instanceof JetLiteralStringTemplateEntry) {
                constantValue.append(entry.getText());
            }
            else if (entry instanceof JetEscapeStringTemplateEntry) {
                constantValue.append(((JetEscapeStringTemplateEntry) entry).getUnescapedValue());
            }
            else {
                constantValue = null;
                break;
            }
        }
        if (constantValue != null) {
            final Type type = expressionType(expression);
            return StackValue.constant(constantValue.toString(), type);
        }
        else {
            generateStringBuilderConstructor();
            for (JetStringTemplateEntry entry : expression.getEntries()) {
                if (entry instanceof JetStringTemplateEntryWithExpression) {
                    invokeAppend(entry.getExpression());
                }
                else {
                    String text = entry instanceof JetEscapeStringTemplateEntry
                            ? ((JetEscapeStringTemplateEntry) entry).getUnescapedValue()
                            : entry.getText();
                    v.aconst(text);
                    invokeAppendMethod(STRING_TYPE);
                }
            }
            v.invokevirtual(CLASS_STRING_BUILDER, "toString", "()Ljava/lang/String;");
            return StackValue.onStack(expressionType(expression));
        }
    }

    @Override
    public StackValue visitBlockExpression(JetBlockExpression expression, StackValue receiver) {
        List<JetElement> statements = expression.getStatements();
        return generateBlock(statements);
    }

    @Override
    public StackValue visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, StackValue receiver) {
        if (bindingContext.get(BindingContext.BLOCK, expression)) {
            return generateBlock(expression.getFunctionLiteral().getBodyExpression().getStatements());
        }
        else {
            final GeneratedAnonymousClassDescriptor closure = new ClosureCodegen(state, this, context).gen(expression);

            v.anew(Type.getObjectType(closure.getClassname()));
            v.dup();

            final Method cons = closure.getConstructor();

            if (closure.isCaptureThis()) {
                thisToStack();
            }

            for (int i = 0; i < closure.getArgs().size(); i++) {
                StackValue arg = closure.getArgs().get(i);
                arg.put(cons.getArgumentTypes()[i], v);
            }

            v.invokespecial(closure.getClassname(), "<init>", cons.getDescriptor());
            return StackValue.onStack(Type.getObjectType(closure.getClassname()));
        }
    }

    @Override
    public StackValue visitObjectLiteralExpression(JetObjectLiteralExpression expression, StackValue receiver) {
        GeneratedAnonymousClassDescriptor descriptor = state.generateObjectLiteral(expression, this, context);
        Type type = Type.getObjectType(descriptor.getClassname());
        v.anew(type);
        v.dup();
        v.invokespecial(descriptor.getClassname(), "<init>", descriptor.getConstructor().getDescriptor());
        return StackValue.onStack(type);
    }

    private StackValue generateBlock(List<JetElement> statements) {
        Label blockStart = new Label();
        v.mark(blockStart);

        for (JetElement statement : statements) {
            if (statement instanceof JetProperty) {
                final VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, statement);
                final Type type = typeMapper.mapType(variableDescriptor.getOutType());
                myMap.enter(variableDescriptor, type.getSize());
            }
        }

        StackValue answer = StackValue.none();
        for (int i = 0, statementsSize = statements.size(); i < statementsSize; i++) {
            JetElement statement = statements.get(i);
            if (i == statements.size() - 1 /*&& statement instanceof JetExpression && !bindingContext.get(BindingContext.STATEMENT, statement)*/) {
                answer = gen(statement);
            }
            else {
                gen(statement, Type.VOID_TYPE);
            }
        }

        Label blockEnd = new Label();
        v.mark(blockEnd);

        for (JetElement statement : statements) {
            if (statement instanceof JetProperty) {
                JetProperty var = (JetProperty) statement;
                VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, var);
                Type outType = typeMapper.mapType(variableDescriptor.getOutType());

                int index = myMap.leave(variableDescriptor);
                v.visitLocalVariable(var.getName(), outType.getDescriptor(), null, blockStart, blockEnd, index);
            }
        }

        return answer;
    }

    private void markLineNumber(JetElement statement) {
        final Document document = statement.getContainingFile().getViewProvider().getDocument();
        if (document != null) {
            int lineNumber = document.getLineNumber(statement.getTextRange().getStartOffset());  // 0-based
            if (lineNumber == myLastLineNumber) {
                return;
            }
            myLastLineNumber = lineNumber;

            Label label = new Label();
            v.visitLabel(label);
            v.visitLineNumber(lineNumber + 1, label);  // 1-based
        }
    }

    @Override
    public StackValue visitReturnExpression(JetReturnExpression expression, StackValue receiver) {
        final JetExpression returnedExpression = expression.getReturnedExpression();
        if (returnedExpression != null) {
            gen(returnedExpression, returnType);
            v.areturn(returnType);
        }
        else {
            v.visitInsn(Opcodes.RETURN);
        }
        return StackValue.none();
    }

    public void returnExpression(JetExpression expr) {
        StackValue lastValue = gen(expr);
        
        if (lastValue.type != Type.VOID_TYPE) {
            lastValue.put(returnType, v);
            v.areturn(returnType);
        }
        else if (!endsWithReturn(expr)) {
            v.areturn(returnType);
        }
    }

    private static boolean endsWithReturn(JetElement bodyExpression) {
        if (bodyExpression instanceof JetBlockExpression) {
            final List<JetElement> statements = ((JetBlockExpression) bodyExpression).getStatements();
            return statements.size() > 0 && statements.get(statements.size()-1) instanceof JetReturnExpression;
        }
    
        return bodyExpression instanceof JetReturnExpression;
    }
    
    @Override
    public StackValue visitSimpleNameExpression(JetSimpleNameExpression expression, StackValue receiver) {
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (descriptor instanceof NamespaceDescriptor) return StackValue.none(); // No code to generate

        if (descriptor instanceof VariableAsFunctionDescriptor) {
            descriptor = ((VariableAsFunctionDescriptor) descriptor).getVariableDescriptor();
        }

        final IntrinsicMethod intrinsic = intrinsics.getIntrinsic(descriptor);
        if (intrinsic != null) {
            final Type expectedType = expressionType(expression);
            return intrinsic.generate(this, v, expectedType, expression, Collections.<JetExpression>emptyList(), receiver);
        }

        final DeclarationDescriptor container = descriptor.getContainingDeclaration();

        PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        if (declaration instanceof PsiField) {
            PsiField psiField = (PsiField) declaration;
            final String owner = JetTypeMapper.jvmName(psiField.getContainingClass());
            final Type fieldType = JetTypeMapper.psiTypeToAsm(psiField.getType());
            final boolean isStatic = psiField.hasModifierProperty(PsiModifier.STATIC);
            if (!isStatic) {
                receiver.put(JetTypeMapper.TYPE_OBJECT, v);
            }
            return StackValue.field(fieldType, owner, psiField.getName(), isStatic);
        }
        else {
            int index = lookupLocal(descriptor);
            if (index >= 0) {
                final JetType outType = ((VariableDescriptor) descriptor).getOutType();
                return StackValue.local(index, typeMapper.mapType(outType));
            }
            else if (descriptor instanceof PropertyDescriptor) {
                final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

                //TODO: hack, will not need if resolve goes to right descriptor itself
                if (declaration instanceof JetParameter) {
                    if (PsiTreeUtil.getParentOfType(expression, JetDelegationSpecifier.class) != null) {
                        JetClass aClass = PsiTreeUtil.getParentOfType(expression, JetClass.class);
                        ConstructorDescriptor constructorDescriptor = bindingContext.get(BindingContext.CONSTRUCTOR, aClass);
                        List<ValueParameterDescriptor> parameters = constructorDescriptor.getValueParameters();
                        for (ValueParameterDescriptor parameter : parameters) {
                            if (parameter.getName().equals(descriptor.getName())) {
                                final JetType outType = ((VariableDescriptor) descriptor).getOutType();
                                return StackValue.local(lookupLocal(parameter), typeMapper.mapType(outType));
                            }
                        }
                    }
                }

                if (declaration instanceof JetObjectDeclarationName) {
                    JetObjectDeclaration objectDeclaration = PsiTreeUtil.getParentOfType(declaration, JetObjectDeclaration.class);
                    ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, objectDeclaration);
                    return StackValue.field(typeMapper.jvmType(classDescriptor, OwnerKind.IMPLEMENTATION),
                            typeMapper.jvmName(classDescriptor, OwnerKind.IMPLEMENTATION),
                            "$instance",
                            true);
                }
                else {
                    boolean isStatic = container instanceof NamespaceDescriptorImpl;
                    final boolean directToField = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER;
                    JetExpression r = getReceiverForSelector(expression);
                    final boolean forceInterface = r != null && !(r instanceof JetThisExpression);
                    final StackValue iValue = intermediateValueForProperty(propertyDescriptor, directToField, forceInterface);
                    if (!isStatic) {
                        if (receiver == StackValue.none()) {
                            receiver = generateThisOrOuter((ClassDescriptor) propertyDescriptor.getContainingDeclaration());
                        }
                        receiver.put(JetTypeMapper.TYPE_OBJECT, v);
                    }
                    return iValue;
                }
            }
            else if (descriptor instanceof ClassDescriptor) {
                final JetClassObject classObject = ((JetClass) declaration).getClassObject();
                if (classObject == null) {
                    throw new UnsupportedOperationException("trying to reference a class which doesn't have a class object");
                }
                final String type = typeMapper.jvmName(classObject);
                return StackValue.field(Type.getObjectType(type),
                                              typeMapper.jvmName((ClassDescriptor) descriptor, OwnerKind.IMPLEMENTATION),
                                              "$classobj",
                                              true);
            }
            else if (descriptor instanceof TypeParameterDescriptor) {
                loadTypeParameterTypeInfo((TypeParameterDescriptor) descriptor);
                v.invokevirtual("jet/typeinfo/TypeInfo", "getClassObject", "()Ljava/lang/Object;");
                return StackValue.onStack(OBJECT_TYPE);
            }
            else {
                // receiver
                StackValue.local(0, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, v);

                final StackValue value = context.lookupInContext(descriptor, v);
                if (value == null) {
                    throw new UnsupportedOperationException("don't know how to generate reference " + descriptor);
                }
                return value;
            }
        }
    }

    public int lookupLocal(DeclarationDescriptor descriptor) {
        return myMap.getIndex(descriptor);
    }

    public StackValue intermediateValueForProperty(PropertyDescriptor propertyDescriptor, final boolean forceField, boolean forceInterface) {
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        boolean isStatic = containingDeclaration instanceof NamespaceDescriptorImpl;
        propertyDescriptor = propertyDescriptor.getOriginal();
        final JetType outType = propertyDescriptor.getOutType();
        boolean isInsideClass = !forceInterface && containingDeclaration == contextType();
        Method getter;
        Method setter;
        if (forceField) {
            getter = null;
            setter = null;
        }
        else {
            getter = isInsideClass && propertyDescriptor.getGetter() == null ? null : typeMapper.mapGetterSignature(propertyDescriptor);
            setter = isInsideClass && propertyDescriptor.getSetter() == null ? null : typeMapper.mapSetterSignature(propertyDescriptor);
        }

        String owner;
        boolean isInterface;
        if (isInsideClass || isStatic) {
            owner = typeMapper.getOwner(propertyDescriptor, contextKind());
            isInterface = false;
        }
        else {
            owner = typeMapper.getOwner(propertyDescriptor, OwnerKind.INTERFACE);
            isInterface = !(containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor) containingDeclaration).isObject());
        }

        return StackValue.property(propertyDescriptor.getName(), owner, typeMapper.mapType(outType), isStatic, isInterface, getter, setter);
    }

    @Override
    public StackValue visitCallExpression(JetCallExpression expression, StackValue receiver) {
        final JetExpression callee = expression.getCalleeExpression();
        DeclarationDescriptor funDescriptor = resolveCalleeDescriptor(expression);

        if (funDescriptor instanceof ConstructorDescriptor) {
            return generateConstructorCall(expression, (JetSimpleNameExpression) callee);
        }
        else if (funDescriptor instanceof FunctionDescriptor) {
            final FunctionDescriptor fd = (FunctionDescriptor) funDescriptor;
            return invokeFunction(expression, fd, receiver);
        }
        else {
            throw new UnsupportedOperationException("unknown type of callee descriptor: " + funDescriptor);
        }
    }

    private StackValue invokeFunction(JetCallExpression expression, DeclarationDescriptor fd, StackValue receiver) {
        Callable callableMethod = resolveToCallable(fd);
        return invokeCallable(fd, callableMethod, expression, receiver);
    }

    @Nullable
    private StackValue invokeCallable(DeclarationDescriptor fd, Callable callable, JetCallExpression expression, StackValue receiver) {
        if (callable instanceof CallableMethod) {
            final CallableMethod callableMethod = (CallableMethod) callable;
            invokeMethodWithArguments(callableMethod, expression, receiver);

            final Type callReturnType = callableMethod.getSignature().getReturnType();
            return returnValueAsStackValue((FunctionDescriptor) fd, callReturnType);
        }
        else {
            IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
            List<JetExpression> args = new ArrayList<JetExpression>();
            for (JetValueArgument argument : expression.getValueArguments()) {
                args.add(argument.getArgumentExpression());
            }
            return intrinsic.generate(this, v, expressionType(expression), expression, args, receiver);
        }
    }

    private StackValue returnValueAsStackValue(FunctionDescriptor fd, Type callReturnType) {
        if (callReturnType != Type.VOID_TYPE) {
            final Type retType = typeMapper.mapType(fd.getReturnType());
            StackValue.onStack(callReturnType).upcast(retType, v);
            return StackValue.onStack(retType);
        }
        return StackValue.none();
    }

    private Callable resolveToCallable(DeclarationDescriptor fd) {
        final IntrinsicMethod intrinsic = intrinsics.getIntrinsic(fd);
        if (intrinsic != null) {
            return intrinsic;
        }
        PsiElement declarationPsiElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, fd);

        CallableMethod callableMethod;
        if (declarationPsiElement instanceof PsiMethod || declarationPsiElement instanceof JetNamedFunction) {
            callableMethod = typeMapper.mapToCallableMethod((PsiNamedElement) declarationPsiElement);
        }
        else if (fd instanceof FunctionDescriptor) {
            callableMethod = ClosureCodegen.asCallableMethod((FunctionDescriptor) fd);
        }
        else {
            throw new UnsupportedOperationException("can't resolve declaration to callable: " + fd);
        }
        return callableMethod;
    }

    private DeclarationDescriptor resolveCalleeDescriptor(JetCallExpression call) {
        JetExpression callee = call.getCalleeExpression();
        if (!(callee instanceof JetSimpleNameExpression)) {
            throw new UnsupportedOperationException("Don't know how to generate a call to " + callee);
        }
        DeclarationDescriptor funDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) callee);
        if (funDescriptor == null) {
            throw new CompilationException("Cannot resolve: " + callee.getText());
        }
        return funDescriptor;
    }

    public void invokeMethodWithArguments(CallableMethod callableMethod, JetCall expression) {
        invokeMethodWithArguments(callableMethod, expression, StackValue.none());
    }

    public void invokeMethodWithArguments(CallableMethod callableMethod, JetCall expression, StackValue receiver) {
        final Type calleeType = callableMethod.getGenerateCalleeType();
        if (calleeType != null && expression instanceof JetCallExpression) {
            gen(expression.getCalleeExpression(), calleeType);
        }
        if (callableMethod.isOwnerFromCall()) {
            setOwnerFromCall(callableMethod, expression);
        }
        if (callableMethod.needsReceiverOnStack()) {
            if (receiver == StackValue.none()) {
                receiver = thisExpression(); 
            }
            receiver.put(JetTypeMapper.TYPE_OBJECT, v);
        }
        pushMethodArguments(expression, callableMethod.getValueParameterTypes());
        if (callableMethod.acceptsTypeArguments()) {
            pushTypeArguments(expression);
        }
        callableMethod.invoke(v);
    }

    private void setOwnerFromCall(CallableMethod callableMethod, JetCall expression) {
        if (expression.getParent() instanceof JetQualifiedExpression) {
            final JetExpression receiver = ((JetQualifiedExpression) expression.getParent()).getReceiverExpression();
            JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, receiver);
            DeclarationDescriptor declarationDescriptor = expressionType.getConstructor().getDeclarationDescriptor();
            PsiElement ownerDeclaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declarationDescriptor);
            if (ownerDeclaration instanceof PsiClass) {
                callableMethod.setOwner(typeMapper.mapType(expressionType).getInternalName());
            }
        }
    }

    private static JetExpression getReceiverForSelector(PsiElement expression) {
        if (expression.getParent() instanceof JetDotQualifiedExpression && !isReceiver(expression)) {
            final JetDotQualifiedExpression parent = (JetDotQualifiedExpression) expression.getParent();
            return parent.getReceiverExpression();
        }
        return null;
    }

    private static boolean isSubclass(ClassDescriptor subClass, ClassDescriptor superClass) {
        Set<JetType> allSuperTypes = new THashSet<JetType>();

        addSuperTypes(subClass.getDefaultType(), allSuperTypes);

        final DeclarationDescriptor superOriginal = superClass.getOriginal();

        for (JetType superType : allSuperTypes) {
            final DeclarationDescriptor descriptor = superType.getConstructor().getDeclarationDescriptor();
            if (descriptor != null && superOriginal == descriptor.getOriginal()) {
                return true;
            }
        }

        return false;
    }

    private static void addSuperTypes(JetType type, Set<JetType> set) {
        set.add(type);

        for (JetType jetType : type.getConstructor().getSupertypes()) {
            addSuperTypes(jetType, set);
        }
    }

    public StackValue generateThisOrOuter(ClassDescriptor calleeContainingClass) {
        boolean thisDone = false;
        StackValue result = null;

        ClassContext cur = context;
        while (true) {
            ClassContext parentContext = cur.getParentContext();
            if (parentContext == null) break;

            final DeclarationDescriptor curContextType = cur.getContextDescriptor();
            if (curContextType instanceof ClassDescriptor) {
                if (isSubclass((ClassDescriptor) curContextType, calleeContainingClass)) break;

                final StackValue outer;
                if (!thisDone && myMap instanceof ConstructorFrameMap) {
                    outer = StackValue.local(((ConstructorFrameMap) myMap).getOuterThisIndex(), JetTypeMapper.TYPE_OBJECT);
                }
                else {
                    thisToStack();
                    outer = StackValue.field(parentContext.jvmType(typeMapper),
                                             cur.jvmType(typeMapper).getInternalName(),
                                             "this$0",
                                             false);
                }

                thisDone = true;
                result = outer;
            }

            cur = parentContext;
        }

        if (!thisDone) {
            return thisExpression();
        }
        return result;
    }

    private static boolean isReceiver(PsiElement expression) {
        final PsiElement parent = expression.getParent();
        if (parent instanceof JetQualifiedExpression) {
            final JetExpression receiverExpression = ((JetQualifiedExpression) parent).getReceiverExpression();
            return expression == receiverExpression;
        }
        return false;
    }

    private void pushMethodArguments(JetCall expression, List<Type> valueParameterTypes) {
        List<JetValueArgument> args = expression.getValueArguments();
        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            JetValueArgument arg = args.get(i);
            gen(arg.getArgumentExpression(), valueParameterTypes.get(i));
        }
    }

    public Type expressionType(JetExpression expr) {
        JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expr);
        return type == null ? Type.VOID_TYPE : typeMapper.mapType(type);
    }

    public int indexOfLocal(JetReferenceExpression lhs) {
        final DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, lhs);
        return lookupLocal(declarationDescriptor);
    }
    
    

    @Override
    public StackValue visitDotQualifiedExpression(JetDotQualifiedExpression expression, StackValue receiver) {
        StackValue receiverValue = resolvesToClassOrPackage(expression.getReceiverExpression())
                                   ? StackValue.none()
                                   : genQualified(receiver, expression.getReceiverExpression());
        return genQualified(receiverValue, expression.getSelectorExpression());
    }

    private boolean resolvesToClassOrPackage(JetExpression receiver) {
        if (receiver instanceof JetReferenceExpression) {
            DeclarationDescriptor declaration = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetReferenceExpression) receiver);
            PsiElement declarationElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declaration);
            if (declarationElement instanceof PsiClass) {
                return true;
            }
        }
        return false;
    }

    @Override
    public StackValue visitSafeQualifiedExpression(JetSafeQualifiedExpression expression, StackValue receiver) {
        genToJVMStack(expression.getReceiverExpression());
        Label ifnull = new Label();
        Label end = new Label();
        v.dup();
        v.ifnull(ifnull);
        JetType receiverType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression.getReceiverExpression());
        StackValue propValue = genQualified(StackValue.onStack(typeMapper.mapType(receiverType)), expression.getSelectorExpression());
        Type type = propValue.type;
        propValue.put(type, v);
        if(JetTypeMapper.isPrimitive(type) && !type.equals(Type.VOID_TYPE)) {
            v.valueOf(type);
            type = typeMapper.boxType(type);
        }
        v.goTo(end);

        v.mark(ifnull);
        v.pop();
        if(!propValue.type.equals(Type.VOID_TYPE)) {
            v.aconst(null);
        }
        v.mark(end);

        return StackValue.onStack(type);
    }

    @Override
    public StackValue visitPredicateExpression(JetPredicateExpression expression, StackValue receiver) {
        genToJVMStack(expression.getReceiverExpression());
        Label ifFalse = new Label();
        Label end = new Label();
        v.dup();
        StackValue result = gen(expression.getSelectorExpression());
        result.condJump(ifFalse, true, v);
        v.goTo(end);
        v.mark(ifFalse);
        v.pop();
        v.aconst(null);
        v.mark(end);
        return StackValue.onStack(expressionType(expression));
    }

    @Override
    public StackValue visitBinaryExpression(JetBinaryExpression expression, StackValue receiver) {
        final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();
        if (opToken == JetTokens.EQ) {
            return generateAssignmentExpression(expression);
        }
        else if (JetTokens.AUGMENTED_ASSIGNMENTS.contains(opToken)) {
            return generateAugmentedAssignment(expression);
        }
        else if (opToken == JetTokens.ANDAND) {
            return generateBooleanAnd(expression);
        }
        else if (opToken == JetTokens.OROR) {
            return generateBooleanOr(expression);
        }
        else if (opToken == JetTokens.EQEQ || opToken == JetTokens.EXCLEQ ||
                 opToken == JetTokens.EQEQEQ || opToken == JetTokens.EXCLEQEQEQ) {
            return generateEquals(expression.getLeft(), expression.getRight(), opToken);
        }
        else if (opToken == JetTokens.LT || opToken == JetTokens.LTEQ ||
                 opToken == JetTokens.GT || opToken == JetTokens.GTEQ) {
            return generateCompareOp(expression.getLeft(), expression.getRight(), opToken, expressionType(expression.getLeft()));
        }
        else if (opToken == JetTokens.ELVIS) {
            return generateElvis(expression);
        }
        else {
            DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
            final Callable callable = resolveToCallable(op);
            if (callable instanceof IntrinsicMethod) {
                IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
                return intrinsic.generate(this, v, expressionType(expression), expression,
                                          Arrays.asList(expression.getLeft(), expression.getRight()), receiver);
            }
            else {
                CallableMethod callableMethod = (CallableMethod) callable;
                genToJVMStack(expression.getLeft());
                genToJVMStack(expression.getRight());
                callableMethod.invoke(v);
                return  returnValueAsStackValue((FunctionDescriptor) op, callableMethod.getSignature().getReturnType());
            }
        }
    }

    private StackValue generateBooleanAnd(JetBinaryExpression expression) {
        gen(expression.getLeft(), Type.BOOLEAN_TYPE);
        Label ifFalse = new Label();
        v.ifeq(ifFalse);
        gen(expression.getRight(), Type.BOOLEAN_TYPE);
        Label end = new Label();
        v.goTo(end);
        v.mark(ifFalse);
        v.aconst(false);
        v.mark(end);
        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private StackValue generateBooleanOr(JetBinaryExpression expression) {
        gen(expression.getLeft(), Type.BOOLEAN_TYPE);
        Label ifTrue = new Label();
        v.ifne(ifTrue);
        gen(expression.getRight(), Type.BOOLEAN_TYPE);
        Label end = new Label();
        v.goTo(end);
        v.mark(ifTrue);
        v.aconst(true);
        v.mark(end);
        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private StackValue generateEquals(JetExpression left, JetExpression right, IElementType opToken) {
        Type leftType = expressionType(left);
        Type rightType = expressionType(right);
        if(JetTypeMapper.isPrimitive(leftType) != JetTypeMapper.isPrimitive(rightType)) {
            gen(left, leftType);
            v.valueOf(leftType);
            leftType = typeMapper.boxType(leftType);
            gen(right, rightType);
            v.valueOf(rightType);
            rightType = typeMapper.boxType(rightType);
        }
        else {
            gen(left, leftType);
            gen(right, rightType);
        }
        return generateEqualsForExpressionsOnStack(opToken, leftType, rightType);
    }

    private StackValue generateEqualsForExpressionsOnStack(IElementType opToken, Type leftType, Type rightType) {
        if (isNumberPrimitive(leftType) && leftType == rightType) {
            return compareExpressionsOnStack(opToken, leftType);
        }
        else {
            if (opToken == JetTokens.EQEQEQ || opToken == JetTokens.EXCLEQEQEQ) {
                return StackValue.cmp(opToken, leftType);
            }
            else {
                return generateNullSafeEquals(opToken);
            }
        }
    }

    private StackValue generateNullSafeEquals(IElementType opToken) {
        v.dup2();   // left right left right
        Label rightNull = new Label();
        v.ifnull(rightNull);
        Label leftNull = new Label();
        v.ifnull(leftNull);
        v.invokevirtual(CLASS_OBJECT, "equals", "(Ljava/lang/Object;)Z");
        Label end = new Label();
        v.goTo(end);
        v.mark(rightNull);
        // left right left
        Label bothNull = new Label();
        v.ifnull(bothNull);
        v.mark(leftNull);
        v.pop2();
        v.aconst(Boolean.FALSE);
        v.goTo(end);
        v.mark(bothNull);
        v.pop2();
        v.aconst(Boolean.TRUE);
        v.mark(end);

        final StackValue onStack = StackValue.onStack(Type.BOOLEAN_TYPE);
        if (opToken == JetTokens.EXCLEQ) {
            return StackValue.not(onStack);
        }
        return onStack;
    }

    private StackValue generateElvis(JetBinaryExpression expression) {
        final Type exprType = expressionType(expression);
        final Type leftType = expressionType(expression.getLeft());
        gen(expression.getLeft(), leftType);
        v.dup();
        Label end = new Label();
        Label ifNull = new Label();
        v.ifnull(ifNull);
        StackValue.onStack(leftType).put(exprType, v);
        v.goTo(end);
        v.mark(ifNull);
        v.pop();
        gen(expression.getRight(), exprType);
        v.mark(end);
        return StackValue.onStack(exprType);
    }

    private static boolean isNumberPrimitive(DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) {
            return false;
        }
        String className = descriptor.getName();
        return className.equals("Int") || className.equals("Long") || className.equals("Short") ||
               className.equals("Byte") || className.equals("Char") || className.equals("Float") ||
               className.equals("Double");
    }

    private static boolean isClass(DeclarationDescriptor descriptor, String name) {
        if (!(descriptor instanceof ClassDescriptor)) {
            return false;
        }
        String className = descriptor.getName();
        return className.equals(name);
    }

    private static boolean isNumberPrimitive(Type type) {
        return JetTypeMapper.isIntPrimitive(type) || type == Type.FLOAT_TYPE || type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE;
    }

    private StackValue generateCompareOp(JetExpression left, JetExpression right, IElementType opToken, Type operandType) {
        gen(left, operandType);
        gen(right, operandType);
        return compareExpressionsOnStack(opToken, operandType);
    }

    private StackValue compareExpressionsOnStack(IElementType opToken, Type operandType) {
        if (operandType.getSort() == Type.OBJECT) {
            v.invokeinterface(CLASS_COMPARABLE, "compareTo", "(Ljava/lang/Object;)I");
            v.aconst(0);
            operandType = Type.INT_TYPE;
        }
        return StackValue.cmp(opToken, operandType);
    }

    private StackValue generateAssignmentExpression(JetBinaryExpression expression) {
        StackValue stackValue = gen(expression.getLeft());
        genToJVMStack(expression.getRight());
        stackValue.store(v);
        return StackValue.none();
    }

    private StackValue generateAugmentedAssignment(JetBinaryExpression expression) {
        DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
        final Callable callable = resolveToCallable(op);
        final JetExpression lhs = expression.getLeft();
        Type lhsType = expressionType(lhs);
        if (bindingContext.get(BindingContext.VARIABLE_REASSIGNMENT, expression)) {
            if (callable instanceof IntrinsicMethod) {
                StackValue value = gen(lhs);              // receiver
                value.dupReceiver(v, 0);                                        // receiver receiver
                value.put(lhsType, v);                                          // receiver lhs
                final IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
                intrinsic.generate(this, v, lhsType, expression, Arrays.asList(expression.getRight()), null);
                value.store(v);
            }
            else {
                callAugAssignMethod(expression, (CallableMethod) callable, lhsType, true);
            }
        }
        else {
            final boolean keepReturnValue = !((FunctionDescriptor) op).getReturnType().equals(JetStandardClasses.getUnitType());
            callAugAssignMethod(expression, (CallableMethod) callable, lhsType, keepReturnValue);
        }
        
        return StackValue.none();
    }

    private void callAugAssignMethod(JetBinaryExpression expression, CallableMethod callable, Type lhsType, final boolean keepReturnValue) {
        StackValue value = gen(expression.getLeft());
        if (keepReturnValue) {
            value.dupReceiver(v, 0);
        }
        value.put(lhsType, v);
        genToJVMStack(expression.getRight());
        callable.invoke(v);
        if (keepReturnValue) {
            value.store(v);
        }
    }

    public void generateStringBuilderConstructor() {
        Type type = Type.getObjectType(CLASS_STRING_BUILDER);
        v.anew(type);
        v.dup();
        Method method = new Method("<init>", Type.VOID_TYPE, new Type[0]);
        v.invokespecial(CLASS_STRING_BUILDER, method.getName(), method.getDescriptor());
    }

    public void invokeAppend(final JetExpression expr) {
        if (expr instanceof JetBinaryExpression) {
            final JetBinaryExpression binaryExpression = (JetBinaryExpression) expr;
            if (binaryExpression.getOperationToken() == JetTokens.PLUS) {
                invokeAppend(binaryExpression.getLeft());
                invokeAppend(binaryExpression.getRight());
                return;
            }
        }
        Type exprType = expressionType(expr);
        gen(expr, exprType);
        invokeAppendMethod(exprType.getSort() == Type.ARRAY ? JetTypeMapper.TYPE_OBJECT : exprType);
    }

    public void invokeAppendMethod(Type exprType) {
        Method appendDescriptor = new Method("append", Type.getObjectType(CLASS_STRING_BUILDER),
                new Type[] { exprType.getSort() == Type.OBJECT ? JetTypeMapper.TYPE_OBJECT : exprType});
        v.invokevirtual(CLASS_STRING_BUILDER, "append", appendDescriptor.getDescriptor());
    }

    @Override
    public StackValue visitPrefixExpression(JetPrefixExpression expression, StackValue receiver) {
        DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationSign());
        final Callable callable = resolveToCallable(op);
        if (callable instanceof IntrinsicMethod) {
            IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
            return intrinsic.generate(this, v, expressionType(expression), expression,
                                      Arrays.asList(expression.getBaseExpression()), receiver);
        }
        else {
            CallableMethod callableMethod = (CallableMethod) callable;
            genToJVMStack(expression.getBaseExpression());
            callableMethod.invoke(v);
            return returnValueAsStackValue((FunctionDescriptor) op, callableMethod.getSignature().getReturnType());
        }
    }

    @Override
    public StackValue visitPostfixExpression(JetPostfixExpression expression, StackValue receiver) {
        DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationSign());
        if (op instanceof FunctionDescriptor) {
            final Type asmType = expressionType(expression);
            DeclarationDescriptor cls = op.getContainingDeclaration();
            if (isNumberPrimitive(cls) && (op.getName().equals("inc") || op.getName().equals("dec"))) {
                receiver.put(receiver.type, v);
                if (bindingContext.get(BindingContext.STATEMENT, expression)) {
                    generateIncrement(op, asmType, expression.getBaseExpression(), receiver);
                    return StackValue.none();
                }
                else {
                    gen(expression.getBaseExpression(), asmType);                               // old value
                    generateIncrement(op, asmType, expression.getBaseExpression(), receiver);   // increment in-place
                    return StackValue.onStack(asmType);                                         // old value
                }
            }
        }
        throw new UnsupportedOperationException("Don't know how to generate this prefix expression");
    }

    private void generateIncrement(DeclarationDescriptor op, Type asmType, JetExpression operand, StackValue receiver) {
        int increment = op.getName().equals("inc") ? 1 : -1;
        if (operand instanceof JetReferenceExpression) {
            final int index = indexOfLocal((JetReferenceExpression) operand);
            if (index >= 0 && JetTypeMapper.isIntPrimitive(asmType)) {
                v.iinc(index, increment);
                return;
            }
        }
        StackValue value = genQualified(receiver, operand);
        value.dupReceiver(v, 0);
        value.put(asmType, v);
        if (asmType == Type.LONG_TYPE) {
            v.aconst(Long.valueOf(increment));
        }
        else if (asmType == Type.FLOAT_TYPE) {
            v.aconst(Float.valueOf(increment));
        }
        else if (asmType == Type.DOUBLE_TYPE) {
            v.aconst(Double.valueOf(increment));
        }
        else {
            v.aconst(increment);
        }
        v.add(asmType);
        value.store(v);
    }

    @Override
    public StackValue visitProperty(JetProperty property, StackValue receiver) {
        VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
        int index = lookupLocal(variableDescriptor);

        assert index >= 0;

        JetExpression initializer = property.getInitializer();
        if (initializer != null) {
            Type type = typeMapper.mapType(variableDescriptor.getOutType());
            gen(initializer, type);
            v.store(index, type);
        }
        return StackValue.none();
    }

    private StackValue generateConstructorCall(JetCallExpression expression, JetSimpleNameExpression constructorReference) {
        DeclarationDescriptor constructorDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, constructorReference);
        final PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, constructorDescriptor);
        Type type;
        if (declaration instanceof PsiMethod) {
            type = generateJavaConstructorCall(expression, (PsiMethod) declaration);
        }
        else if (constructorDescriptor instanceof ConstructorDescriptor) {
            type = typeMapper.mapType(bindingContext.get(BindingContext.EXPRESSION_TYPE, expression), OwnerKind.IMPLEMENTATION);
            if (type.getSort() == Type.ARRAY) {
                generateNewArray(expression, type);
            }
            else {
                ClassDescriptor classDecl = (ClassDescriptor) constructorDescriptor.getContainingDeclaration();

                v.anew(type);
                v.dup();

                // TODO typechecker must verify that we're the outer class of the instance being created
                pushOuterClassArguments(classDecl);

                CallableMethod method = typeMapper.mapToCallableMethod((ConstructorDescriptor) constructorDescriptor, OwnerKind.IMPLEMENTATION);
                invokeMethodWithArguments(method, expression);
            }
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate this new expression");
        }
        return StackValue.onStack(type);
    }

    private void pushTypeArguments(JetCall expression) {
        for (JetTypeProjection jetTypeArgument : expression.getTypeArguments()) {
            pushTypeArgument(jetTypeArgument);
        }
    }

    public void pushTypeArgument(JetTypeProjection jetTypeArgument) {
        JetType typeArgument = bindingContext.get(BindingContext.TYPE, jetTypeArgument.getTypeReference());
        generateTypeInfo(typeArgument);
    }

    private void pushOuterClassArguments(ClassDescriptor classDecl) {
        if (classDecl.getContainingDeclaration() instanceof ClassDescriptor) {
            v.load(0, JetTypeMapper.jetImplementationType(classDecl));
        }
    }

    private Type generateJavaConstructorCall(JetCallExpression expression, PsiMethod constructor) {
        PsiClass javaClass = constructor.getContainingClass();
        Type type = JetTypeMapper.psiClassType(javaClass);
        v.anew(type);
        v.dup();
        final CallableMethod callableMethod = JetTypeMapper.mapToCallableMethod(constructor);
        invokeMethodWithArguments(callableMethod, expression);
        return type;
    }

    private void generateNewArray(JetCallExpression expression, Type type) {
        List<JetValueArgument> args = expression.getValueArguments();
        if (args.size() != 1) {
            throw new CompilationException("array constructor requires one value argument");
        }
        gen(args.get(0).getArgumentExpression(), Type.INT_TYPE);
        v.newarray(type.getElementType());
    }

    @Override
    public StackValue visitArrayAccessExpression(JetArrayAccessExpression expression, StackValue receiver) {
        final JetExpression array = expression.getArrayExpression();
        final Type arrayType = expressionType(array);
        gen(array, arrayType);
        generateArrayIndex(expression);
        if (arrayType.getSort() == Type.ARRAY) {
            final Type elementType = arrayType.getElementType();
            return StackValue.arrayElement(elementType);
        }
        else {
            final PsiElement declaration = BindingContextUtils.resolveToDeclarationPsiElement(bindingContext, expression);
            final CallableMethod accessor;
            if (declaration instanceof PsiMethod) {
                accessor = JetTypeMapper.mapToCallableMethod((PsiMethod) declaration);
            }
            else if (declaration instanceof JetNamedFunction) {
                accessor = typeMapper.mapToCallableMethod((JetNamedFunction) declaration);
            }
            else {
                throw new UnsupportedOperationException("unknown accessor type");
            }
            boolean isGetter = accessor.getSignature().getName().equals("get");
            return StackValue.collectionElement(JetTypeMapper.TYPE_OBJECT, isGetter ? accessor : null,
                                                isGetter ? null : accessor);
        }
    }

    private void generateArrayIndex(JetArrayAccessExpression expression) {
        final List<JetExpression> indices = expression.getIndexExpressions();
        for (JetExpression index : indices) {
            gen(index, Type.INT_TYPE);
        }
    }

    @Override
    public StackValue visitThrowExpression(JetThrowExpression expression, StackValue receiver) {
        gen(expression.getThrownExpression(), JetTypeMapper.TYPE_OBJECT);
        v.athrow();
        return StackValue.none();
    }

    @Override
    public StackValue visitThisExpression(JetThisExpression expression, StackValue receiver) {
        final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getThisReference());
        if (descriptor instanceof ClassDescriptor) {
            return generateThisOrOuter((ClassDescriptor) descriptor);
        }
        else {
            return thisExpression();
        }
    }

    public void thisToStack() {
        thisExpression().put(JetTypeMapper.TYPE_OBJECT, v);
    }

    @Override
    public StackValue visitTryExpression(JetTryExpression expression, StackValue receiver) {
        Label tryStart = new Label();
        v.mark(tryStart);
        gen(expression.getTryBlock(), Type.VOID_TYPE);
        Label tryEnd = new Label();
        v.mark(tryEnd);
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        if (finallyBlock != null) {
            gen(finallyBlock.getFinalExpression(), Type.VOID_TYPE);
        }
        Label end = new Label();
        v.goTo(end);         // TODO don't generate goto if there's no code following try/catch
        for (JetCatchClause clause : expression.getCatchClauses()) {
            Label clauseStart = new Label();
            v.mark(clauseStart);

            VariableDescriptor descriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, clause.getCatchParameter());
            Type descriptorType = typeMapper.mapType(descriptor.getOutType());
            myMap.enter(descriptor, 1);
            int index = lookupLocal(descriptor);
            v.store(index, descriptorType);

            gen(clause.getCatchBody(), Type.VOID_TYPE);
            v.goTo(end);     // TODO don't generate goto if there's no code following try/catch

            myMap.leave(descriptor);
            v.visitTryCatchBlock(tryStart, tryEnd, clauseStart, descriptorType.getInternalName());
        }
        if (finallyBlock != null) {
            Label finallyStart = new Label();
            v.mark(finallyStart);

            int index = myMap.enterTemp();
            v.store(index, THROWABLE_TYPE);

            gen(finallyBlock.getFinalExpression(), Type.VOID_TYPE);

            v.load(index, THROWABLE_TYPE);
            v.athrow();

            myMap.leaveTemp();

            v.visitTryCatchBlock(tryStart, tryEnd, finallyStart, null);
        }
        v.mark(end);

        return StackValue.none();
    }

    @Override
    public StackValue visitBinaryWithTypeRHSExpression(final JetBinaryExpressionWithTypeRHS expression, StackValue receiver) {
        JetSimpleNameExpression operationSign = expression.getOperationSign();
        IElementType opToken = operationSign.getReferencedNameElementType();
        if (opToken == JetTokens.COLON) {
            return gen(expression.getLeft());
        }
        else {
            JetTypeReference typeReference = expression.getRight();
            JetType jetType = bindingContext.get(BindingContext.TYPE, typeReference);
            DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
            if (!(descriptor instanceof ClassDescriptor)) {
                throw new UnsupportedOperationException("don't know how to handle non-class types in as/as?");
            }
            Type type = typeMapper.mapType(jetType, OwnerKind.INTERFACE);
            generateInstanceOf(StackValue.expression(OBJECT_TYPE, expression.getLeft(), this), jetType, true);
            Label isInstance = new Label();
            v.ifne(isInstance);
            v.pop();
            if (opToken == JetTokens.AS_SAFE) {
                v.aconst(null);
            }
            else {
                throwNewException(CLASS_TYPE_CAST_EXCEPTION);
            }
            v.mark(isInstance);
            v.checkcast(type);
            return StackValue.onStack(type);
        }
    }

    @Override
    public StackValue visitIsExpression(final JetIsExpression expression, StackValue receiver) {
        final StackValue match = StackValue.expression(OBJECT_TYPE, expression.getLeftHandSide(), this);
        return generatePatternMatch(expression.getPattern(), expression.isNegated(), match, null);
    }

    // on entering the function, expressionToMatch is already placed on stack, and we should consume it
    private StackValue generatePatternMatch(JetPattern pattern, boolean negated, StackValue expressionToMatch,
                                            @Nullable Label nextEntry) {
        if (pattern instanceof JetTypePattern) {
            JetTypeReference typeReference = ((JetTypePattern) pattern).getTypeReference();
            JetType jetType = bindingContext.get(BindingContext.TYPE, typeReference);
            expressionToMatch.dupReceiver(v, 0);
            generateInstanceOf(expressionToMatch, jetType, false);
            StackValue value = StackValue.onStack(Type.BOOLEAN_TYPE);
            return negated ? StackValue.not(value) : value;
        }
        else if (pattern instanceof JetTuplePattern) {
            return generateTuplePatternMatch((JetTuplePattern) pattern, negated, expressionToMatch, nextEntry);
        }
        else if (pattern instanceof JetExpressionPattern) {
            final Type subjectType = expressionToMatch.type;
            expressionToMatch.dupReceiver(v, 0);
            expressionToMatch.put(subjectType, v);
            JetExpression condExpression = ((JetExpressionPattern) pattern).getExpression();
            Type condType = isNumberPrimitive(subjectType) ? expressionType(condExpression) : OBJECT_TYPE;
            gen(condExpression, condType);
            return generateEqualsForExpressionsOnStack(JetTokens.EQEQ, subjectType, condType);
        }
        else if (pattern instanceof JetWildcardPattern) {
            return StackValue.constant(!negated, Type.BOOLEAN_TYPE);
        }
        else if (pattern instanceof JetBindingPattern) {
            final JetProperty var = ((JetBindingPattern) pattern).getVariableDeclaration();
            final VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, var);
            final Type varType = typeMapper.mapType(variableDescriptor.getOutType());
            myMap.enter(variableDescriptor, varType.getSize());
            expressionToMatch.dupReceiver(v, 0);
            expressionToMatch.put(varType, v);
            final int varIndex = myMap.getIndex(variableDescriptor);
            v.store(varIndex, varType);
            return generateWhenCondition(varType, varIndex, ((JetBindingPattern) pattern).getCondition(), null);
        }
        else {
            throw new UnsupportedOperationException("Unsupported pattern type: " + pattern);
        }
    }

    private StackValue generateTuplePatternMatch(JetTuplePattern pattern, boolean negated, StackValue expressionToMatch,
                                                 @Nullable Label nextEntry) {
        final List<JetTuplePatternEntry> entries = pattern.getEntries();

        Label lblFail = new Label();
        Label lblDone = new Label();
        expressionToMatch.dupReceiver(v, 0);
        expressionToMatch.put(OBJECT_TYPE, v);
        v.dup();
        final String tupleClassName = "jet/Tuple" + entries.size();
        Type tupleType = Type.getObjectType(tupleClassName);
        v.instanceOf(tupleType);
        Label lblCheck = new Label();
        v.ifne(lblCheck);
        Label lblPopAndFail = new Label();
        v.mark(lblPopAndFail);
        v.pop();
        v.goTo(lblFail);

        v.mark(lblCheck);
        for (int i = 0; i < entries.size(); i++) {
            final boolean isLast = i == entries.size() - 1;
            final StackValue tupleField = StackValue.field(OBJECT_TYPE, tupleClassName, "_" + (i + 1), false);
            final StackValue stackValue = generatePatternMatch(entries.get(i).getPattern(), false, tupleField, nextEntry);
            stackValue.condJump(lblPopAndFail, true, v);
        }

        v.pop();  // delete extra copy of expressionToMatch
        if (negated && nextEntry != null) {
            v.goTo(nextEntry);
        }
        else {
            v.aconst(!negated);
        }
        v.goTo(lblDone);
        v.mark(lblFail);
        if (!negated && nextEntry != null) {
            v.goTo(nextEntry);
        }
        else {
            v.aconst(negated);
        }
        v.mark(lblDone);
        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private void generateInstanceOf(StackValue expressionToGen, JetType jetType, boolean leaveExpressionOnStack) {
        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (jetType.getArguments().size() > 0 || !(descriptor instanceof ClassDescriptor)) {
            generateTypeInfo(jetType);
            expressionToGen.put(OBJECT_TYPE, v);
            if (leaveExpressionOnStack) {
                v.dupX1();
            }
            v.invokevirtual("jet/typeinfo/TypeInfo", "isInstance", "(Ljava/lang/Object;)Z");
        }
        else {
            expressionToGen.put(OBJECT_TYPE, v);
            if (leaveExpressionOnStack) {
                v.dup();
            }
            Type type = typeMapper.mapType(jetType, OwnerKind.INTERFACE);
            v.instanceOf(type);
        }
    }

    private void generateTypeInfo(JetType jetType) {
        DeclarationDescriptor declarationDescriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof TypeParameterDescriptor) {
            loadTypeParameterTypeInfo((TypeParameterDescriptor) declarationDescriptor);
            return;
        }

        final Type jvmType = typeMapper.mapType(jetType, OwnerKind.INTERFACE);
        if (jvmType.getSort() <= Type.DOUBLE) {
            v.getstatic("jet/typeinfo/TypeInfo", PRIMITIVE_TYPE_INFO_FIELDS[jvmType.getSort()], "Ljet/typeinfo/TypeInfo;");
            return;
        }

        v.anew(JetTypeMapper.TYPE_TYPEINFO);
        v.dup();
        v.aconst(jvmType);
        v.aconst(jetType.isNullable());
        List<TypeProjection> arguments = jetType.getArguments();
        if (arguments.size() > 0) {
            v.iconst(arguments.size());
            v.newarray(JetTypeMapper.TYPE_TYPEINFO);

            for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
                TypeProjection argument = arguments.get(i);
                v.dup();
                v.iconst(i);
                generateTypeInfo(argument.getType());
                v.astore(JetTypeMapper.TYPE_OBJECT);
            }
            v.invokespecial("jet/typeinfo/TypeInfo", "<init>", "(Ljava/lang/Class;Z[Ljet/typeinfo/TypeInfo;)V");
        }
        else {
            v.invokespecial("jet/typeinfo/TypeInfo", "<init>", "(Ljava/lang/Class;Z)V");
        }
    }

    private void loadTypeParameterTypeInfo(TypeParameterDescriptor typeParameterDescriptor) {
        final StackValue value = typeParameterExpressions.get(typeParameterDescriptor);
        if (value != null) {
            value.put(JetTypeMapper.TYPE_TYPEINFO, v);
            return;
        }
        DeclarationDescriptor containingDeclaration = typeParameterDescriptor.getContainingDeclaration();
        if (containingDeclaration == contextType() && contextType() instanceof ClassDescriptor) {
            loadTypeInfo(typeMapper, (ClassDescriptor) contextType(), v);
            v.iconst(typeParameterDescriptor.getIndex());
            v.invokevirtual("jet/typeinfo/TypeInfo", "getTypeParameter", "(I)Ljet/typeinfo/TypeInfo;");
            return;
        }
        throw new UnsupportedOperationException("don't know what this type parameter resolves to");
    }

    @Override
    public StackValue visitWhenExpression(JetWhenExpression expression, StackValue receiver) {
        JetExpression expr = expression.getSubjectExpression();
        final Type subjectType = expressionType(expr);
        final int subjectLocal = myMap.enterTemp(subjectType.getSize());
        gen(expr, subjectType);
        v.store(subjectLocal, subjectType);

        Label end = new Label();
        Label nextCondition = null;
        boolean hasElse = false;
        for (JetWhenEntry whenEntry : expression.getEntries()) {
            if (nextCondition != null) {
                v.mark(nextCondition);
            }
            nextCondition = new Label();
            FrameMap.Mark mark = myMap.mark();
            Label thisEntry = new Label();
            if (!whenEntry.isElse()) {
                final JetWhenCondition[] conditions = whenEntry.getConditions();
                for (int i = 0; i < conditions.length; i++) {
                    StackValue conditionValue = generateWhenCondition(subjectType, subjectLocal, conditions[i], nextCondition);
                    conditionValue.condJump(nextCondition, true, v);
                    if (i < conditions.length - 1) {
                        v.goTo(thisEntry);
                        v.mark(nextCondition);
                        nextCondition = new Label();
                    }
                }
            }
            else {
                hasElse = true;
            }
            v.visitLabel(thisEntry);
            genToJVMStack(whenEntry.getExpression());
            mark.dropTo();
            v.goTo(end);
        }
        if (!hasElse && nextCondition != null) {
            v.mark(nextCondition);
            throwNewException(CLASS_NO_PATTERN_MATCHED_EXCEPTION);
        }
        v.mark(end);

        myMap.leaveTemp(subjectType.getSize());
        return StackValue.onStack(expressionType(expression));
    }

    private StackValue generateWhenCondition(Type subjectType, int subjectLocal, JetWhenCondition condition,
                                             @Nullable Label nextEntry) {
        StackValue conditionValue;
        if (condition instanceof JetWhenConditionInRange) {
            JetExpression range = ((JetWhenConditionInRange) condition).getRangeExpression();
            gen(range, RANGE_TYPE);
            new StackValue.Local(subjectLocal, subjectType).put(INTEGER_TYPE, v);
            v.invokeinterface(CLASS_RANGE, "contains", "(Ljava/lang/Comparable;)Z");
            conditionValue = new StackValue.OnStack(Type.BOOLEAN_TYPE);
        }
        else if (condition instanceof JetWhenConditionIsPattern) {
            JetWhenConditionIsPattern patternCondition = (JetWhenConditionIsPattern) condition;
            JetPattern pattern = patternCondition.getPattern();
            conditionValue = generatePatternMatch(pattern, patternCondition.isNegated(),
                                                  StackValue.local(subjectLocal, subjectType), nextEntry);
        }
        else if (condition instanceof JetWhenConditionCall) {
            final JetExpression call = ((JetWhenConditionCall) condition).getCallSuffixExpression();
            if (call instanceof JetCallExpression) {
                v.load(subjectLocal, subjectType);
                final DeclarationDescriptor declarationDescriptor = resolveCalleeDescriptor((JetCallExpression) call);
                if (!(declarationDescriptor instanceof FunctionDescriptor)) {
                    throw new UnsupportedOperationException("expected function descriptor in when condition with call, found " + declarationDescriptor);
                }
                conditionValue = invokeFunction((JetCallExpression) call, declarationDescriptor, StackValue.none());
            }
            else if (call instanceof JetSimpleNameExpression) {
                final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) call);
                if (descriptor instanceof PropertyDescriptor) {
                    v.load(subjectLocal, subjectType);
                    conditionValue = intermediateValueForProperty((PropertyDescriptor) descriptor, false, false);
                }
                else {
                    throw new UnsupportedOperationException("unknown simple name resolve result: " + descriptor);
                }
            }
            else {
                throw new UnsupportedOperationException("unsupported kind of call suffix");
            }
        }
        else {
            throw new UnsupportedOperationException("unsupported kind of when condition");
        }
        return conditionValue;
    }

    @Override
    public StackValue visitTupleExpression(JetTupleExpression expression, StackValue receiver) {
        final List<JetExpression> entries = expression.getEntries();
        if (entries.size() > 22) {
            throw new UnsupportedOperationException("tuple too large");
        }
        final String className = "jet/Tuple" + entries.size();
        Type tupleType = Type.getObjectType(className);
        StringBuilder signature = new StringBuilder("(");
        for (JetExpression entry : entries) {
            signature.append("Ljava/lang/Object;");
        }
        signature.append(")V");

        v.anew(tupleType);
        v.dup();
        for (JetExpression entry : entries) {
            gen(entry, OBJECT_TYPE);
        }
        v.invokespecial(className, "<init>", signature.toString());
        return StackValue.onStack(tupleType);
    }

    private void throwNewException(final String className) {
        v.anew(Type.getObjectType(className));
        v.dup();
        v.invokespecial(className, "<init>", "()V");
        v.athrow();
    }

    private static class CompilationException extends RuntimeException {
        private CompilationException() {
        }

        private CompilationException(String message) {
            super(message);
        }
    }
}
