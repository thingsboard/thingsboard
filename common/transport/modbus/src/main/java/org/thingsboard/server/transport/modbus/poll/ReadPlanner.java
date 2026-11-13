// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.poll;

import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the values a device profile wants to poll into as few Modbus read requests as possible.
 * <p>
 * Background: a Modbus device exposes numbered slots ("addresses"). A read request asks for a continuous range
 * of slots, e.g. "give me 8 slots starting at address 0". Every request is a network round trip, often to a slow
 * serial bus behind a gateway, so reading {@code voltage@0} and {@code current@6} in one request is much cheaper than
 * two requests. The planner does that merging:
 * <ol>
 *     <li><b>Group by function code.</b> The function code says which kind of slots are read (coils, discrete inputs,
 *     holding registers, input registers). Each kind is a separate address space and needs its own request.</li>
 *     <li><b>Sort each group by address.</b> Neighbours in the address space become neighbours in the list.</li>
 *     <li><b>Merge neighbours into blocks.</b> Walk the sorted list and keep extending the current block while the
 *     next value is close enough ({@code maxGap}) and the block stays within the per-request size limit
 *     of the function code. Otherwise start a new block.</li>
 * </ol>
 * Example with {@code maxGap = 8}, all input registers: {@code voltage@0..1}, {@code current@6..7},
 * {@code energy@72..75} become two reads: {@code [0, 8)} for voltage and current (slots 2–5 are read and ignored),
 * and {@code [72, 76)} for energy (gap of 64 slots is too large to bridge).
 */
public final class ReadPlanner {

    private ReadPlanner() {
    }

    /**
     * @param items  values to poll; the same slot may appear more than once (e.g. as telemetry and as an attribute)
     * @param maxGap how many unused slots may be read in between two values to save a separate request
     */
    public static List<ReadBlock> plan(Collection<PollItem> items, int maxGap) {
        List<ReadBlock> blocks = new ArrayList<>();
        groupByFunctionCode(items).forEach((functionCode, group) -> {
            sortByAddress(group);
            blocks.addAll(mergeIntoBlocks(functionCode, group, maxGap));
        });
        return blocks;
    }

    /** Step 1: one group per kind of slot, because one request reads only one kind. */
    private static Map<ModbusFunctionCode, List<PollItem>> groupByFunctionCode(Collection<PollItem> items) {
        Map<ModbusFunctionCode, List<PollItem>> groups = new EnumMap<>(ModbusFunctionCode.class);
        for (PollItem item : items) {
            groups.computeIfAbsent(item.mapping().getFunctionCode(), functionCode -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    /** Step 2: lowest address first; on equal addresses the shorter value first. */
    private static void sortByAddress(List<PollItem> group) {
        group.sort(Comparator.comparingInt(PollItem::address).thenComparingInt(PollItem::count));
    }

    /** Step 3: walk the sorted values and grow a block while the next one fits; otherwise close it and start a new one. */
    private static List<ReadBlock> mergeIntoBlocks(ModbusFunctionCode functionCode, List<PollItem> sortedGroup, int maxGap) {
        List<ReadBlock> blocks = new ArrayList<>();
        BlockBuilder current = null;
        for (PollItem item : sortedGroup) {
            if (current != null && current.canAccept(item, maxGap)) {
                current.add(item);
            } else {
                if (current != null) {
                    blocks.add(current.build());
                }
                current = new BlockBuilder(functionCode, item);
            }
        }
        if (current != null) {
            blocks.add(current.build());
        }
        return blocks;
    }

    /**
     * A read request being assembled: covers slots {@code [start, end)} and the values decoded from them.
     */
    private static final class BlockBuilder {

        private final ModbusFunctionCode functionCode;
        private final int start;
        /** Exclusive: the first slot after the block. */
        private int end;
        private final List<PollItem> items = new ArrayList<>();

        BlockBuilder(ModbusFunctionCode functionCode, PollItem first) {
            this.functionCode = functionCode;
            this.start = first.address();
            this.end = endOf(first);
            this.items.add(first);
        }

        /**
         * The item joins this block if both hold:
         * <ul>
         *     <li><b>close enough</b>: at most {@code maxGap} unused slots between the block and the item
         *     (overlapping items give a negative gap and always qualify);</li>
         *     <li><b>small enough</b>: the grown block still fits the function code's per-request limit
         *     (125 registers or 2000 coils); devices reject larger requests.</li>
         * </ul>
         */
        boolean canAccept(PollItem item, int maxGap) {
            int unusedSlotsBetween = item.address() - end;
            int sizeIfAdded = endIfAdded(item) - start;
            return unusedSlotsBetween <= maxGap && sizeIfAdded <= functionCode.getMaxCount();
        }

        void add(PollItem item) {
            end = endIfAdded(item);
            items.add(item);
        }

        ReadBlock build() {
            return new ReadBlock(functionCode, start, end - start, List.copyOf(items));
        }

        /** An item may lie entirely inside the block (e.g. a short value inside a long string), so the block never shrinks. */
        private int endIfAdded(PollItem item) {
            return Math.max(end, endOf(item));
        }

        private static int endOf(PollItem item) {
            return item.address() + item.count();
        }

    }

}
