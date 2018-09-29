/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package solver.search;


import org.testng.Assert;
import org.testng.annotations.Test;
import solver.ResolutionPolicy;
import solver.Solver;
import solver.constraints.ICF;
import solver.search.loop.monitors.IMonitorSolution;
import solver.variables.BoolVar;
import solver.variables.IntVar;
import solver.variables.VF;

import java.util.Random;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 19/07/13
 */
public class ObjectiveTest {

    @Test(groups = "1s")
    public void test1() {
        Solver solver = new Solver();

        IntVar iv = VF.enumerated("iv", -5, 15, solver);
        solver.post(ICF.arithm(iv, ">=", 0));
        solver.post(ICF.arithm(iv, "<=", 10));
        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            int k = rnd.nextInt(4);
            switch (k) {
                case 0:
                    one(solver, iv);
                    break;
                case 1:
                    all(solver, iv);
                    break;
                case 2:
                    min(solver, iv);
                    break;
                case 3:
                    max(solver, iv);
                    break;
            }
        }
    }

    private void one(Solver solver, IntVar iv) {
        for (int i = 0; i < 2; i++) {
            solver.getSearchLoop().reset();
            solver.findSolution();
            Assert.assertEquals(solver.getMeasures().getSolutionCount(), 1);
            Assert.assertEquals(solver.getMeasures().getNodeCount(), 2);
        }
    }

    private void all(Solver solver, IntVar iv) {
        for (int i = 0; i < 2; i++) {
            solver.getSearchLoop().reset();
            solver.findAllSolutions();
            Assert.assertEquals(solver.getMeasures().getSolutionCount(), 11);
            Assert.assertEquals(solver.getMeasures().getNodeCount(), 21);
        }
    }

    private void min(Solver solver, IntVar iv) {
        for (int i = 0; i < 2; i++) {
            solver.getSearchLoop().reset();
            solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, iv);
            Assert.assertEquals(solver.getMeasures().getBestSolutionValue(), 0);
            Assert.assertEquals(solver.getMeasures().getNodeCount(), 2);
        }
    }

    private void max(Solver solver, IntVar iv) {
        for (int i = 0; i < 2; i++) {
            solver.getSearchLoop().reset();
            solver.findOptimalSolution(ResolutionPolicy.MAXIMIZE, iv);
            Assert.assertEquals(solver.getMeasures().getBestSolutionValue(), 10);
            Assert.assertEquals(solver.getMeasures().getNodeCount(), 21);
        }
    }

    @Test(groups = "1s")
    public void test2() {
        Solver solver = new Solver();
        IntVar iv = VF.enumerated("iv", 0, 10, solver);
        solver.post(ICF.arithm(iv, ">=", 2));

        solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, iv);
        Assert.assertEquals(iv.getValue(), 2);

        solver.getSearchLoop().reset();

        solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, iv);
        Assert.assertEquals(iv.getValue(), 2);
    }

    @Test(groups = "1s")
    public void test3() {
        final Solver solver = new Solver();
        final IntVar iv = VF.enumerated("iv", 0, 10, solver);
        solver.post(ICF.arithm(iv, ">=", 2));

        solver.getSearchLoop().plugSearchMonitor(new IMonitorSolution() {
            @Override
            public void onSolution() {
                solver.post(ICF.arithm(iv, ">=", 4));
            }
        });
        solver.findSolution();
        Assert.assertEquals(iv.getValue(), 2);

        solver.getSearchLoop().reset();
        solver.getSearchLoop().plugSearchMonitor(new IMonitorSolution() {
            @Override
            public void onSolution() {
                solver.postCut(ICF.arithm(iv, ">=", 6));
            }
        });
        solver.findSolution();
        Assert.assertEquals(iv.getValue(), 2);

        solver.getSearchLoop().reset();
        solver.findSolution();
        Assert.assertEquals(iv.getValue(), 6);
    }

    @Test(groups = "1s")
    public void test4() {
        Solver solver = new Solver();
        IntVar iv = VF.enumerated("iv", 0, 10, solver);
        BoolVar v = ICF.arithm(iv, "<=", 2).reif();

        solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, v);
        System.out.println("Minimum1: " + iv + " : " + solver.isEntailed());

        solver.getSearchLoop().reset();

        solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, v);
        System.out.println("Minimum2: " + iv + " : " + solver.isEntailed());
    }

}
