/**
 * DocumentValidator
 * Copyright (c) 2013-, Takahiko Ito, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package org.unigram.docvalidator.validator.sentence;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.unigram.docvalidator.store.Sentence;
import org.unigram.docvalidator.util.ValidationError;
import org.unigram.docvalidator.validator.sentence.SpaceBegginingOfSentenceValidator;

public class SpaceBeginingOfStenceValidatorTest {

  @Test
  public void testProcessSetenceWithoutEndSpace() {
    SpaceBegginingOfSentenceValidator spaceValidator =
        new SpaceBegginingOfSentenceValidator();
    Sentence str = new Sentence("That is true.",0);
    List<ValidationError> errors = spaceValidator.check(str);
    assertNotNull(errors);
    assertEquals(1, errors.size());
  }

  @Test
  public void testProcessEndSpace() {
    SpaceBegginingOfSentenceValidator spaceValidator =
        new SpaceBegginingOfSentenceValidator();
    Sentence str = new Sentence(" That is true.",0);
    List<ValidationError> errors = spaceValidator.check(str);
    assertNotNull(errors);
    assertEquals(0, errors.size());
  }

  @Test
  public void testProcessHeadSentenceInAParagraph() {
    SpaceBegginingOfSentenceValidator spaceValidator =
        new SpaceBegginingOfSentenceValidator();
    Sentence str = new Sentence("That is true.",0);
    str.isStartaragraph = true;
    List<ValidationError> errors = spaceValidator.check(str);
    assertNotNull(errors);
    assertEquals(0, errors.size());
  }

  @Test
  public void testProcessZerorLengthSentence() {
    SpaceBegginingOfSentenceValidator spaceValidator =
        new SpaceBegginingOfSentenceValidator();
    Sentence str = new Sentence("",0);
    str.isStartaragraph = true;
    List<ValidationError> errors = spaceValidator.check(str);
    assertNotNull(errors);
    assertEquals(0, errors.size());
  }
}
