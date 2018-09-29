package de.benjaminborbe.shortener.dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.benjaminborbe.shortener.api.ShortenerUrlIdentifier;
import de.benjaminborbe.tools.mapper.MapperCalendar;
import de.benjaminborbe.tools.mapper.MapperString;
import de.benjaminborbe.tools.mapper.mapobject.MapObjectMapperAdapter;
import de.benjaminborbe.tools.mapper.stringobject.StringObjectMapper;
import de.benjaminborbe.tools.mapper.stringobject.StringObjectMapperAdapter;

@Singleton
public class ShortenerUrlBeanMapper extends MapObjectMapperAdapter<ShortenerUrlBean> {

	@Inject
	public ShortenerUrlBeanMapper(
			final Provider<ShortenerUrlBean> provider,
			final MapperString mapperString,
			final MapperCalendar mapperCalendar,
			final ShortenerUrlIdentifierMapper shortenerUrlIdentifierMapper) {
		super(provider, buildMappings(shortenerUrlIdentifierMapper, mapperString, mapperCalendar));
	}

	private static Collection<StringObjectMapper<ShortenerUrlBean>> buildMappings(final ShortenerUrlIdentifierMapper shortenerUrlIdentifierMapper, final MapperString mapperString,
			final MapperCalendar mapperCalendar) {
		final List<StringObjectMapper<ShortenerUrlBean>> result = new ArrayList<StringObjectMapper<ShortenerUrlBean>>();
		result.add(new StringObjectMapperAdapter<ShortenerUrlBean, ShortenerUrlIdentifier>("id", shortenerUrlIdentifierMapper));
		result.add(new StringObjectMapperAdapter<ShortenerUrlBean, String>("name", mapperString));
		result.add(new StringObjectMapperAdapter<ShortenerUrlBean, Calendar>("created", mapperCalendar));
		result.add(new StringObjectMapperAdapter<ShortenerUrlBean, Calendar>("modified", mapperCalendar));
		return result;
	}
}
