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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.mysema.codegen.model.Types;
import com.mysema.query.codegen.EntityType;
import com.mysema.query.sql.codegen.DefaultNamingStrategy;
import com.mysema.query.sql.codegen.NamingStrategy;

public class DefaultNamingStrategyTest {

    private NamingStrategy namingStrategy = new DefaultNamingStrategy();

    private EntityType entityModel;
    
    @Before
    public void setUp(){
        entityModel = new EntityType(Types.OBJECT);
        //entityModel.addAnnotation(new TableImpl("OBJECT"));
        entityModel.getData().put("table", "OBJECT");
    }
    
    @Test
    public void GetClassName() {
        assertEquals("UserData", namingStrategy.getClassName("user_data"));
        assertEquals("U", namingStrategy.getClassName("u"));
        assertEquals("Us",namingStrategy.getClassName("us"));
        assertEquals("U_", namingStrategy.getClassName("u_"));
        assertEquals("Us_",namingStrategy.getClassName("us_"));
    }

    @Test
    public void GetPropertyName() {
        assertEquals("whileCol", namingStrategy.getPropertyName("while", entityModel));
        assertEquals("name", namingStrategy.getPropertyName("name", entityModel));
        assertEquals("userId", namingStrategy.getPropertyName("user_id", entityModel));
        assertEquals("accountEventId", namingStrategy.getPropertyName("accountEvent_id", entityModel));
    }
    
    @Test
    public void GetPropertyName_With_Dashes() {
        assertEquals("aFoobar", namingStrategy.getPropertyName("A-FOOBAR" , entityModel));
        assertEquals("aFoobar", namingStrategy.getPropertyName("A_FOOBAR" , entityModel));
    }

    @Test
    public void GetPropertyName_For_Column_With_Spaces() {
        assertEquals("userId", namingStrategy.getPropertyName("user id", entityModel));
    }
    
    @Test
    public void GetPropertyNameForInverseForeignKey(){
        assertEquals("_superiorFk", namingStrategy.getPropertyNameForInverseForeignKey("fk_superior", entityModel));
    }
    
    @Test
    public void GetPropertyNameForForeignKey(){
        assertEquals("superiorFk", namingStrategy.getPropertyNameForForeignKey("fk_superior", entityModel));
        assertEquals("superiorFk", namingStrategy.getPropertyNameForForeignKey("FK_SUPERIOR", entityModel));        
        
        assertEquals("reffooBar", namingStrategy.getPropertyNameForForeignKey("REFFOO_BAR", entityModel));
        assertEquals("refFooBar", namingStrategy.getPropertyNameForForeignKey("REF_FOO_BAR", entityModel));
        assertEquals("refFooBar_", namingStrategy.getPropertyNameForForeignKey("REF_FOO_BAR_", entityModel));
    }
        
    
    @Test
    public void GetPropertyNameForPrimaryKey(){
        assertEquals("superiorPk", namingStrategy.getPropertyNameForPrimaryKey("pk_superior", entityModel));
        assertEquals("superiorPk", namingStrategy.getPropertyNameForPrimaryKey("PK_SUPERIOR", entityModel));        
    }
    
    @Test
    public void GetDefaultVariableName(){
        assertEquals("object", namingStrategy.getDefaultVariableName(entityModel));
    }
    
}
