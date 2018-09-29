package org.atlasapi.remotesite.channel4.epg;

import static org.atlasapi.media.entity.Channel.CHANNEL_FOUR;
import static org.atlasapi.media.entity.Channel.E_FOUR;
import static org.atlasapi.media.entity.Channel.FILM_4;
import static org.atlasapi.media.entity.Channel.FOUR_MUSIC;
import static org.atlasapi.media.entity.Channel.MORE_FOUR;
import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.ERROR;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import java.util.List;
import java.util.Map;

import nu.xom.Document;
import nu.xom.Nodes;
import nu.xom.XPathContext;

import org.atlasapi.media.entity.Channel;
import org.atlasapi.media.entity.ScheduleEntry.ItemRefAndBroadcast;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

public class C4EpgUpdater extends ScheduledTask {

    private final static BiMap<String, Channel> CHANNEL_MAP = ImmutableBiMap.of(
            "C4", CHANNEL_FOUR,
            "M4", MORE_FOUR,
            "F4", FILM_4,
            "E4", E_FOUR,
            "4M", FOUR_MUSIC
    );
    
    private final static String epgUriPattern = "http://api.channel4.com/pmlsd/tv-listings/daily/%s/%s.atom";

    private final RemoteSiteClient<Document> c4AtomFetcher;
    private final AdapterLog log;
    private final DayRangeGenerator rangeGenerator;
    private final C4EpgEntryProcessor entryProcessor;
    private final C4EpgBrandlessEntryProcessor brandlessEntryProcessor;
    private final BroadcastTrimmer trimmer;

    public C4EpgUpdater(RemoteSiteClient<Document> fetcher, C4EpgEntryProcessor entryProcessor, C4EpgBrandlessEntryProcessor brandlessEntryProcessor, BroadcastTrimmer trimmer, AdapterLog log, DayRangeGenerator generator) {
        this.c4AtomFetcher = fetcher;
        this.trimmer = trimmer;
        this.log = log;
        this.rangeGenerator = generator;
        this.entryProcessor = entryProcessor;
        this.brandlessEntryProcessor = brandlessEntryProcessor;
    }

    @Override
    protected void runTask() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("C4 EPG Update initiated"));
        
        DayRange dayRange = rangeGenerator.generate(new LocalDate(DateTimeZones.UTC));
        
        int total = Iterables.size(dayRange) * CHANNEL_MAP.entrySet().size();
        int processed = 0;
        int failures = 0;
        int broadcasts = 0;
        
        for (Map.Entry<String, Channel> channelEntry : CHANNEL_MAP.entrySet()) {
            for (LocalDate scheduleDay : dayRange) {
                reportStatus(String.format("Processed %s/%s. %s failures. %s broadcasts processed", processed++, total, failures, broadcasts));
                
                String uri = uriFor(channelEntry.getKey(), scheduleDay);
                log.record(new AdapterLogEntry(Severity.DEBUG).withDescription("Updating from " + uri).withSource(getClass()));
                
                Document scheduleDocument = getSchedule(uri);
                if(scheduleDocument == null) {
                    failures++;
                    continue;
                }

                try {
                    List<C4EpgEntry> entries = getEntries(scheduleDocument);
                    List<ItemRefAndBroadcast> processedItems = process(entries, channelEntry.getValue());
                    trim(scheduleDay, channelEntry.getValue(), processedItems);
                } catch (Exception e) {
                    log.record(new AdapterLogEntry(ERROR).withCause(e).withSource(getClass()).withDescription("Exception updating from " + uri));
                    failures++;
                }
            }
        }
        
        reportStatus(String.format("Processed %s/%s. %s failures. %s broadcasts processed", processed++, total, failures, broadcasts));
        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("C4 EPG Update finished in " + runTime));
    }

    private void trim(LocalDate scheduleDay, Channel channel, List<ItemRefAndBroadcast> processedItems) {
        DateTime scheduleStart = scheduleDay.toDateTime(new LocalTime(6,0,0), DateTimeZones.LONDON);
        Interval scheduleInterval = new Interval(scheduleStart, scheduleStart.plusDays(1));
        trimmer.trimBroadcasts(scheduleInterval, channel, broacastIdsFrom(processedItems));
    }

    private Map<String, String> broacastIdsFrom(List<ItemRefAndBroadcast> processedItems) {
        ImmutableMap.Builder<String, String> broadcastIdItemIdMap = ImmutableMap.builder();
        for (ItemRefAndBroadcast itemRefAndBroadcast : processedItems) {
            broadcastIdItemIdMap.put(itemRefAndBroadcast.getBroadcast().getId(), itemRefAndBroadcast.getItemUri());
        }
        return broadcastIdItemIdMap.build();
    }

    private Document getSchedule(String uri) {
        try {
            return c4AtomFetcher.get(uri);
        } catch (Exception e) {
            log.record(new AdapterLogEntry(WARN).withCause(e).withSource(getClass()).withDescription("Exception fetching feed at " + uri));
            return null;
        }
    }

    private String uriFor(String channelKey, LocalDate scheduleDay) {
        return String.format(epgUriPattern, scheduleDay.toString("yyyy/MM/dd"), channelKey);
    }

    private List<ItemRefAndBroadcast> process(Iterable<C4EpgEntry> entries, Channel channel) {
        ImmutableList.Builder<ItemRefAndBroadcast> episodes = ImmutableList.builder();
        for (C4EpgEntry entry : entries) {
            try {
                ItemRefAndBroadcast episode;
                if (entry.relatedEntry() != null) {
                    episode = entryProcessor.process(entry, channel);
                } else {
                    episode = brandlessEntryProcessor.process(entry, channel);
                }
                if(episode != null) {
                    episodes.add(episode);
                }
            } catch (Exception e) {
                log.record(errorEntry().withCause(e).withSource(getClass()).withDescription("Exception processing entry %s" + entry.id()));
            }
        }
        return episodes.build();
    }

    private List<C4EpgEntry> getEntries(Document scheduleDocument) {
        Nodes entryNodes = scheduleDocument.query("//atom:feed/atom:entry", new XPathContext("atom", "http://www.w3.org/2005/Atom"));
        
        List<C4EpgEntry> entries = Lists.newArrayListWithCapacity(entryNodes.size());
        
        for (int i = 0; i < entryNodes.size(); i++) {
            C4EpgEntry entry = C4EpgEntry.from((C4EpgEntryElement) entryNodes.get(i));
            entries.add(entry);
        }
        
        return entries;
    }

}
