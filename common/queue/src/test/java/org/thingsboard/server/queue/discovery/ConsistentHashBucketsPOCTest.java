/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.queue.discovery;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentNavigableMap;

@ResourceLock("one-by-one")
@Slf4j
class ConsistentHashBucketsPOCTest {

    int partitions = 6;
    int vNodesPerNode = Math.min(10, PrimeNumbers.FIRST_1000_PRIMES.length);

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9})
    void circleVirtualNodes(int nodeCount) {
        log.warn("\n\uD83C\uDFAF CASE: {} nodes {} partitions", nodeCount, partitions);
        ConsistentHashCircle<Long, VNode> circle = new ConsistentHashCircle<>();
        HashFunction hashFunction = Hashing.murmur3_128(5198731);
        List<Node> nodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            Node node = new Node("tb-rule-engine-" + i);
            nodes.add(node);
            for (int j = 0; j < nodeCount * vNodesPerNode; j++) {
                VNode vnode = new VNode(j, node);
                circle.put(getHash(hashFunction, vnode), vnode);
            }
        }

        SortedMap<String, Set<Integer>> snapshotOK;
        log.warn("======== ROLLOUT RESTART ============================================================");
        nodes.forEach(node -> node.getUp().set(true));
        repartition(circle, nodes, partitions, hashFunction);
        snapshotOK = createSnapshot(nodes);
        logDistribution(nodes, partitions,0);
        int totalDiff = 0;
        for (int i = nodeCount - 1; i >= 0; i--) {
            var node = nodes.get(i);
            node.getUp().set(false);
            repartition(circle, nodes, partitions, hashFunction);
            var snapshot = createSnapshot(nodes);
            int diff = calculateDiff(snapshotOK, snapshot);
            totalDiff += diff;
            logDistribution(nodes, partitions, diff);
            node.getUp().set(true);
        }
        log.warn("ℹ️ totalDiff with the OK state {}", totalDiff);

        if (nodeCount < 3) {
            return;
        }

        log.warn("======== SCALE DOWN =================================================================");
        nodes.forEach(node -> node.getUp().set(true));
        repartition(circle, nodes, partitions, hashFunction);
        var previous = createSnapshot(nodes);
        logDistribution(nodes, partitions, 0);
        totalDiff = 0;

        for (int i = nodeCount - 1; i >= 1; i--) {
            var node = nodes.get(i);
            node.getUp().set(false);
            repartition(circle, nodes, partitions, hashFunction);
            var snapshot = createSnapshot(nodes);
            int diff = calculateDiff(previous, snapshot);
            previous = snapshot;
            totalDiff += diff;
            logDistribution(nodes, partitions, diff);
            //node.getUp().set(true);
        }
        log.warn("ℹ️ totalDiff with previous state {}", totalDiff);


        log.warn("======== ROLLOUT RESTART BY 2 =======================================================");
        nodes.forEach(node -> node.getUp().set(true));
        repartition(circle, nodes, partitions, hashFunction);
        snapshotOK = createSnapshot(nodes);
        logDistribution(nodes, partitions,0);
        totalDiff = 0;

        for (int i = nodeCount - 2; i >= 0; i--) {
            var node = nodes.get(i);
            node.getUp().set(false);
            nodes.get(i + 1).getUp().set(false);
            repartition(circle, nodes, partitions, hashFunction);
            var snapshot = createSnapshot(nodes);
            int diff = calculateDiff(snapshotOK, snapshot);
            totalDiff += diff;
            logDistribution(nodes, partitions, diff);
            nodes.get(i + 1).getUp().set(true);
            node.getUp().set(true);
        }
        log.warn("ℹ️ totalDiff with the OK state {}", totalDiff);

    }

    private long getHash(HashFunction hashFunction, VNode vnode) {
        return hashFunction.hashObject(vnode, VNodeFunnel.INSTANCE).padToLong();
    }

    private int calculateDiff(SortedMap<String, Set<Integer>> snapshotOK, SortedMap<String, Set<Integer>> snapshot) {
        int diff = 0;
        for (var entry : snapshotOK.entrySet()) {
            String key = entry.getKey();
            Set<Integer> set1 = entry.getValue();
            Set<Integer> set2 = snapshot.get(key);

            if (set2 != null) {
                Set<Integer> diffSet = new HashSet<>(set2);
                diffSet.removeAll(set1); // Symmetric difference
//                if (!diffSet.isEmpty()) {
//                    log.warn("diffSet {} for key {}", diffSet.size(), key);
//                }
                diff += diffSet.size();
            } else {
                log.warn("Set not found for key {}", key);
                diff += set1.size(); // If key is not present in snapshot, add size of set1
            }
        }
        // Also need to check for keys in snapshot that are not in snapshotOK
        for (var entry : snapshot.entrySet()) {
            String key = entry.getKey();
            if (!snapshotOK.containsKey(key)) {
                log.warn("SetOK not found for key {}", key);
                diff += entry.getValue().size();
            }
        }
        return diff;
    }

    private SortedMap<String, Set<Integer>> createSnapshot(List<Node> nodes) {
        SortedMap<String, Set<Integer>> snapshot = new TreeMap<>();
        for (var node: nodes) {
            snapshot.put(node.getId(), Set.copyOf(node.getPartitions()));
        }
        return Collections.unmodifiableSortedMap(snapshot);
    }

    private void logDistribution(List<Node> nodes, int partitions, int diff) {
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_RESET = "\u001B[0m";
        final String RED_X = ANSI_RED + "X" + ANSI_RESET;
        List<String> parts = new ArrayList<>();
        for (Node node : nodes) {
            String part = new StringBuilder()
                    .append(node.up.get() ? node.id.substring(node.id.length() - 1) : RED_X)
                    //.append(node.id)
                    .append("->")
                    .append("[")
                    .append(getPartitionsAsFixedString(node.getPartitions(), partitions))
                    .append("]")
                    .toString();
            parts.add(part);
        }

        var sb = new StringBuilder();
        for (String part : parts) {
            sb
                    .append(part)
                    //.append(" ".repeat((floor + 1) * 3 + 5 - part.length()))
                    .append("   ")
            ;
        }

        log.warn("{} diff {}", sb, diff);
    }

    private String getPartitionsAsFixedString(Set<Integer> partitions, int partitionCount) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partitionCount; i++) {
            if (i > 0) {
                sb.append("");
            }
            String hexString = Integer.toHexString(i);
            String num = partitions.contains(i) ? hexString : "";
            sb.append(num);
            sb.append(" ".repeat(Math.max(0, hexString.length() - num.length())));
        }
        return sb.toString();
    }

    private void repartition(ConsistentHashCircle<Long, VNode> circle, List<Node> nodes, int partitions, HashFunction hashFunction) {
        nodes.forEach(node -> node.getPartitions().clear());
        circle.getTotal().set(0);
        long liveNodesCount = nodes.stream().filter(x -> x.getUp().get()).count();
        //log.warn("liveNodesCount {}", liveNodesCount);

        SortedMap<Integer, VNode> stickyMap = new TreeMap<>();
        for (int i = 0; i < partitions; i++) {
            Node partition = new Node("tb_rule_engine.main." + i);
            matchVNodePartition(stickyMap, nodes, partition, i);
        }

        SortedMap<Integer, VNode> liveMap = new TreeMap<>(stickyMap);
        SortedMap<Integer, VNode> failedMap = new TreeMap<>();
        // extract failed nodes to the failedMap
        for (var entry : stickyMap.entrySet()) {
            int partitionId = entry.getKey();
            VNode vNode = entry.getValue();
            if (vNode.getNode().getUp().get()) {
                continue;
            }
            liveMap.remove(partitionId);
            failedMap.put(partitionId, vNode);
        }

        fillByCircle(circle, hashFunction, liveNodesCount, liveMap);

        if (!failedMap.isEmpty()) {
            fillByCircle(circle, hashFunction, liveNodesCount, failedMap);
        }

    }

    private VNode matchVNodePartition(Map<Integer, VNode> stickyMap, List<Node> nodes, Node partition, int partitionNum) {
        VNode stikyNode = stickyMap.get(partitionNum);
        if (stikyNode != null) {
            return stikyNode;
        }

        int vIdx = partitionNum / nodes.size();
        int nodeIdx = partitionNum % nodes.size();

        for (Node node : nodes) {
            if (node.getId().endsWith("-" + nodeIdx)) {
                VNode vnode = new VNode(vIdx, node);
                stickyMap.put(partitionNum, vnode);
                return vnode;
            }
        }

        return new VNode(0, partition);
    }

    private boolean assignPartition(ConsistentHashCircle<Long, VNode> circle, ConcurrentNavigableMap<Long, VNode> tailMap, long capacity, int i) {
        for (Map.Entry<Long, VNode> entry : tailMap.entrySet()) {
            Node node = entry.getValue().getNode();
            if (!node.getUp().get()) {
                continue;
            }
            if (node.getPartitions().size() < capacity) {
                //log.warn("assignPartition partition {} capacity {} total {} node {} already assigned partitions {}", i, capacity, circle.getTotal().get(), node.getId(), node.getPartitions());
                node.getPartitions().add(i);
                circle.getTotal().incrementAndGet();
                return true;
            }
        }
        return false;
    }

    private void fillByCircle(ConsistentHashCircle<Long, VNode> circle, HashFunction hashFunction, long liveNodesCount, SortedMap<Integer, VNode> partitionsMap) {
        int partitionsToDistribute = partitionsMap.size();
        long floor = (partitionsToDistribute + circle.getTotal().get()) / liveNodesCount;                         // 10 / 3 = 3
        long ceil = floor + ((partitionsToDistribute + circle.getTotal().get() ) % liveNodesCount > 0 ? 1 : 0);    // 4
        long capacity = floor;

        for (var entry : partitionsMap.entrySet()) {
            int partitionId = entry.getKey();
            VNode vnode = entry.getValue();

            long hash = getHash(hashFunction, vnode);
            if (circle.getTotal().get() >= liveNodesCount * capacity) {
                capacity++;
                if (capacity > ceil) {
                    throw new RuntimeException("capacity exceed ceil " + partitionId);
                }
            }

            if (assignPartition(circle, circle.tailMap(hash), capacity, partitionId)
                    || assignPartition(circle, circle.getCircle(), capacity, partitionId)) {
                continue;
            }

            throw new RuntimeException("failed to assign partition " + partitionId);
        }
    }

}
