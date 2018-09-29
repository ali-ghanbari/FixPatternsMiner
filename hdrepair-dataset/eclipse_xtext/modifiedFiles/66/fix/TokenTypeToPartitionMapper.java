/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtend2.ui.autoedit;

import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.ui.editor.model.TerminalsTokenTypeToPartitionMapper;

import com.google.inject.Singleton;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
@Singleton
public class TokenTypeToPartitionMapper extends TerminalsTokenTypeToPartitionMapper {

	public final static String RICH_STRING_LITERAL_PARTITION = "__rich_string";
	public static final String[] SUPPORTED_TOKEN_TYPES = new String[] { 
		COMMENT_PARTITION, 
		SL_COMMENT_PARTITION, 
		STRING_LITERAL_PARTITION, 
		RICH_STRING_LITERAL_PARTITION,
		IDocument.DEFAULT_CONTENT_TYPE 
	};

	@Override
	protected String calculateId(String tokenName, int tokenType) {
		if (
				"RULE_RICH_TEXT".equals(tokenName) || 
				"RULE_RICH_TEXT_START".equals(tokenName) || 
				"RULE_RICH_TEXT_END".equals(tokenName) || 
				"RULE_RICH_TEXT_INBETWEEN".equals(tokenName)) {
			return RICH_STRING_LITERAL_PARTITION;
		}
		return super.calculateId(tokenName, tokenType);
	}

	@Override
	public String[] getSupportedPartitionTypes() {
		return SUPPORTED_TOKEN_TYPES;
	}
}
