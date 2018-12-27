package edu.utdallas.fpm.pattern.rules;

public class CaseBreakerReturnInsertionRule implements Rule {
    private final UsagePreference usagePreference;

    public CaseBreakerReturnInsertionRule(final UsagePreference usagePreference) {
        this.usagePreference = usagePreference;
    }

    @Override
    public String getId() {
        final String upStr = this.usagePreference == UsagePreference.VOID ?
                "enclosing method" : this.usagePreference.name();
        return String.format("%s (Returning %s)", this.getClass().getSimpleName(), upStr);
    }
}
