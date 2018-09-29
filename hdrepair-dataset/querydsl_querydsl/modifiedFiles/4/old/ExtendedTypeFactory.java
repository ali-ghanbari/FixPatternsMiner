/*
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysema.query.apt;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractTypeVisitor6;

import com.mysema.codegen.model.SimpleType;
import com.mysema.codegen.model.Type;
import com.mysema.codegen.model.TypeCategory;
import com.mysema.codegen.model.TypeExtends;
import com.mysema.codegen.model.TypeSuper;
import com.mysema.codegen.model.Types;
import com.mysema.query.annotations.QueryExclude;
import com.mysema.query.codegen.EntityType;
import com.mysema.query.codegen.QueryTypeFactory;
import com.mysema.query.codegen.Supertype;
import com.mysema.query.codegen.TypeMappings;

/**
 * ExtendedTypeFactory is a factory for APT inspection based Type creation
 *
 * @author tiwe
 *
 */
public final class ExtendedTypeFactory {

    private final Map<List<String>, Type> typeCache = new HashMap<List<String>, Type>();

    private final Map<List<String>, EntityType> entityTypeCache = new HashMap<List<String>, EntityType>();
    
    private final Type defaultType;

    private final Set<Class<? extends Annotation>> entityAnnotations;

    private final ProcessingEnvironment env;

    private final TypeMirror objectType, numberType, comparableType, collectionType, setType, listType, mapType;

    private final TypeMappings typeMappings;

    private final QueryTypeFactory queryTypeFactory;

    private boolean doubleIndexEntities = true;
    
    private final TypeVisitor<Type, Boolean> visitor = new AbstractTypeVisitor6<Type, Boolean>() {

        @Override
        public Type visitPrimitive(PrimitiveType primitiveType, Boolean p) {
            //return visit(env.getTypeUtils().boxedClass(primitiveType).asType(), p);
            switch (primitiveType.getKind()) {
            case BOOLEAN: return Types.BOOLEAN;
            case BYTE: return Types.BYTE;
            case SHORT: return Types.SHORT;
            case INT: return Types.INTEGER;
            case LONG: return Types.LONG;
            case CHAR: return Types.CHARACTER;
            case FLOAT: return Types.FLOAT;
            case DOUBLE: return Types.DOUBLE;
            default: return null;
            }
        }

        @Override
        public Type visitNull(NullType nullType, Boolean p) {
            throw new IllegalStateException();
        }

        @Override
        public Type visitArray(ArrayType arrayType, Boolean p) {
            return visit(arrayType.getComponentType(), p).asArrayType();
        }

        @Override
        public Type visitDeclared(DeclaredType declaredType, Boolean p) {
            if (declaredType.asElement() instanceof TypeElement) {
                TypeElement typeElement = (TypeElement)declaredType.asElement();
                switch(typeElement.getKind()){
                case ENUM:      return createEnumType(declaredType, typeElement, p);
                case CLASS:     return createClassType(declaredType, typeElement, p);
                case INTERFACE: return createInterfaceType(declaredType, typeElement, p);
                default: throw new IllegalArgumentException("Illegal type " + typeElement);
                }
            } else {
                throw new IllegalArgumentException("Unsupported element type " + declaredType.asElement());
            }
        }

        @Override
        public Type visitError(ErrorType errorType, Boolean p) {
            return visitDeclared(errorType, p);
        }

        @Override
        public Type visitTypeVariable(TypeVariable typeVariable, Boolean p) {
            String varName = typeVariable.toString();
            if (typeVariable.getUpperBound() != null) {
                Type type = visit(typeVariable.getUpperBound(), p);
                return new TypeExtends(varName, type);
            } else if (typeVariable.getLowerBound() != null && !(typeVariable.getLowerBound() instanceof NullType)) {
                return new TypeSuper(varName, visit(typeVariable.getLowerBound(), p));
            } else {
                return null;
            }
        }

        @Override
        public Type visitWildcard(WildcardType wildardType, Boolean p) {
            if (wildardType.getExtendsBound() != null) {
                Type type = visit(wildardType.getExtendsBound(), p);
                return new TypeExtends(type);
            } else if (wildardType.getSuperBound() != null) {
                return new TypeSuper(visit(wildardType.getSuperBound(), p));
            } else {
                return null;
            }
        }

        @Override
        public Type visitExecutable(ExecutableType t, Boolean p) {
            throw new IllegalStateException();
        }

        @Override
        public Type visitNoType(NoType t, Boolean p) {
            return defaultType;
        }

        @Override
        public Type visitUnknown(TypeMirror t, Boolean p) {
            return defaultType;
        }
        
    };

