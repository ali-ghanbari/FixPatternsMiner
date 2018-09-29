/*
 * Copyright (c) 2006, 2008 Borland Software Corporation
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: dvorak - initial API and implementation
 */
package org.eclipse.gmf.tests.migration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.gmf.codegen.gmfgen.FeatureLabelModelFacet;
import org.eclipse.gmf.codegen.gmfgen.GenAuditContainer;
import org.eclipse.gmf.codegen.gmfgen.GenAuditContext;
import org.eclipse.gmf.codegen.gmfgen.GenAuditRoot;
import org.eclipse.gmf.codegen.gmfgen.GenAuditRule;
import org.eclipse.gmf.codegen.gmfgen.GenDiagram;
import org.eclipse.gmf.codegen.gmfgen.GenEditorGenerator;
import org.eclipse.gmf.codegen.gmfgen.GenPlugin;
import org.eclipse.gmf.codegen.gmfgen.GenTopLevelNode;
import org.eclipse.gmf.codegen.gmfgen.LabelModelFacet;
import org.eclipse.gmf.gmfgraph.Canvas;
import org.eclipse.gmf.gmfgraph.Compartment;
import org.eclipse.gmf.gmfgraph.Connection;
import org.eclipse.gmf.gmfgraph.CustomFigure;
import org.eclipse.gmf.gmfgraph.DiagramLabel;
import org.eclipse.gmf.gmfgraph.Figure;
import org.eclipse.gmf.gmfgraph.FigureDescriptor;
import org.eclipse.gmf.gmfgraph.FigureGallery;
import org.eclipse.gmf.gmfgraph.FlowLayout;
import org.eclipse.gmf.gmfgraph.GMFGraphPackage;
import org.eclipse.gmf.gmfgraph.Label;
import org.eclipse.gmf.gmfgraph.LabeledContainer;
import org.eclipse.gmf.gmfgraph.LineKind;
import org.eclipse.gmf.gmfgraph.Node;
import org.eclipse.gmf.gmfgraph.PolylineConnection;
import org.eclipse.gmf.gmfgraph.PolylineDecoration;
import org.eclipse.gmf.gmfgraph.RealFigure;
import org.eclipse.gmf.gmfgraph.Rectangle;
import org.eclipse.gmf.internal.common.ToolingResourceFactory;
import org.eclipse.gmf.internal.common.migrate.MigrationResource;
import org.eclipse.gmf.internal.common.migrate.ModelLoadHelper;
import org.eclipse.gmf.mappings.CanvasMapping;
import org.eclipse.gmf.mappings.ElementInitializer;
import org.eclipse.gmf.mappings.FeatureInitializer;
import org.eclipse.gmf.mappings.FeatureLabelMapping;
import org.eclipse.gmf.mappings.FeatureSeqInitializer;
import org.eclipse.gmf.mappings.FeatureValueSpec;
import org.eclipse.gmf.mappings.LabelMapping;
import org.eclipse.gmf.mappings.Language;
import org.eclipse.gmf.mappings.Mapping;
import org.eclipse.gmf.mappings.MappingEntry;
import org.eclipse.gmf.mappings.NodeMapping;
import org.eclipse.gmf.mappings.ReferenceNewElementSpec;
import org.eclipse.gmf.mappings.TopNodeReference;
import org.eclipse.gmf.mappings.ValueExpression;
import org.eclipse.gmf.tests.Plugin;

public class MigrationPatchesTest extends TestCase {

	public MigrationPatchesTest(String name) {
		super(name);
	}

	/*
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=138440
	 */
	public void testPatch_138440() throws Exception {
		URI genmodelFileName = createURI("patch_138440.gmfgen"); //$NON-NLS-1$
		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
		assertTrue("expected IllegalArgumentException from metamodel EFactory", caughtGenException instanceof IllegalArgumentException); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(genmodelFileName);

		URI newGenUri = temporarySaveMigratedModel(genmodelFileName, "patch_138440", "gmfgen");
		// since we now migrate old (2005) models to build dynamic 2006 model, and saved migrated model
		// is newest, 2008 one, this approach of reading it with older nsURI becomes confusing
		// changeNsUriToOldOne(newGenUri, "gmfgen", "http://www.eclipse.org/gmf/2005/GenModel/2.0");
		
		// this model should be loaded only with latest-model-aware resource, not the one that is
		// registered for 2005 nsURI, because 2006 and 2008 models are not compatible
		assertNoOrdinaryLoadModelProblems(newGenUri);
		
		URI gmfmapmodelFileName = createURI("patch_138440.gmfmap"); //$NON-NLS-1$
		Exception caughtMapException = assertOrdinaryLoadModelProblems(gmfmapmodelFileName);
		assertTrue("expected IllegalArgumentException from metamodel EFactory", caughtMapException instanceof IllegalArgumentException); //$NON-NLS-1$

		assertOnLoadModelMigrationSuccess(gmfmapmodelFileName);

		URI newMapUri = temporarySaveMigratedModel(gmfmapmodelFileName, "patch_138440", "gmfmap");
		changeNsUriToOldOne(newMapUri, "gmfmap", "http://www.eclipse.org/gmf/2005/mappings/2.0");
		
		assertOnLoadModelMigrationDidNothing(newMapUri);
	}

	/*
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=161380
	 */
	public void testPatch_161380() throws Exception {
		URI genmodelFileName = createURI("patch_161380.gmfgen"); //$NON-NLS-1$
		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(genmodelFileName);

		URI newGenUri = temporarySaveMigratedModel(genmodelFileName, "patch_138440", "gmfgen");
		changeNsUriToOldOne(newGenUri, "gmfgen", "http://www.eclipse.org/gmf/2005/GenModel/2.0");
		
		assertOnLoadModelMigrationDidNothing(newGenUri);
		
		URI gmfmapmodelFileName = createURI("patch_161380.gmfmap"); //$NON-NLS-1$		
		Exception caughtMapException = assertOrdinaryLoadModelProblems(gmfmapmodelFileName);
		assertTrue("expected diagnostic exception", caughtMapException != null); //$NON-NLS-1$

		assertOnLoadModelMigrationSuccess(gmfmapmodelFileName);

		URI newUri = temporarySaveMigratedModel(gmfmapmodelFileName, "patch_161380", "gmfmap");
		changeNsUriToOldOne(newUri, "gmfmap", "http://www.eclipse.org/gmf/2005/mappings/2.0");
		
		assertOnLoadModelMigrationDidNothing(newUri);
	}

