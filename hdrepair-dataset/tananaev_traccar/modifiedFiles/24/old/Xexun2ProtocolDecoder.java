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
import org.traccar.helper.Log;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * Xexun tracker protocol decoder
 */
public class Xexun2ProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Initialize
     */
    public Xexun2ProtocolDecoder(DataManager dataManager) {
        super(dataManager);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "[\r\n]*" +
            "(\\d+)," +                         // Serial
            "(\\+\\d+)," +                      // Number
            "GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d{3})," + // Time (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "(\\d{2})(\\d{2}\\.\\d{4})," +      // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d{4})," +      // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+\\.\\d+)," +                  // Speed
            "(\\d+\\.\\d+)?," +                 // Course
            "(\\d{2})(\\d{2})(\\d{2})," +       // Date (DDMMYY)
            ",,.\\*..," +                       // Checksum
            "([FL])," +                         // Signal
            "(.*)," +                           // Alarm
            ".*imei:" +
            "(\\d+)," +                         // IMEI
            "(\\d+)," +                         // Satellites
            "(\\d+\\.\\d+)," +                  // Altitude
            "[FL]:(\\d+\\.\\d+)V," +               // Power
            ".*" +
            "[\r\n]*");

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        // Parse message
        String sentence = (String) msg;
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            //throw new ParseException(null, 0);
            return null;
        }

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>xexun2</protocol>");

        Integer index = 1;

        // Serial
        extendedInfo.append("<serial>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</serial>");

        // Number
        extendedInfo.append("<number>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</number>");

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MILLISECOND, Integer.valueOf(parser.group(index++)));

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0 ? true : false);

        // Latitude
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double lonlitude = Double.valueOf(parser.group(index++));
        lonlitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) lonlitude = -lonlitude;
        position.setLongitude(lonlitude);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Course
        String course = parser.group(index++);
        if (course != null) {
            position.setCourse(Double.valueOf(course));
        } else {
            position.setCourse(0.0);
        }

        // Date
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Signal
        extendedInfo.append("<signal>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</signal>");

        // Alarm
        extendedInfo.append("<alarm>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</alarm>");

        // Get device by IMEI
        String imei = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
            return null;
        }

        // Satellites
        extendedInfo.append("<satellites>");
        extendedInfo.append(parser.group(index++).replaceFirst ("^0*(?![\\.$])", ""));
        extendedInfo.append("</satellites>");

        // Altitude
        position.setAltitude(Double.valueOf(parser.group(index++)));

        // Power
        position.setPower(Double.valueOf(parser.group(index++)));

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
