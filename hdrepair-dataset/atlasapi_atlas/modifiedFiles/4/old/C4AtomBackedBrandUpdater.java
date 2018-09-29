package org.atlasapi.remotesite.channel4;

import static org.atlasapi.media.entity.Identified.TO_URI;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Restriction;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.remotesite.FetchException;
import org.joda.time.Duration;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.syndication.feed.atom.Feed;

public class C4AtomBackedBrandUpdater implements C4BrandUpdater {

	private static final Pattern BRAND_PAGE_PATTERN = Pattern.compile("http://www.channel4.com/programmes/([^/\\s]+)");

	private final Log log = LogFactory.getLog(getClass());
	
	private final C4AtomApiClient feedClient;
	private final C4AtomContentResolver resolver;
	private final ContentWriter writer;
	private final C4BrandExtractor extractor;

	private boolean canUpdateDescriptions = true;
	
	public C4AtomBackedBrandUpdater(C4AtomApiClient feedClient, ContentResolver contentResolver, ContentWriter contentWriter, ChannelResolver channelResolver) {
		this.feedClient = feedClient;
		this.resolver = new C4AtomContentResolver(contentResolver);
		this.writer = contentWriter;
		this.extractor = new C4BrandExtractor(feedClient, channelResolver);
	}
	
	@Override
	public boolean canFetch(String uri) {
		return BRAND_PAGE_PATTERN.matcher(uri).matches();
	}

