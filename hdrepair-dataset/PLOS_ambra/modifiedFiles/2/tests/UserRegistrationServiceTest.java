/*
 * Copyright (c) 2007-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
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
package org.ambraproject.service.user;

import org.ambraproject.action.BaseTest;
import org.ambraproject.models.UserProfile;
import org.ambraproject.service.password.PasswordDigestService;
import org.ambraproject.testutils.DummyAmbraMailer;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Alex Kudlick 9/24/12
 */
public class UserRegistrationServiceTest extends BaseTest {

  @Autowired
  protected UserRegistrationService userRegistrationService;

  @Autowired
  protected DummyAmbraMailer dummyMailer;

  @Autowired
  protected PasswordDigestService passwordDigestService;


  @Test
  public void testRegisterUser() throws DuplicateUserException {
    int numSentEmails = dummyMailer.getEmailsSent().size();

    String password = "myCoolPass";
    UserProfile profile = new UserProfile("testRegisterUser@example.com", "testRegisterUser", password);

    Long userId = userRegistrationService.registerUser(profile, password);
    assertNotNull(userId, "returned null id");
    UserProfile storedUser = dummyDataStore.get(UserProfile.class, userId);
    assertNotNull(storedUser, "failed to save user profile");
    assertEquals(storedUser.getEmail(), profile.getEmail(), "stored user with incorrect email");

    assertNotNull(storedUser.getPassword(), "failed to store password");
    assertTrue(passwordDigestService.verifyPassword(password, storedUser.getPassword()),
        "failed to hash password");

    assertEquals(dummyMailer.getEmailsSent().size(), numSentEmails + 1, "failed to send verifiction email");
    DummyAmbraMailer.DummyEmail verificationEmail = dummyMailer.getEmailsSent().get(numSentEmails);
    assertEquals(verificationEmail.getToEmailAddresses(), profile.getEmail(),
        "sent verification email to incorrect address");
    assertEquals(verificationEmail.getContext().get("email"), profile.getEmail(), "Incorrect email in context");
    assertEquals(verificationEmail.getContext().get("verificationToken"), storedUser.getVerificationToken(),
        "Incorrect email verification token in context");
    assertFalse(storedUser.getVerified(), "Stored user object was already verified");
  }


  @Test()
  public void testRegisterUserWithDuplicateEmail() throws DuplicateUserException {
    userRegistrationService.registerUser(
        new UserProfile("registerWithDuplicate@example.com", "registerWithDuplicate", "pass"), "pass");
    try {
      userRegistrationService.registerUser(
          new UserProfile("registerWithDuplicate@example.com", "registerWithDuplicate", "pass"), "pass");
      fail("should have thrown duplicate user exception");
    } catch (DuplicateUserException e) {
      assertEquals(e.getField(), DuplicateUserException.Field.EMAIL,
          "didn't throw duplicate user exception with appropriate field");
    }
  }

  @Test
  public void testResendVerificationEmail() throws DuplicateUserException, UserAlreadyVerifiedException, NoSuchUserException {
    UserProfile profile = new UserProfile("testResendVerification@test.org", "resendVerificationEmail@example.com", "pass");
    dummyDataStore.store(profile);

    String oldVerificationToken = profile.getVerificationToken();

    int numSentEmails = dummyMailer.getEmailsSent().size();
    userRegistrationService.resendVerificationEmail(profile.getEmail());

    String verificationToken = dummyDataStore.get(UserProfile.class, profile.getID()).getVerificationToken();
    assertFalse(verificationToken.equals(oldVerificationToken),
        "failed to change verification token");

    assertEquals(dummyMailer.getEmailsSent().size(), numSentEmails + 1, "failed to resend verifiction email");
    DummyAmbraMailer.DummyEmail verificationEmail = dummyMailer.getEmailsSent().get(numSentEmails);
    assertEquals(verificationEmail.getToEmailAddresses(), profile.getEmail(),
        "sent verification email to incorrect address");
    assertEquals(verificationEmail.getContext().get("email"), profile.getEmail(), "Incorrect email in context");
    assertEquals(verificationEmail.getContext().get("verificationToken"), verificationToken,
        "Incorrect email verification token in context");
  }

