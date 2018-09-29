/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.continuuity.hive.objectinspector;

import junit.framework.Assert;
import org.apache.hadoop.hive.serde2.SerDeUtils;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector.Category;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.UnionObject;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.apache.hadoop.hive.serde2.typeinfo.UnionTypeInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * TestStandardObjectInspectors.
 *
 */
public class StandardObjectInspectorsTest {

  @Test
  public void testStandardListObjectInspector() throws Throwable {
    try {
      StandardListObjectInspector loi1 = ObjectInspectorFactory
          .getStandardListObjectInspector(PrimitiveObjectInspectorFactory.javaIntObjectInspector);
      StandardListObjectInspector loi2 = ObjectInspectorFactory
          .getStandardListObjectInspector(PrimitiveObjectInspectorFactory.javaIntObjectInspector);
      Assert.assertEquals(loi1, loi2);

      // metadata
      Assert.assertEquals(Category.LIST, loi1.getCategory());
      Assert.assertEquals(PrimitiveObjectInspectorFactory.javaIntObjectInspector,
                          loi1.getListElementObjectInspector());

      // null
      Assert.assertNull("loi1.getList(null) should be null.", loi1.getList(null));
      Assert.assertEquals("loi1.getListLength(null) should be -1.", loi1.getListLength(null), -1);
      Assert.assertNull("loi1.getListElement(null, 0) should be null", loi1.getListElement(null, 0));
      Assert.assertNull("loi1.getListElement(null, 100) should be null", loi1.getListElement(null, 100));

      // ArrayList
      ArrayList<Integer> list = new ArrayList<Integer>();
      list.add(0);
      list.add(1);
      list.add(2);
      list.add(3);
      Assert.assertEquals(4, loi1.getList(list).size());
      Assert.assertEquals(4, loi1.getListLength(list));
      Assert.assertEquals(0, loi1.getListElement(list, 0));
      Assert.assertEquals(3, loi1.getListElement(list, 3));
      Assert.assertNull(loi1.getListElement(list, -1));
      Assert.assertNull(loi1.getListElement(list, 4));

      // Settable
      Object list4 = loi1.create(4);
      loi1.set(list4, 0, 0);
      loi1.set(list4, 1, 1);
      loi1.set(list4, 2, 2);
      loi1.set(list4, 3, 3);
      Assert.assertEquals(list, list4);

      loi1.resize(list4, 5);
      loi1.set(list4, 4, 4);
      loi1.resize(list4, 4);
      Assert.assertEquals(list, list4);

    } catch (Throwable e) {
      e.printStackTrace();
      throw e;
    }

  }

  @Test
  public void testStandardMapObjectInspector() throws Throwable {
    try {
      StandardMapObjectInspector moi1 = ObjectInspectorFactory.getStandardMapObjectInspector(
          PrimitiveObjectInspectorFactory.javaStringObjectInspector,
          PrimitiveObjectInspectorFactory.javaIntObjectInspector);
      StandardMapObjectInspector moi2 = ObjectInspectorFactory.getStandardMapObjectInspector(
          PrimitiveObjectInspectorFactory.javaStringObjectInspector,
          PrimitiveObjectInspectorFactory.javaIntObjectInspector);
      Assert.assertEquals(moi1, moi2);

      // metadata
      Assert.assertEquals(Category.MAP, moi1.getCategory());
      Assert.assertEquals(moi1.getMapKeyObjectInspector(), PrimitiveObjectInspectorFactory.javaStringObjectInspector);
      Assert.assertEquals(moi2.getMapValueObjectInspector(), PrimitiveObjectInspectorFactory.javaIntObjectInspector);

      // null
      Assert.assertNull(moi1.getMap(null));
      Assert.assertNull(moi1.getMapValueElement(null, null));
      Assert.assertNull(moi1.getMapValueElement(null, "nokey"));
      Assert.assertEquals(-1, moi1.getMapSize(null));
      Assert.assertEquals("map<" +
                          PrimitiveObjectInspectorFactory.javaStringObjectInspector.getTypeName() + "," +
                          PrimitiveObjectInspectorFactory.javaIntObjectInspector.getTypeName() +
                          ">",
                          moi1.getTypeName());

      // HashMap
      HashMap<String, Integer> map = new HashMap<String, Integer>();
      map.put("one", 1);
      map.put("two", 2);
      map.put("three", 3);
      Assert.assertEquals(map, moi1.getMap(map));
      Assert.assertEquals(3, moi1.getMapSize(map));
      Assert.assertEquals(1, moi1.getMapValueElement(map, "one"));
      Assert.assertEquals(2, moi1.getMapValueElement(map, "two"));
      Assert.assertEquals(3, moi1.getMapValueElement(map, "three"));
      Assert.assertNull(moi1.getMapValueElement(map, null));
      Assert.assertNull(moi1.getMapValueElement(map, "null"));

      // Settable
      Object map3 = moi1.create();
      moi1.put(map3, "one", 1);
      moi1.put(map3, "two", 2);
      moi1.put(map3, "three", 3);
      Assert.assertEquals(map, map3);
      moi1.clear(map3);
      Assert.assertEquals(0, moi1.getMapSize(map3));

    } catch (Throwable e) {
      e.printStackTrace();
      throw e;
    }

  }