	public Brand createOrUpdateBrand(String uri) {
	    Preconditions.checkArgument(canFetch(uri), "Cannot fetch C4 uri: %s as it is not in the expected format: %s",uri, BRAND_PAGE_PATTERN.toString());

	    try {
			log.info("Fetching C4 brand " + uri);
			Optional<Feed> source = feedClient.brandFeed(uri);
			
			if (source.isPresent()) {
			    BrandSeriesAndEpisodes brandHierarchy = extractor.extract(source.get());
			    
			    writer.createOrUpdate(resolveAndUpdate(brandHierarchy.getBrand()));
			    
			    for (SeriesAndEpisodes seriesAndEpisodes : brandHierarchy.getSeriesAndEpisodes()) {
			        writer.createOrUpdate(resolveAndUpdate(seriesAndEpisodes.getSeries()));
			        for (Episode episode : seriesAndEpisodes.getEpisodes()) {
			            writer.createOrUpdate(resolveAndUpdate(episode));
			        }
			    }
			    
			    return brandHierarchy.getBrand();
			}
			throw new FetchException("Failed to fetch " + uri);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    private Item resolveAndUpdate(Episode episode) {
        Optional<Item> existingEpisode = resolve(episode);
        if (!existingEpisode.isPresent()) {
            return episode;
        }
        return updateItem(existingEpisode.get(), episode);
    }

    private Optional<Item> resolve(Episode episode) {
        return resolver.itemFor(episode.getCanonicalUri(), Optional.fromNullable(hierarchyUri(episode)), Optional.<String>absent());
    }

    private String hierarchyUri(Episode episode) {
        for (String alias : episode.getAliases()) {
            if (C4AtomApi.isACanonicalEpisodeUri(alias)) {
                return alias;
            }
        }
        return null;
    }

    private Brand resolveAndUpdate(Brand brand) {
        Optional<Brand> existingBrand = resolver.brandFor(brand.getCanonicalUri());
        if (!existingBrand.isPresent()) {
            return brand;
        }
        return updateContent(existingBrand.get(), brand);
    }
    
    private Series resolveAndUpdate(Series series) {
        Optional<Series> existingSeries = resolver.seriesFor(series.getCanonicalUri());
        if (!existingSeries.isPresent()) {
            return series;
        }
        return updateContent(existingSeries.get(), series);
    }

    private <T extends Content> T updateContent(T existing, T fetched) {
        existing = updateDescribed(existing, fetched);
        
        Set<Clip> mergedClips = mergeClips(existing, fetched);
        
        existing.setClips(mergedClips);
        if (!Objects.equal(mergedClips, existing.getClips())) {
            copyLastUpdated(fetched, existing);
        }

        return existing;
    }

    private <T extends Content> Set<Clip> mergeClips(T existing, T fetched) {
        Set<Clip> mergedClips = Sets.newHashSet();
        ImmutableMap<String, Clip> fetchedClips = Maps.uniqueIndex(fetched.getClips(), TO_URI);
        for (Clip existingClip : existing.getClips()) {
            Clip fetchedClip = fetchedClips.get(existingClip.getCanonicalUri());
            if (fetchedClip != null) {
                mergedClips.add(updateItem(existingClip, fetchedClip));
            }
        }
        for (Clip fetchedClip : fetched.getClips()) {
            mergedClips.add(fetchedClip);
        }
        return mergedClips;
    }

    private <T extends Item> T updateItem(T existingClip, T fetchedClip) {
        existingClip = updateContent(existingClip, fetchedClip);
        
        Version existingVersion = Iterables.getOnlyElement(existingClip.getVersions());
        Version fetchedVersion = Iterables.getOnlyElement(fetchedClip.getVersions());
        existingClip.setVersions(Sets.newHashSet(updateVersion(existingVersion, fetchedVersion)));
        return existingClip;
    }

    private Version updateVersion(Version existing, Version fetched) {
        if (!Objects.equal(existing.getDuration(), fetched.getDuration())) {
            existing.setDuration(Duration.standardSeconds(fetched.getDuration()));
            copyLastUpdated(fetched, existing);
        }
        if (!equivalentRestrictions(existing.getRestriction(), fetched.getRestriction())) {
            existing.setRestriction(fetched.getRestriction());
            copyLastUpdated(fetched, existing);
        }

        Set<Broadcast> broadcasts = Sets.newHashSet();
        Map<String, Broadcast> fetchedBroadcasts = Maps.uniqueIndex(fetched.getBroadcasts(), new Function<Broadcast, String>() {
            @Override
            public String apply(Broadcast input) {
                return input.getSourceId();
            }
        });
        for (Broadcast broadcast : existing.getBroadcasts()) {
            Broadcast fetchedBroadcast = fetchedBroadcasts.get(broadcast.getSourceId());
            if (fetchedBroadcast != null) {
                broadcasts.add(updateBroadcast(broadcast, fetchedBroadcast));
            } else {
                broadcasts.add(broadcast);
            }
        }
        for (Broadcast broadcast : fetched.getBroadcasts()) {
            broadcasts.add(broadcast);
        }
        existing.setBroadcasts(broadcasts);
        
        Encoding existingEncoding = Iterables.getOnlyElement(existing.getManifestedAs());
        Encoding fetchedEncoding = Iterables.getOnlyElement(fetched.getManifestedAs());
        existing.setManifestedAs(Sets.newHashSet(updateEncoding(existingEncoding, fetchedEncoding)));
        return existing;
    }

    private Broadcast updateBroadcast(Broadcast existing, Broadcast fetched) {
        if (!Objects.equal(existing.getBroadcastOn(), fetched.getBroadcastOn())
            || !Objects.equal(existing.getTransmissionTime(), fetched.getTransmissionTime())
            || !Objects.equal(existing.getTransmissionEndTime(), fetched.getTransmissionEndTime())){
            fetched.setIsActivelyPublished(existing.isActivelyPublished());
            return fetched;
        }
        return existing;
    }

    private Encoding updateEncoding(Encoding existingEncoding, Encoding fetchedEncoding) {
        Set<Location> mergedLocations = Sets.newHashSet();
        for (Location fetchedLocation : fetchedEncoding.getAvailableAt()) {
            Location existingEquivalent = findExistingLocation(fetchedLocation, existingEncoding.getAvailableAt());
            if (existingEquivalent != null) {
                mergedLocations.add(updateLocation(existingEquivalent, fetchedLocation));
            } else {
                mergedLocations.add(fetchedLocation);
            }
        }
        
        existingEncoding.setAvailableAt(mergedLocations);
        
        return existingEncoding;
    }

    private Location updateLocation(Location existing, Location fetched) {
        if (!Objects.equal(existing.getAliases(), fetched.getAliases())) {
            existing.setAliases(fetched.getAliases());
            copyLastUpdated(fetched, existing);
        }
        if (!Objects.equal(existing.getEmbedCode(), fetched.getEmbedCode())) {
            existing.setEmbedCode(fetched.getEmbedCode());
            copyLastUpdated(fetched, existing);
        }
        if (existing.getPolicy() == null && fetched.getPolicy() != null 
                || existing.getPolicy() != null && fetched.getPolicy() == null) {
            existing.setPolicy(fetched.getPolicy());
            copyLastUpdated(fetched, existing);
        }
        return existing;
    }

    private Location findExistingLocation(Location fetched, Set<Location> existingLocations) {
        for (Location existing : existingLocations) {
            if (existing.getUri() != null && existing.getUri().equals(fetched.getUri())
                || existing.getEmbedId() != null && existing.getEmbedId().equals(existing.getEmbedId())) {
                return existing;
            }
        }
        return null;
    }

    private boolean equivalentRestrictions(Restriction existing, Restriction fetched) {
        return Objects.equal(existing.isRestricted(), fetched.isRestricted())
            && Objects.equal(existing.getMessage(), fetched.getMessage())
            && Objects.equal(existing.getMinimumAge(), fetched.getMinimumAge());
    }

    private <T extends Described> T updateDescribed(T existing, T fetched) {
        if (canUpdateDescriptions) {
            if (!Objects.equal(existing.getTitle(), fetched.getTitle())) {
                existing.setTitle(fetched.getTitle());
                copyLastUpdated(fetched, existing);
            }
            if (!Objects.equal(existing.getDescription(), fetched.getDescription())) {
                existing.setDescription(fetched.getDescription());
                copyLastUpdated(fetched, existing);
            }
            if (!Objects.equal(existing.getImage(), fetched.getImage())) {
                existing.setImage(fetched.getImage());
                copyLastUpdated(fetched, existing);
            }
            if (!Objects.equal(existing.getThumbnail(), fetched.getThumbnail())) {
                existing.setThumbnail(fetched.getThumbnail());
                copyLastUpdated(fetched, existing);
            }
            if (!Objects.equal(existing.getGenres(), fetched.getGenres())) {
                existing.setGenres(fetched.getGenres());
                copyLastUpdated(fetched, existing);
            }
        }
        return existing;
    }

    private void copyLastUpdated(Identified from, Identified to) {
        to.setLastUpdated(from.getLastUpdated());
    }

}
