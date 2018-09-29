/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.issue;

import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;


public class SetSeverityAction extends Action implements ServerComponent {

  public static final String SET_SEVERITY_ACTION_KEY = "set_severity";

  private final IssueUpdater issueUpdater;

  public SetSeverityAction(IssueUpdater issueUpdater) {
    super(SET_SEVERITY_ACTION_KEY);
    this.issueUpdater = issueUpdater;
    super.setConditions(new IsUnResolved());
  }

  @Override
  public boolean verify(Map<String, Object> properties, List<Issue> issues, UserSession userSession) {
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    return issueUpdater.setManualSeverity((DefaultIssue) context.issue(), severity(properties), context.issueChangeContext());
  }

  private String severity(Map<String, Object> properties) {
    return (String) properties.get("severity");
  }
}