  @Test
  public void testStandardStructObjectInspector() throws Throwable {
    try {
      // Test StandardObjectInspector both with field comments and without
      doStandardObjectInspectorTest(true);
      doStandardObjectInspectorTest(false);
    } catch (Throwable e) {
      e.printStackTrace();
      throw e;
    }

  }

  private void doStandardObjectInspectorTest(boolean testComments) {
    ArrayList<String> fieldNames = new ArrayList<String>();
    fieldNames.add("firstInteger");
    fieldNames.add("secondString");
    fieldNames.add("thirdBoolean");
    ArrayList<ObjectInspector> fieldObjectInspectors = new ArrayList<ObjectInspector>();
    fieldObjectInspectors
        .add(PrimitiveObjectInspectorFactory.javaIntObjectInspector);
    fieldObjectInspectors
        .add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);
    fieldObjectInspectors
        .add(PrimitiveObjectInspectorFactory.javaBooleanObjectInspector);
    ArrayList<String> fieldComments = new ArrayList<String>(3);
    if (testComments) {
      fieldComments.add("firstInteger comment");
      fieldComments.add("secondString comment");
      fieldComments.add("thirdBoolean comment");
    } else { // should have null for non-specified comments
      for (int i = 0; i < 3; i++) {
        fieldComments.add(null);
      }
    }

    StandardStructObjectInspector soi1 = testComments ?
        ObjectInspectorFactory
        .getStandardStructObjectInspector(fieldNames, fieldObjectInspectors,
            fieldComments)
      : ObjectInspectorFactory
        .getStandardStructObjectInspector(fieldNames, fieldObjectInspectors);
    StandardStructObjectInspector soi2 = testComments ?
        ObjectInspectorFactory
        .getStandardStructObjectInspector((ArrayList<String>) fieldNames
        .clone(), (ArrayList<ObjectInspector>) fieldObjectInspectors
        .clone(), (ArrayList<String>) fieldComments.clone())
        : ObjectInspectorFactory
        .getStandardStructObjectInspector((ArrayList<String>) fieldNames
        .clone(), (ArrayList<ObjectInspector>) fieldObjectInspectors
        .clone());
    Assert.assertEquals(soi1, soi2);

    // metadata
    Assert.assertEquals(Category.STRUCT, soi1.getCategory());
    List<? extends StructField> fields = soi1.getAllStructFieldRefs();
    Assert.assertEquals(3, fields.size());
    for (int i = 0; i < 3; i++) {
      Assert.assertEquals(fieldNames.get(i).toLowerCase(), fields.get(i)
          .getFieldName());
      Assert.assertEquals(fieldObjectInspectors.get(i), fields.get(i)
          .getFieldObjectInspector());
      Assert.assertEquals(fieldComments.get(i), fields.get(i).getFieldComment());
    }
    Assert.assertEquals(fields.get(1), soi1.getStructFieldRef("secondString"));
    StringBuilder structTypeName = new StringBuilder();
    structTypeName.append("struct<");
    for (int i = 0; i < fields.size(); i++) {
      if (i > 0) {
        structTypeName.append(",");
      }
      structTypeName.append(fields.get(i).getFieldName());
      structTypeName.append(":");
      structTypeName.append(fields.get(i).getFieldObjectInspector()
          .getTypeName());
    }
    structTypeName.append(">");
    Assert.assertEquals(structTypeName.toString(), soi1.getTypeName());

    // null
    Assert.assertNull(soi1.getStructFieldData(null, fields.get(0)));
    Assert.assertNull(soi1.getStructFieldData(null, fields.get(1)));
    Assert.assertNull(soi1.getStructFieldData(null, fields.get(2)));
    Assert.assertNull(soi1.getStructFieldsDataAsList(null));

    // HashStruct
    ArrayList<Object> struct = new ArrayList<Object>(3);
    struct.add(1);
    struct.add("two");
    struct.add(true);

    Assert.assertEquals(1, soi1.getStructFieldData(struct, fields.get(0)));
    Assert.assertEquals("two", soi1.getStructFieldData(struct, fields.get(1)));
    Assert.assertEquals(true, soi1.getStructFieldData(struct, fields.get(2)));

