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
package org.languagetool.tagging.disambiguation.rules;

import java.io.IOException;
import java.util.List;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.rules.patterns.AbstractPatternRule;
import org.languagetool.rules.patterns.Element;
import org.languagetool.rules.patterns.Match;
import org.languagetool.tools.StringTools;

/**
 * A Rule that describes a pattern of words or part-of-speech tags used for
 * disambiguation.
 * 
 * @author Marcin Miłkowski
 */
public class DisambiguationPatternRule extends AbstractPatternRule {

  /** Possible disambiguator actions. **/
  public enum DisambiguatorAction {
    ADD, FILTER, REMOVE, REPLACE, UNIFY, IMMUNIZE, FILTERALL;

    /**
     * Converts string to the constant enum.
     * 
     * @param str
     *          String value to be converted.
     * @return DisambiguatorAction enum.
     */
    public static DisambiguatorAction toAction(final String str) {
      try {
        return valueOf(str);
      } catch (final Exception ex) {
        return REPLACE;
      }
    }
  }
  
  private final String disambiguatedPOS;
  private final Match matchElement;
  private final DisambiguatorAction disAction;

  private AnalyzedToken[] newTokenReadings;
  private List<DisambiguatedExample> examples;
  private List<String> untouchedExamples;

  /**
   * @param id Id of the Rule
   * @param language Language of the Rule
   * @param elements Element (token) list
   * @param description Description to be shown (name)
   * @param disambAction the action to be executed on found token(s), one of the
   *          following: add, filter, remove, replace, unify.
   */
  DisambiguationPatternRule(final String id, final String description,
      final Language language, final List<Element> elements,
      final String disamb, final Match posSelect,
      final DisambiguatorAction disambAction) {
    super(id, description, language, elements, true);
    if (id == null) {
      throw new NullPointerException("id cannot be null");
    }
    if (language == null) {
      throw new NullPointerException("language cannot be null");
    }
    if (elements == null) {
      throw new NullPointerException("elements cannot be null");
    }
    if (description == null) {
      throw new NullPointerException("description cannot be null");
    }
    if (disamb == null && posSelect == null
        && disambAction != DisambiguatorAction.UNIFY
        && disambAction != DisambiguatorAction.ADD
        && disambAction != DisambiguatorAction.REMOVE
        && disambAction != DisambiguatorAction.IMMUNIZE
        && disambAction != DisambiguatorAction.REPLACE
        && disambAction != DisambiguatorAction.FILTERALL) {
      throw new NullPointerException("disambiguated POS cannot be null");
    }    
    this.disambiguatedPOS = disamb;
    this.matchElement = posSelect;
    this.disAction = disambAction;
    this.unifier = language.getDisambiguationUnifier();
  }
  
  /**
   * Used to add new interpretations.
   * 
   * @param newReadings
   *          An array of AnalyzedTokens. The length of the array should be the
   *          same as the number of the tokens matched and selected by
   *          mark/mark_from & mark_to attributes (>1).
   */
  public final void setNewInterpretations(final AnalyzedToken[] newReadings) {
    newTokenReadings = newReadings.clone();
  }

  /**
   * Performs disambiguation on the source sentence.
   * 
   * @param text {@link AnalyzedSentence} Sentence to be disambiguated.
   * @return {@link AnalyzedSentence} Disambiguated sentence (might be unchanged).
   */
  public final AnalyzedSentence replace(final AnalyzedSentence text)
      throws IOException {
    final AnalyzedTokenReadings[] tokens = text.getTokensWithoutWhitespace(); 
    AnalyzedTokenReadings[] whTokens = text.getTokens();
    final int[] tokenPositions = new int[tokens.length + 1];
    final int patternSize = patternElements.size();
    final int limit = Math.max(0, tokens.length - patternSize + 1);
    Element elem = null;
    boolean changed = false;
    for (int i = 0; i < limit && !(sentStart && i > 0); i++) {
      boolean allElementsMatch = false;
      unifiedTokens = null;
      int matchingTokens = 0;
      int skipShiftTotal = 0;
      int firstMatchToken = -1;
      int prevSkipNext = 0;
      if (testUnification) {
        unifier.reset();
      }
      for (int k = 0; k < patternSize; k++) {
        final Element prevElement = elem;
        elem = patternElements.get(k);
        setupRef(firstMatchToken, elem, tokens);        
        final int nextPos = i + k + skipShiftTotal;
        prevMatched = false;
        if (prevSkipNext + nextPos >= tokens.length || prevSkipNext < 0) { // SENT_END?
          prevSkipNext = tokens.length - (nextPos + 1);
        }
        final int maxTok = Math.min(nextPos + prevSkipNext, tokens.length - (patternSize - k));
        for (int m = nextPos; m <= maxTok; m++) {       
          allElementsMatch = testAllReadings(tokens, elem, prevElement, m,
              firstMatchToken, prevSkipNext);
          if (allElementsMatch) {
            final int skipShift = m - nextPos;
            tokenPositions[matchingTokens] = skipShift + 1;
            prevSkipNext = elem.getSkipNext();
            matchingTokens++;
            skipShiftTotal += skipShift;
            if (firstMatchToken == -1) {
              firstMatchToken = m;
            }
            break;
          }
        }
        if (!allElementsMatch) {
          break;
        }
      }
      if (allElementsMatch && matchingTokens == patternSize) {
        whTokens = executeAction(text, whTokens, unifiedTokens,
            firstMatchToken, matchingTokens, tokenPositions);
        changed = true;
      }
    }
    if (changed) {
      return new AnalyzedSentence(whTokens, text.getWhPositions());
    }
    return text;
  }

