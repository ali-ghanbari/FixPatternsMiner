package edu.utdallas.fpm.pattern.rules;

public class MethodGuardRule implements Rule {
    private final UsagePreference usagePreference;

    public MethodGuardRule(final UsagePreference usagePreference) {
        this.usagePreference = usagePreference;
    }

    @Override
    public String getId() {
        return String.format("%s (Using %s)",
                this.getClass().getSimpleName(),
                this.usagePreference.name());
    }
}
