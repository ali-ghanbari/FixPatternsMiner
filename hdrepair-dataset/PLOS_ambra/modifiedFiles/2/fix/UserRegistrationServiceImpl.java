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

import org.ambraproject.models.UserProfile;
import org.ambraproject.service.hibernate.HibernateServiceImpl;
import org.ambraproject.service.mailer.AmbraMailer;
import org.ambraproject.service.password.PasswordDigestService;
import org.ambraproject.util.TokenGenerator;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Alex Kudlick 9/24/12
 */
public class UserRegistrationServiceImpl extends HibernateServiceImpl implements UserRegistrationService {
  private static final Logger log = LoggerFactory.getLogger(UserRegistrationServiceImpl.class);

  private AmbraMailer ambraMailer;

  private PasswordDigestService passwordDigestService;

  @Required
  public void setAmbraMailer(AmbraMailer ambraMailer) {
    this.ambraMailer = ambraMailer;
  }

  @Required
  public void setPasswordDigestService(PasswordDigestService passwordDigestService) {
    this.passwordDigestService = passwordDigestService;
  }

  /**
   * {@inheritDoc}
   *
   * @param userProfile
   * @param password
   */
  @Override
  @Transactional
  public Long registerUser(UserProfile userProfile, String password) throws DuplicateUserException {
    int existingUserCount = DataAccessUtils.intResult(
        hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(UserProfile.class)
                .add(Restrictions.eq("email", userProfile.getEmail()))
                .setProjection(Projections.count("email")))
    );
    if (existingUserCount > 0) {
      throw new DuplicateUserException(DuplicateUserException.Field.EMAIL);
    }