    // Settable
    Object struct3 = soi1.create();
    System.out.println(struct3);
    soi1.setStructFieldData(struct3, fields.get(0), 1);
    soi1.setStructFieldData(struct3, fields.get(1), "two");
    soi1.setStructFieldData(struct3, fields.get(2), true);
    Assert.assertEquals(struct, struct3);
  }

  @Test
  public void testStandardUnionObjectInspector() throws Throwable {
    try {
      ArrayList<ObjectInspector> objectInspectors = new ArrayList<ObjectInspector>();
      // add primitive types
      objectInspectors
          .add(PrimitiveObjectInspectorFactory.javaIntObjectInspector);
      objectInspectors
          .add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);
      objectInspectors
          .add(PrimitiveObjectInspectorFactory.javaBooleanObjectInspector);

      // add a list
      objectInspectors
          .add(ObjectInspectorFactory
          .getStandardListObjectInspector(PrimitiveObjectInspectorFactory.javaIntObjectInspector));

      // add a map
      objectInspectors
          .add(ObjectInspectorFactory
          .getStandardMapObjectInspector(
          PrimitiveObjectInspectorFactory.javaIntObjectInspector,
          PrimitiveObjectInspectorFactory.javaStringObjectInspector));

      // add a struct
      List<String> fieldNames = new ArrayList<String>();
      fieldNames.add("myDouble");
      fieldNames.add("myLong");
      ArrayList<ObjectInspector> fieldObjectInspectors = new ArrayList<ObjectInspector>();
      fieldObjectInspectors
          .add(PrimitiveObjectInspectorFactory.javaDoubleObjectInspector);
      fieldObjectInspectors
          .add(PrimitiveObjectInspectorFactory.javaLongObjectInspector);
      objectInspectors
          .add(ObjectInspectorFactory
          .getStandardStructObjectInspector(fieldNames, fieldObjectInspectors));

      StandardUnionObjectInspector uoi1 = ObjectInspectorFactory
          .getStandardUnionObjectInspector(objectInspectors);
      StandardUnionObjectInspector uoi2 = ObjectInspectorFactory
          .getStandardUnionObjectInspector(
          (ArrayList<ObjectInspector>) objectInspectors.clone());
      Assert.assertEquals(uoi1, uoi2);
      Assert.assertEquals(ObjectInspectorUtils.getObjectInspectorName(uoi1),
          ObjectInspectorUtils.getObjectInspectorName(uoi2));
      Assert.assertTrue(ObjectInspectorUtils.compareTypes(uoi1, uoi2));
      // compareSupported returns false because Union can contain
      // an object of Map
      Assert.assertFalse(ObjectInspectorUtils.compareSupported(uoi1));

      // construct unionObjectInspector without Map field.
      ArrayList<ObjectInspector> ois =
          (ArrayList<ObjectInspector>) objectInspectors.clone();
      ois.set(4, PrimitiveObjectInspectorFactory.javaIntObjectInspector);
      Assert.assertTrue(ObjectInspectorUtils.compareSupported(ObjectInspectorFactory
          .getStandardUnionObjectInspector(ois)));

      // metadata
      Assert.assertEquals(Category.UNION, uoi1.getCategory());
      List<? extends ObjectInspector> uois = uoi1.getObjectInspectors();
      Assert.assertEquals(6, uois.size());
      for (int i = 0; i < 6; i++) {
        Assert.assertEquals(objectInspectors.get(i), uois.get(i));
      }
      StringBuilder unionTypeName = new StringBuilder();
      unionTypeName.append("uniontype<");
      for (int i = 0; i < uois.size(); i++) {
        if (i > 0) {
          unionTypeName.append(",");
        }
        unionTypeName.append(uois.get(i).getTypeName());
      }
      unionTypeName.append(">");
      Assert.assertEquals(unionTypeName.toString(), uoi1.getTypeName());
      // TypeInfo
      TypeInfo typeInfo1 = TypeInfoUtils.getTypeInfoFromObjectInspector(uoi1);
      Assert.assertEquals(Category.UNION, typeInfo1.getCategory());
      Assert.assertEquals(UnionTypeInfo.class.getName(), typeInfo1.getClass().getName());
      Assert.assertEquals(typeInfo1.getTypeName(), uoi1.getTypeName());
      Assert.assertEquals(typeInfo1,
          TypeInfoUtils.getTypeInfoFromTypeString(uoi1.getTypeName()));
      TypeInfo typeInfo2 = TypeInfoUtils.getTypeInfoFromObjectInspector(uoi2);
      Assert.assertEquals(typeInfo1, typeInfo2);
      Assert.assertEquals(TypeInfoUtils.
          getStandardJavaObjectInspectorFromTypeInfo(typeInfo1), TypeInfoUtils.
          getStandardJavaObjectInspectorFromTypeInfo(typeInfo2));
      Assert.assertEquals(TypeInfoUtils.
              getStandardWritableObjectInspectorFromTypeInfo(typeInfo1),
          TypeInfoUtils.getStandardWritableObjectInspectorFromTypeInfo(
              typeInfo2)
      );

      // null
      Assert.assertNull(uoi1.getField(null));
      Assert.assertEquals(-1, uoi1.getTag(null));

      // Union
      UnionObject union = new StandardUnionObjectInspector.StandardUnion((byte) 0, 1);
      Assert.assertEquals(0, uoi1.getTag(union));
      Assert.assertEquals(1, uoi1.getField(union));
      Assert.assertEquals("{0:1}", SerDeUtils.getJSONString(union, uoi1));
      Assert.assertEquals(0, ObjectInspectorUtils.compare(union, uoi1,
          new StandardUnionObjectInspector.StandardUnion((byte) 0, 1), uoi2));
      Assert.assertTrue(ObjectInspectorUtils.copyToStandardObject(
          union, uoi1).equals(1));

      union = new StandardUnionObjectInspector.StandardUnion((byte) 1, "two");
      Assert.assertEquals(1, uoi1.getTag(union));
      Assert.assertEquals("two", uoi1.getField(union));
      Assert.assertEquals("{1:\"two\"}", SerDeUtils.getJSONString(union, uoi1));
      Assert.assertEquals(0, ObjectInspectorUtils.compare(union, uoi1,
          new StandardUnionObjectInspector.StandardUnion((byte) 1, "two"), uoi2));
      Assert.assertTrue(ObjectInspectorUtils.copyToStandardObject(
          union, uoi1).equals("two"));

      union = new StandardUnionObjectInspector.StandardUnion((byte) 2, true);
      Assert.assertEquals(2, uoi1.getTag(union));
      Assert.assertEquals(true, uoi1.getField(union));
      Assert.assertEquals("{2:true}", SerDeUtils.getJSONString(union, uoi1));
      Assert.assertEquals(0, ObjectInspectorUtils.compare(union, uoi1,
          new StandardUnionObjectInspector.StandardUnion((byte) 2, true), uoi2));
      Assert.assertTrue(ObjectInspectorUtils.copyToStandardObject(
          union, uoi1).equals(true));

      ArrayList<Integer> iList = new ArrayList<Integer>();
      iList.add(4);
      iList.add(5);
      union = new StandardUnionObjectInspector.StandardUnion((byte) 3, iList);
      Assert.assertEquals(3, uoi1.getTag(union));
      Assert.assertEquals(iList, uoi1.getField(union));
      Assert.assertEquals("{3:[4,5]}", SerDeUtils.getJSONString(union, uoi1));
      Assert.assertEquals(0, ObjectInspectorUtils.compare(union, uoi1,
          new StandardUnionObjectInspector.StandardUnion((byte) 3, iList.clone()), uoi2));
      Assert.assertTrue(ObjectInspectorUtils.copyToStandardObject(
          union, uoi1).equals(iList));

      HashMap<Integer, String> map = new HashMap<Integer, String>();
      map.put(6, "six");
      map.put(7, "seven");
      map.put(8, "eight");
      union = new StandardUnionObjectInspector.StandardUnion((byte) 4, map);
      Assert.assertEquals(4, uoi1.getTag(union));
      Assert.assertEquals(map, uoi1.getField(union));
      Assert.assertEquals("{4:{6:\"six\",7:\"seven\",8:\"eight\"}}",
          SerDeUtils.getJSONString(union, uoi1));
      Throwable th = null;
      try {
        ObjectInspectorUtils.compare(union, uoi1,
            new StandardUnionObjectInspector.StandardUnion((byte) 4, map.clone()), uoi2, null);
      } catch (Throwable t) {
        th = t;
      }
      Assert.assertNotNull(th);
      Assert.assertEquals("Compare on map type not supported!", th.getMessage());
      Assert.assertTrue(ObjectInspectorUtils.copyToStandardObject(
          union, uoi1).equals(map));


      ArrayList<Object> struct = new ArrayList<Object>(2);
      struct.add(9.0);
      struct.add(10L);
      union = new StandardUnionObjectInspector.StandardUnion((byte) 5, struct);
      Assert.assertEquals(5, uoi1.getTag(union));
      Assert.assertEquals(struct, uoi1.getField(union));
      Assert.assertEquals("{5:{\"mydouble\":9.0,\"mylong\":10}}",
          SerDeUtils.getJSONString(union, uoi1));
      Assert.assertEquals(0, ObjectInspectorUtils.compare(union, uoi1,
          new StandardUnionObjectInspector.StandardUnion((byte) 5, struct.clone()), uoi2));
      Assert.assertTrue(ObjectInspectorUtils.copyToStandardObject(
          union, uoi1).equals(struct));

    } catch (Throwable e) {
      e.printStackTrace();
      throw e;
    }
  }

}
