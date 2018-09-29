/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 FoundationDriven
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.foundationdriven.foundation.api.formatting.styles;

import io.foundationdriven.foundation.api.formatting.errors.StyleNotFound;

import java.util.List;

/**
 * Manages the available styles
 */
public class StyleManager {
    /**
     * Stores all available styles
     */
    private static List<Style> styles;

    /**
     * Gets the available styles
     * @return styles
     */
    public static List<Style> getStyles() {
        return styles;
    }

    /**
     * Adds a style to the list
     * @param s the style to add
     * @return the style added
     */
    public static Style addStyle(Style s){
        if (styles.contains(s)) styles.remove(s);
        styles.add(s);
        return s;
    }

    /**
     * Gets the style with the provided name
     * @param name the name to search for
     * @return the style found
     * @see io.foundationdriven.foundation.api.formatting.errors.StyleNotFound
     */
    public static Style getStyle(String name){
        for (Style s : styles){
            if (s.getName() == name) return s;
        }
        throw new StyleNotFound(name);
    }
}
