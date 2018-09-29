package org.atlasapi.remotesite.channel4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.atlasapi.persistence.testing.StubContentResolver;
import com.metabroadcast.common.http.FixedResponseHttpClient;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpStatusCodeException;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.time.DateTimeZones;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.WireFeedOutput;

@RunWith(JMock.class)
public class C4BrandExtractorTest extends TestCase {
    
    private final Mockery context = new Mockery();

	private final AtomFeedBuilder rknSeries3Feed = new AtomFeedBuilder(Resources.getResource(getClass(), "ramsays-kitchen-nightmares-series-3.atom"));
	private final AtomFeedBuilder rknSeries4Feed = new AtomFeedBuilder(Resources.getResource(getClass(), "ramsays-kitchen-nightmares-series-4.atom"));
	private final AtomFeedBuilder rknBrandFeed = new AtomFeedBuilder(Resources.getResource(getClass(), "ramsays-kitchen-nightmares.atom"));

	private final SimpleHttpClient httpClient = new FixedResponseHttpClient(
	    ImmutableMap.<String, String>builder()
		.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares.atom", fileContentsFromResource("ramsays-kitchen-nightmares.atom"))
		.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/4od.atom", fileContentsFromResource("ramsays-kitchen-nightmares-4od.atom"))
		.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide.atom", fileContentsFromResource("ramsays-kitchen-nightmares-episode-guide.atom"))
		.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-1.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-1.atom"))
        .put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-2.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-2.atom"))
		.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-3.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-3.atom"))
		.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-4.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-4.atom"))
		.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-5.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-5.atom"))
		.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/epg.atom", fileContentsFromResource("ramsays-kitchen-nightmares-epg.atom"))
		.build());
		
	private final C4AtomApiClient atomApiClient = new C4AtomApiClient(httpClient, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());
	
	private final StubContentResolver contentResolver = new StubContentResolver();
	private ChannelResolver channelResolver;
	