    // TODO : return TypeMirror instead ?!?
    
    private final TypeVisitor<List<String>, Boolean> keyBuilder = new AbstractTypeVisitor6<List<String>, Boolean>() {

        private List<String> visitBase(TypeMirror t) {
            List<String> rv = new ArrayList<String>();
            String name = t.toString();
            if (name.contains("<")) {
                name = name.substring(0, name.indexOf('<'));
            }
            rv.add(name);
            return rv;
        }
        
        @Override
        public List<String> visitPrimitive(PrimitiveType t, Boolean p) {
            return visit(env.getTypeUtils().boxedClass(t).asType(), p);
        }

        @Override
        public List<String> visitNull(NullType t, Boolean p) {
            return Collections.singletonList("Object");
        }

        @Override
        public List<String> visitArray(ArrayType t, Boolean p) {
            List<String> rv = new ArrayList<String>(visit(t.getComponentType()));
            rv.add("[]");
            return rv;
        }

        @Override
        public List<String> visitDeclared(DeclaredType t, Boolean p) {
            List<String> rv = visitBase(t);            
            for (TypeMirror arg : t.getTypeArguments()) {
                if (p) {
                    rv.addAll(visit(arg, false));
                } else {
                    rv.add(arg.toString());
                }
            }
            return rv;
        }

        @Override
        public List<String> visitError(ErrorType t, Boolean p) {
            return visitDeclared(t, p);
        }

        @Override
        public List<String> visitTypeVariable(TypeVariable t, Boolean p) {
            List<String> rv = visitBase(t); 
            if (t.getUpperBound() != null) {
                rv.addAll(visit(t.getUpperBound(), p));
            }
            if (t.getLowerBound() != null) {
                rv.addAll(visit(t.getLowerBound(), p));
            }
            return rv;
        }

        @Override
        public List<String> visitWildcard(WildcardType t, Boolean p) {
            List<String> rv = visitBase(t); 
            if (t.getExtendsBound() != null) {
                rv.addAll(visit(t.getExtendsBound(), p));
            }
            if (t.getSuperBound() != null) {
                rv.addAll(visit(t.getSuperBound(), p));
            }
            return rv;
        }

        @Override
        public List<String> visitExecutable(ExecutableType t, Boolean p) {
            throw new IllegalStateException();
        }

        @Override
        public List<String> visitNoType(NoType t, Boolean p) {
            return Collections.singletonList("Object");
        }
        
    };
    
    public ExtendedTypeFactory(
            ProcessingEnvironment env,
            Configuration configuration,
            Set<Class<? extends Annotation>> annotations,
            TypeMappings typeMappings,
            QueryTypeFactory queryTypeFactory){
        this.env = env;
        this.defaultType = Types.OBJECT;
        this.entityAnnotations = annotations;
        this.objectType = getErasedType(Object.class);
        this.numberType = getErasedType(Number.class);
        this.comparableType = getErasedType(Comparable.class);
        this.collectionType = getErasedType(Collection.class);
        this.listType = getErasedType(List.class);
        this.setType = getErasedType(Set.class);
        this.mapType = getErasedType(Map.class);
        this.typeMappings = typeMappings;
        this.queryTypeFactory = queryTypeFactory;
    }
    
    private TypeMirror getErasedType(Class<?> clazz) {
        return env.getTypeUtils().erasure(env.getElementUtils().getTypeElement(clazz.getName()).asType());
    }
    
