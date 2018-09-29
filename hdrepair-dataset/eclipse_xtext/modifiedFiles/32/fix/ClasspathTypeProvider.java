/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.access.impl;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.access.IMirror;
import org.eclipse.xtext.common.types.access.TypeResource;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.Strings;

import com.google.common.collect.Maps;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class ClasspathTypeProvider extends AbstractJvmTypeProvider {

	protected static class TypeInResourceSetAdapter extends AdapterImpl {
		
		private Map<String, JvmType> typeByQueryString = Maps.newHashMap();
		
		@Override
		public boolean isAdapterForType(Object type) {
			return TypeInResourceSetAdapter.class.equals(type);
		}
		
		protected JvmType tryFindTypeInIndex(String name, ClasspathTypeProvider typeProvider, boolean binaryNestedTypeDelimiter) {
			JvmType result = typeByQueryString.get(name);
			if (result != null)
				return result;
			JvmType candidate = typeProvider.doTryFindInIndex(name, binaryNestedTypeDelimiter);
			if (candidate != null) {
				typeByQueryString.put(name, candidate);
				return candidate;
			}
			return null;
		}
		
	}
	
	private final ClassFinder classFinder;
	
	private final DeclaredTypeFactory declaredTypeFactory;
	
	private final ClassURIHelper uriHelper;
	
	public ClasspathTypeProvider(ClassLoader classLoader, ResourceSet resourceSet, IndexedJvmTypeAccess indexedJvmTypeAccess) {
		super(resourceSet, indexedJvmTypeAccess);
		classFinder = createClassFinder(classLoader);
		uriHelper = createClassURIHelper();
		declaredTypeFactory = createDeclaredTypeFactory();
	}
	
	@Deprecated
	public ClasspathTypeProvider(ClassLoader classLoader, ResourceSet resourceSet) {
		this(classLoader, resourceSet, null);
	}
	
	@Override
	protected void registerProtocol(ResourceSet resourceSet) {
		super.registerProtocol(resourceSet);
		resourceSet.eAdapters().add(new TypeInResourceSetAdapter());
	}

	protected ClassFinder createClassFinder(ClassLoader classLoader) {
		return new ClassFinder(classLoader);
	}

	protected DeclaredTypeFactory createDeclaredTypeFactory() {
		return new DeclaredTypeFactory(uriHelper);
	}

	protected ClassURIHelper createClassURIHelper() {
		return new ClassURIHelper();
	}
	
	public ClassURIHelper getClassURIHelper() {
		return uriHelper;
	}
	
	public DeclaredTypeFactory getDeclaredTypeFactory() {
		return declaredTypeFactory;
	}

	public ClassFinder getClassFinder() {
		return classFinder;
	}
	
	@Override
	public JvmType findTypeByName(String name) {
		try {
			// seems to be the only reliable way to locate nested types
			// since dollar signs are a quite good indicator but not necessarily the best
			Class<?> clazz = classFinder.forName(name);
			return findTypeByClass(clazz);
		} catch (ClassNotFoundException e) {
			return tryFindTypeInIndex(name, true);
		} catch (NoClassDefFoundError e) { 
			/* 
			 * Error will be thrown if the contents of the binary class file does not match the expectation (transitively).
			 * See java.lang.ClassLoader.defineClass(String, byte[], int, int, ProtectionDomain)
			 */
			return tryFindTypeInIndex(name, true);
		}
	}

	/**
	 * @since 2.4
	 */
	@Override
	public JvmType findTypeByName(String name, boolean binaryNestedTypeDelimiter) {
		if (isBinaryNestedTypeDelimiter(name, binaryNestedTypeDelimiter)) {
			return findTypeByName(name);
		}
		return doFindTypeByName(name);
	}

	private JvmType doFindTypeByName(String name) {
		try {
			// seems to be the only reliable way to locate nested types
			// since dollar signs are a quite good indicator but not necessarily the best
			Class<?> clazz = findClassByName(name);
			return findTypeByClass(clazz);
		} catch (ClassNotFoundException e) {
			return tryFindTypeInIndex(name, false);
		} catch (NoClassDefFoundError e) { 
			/* 
			 * Error will be thrown if the contents of the binary class file does not match the expectation (transitively).
			 * See java.lang.ClassLoader.defineClass(String, byte[], int, int, ProtectionDomain)
			 */
			return tryFindTypeInIndex(name, false);
		}
	}

	private JvmType findTypeByClass(Class<?> clazz) {
		IndexedJvmTypeAccess indexedJvmTypeAccess = getIndexedJvmTypeAccess();
		URI resourceURI = uriHelper.createResourceURI(clazz);
		if (indexedJvmTypeAccess != null) {
			URI proxyURI = resourceURI.appendFragment(uriHelper.getFragment(clazz));
			EObject candidate = indexedJvmTypeAccess.getIndexedJvmType(proxyURI, getResourceSet());
			if (candidate instanceof JvmType)
				return (JvmType) candidate;
		}
		TypeResource result = (TypeResource) getResourceSet().getResource(resourceURI, true);
		return findTypeByClass(clazz, result);
	}

	private Class<?> findClassByName(String name) throws ClassNotFoundException {
		try {
			Class<?> clazz = classFinder.forName(name);
			return clazz;
		} catch(ClassNotFoundException exception) {
			int index = name.lastIndexOf('.');
			if (index <= 0) {
				throw exception;
			}
			String baseName = name.substring(0, index);
			try {
				Class<?> resolvedOuterClass = findClassByName(baseName);
				baseName = resolvedOuterClass.getName();
			} catch (ClassNotFoundException baseNameException) {
				throw exception;
			}
			Class<?> clazz = classFinder.forName(baseName + '$' + name.substring(index + 1));
			return clazz;
		}
	}

	protected JvmType tryFindTypeInIndex(String name, boolean binaryNestedTypeDelimiter) {
		TypeInResourceSetAdapter adapter = (TypeInResourceSetAdapter) EcoreUtil.getAdapter(getResourceSet().eAdapters(), TypeInResourceSetAdapter.class);
		if (adapter != null) {
			return adapter.tryFindTypeInIndex(name, this, binaryNestedTypeDelimiter);
		} else {
			return doTryFindInIndex(name, binaryNestedTypeDelimiter);
		}
	}

	@Nullable
	private JvmType doTryFindInIndex(String name, boolean binaryNestedTypeDelimiter) {
		IndexedJvmTypeAccess indexAccess = getIndexedJvmTypeAccess();
		if (indexAccess != null) {
			JvmType result = doTryFindInIndex(name, indexAccess);
			if (result == null && !isBinaryNestedTypeDelimiter(name, binaryNestedTypeDelimiter)) {
				ClassNameVariants variants = new ClassNameVariants(name);
				while(result == null && variants.hasNext()) {
					String nextVariant = variants.next();
					result = doTryFindInIndex(nextVariant, indexAccess);
				}
			}
			return result;
		}
		return null;
	}

	private JvmType doTryFindInIndex(String name, IndexedJvmTypeAccess indexAccess) {
		int index = name.indexOf('$');
		if (index < 0)
			index = name.indexOf('[');
		String qualifiedNameString = index < 0 ? name : name.substring(0, index);
		List<String> nameSegments = Strings.split(qualifiedNameString, '.');
		QualifiedName qualifiedName = QualifiedName.create(nameSegments);
		EObject candidate = indexAccess.getIndexedJvmType(qualifiedName, name, getResourceSet());
		if (candidate instanceof JvmType)
			return (JvmType) candidate;
		return null;
	}
	
	@Override
	protected IMirror createMirrorForFQN(String name) {
		try {
			Class<?> clazz = classFinder.forName(name);
			return createMirror(clazz);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	
	public ClassMirror createMirror(Class<?> clazz) {
		return ClassMirror.createClassMirror(clazz, declaredTypeFactory);
	}
	
	public JvmType findTypeByClass(Class<?> clazz, Resource resource) {
		// TODO: Maybe iterate the resource without computing a fragment
		String fragment = uriHelper.getFragment(clazz);
		JvmType result = (JvmType) resource.getEObject(fragment);
		if (result == null) {
			throw new IllegalStateException("Resource has not been loaded");
		} 
		return result;
	}

}