	private String fileContentsFromResource(String resourceName)  {
	    try {
            return Files.toString(new File(Resources.getResource(getClass(), resourceName).getFile()), Charsets.UTF_8);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
	    return null;
	}
	
	@Before
	public void setUp() {
		channelResolver = context.mock(ChannelResolver.class);
		context.checking(new Expectations() {
			{
				allowing(channelResolver).fromUri("http://www.channel4.com");
				will(returnValue(Maybe.just(new Channel(Publisher.METABROADCAST, "Channel 4", "channel4", false, MediaType.VIDEO, "http://www.channel4.com"))));
				allowing(channelResolver).fromUri("http://www.channel4.com/more4");
				will(returnValue(Maybe.just(new Channel(Publisher.METABROADCAST, "More4", "more4", false, MediaType.VIDEO, "http://www.more4.com"))));
				allowing(channelResolver).fromUri("http://film4.com");
				will(returnValue(Maybe.just(new Channel(Publisher.METABROADCAST, "Film4", "more4", false, MediaType.VIDEO, "http://film4.com"))));
				allowing(channelResolver).fromUri("http://www.e4.com");
				will(returnValue(Maybe.just(new Channel(Publisher.METABROADCAST, "E4", "more4", false, MediaType.VIDEO, "http://www.e4.com"))));
				allowing(channelResolver).fromUri("http://www.4music.com");
				will(returnValue(Maybe.just(new Channel(Publisher.METABROADCAST, "4Music", "more4", false, MediaType.VIDEO, "http://www.4music.com"))));
				allowing(channelResolver).fromUri("http://www.channel4.com/4seven");
                will(returnValue(Maybe.just(new Channel(Publisher.METABROADCAST, "4seven", "4seven", false, MediaType.VIDEO, "http://www.channel4.com/4seven"))));
			}
		});

	}

    @Test
	public void testExtractingABrand() throws Exception {
		RecordingContentWriter recordingWriter = new RecordingContentWriter();
		
		new C4AtomBackedBrandUpdater(atomApiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");

		Brand brand = Iterables.getOnlyElement(recordingWriter.updatedBrands);
		
		assertThat(brand.getCanonicalUri(), is("http://www.channel4.com/programmes/ramsays-kitchen-nightmares"));

		Item firstItem = recordingWriter.updatedItems.get(0);
		
		assertThat(firstItem.getCanonicalUri(), is("http://www.channel4.com/programmes/36423/001"));

		// TODO new alias
		assertThat(firstItem.getAliasUrls(), is((Set<String>) ImmutableSet.of(
	        "http://www.channel4.com/programmes/ramsays-kitchen-nightmares/4od#2921983", 
	        "tag:www.channel4.com,2009:/programmes/ramsays-kitchen-nightmares/episode-guide/series-3/episode-1"
        )));
		
		assertThat(firstItem.getTitle(), is(("Series 1 Episode 1")));
		
		Version firstItemVersion = Iterables.getOnlyElement(firstItem.getVersions());
		
		assertThat(firstItemVersion.getDuration(), is(2935));

		Encoding firstItemEncoding = Iterables.getOnlyElement(firstItemVersion.getManifestedAs());
		Location firstItemLocation = Iterables.getOnlyElement(firstItemEncoding.getAvailableAt());
		assertThat(firstItemLocation.getUri(), is("http://www.channel4.com/programmes/ramsays-kitchen-nightmares/4od#2922045"));
		
		Episode episodeNotOn4od = (Episode) find("http://www.channel4.com/programmes/41337/005", recordingWriter.updatedItems);
		assertThat(episodeNotOn4od.getVersions().size(), is(0));
	}

    @Test
	public void testThatBroadcastIsExtractedFromEpg() throws Exception {
		
		RecordingContentWriter recordingWriter = new RecordingContentWriter();

	    new C4AtomBackedBrandUpdater(atomApiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");
	    
	    boolean found = false;
	    for (Item item : recordingWriter.updatedItems) {
	        if (item.getCanonicalUri().equals("http://www.channel4.com/programmes/43065/005")) {
	            assertFalse(item.getVersions().isEmpty());
	            Version version = item.getVersions().iterator().next();
	            
	            assertEquals(1, version.getBroadcasts().size());
	            for (Broadcast broadcast: version.getBroadcasts()) {
	                if (broadcast.getBroadcastDuration() == 60*55) {
	                    assertTrue(broadcast.getAliasUrls().contains("tag:www.channel4.com,2009:slot/E439861"));
	                    assertThat(broadcast.getSourceId(), is("e4:39861"));
	                    found = true;
	                }
	            }
	        }
	    }
	    
	    assertTrue(found);
	}

    @Test
	public void testOldEpisodeWithBroadcast() throws Exception {
		RecordingContentWriter recordingWriter = new RecordingContentWriter();
		
	    Episode episode = new Episode("http://www.channel4.com/programmes/43065/005", "c4:ramsays-kitchen-nightmares_series-4_episode-5", Publisher.C4);
	    Version version = new Version();
	    episode.addVersion(version);
	    Broadcast oldBroadcast = new Broadcast("some channel", new DateTime(), new DateTime());
	    // TODO new alias
	    oldBroadcast.addAliasUrl("tag:www.channel4.com:someid");
	    version.addBroadcast(oldBroadcast);
	    contentResolver.respondTo(episode);
	    
	    new C4AtomBackedBrandUpdater(atomApiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");
        
        boolean found = false;
        boolean foundOld = false;
        for (Item item: recordingWriter.updatedItems) {
            if (item.getCanonicalUri().equals("http://www.channel4.com/programmes/43065/005")) {
                assertFalse(item.getVersions().isEmpty());
                version = item.getVersions().iterator().next();

                assertEquals(2, version.getBroadcasts().size());
                for (Broadcast broadcast: version.getBroadcasts()) {
                    if (broadcast.getBroadcastDuration() == 60*55) {
                        assertTrue(broadcast.getAliasUrls().contains("tag:www.channel4.com,2009:slot/E439861"));
                        assertThat(broadcast.getSourceId(), is("e4:39861"));
                        assertEquals(new DateTime("2010-08-11T14:06:33.341Z", DateTimeZones.UTC), broadcast.getLastUpdated());
                        found = true;
                    } else if (broadcast.getAliasUrls().contains("tag:www.channel4.com:someid")) {
                        foundOld = true;
                    }
                }
            }
        }
        
        assertTrue(found);
        assertTrue(foundOld);
	}

    /* The API no longer exhibits this behavior; leaving these tests here for a record of the behaviour temporarily
      but they can be removed in July 2012
     
     
    @Test
	public void testThatWhenTheEpisodeGuideReturnsABadStatusCodeSeries1IsAssumed() throws Exception {
		
	    SimpleHttpClient client = new FixedResponseHttpClient(
	        ImmutableMap.<String, String>builder()	
			.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares.atom", fileContentsFromResource("ramsays-kitchen-nightmares.atom"))
			.put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-1.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-1.atom"))
			.build(),
			ImmutableMap.<String, Integer>of("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide.atom", 403));

	    C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());
		RecordingContentWriter recordingWriter = new RecordingContentWriter();
		new C4AtomBackedBrandUpdater(apiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");
		assertThat(recordingWriter.updatedItems.size(), is(greaterThan(1)));
	}

    @Test
	public void testThatWhenTheEpisodeGuide404sSeries1IsAssumed() throws Exception {
        SimpleHttpClient client = new FixedResponseHttpClient(
                ImmutableMap.<String, String>builder()  
                .put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares.atom", fileContentsFromResource("ramsays-kitchen-nightmares.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-1.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-1.atom"))
                .build(),
                ImmutableMap.<String, Integer>of("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide.atom", 404));

        C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());

		RecordingContentWriter recordingWriter = new RecordingContentWriter();
		new C4AtomBackedBrandUpdater(apiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");
		assertThat(recordingWriter.updatedItems.size(), is(greaterThan(1)));
	}

    @Test
	public void testThatWhenTheEpisodeGuideReturnsABadStatusCodeSeries3IsReturned() throws Exception {
	    HttpResponsePrologue response = new HttpResponsePrologue(403, "error").withFinalUrl("http://www.channel4.com/programmes/ramsays-kitchen-nightmares/episode-guide/series-3.atom");
        RemoteSiteClient<Feed> feedClient = new StubC4AtomClient()
            .respondTo("http://api.channel4.com/pmlsd/ramsays-kitchen-nightmares.atom", rknBrandFeed.build())
            .respondTo("http://api.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide.atom", new HttpStatusCodeException(response, "403"))
            .respondTo("http://api.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-3.atom", rknSeries4Feed.build());

		RecordingContentWriter recordingWriter = new RecordingContentWriter();
        new C4AtomBackedBrandUpdater(feedClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");
        assertThat(recordingWriter.updatedItems.size(), is(greaterThan(1)));
    }
    */
	
    @Test
	public void testFlattenedBrandsItemsAreNotPutIntoSeries() throws Exception {
        SimpleHttpClient client = new FixedResponseHttpClient(
                ImmutableMap.<String, String>builder()  
                .put("https://pmlsc.channel4.com/pmlsd/dispatches.atom", fileContentsFromResource("dispatches.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/dispatches/episode-guide.atom", fileContentsFromResource("dispatches-episode-guide.atom"))
                .build());
        
        C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());
		 
		 RecordingContentWriter recordingWriter = new RecordingContentWriter();
	     new C4AtomBackedBrandUpdater(apiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/dispatches");
	     
	     assertThat(recordingWriter.updatedItems.size(), is(greaterThan(1)));
	     for (Item item : recordingWriter.updatedItems) {
			assertThat(item.getVersions().size(), is(0));
		}
	}

    @Test
	public void testThatWhenTheEpisodeGuideRedirectsToAnEpisodeFeedTheSeriesIsFetched() throws Exception {
	   
		Feed episodeFeed = new Feed();
		episodeFeed.setFeedType("atom_1.0");
		episodeFeed.setId("tag:www.channel4.com,2009:/programmes/ramsays-kitchen-nightmares/episode-guide/series-3/episode-5");
		WireFeedOutput output = new WireFeedOutput();
		StringWriter os = new StringWriter();
		output.output(episodeFeed, os);
		String redirectingFeed = os.getBuffer().toString();
		
		SimpleHttpClient client = new FixedResponseHttpClient(
		        ImmutableMap.<String,String>of(
		                "https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares.atom", fileContentsFromResource("ramsays-kitchen-nightmares.atom"), 
		                "https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide.atom", redirectingFeed,
		                "https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-3.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-3.atom")));
		
		C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());

   	    RecordingContentWriter recordingWriter = new RecordingContentWriter();
        new C4AtomBackedBrandUpdater(apiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");
        assertThat(recordingWriter.updatedItems.size(), is(greaterThan(1)));
	}

    @Test
	public void testThatWhenTheEpisodeGuideRedirectsToSeries1TheSeriesIsRead() throws Exception {
        
        SimpleHttpClient client = new FixedResponseHttpClient(
                ImmutableMap.<String,String>of(
                        "https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares.atom", fileContentsFromResource("ramsays-kitchen-nightmares.atom"), 
                        "https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-3.atom")));

        C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());

		RecordingContentWriter recordingWriter = new RecordingContentWriter();
		new C4AtomBackedBrandUpdater(apiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");
	    assertThat(recordingWriter.updatedItems.size(), is(greaterThan(1)));
	}

    @Test
	public void testThatClipsAreAddedToBrands() throws Exception {
        
        SimpleHttpClient client = new FixedResponseHttpClient(
                ImmutableMap.<String, String>builder()  
                .put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares.atom", fileContentsFromResource("ramsays-kitchen-nightmares.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide.atom", fileContentsFromResource("ramsays-kitchen-nightmares-series-3.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/ramsays-kitchen-nightmares/video.atom", fileContentsFromResource("ugly-betty-video.atom"))
                .build());

        C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());
        
		RecordingContentWriter recordingWriter = new RecordingContentWriter();
		new C4AtomBackedBrandUpdater(apiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");
		assertThat(Iterables.getOnlyElement(recordingWriter.updatedBrands).getClips().size(), is(greaterThan(1)));
	}

    @Test
	public void testThatOldLocationsAndBroadcastsAreCopied() {

		RemoteSiteClient<Feed> feedClient = new StubC4AtomClient()
		.respondTo("http://api.channel4.com/pmlsd/ramsays-kitchen-nightmares.atom", rknBrandFeed.build())
		.respondTo("http://api.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide.atom", rknSeries3Feed.build())
        .respondTo("http://api.channel4.com/pmlsd/ramsays-kitchen-nightmares/episode-guide/series-3.atom", rknSeries3Feed.build());

		
		Episode series3Ep1 = new Episode("http://www.channel4.com/programmes/ramsays-kitchen-nightmares/episode-guide/series-3/episode-1", "curie", Publisher.C4);
		
		Version c4Version = new Version();
		c4Version.setCanonicalUri("v1");

		// this version shouldn't be merged because it's not from C4
		Version otherPublisherVersion = new Version();
		otherPublisherVersion.setProvider(Publisher.YOUTUBE);
		otherPublisherVersion.setCanonicalUri("v2");

		series3Ep1.addVersion(c4Version);
		series3Ep1.addVersion(otherPublisherVersion);
		
		contentResolver.respondTo(series3Ep1);
		
		RecordingContentWriter recordingWriter = new RecordingContentWriter();
		new C4AtomBackedBrandUpdater(feedClient, contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/ramsays-kitchen-nightmares");
		Item series3Ep1Parsed = Iterables.get(recordingWriter.updatedItems, 0);
		
		assertTrue(Iterables.getOnlyElement(series3Ep1Parsed.getVersions()) == c4Version);
	}
    
    @Test
    public void testPlatformLocation() {
        
        SimpleHttpClient client = new FixedResponseHttpClient(
                ImmutableMap.<String, String>builder()  
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does.atom?platform=xbox", fileContentsFromResource("jamie-does-xbox.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/4od.atom?platform=xbox", fileContentsFromResource("jamie-does-4od-xbox.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/epg.atom?platform=xbox", fileContentsFromResource("jamie-does-epg-xbox.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide.atom?platform=xbox", fileContentsFromResource("jamie-does-episode-guide-xbox.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide/series-1.atom?platform=xbox", fileContentsFromResource("jamie-does-series-1-xbox.atom"))
                .build());

        C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.of("xbox"));

        RecordingContentWriter recordingWriter = new RecordingContentWriter();
        new C4AtomBackedBrandUpdater(apiClient, Optional.of(Platform.XBOX), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/jamie-does");
        
        Item item = find("http://www.channel4.com/programmes/48367/006", recordingWriter.updatedItems);
        Episode episode = (Episode) item;
        
        Location location = Iterables.getOnlyElement(
                Iterables.getOnlyElement(
                Iterables.getOnlyElement(episode.getVersions())
                .getManifestedAs())
                .getAvailableAt());
        assertThat(location.getPolicy().getPlatform(), is(Platform.XBOX));
        assertThat(location.getUri(), is("https://ais.channel4.com/asset/3262609"));
    }
    
    @Test 
    public void testMultipleLocationsOnDifferentPlatforms() {
        SimpleHttpClient xboxClient = new FixedResponseHttpClient(
                ImmutableMap.<String, String>builder()  
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does.atom?platform=xbox", fileContentsFromResource("jamie-does-xbox.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/4od.atom?platform=xbox", fileContentsFromResource("jamie-does-4od-xbox.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/epg.atom?platform=xbox", fileContentsFromResource("jamie-does-epg-xbox.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide.atom?platform=xbox", fileContentsFromResource("jamie-does-episode-guide-xbox.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide/series-1.atom?platform=xbox", fileContentsFromResource("jamie-does-series-1-xbox.atom"))
                .build());
        
        SimpleHttpClient client = new FixedResponseHttpClient(
                ImmutableMap.<String, String>builder()  
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does.atom", fileContentsFromResource("jamie-does.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/4od.atom", fileContentsFromResource("jamie-does-4od.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/epg.atom", fileContentsFromResource("jamie-does-epg.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide.atom", fileContentsFromResource("jamie-does-episode-guide.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide/series-1.atom", fileContentsFromResource("jamie-does-series-1.atom"))
                .build());

        C4AtomApiClient xboxApiClient = new C4AtomApiClient(xboxClient, "https://pmlsc.channel4.com/pmlsd/", Optional.of("xbox"));
        C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());

        RecordingContentWriter recordingWriter = new RecordingContentWriter();
        new C4AtomBackedBrandUpdater(xboxApiClient, Optional.of(Platform.XBOX), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/jamie-does");
        
        contentResolver.respondTo(find("http://www.channel4.com/programmes/48367/006", recordingWriter.updatedItems));
        
        new C4AtomBackedBrandUpdater(apiClient, Optional.<Platform>absent(), contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/jamie-does");
        
        
        Item item = find("http://www.channel4.com/programmes/48367/006", recordingWriter.updatedItems);
        Episode episode = (Episode) item;
        
        Location location = Iterables.getOnlyElement(
                Iterables.getOnlyElement(
                Iterables.getOnlyElement(episode.getVersions())
                .getManifestedAs())
                .getAvailableAt());
        assertThat(location.getPolicy().getPlatform(), is(Platform.XBOX));
        assertThat(location.getUri(), is("https://ais.channel4.com/asset/3262609"));
      
    }
    
    @Test 
    /**
     * The description may differ on a platform-by-platform basis. Therefore 
     * we only ingest the description from a platform feed if we don't have
     * a description already.
     */
    public void testPlatformDoesNotUpdateDescription() {
        SimpleHttpClient client = new FixedResponseHttpClient(
                ImmutableMap.<String, String>builder()  
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does.atom?platform=xbox", fileContentsFromResource("jamie-does.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide.atom?platform=xbox", fileContentsFromResource("jamie-does-episode-guide.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide/series-1.atom?platform=xbox", fileContentsFromResource("jamie-does-series-1.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide/series-1/episode-1.atom?platform=xbox", fileContentsFromResource("jamie-does-series-1-episode-1.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does.atom", fileContentsFromResource("jamie-does.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide.atom", fileContentsFromResource("jamie-does-episode-guide.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide/series-1.atom", fileContentsFromResource("jamie-does-series-1.atom"))
                .put("https://pmlsc.channel4.com/pmlsd/jamie-does/episode-guide/series-1/episode-1.atom", fileContentsFromResource("jamie-does-series-1-episode-1-dummy-description.atom"))
                .build());

        C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());
        C4AtomApiClient xboxApiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.of("xbox"));

        RecordingContentWriter recordingWriter = new RecordingContentWriter();
        new C4AtomBackedBrandUpdater(apiClient, contentResolver, recordingWriter, channelResolver).createOrUpdateBrand("http://www.channel4.com/programmes/jamie-does");
        
        Item item = find("http://www.channel4.com/programmes/48367/005", recordingWriter.updatedItems);
        Episode episode = (Episode) item;
        
        Location location = Iterables.getOnlyElement(
                Iterables.getOnlyElement(
                Iterables.getOnlyElement(episode.getVersions())
                .getManifestedAs())
                .getAvailableAt());
        assertThat(location.getPolicy().getPlatform(), is(Platform.XBOX));
        
        assertThat(recordingWriter.updatedItems.size(), is(greaterThan(1)));
    }
	
	private static class StubC4AtomClient implements RemoteSiteClient<Feed> {

		private Map<String, Object> respondsTo = Maps.newHashMap();

		@Override
		public Feed get(String uri) throws Exception {
			// Remove API key
			uri = removeQueryString(uri);
			Object response = respondsTo.get(uri);
			if (response == null) {
				throw new HttpStatusCodeException(404, "Not found: " + uri);
			} else if (response instanceof HttpException) {
			    throw (HttpException) response;
			}
			return (Feed) response;
		}

		private String removeQueryString(String url) throws MalformedURLException {
			String queryString = "?" + new URL(url).getQuery();
			return url.replace(queryString, "");
		}
		
		StubC4AtomClient respondTo(String url, Feed feed) {
			respondsTo.put(url, feed);
			return this;
		}
		
		StubC4AtomClient respondTo(String url, HttpException exception) {
		    respondsTo.put(url, exception);
		    return this;
		}
	}
	
	private final <T extends Content> T find(String uri, Iterable<T> episodes) {
		for (T episode : episodes) {
			if (episode.getCanonicalUri().equals(uri)) {
				return episode;
			}
		}
		throw new IllegalStateException("Not found");
	}
}
