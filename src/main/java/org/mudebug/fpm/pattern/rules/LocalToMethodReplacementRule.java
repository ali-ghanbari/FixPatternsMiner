package org.mudebug.fpm.pattern.rules;

public class LocalToMethodReplacementRule implements Rule {
    private final String localName;
    private final String calleeName;

    public LocalToMethodReplacementRule(String localName, String calleeName) {
        this.localName = localName;
        this.calleeName = calleeName;
    }
}
