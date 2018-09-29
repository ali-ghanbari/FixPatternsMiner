/* 
 * Copyright (C) 2013 The Rythm Engine project
 * Gelin Luo <greenlaw110(at)gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.rythmengine.essential;

import org.rythmengine.TestBase;
import org.junit.Test;

/**
 * Test @verbatim()
 */
public class VerbatimParserTest extends TestBase {
    @Test
    public void test() {
        t = "@verbatim(){@args String s; @s}";
        s = r(t);
        eq("@args String s; @s");

        t = "@verbatim(){\n@args String s\n@s\n}";
        s = r(t);
        eq("@args String s\n@s");
        
        t = "@verbatim(){<pre>abc</pre>}";
        s = r(t);
        eq("<pre>abc</pre>");
    }
    
}