  private AnalyzedTokenReadings[] executeAction(final AnalyzedSentence text,
                                                final AnalyzedTokenReadings[] whiteTokens,
                                                final AnalyzedTokenReadings[] unifiedTokens, final int firstMatchToken,
                                                final int matchingTokens, final int[] tokenPositions) {
    final AnalyzedTokenReadings[] whTokens = whiteTokens.clone();
    int correctedStPos = 0;
    if (startPositionCorrection > 0) {
      for (int l = 0; l <= startPositionCorrection; l++) {
        correctedStPos += tokenPositions[l];
      }
      correctedStPos--;
    }
    final int fromPos = text.getOriginalPosition(firstMatchToken + correctedStPos);
    final int numRead = whTokens[fromPos].getReadingsLength();
    final boolean spaceBefore = whTokens[fromPos].isWhitespaceBefore();
    boolean filtered = false;
    switch (disAction) {
      case UNIFY:
        if (unifiedTokens != null) {
          if (unifiedTokens.length == matchingTokens - startPositionCorrection
                  + endPositionCorrection) {
            if (whTokens[text.getOriginalPosition(firstMatchToken + correctedStPos
                    + unifiedTokens.length - 1)].isSentEnd()) {
              unifiedTokens[unifiedTokens.length - 1].setSentEnd();
            }
            for (int i = 0; i < unifiedTokens.length; i++) {
              final int position = text.getOriginalPosition(firstMatchToken + correctedStPos
                      + i);
              unifiedTokens[i].setStartPos(whTokens[position].getStartPos());
              final String prevValue = whTokens[position].toString();
              final String prevAnot = whTokens[position].getHistoricalAnnotations();
              whTokens[position] = unifiedTokens[i];
              annotateChange(whTokens[position], prevValue, prevAnot);
            }
          }
        }
        break;
      case REMOVE:
        if (newTokenReadings != null) {
          if (newTokenReadings.length == matchingTokens - startPositionCorrection
                  + endPositionCorrection) {
            for (int i = 0; i < newTokenReadings.length; i++) {
              final int position = text.getOriginalPosition(firstMatchToken + correctedStPos + i);
              final String prevValue = whTokens[position].toString();
              final String prevAnot = whTokens[position].getHistoricalAnnotations();
              whTokens[position].removeReading(newTokenReadings[i]);
              annotateChange(whTokens[position], prevValue, prevAnot);
            }
          }
        }
        break;
      case ADD:
        if (newTokenReadings != null) {
          if (newTokenReadings.length == matchingTokens - startPositionCorrection
                  + endPositionCorrection) {
            String lemma = "";
            String token = "";
            for (int i = 0; i < newTokenReadings.length; i++) {
              final int position = text.getOriginalPosition(firstMatchToken + correctedStPos
                      + i);
              if ("".equals(newTokenReadings[i].getToken())) { //empty token 
                token = whTokens[position].getToken();
              } else {
                token = newTokenReadings[i].getToken();
              }
              if (newTokenReadings[i].getLemma() == null) { //empty lemma
                lemma = token;
              } else {
                lemma = newTokenReadings[i].getLemma();
              }
              final AnalyzedToken newTok = new AnalyzedToken(token, newTokenReadings[i].getPOSTag(), lemma);

              final String prevValue = whTokens[position].toString();
              final String prevAnot = whTokens[position].getHistoricalAnnotations();
              whTokens[position].addReading(newTok);
              annotateChange(whTokens[position], prevValue, prevAnot);
            }
          }
        }
        break;
      case FILTERALL:
        for (int i = 0; i < matchingTokens - startPositionCorrection
                + endPositionCorrection; i++) {
          final int position = text.getOriginalPosition(firstMatchToken
                  + correctedStPos + i);
          final Element myEl = patternElements.get(i+startPositionCorrection);
          final Match tmpMatchToken = new Match(myEl.getPOStag(), null,
                  true, myEl.getPOStag(), //myEl.isPOStagRegularExpression()
                  null, Match.CaseConversion.NONE, false, false,
                  Match.IncludeRange.NONE);
          tmpMatchToken.setToken(whTokens[position]);
          final String prevValue = whTokens[position].toString();
          final String prevAnot = whTokens[position]
                  .getHistoricalAnnotations();
          whTokens[position] = tmpMatchToken.filterReadings();
          annotateChange(whTokens[position], prevValue, prevAnot);
        }
        break;
      case IMMUNIZE:
        for (int i = 0; i < matchingTokens - startPositionCorrection + endPositionCorrection; i++) {
          whTokens[text.getOriginalPosition(firstMatchToken + correctedStPos + i)].immunize();
        }
      case FILTER:
        if (matchElement == null) { // same as REPLACE if using <match>
          final Match tmpMatchToken = new Match(disambiguatedPOS, null, true,
                  disambiguatedPOS, null, Match.CaseConversion.NONE,
                  false, false, Match.IncludeRange.NONE);
          tmpMatchToken.setToken(whTokens[fromPos]);
          final String prevValue = whTokens[fromPos].toString();
          final String prevAnot = whTokens[fromPos].getHistoricalAnnotations();
          whTokens[fromPos] = tmpMatchToken.filterReadings();
          annotateChange(whTokens[fromPos], prevValue, prevAnot);
          filtered = true;
        }
      case REPLACE:
      default:
        if (!filtered) {
          if (newTokenReadings != null && newTokenReadings.length > 0) {
            if (newTokenReadings.length == matchingTokens - startPositionCorrection + endPositionCorrection) {
              String lemma = "";
              String token = "";
              for (int i = 0; i < newTokenReadings.length; i++) {
                final int position = text.getOriginalPosition(firstMatchToken + correctedStPos
                        + i);
                if ("".equals(newTokenReadings[i].getToken())) { //empty token 
                  token = whTokens[position].getToken();
                } else {
                  token = newTokenReadings[i].getToken();
                }
                if (newTokenReadings[i].getLemma() == null) { //empty lemma
                  lemma = token;
                } else {
                  lemma = newTokenReadings[i].getLemma();
                }
                final AnalyzedTokenReadings toReplace = new AnalyzedTokenReadings(
                        new AnalyzedToken(token, newTokenReadings[i].getPOSTag(), lemma),
                        whTokens[fromPos].getStartPos());
                whTokens[position] = replaceTokens(whTokens[position], toReplace);
              }
            }
          } else if (matchElement == null) {
            String lemma = "";
            for (int l = 0; l < numRead; l++) {
              if (whTokens[fromPos].getAnalyzedToken(l).getPOSTag() != null
                      && (whTokens[fromPos].getAnalyzedToken(l).getPOSTag().equals(
                      disambiguatedPOS) && (whTokens[fromPos].getAnalyzedToken(l)
                      .getLemma() != null))) {
                lemma = whTokens[fromPos].getAnalyzedToken(l).getLemma();
              }
            }
            if (StringTools.isEmpty(lemma)) {
              lemma = whTokens[fromPos].getAnalyzedToken(0).getLemma();
            }

            final AnalyzedTokenReadings toReplace = new AnalyzedTokenReadings(
                    new AnalyzedToken(whTokens[fromPos].getToken(), disambiguatedPOS,
                            lemma), whTokens[fromPos].getStartPos());
            whTokens[fromPos] = replaceTokens(whTokens[fromPos], toReplace);
          } else {
            // using the match element
            matchElement.setToken(whTokens[fromPos]);
            final String prevValue = whTokens[fromPos].toString();
            final String prevAnot = whTokens[fromPos].getHistoricalAnnotations();
            whTokens[fromPos] = matchElement.filterReadings();
            whTokens[fromPos].setWhitespaceBefore(spaceBefore);
            annotateChange(whTokens[fromPos], prevValue, prevAnot);
          }
        }

    }
    return whTokens;
  }

