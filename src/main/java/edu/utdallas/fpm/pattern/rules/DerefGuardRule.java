package edu.utdallas.fpm.pattern.rules;

public class DerefGuardRule implements Rule {
    private final UsagePreference usagePreference;

    public DerefGuardRule(final UsagePreference usagePreference) {
        this.usagePreference = usagePreference;
    }

    @Override
    public String getId() {
        return String.format("%s (Using %s)",
                this.getClass().getSimpleName(),
                this.usagePreference.name());
    }
}
