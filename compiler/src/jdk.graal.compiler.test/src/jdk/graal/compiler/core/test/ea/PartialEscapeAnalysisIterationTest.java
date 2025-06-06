/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.graal.compiler.core.test.ea;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import jdk.graal.compiler.nodes.EndNode;
import jdk.graal.compiler.nodes.extended.BoxNode;
import jdk.graal.compiler.nodes.extended.UnboxNode;
import jdk.graal.compiler.nodes.java.StoreFieldNode;
import jdk.graal.compiler.nodes.virtual.CommitAllocationNode;
import jdk.graal.compiler.virtual.phases.ea.PartialEscapePhase;

public class PartialEscapeAnalysisIterationTest extends EATestBase {

    @Override
    protected void canonicalizeGraph() {
        super.canonicalizeGraph();
    }

    private static final class AllocatedObject {
        private int value;

        AllocatedObject(int value) {
            this.value = value;
        }

        AllocatedObject() {
            // empty
        }
    }

    public static int cnt;
    public static volatile Object obj1;
    public static volatile Double object1 = (double) 123;
    public static volatile AllocatedObject object2 = new AllocatedObject(123);

    public static String moveIntoBranchBox(int id) {
        Double box = object1 + 1;
        if (id == 0) {
            // Prevent if simplification
            cnt++;
            obj1 = new AtomicReference<>(box);
        }
        return "value";
    }

    public static String moveIntoBranch(int id) {
        AllocatedObject box = new AllocatedObject(object2.value + 1);
        if (id == 0) {
            obj1 = new AtomicReference<>(box);
        }
        return "value";
    }

    @Test
    public void testJMHBlackholePattern() {
        /*
         * The overall number of allocations in these methods does not change during PEA, but the
         * effects still need to be applied since they move the allocation between blocks.
         */

        // test with a boxing object
        prepareGraph("moveIntoBranchBox", false);
        Assert.assertEquals(1, graph.getNodes().filter(UnboxNode.class).count());
        Assert.assertEquals(1, graph.getNodes().filter(BoxNode.class).count());
        // the boxing needs to be moved into the branch
        Assert.assertTrue(graph.getNodes().filter(BoxNode.class).first().next() instanceof CommitAllocationNode);

        // test with a normal object
        prepareGraph("moveIntoBranch", false);
        Assert.assertEquals(1, graph.getNodes().filter(CommitAllocationNode.class).count());
        // the allocation needs to be moved into the branch
        Assert.assertTrue(graph.getNodes().filter(CommitAllocationNode.class).first().next() instanceof StoreFieldNode);
    }

    public static String noLoopIterationBox(int id) {
        Double box = object1 + 1;
        for (int i = 0; i < 100; i++) {
            if (id == i) {
                obj1 = new AtomicReference<>(box);
            }
        }
        return "value";
    }

    public static String noLoopIteration(int id) {
        AllocatedObject box = new AllocatedObject(object2.value + 1);
        for (int i = 0; i < 100; i++) {
            if (id == i) {
                obj1 = new AtomicReference<>(box);
            }
        }
        return "value";
    }

    public static String noLoopIterationEmpty(int id) {
        AllocatedObject box = new AllocatedObject();
        for (int i = 0; i < 100; i++) {
            if (id == i) {
                obj1 = new AtomicReference<>(box);
            }
        }
        return "value";
    }

    @Test
    public void testNoLoopIteration() {
        /*
         * After PEA, the BoxNode stays outside the loop.
         */

        // test with a boxing object
        prepareGraph("noLoopIterationBox", true);
        List<BoxNode> boxNodes = graph.getNodes().filter(BoxNode.class).snapshot();
        Assert.assertEquals(1, boxNodes.size());
        Assert.assertTrue(boxNodes.getFirst().next() instanceof EndNode);

        // test with a normal object (needs one iteration to replace NewInstance with
        // CommitAllocation)
        for (String name : new String[]{"noLoopIterationEmpty", "noLoopIteration"}) {
            prepareGraph(name, false);
            List<CommitAllocationNode> allocations = graph.getNodes().filter(CommitAllocationNode.class).snapshot();
            new PartialEscapePhase(true, false, createCanonicalizerPhase(), null, graph.getOptions()).apply(graph, context);
            Assert.assertEquals(2, allocations.size());
            Assert.assertTrue(allocations.get(0).isAlive());
            Assert.assertTrue(allocations.get(1).isAlive());
        }
    }

    public static int foldFloatLessThanSelf(short y) {
        Double x = (double) y;
        int i = 0;
        while (x < x) {
            if (i++ >= 100) {
                break;
            }
            x = x + 1;
        }
        return i;
    }

    @Test
    public void testFloatLessThanSelf() {
        prepareGraph("foldFloatLessThanSelf", true);
        new PartialEscapePhase(true, false, createCanonicalizerPhase(), null, graph.getOptions()).apply(graph, context);
    }
}
