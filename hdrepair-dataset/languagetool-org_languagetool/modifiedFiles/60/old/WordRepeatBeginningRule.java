/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;

/**
 * Check if three successive sentences begin with the same word, e.g. "I am Max. I am living in Germany. I like ice cream.",
 * and if two successive sentences begin with the same adverb, e.g. "Furthermore, he is ill. Furthermore, he likes her."
 * 
 * @author Markus Brenneis
 */
public class WordRepeatBeginningRule extends Rule {
  
  private String lastToken = "";
  private String beforeLastToken = "";
  
  public WordRepeatBeginningRule(final ResourceBundle messages, final Language language) {
    super(messages);
    super.setCategory(new Category(messages.getString("category_misc")));
  }

  @Override
  public String getId() {
    return "WORD_REPEAT_BEGINNING_RULE";
  }

  @Override
  public String getDescription() {
    return messages.getString("desc_repetition_beginning");
  }
  
  public boolean isAdverb(String token) {
    return false;
  }
  
  public boolean isException(String token) {
    // avoid warning when having lists like "2007: ..." or the like
    if (token.equals(":") || token.equals("–") || token.equals("-")) {
        return true;
    }
    return false;
  }

  @Override
  public RuleMatch[] match(final AnalyzedSentence text) {
    final List<RuleMatch> ruleMatches = new ArrayList<RuleMatch>();
    final AnalyzedTokenReadings[] tokens = text.getTokensWithoutWhitespace();
    
    if (tokens.length>3) {
      final String token = tokens[1].getToken();
      // avoid "..." etc. to be matched:
      boolean isWord = true;
      if (token.length() == 1) {
        final char c = token.charAt(0);
        if (!Character.isLetter(c)) {
          isWord = false;
        }
      }
      
      if (isWord && lastToken.equals(token)
          && !isException(token) && !isException(tokens[2].getToken()) && !isException(tokens[3].getToken())) {
        final String shortMsg;
        if (isAdverb(token)) {
          shortMsg = messages.getString("desc_repetition_beginning_adv");
        } else if (beforeLastToken.equals(token)) {
          shortMsg = messages.getString("desc_repetition_beginning_word");
        } else {
          shortMsg = "";
        }
          
        if (!shortMsg.equals("")) {
          final String msg = shortMsg + " " + messages.getString("desc_repetition_beginning_thesaurus");
          final int startPos = tokens[1].getStartPos();
          final int endPos = startPos + token.length();
          final RuleMatch ruleMatch = new RuleMatch(this, startPos, endPos, msg, shortMsg);
          ruleMatches.add(ruleMatch);
        }
      }
      beforeLastToken = lastToken;
      lastToken = token;
    }
    
    //TODO should we ignore repetitions involving multiple paragraphs?
    //if (tokens[tokens.length - 1].isParaEnd()) beforeLastToken = "";
    
    return toRuleMatchArray(ruleMatches);
  }

  @Override
  public void reset() {
    // nothing
  }

}