  @Test(expectedExceptions = {UserAlreadyVerifiedException.class})
  public void testResendToVerifiedUser() throws DuplicateUserException, UserAlreadyVerifiedException, NoSuchUserException {
    String email = "resendToVerifiedUser@test.org";
    UserProfile profile = new UserProfile(email, "testResendToVerifiedUser", "pass");
    profile.setVerified(true);
    dummyDataStore.store(profile);

    userRegistrationService.resendVerificationEmail(email);
  }

  @Test(expectedExceptions = {NoSuchUserException.class})
  public void testResendToNonexistentUser() throws UserAlreadyVerifiedException, NoSuchUserException {
    userRegistrationService.resendVerificationEmail("NonExistentEmail@example.com");
  }

  @Test
  public void testSendForgotPasswordMessage() throws Exception {
    UserProfile profile = new UserProfile(
        "sendForgotPasswordMessage@test.org",
        "testSendForgotPasswordMessage",
        "pass");

    profile.setVerified(true);

    Long id = Long.valueOf(dummyDataStore.store(profile));
    String oldEmailVerificationToken = profile.getVerificationToken();
    int numSentEmails = dummyMailer.getEmailsSent().size();

    userRegistrationService.sendForgotPasswordMessage(profile.getEmail());

    profile = dummyDataStore.get(UserProfile.class, id);
    assertNotNull(profile.getVerificationToken(), "nulled out email verification token");
    assertFalse(profile.getVerificationToken().equalsIgnoreCase(oldEmailVerificationToken),
        "Failed to set new email verification token");
    assertEquals(dummyMailer.getEmailsSent().size(), numSentEmails + 1, "failed to send an email");
    DummyAmbraMailer.DummyEmail passwordEmail = dummyMailer.getEmailsSent().get(numSentEmails);
    assertEquals(passwordEmail.getToEmailAddresses(), profile.getEmail(), "sent email to incorrect email");
    assertEquals(passwordEmail.getContext().get("email"), profile.getEmail(), "sent incorrect email in template");
    assertEquals(passwordEmail.getContext().get("verificationToken"), profile.getVerificationToken(),
        "sent incorrect email verification token");
  }

  @Test(expectedExceptions = NoSuchUserException.class)
  public void testSendForgotPasswordMessageNotVerified() throws Exception {
    UserProfile profile = new UserProfile(
      "sendForgotPasswordMessage2@test.org",
      "testSendForgotPasswordMessage2",
      "pass2");

    dummyDataStore.store(profile);

    userRegistrationService.sendForgotPasswordMessage(profile.getEmail());
  }

  @Test
  public void testValidateEmailVerificationToken() throws Exception {
    UserProfile profile1 = new UserProfile("validateEmailToken@test.org", "validateEmail1", "pass");
    dummyDataStore.store(profile1);
    UserProfile profile2 = new UserProfile("validateEmailToken2@test.org", "validateEmail2", "pass");
    dummyDataStore.store(profile2);

    assertTrue(userRegistrationService.validateVerificationToken(
        profile1.getEmail(),
        profile1.getVerificationToken()),
        "failed to correctly verify token");

    assertFalse(userRegistrationService.validateVerificationToken(
        profile2.getEmail(),
        profile1.getVerificationToken()),
        "should return false for non-matching email and token");

    assertFalse(userRegistrationService.validateVerificationToken(
        "nonExistent@email.gov",
        profile2.getVerificationToken()),
        "should return false for non-existent email");

    assertFalse(userRegistrationService.validateVerificationToken(
        profile2.getEmail(),
        "fakeVerificationToken"),
        "should return false for non-existent verification token");
  }

  @Test
  public void testResetPassword() throws Exception {
    UserProfile profile = new UserProfile("resetPassword@example.com", "resetPassword", "testPass");
    dummyDataStore.store(profile);

    String passwordToSet = "a new password";
    userRegistrationService.resetPassword(profile.getEmail(), passwordToSet);

    String storedPassword = dummyDataStore.get(UserProfile.class, profile.getID()).getPassword();
    assertFalse(storedPassword.equalsIgnoreCase(profile.getPassword()), "failed to change password");
    assertTrue(passwordDigestService.verifyPassword(passwordToSet, storedPassword), "failed to hash new password");
  }

