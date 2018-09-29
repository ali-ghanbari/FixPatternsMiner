
/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.zoltran.memorymonitor;

import com.google.common.base.Strings;
import com.zoltran.perf.RecorderFactory;
import java.io.IOException;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class AllocationMonitorAspectTest {

    public AllocationMonitorAspectTest() {
    }
    private long startTime = System.currentTimeMillis();

    private static void testAllocInStaticContext() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            System.err.println("S" + i + Strings.repeat("A", i % 2 * 10));
            if (i % 100 == 0) {
                Thread.sleep(100);
            }
        }
    }

    /**
     * Test of afterAllocation method, of class AllocationMonitorAspect.
     */
    @Test
    public void testAfterAllocation() throws InterruptedException, IOException {
        System.setProperty("perf.memory.sampleTime", "1000");
        System.setProperty("perf.allocations.sampleTime", "1000");
        MemoryUsageSampler.startMemoryUsageSampling(500);
        for (int i = 0; i < 1000; i++) {
            System.err.println("T" + i);
            if (i % 100 == 0) {
                Thread.sleep(500);
            }
        }
        testAllocInStaticContext();
        TestClass.testAllocInStaticContext();
        System.out.println(RecorderFactory.TS_DATABASE.generateCharts(startTime, System.currentTimeMillis(), 1200, 600));
    }
}