    private Type createType(TypeElement typeElement, TypeCategory category, 
            List<? extends TypeMirror> typeArgs, boolean deep) {
        String name = typeElement.getQualifiedName().toString();
        String simpleName = typeElement.getSimpleName().toString();
        String packageName = env.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        Type[] params = new Type[typeArgs.size()];
        for (int i = 0; i < params.length; i++) {
            params[i] = getType(typeArgs.get(i), deep);
        }
        return new SimpleType(category, name, packageName, simpleName, false,
                typeElement.getModifiers().contains(Modifier.FINAL), params);
    }

    public Collection<EntityType> getEntityTypes() {
        return entityTypeCache.values();
    }
    
    @Nullable
    public Type getType(TypeMirror typeMirror, boolean deep){
        List<String> key = keyBuilder.visit(typeMirror,true);
        if (entityTypeCache.containsKey(key)) {
            return entityTypeCache.get(key);
        } else if (typeCache.containsKey(key)) {
            return typeCache.get(key);
        } else {
            return createType(typeMirror, key, deep);
        }
    }

    @Nullable
    private Type createType(TypeMirror typeMirror, List<String> key, boolean deep) {
        typeCache.put(key, null);
        Type type = visitor.visit(typeMirror, deep);
        if (type != null && (type.getCategory() == TypeCategory.ENTITY || type.getCategory() == TypeCategory.CUSTOM)) {
            EntityType entityType = getEntityType(typeMirror, deep);
            typeCache.put(key, entityType);
            return entityType;
        } else {
            typeCache.put(key, type);
            return type;
        }
    }

    // TODO : simplify
    private Type createClassType(DeclaredType declaredType, TypeElement typeElement, boolean deep) {
        // other
        String name = typeElement.getQualifiedName().toString();
        TypeCategory typeCategory = TypeCategory.get(name);
        
        if (typeCategory != TypeCategory.NUMERIC 
                && isAssignable(typeElement.asType(), comparableType) 
                && isSubType(typeElement.asType(), numberType)) {
            typeCategory = TypeCategory.NUMERIC;
            
        } else if (!typeCategory.isSubCategoryOf(TypeCategory.COMPARABLE) 
                && isAssignable(typeElement.asType(), comparableType)) {
            typeCategory = TypeCategory.COMPARABLE;
            
        } if (typeCategory == TypeCategory.SIMPLE) {
            for (Class<? extends Annotation> entityAnn : entityAnnotations) {
                if (typeElement.getAnnotation(entityAnn) != null) {
                    typeCategory = TypeCategory.ENTITY;
                }
            }
        }

        // for intersection types etc
        if (name.equals("")) {
            TypeMirror type = objectType;
            if (typeCategory == TypeCategory.COMPARABLE) {
                type = comparableType;
            }
            // find most specific type of superTypes which is a subtype of type
            List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(declaredType);
            for (TypeMirror superType : superTypes) {
                if (env.getTypeUtils().isSubtype(superType, type)) {
                    type = superType;
                }
            }
            typeElement = (TypeElement)env.getTypeUtils().asElement(type);
        }
        
        Type type;
        if (typeElement.asType() instanceof DeclaredType && declaredType.getTypeArguments().isEmpty()) {
            type = createType(typeElement, typeCategory, ((DeclaredType)typeElement.asType()).getTypeArguments(), deep);
        } else {
            type = createType(typeElement, typeCategory, declaredType.getTypeArguments(), deep);
        }        
        
        TypeMirror superType = typeElement.getSuperclass();
        TypeElement superTypeElement = null;
        if (superType instanceof DeclaredType) {
            superTypeElement = (TypeElement) ((DeclaredType)superType).asElement();
        }

        // entity type
        for (Class<? extends Annotation> entityAnn : entityAnnotations) {
            if (typeElement.getAnnotation(entityAnn) != null || 
                    (superTypeElement != null && superTypeElement.getAnnotation(entityAnn) != null)){
                EntityType entityType = new EntityType(type);
                typeMappings.register(entityType, queryTypeFactory.create(entityType));
                return entityType;
            } 
        }
        return type;
    }

