package edu.utdallas.fpm.pattern.rules;

public class RetMethodGuardRule implements Rule {
    public final UsagePreference usagePreference;

    public RetMethodGuardRule(final UsagePreference usagePreference) {
        this.usagePreference = usagePreference;
    }

    @Override
    public String getId() {
        return String.format("%s (Using %s)",
                this.getClass().getSimpleName(),
                this.usagePreference.name());
    }
}
