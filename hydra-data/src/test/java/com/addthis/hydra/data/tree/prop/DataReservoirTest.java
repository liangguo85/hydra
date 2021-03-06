/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydra.data.tree.prop;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import java.math.RoundingMode;

import com.addthis.basis.util.ClosableIterator;

import com.addthis.bundle.value.ValueArray;
import com.addthis.bundle.value.ValueObject;
import com.addthis.hydra.data.tree.DataTreeNode;

import com.google.common.math.DoubleMath;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DataReservoirTest {

    @Test
    public void testResizeReservoir() {
        DataReservoir reservoir = new DataReservoir();
        reservoir.updateReservoir(0, 4);
        reservoir.updateReservoir(1, 4);
        reservoir.updateReservoir(2, 4);
        reservoir.updateReservoir(3, 4);
        assertEquals(-2, reservoir.retrieveCount(4));
        reservoir.updateReservoir(4, 8);
        reservoir.updateReservoir(5, 8);
        assertEquals(1, reservoir.retrieveCount(4));
        assertEquals(1, reservoir.retrieveCount(5));
        assertEquals(0, reservoir.retrieveCount(6));
        assertEquals(0, reservoir.retrieveCount(7));
        assertEquals(-2, reservoir.retrieveCount(8));
        reservoir.updateReservoir(5, 4);
        assertEquals(-1, reservoir.retrieveCount(3));
        assertEquals(1, reservoir.retrieveCount(4));
        assertEquals(2, reservoir.retrieveCount(5));
        assertEquals(0, reservoir.retrieveCount(6));
        assertEquals(0, reservoir.retrieveCount(7));
        assertEquals(-2, reservoir.retrieveCount(8));
    }

    @Test
    public void testSimpleUpdateReservoir() {
        DataReservoir reservoir = new DataReservoir();
        assertEquals(-3, reservoir.retrieveCount(0));
        assertEquals(-3, reservoir.retrieveCount(10));
        assertEquals(-3, reservoir.retrieveCount(-10));
        reservoir.updateReservoir(0, 4);
        reservoir.updateReservoir(1, 4);
        reservoir.updateReservoir(2, 4);
        reservoir.updateReservoir(3, 4);
        assertEquals(1, reservoir.retrieveCount(0));
        assertEquals(1, reservoir.retrieveCount(1));
        assertEquals(1, reservoir.retrieveCount(2));
        assertEquals(1, reservoir.retrieveCount(3));
        assertEquals(-1, reservoir.retrieveCount(-1));
        assertEquals(-2, reservoir.retrieveCount(4));
        reservoir.updateReservoir(5, 4);
        assertEquals(-1, reservoir.retrieveCount(0));
        assertEquals(-1, reservoir.retrieveCount(1));
        assertEquals(1, reservoir.retrieveCount(2));
        assertEquals(1, reservoir.retrieveCount(3));
        assertEquals(0, reservoir.retrieveCount(4));
        assertEquals(1, reservoir.retrieveCount(5));
        assertEquals(-2, reservoir.retrieveCount(6));
        reservoir.updateReservoir(9, 4);
        assertEquals(-1, reservoir.retrieveCount(5));
        assertEquals(0, reservoir.retrieveCount(6));
        assertEquals(0, reservoir.retrieveCount(7));
        assertEquals(0, reservoir.retrieveCount(8));
        assertEquals(1, reservoir.retrieveCount(9));
        assertEquals(-2, reservoir.retrieveCount(10));
    }

    @Test
    public void testCompoundUpdateReservoir() {
        DataReservoir reservoir = new DataReservoir();
        reservoir.updateReservoir(1, 4, 4);
        reservoir.updateReservoir(2, 4, 8);
        reservoir.updateReservoir(3, 4, 4);
        reservoir.updateReservoir(4, 4, 4);
        assertEquals(4, reservoir.retrieveCount(1));
        assertEquals(8, reservoir.retrieveCount(2));
        assertEquals(4, reservoir.retrieveCount(3));
        assertEquals(4, reservoir.retrieveCount(4));
    }

    @Test
    public void testMergeEqualSizedReservoir() {
        DataReservoir reservoir1 = new DataReservoir();
        reservoir1.updateReservoir(1, 4, 4);
        reservoir1.updateReservoir(2, 4, 8);
        reservoir1.updateReservoir(3, 4, 4);
        reservoir1.updateReservoir(4, 4, 4);
        DataReservoir reservoir2 = new DataReservoir();
        DataReservoir reservoir3 = reservoir1.merge(reservoir2);
        assertEquals(4, reservoir3.retrieveCount(1));
        assertEquals(8, reservoir3.retrieveCount(2));
        assertEquals(4, reservoir3.retrieveCount(3));
        assertEquals(4, reservoir3.retrieveCount(4));
        reservoir2.updateReservoir(1, 4, 1);
        reservoir2.updateReservoir(2, 4, 2);
        reservoir2.updateReservoir(3, 4, 3);
        reservoir2.updateReservoir(4, 4, 4);
        reservoir3 = reservoir1.merge(reservoir2);
        assertEquals(5, reservoir3.retrieveCount(1));
        assertEquals(10, reservoir3.retrieveCount(2));
        assertEquals(7, reservoir3.retrieveCount(3));
        assertEquals(8, reservoir3.retrieveCount(4));
    }

    @Test
    public void testMergeUnequalSizedReservoir() {
        DataReservoir reservoir1 = new DataReservoir();
        reservoir1.updateReservoir(1, 4, 4);
        reservoir1.updateReservoir(2, 4, 8);
        reservoir1.updateReservoir(3, 4, 4);
        reservoir1.updateReservoir(4, 4, 4);
        DataReservoir reservoir2 = new DataReservoir();
        reservoir2.updateReservoir(0, 3, 1);
        reservoir2.updateReservoir(1, 3, 2);
        reservoir2.updateReservoir(2, 3, 3);
        DataReservoir reservoir3 = reservoir1.merge(reservoir2);
        assertEquals(1, reservoir3.retrieveCount(0));
        assertEquals(6, reservoir3.retrieveCount(1));
        assertEquals(11, reservoir3.retrieveCount(2));
        assertEquals(4, reservoir3.retrieveCount(3));
        assertEquals(4, reservoir3.retrieveCount(4));
    }

    @Test
    public void testGetNodesBadInput() {
        DataReservoir reservoir = new DataReservoir();
        reservoir.updateReservoir(1, 4, 4);
        reservoir.updateReservoir(2, 4, 12);
        reservoir.updateReservoir(3, 4, 4);
        reservoir.updateReservoir(4, 4, 100);
        List<DataTreeNode> result = reservoir.getNodes(null, "epoch=4~sigma=2.0");
        assertEquals(0, result.size());
        result = reservoir.getNodes(null, "epoch=4~obs=3");
        assertEquals(0, result.size());
        result = reservoir.getNodes(null, "sigma=2.0~obs=3");
        assertEquals(0, result.size());
        result = reservoir.getNodes(null, "epoch=4~sigma=2.0~obs=4");
        assertEquals(0, result.size());
        result = reservoir.getNodes(null, "epoch=4~sigma=2.0~obs=3");
        assertEquals(1, result.size());
    }

    @Test
    public void testGetNodes() {
        DataReservoir reservoir = new DataReservoir();
        reservoir.updateReservoir(1, 4, 4);
        reservoir.updateReservoir(2, 4, 12);
        reservoir.updateReservoir(3, 4, 4);
        reservoir.updateReservoir(4, 4, 100);
        List<DataTreeNode> result = reservoir.getNodes(null, "epoch=4~sigma=2.0~obs=3");
        assertEquals(1, result.size());
        DataTreeNode node = result.iterator().next();
        assertEquals(node.getName(), "delta");
        assertEquals(86, node.getCounter());
        node = node.getIterator().next();
        assertEquals(node.getName(), "measurement");
        assertEquals(100, node.getCounter());
        node = node.getIterator().next();
        assertEquals(node.getName(), "mean");
        assertEquals(7, node.getCounter());
        node = node.getIterator().next();
        assertEquals(node.getName(), "stddev");
        assertEquals(4, node.getCounter());
        node = node.getIterator().next();
        assertEquals(node.getName(), "threshold");
        assertEquals(14, node.getCounter());
    }

    @Test
    public void testGetValue() {
        DataReservoir reservoir = new DataReservoir();
        reservoir.updateReservoir(1, 4, 4);
        reservoir.updateReservoir(2, 4, 12);
        reservoir.updateReservoir(3, 4, 4);
        reservoir.updateReservoir(4, 4, 100);
        ValueArray result = reservoir.getValue("epoch||4~sigma||2.0~obs||3").asArray();
        assertEquals(5, result.size());
        assertEquals(86, DoubleMath.roundToLong(result.get(0).asDouble().getDouble(), RoundingMode.HALF_UP));
        assertEquals(100, result.get(1).asLong().getLong());
        assertEquals(7, DoubleMath.roundToLong(result.get(2).asDouble().getDouble(), RoundingMode.HALF_UP));
        assertEquals(4, DoubleMath.roundToLong(result.get(3).asDouble().getDouble(), RoundingMode.HALF_UP));
        assertEquals(14, result.get(4).asLong().getLong());

        // test mode "get"
        assertEquals(0, reservoir.getValue("mode||get~epoch||0").asLong().getLong());
        assertEquals(4, reservoir.getValue("mode||get~epoch||1").asLong().getLong());
        assertEquals(12, reservoir.getValue("mode||get~epoch||2").asLong().getLong());
        assertEquals(4, reservoir.getValue("mode||get~epoch||3").asLong().getLong());
        assertEquals(100, reservoir.getValue("mode||get~epoch||4").asLong().getLong());
        assertEquals(0, reservoir.getValue("mode||get~epoch||5").asLong().getLong());
    }

    @Test
    public void testGetSerialization() {
        DataReservoir reservoir = new DataReservoir();
        reservoir.updateReservoir(1, 4, 4);
        reservoir.updateReservoir(2, 4, 12);
        reservoir.updateReservoir(3, 4, 4);
        reservoir.updateReservoir(4, 4, 100);
        DataReservoir.DataReservoirValue original = (DataReservoir.DataReservoirValue) reservoir.getValue("epoch||4~sigma||2.0~obs||3");
        DataReservoir.DataReservoirValue translated = new DataReservoir.DataReservoirValue();
        translated.setValues(original.asMap());
        ValueArray result = translated.asArray();
        assertEquals(5, result.size());
        assertEquals(86, DoubleMath.roundToLong(result.get(0).asDouble().getDouble(), RoundingMode.HALF_UP));
        assertEquals(100, result.get(1).asLong().getLong());
        assertEquals(7, DoubleMath.roundToLong(result.get(2).asDouble().getDouble(), RoundingMode.HALF_UP));
        assertEquals(4, DoubleMath.roundToLong(result.get(3).asDouble().getDouble(), RoundingMode.HALF_UP));
        assertEquals(14, result.get(4).asLong().getLong());

        // test mode "get"
        assertEquals(0, reservoir.getValue("mode||get~epoch||0").asLong().getLong());
        assertEquals(4, reservoir.getValue("mode||get~epoch||1").asLong().getLong());
        assertEquals(12, reservoir.getValue("mode||get~epoch||2").asLong().getLong());
        assertEquals(4, reservoir.getValue("mode||get~epoch||3").asLong().getLong());
        assertEquals(100, reservoir.getValue("mode||get~epoch||4").asLong().getLong());
        assertEquals(0, reservoir.getValue("mode||get~epoch||5").asLong().getLong());
    }


    @Test
    public void testGetNodesWithMin() {
        DataReservoir reservoir = new DataReservoir();
        reservoir.updateReservoir(1, 4, 4);
        reservoir.updateReservoir(2, 4, 12);
        reservoir.updateReservoir(3, 4, 4);
        reservoir.updateReservoir(4, 4, 100);
        List<DataTreeNode> result = reservoir.getNodes(null, "epoch=4~sigma=2.0~obs=3~min=101");
        assertEquals(0, result.size());
        reservoir.updateReservoir(4, 4);
        result = reservoir.getNodes(null, "epoch=4~sigma=2.0~obs=3~min=101");
        assertEquals(1, result.size());
    }

    @Test
    public void testGetNodesWithRaw() {
        DataReservoir reservoir = new DataReservoir();
        reservoir.updateReservoir(1, 4, 4);
        reservoir.updateReservoir(2, 4, 12);
        reservoir.updateReservoir(3, 4, 4);
        reservoir.updateReservoir(4, 4, 100);
        List<DataTreeNode> result = reservoir.getNodes(null, "raw=true");
        assertEquals(1, result.size());
        for(DataTreeNode node : result) {
            switch (node.getName()) {
                case "observations": {
                    ClosableIterator<DataTreeNode> children = node.getIterator();
                    DataTreeNode child;
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("4", child.getName());
                    assertEquals(100, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("3", child.getName());
                    assertEquals(4, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("2", child.getName());
                    assertEquals(12, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("1", child.getName());
                    assertEquals(4, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("minEpoch", child.getName());
                    assertEquals(1, child.getCounter());
                    assertFalse(children.hasNext());
                    children.close();
                    break;
                }
                default:
                    fail("Unexpected node " + node.getName());
            }
        }
        result = reservoir.getNodes(null, "epoch=4~sigma=2.0~obs=3~raw=true");
        assertEquals(2, result.size());
        for(DataTreeNode node : result) {
            switch (node.getName()) {
                case "delta":
                    break;
                case "observations": {
                    ClosableIterator<DataTreeNode> children = node.getIterator();
                    DataTreeNode child;
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("4", child.getName());
                    assertEquals(100, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("3", child.getName());
                    assertEquals(4, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("2", child.getName());
                    assertEquals(12, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("1", child.getName());
                    assertEquals(4, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("minEpoch", child.getName());
                    assertEquals(1, child.getCounter());
                    assertFalse(children.hasNext());
                    children.close();
                    break;
                }
                default:
                    fail("Unexpected node " + node.getName());
            }
        }
        result = reservoir.getNodes(null, "epoch=4~sigma=2.0~obs=2~raw=true");
        assertEquals(2, result.size());
        for(DataTreeNode node : result) {
            switch (node.getName()) {
                case "delta":
                    break;
                case "observations": {
                    ClosableIterator<DataTreeNode> children = node.getIterator();
                    DataTreeNode child;
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("4", child.getName());
                    assertEquals(100, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("3", child.getName());
                    assertEquals(4, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("2", child.getName());
                    assertEquals(12, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("minEpoch", child.getName());
                    assertEquals(1, child.getCounter());
                    assertFalse(children.hasNext());
                    children.close();
                    break;
                }
                default:
                    fail("Unexpected node " + node.getName());
            }
        }
        result = reservoir.getNodes(null, "epoch=3~sigma=-2.0~obs=2~raw=true");
        assertEquals(2, result.size());
        for(DataTreeNode node : result) {
            switch (node.getName()) {
                case "delta":
                    break;
                case "observations": {
                    ClosableIterator<DataTreeNode> children = node.getIterator();
                    DataTreeNode child;
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("3", child.getName());
                    assertEquals(4, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("2", child.getName());
                    assertEquals(12, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("1", child.getName());
                    assertEquals(4, child.getCounter());
                    assertTrue(children.hasNext());
                    child = children.next();
                    assertEquals("minEpoch", child.getName());
                    assertEquals(1, child.getCounter());
                    assertFalse(children.hasNext());
                    children.close();
                    break;
                }
                default:
                    fail("Unexpected node " + node.getName());
            }
        }
    }

    @Test
    public void testSerialization() {
        DataReservoir reservoir = new DataReservoir();
        byte[] encoding = reservoir.bytesEncode(0);
        assertEquals(0, encoding.length);
        reservoir.bytesDecode(encoding, 0);
        assertEquals(-3, reservoir.retrieveCount(0));
        reservoir.updateReservoir(0, 4);
        reservoir.updateReservoir(1, 4);
        reservoir.updateReservoir(2, 4);
        reservoir.updateReservoir(3, 4);
        encoding = reservoir.bytesEncode(0);
        reservoir = new DataReservoir();
        reservoir.bytesDecode(encoding, 0);
        assertEquals(1, reservoir.retrieveCount(0));
        assertEquals(1, reservoir.retrieveCount(1));
        assertEquals(1, reservoir.retrieveCount(2));
        assertEquals(1, reservoir.retrieveCount(3));
        assertEquals(-1, reservoir.retrieveCount(-1));
        assertEquals(-2, reservoir.retrieveCount(4));
    }

    private DataTreeNode retrieveNode(Iterator<DataTreeNode> iterator, String... names) {
        if (names.length == 0) {
            return null;
        }
        while (iterator.hasNext()) {
            DataTreeNode node = iterator.next();
            if (node.getName().equals(names[0])) {
                if (names.length == 1) {
                    return node;
                } else {
                    return retrieveNode(node.iterator(), Arrays.copyOfRange(names, 1, names.length));
                }
            }
        }
        return null;
    }

    private static final String[] percentilePath = {"delta", "measurement", "mean", "stddev", "mode", "percentile"};

    @Test
    public void testModelFitting() {
        DataReservoir reservoir = new DataReservoir();
        reservoir.updateReservoir(1, 10, 0);
        reservoir.updateReservoir(2, 10, 0);
        reservoir.updateReservoir(3, 10, 0);
        reservoir.updateReservoir(4, 10, 0);
        reservoir.updateReservoir(5, 10, 0);
        reservoir.updateReservoir(6, 10, 0);
        reservoir.updateReservoir(7, 10, 0);
        reservoir.updateReservoir(8, 10, 0);
        reservoir.updateReservoir(9, 10, 1);
        reservoir.updateReservoir(10, 10, 1);
        List<DataTreeNode> result = reservoir.modelFitAnomalyDetection(10, 9, true, true, 0, 0);
        DataTreeNode percentile = retrieveNode(result.iterator(), percentilePath);
        assertTrue(Double.longBitsToDouble(percentile.getCounter()) > 90.0);
        reservoir = new DataReservoir();
        reservoir.updateReservoir(1, 10, 10);
        reservoir.updateReservoir(2, 10, 10);
        reservoir.updateReservoir(3, 10, 10);
        reservoir.updateReservoir(4, 10, 10);
        reservoir.updateReservoir(5, 10, 10);
        reservoir.updateReservoir(6, 10, 10);
        reservoir.updateReservoir(7, 10, 10);
        reservoir.updateReservoir(8, 10, 10);
        reservoir.updateReservoir(9, 10, 9);
        reservoir.updateReservoir(10, 10, 11);
        result = reservoir.modelFitAnomalyDetection(10, 9, true, true, 0, 0);
        percentile = retrieveNode(result.iterator(), percentilePath);
        assertTrue(Double.longBitsToDouble(percentile.getCounter()) > 90.0);
    }

}
