/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.redhat.ceylon.compiler.java.test.issues;

import org.junit.Test;

import com.redhat.ceylon.compiler.java.test.CompilerTest;

/**
 * Special issue with source path mimicking a one-package qualified name 
 * @author Stéphane Épardaud <stef@epardaud.fr>
 */
public class PackageIssuesTest extends CompilerTest {
    
    @Override
    protected String getSourcePath() {
        return path;
    }
	
    @Override
    protected String transformDestDir(String name) {
        return name + "-package-issues";
    }

    @Test
    public void testBug148(){
        compareWithJavaSource("bug148/Bug148.src", "bug148/Bug148.ceylon");
        compareWithJavaSource("bug148_2/Bug148_2.src", "bug148_2/Bug148_2.ceylon");
        compareWithJavaSource("bug148_3/Bug148_3.src", "bug148_3/Bug148_3.ceylon");
    }
    
    @Test
    public void testBug187(){
        compile("bug187/Main.ceylon");
        compareWithJavaSource("bug187/Bug187");
    }
	
    @Test
    public void testBug214(){
        compareWithJavaSource("Bug214.src", "Bug214.ceylon");
    }
    
    @Test
    public void testBug542_fail(){
        compareWithJavaSource("Bug542");
    }
    
}