  private AnalyzedTokenReadings replaceTokens(AnalyzedTokenReadings oldAtr, final AnalyzedTokenReadings newAtr) {
    final String prevValue = oldAtr.toString();
    final String prevAnot = oldAtr.getHistoricalAnnotations();
    final boolean isSentEnd = oldAtr.isSentEnd();
    final boolean isParaEnd = oldAtr.isParaEnd();
    final boolean spaceBefore = oldAtr.isWhitespaceBefore();
    final int startPosition = oldAtr.getStartPos();
    if (isSentEnd) {
      newAtr.setSentEnd();
    }
    if (isParaEnd) {
      newAtr.setParaEnd();
    }
    newAtr.setWhitespaceBefore(spaceBefore);
    newAtr.setStartPos(startPosition);
    annotateChange(newAtr, prevValue, prevAnot);
    return newAtr;
  }

  private void annotateChange(AnalyzedTokenReadings atr, final String prevValue, String prevAnot) {
    atr.setHistoricalAnnotations(prevAnot + "\n" +
            this.getId() + ":" + this.getSubId() + " " + prevValue + " -> " + atr.toString());
  }
  
  /**
   * @param examples the examples to set
   */
  public void setExamples(final List<DisambiguatedExample> examples) {
    this.examples = examples;
  }

  /**
   * @return the examples
   */
  public List<DisambiguatedExample> getExamples() {
    return examples;
  }

  /**
   * @param untouchedExamples the untouchedExamples to set
   */
  public void setUntouchedExamples(final List<String> untouchedExamples) {
    this.untouchedExamples = untouchedExamples;
  }

  /**
   * @return the untouchedExamples
   */
  public List<String> getUntouchedExamples() {
    return untouchedExamples;
  }
  
  /**
   * For testing only.
   */
  public final List<Element> getElements() {
    return patternElements;
  }


}
