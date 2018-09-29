/*******************************************************************************
 * Copyright (c) 2013 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.core.conversion;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.ValueConverterWithValueException;
import org.eclipse.xtext.conversion.impl.IDValueConverter;
import org.eclipse.xtext.nodemodel.INode;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class JavaIDValueConverter extends IDValueConverter {
	
	@Override
	public String toValue(String string, INode node) {
		if (string == null)
			return null;
		try {
			if (string.charAt(0) == '^') {
				string = string.substring(1);
			}
			String result = convertFromJavaIdentifier(string, node);
			return result;
		} catch (IllegalArgumentException e) {
			throw new ValueConverterException(e.getMessage(), node, e);
		}
	}
	
	public static boolean isValidIdentifierStart(char c) {
		return Character.isJavaIdentifierStart(c);
	}
	
	public static boolean isValidIdentifierPart(char c) {
		return Character.isJavaIdentifierPart(c);
	}

	/**
	 * Mostly copied from {@link Strings#convertFromJavaString(String, boolean)}
	 */
	public static String convertFromJavaIdentifier(String identifier, INode node) {
		char[] in = identifier.toCharArray();
		int off = 0;
		int len = identifier.length();
		char[] convtBuf = new char[len];
		char aChar;
		char[] out = convtBuf;
		int outLen = 0;
		int end = off + len;
		boolean error = false;
		boolean badChar = false;
		while (off < end) {
			aChar = in[off++];
			if (aChar == '\\') {
				aChar = in[off++];
				switch (aChar) {
					case 'u': {
						// Read the xxxx
						int value = 0;
						if (off + 4 > end || !isHexSequence(in, off, 4)) {
							error = true;
							out[outLen++] = aChar;
							break;
						} else {
							for (int i = 0; i < 4; i++) {
								aChar = in[off++];
								switch (aChar) {
								case '0':
								case '1':
								case '2':
								case '3':
								case '4':
								case '5':
								case '6':
								case '7':
								case '8':
								case '9':
									value = (value << 4) + aChar - '0';
									break;
								case 'a':
								case 'b':
								case 'c':
								case 'd':
								case 'e':
								case 'f':
									value = (value << 4) + 10 + aChar - 'a';
									break;
								case 'A':
								case 'B':
								case 'C':
								case 'D':
								case 'E':
								case 'F':
									value = (value << 4) + 10 + aChar - 'A';
									break;
								default:
									throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
								}
							}
							if (setChar(outLen, out, (char)value)) {
								outLen++;
							} else {
								badChar = true;
							}
							break;
						}
					}
					default: {
						if (setChar(outLen, out, aChar)) {
							outLen++;
						} else {
							badChar = true;
						}
					}
				}
			} else {
				if (setChar(outLen, out, aChar)) {
					outLen++;
				} else {
					badChar = true;
				}
			}
		}
		String result = new String(out, 0, outLen);
		if (error) {
			throw new ValueConverterWithValueException("Illegal escape sequence in identifier '" + identifier + "'", node, result, null);
		}
		if (badChar) {
			if (result.length() != 0)
				throw new ValueConverterWithValueException("Illegal character in identifier '" + result + "' (" + identifier + ")", node, result, null);
			else
				throw new ValueConverterWithValueException("Illegal character in identifier '" + identifier + "'", node, null, null);
		}
		return result;
	}
	
	private static boolean setChar(final int outLen, char[] out, char c) {
		if (outLen == 0) {
			if (!isValidIdentifierStart(c)) {
				return false;
			}
		} else {
			if (!isValidIdentifierPart(c)) {
				return false;
			}
		}
		out[outLen] = c;
		return true;
	}

	private static boolean isHexSequence(char[] in, int off, int chars) {
		for(int i = off; i < in.length && i < off + chars; i++) {
			char c = in[i];
			switch (c) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				case 'a':
				case 'b':
				case 'c':
				case 'd':
				case 'e':
				case 'f':
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
					continue;
				default:
					return false;
			}
		}
		return true;
	}

}
