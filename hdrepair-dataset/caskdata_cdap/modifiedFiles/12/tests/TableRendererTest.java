/*
 * Copyright © 2015 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package co.cask.cdap.cli.util.table;

import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.PrintStream;

/**
 *
 */
@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TableRendererTest {

  protected static final int LINE_WIDTH = 80;
  protected static final PrintStream OUTPUT = System.out;
  protected static final TableRendererConfig TEST_CONFIG = new TableRendererConfig() {
    @Override
    public int getLineWidth() {
      return LINE_WIDTH;
    }
  };

  public abstract TableRenderer getRenderer();

  @Before
  public void setUp() {
    OUTPUT.flush();
  }

  @Test
  public void testFormat() {
    Table table = Table.builder()
      .setHeader("c1", "c2", "c3333")
      .setRows(Table.rows()
                 .add("r1\n456", "r11", "r1")
                 .add("r2", "r2222\n123", "r")
                 .add("r3333", "r3", "r3\n1")
                 .build())
      .build();
    getRenderer().render(TEST_CONFIG, OUTPUT, table);
  }

  @Test
  public void testBigCell() {
    Table table = Table.builder()
      .setHeader("c1", "c2", "c3333")
      .setRows(Table.rows()
                 .add("r1zz" + Strings.repeat("z", 300) + "456", "r11", "r1")
                 .add("r2", "r2222 zzzzzzz z z z zzzzzz z zzzzzzzzz zzzzzzz zzzzzzz zzzzzzz zzzzz zzz123", "r")
                 .add("r3333", "r3", "r3\n1")
                 .build())
      .build();
    getRenderer().render(TEST_CONFIG, OUTPUT, table);
  }

  @Test
  public void testTwoLineCell() {
    Table table = Table.builder()
      .setHeader("c1", "c2", "c3333")
      .setRows(Table.rows()
                 .add("123456789012345678901234567890", "2", "3")
                 .add("r2", "r2222", "z")
                 .add("r3333", "r3", "r3\n1")
                 .build())
      .build();
    getRenderer().render(TEST_CONFIG, OUTPUT, table);
  }

  @Test
  public void testTwoLineCell2() {
    Table table = Table.builder()
      .setHeader("c1", "c2", "c3333")
      .setRows(Table.rows()
                 .add(Strings.repeat("z", 27) + "a", "2", "3")
                 .add("r2", "r2222", "z")
                 .add("r3333", "r3", "r3\n1")
                 .build())
      .build();
    getRenderer().render(TEST_CONFIG, OUTPUT, table);
  }

  @Test
  public void testLongHeader() {
    Table table = Table.builder()
      .setHeader("c1", "c2", "c3333veryveryveryveryveryveryveryveryveryveryvery long header")
      .setRows(Table.rows()
                 .add(Strings.repeat("z", 27) + "a", "2", "3")
                 .add("r2", "r2222", "z")
                 .add("r3333", "r3", "r3\n1")
                 .build())
      .build();
    getRenderer().render(TEST_CONFIG, OUTPUT, table);
  }
}
