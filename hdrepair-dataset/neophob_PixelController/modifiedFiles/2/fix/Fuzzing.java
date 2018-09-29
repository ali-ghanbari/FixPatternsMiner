/**
 * Copyright (C) 2011-2013 Michael Vogt <michu@neophob.com>
 *
 * This file is part of PixelController.
 *
 * PixelController is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PixelController is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PixelController.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.neophob.sematrix.listener;

import java.util.Random;
import java.util.UUID;

import processing.core.PApplet;

import com.neophob.sematrix.properties.ValidCommands;

/**
 * manual fuzzing test
 * 
 * @author michu
 *
 */
public class Fuzzing {
    
    //@Test
    public void processMessages() {
                
        PApplet.main(new String[] { "com.neophob.PixelController" });
        
        ValidCommands[] allCommands = ValidCommands.values();
        Random r = new Random();
        
        for (int i=0; i<10000; i++) {
            ValidCommands cmd = allCommands[r.nextInt(allCommands.length)];
            String[] param = new String[2];
            param[0] = cmd.toString();
            
            if (i%3==0) {
                param[1] = ""+r.nextInt(512);
            } else if (i%3==1) {
                param[1] = ""+r.nextFloat();
            } else {
                param[1] = UUID.randomUUID().toString();
            }
            
            MessageProcessor.processMsg(param, false, null);
        }
    }


}
