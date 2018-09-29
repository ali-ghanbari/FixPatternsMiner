/**
 * redpen: a text inspection tool
 * Copyright (C) 2014 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.redpen;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainTest {

    @Test
    public void testMain() throws RedPenException {
        String[] args = new String[]{
            "-c", "sample/conf/redpen-conf-en.xml",
                "sample/sample-doc/en/sampledoc-en.txt"
        };
        Main.run(args);
    }

    @Test
    public void testHelp() throws RedPenException {
        assertEquals(0, Main.run("-h"));
    }

    @Test
    public void testVersion() throws RedPenException {
        assertEquals(0, Main.run("-v"));
    }

}
