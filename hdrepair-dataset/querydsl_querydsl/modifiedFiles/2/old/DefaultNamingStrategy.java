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
package com.mysema.query.sql.codegen;

import java.util.Locale;

import com.mysema.query.codegen.EntityType;
import static com.mysema.util.JavaSyntaxUtils.*;

/**
 * DefaultNamingStrategy is the default implementation of the NamingStrategy
 * interface. It changes underscore usage into camel case form.
 *
 * @author tiwe
 *
 */
public class DefaultNamingStrategy extends AbstractNamingStrategy {
    
    public DefaultNamingStrategy() {
        reservedSuffix = "Col";
    }
    
    @Override
    public String getClassName(String tableName) {
        if (tableName.length() > 1) {
            return tableName.substring(0, 1).toUpperCase(Locale.ENGLISH) + 
                    toCamelCase(tableName.substring(1));    
        } else {
            return tableName.toUpperCase(Locale.ENGLISH);
        }        
    }

    @Override
    public String getDefaultAlias(EntityType entityType) {
        return entityType.getData().get("table").toString();
    }

    @Override
    public String getDefaultVariableName(EntityType entityType) {
        return escape(entityType, toCamelCase(entityType.getData().get("table").toString()));    
    }
    
    @Override
    public String getForeignKeysVariable(EntityType entityType) {
        return escape(entityType, foreignKeysVariable);
    }

    @Override
    public String getPrimaryKeysVariable(EntityType entityType) {
        return escape(entityType, primaryKeysVariable);
    }

    @Override
    public String getPropertyName(String columnName, EntityType entityType) {
        if (columnName.length() > 1) {
            return normalizePropertyName(
                    columnName.substring(0, 1).toLowerCase(Locale.ENGLISH) 
                    + toCamelCase(columnName.substring(1)));    
        } else {
            return columnName.toLowerCase(Locale.ENGLISH);
        }                
    }

    @Override
    public String getPropertyNameForForeignKey(String fkName, EntityType entityType) {
        if (fkName.toLowerCase().startsWith("fk_")) {
            fkName = fkName.substring(3) + "_" + fkName.substring(0,2);
        }
        return getPropertyName(fkName, entityType);
    }

    @Override
    public String getPropertyNameForInverseForeignKey(String fkName, EntityType entityType) {
        return "_" + getPropertyNameForForeignKey(fkName, entityType);
    }
    

    @Override
    public String getPropertyNameForPrimaryKey(String pkName, EntityType entityType) {
        if (pkName.toLowerCase().startsWith("pk_")) {
            pkName = pkName.substring(3) + "_" + pkName.substring(0,2);
        }
        return getPropertyName(pkName, entityType);        
    }

    @Override
    public String normalizeColumnName(String columnName) {
        return columnName;
    }

    @Override
    public String normalizeTableName(String tableName) {
        return tableName;
    }

    @Override
    public String normalizeSchemaName(String schemaName) {
        return schemaName;
    }
    
    protected String normalizePropertyName(String name) {
        return isReserved(name) ? name + reservedSuffix : name;   
    }
    
    protected String escape(EntityType entityType, String name) {
        int suffix = 0;
        while (true) {
            String candidate = suffix > 0 ? name + suffix : name;
            if (entityType.getEscapedPropertyNames().contains(candidate)) {
                suffix++;
            } else {
                return candidate;
            }
        }      
    }

    protected String toCamelCase(String str) {
        boolean toLower = str.toUpperCase().equals(str);
        StringBuilder builder = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '_' || str.charAt(i) == ' ' || str.charAt(i) == '-') {
                i += 1;
                if (i < str.length()) {
                    builder.append(Character.toUpperCase(str.charAt(i)));    
                }                               
            } else if (toLower) {
                builder.append(Character.toLowerCase(str.charAt(i)));
            } else{
                builder.append(str.charAt(i));
            }
        }
        return builder.toString();
    }
    
}
