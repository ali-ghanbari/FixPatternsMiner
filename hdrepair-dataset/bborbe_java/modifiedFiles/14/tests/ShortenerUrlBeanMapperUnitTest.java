package de.benjaminborbe.shortener.dao;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;

import com.google.inject.Provider;

import de.benjaminborbe.shortener.api.ShortenerUrlIdentifier;
import de.benjaminborbe.tools.date.CalendarUtil;
import de.benjaminborbe.tools.date.CalendarUtilImpl;
import de.benjaminborbe.tools.date.CurrentTime;
import de.benjaminborbe.tools.date.TimeZoneUtil;
import de.benjaminborbe.tools.date.TimeZoneUtilImpl;
import de.benjaminborbe.tools.mapper.MapperCalendar;
import de.benjaminborbe.tools.mapper.MapperString;
import de.benjaminborbe.tools.util.ParseUtil;
import de.benjaminborbe.tools.util.ParseUtilImpl;

public class ShortenerUrlBeanMapperUnitTest {

	private ShortenerUrlBeanMapper getShortenerUrlBeanMapper() {
		final Provider<ShortenerUrlBean> beanProvider = new Provider<ShortenerUrlBean>() {

			@Override
			public ShortenerUrlBean get() {
				return new ShortenerUrlBean();
			}
		};

		final Logger logger = EasyMock.createNiceMock(Logger.class);
		EasyMock.replay(logger);

		final TimeZoneUtil timeZoneUtil = new TimeZoneUtilImpl();
		final ParseUtil parseUtil = new ParseUtilImpl();

		final CurrentTime currentTime = EasyMock.createMock(CurrentTime.class);
		EasyMock.replay(currentTime);

		final CalendarUtil calendarUtil = new CalendarUtilImpl(logger, currentTime, parseUtil, timeZoneUtil);
		final MapperCalendar mapperCalendar = new MapperCalendar(timeZoneUtil, calendarUtil, parseUtil);
		final MapperString mapperString = new MapperString();
		final ShortenerUrlIdentifierMapper shortenerUrlIdentifierMapper = new ShortenerUrlIdentifierMapper();
		return new ShortenerUrlBeanMapper(beanProvider, mapperString, mapperCalendar, shortenerUrlIdentifierMapper);
	}

	@Test
	public void testId() throws Exception {
		final ShortenerUrlBeanMapper mapper = getShortenerUrlBeanMapper();
		final ShortenerUrlIdentifier value = new ShortenerUrlIdentifier("1337");
		final String fieldname = "id";
		{
			final ShortenerUrlBean bean = new ShortenerUrlBean();
			bean.setId(value);
			final Map<String, String> data = mapper.map(bean);
			assertEquals(data.get(fieldname), String.valueOf(value));
		}
		{
			final Map<String, String> data = new HashMap<String, String>();
			data.put(fieldname, String.valueOf(value));
			final ShortenerUrlBean bean = mapper.map(data);
			assertEquals(value, bean.getId());
		}
	}
}