  @Test
  public void testVerfiyUser() throws Exception {
    UserProfile profile = new UserProfile("verifyUser@example.com", "verifyUser", "pass");
    dummyDataStore.store(profile);

    userRegistrationService.verifyUser(profile.getEmail(), profile.getVerificationToken());
    assertTrue(dummyDataStore.get(UserProfile.class, profile.getID()).getVerified(),
        "failed to verify user");
    try {
      userRegistrationService.verifyUser(profile.getEmail(), profile.getVerificationToken());
      fail("Should have thrown UserAlreadyVerifiedException");
    } catch (UserAlreadyVerifiedException e) {
      //expected
    }

    try {
      userRegistrationService.verifyUser("nonExistent@user.org", profile.getVerificationToken());
      fail("Should have thrown NoSuchUserException");
    } catch (NoSuchUserException e) {
      //expected
    }
  }

  @Test
  public void testSendChangeEmailNotice() throws Exception {
    String password = "pass";
    int numSentEmails = dummyMailer.getEmailsSent().size();
    UserProfile profile = new UserProfile("changeEmailMessage@example.org",
        "changeEmailMessage",
        passwordDigestService.generateDigest(password));
    dummyDataStore.store(profile);
    String oldVerificationToken = profile.getVerificationToken();

    String newEmail = "newEmail1@example.org";
    userRegistrationService.sendEmailChangeMessage(profile.getEmail(), newEmail, password);

    assertFalse(dummyDataStore.get(UserProfile.class, profile.getID())
        .getVerificationToken().equalsIgnoreCase(oldVerificationToken),
        "failed to change verification token");
    assertEquals(dummyMailer.getEmailsSent().size(), numSentEmails + 1, "failed to send message");
    assertEquals(dummyMailer.getEmailsSent().get(numSentEmails).getToEmailAddresses(), newEmail,
        "sent email to incorrect address");
  }

  @Test(expectedExceptions = {SecurityException.class})
  public void testSendChangeEmailNoticeWithInvalidPassword() throws Exception {
    UserProfile profile = new UserProfile("invalidPassword@example.org",
        "invalidPassword",
        passwordDigestService.generateDigest("pass"));
    dummyDataStore.store(profile);

    userRegistrationService.sendEmailChangeMessage(profile.getEmail(), "foo", "badPass");
  }

  @Test
  public void testChangeEmail() throws Exception {
    UserProfile profile = new UserProfile("changeEmail@example.org", "testChangeEmail", "pass");
    dummyDataStore.store(profile);

    UserProfile userProfile = new UserProfile(
        profile.getEmail(),
        "displayNameChangeEmail", "pass");
    dummyDataStore.store(userProfile);

    String newEmail = "newEmail@example.org";
    userRegistrationService.updateEmailAddress(profile.getEmail(), newEmail,
        profile.getVerificationToken());

    assertEquals(dummyDataStore.get(UserProfile.class, profile.getID()).getEmail(), newEmail,
        "failed to update email");
  }

  @Test(expectedExceptions = { DuplicateUserException.class })
  public void testSendChangeEmailDupe() throws Exception {
    dummyDataStore.store(new UserProfile("test@duplicate.org", "testChangeDupeEmail",
      passwordDigestService.generateDigest("pass")));

    UserProfile profile = new UserProfile("test1@duplicate.org", "test1ChangeDupeEmail",
      passwordDigestService.generateDigest("pass1"));
    dummyDataStore.store(profile);

    userRegistrationService.sendEmailChangeMessage(profile.getEmail(), "test@duplicate.org", "pass1");
  }

  @Test(expectedExceptions = { DuplicateUserException.class })
  public void testChangeEmailDupe() throws Exception {
    dummyDataStore.store(new UserProfile("test@duplicate1.org", "testChangeDupe1Email",
      passwordDigestService.generateDigest("pass")));

    UserProfile profile = new UserProfile("test1@duplicate1.org", "test1ChangeDupe1Email",
      passwordDigestService.generateDigest("pass1"));
    dummyDataStore.store(profile);

    userRegistrationService.updateEmailAddress(profile.getEmail(), "test@duplicate1.org", profile.getVerificationToken());
  }
}
