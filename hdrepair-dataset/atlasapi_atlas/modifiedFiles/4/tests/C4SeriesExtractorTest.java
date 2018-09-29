package org.atlasapi.remotesite.channel4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.SystemClock;

public class C4SeriesExtractorTest extends TestCase {

	private final AtomFeedBuilder seriesFeed = new AtomFeedBuilder(Resources.getResource(getClass(), "ramsays-kitchen-nightmares-series-3.atom"));
	
	public void testParsingASeries() throws Exception {
		
		SeriesAndEpisodes seriesAndEpisodes = new C4SeriesAndEpisodesExtractor(new SystemClock()).extract(seriesFeed.build());
		Series series = seriesAndEpisodes.getSeries();
		
		assertThat(series.getCanonicalUri(), is("http://www.channel4.com/programmes/ramsays-kitchen-nightmares/episode-guide/series-3"));
		assertThat(series.getCurie(), is("c4:ramsays-kitchen-nightmares-series-3"));
		// TODO new alias
		assertThat(series.getAliasUrls(), is((Set<String>) ImmutableSet.of("tag:www.channel4.com,2009:/programmes/ramsays-kitchen-nightmares/episode-guide/series-3")));

		assertThat(series.getImage(), is("http://www.channel4.com/assets/programmes/images/ramsays-kitchen-nightmares/series-3/ramsays-kitchen-nightmares-s3-20090617160853_625x352.jpg"));
		assertThat(series.getThumbnail(), is("http://www.channel4.com/assets/programmes/images/ramsays-kitchen-nightmares/series-3/ramsays-kitchen-nightmares-s3-20090617160853_200x113.jpg"));

		assertThat(series.getTitle(), is("Series 3 - Ramsay's Kitchen Nightmares"));
		assertThat(series.getDescription(), startsWith("Multi Michelin-starred chef Gordon Ramsay"));
		
		List<Episode> episodes = seriesAndEpisodes.getEpisodes();
		
		Episode firstEpisode = episodes.get(0);
		
		assertThat(firstEpisode.getCanonicalUri(), is("http://www.channel4.com/programmes/41337/001"));
		assertThat(firstEpisode.getCurie(), is("c4:ramsays-kitchen-nightmares-series-3-episode-1"));
		assertThat(firstEpisode.getThumbnail(), is("http://www.channel4.com/assets/programmes/images/ramsays-kitchen-nightmares/series-3/ramsays-kitchen-nightmares-s3-20090617160853_200x113.jpg"));
		assertThat(firstEpisode.getImage(), is("http://www.channel4.com/assets/programmes/images/shameless/series-7/episode-8/c842994a-5c06-493e-9d1f-5b6a03188848_625x352.jpg"));

		assertThat(series.getSeriesNumber(), is(3));

		assertThat(series.getLastUpdated(), is(new DateTime("2010-08-09T16:49:33.651Z", DateTimeZones.UTC)));
		
		assertThat(firstEpisode.getSeriesNumber(), is(3));
		assertThat(firstEpisode.getEpisodeNumber(), is(1));

		// since this is not a /4od feed there should be no On Demand entries
		assertThat(firstEpisode.getVersions(), is((Set) ImmutableSet.of()));

		// The outer adapter will notice that this is the same as the brand title (ignoring punctuation and spacing) and will replace it with the series and episode number
		assertThat(firstEpisode.getTitle(), is("Ramsay's Kitchen: Nightmares"));
	}
}
