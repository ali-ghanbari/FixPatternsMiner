package de.benjaminborbe.analytics.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.inject.Inject;

import de.benjaminborbe.analytics.api.AnalyticsReportAggregation;
import de.benjaminborbe.analytics.api.AnalyticsReportIdentifier;
import de.benjaminborbe.api.ValidationError;
import de.benjaminborbe.tools.validation.ValidationConstraintValidator;
import de.benjaminborbe.tools.validation.ValidatorBase;
import de.benjaminborbe.tools.validation.ValidatorRule;
import de.benjaminborbe.tools.validation.constraint.ValidationConstraint;
import de.benjaminborbe.tools.validation.constraint.ValidationConstraintNotNull;
import de.benjaminborbe.tools.validation.constraint.ValidationConstraintStringMaxLength;
import de.benjaminborbe.tools.validation.constraint.ValidationConstraintStringMinLength;

public class AnalyticsReportValidator extends ValidatorBase<AnalyticsReportBean> {

	private final class ValidationConstrainAllowedCharacters implements ValidationConstraint<String> {

		@Override
		public boolean precondition(final String object) {
			return object != null;
		}

		@Override
		public boolean validate(final String object) {
			for (final char c : object.toCharArray()) {
				if (!Character.isLetterOrDigit(c) || c == AnalyticsReportDao.SEPERATOR) {
					return false;
				}
			}
			return true;
		}
	}

	private final ValidationConstraintValidator validationConstraintValidator;

	@Inject
	public AnalyticsReportValidator(final ValidationConstraintValidator validationConstraintValidator) {
		this.validationConstraintValidator = validationConstraintValidator;
	}

	@Override
	public Class<AnalyticsReportBean> getType() {
		return AnalyticsReportBean.class;
	}

	@Override
	protected Map<String, ValidatorRule<AnalyticsReportBean>> buildRules() {
		final Map<String, ValidatorRule<AnalyticsReportBean>> result = new HashMap<String, ValidatorRule<AnalyticsReportBean>>();

		// id
		{
			final String field = "id";
			result.put(field, new ValidatorRule<AnalyticsReportBean>() {

				@Override
				public Collection<ValidationError> validate(final AnalyticsReportBean bean) {
					final AnalyticsReportIdentifier value = bean.getId();
					final List<ValidationConstraint<AnalyticsReportIdentifier>> constraints = new ArrayList<ValidationConstraint<AnalyticsReportIdentifier>>();
					constraints.add(new ValidationConstraintNotNull<AnalyticsReportIdentifier>());
					return validationConstraintValidator.validate(field, value, constraints);
				}
			});
		}

		// aggregation
		{
			final String field = "aggregation";
			result.put(field, new ValidatorRule<AnalyticsReportBean>() {

				@Override
				public Collection<ValidationError> validate(final AnalyticsReportBean bean) {
					final AnalyticsReportAggregation value = bean.getAggregation();
					final List<ValidationConstraint<AnalyticsReportAggregation>> constraints = new ArrayList<ValidationConstraint<AnalyticsReportAggregation>>();
					constraints.add(new ValidationConstraintNotNull<AnalyticsReportAggregation>());
					return validationConstraintValidator.validate(field, value, constraints);
				}
			});
		}

		// name
		{
			final String field = "name";
			result.put(field, new ValidatorRule<AnalyticsReportBean>() {

				@Override
				public Collection<ValidationError> validate(final AnalyticsReportBean bean) {
					final String value = bean.getName();
					final List<ValidationConstraint<String>> constraints = new ArrayList<ValidationConstraint<String>>();
					constraints.add(new ValidationConstraintNotNull<String>());
					constraints.add(new ValidationConstraintStringMinLength(1));
					constraints.add(new ValidationConstraintStringMaxLength(255));
					constraints.add(new ValidationConstrainAllowedCharacters());
					return validationConstraintValidator.validate(field, value, constraints);
				}
			});
		}

		return result;
	}

}
