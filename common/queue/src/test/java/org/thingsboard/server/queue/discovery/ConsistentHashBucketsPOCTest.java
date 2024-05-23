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

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

@ResourceLock("one-by-one")
@Slf4j
class ConsistentHashBucketsPOCTest {

    private static long getHash(HashFunction hashFunction, VNode vnode) {
        return hashFunction.hashObject(vnode, VNodeFunnel.INSTANCE).padToLong();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9})
    void circleVirtualNodes(int nodeCount) {
        int partitions = nodeCount;
        log.warn("\n\uD83C\uDFAF CASE: {} nodes {} partitions", nodeCount, partitions);
        ConsistentHashCircle<Long, VNode> circle = new ConsistentHashCircle<>();
        HashFunction hashFunction = Hashing.murmur3_128();
        List<Node> nodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            Node node = new Node(i);
            nodes.add(node);
            for (int j = 0; j < nodeCount * 10; j++) {
                VNode vnode = new VNode(j, node);
                circle.put(getHash(hashFunction, vnode), vnode);
            }
        }

//        for (int i = 0; i < nodeCount; i++) {
//            Node node = new Node(i, "TOPIC-" + i);
//            VNode vnode = new VNode(i, node);
//            long hash = getHash(hashFunction, vnode);
//            //circle.put(hash - 1, vnode);
//
//        }
        log.warn("======== ROLLOUT RESTART ============================================================");
        repartition(circle, nodes, partitions, hashFunction);
        logDistribution(nodes, partitions);

        for (int i = nodeCount - 1; i >= 0; i--) {
            var node = nodes.get(i);
            node.getUp().set(false);
            repartition(circle, nodes, partitions, hashFunction);
            logDistribution(nodes, partitions);
            node.getUp().set(true);
        }
//        repartition(circle, nodes, partitions, hashFunction);
//        logDistribution(nodes, partitions);

        if (nodeCount < 3) {
            return;
        }

        log.warn("======== SCALE DOWN =================================================================");
        repartition(circle, nodes, partitions, hashFunction);
        logDistribution(nodes, partitions);

        for (int i = nodeCount - 1; i >= 1; i--) {
            var node = nodes.get(i);
            node.getUp().set(false);
            repartition(circle, nodes, partitions, hashFunction);
            logDistribution(nodes, partitions);
            //node.getUp().set(true);
        }
        nodes.forEach(node -> node.getUp().set(true));
//        repartition(circle, nodes, partitions, hashFunction);
//        logDistribution(nodes, partitions);

        log.warn("======== ROLLOUT RESTART BY 2 =======================================================");
        repartition(circle, nodes, partitions, hashFunction);
        logDistribution(nodes, partitions);

        for (int i = nodeCount - 2; i >= 0; i--) {
            var node = nodes.get(i);
            node.getUp().set(false);
            nodes.get(i + 1).getUp().set(false);
            repartition(circle, nodes, partitions, hashFunction);
            logDistribution(nodes, partitions);
            nodes.get(i + 1).getUp().set(true);
            node.getUp().set(true);
        }
//        repartition(circle, nodes, partitions, hashFunction);
//        logDistribution(nodes, partitions);

    }

    private void logDistribution(List<Node> nodes, int partitions) {
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_RESET = "\u001B[0m";
        final String RED_X = ANSI_RED + "X" + ANSI_RESET;
        List<String> parts = new ArrayList<>();
        for (Node node : nodes) {
            String part = new StringBuilder()
                    .append(node.up.get() ? node.id : RED_X)
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

        log.warn("{}", sb);
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
        long liveNodesCount = nodes.stream().filter(x -> x.getUp().get()).count();
        //log.warn("liveNodesCount {}", liveNodesCount);
        long floor = partitions / liveNodesCount;                         // 10 / 3 = 3
        long ceil = floor + (partitions % liveNodesCount > 0 ? 1 : 0);    // 4
        long capacity = floor;

        circle.getTotal().set(0);
        circle.getFloor().set(floor);
        circle.getCeil().set(ceil);
        nodes.forEach(node -> node.getPartitions().clear());

        for (int i = 0; i < partitions; i++) {
            Node partition = new Node(i);
            VNode vnode = new VNode(i, partition);
            long hash = getHash(hashFunction, vnode);
            if (circle.getTotal().get() >= liveNodesCount * capacity) {
                capacity++;
                if (capacity > ceil) {
                    throw new RuntimeException("capacity exceed ceil");
                }
            }

            if (assignPartition(circle, circle.tailMap(hash), capacity, i)
                    || assignPartition(circle, circle.getCircle(), capacity, i)) {
                continue;
            }

            throw new RuntimeException("failed to assign partition " + i);
        }
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

    public enum VNodeFunnel implements Funnel<VNode> {
        INSTANCE;

        @Override
        public void funnel(VNode vnode, PrimitiveSink into) {
            into
                    .putUnencodedChars("Node-")
                    .putInt(vnode.getNode().getId())
                    .putUnencodedChars("-")
                    .putInt(vnode.getId())
            ;
            //.putUnencodedChars(vnode.getNode().getName())
        }
    }

}
