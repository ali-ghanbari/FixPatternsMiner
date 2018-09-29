/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.GenericProtocolDecoder;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * GL200 tracker protocol decoder
 */
public class Gl200ProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Initialize
     */
    public Gl200ProtocolDecoder(DataManager dataManager, Integer resetDelay) {
        super(dataManager, resetDelay);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "\\+RESP:GT...," +
            "[0-9a-fA-F]{6}," +                 // Protocol version
            "(\\d{15})," +                      // IMEI
            "[^,]*," +                          // Device name
            "(?:(?:\\d," +                      // Report ID / Geo mode
            "\\d*," +                           // Report type / Geo radius
            "\\d*)|" +                          // Number / Geo check interval
            "(?:[^,]*))," +                     // Call number
            "(\\d*)," +                         // GPS accuracy
            "(\\d+.\\d)," +                     // Speed
            "(\\d+)," +                         // Course
            "(-?\\d+.\\d)," +                   // Altitude
            "(-?\\d+.\\d+)," +                  // Longitude
            "(-?\\d+.\\d+)," +                  // Latitude
            "(\\d{4})(\\d{2})(\\d{2})" +        // Date (YYYYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time (HHMMSS)
            ".*");

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();

        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());

        // Validity
        position.setValid(Integer.valueOf(parser.group(index++)) == 0 ? false : true);

        // Position info
        position.setSpeed(Double.valueOf(parser.group(index++)));
        position.setCourse(Double.valueOf(parser.group(index++)));
        position.setAltitude(Double.valueOf(parser.group(index++)));
        position.setLongitude(Double.valueOf(parser.group(index++)));
        position.setLatitude(Double.valueOf(parser.group(index++)));

        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));

        // Time
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        return position;
    }

}