    private Type createMapType(String simpleName, Iterator<? extends TypeMirror> typeMirrors, boolean deep) {
        if (!typeMirrors.hasNext()) {
            return new SimpleType(Types.MAP, defaultType, defaultType);
        }
        
        Type keyType = getType(typeMirrors.next(), deep);
        if (keyType == null) {
            keyType = defaultType;
        }
        
        Type valueType = getType(typeMirrors.next(), deep);
        if (valueType == null) {
            valueType = defaultType;
        } else if (valueType.getParameters().isEmpty()) {
            TypeElement element = env.getElementUtils().getTypeElement(valueType.getFullName());
            if (element != null) {
                Type type = getType(element.asType(), deep);
                if (!type.getParameters().isEmpty()) {
                    valueType = new SimpleType(valueType, new Type[type.getParameters().size()]);
                }
            }
        }
        return new SimpleType(Types.MAP, keyType, valueType);
    }

    private Type createCollectionType(Type baseType, String simpleName, 
            Iterator<? extends TypeMirror> typeMirrors, boolean deep) {
        if (!typeMirrors.hasNext()){
            return new SimpleType(baseType, defaultType);
        }
        
        Type componentType = getType(typeMirrors.next(), deep);
        if (componentType == null) {
            componentType = defaultType;
        } else if (componentType.getParameters().isEmpty()) {
            TypeElement element = env.getElementUtils().getTypeElement(componentType.getFullName());
            if (element != null) {
                Type type = getType(element.asType(), deep);
                if (!type.getParameters().isEmpty()) {
                    componentType = new SimpleType(componentType, new Type[type.getParameters().size()]);
                }
            }
        }
        return new SimpleType(baseType, componentType);
    }

    @Nullable
    public EntityType getEntityType(TypeMirror typeMirror, boolean deep) { 
        List<String> key = keyBuilder.visit(typeMirror, true);
        // get from cache
        if (entityTypeCache.containsKey(key)) {
            EntityType entityType = entityTypeCache.get(key);
            if (deep && entityType.getSuperTypes().isEmpty()) {
                for (Type superType : getSupertypes(typeMirror, entityType, deep)) {
                    entityType.addSupertype(new Supertype(superType));
                }
            }
            return entityType;

        // create
        } else {
            return createEntityType(typeMirror, key, deep);

        }
    }

    @Nullable
    private EntityType createEntityType(TypeMirror typeMirror, List<String> key, boolean deep) {
        entityTypeCache.put(key, null);
        Type value = visitor.visit(typeMirror, deep);
        if (value != null) {
            EntityType entityType = null;
            if (value instanceof EntityType) {
                entityType = (EntityType)value;
            } else {
                entityType = new EntityType(value);
                typeMappings.register(entityType, queryTypeFactory.create(entityType));
            }
            entityTypeCache.put(key, entityType);

            if (key.size() > 1 && key.get(0).equals(entityType.getFullName()) && doubleIndexEntities) {
                List<String> newKey = new ArrayList<String>();
                newKey.add(entityType.getFullName());
                for (int i = 0; i < entityType.getParameters().size(); i++) {
                    newKey.add("?");
                }
                if (!entityTypeCache.containsKey(newKey)) {
                    entityTypeCache.put(newKey, entityType);
                }
            }

            if (deep) {
                for (Type superType : getSupertypes(typeMirror, value, deep)) {
                    entityType.addSupertype(new Supertype(superType));
                }
            }

            return entityType;
        }else{
            return null;
        }
    }

    private Type createEnumType(DeclaredType declaredType, TypeElement typeElement, boolean deep) {
        // fallback
        Type enumType = createType(typeElement, TypeCategory.ENUM, declaredType.getTypeArguments(), deep);

        for (Class<? extends Annotation> entityAnn : entityAnnotations) {
            if (typeElement.getAnnotation(entityAnn) != null) {
                EntityType entityType = new EntityType(enumType);
                typeMappings.register(entityType, queryTypeFactory.create(entityType));
                return entityType;
            }
        }
        return enumType;
    }

