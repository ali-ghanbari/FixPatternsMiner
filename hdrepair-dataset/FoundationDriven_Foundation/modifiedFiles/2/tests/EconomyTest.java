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
import io.foundationdriven.foundation.api.economy.errors.InvalidAmount;
import io.foundationdriven.foundation.api.economy.errors.InvalidGlobalValue;
import io.foundationdriven.foundation.api.economy.errors.NotEnoughMoney;
import io.foundationdriven.foundation.api.economy.managers.AccountManager;
import io.foundationdriven.foundation.api.economy.managers.CurrencyManager;
import io.foundationdriven.foundation.api.economy.objects.Account;
import io.foundationdriven.foundation.api.economy.objects.Currency;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the economy API
 */
public class EconomyTest {
    /**
     * Creates two currencies ( Waffles and pancakes ) for use throughout the tests as well as a test account
     * @see io.foundationdriven.foundation.api.economy.objects.Currency
     * @see io.foundationdriven.foundation.api.economy.managers.CurrencyManager
     */
    @BeforeClass
    public static void createCurrency(){
        Currency waffle = CurrencyManager.createCurrency("Waffle", "Waffles", 1);
        Currency pancake = CurrencyManager.createCurrency("Pancake", "Pancakes", 2);
        org.junit.Assert.assertSame("Waffle currency wasn't registered", CurrencyManager.getCurrency("Waffle"), waffle);
        org.junit.Assert.assertSame("Pancake currency wasn't registered", CurrencyManager.getCurrency("Pancake"), pancake);
        Account a = AccountManager.createAccount("test");
        org.junit.Assert.assertSame("test Account wasn't registered", AccountManager.getAccount("test"), a);
    }

    /**
     * Tries to register a currency with a negative globalValue (-1)
     * @throws io.foundationdriven.foundation.api.economy.errors.InvalidGlobalValue
     * @see io.foundationdriven.foundation.api.economy.objects.Currency
     * @see io.foundationdriven.foundation.api.economy.managers.CurrencyManager
     */
    @Test(expected = InvalidGlobalValue.class)
    public void testInvalidCurrencyCreation(){
        CurrencyManager.createCurrency("Invalid", "Invalids", -1);
    }

    /**
     * Sets the test account's waffle count to 0, gives it 100 and tries to give it -100
     * @throws io.foundationdriven.foundation.api.economy.errors.InvalidAmount
     * @see io.foundationdriven.foundation.api.economy.objects.Account
     */
    @Test(expected = InvalidAmount.class)
    public void testGiving() {
        Account a = AccountManager.getAccount("test");
        Currency c = CurrencyManager.getCurrency("Waffle");
        if (a == null) org.junit.Assert.fail("Could not find test account");
        else if (c == null) org.junit.Assert.fail("Could not find waffle currency");
        else {

            a.getAmounts().put(c, 0);
            a.give(c, 100);
            org.junit.Assert.assertEquals("When given 100 waffles, test account didn't have 100 waffles", ((Integer) 100), a.getAmount(c));
            a.give(c, -100);
        }
    }

    /**
     * Sets the test account's waffle count to 200, takes 50 and tries to take -50
     * @throws io.foundationdriven.foundation.api.economy.errors.InvalidAmount
     * @see io.foundationdriven.foundation.api.economy.objects.Account
     */
    @Test(expected = InvalidAmount.class)
    public void testTaking(){
        Account a = AccountManager.getAccount("test");
        Currency c = CurrencyManager.getCurrency("Waffle");
        if (a == null) org.junit.Assert.fail("Could not find test account");
        else if (c == null) org.junit.Assert.fail("Could not find waffle currency");
        else{

            a.getAmounts().put(c, 200);
            a.take(c, 50);
            org.junit.Assert.assertEquals("When given 200 waffles and 50 waffles were taken away, test Account didn't have 50 waffles", ((Integer)150), a.getAmount(c));
            a.take(c, (-50));
        }
    }

    /**
     * Sets the test account's waffle count to 100 and tries to take 200
     * @throws io.foundationdriven.foundation.api.economy.errors.NotEnoughMoney
     * @see io.foundationdriven.foundation.api.economy.objects.Account
     */
    @Test(expected = NotEnoughMoney.class)
    public void testTooMuchTake() {
        Account a = AccountManager.getAccount("test");
        Currency c = CurrencyManager.getCurrency("Waffle");
        if (a == null) org.junit.Assert.fail("Could not find test account");
        else if (c == null) org.junit.Assert.fail("Could not find waffle currency");
        else{
            a.getAmounts().put(c, 100);
            a.take(c, 200);
        }
    }

    /**
     * Purges and converts all waffles to pancakes then purges all pancakes and deletes the test account
     */
    @AfterClass
    public static void cleanup(){
        Account a = AccountManager.getAccount("test");
        Currency w = CurrencyManager.getCurrency("Waffle");
        Currency p = CurrencyManager.getCurrency("Pancake");
        if (a == null) org.junit.Assert.fail("Could not find test account");
        else if (w == null) org.junit.Assert.fail("Could not find waffle currency");
        else if (p == null) org.junit.Assert.fail("Could not find pancake currency");
        else{
            a.getAmounts().put(w, 100);
            CurrencyManager.deleteCurrency(w, p);
            org.junit.Assert.assertEquals("100 waffles should be 50 pancakes", ((Integer)a.getAmount(p)), ((Integer)50));
            CurrencyManager.deleteCurrency(p);
            org.junit.Assert.assertEquals("Amounts hashmap should be empty", a.getAmounts().size(), 0);
            AccountManager.unregisterAccount(a);
            org.junit.Assert.assertEquals("Should be no accounts left", AccountManager.getAccounts().size(), 0);
        }
    }
}