package de.benjaminborbe.task.gui.util;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;

import de.benjaminborbe.task.api.TaskContext;

public class TaskContextComparatorUnitTest {

	@Test
	public void testCompare() {
		final TaskContextComparator c = new TaskContextComparator();

		{
			final List<TaskContext> list = new ArrayList<TaskContext>();
			list.add(build("a"));
			list.add(build("B"));
			list.add(build("c"));
			list.add(build("D"));
			Collections.sort(list, c);
			assertEquals("a", list.get(0).getName());
			assertEquals("B", list.get(1).getName());
			assertEquals("c", list.get(2).getName());
			assertEquals("D", list.get(3).getName());
		}
	}

	private TaskContext build(final String string) {
		final TaskContext c = EasyMock.createMock(TaskContext.class);
		EasyMock.expect(c.getName()).andReturn(string).anyTimes();
		EasyMock.replay(c);
		return c;
	}
}