    private Type createInterfaceType(DeclaredType declaredType, TypeElement typeElement, boolean deep) {
        // entity type
        for (Class<? extends Annotation> entityAnn : entityAnnotations) {
            if (typeElement.getAnnotation(entityAnn) != null) {
                return createType(typeElement, TypeCategory.ENTITY, declaredType.getTypeArguments(), deep);
            }
        }
        
        String simpleName = typeElement.getSimpleName().toString();
        Iterator<? extends TypeMirror> i = declaredType.getTypeArguments().iterator();
        
        if (isAssignable(declaredType, mapType)) {
            return createMapType(simpleName, i, deep);

        } else if (isAssignable(declaredType, listType)) {
            return createCollectionType(Types.LIST, simpleName, i, deep);

        } else if (isAssignable(declaredType, setType)) {
            return createCollectionType(Types.SET, simpleName, i, deep);

        } else if (isAssignable(declaredType, collectionType)) {
            return createCollectionType(Types.COLLECTION, simpleName, i, deep);

        } else {
            String name = typeElement.getQualifiedName().toString();
            return createType(typeElement, TypeCategory.get(name), declaredType.getTypeArguments(), deep);
        }
    }
    
    private Set<Type> getSupertypes(TypeMirror typeMirror, Type type, boolean deep) {
        boolean doubleIndex = doubleIndexEntities;
        doubleIndexEntities = false;
        Set<Type> superTypes = Collections.emptySet();
        typeMirror = normalize(typeMirror);
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType)typeMirror;
            TypeElement e = (TypeElement)declaredType.asElement();
            // class
            if (e.getKind() == ElementKind.CLASS) {
                if (e.getSuperclass().getKind() != TypeKind.NONE) {
                    TypeMirror supertype = normalize(e.getSuperclass());
                    if (supertype instanceof DeclaredType 
                            && ((DeclaredType)supertype).asElement().getAnnotation(QueryExclude.class) != null) {
                        return Collections.emptySet();
                    } else {
                        Type superClass = getType(supertype, deep);                   
                        if (superClass == null) {
                            System.err.println("Got no type for " + supertype);
                        } else  if (!superClass.getFullName().startsWith("java")) {
                            superTypes = Collections.singleton(getType(supertype, deep));
                        }    
                    }
                    
                }
            // interface
            } else {
                superTypes = new HashSet<Type>(e.getInterfaces().size());
                for (TypeMirror mirror : e.getInterfaces()) {
                    Type iface = getType(mirror, deep);
                    if (!iface.getFullName().startsWith("java")) {
                        superTypes.add(iface);
                    }
                }
            }

        } else {
            return Collections.emptySet();
        }
        doubleIndexEntities = doubleIndex;
        return superTypes;
    }
    
    private boolean isAssignable(TypeMirror type, TypeMirror iface) {
        return env.getTypeUtils().isAssignable(type, iface)
            // XXX Eclipse 3.6 support
            || env.getTypeUtils().erasure(type).toString().equals(iface.toString());
    }

    private boolean isSubType(TypeMirror type1, TypeMirror clazz) {
        return env.getTypeUtils().isSubtype(type1, clazz) 
             // XXX Eclipse 3.6 support
             ||  env.getTypeUtils().directSupertypes(type1).contains(clazz); 
    }
    
    private TypeMirror normalize(TypeMirror type) {
        if (type.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typeVar = (TypeVariable)type;
            if (typeVar.getUpperBound() != null) {
                return typeVar.getUpperBound();
            }
        } else if (type.getKind() == TypeKind.WILDCARD) {
            WildcardType wildcard = (WildcardType)type;
            if (wildcard.getExtendsBound() != null) {
                return wildcard.getExtendsBound();
            }
        }
        return type;
    }
}
