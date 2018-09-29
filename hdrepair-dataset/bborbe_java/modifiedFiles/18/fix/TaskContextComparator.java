package de.benjaminborbe.task.gui.util;

import de.benjaminborbe.task.api.TaskContext;
import de.benjaminborbe.tools.util.ComparatorBase;

public class TaskContextComparator extends ComparatorBase<TaskContext, String> {

	@Override
	public String getValue(final TaskContext o) {
		return o.getName() != null ? o.getName().toLowerCase() : o.getName();
	}
}
