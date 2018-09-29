/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.Crc;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class KhdProtocolDecoder extends BaseProtocolDecoder {

    public KhdProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    private String readSerialNumber(ChannelBuffer buf) {
        int b1 = buf.readUnsignedByte();
        int b2 = buf.readUnsignedByte(); if (b2 > 0x80) b2 -= 0x80;
        int b3 = buf.readUnsignedByte(); if (b3 > 0x80) b3 -= 0x80;
        int b4 = buf.readUnsignedByte();
        String serialNumber = String.format("%02d%02d%02d%02d", b1, b2, b3, b4);
        return String.valueOf(Long.valueOf(serialNumber));
    }

    private static final int MSG_LOGIN = 0xB1;
    private static final int MSG_CONFIRMATION = 0x21;
    private static final int MSG_ON_DEMAND = 0x81;
    private static final int MSG_POSITION_UPLOAD = 0x80;
    private static final int MSG_POSITION_REUPLOAD = 0x8E;
    private static final int MSG_ALARM = 0x82;
    private static final int MSG_REPLY = 0x85;
    private static final int MSG_PERIPHERAL = 0xA3;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // size

        if (type == MSG_ON_DEMAND ||
            type == MSG_POSITION_UPLOAD ||
            type == MSG_POSITION_REUPLOAD ||
            type == MSG_ALARM ||
            type == MSG_REPLY ||
            type == MSG_PERIPHERAL) {

            // Create new position
            Position position = new Position();
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("khd");

            // Device identification
            String id = readSerialNumber(buf);
            try {
                position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
            } catch(Exception error) {
                Log.warning("Unknown device - " + id);
            }
            
            // Date and time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, 2000 + ChannelBufferTools.readHexInteger(buf, 2));
            time.set(Calendar.MONTH, ChannelBufferTools.readHexInteger(buf, 2) - 1);
            time.set(Calendar.DAY_OF_MONTH, ChannelBufferTools.readHexInteger(buf, 2));
            time.set(Calendar.HOUR_OF_DAY, ChannelBufferTools.readHexInteger(buf, 2));
            time.set(Calendar.MINUTE, ChannelBufferTools.readHexInteger(buf, 2));
            time.set(Calendar.SECOND, ChannelBufferTools.readHexInteger(buf, 2));
            position.setTime(time.getTime());

            // Location
            position.setLatitude(ChannelBufferTools.readCoordinate(buf));
            position.setLongitude(ChannelBufferTools.readCoordinate(buf));
            position.setSpeed(ChannelBufferTools.readHexInteger(buf, 4) * 0.539957);
            position.setCourse((double) ChannelBufferTools.readHexInteger(buf, 4));
            position.setAltitude(0.0);

            // Flags
            int flags = buf.readUnsignedByte();
            position.setValid((flags & 0x80) != 0);
            
            if (type == MSG_ALARM) {
                
                buf.skipBytes(2);

            } else {

                // Milage
                extendedInfo.set("milage", buf.readUnsignedMedium());
            
                // Status
                buf.skipBytes(4);
                
                // Other
                buf.skipBytes(8);

            }
            
            // TODO: parse extra data

            position.setExtendedInfo(extendedInfo.toString());
            return position;
        }

        else if (type == MSG_LOGIN && channel != null) {
            
            buf.skipBytes(4); // serial number
            buf.readByte(); // reserved
            
            ChannelBuffer response = ChannelBuffers.directBuffer(10);
            response.writeByte(0x29); response.writeByte(0x29); // header
            response.writeByte(MSG_CONFIRMATION);
            response.writeShort(5); // size
            response.writeByte(buf.readUnsignedByte());
            response.writeByte(type);
            response.writeByte(0); // reserved
            response.writeByte(Crc.xorChecksum(response.toByteBuffer(0, 8)));
            response.writeByte(0x0D); // ending
            channel.write(response);

        }
        
        return null;
    }

}
