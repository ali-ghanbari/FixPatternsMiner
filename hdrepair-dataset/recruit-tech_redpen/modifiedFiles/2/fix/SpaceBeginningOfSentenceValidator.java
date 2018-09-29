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
package cc.redpen.validator.sentence;

import cc.redpen.model.Sentence;
import cc.redpen.validator.ValidationError;
import cc.redpen.validator.Validator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Validate input sentences except for first sentence of a paragraph start with
 * a space.
 */
final public class SpaceBeginningOfSentenceValidator extends Validator {
    private Map<Integer, List<Sentence>> sentencePositions = new HashMap<>();

    private boolean isFistInLine(Sentence sentence) {
        return sentence.isFirstSentence || sentencePositions.get(sentence.position).get(0) == sentence;
    }

    @Override
    public void validate(List<ValidationError> errors, Sentence sentence) {
        String content = sentence.content;
        if (!isFistInLine(sentence) && content.length() > 0 && content.charAt(0) != ' ') {
            errors.add(createValidationError(sentence));
        }
    }

    @Override
    public void preValidate(Sentence sentence) {
        if (!sentencePositions.containsKey(sentence.position)) {
            sentencePositions.put(sentence.position, new LinkedList<>());
        }
        List<Sentence> list = sentencePositions.get(sentence.position);
        list.add(sentence);
    }

    @Override
    public String toString() {
        return "SpaceBeginningOfSentenceValidator{" +
                "sentencePositions=" + sentencePositions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpaceBeginningOfSentenceValidator that = (SpaceBeginningOfSentenceValidator) o;

        return !(sentencePositions != null ? !sentencePositions.equals(that.sentencePositions) : that.sentencePositions != null);

    }

    @Override
    public int hashCode() {
        return sentencePositions != null ? sentencePositions.hashCode() : 0;
    }
}
