/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.test.parser.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.EnumConstant;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaEnum;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.parser.java.Visibility;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class JavaEnumTest
{
   private JavaEnum javaEnum;

   @SuppressWarnings("resource")
   @Before
   public void reset()
   {
      InputStream stream = JavaEnumTest.class.getResourceAsStream("/org/jboss/forge/grammar/java/MockEnum.java");
      javaEnum = JavaParser.parse(JavaEnum.class, stream);
   }

   @Test
   public void testCanParseEnum() throws Exception
   {
      assertEquals("MockEnum", javaEnum.getName());
   }

   @Test
   public void testAddEnumConstant()
   {
      int i = javaEnum.getEnumConstants().size();
      EnumConstant enumConstant = javaEnum.addEnumConstant().setName("BLAH");
      assertEquals(i + 1, javaEnum.getEnumConstants().size());
      assertEquals("BLAH", enumConstant.getName());
   }

   @Test
   public void testAddEnumConstantFromDeclaration()
   {
      int i = javaEnum.getEnumConstants().size();
      EnumConstant enumConstant = javaEnum.addEnumConstant("BLAH");
      assertEquals(i + 1, javaEnum.getEnumConstants().size());
      assertEquals("BLAH", enumConstant.getName());
   }

   @Test
   public void testAddEnumField()
   {
      int i = javaEnum.getFields().size();
      Field<JavaEnum> fld = javaEnum.addField().setName("fld").setType(Integer.TYPE).setVisibility(Visibility.PRIVATE);
      assertEquals(i + 1, javaEnum.getFields().size());
      assertEquals("fld", fld.getName());
      assertEquals(Integer.TYPE.getName(), fld.getType());
      assertSame(Visibility.PRIVATE, fld.getVisibility());
   }

   @Test
   public void testAddEnumMethod()
   {
      int i = javaEnum.getMethods().size();
      Method<JavaEnum> method = javaEnum.addMethod().setName("something").setReturnType(Void.TYPE)
               .setVisibility(Visibility.PUBLIC);
      assertEquals(i + 1, javaEnum.getMethods().size());
      assertEquals("something", method.getName());
      assertTrue(method.getParameters().isEmpty());
      assertSame(Visibility.PUBLIC, method.getVisibility());
   }
}
