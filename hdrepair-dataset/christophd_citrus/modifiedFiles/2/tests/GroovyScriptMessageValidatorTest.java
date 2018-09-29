/*
 * Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.validation.script;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.testng.AbstractBaseTest;

/**
 * @author Christoph Deppisch
 */
public class GroovyScriptMessageValidatorTest extends AbstractBaseTest {

    private GroovyScriptMessageValidator validator = new GroovyScriptMessageValidator();
    
    private Message<?> message;

    @BeforeMethod
    public void prepareTestData() {
        message = MessageBuilder.withPayload("<RequestMessage Id=\"123456789\" xmlns=\"http://citrus/test\">"
                + "<CorrelationId>Kx1R123456789</CorrelationId>"
                + "<BookingId>Bx1G987654321</BookingId>"
                + "<Text>Hello TestFramework</Text>"
            + "</RequestMessage>").build();
    }
    
    @Test
    public void testGroovyScriptValidation() {
        String validationScript = "assert root.children().size() == 3 \n" +
                        "assert root.CorrelationId.text() == 'Kx1R123456789' \n" +
                        "assert root.BookingId.text() == 'Bx1G987654321' \n" +
                        "assert root.Text.text() == 'Hello TestFramework'";
                        
        ScriptValidationContext validationContext = new ScriptValidationContext(validationScript, context);
        
        validator.validateMessage(message, context, validationContext);
    }
    
    @Test
    public void testGroovyScriptValidationVariableSupport() {
        context.setVariable("user", "TestFramework");
        context.setVariable("correlationId", "Kx1R123456789");
        
        String validationScript = "assert root.children().size() == 3 \n" +
                        "assert root.CorrelationId.text() == '${correlationId}' \n" +
                        "assert root.BookingId.text() == 'Bx1G987654321' \n" +
                        "assert root.Text.text() == 'Hello ' + context.getVariable(\"user\")";
                        
        ScriptValidationContext validationContext = new ScriptValidationContext(validationScript, context);
        
        validator.validateMessage(message, context, validationContext);
    }
    
    @Test
    public void testGroovyScriptValidationFailed() {
        String validationScript = "assert root.children().size() == 3 \n" +
                        "assert root.CorrelationId.text() == 'Kx1R123456789' \n" +
                        "assert root.BookingId.text() == 'Bx1G987654321' \n" +
                        "assert root.Text == 'Hello Citrus'"; //should fail
                        
        ScriptValidationContext validationContext = new ScriptValidationContext(validationScript, context);
        
        try {
            validator.validateMessage(message, context, validationContext);
        } catch (CitrusRuntimeException e) {
            Assert.assertTrue(e.getMessage().startsWith("Groovy script validation failed"));
            Assert.assertTrue(e.getMessage().contains("Hello Citrus"));
            Assert.assertTrue(e.getMessage().contains("Hello TestFramework"));
            return;
        }
        
        Assert.fail("Missing script validation exception caused by wrong control value");
    }
    
    @Test
    public void testEmptyValidationScript() {
        String validationScript = "";
        ScriptValidationContext validationContext = new ScriptValidationContext(validationScript, context);
        
        validator.validateMessage(message, context, validationContext);
    }
}