	static URI createURI(String testModelFileName) {
		try {
			return Plugin.createURI("/models/migration/" + testModelFileName); //$NON-NLS-1$
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not create test model URI"); //$NON-NLS-1$
		}
		return null;
	}

	void assertOnLoadModelMigrationSuccess(URI uri) throws Exception {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), uri);
		
		EList<Resource.Diagnostic> errors = loadHelper.getLoadedResource().getErrors();
		assertTrue("Errors found after migration: "+errors, errors.isEmpty()); //$NON-NLS-1$
		
		assertTrue("Migration warning load status expected", loadHelper.getStatus().matches(IStatus.WARNING)); //$NON-NLS-1$
		Collection<Resource.Diagnostic> warnings = new ArrayList<Resource.Diagnostic>();
		for (Resource nextResource : loadHelper.getLoadedResource().getResourceSet().getResources()) {
			warnings.addAll(nextResource.getWarnings());
		}
		for (Resource.Diagnostic warning : warnings) {
			assertTrue("Migration Warning diagnostic expected", warning instanceof MigrationResource.Diagnostic); //$NON-NLS-1$
		}
		
		assertTrue(loadHelper.getLoadedResource() instanceof XMLResource);
		XMLResource xmlResource = (XMLResource) loadHelper.getLoadedResource();
		assertEquals("Unknown elements were found after migration", 0, xmlResource.getEObjectToExtensionMap().size());
	}

	void assertOnLoadModelMigrationDidNothing(URI uri) throws Exception {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), uri);
		
		EList<Resource.Diagnostic> errors = loadHelper.getLoadedResource().getErrors();
		assertTrue("Errors after re-run migration on new migrated model: "+errors, errors.isEmpty());
		
		EList<Resource.Diagnostic> warnings = loadHelper.getLoadedResource().getWarnings();
		assertTrue("Warnings after re-run migration on new migrated model: "+warnings, warnings.isEmpty());
		
		assertTrue(loadHelper.getLoadedResource() instanceof XMLResource);
		XMLResource xmlResource = (XMLResource) loadHelper.getLoadedResource();
		assertEquals("Unknown elements were found after re-migration", 0, xmlResource.getEObjectToExtensionMap().size());
	}

	Exception assertOrdinaryLoadModelProblems(URI uri) throws Exception {
		Resource resource = new ToolingResourceFactory().createResource(uri);
		ResourceSet rset = new ResourceSetImpl();
		rset.getResources().add(resource);

		RuntimeException caughtException = null;
		try {
			rset.getResource(uri, true);
		} catch (RuntimeException e) {
			caughtException = e;
		}
		assertTrue("Expected model loading problems", //$NON-NLS-1$
				caughtException != null || !resource.getErrors().isEmpty() || !resource.getWarnings().isEmpty());
		return caughtException;
	}

	Resource assertNoOrdinaryLoadModelProblems(URI uri) throws Exception {
		Resource resource = new ToolingResourceFactory().createResource(uri);
		ResourceSet rset = new ResourceSetImpl();
		rset.getResources().add(resource);

		RuntimeException caughtException = null;
		try {
			rset.getResource(uri, true);
		} catch (RuntimeException e) {
			caughtException = e;
		}
		assertFalse("Unexpected model loading problems", //$NON-NLS-1$
				caughtException != null || !resource.getErrors().isEmpty() || !resource.getWarnings().isEmpty());
		return resource;
	}

	/*
	GenDiagram
	Removed attrs:
	attr String paletteProviderClassName;
	attr ProviderPriority paletteProviderPriority;
	attr String propertyProviderClassName;
	attr ProviderPriority propertyProviderPriority;
	attr String referenceConnectionEditPolicyClassName;
	attr String externalNodeLabelHostLayoutEditPolicyClassName;
	attr String diagramFileCreatorClassName;
	attr String preferenceInitializerClassName;
	 */
	public void testGenDiagram() throws Exception {
		URI genmodelFileName = createURI("testGenDiagram.gmfgen"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(genmodelFileName);

		URI newUri = temporarySaveMigratedModel(genmodelFileName, "testGenDiagram", "gmfgen");
		changeNsUriToOldOne(newUri, "gmfgen", "http://www.eclipse.org/gmf/2005/GenModel/2.0");
		
		assertOnLoadModelMigrationDidNothing(newUri);
	}

	/*
	FeatureLabelModelFacet 
	Removed refs:
	ref genmodel.GenFeature[1] metaFeature;
	 */
	public void testFeatureLabelModelFacet() throws Exception {
		URI genmodelFileName = createURI("testFeatureLabelModelFacet.gmfgen"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(genmodelFileName);
		checkFeatureLabelModelFacetsMigrated(genmodelFileName);

		URI newUri = temporarySaveMigratedModel(genmodelFileName, "testFeatureLabelModelFacet", "gmfgen");
		changeNsUriToOldOne(newUri, "gmfgen", "http://www.eclipse.org/gmf/2005/GenModel/2.0");
		
		assertOnLoadModelMigrationDidNothing(newUri);
		checkFeatureLabelModelFacetsMigrated(newUri);
	}

//	/*
//	TypeLinkModelFacet 
//	Removed attrs:
//	attr String createCommandClassName;
//	 */
//	public void testTypeLinkModelFacet() throws Exception {
//		String genmodelFileName = "testTypeLinkModelFacet.gmfgen"; //$NON-NLS-1$
//		
//		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
//		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				
//
//		assertOnLoadModelMigrationSuccess(genmodelFileName);
//	}

	public void testGenAuditRootDefaultAndNested() throws Exception {
		URI genmodelFileName = createURI("testGenAuditRootDefaultAndNested.gmfgen"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(genmodelFileName);

		URI newUri = temporarySaveMigratedModel(genmodelFileName, "testGenAuditRootDefaultAndNested", "gmfgen");
		changeNsUriToOldOne(newUri, "gmfgen", "http://www.eclipse.org/gmf/2005/GenModel/2.0");
		
		assertOnLoadModelMigrationDidNothing(newUri);
	}

	public void testGenAuditRootNoDefaultButNested() throws Exception {
		URI genmodelFileName = createURI("testGenAuditRootNoDefaultButNested.gmfgen"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(genmodelFileName);
	}

	public void testGenAudits() throws Exception {
		URI genmodelFileName = createURI("testGenAudits.gmfgen"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(genmodelFileName);

		URI newUri = temporarySaveMigratedModel(genmodelFileName, "testGenAudits", "gmfgen");
		changeNsUriToOldOne(newUri, "gmfgen", "http://www.eclipse.org/gmf/2005/GenModel/2.0");
		
		assertOnLoadModelMigrationDidNothing(newUri);
	}

	public void testGenEditorAuditRootNoDefaultButNested() throws Exception {
		URI genmodelFileName = createURI("testGenEditorAuditRootNoDefaultButNested.gmfgen"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(genmodelFileName);

		URI newUri = temporarySaveMigratedModel(genmodelFileName, "testGenEditorAuditRootNoDefaultButNested", "gmfgen");
		changeNsUriToOldOne(newUri, "gmfgen", "http://www.eclipse.org/gmf/2005/GenModel/2.0");
		
		assertOnLoadModelMigrationDidNothing(newUri);
	}

	public void testGenAuditsCorrectCategories() throws Exception {
		URI genmodelFileName = createURI("testGenAuditsCorrectCategories.gmfgen"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(genmodelFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(genmodelFileName);
		
		checkModelAndCorrectCategories(genmodelFileName);

		URI newUri = temporarySaveMigratedModel(genmodelFileName, "testGenAuditsCorrectCategories", "gmfgen");
		changeNsUriToOldOne(newUri, "gmfgen", "http://www.eclipse.org/gmf/2005/GenModel/2.0");
		
		assertOnLoadModelMigrationDidNothing(newUri);
		
		checkModelAndCorrectCategories(newUri);
	}

	private URI temporarySaveMigratedModel(URI uri, String tempFilename, String tempFileExtension) throws IOException {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), uri);
		Resource resource = loadHelper.getLoadedResource();
		ResourceSet resourceSet = resource.getResourceSet();
		URI newUri = null;
		for (Resource nextResource : resourceSet.getResources()) {
			File newGenmodelFile = File.createTempFile(tempFilename, tempFileExtension.startsWith(".") ? tempFileExtension : "."+tempFileExtension);
			newGenmodelFile.deleteOnExit();
			// all references for an old URI within resource set should be changed! 
			nextResource.setURI(URI.createFileURI(newGenmodelFile.getAbsolutePath()));
			if (nextResource.equals(resource)) {
				newUri = nextResource.getURI();
			}
		}
		for (Resource nextResource : resourceSet.getResources()) {
			try {
				nextResource.save(null);
			} catch (IOException ex) {
				fail(ex.toString());
			}
		}
		return newUri;
	}

	private void changeNsUriToOldOne(URI newUri, String nsPrefix, String nsUri) throws IOException {
		Path path = new Path(newUri.toFileString());
		File file = path.toFile();
		FileReader reader = new FileReader(file);
		char[] chars = new char[100000];
		int length = reader.read(chars);
		String content = new String(chars, 0, length).replaceFirst("xmlns:"+nsPrefix+"=\"[^\"]+\"", "xmlns:"+nsPrefix+"=\""+nsUri+"\"");
		FileWriter writer = new FileWriter(file);
		writer.write(content.toCharArray());
		writer.flush();
	}

	private void checkModelAndCorrectCategories(URI uri) {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), uri);
		Resource resource = loadHelper.getLoadedResource();
		int allContentsSize = 0;
		for (Iterator<EObject> it = resource.getAllContents(); it.hasNext();) {
			EObject next = it.next();
			allContentsSize++;
			if (next instanceof GenEditorGenerator) {
				GenEditorGenerator genEditor = (GenEditorGenerator) next;
				assertNotNull(genEditor.getAudits());
			} else if (next instanceof GenAuditRoot) {
				GenAuditRoot root = (GenAuditRoot) next;
				assertFalse(root.getCategories().isEmpty());
				assertFalse(root.getRules().isEmpty());
				assertEquals(3, root.getCategories().size());
				assertEquals(3, root.getRules().size());
			} else if (next instanceof GenAuditContainer) {
				GenAuditContainer nextContainer = (GenAuditContainer) next;
				assertFalse(nextContainer.getAudits().isEmpty());
				assertEquals(nextContainer.getAudits().size(), 1);
			} else if (next instanceof GenAuditRule) {
				GenAuditRule nextRule = (GenAuditRule) next;
				GenAuditContainer nextCategory = nextRule.getCategory();
				assertNotNull(nextCategory);
				assertEquals("Audit rule expected to be placed to correct audit category after migration", "rule:"+nextCategory.getId(), nextRule.getId()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		assertEquals(8, allContentsSize);
	}

	public void testNotChangingOrderOfLabelMappings() throws Exception {
		URI gmfmapmodelFileName = createURI("testNotChangingOrderOfLabelMappings.gmfmap"); //$NON-NLS-1$
		Exception caughtMapException = assertOrdinaryLoadModelProblems(gmfmapmodelFileName);
		assertTrue("expected diagnostic exception", caughtMapException != null); //$NON-NLS-1$

		assertOnLoadModelMigrationSuccess(gmfmapmodelFileName);
		checkOrderOfLabelMappings(gmfmapmodelFileName);

		URI newMapUri = temporarySaveMigratedModel(gmfmapmodelFileName, "testNotChangingOrderOfLabelMappings", "gmfmap"); //$NON-NLS-1$ //$NON-NLS-2$
		changeNsUriToOldOne(newMapUri, "gmfmap", "http://www.eclipse.org/gmf/2005/mappings/2.0"); //$NON-NLS-1$ //$NON-NLS-2$
		
		assertOnLoadModelMigrationDidNothing(newMapUri);
		checkOrderOfLabelMappings(newMapUri);
	}

	public void testRequiredPluginsMoved() throws Exception {
		URI gmfmapmodelFileName = createURI("testRequiredPluginsMoved.gmfgen"); //$NON-NLS-1$
		Exception caughtMapException = assertOrdinaryLoadModelProblems(gmfmapmodelFileName);
		assertTrue("expected diagnostic exception", caughtMapException != null); //$NON-NLS-1$

		assertOnLoadModelMigrationSuccess(gmfmapmodelFileName);
		checkAllRequiredPluginsAreNotLost(gmfmapmodelFileName);

		URI newMapUri = temporarySaveMigratedModel(gmfmapmodelFileName, "testRequiredPluginsMoved", "gmfgen"); //$NON-NLS-1$ //$NON-NLS-2$
		changeNsUriToOldOne(newMapUri, "gmfgen", "http://www.eclipse.org/gmf/2005/GenModel/2.0"); //$NON-NLS-1$ //$NON-NLS-2$
		
		assertOnLoadModelMigrationDidNothing(newMapUri);
		checkAllRequiredPluginsAreNotLost(newMapUri);
	}

	private void checkAllRequiredPluginsAreNotLost(URI modelUri) {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), modelUri);
		Resource resource = loadHelper.getLoadedResource();
		assertEquals(1, resource.getContents().size());
		Object first = resource.getContents().get(0);
		assertTrue(first instanceof GenEditorGenerator);
		GenEditorGenerator genEditor = (GenEditorGenerator) first;
		assertNotNull(genEditor.getExpressionProviders());
		assertFalse(genEditor.getExpressionProviders().getProviders().isEmpty());
		GenPlugin plugin = genEditor.getPlugin();
		assertNotNull(plugin);
		EList<String> requiredPlugins = plugin.getRequiredPlugins();
		assertEquals(3, requiredPlugins.size());
		assertEquals("org.eclipse.fake.x1", requiredPlugins.get(0));
		assertEquals("org.eclipse.fake.x2", requiredPlugins.get(1));
		assertEquals("org.eclipse.x3", requiredPlugins.get(2));
	}

	private void checkOrderOfLabelMappings(URI modelURI) {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), modelURI);
		Resource res = loadHelper.getLoadedResource();
		for (Iterator<EObject> it = res.getAllContents(); it.hasNext();) {
			EObject next = it.next();
			if (next instanceof MappingEntry) {
				MappingEntry nextEntry = (MappingEntry) next;
				EList<LabelMapping> labelMappings = nextEntry.getLabelMappings();
				assertFalse(labelMappings.isEmpty());
				assertEquals(5, labelMappings.size());
				checkMapping(labelMappings.get(0), false);
				checkMapping(labelMappings.get(1), true);
				checkMapping(labelMappings.get(2), false);
				checkMapping(labelMappings.get(3), true);
				checkMapping(labelMappings.get(4), false);
			}
		}
	}

	private void checkMapping(LabelMapping mapping, boolean shouldBeNarrowed) {
		assertEquals(shouldBeNarrowed, mapping instanceof FeatureLabelMapping);
		assertNotNull(mapping.getDiagramLabel());
		if (shouldBeNarrowed) {
			assertFalse(((FeatureLabelMapping)mapping).getFeatures().isEmpty());
		}
	}

	private void checkFeatureLabelModelFacetsMigrated(URI uri) {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), uri);
		Resource resource = loadHelper.getLoadedResource();
		assertEquals(1, resource.getContents().size());
		EObject editorGen = resource.getContents().get(0);
		assertTrue(editorGen instanceof GenEditorGenerator);
		assertEquals(1, editorGen.eContents().size());
		EObject diagram = editorGen.eContents().get(0);
		assertTrue(diagram instanceof GenDiagram);
		assertEquals(1, diagram.eContents().size());
		GenTopLevelNode root = (GenTopLevelNode) diagram.eContents().get(0);
		assertEquals(2, root.eContents().size());
		assertEquals(2, root.getLabels().size());
		//
		LabelModelFacet first = root.getLabels().get(0).getModelFacet();
		assertTrue(first instanceof FeatureLabelModelFacet);
		FeatureLabelModelFacet firstFeatureLabelModelFacet = (FeatureLabelModelFacet) first;
		assertEquals(1, firstFeatureLabelModelFacet.getMetaFeatures().size());
		LabelModelFacet second = root.getLabels().get(1).getModelFacet();
		assertTrue(second instanceof FeatureLabelModelFacet);
		FeatureLabelModelFacet secondFeatureLabelModelFacet = (FeatureLabelModelFacet) second;
		assertEquals(2, secondFeatureLabelModelFacet.getMetaFeatures().size());
	}


	public void testGraphReferencingElements() throws Exception {
		URI gmfgraphFileName = createURI("basic.gmfgraph"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(gmfgraphFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(gmfgraphFileName);
		checkAllFigureReferences(gmfgraphFileName);

		URI newUri = temporarySaveMigratedModel(gmfgraphFileName, "basic", "gmfgraph");
		changeNsUriToOldOne(newUri, "gmfgraph", "http://www.eclipse.org/gmf/2005/GraphicalDefinition");
		
		assertOnLoadModelMigrationDidNothing(newUri);
		checkAllFigureReferences(newUri);
	}

	private void checkAllFigureReferences(URI modelUri) {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), modelUri);
		Resource resource = loadHelper.getLoadedResource();
		
		assertEquals(1, resource.getContents().size());
		Object first = resource.getContents().get(0);
		assertTrue(first instanceof Canvas);
		Canvas canvas = (Canvas) first;
		assertEquals(8, canvas.eContents().size());
		
		assertNotNull(canvas.getFigures());
		assertFalse(canvas.getFigures().isEmpty());
		assertEquals(1, canvas.getFigures().size());
		
		FigureGallery fg = canvas.getFigures().get(0);
		assertEquals("GenericDiagramFigures", fg.getName());
		assertFalse(fg.getFigures().isEmpty());
		assertEquals(1, fg.getFigures().size());

		Figure figure0 = fg.getFigures().get(0);
		assertTrue(figure0 instanceof PolylineDecoration);
		PolylineDecoration linked = (PolylineDecoration) figure0;
		assertEquals("ArrowDecoration", linked.getName());
		
		assertFalse(fg.getDescriptors().isEmpty());
		assertEquals(6, fg.getDescriptors().size());
		
		FigureDescriptor fg1 = fg.getDescriptors().get(0);
		assertTrue(fg1.getAccessors().isEmpty());
		
		FigureDescriptor fg5 = fg.getDescriptors().get(4);
		assertFalse(fg5.getAccessors().isEmpty());
		assertEquals(1, fg5.getAccessors().size());
		
		Figure figure1 = fg.getDescriptors().get(0).getActualFigure();
		assertTrue(figure1 instanceof Rectangle);
		Rectangle nr  = (Rectangle) figure1;
		assertEquals("NodeRectangle", nr.getName());
		assertNotNull(nr.getLayout());
		assertTrue(nr.getLayout() instanceof FlowLayout);
		assertNotNull(nr.getDescriptor());
		assertEquals(nr, nr.getDescriptor().getActualFigure());
		assertEquals(0, nr.getDescriptor().getAccessors().size());
		
		Figure figure2 = fg.getDescriptors().get(1).getActualFigure();
		assertTrue(figure2 instanceof PolylineConnection);
		PolylineConnection pc = (PolylineConnection) figure2;
		assertEquals("ConnectionLine", pc.getName());
		assertNotNull(pc.getDescriptor());
		assertEquals(pc, pc.getDescriptor().getActualFigure());
		assertEquals(0, pc.getDescriptor().getAccessors().size());
		
		Figure figure3 = fg.getDescriptors().get(2).getActualFigure();
		assertTrue(figure3 instanceof LabeledContainer);
		LabeledContainer lc = (LabeledContainer) figure3;
		assertEquals("ContainerFigure", lc.getName());
		assertNotNull(lc.getDescriptor());
		assertEquals(lc, lc.getDescriptor().getActualFigure());
		assertEquals(0, lc.getDescriptor().getAccessors().size());
		
		Figure figure4 = fg.getDescriptors().get(3).getActualFigure();
		assertTrue(figure4 instanceof Label);
		Label lab = (Label) figure4;
		assertEquals("LabelFigure", lab.getName());
		assertNotNull(lab.getDescriptor());
		assertEquals(lab, lab.getDescriptor().getActualFigure());
		assertEquals(0, lab.getDescriptor().getAccessors().size()); //2 references!!!
		
		Figure figure5 = fg.getDescriptors().get(4).getActualFigure();
		assertTrue(figure5 instanceof Rectangle);
		Rectangle nnr = (Rectangle) figure5;
		assertEquals("NamedNodeRectangle", nnr.getName());
		assertNotNull(nnr.getLayout());
		assertTrue(nnr.getLayout() instanceof FlowLayout);
		assertNotNull(nnr.getChildren());
		assertFalse(nnr.getChildren().isEmpty());
		assertEquals(1, nnr.getChildren().size());
		assertNotNull(nnr.getDescriptor());
		assertEquals(nnr, nnr.getDescriptor().getActualFigure());

		Figure figure1in5 = nnr.getChildren().get(0);
		assertTrue(figure1in5 instanceof Label);
		Label nnrLabel = (Label) figure1in5;
		assertEquals("NamedNode_NameLabelFigure", nnrLabel.getName());
		assertNotNull(nnrLabel.getDescriptor());
		assertEquals(nnrLabel.getDescriptor(), nnr.getDescriptor());
		assertEquals(1, nnr.getDescriptor().getAccessors().size());
		assertEquals(nnrLabel, nnr.getDescriptor().getAccessors().get(0).getFigure());

		Figure figure6 = fg.getDescriptors().get(5).getActualFigure();
		assertTrue(figure6 instanceof PolylineConnection);
		PolylineConnection fcf = (PolylineConnection) figure6;
		assertEquals("FigureConnectionFigure", fcf.getName());
		assertEquals(LineKind.LINE_DASHDOT_LITERAL, fcf.getLineKind());
		assertNotNull(fcf.getTargetDecoration());
		assertEquals(linked, fcf.getTargetDecoration());
		assertNotNull(fcf.getDescriptor());
		assertEquals(fcf, fcf.getDescriptor().getActualFigure());

		assertNotNull(canvas.getNodes());
		assertFalse(canvas.getNodes().isEmpty());
		assertEquals(2, canvas.getNodes().size());
		
		Node node1 = canvas.getNodes().get(0);
		assertEquals("Node", node1.getName());
		
		Node node2 = canvas.getNodes().get(1);
		assertEquals("NamedNode", node2.getName());
		
		assertNotNull(canvas.getConnections());
		assertFalse(canvas.getConnections().isEmpty());
		assertEquals(1, canvas.getConnections().size());
		
		Connection connection = canvas.getConnections().get(0);
		assertEquals("Link", connection.getName());
		
		assertNotNull(canvas.getCompartments());
		assertFalse(canvas.getCompartments().isEmpty());
		assertEquals(1, canvas.getCompartments().size());
		
		Compartment compartment = canvas.getCompartments().get(0);
		assertEquals("Compartment", compartment.getName());
		
		assertNotNull(canvas.getLabels());
		assertFalse(canvas.getLabels().isEmpty());
		assertEquals(3, canvas.getLabels().size());
		
		DiagramLabel l1 = canvas.getLabels().get(0);
		assertEquals("NamedNode_Name", l1.getName());
		
		DiagramLabel l2 = canvas.getLabels().get(1);
		assertEquals("Label", l2.getName());
		
		DiagramLabel l3 = canvas.getLabels().get(2);
		assertEquals("LabelWOIcon", l3.getName());
	}

	public void testCustomFigures() throws Exception {
		assertTrue(((EClass)GMFGraphPackage.eINSTANCE.getFigureAccessor_TypedFigure().getEType()).isAbstract());
		
		URI gmfgraphFileName = createURI("customFigures.gmfgraph"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(gmfgraphFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(gmfgraphFileName);
		checkCustomFiguresContent(gmfgraphFileName);

		URI newUri = temporarySaveMigratedModel(gmfgraphFileName, "customFigures", "gmfgraph");
		changeNsUriToOldOne(newUri, "gmfgraph", "http://www.eclipse.org/gmf/2005/GraphicalDefinition");
		
		assertOnLoadModelMigrationDidNothing(newUri);
		checkCustomFiguresContent(newUri);
	}

	private void checkCustomFiguresContent(URI modelUri) {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), modelUri);
		Resource resource = loadHelper.getLoadedResource();
		
		assertEquals(1, resource.getContents().size());
		Object first = resource.getContents().get(0);
		assertTrue(first instanceof Canvas);
		Canvas canvas = (Canvas) first;
		assertEquals(7, canvas.eContents().size());
		
		assertNotNull(canvas.getFigures());
		assertEquals(1, canvas.getFigures().size());
		FigureGallery fg = canvas.getFigures().get(0);
		
		assertNotNull(fg.getFigures());
		assertEquals(3, fg.getFigures().size());
		
		Figure figure1 = fg.getFigures().get(0);
		assertTrue(figure1 instanceof CustomFigure);
		assertEquals("org.eclipse.draw2d.ScalableFigure", ((CustomFigure)figure1).getQualifiedClassName());
		
		assertNotNull(fg.getDescriptors());
		assertEquals(4, fg.getDescriptors().size());
		
		Figure node1figure = fg.getDescriptors().get(0).getActualFigure();
		assertNotNull(node1figure);
		assertTrue(node1figure instanceof CustomFigure);
		assertEquals(1, ((CustomFigure) node1figure).getCustomChildren().size());
		RealFigure compartment2figure = ((CustomFigure) node1figure).getCustomChildren().get(0).getTypedFigure();
		assertNotNull(compartment2figure);
		assertTrue(compartment2figure instanceof CustomFigure);
		assertEquals("org.eclipse.draw2d.IFigure", ((CustomFigure)compartment2figure).getQualifiedClassName());
		
		Figure node2figure = fg.getDescriptors().get(1).getActualFigure();
		assertNotNull(node2figure);
		assertTrue(node2figure instanceof CustomFigure);
		assertEquals(1, ((CustomFigure) node2figure).getCustomChildren().size());
		RealFigure compartment1figure = ((CustomFigure) node2figure).getCustomChildren().get(0).getTypedFigure();
		assertNotNull(compartment1figure);
		
		Figure node3figure = fg.getDescriptors().get(2).getActualFigure();
		assertNotNull(node3figure);
		assertTrue(node3figure instanceof CustomFigure);
		assertEquals(1, ((CustomFigure) node3figure).getCustomChildren().size());
		RealFigure compartment4figure = ((CustomFigure) node3figure).getCustomChildren().get(0).getTypedFigure();
		assertNotNull(compartment4figure);
		assertTrue(compartment4figure instanceof CustomFigure);
		assertEquals("org.eclipse.draw2d.ScalableFigure", ((CustomFigure)compartment4figure).getQualifiedClassName());
		assertNotNull(node3figure);
		
		Figure compartment3figure = fg.getDescriptors().get(3).getActualFigure();
		assertNotNull(compartment3figure);
		
		assertNotNull(canvas.getNodes());
		assertEquals(2, canvas.getNodes().size());
		
		Node node1 = canvas.getNodes().get(0);
		assertEquals("LocalPreconditionNode", node1.getName());
		assertNotNull(node1.getFigure());
		assertEquals(node1figure, node1.getFigure().getActualFigure());
		
		Node node2 = canvas.getNodes().get(1);
		assertEquals("LocalPostconditionNode", node2.getName());
		assertNotNull(node2.getFigure());
		assertEquals(node2figure, node2.getFigure().getActualFigure());
		
		assertNotNull(canvas.getCompartments());
		assertEquals(4, canvas.getCompartments().size());
		
		Compartment compartment1 = canvas.getCompartments().get(0);
		assertEquals("postcondition", compartment1.getName());
		assertNotNull(compartment1.getFigure());
		assertNotNull(compartment1.getAccessor());
		assertEquals(compartment1figure, compartment1.getAccessor().getFigure());
		
		Compartment compartment2 = canvas.getCompartments().get(1);
		assertEquals("precondition", compartment2.getName());
		assertNotNull(compartment2.getFigure());
		assertNotNull(compartment2.getAccessor());
		assertEquals(compartment2figure, compartment2.getAccessor().getFigure());
		
		Compartment compartment3 = canvas.getCompartments().get(2);
		assertEquals("anotherPostcondition", compartment3.getName());
		assertNotNull(compartment3.getFigure());
		assertNull(compartment3.getAccessor());
		
		Compartment compartment4 = canvas.getCompartments().get(3);
		assertEquals("TargetCustomDecorCompartment", compartment4.getName());
		assertNotNull(compartment4.getFigure());
		assertNotNull(compartment4.getAccessor());
		assertEquals(compartment4figure, compartment4.getAccessor().getFigure());
	}
	
	public void testMultifiles() throws Exception {
		URI gmfgraphFileName = createURI("multifile_main.gmfgraph"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(gmfgraphFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(gmfgraphFileName);

		URI newUri = temporarySaveMigratedModel(gmfgraphFileName, "multifile_main", "gmfgraph");
		changeNsUriToOldOne(newUri, "gmfgraph", "http://www.eclipse.org/gmf/2005/GraphicalDefinition");
		
		assertOnLoadModelMigrationDidNothing(newUri);
	}

	public void testMultifilesLoadOrder() throws Exception {
		// load figure gallery with referencing elements from another file
		URI figureGalleryFileName = createURI("test_main.gmfgraph"); //$NON-NLS-1$
		
		Exception caughtFGException = assertOrdinaryLoadModelProblems(figureGalleryFileName);
		assertTrue("expected diagnostic exception", caughtFGException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(figureGalleryFileName);
		checkMultifilesStructure(figureGalleryFileName, false);
		
		URI newFigureGalleryUri = temporarySaveMigratedModel(figureGalleryFileName, "test_main", "gmfgraph");
		changeNsUriToOldOne(newFigureGalleryUri, "gmfgraph", "http://www.eclipse.org/gmf/2005/GraphicalDefinition");
		
		assertOnLoadModelMigrationDidNothing(newFigureGalleryUri);
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), newFigureGalleryUri);
		Resource mainResource = loadHelper.getLoadedResource();
		assertEquals(1, mainResource.getResourceSet().getResources().size());
		checkMultifilesGalleryStructure(mainResource);

		// and opposite load order - nodes first
		URI diagramElementsFileName = createURI("test_linked.gmfgraph"); //$NON-NLS-1$
		
		Exception caughtDEException = assertOrdinaryLoadModelProblems(diagramElementsFileName);
		assertTrue("expected diagnostic exception", caughtDEException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(diagramElementsFileName);
		checkMultifilesStructure(diagramElementsFileName, true);

		URI newDiagramElementsUri = temporarySaveMigratedModel(diagramElementsFileName, "test_linked", "gmfgraph");
		changeNsUriToOldOne(newDiagramElementsUri, "gmfgraph", "http://www.eclipse.org/gmf/2005/GraphicalDefinition");
		
		assertOnLoadModelMigrationDidNothing(newDiagramElementsUri);
		checkMultifilesStructure(newDiagramElementsUri, true);
	}

	private void checkMultifilesStructure(URI modelUri, boolean revertOrder) {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), modelUri);
		Resource mainResource = loadHelper.getLoadedResource();
		assertEquals(2, mainResource.getResourceSet().getResources().size());
		Resource linkedResource = mainResource.getResourceSet().getResources().get(1);
		if (revertOrder) {
			checkMultifilesNodesStructure(mainResource);
			checkMultifilesGalleryStructure(linkedResource);
		} else {
			checkMultifilesNodesStructure(linkedResource);
			checkMultifilesGalleryStructure(mainResource);
		}
	}

	private void checkMultifilesGalleryStructure(Resource resource) {
		assertEquals(1, resource.getContents().size());
		Object first = resource.getContents().get(0);
		assertTrue(first instanceof Canvas);
		Canvas canvas = (Canvas) first;
		assertEquals(1, canvas.eContents().size());
		
		assertNotNull(canvas.getFigures());
		assertEquals(1, canvas.getFigures().size());
		FigureGallery fg = canvas.getFigures().get(0);
		
		assertNotNull(fg.getFigures());
		assertEquals(1, fg.getFigures().size());
		
		assertNotNull(fg.getDescriptors());
		assertEquals(3, fg.getDescriptors().size());
		
		FigureDescriptor descriptor1 = fg.getDescriptors().get(0);
		assertEquals(0, descriptor1.getAccessors().size());
		
		FigureDescriptor descriptor2 = fg.getDescriptors().get(1);
		assertEquals(1, descriptor2.getAccessors().size());
		
		FigureDescriptor descriptor3 = fg.getDescriptors().get(2);
		assertEquals(2, descriptor3.getAccessors().size());
	}
	
	private void checkMultifilesNodesStructure(Resource resource) {
		assertEquals(1, resource.getContents().size());
		Object first = resource.getContents().get(0);
		assertTrue(first instanceof Canvas);
		Canvas canvas = (Canvas) first;
		assertEquals(6, canvas.eContents().size());
		
		assertNotNull(canvas.getFigures());
		assertEquals(1, canvas.getFigures().size());
		FigureGallery fg = canvas.getFigures().get(0);
		
		assertNotNull(fg.getFigures());
		assertEquals(1, fg.getFigures().size());
		assertNotNull(fg.getDescriptors());
		assertEquals(1, fg.getDescriptors().size());

		assertNotNull(canvas.getNodes());
		assertEquals(1, canvas.getNodes().size());
		assertNotNull(canvas.getNodes().get(0).getFigure());
		
		assertNotNull(canvas.getConnections());
		assertEquals(1, canvas.getConnections().size());
		assertNotNull(canvas.getConnections().get(0).getFigure());
		
		assertNotNull(canvas.getCompartments());
		assertEquals(2, canvas.getCompartments().size());
		assertNotNull(canvas.getCompartments().get(0).getFigure());
		assertNotNull(canvas.getCompartments().get(1).getFigure());
		
		assertNotNull(canvas.getLabels());
		assertEquals(1, canvas.getLabels().size());
		assertNotNull(canvas.getLabels().get(0).getFigure());
		
	}

	public void testAuditContexts() throws Exception {
		URI gmfgenFileName = createURI("test226149.gmfgen"); //$NON-NLS-1$
		
		Resource resource = assertNoOrdinaryLoadModelProblems(gmfgenFileName);
		assertEquals("http://www.eclipse.org/gmf/2006/GenModel", resource.getContents().get(0).eClass().getEPackage().getNsURI());

		assertOnLoadModelMigrationSuccess(gmfgenFileName);
		checkAuditContexts(gmfgenFileName);

		URI newUri = temporarySaveMigratedModel(gmfgenFileName, "test226149", "gmfgen");
		
		assertOnLoadModelMigrationDidNothing(newUri);
		checkAuditContexts(newUri);
	}

	private void checkAuditContexts(URI modelUri) {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), modelUri);
		Resource resource = loadHelper.getLoadedResource();
		
		assertEquals(1, resource.getContents().size());
		Object first = resource.getContents().get(0);
		assertTrue(first instanceof GenEditorGenerator);
		GenEditorGenerator editor = (GenEditorGenerator) first;
		assertEquals(1, editor.eContents().size());
		first = editor.eContents().get(0);
		assertTrue(first instanceof GenAuditRoot);
		GenAuditRoot root = (GenAuditRoot) first;
		assertEquals(6, root.eContents().size());
		
		assertNotNull(root.getClientContexts());
		assertFalse(root.getClientContexts().isEmpty());
		assertEquals(2, root.getClientContexts().size());
		
		GenAuditContext saveMe1 = root.getClientContexts().get(0);
		assertEquals("SaveMe1", saveMe1.getId());
		assertFalse(saveMe1.getRuleTargets().isEmpty());
		assertEquals(2, saveMe1.getRuleTargets().size());

		GenAuditContext saveMe2 = root.getClientContexts().get(1);
		assertEquals("SaveMe2", saveMe2.getId());
		assertFalse(saveMe2.getRuleTargets().isEmpty());
		assertEquals(1, saveMe2.getRuleTargets().size());
	}

	public void testFeatureValueSpecRefactor227505() throws Exception {
		URI gmfmapFileName = createURI("test227505.gmfmap"); //$NON-NLS-1$
		
		Exception caughtGenException = assertOrdinaryLoadModelProblems(gmfmapFileName);
		assertTrue("expected diagnostic exception", caughtGenException != null); //$NON-NLS-1$				

		assertOnLoadModelMigrationSuccess(gmfmapFileName);
		checkValueExpressions(gmfmapFileName);

		URI newUri = temporarySaveMigratedModel(gmfmapFileName, "test227505", "gmfmap");
		
		assertOnLoadModelMigrationDidNothing(newUri);
		checkValueExpressions(newUri);
	}

	private void checkValueExpressions(URI modelUri) {
		ModelLoadHelper loadHelper = new ModelLoadHelper(new ResourceSetImpl(), modelUri);
		Resource resource = loadHelper.getLoadedResource();
		//EcoreUtil.resolveAll(resource);
		
		assertEquals(1, resource.getContents().size());
		Object first = resource.getContents().get(0);
		assertTrue(first instanceof Mapping);
		Mapping mapping = (Mapping) first;
		assertEquals(2, mapping.eContents().size());
		first = mapping.eContents().get(0);
		assertTrue(first instanceof TopNodeReference);
		Object second = mapping.eContents().get(1);
		assertTrue(second instanceof CanvasMapping);
		TopNodeReference topNode = (TopNodeReference) first;
		assertEquals(1, topNode.eContents().size());
		first = topNode.eContents().get(0);
		assertTrue(first instanceof NodeMapping);
		NodeMapping node = (NodeMapping) first;
		
		ElementInitializer initer = node.getDomainInitializer();
		assertNotNull(initer);
		assertTrue(initer instanceof FeatureSeqInitializer);
		FeatureSeqInitializer featureRef = (FeatureSeqInitializer) initer;
		assertNotNull(featureRef.getInitializers());
		assertFalse(featureRef.getInitializers().isEmpty());
		assertEquals(3, featureRef.getInitializers().size());
		
		FeatureInitializer init1 = featureRef.getInitializers().get(0);
		assertTrue(init1 instanceof FeatureValueSpec);
		FeatureValueSpec feature1 = (FeatureValueSpec) init1;
		assertEquals(EcoreUtil.getURI(EcorePackage.eINSTANCE.getEModelElement_EAnnotations()).fragment(), EcoreUtil.getURI(feature1.getFeature()).fragment());
		ValueExpression value1 = feature1.getValue();
		assertNotNull(value1);
		assertEquals(Language.JAVA_LITERAL, value1.getLanguage());
		assertEquals("some.Checker", value1.getBody());

		FeatureInitializer init2 = featureRef.getInitializers().get(1);
		assertTrue(init2 instanceof FeatureValueSpec);
		FeatureValueSpec feature2 = (FeatureValueSpec) init2;
		assertEquals(EcoreUtil.getURI(EcorePackage.eINSTANCE.getENamedElement_Name()).fragment(), EcoreUtil.getURI(feature2.getFeature()).fragment());
		ValueExpression value2 = feature2.getValue();
		assertNotNull(value2);
		assertEquals(Language.OCL_LITERAL, value2.getLanguage());
		assertEquals("self.name", value2.getBody());

		FeatureInitializer init3 = featureRef.getInitializers().get(2);
		assertTrue(init3 instanceof ReferenceNewElementSpec);
		ReferenceNewElementSpec ref3 = (ReferenceNewElementSpec) init3;
		assertEquals(EcoreUtil.getURI(EcorePackage.eINSTANCE.getETypeParameter_EBounds()).fragment(), EcoreUtil.getURI(ref3.getFeature()).fragment());
		assertNotNull(ref3.getNewElementInitializers());
		assertFalse(ref3.getNewElementInitializers().isEmpty());
		assertEquals(1, ref3.getNewElementInitializers().size());
		
		FeatureSeqInitializer featureRef3 = ref3.getNewElementInitializers().get(0);
		assertNotNull(featureRef3);
		assertNotNull(featureRef3.getInitializers());
		assertFalse(featureRef3.getInitializers().isEmpty());
		assertEquals(1, featureRef3.getInitializers().size());
		
		FeatureInitializer init31 = featureRef3.getInitializers().get(0);
		assertTrue(init31 instanceof FeatureValueSpec);
		FeatureValueSpec feature31 = (FeatureValueSpec) init31;
		assertEquals(EcoreUtil.getURI(EcorePackage.eINSTANCE.getEGenericType_ELowerBound()).fragment(), EcoreUtil.getURI(feature31.getFeature()).fragment());
		ValueExpression value31 = feature31.getValue();
		assertNotNull(value31);
		assertEquals(Language.JAVA_LITERAL, value31.getLanguage());
		assertEquals("some.Checker2", value31.getBody());
	}

}
