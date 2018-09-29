/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtext;

import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.AbstractMetamodelDeclaration;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Alternatives;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.EnumLiteralDeclaration;
import org.eclipse.xtext.EnumRule;
import org.eclipse.xtext.GeneratedMetamodel;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.Group;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.TerminalRule;
import org.eclipse.xtext.XtextPackage;
import org.eclipse.xtext.util.XtextSwitch;
import org.eclipse.xtext.validator.AbstractDeclarativeValidator;
import org.eclipse.xtext.validator.Check;
import org.eclipse.xtext.validator.CheckType;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class XtextValidator extends AbstractDeclarativeValidator {

	@Override
	protected List<? extends EPackage> getEPackages() {
		return Collections.singletonList(XtextPackage.eINSTANCE);
	}

	@Check(CheckType.FAST)
	public void checkGrammarUsesMaxOneOther(Grammar grammar) {
		assertTrue("You may not use more than one other grammar.", XtextPackage.GRAMMAR__USED_GRAMMARS,
				grammar.getUsedGrammars().size() <= 1);
	}

	@Check
	public void checkGrammarName(Grammar g) {
		String[] split = g.getName().split("\\.");
		if (split.length==1)
			warning("You should use a namespace.", XtextPackage.GRAMMAR__NAME);
		for (int i=0;i<split.length-1;i++) {
			String nsEle = split[i];
			if (Character.isUpperCase(nsEle.charAt(0)))
				warning("Namespace elements should start with a lower case letter.", XtextPackage.GRAMMAR__NAME);
		}
		String ele = split[split.length-1];
		if (!Character.isUpperCase(ele.charAt(0)))
			warning("The last element should start with an upper case letter.", XtextPackage.GRAMMAR__NAME);
	}

	@Check
	public void checkGeneratedMetamodel(GeneratedMetamodel metamodel) {
		if (metamodel.getName() != null && metamodel.getName().length() != 0)
			if (Character.isUpperCase(metamodel.getName().charAt(0)))
				warning("Metamodel names should start with a lower case letter.", XtextPackage.GENERATED_METAMODEL__NAME);
	}

	@Check
	public void checkMetamodelUris(final AbstractMetamodelDeclaration declaration) {
		guard(declaration.getEPackage().getNsURI() != null);

		Grammar grammar = GrammarUtil.getGrammar(declaration);
		Iterable<String> nsUris = Iterables.transform(grammar.getMetamodelDeclarations(), new Function<AbstractMetamodelDeclaration, String>() {
			public String apply(AbstractMetamodelDeclaration param) {
				if (param.getEPackage() != null)
					return param.getEPackage().getNsURI();
				return null;
			}
		});
		int count = Iterables.size(Iterables.filter(nsUris, new Predicate<String>() {
			public boolean apply(String param) {
				return declaration.getEPackage().getNsURI().equals(param);
			}
		}));
		assertTrue("EPackage with ns-uri '"+ declaration.getEPackage().getNsURI() + "' is used twice.",
			XtextPackage.ABSTRACT_METAMODEL_DECLARATION__EPACKAGE, count == 1);
	}

	@Check
	public void checkCrossReferenceTerminal(CrossReference reference) {
		if (reference.getTerminal() != null && !(reference.getTerminal() instanceof RuleCall))
			warning("Your grammar will not work with the default linking implementation, " +
					"because Alternatives are currently not handled properly in CrossReferences.",
					XtextPackage.CROSS_REFERENCE__TERMINAL);
	}

	@Check
	public void checkRuleName(AbstractRule rule) {
		final Grammar grammar = GrammarUtil.getGrammar(rule);
		final TreeSet<String> foundNames = new TreeSet<String>();
		for(AbstractRule otherRule: GrammarUtil.allRules(grammar)) {
			if (rule.getName().equalsIgnoreCase(otherRule.getName()) && rule != otherRule) {
				foundNames.add(otherRule.getName());
			}
		}
		if (!foundNames.isEmpty()) {
			final String message = "Rulename has to be unique even case insensitive.";
			if (foundNames.size() == 1)
				error(message + "\nOther rule was: " + foundNames.first(), XtextPackage.ABSTRACT_RULE__NAME);
			else {
				final StringBuilder builder = new StringBuilder((rule.getName().length() + 2) * foundNames.size() - 2);
				for(String name: foundNames) {
					if (builder.length() != 0)
						builder.append(", ");
					builder.append(name);
				}
				error(message + "\nOther rules were: " + builder + ".", XtextPackage.ABSTRACT_RULE__NAME);
			}
		}
	}

	@Check
	public void checkUnassignedActionAfterAssignment(final Action action) {
		if (action.getFeature() == null) {
			checkCurrentMustBeUnassigned(action);
		}
	}

	@Check
	public void checkUnassignedRuleCallAllowed(final RuleCall call) {
		if (call.getRule() != null && GrammarUtil.containingAssignment(call) == null) {
			AbstractRule container = EcoreUtil2.getContainerOfType(call, AbstractRule.class);
			if (call.getRule() instanceof ParserRule) {
				if (container instanceof TerminalRule) {
					error("Cannot call parser rule from terminal rule.", null);
				} else if (!GrammarUtil.isDatatypeRule((ParserRule) call.getRule()))
					checkCurrentMustBeUnassigned(call);
			}
		}
	}

	private void checkCurrentMustBeUnassigned(final AbstractElement element) {
		ParserRule rule = GrammarUtil.containingParserRule(element);
		if (GrammarUtil.isDatatypeRule(rule))
			return;
		XtextSwitch<Boolean> visitor = new XtextSwitch<Boolean>() {
			private boolean isNull = true;

			@Override
			public Boolean caseAbstractElement(AbstractElement object) {
				return isNull;
			}

			@Override
			public Boolean caseAlternatives(Alternatives object) {
				final boolean wasIsNull = isNull;
				boolean localIsNull = wasIsNull;
				for(AbstractElement element: object.getGroups()) {
					isNull = wasIsNull;
					localIsNull &= doSwitch(element);
				}
				isNull = localIsNull;
				return isNull;
			}

			@Override
			public Boolean caseAssignment(Assignment object) {
				isNull = false;
				return isNull;
			}

			@Override
			public Boolean caseGroup(Group object) {
				for(AbstractElement element: object.getTokens())
					doSwitch(element);
				return isNull;
			}

			@Override
			public Boolean caseAction(Action object) {
				if (object == element) {
					assertTrue("An unassigned action is not allowed, when the 'current' was already created.",
							null, isNull && !isMany(object));
					checkDone();
				}
				isNull = false;
				return isNull;
			}

			@Override
			public Boolean caseRuleCall(RuleCall object) {
				if (object == element) {
					assertTrue("An unassigned rule call is not allowed, when the 'current' was already created.", null, isNull && !isMany(object));
					checkDone();
				}
				return doSwitch(object.getRule());
			}

			@Override
			public Boolean caseParserRule(ParserRule object) {
				isNull = false;
				return isNull;
			}

			@Override
			public Boolean caseTerminalRule(TerminalRule object) {
				isNull = false;
				return isNull;
			}

			public boolean isMany(AbstractElement element) {
				return GrammarUtil.isMultipleCardinality(element) ||
					((element.eContainer() instanceof AbstractElement) && isMany((AbstractElement) element.eContainer()));
			}

		};
		visitor.doSwitch(rule.getAlternatives());
	}

	@Check
	public void checkAssignedActionAfterAssignment(final Action action) {
		if (action.getFeature() != null) {
			ParserRule rule = GrammarUtil.containingParserRule(action);
			XtextSwitch<Boolean> visitor = new XtextSwitch<Boolean>() {
				private boolean assignedActionAllowed = false;

				@Override
				public Boolean caseAbstractElement(AbstractElement object) {
					return assignedActionAllowed;
				}

				@Override
				public Boolean caseAlternatives(Alternatives object) {
					boolean wasActionAllowed = assignedActionAllowed;
					boolean localActionAllowed = true;
					for(AbstractElement element: object.getGroups()) {
						assignedActionAllowed = wasActionAllowed;
						localActionAllowed &= doSwitch(element);
					}
					assignedActionAllowed = wasActionAllowed || (localActionAllowed && !GrammarUtil.isOptionalCardinality(object));
					return assignedActionAllowed;
				}

				@Override
				public Boolean caseAssignment(Assignment object) {
					assignedActionAllowed = assignedActionAllowed || !GrammarUtil.isOptionalCardinality(object);
					return assignedActionAllowed;
				}

				@Override
				public Boolean caseGroup(Group object) {
					boolean wasAssignedActionAllowed = assignedActionAllowed;
					for(AbstractElement element: object.getTokens())
						doSwitch(element);
					assignedActionAllowed = wasAssignedActionAllowed || (assignedActionAllowed && !GrammarUtil.isOptionalCardinality(object));
					return assignedActionAllowed;
				}

				@Override
				public Boolean caseAction(Action object) {
					if (object == action) {
						assertTrue("An action is not allowed, when the current may still be unassigned.",
								null, assignedActionAllowed);
						checkDone();
					}
					assignedActionAllowed = true;
					return assignedActionAllowed;
				}

				@Override
				public Boolean caseRuleCall(RuleCall object) {
					assignedActionAllowed = assignedActionAllowed || doSwitch(object.getRule()) && !GrammarUtil.isOptionalCardinality(object);
					return assignedActionAllowed;
				}

				@Override
				public Boolean caseParserRule(ParserRule object) {
					assignedActionAllowed = !GrammarUtil.isDatatypeRule(object);
					return assignedActionAllowed;
				}

				@Override
				public Boolean caseTerminalRule(TerminalRule object) {
					return assignedActionAllowed;
				}

			};
			visitor.doSwitch(rule.getAlternatives());
		}
	}

	@Check
	public void checkEnumLiteralIsUnique(EnumLiteralDeclaration decl) {
		EnumRule rule = GrammarUtil.containingEnumRule(decl);
		List<EnumLiteralDeclaration> declarations = EcoreUtil2.getAllContentsOfType(rule, EnumLiteralDeclaration.class);
		String literal = decl.getLiteral().getValue();
		for(EnumLiteralDeclaration otherDecl: declarations) {
			if (otherDecl != decl && literal.equals(otherDecl.getLiteral().getValue())) {
				error("Enum literal '" + literal + "' is used multiple times in enum rule '" + rule.getName() + "'.",
						XtextPackage.ENUM_LITERAL_DECLARATION__LITERAL);
			}
		}
	}
	
	@Check
	public void checkGeneratedEnumIsValid(EnumLiteralDeclaration decl) {
		EnumRule rule = GrammarUtil.containingEnumRule(decl);
		guard(rule.getType().getMetamodel() instanceof GeneratedMetamodel);
		List<EnumLiteralDeclaration> declarations = EcoreUtil2.getAllContentsOfType(rule, EnumLiteralDeclaration.class);
		EEnum eEnum = (EEnum) rule.getType().getClassifier();
		guard(declarations.size() != eEnum.getELiterals().size());
		for(EnumLiteralDeclaration otherDecl: declarations) {
			if (decl == otherDecl) {
				return;
			}
			if (otherDecl.getEnumLiteral() == decl.getEnumLiteral()) {
				if (!decl.getEnumLiteral().getLiteral().equals(decl.getLiteral().getValue()))
					warning("Enum literal '" + decl.getEnumLiteral().getName() +
							"' has already been defined with literal '" + decl.getEnumLiteral().getLiteral() + "'.",
							XtextPackage.ENUM_LITERAL_DECLARATION__ENUM_LITERAL);
				return;
			}
		}
	}
	
	@Check
	public void checkEnumLiteralIsValid(EnumLiteralDeclaration decl) {
		if("".equals(decl.getLiteral().getValue()))
			error("Enum literal must not be an empty string.", XtextPackage.ENUM_LITERAL_DECLARATION__LITERAL);
	}

}