    existingUserCount = DataAccessUtils.intResult(
        hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(UserProfile.class)
                .add(Restrictions.eq("displayName", userProfile.getDisplayName()))
                .setProjection(Projections.count("displayName")))
    );
    if (existingUserCount > 0) {
      throw new DuplicateUserException(DuplicateUserException.Field.DISPLAY_NAME);
    }

    try {
      log.debug("Registering new user with email: {}", userProfile.getEmail());
      userProfile.setPassword(passwordDigestService.generateDigest(password));
      Long id = (Long) hibernateTemplate.save(userProfile);
      ambraMailer.sendVerificationEmail(userProfile.getEmail(), userProfile.getVerificationToken());
      return id;
    } catch (DataIntegrityViolationException e) {
      throw new IllegalArgumentException("Didn't provide required field for user profile", e);
    }
  }

  @Override
  public void verifyUser(String email, String verificationToken) throws NoSuchUserException,
    UserAlreadyVerifiedException, VerificationTokenException {
    if (StringUtils.isEmpty(email)) {
      throw new IllegalArgumentException("Must supply an email");
    }
    if (StringUtils.isEmpty(verificationToken)) {
      throw new IllegalArgumentException("Must supply a verification token");
    }
    UserProfile profile = (UserProfile) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(UserProfile.class)
            .add(Restrictions.eq("email", email)))
    );
    if (profile == null) {
      throw new NoSuchUserException("No user with email: " + email);
    } else if (profile.getVerified()) {
      throw new UserAlreadyVerifiedException("User " + email + " was already verified");
    } else if (!verificationToken.equals(profile.getVerificationToken())) {
      throw new VerificationTokenException("An invalid verification token was given for this user");
    }
    log.debug("Verifying user {}", email);
    profile.setVerified(true);
    hibernateTemplate.update(profile);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public String resendVerificationEmail(String email) throws NoSuchUserException, UserAlreadyVerifiedException {
    if (StringUtils.isEmpty(email)) {
      throw new IllegalArgumentException("Must supply an email");
    }

    UserProfile profile = (UserProfile) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(UserProfile.class)
                .add(Restrictions.eq("email", email)))
    );
    if (profile == null) {
      throw new NoSuchUserException("No user with the email: " + email);
    }
    if (profile.getVerified()) {
      throw new UserAlreadyVerifiedException();
    }

    profile.setVerificationToken(TokenGenerator.getUniqueToken());
    hibernateTemplate.update(profile);

    log.debug("Resending verification email to {}", email);
    ambraMailer.sendVerificationEmail(email, profile.getVerificationToken());

    return profile.getVerificationToken();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public String sendForgotPasswordMessage(String email) throws NoSuchUserException {
    if (StringUtils.isEmpty(email)) {
      throw new IllegalArgumentException("Must supply an email");
    }
    UserProfile profile = (UserProfile) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(UserProfile.class)
                .add(Restrictions.eq("email", email))
                .add(Restrictions.eq("verified", true))));
    if (profile == null) {
      throw new NoSuchUserException("No user with the email: " + email);
    }
    log.debug("Sending forgotten newPassword message to {}", email);
    String passwordResetToken = TokenGenerator.getUniqueToken();
    profile.setVerificationToken(passwordResetToken);
    hibernateTemplate.update(profile);
    ambraMailer.sendForgotPasswordEmail(email, passwordResetToken);

    return passwordResetToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(readOnly = true)
  public boolean validateVerificationToken(String email, String verificationToken) {
    if (StringUtils.isEmpty(email)) {
      throw new IllegalArgumentException("Must supply an email");
    }
    if (StringUtils.isEmpty(verificationToken)) {
      throw new IllegalArgumentException("Must supplay an verificationToken");
    }
    int count = DataAccessUtils.intResult(
        hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(UserProfile.class)
                .add(Restrictions.eq("email", email))
                .add(Restrictions.eq("verificationToken", verificationToken))
                .setProjection(Projections.count("email"))
        )
    );
    if (count > 1) {
      throw new IllegalStateException("More than one user with email: " + email +
          " and verification token: " + verificationToken);
    }

    return count == 1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public void removeVerificationToken(String email) {
    UserProfile profile = (UserProfile) DataAccessUtils.uniqueResult(
      hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(UserProfile.class)
          .add(Restrictions.eq("email", email))
      )
    );
    if (profile == null) {
      throw new IllegalArgumentException("Incorrect email: " + email);
    }

    log.debug("Resetting token for {}", email);

    profile.setVerificationToken(null);

    hibernateTemplate.update(profile);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public void resetPassword(final String email, final String newPassword) {
    for (Map.Entry<String, String> argument : new HashMap<String, String>() {{
      put("email", email);
      put("new password", newPassword);
    }}.entrySet()) {
      if (StringUtils.isEmpty(argument.getValue())) {
        throw new IllegalArgumentException("Must supply a(n) " + argument.getKey());
      }
    }

    UserProfile profile = (UserProfile) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(UserProfile.class)
                .add(Restrictions.eq("email", email)))
    );
    if (profile == null) {
      throw new IllegalArgumentException("Incorrect email: " + email);
    }

    log.debug("Setting new password for {}", email);
    profile.setPassword(passwordDigestService.generateDigest(newPassword));
    hibernateTemplate.update(profile);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public String sendEmailChangeMessage(final String oldEmail, final String newEmail, final String password)
      throws NoSuchUserException, DuplicateUserException {
    for (Map.Entry<String, String> argument : new HashMap<String, String>() {{
      put("old email", oldEmail);
      put("new email", newEmail);
      put("password", password);
    }}.entrySet()) {
      if (StringUtils.isEmpty(argument.getValue())) {
        throw new IllegalArgumentException("Must supply a(n) " + argument.getKey());
      }
    }

    UserProfile profile = (UserProfile) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(UserProfile.class)
                .add(Restrictions.eq("email", oldEmail))
        )
    );

    if (profile == null) {
      throw new NoSuchUserException("No user with the email: " + oldEmail);
    }

    boolean validPassword = passwordDigestService.verifyPassword(password, profile.getPassword());
    if (!validPassword) {
      throw new SecurityException("Invalid password");
    }

    int existingUserCount = DataAccessUtils.intResult(
      hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(UserProfile.class)
          .add(Restrictions.eq("email", newEmail).ignoreCase())
          .setProjection(Projections.count("email")))
    );
    if (existingUserCount > 0) {
      throw new DuplicateUserException(DuplicateUserException.Field.EMAIL);
    }

    log.debug("sending email change verification to {}", newEmail);
    profile.setVerificationToken(TokenGenerator.getUniqueToken());
    hibernateTemplate.update(profile);
    ambraMailer.sendChangeEmailNotice(oldEmail, newEmail, profile.getVerificationToken());

    return profile.getVerificationToken();
  }

  @Override
  @Transactional
  public void updateEmailAddress(final String oldEmail, final String newEmail, final String verificationToken) throws
    NoSuchUserException, VerificationTokenException, DuplicateUserException {
    for (Map.Entry<String, String> argument : new HashMap<String, String>() {{
      put("old email", oldEmail);
      put("new email", newEmail);
      put("verification token", verificationToken);
    }}.entrySet()) {
      if (StringUtils.isEmpty(argument.getValue())) {
        throw new IllegalArgumentException("Must supply a(n) " + argument.getKey());
      }
    }

    UserProfile profile = (UserProfile) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(UserProfile.class)
                .add(Restrictions.eq("email", oldEmail))
        )
    );

    if (profile == null) {
      throw new NoSuchUserException("No user with the email: " + oldEmail);
    } else if (!verificationToken.equals(profile.getVerificationToken())) {
      throw new VerificationTokenException("An invalid verification token was given for this user");
    }

    int existingUserCount = DataAccessUtils.intResult(
      hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(UserProfile.class)
          .add(Restrictions.eq("email", newEmail).ignoreCase())
          .setProjection(Projections.count("email")))
    );
    if (existingUserCount > 0) {
      throw new DuplicateUserException(DuplicateUserException.Field.EMAIL);
    }

    log.debug("Changing email for {} to {}", oldEmail, newEmail);
    profile.setEmail(newEmail);
    hibernateTemplate.update(profile);
  }
}
