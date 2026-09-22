package com.osmium.chunk;

import com.osmium.core.OffHeapCache;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Storage interface for SVDAG compressed chunk sections.
 * Stores deduplicated DAGs directly into OffHeapCache.
 */
public final class SvdagStorage {

    private static final AtomicLong totalRawBytes = new AtomicLong(0);
    private static final AtomicLong totalCompressedBytes = new AtomicLong(0);

    private SvdagStorage() {}

    /**
     * Stores a 16x16x16 chunk section using SVDAG compression.
     * @param sectionData short[4096] block states
     * @return Off-heap storage handle, or -1 on failure
     */
    public static long store(short[] sectionData) {
        if (sectionData == null || sectionData.length != 4096) {
            return -1;
        }

        byte[] serialized = SvdagBuilder.build(sectionData);
        if (serialized.length == 0) {
            return -1;
        }

        long handle = OffHeapCache.store(serialized);
        if (handle != -1) {
            totalRawBytes.addAndGet(sectionData.length * 2L);
            totalCompressedBytes.addAndGet(serialized.length);
        }
        return handle;
    }

    /**
     * Loads and reconstructs a 16x16x16 chunk section from an off-heap SVDAG handle.
     * @param handle Off-heap handle
     * @return reconstructed short[4096] block states, or null on failure
     */
    public static short[] load(long handle) {
        byte[] bytes = OffHeapCache.load(handle);
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);

            int rootOffset = dis.readInt();
            int nodeCount = dis.readInt();

            SvdagNode[] nodes = new SvdagNode[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                byte level = dis.readByte();
                boolean isLeaf = dis.readBoolean();
                short value = dis.readShort();
                int childMask = dis.readByte() & 0xFF;
                int childCount = dis.readByte() & 0xFF;
                long[] children = new long[childCount];
                for (int c = 0; c < childCount; c++) {
                    children[c] = dis.readInt();
                }
                nodes[i] = new SvdagNode(level, childMask, children, value, isLeaf);
            }

            short[] out = new short[4096];
            reconstruct(nodes, rootOffset, out, 0, 0, 0, 16);
            return out;
        } catch (IOException e) {
            return null;
        }
    }

    private static void reconstruct(SvdagNode[] nodes, int nodeIdx, short[] out,
                                    int x0, int y0, int z0, int size) {
        if (nodeIdx < 0 || nodeIdx >= nodes.length) return;
        SvdagNode node = nodes[nodeIdx];

        if (node.isLeaf) {
            short val = node.value;
            for (int y = y0; y < y0 + size; y++) {
                for (int z = z0; z < z0 + size; z++) {
                    for (int x = x0; x < x0 + size; x++) {
                        int idx = (y * 16 + z) * 16 + x;
                        if (idx >= 0 && idx < out.length) {
                            out[idx] = val;
                        }
                    }
                }
            }
            return;
        }

        int half = size / 2;
        int childIdx = 0;
        for (int dy = 0; dy < 2; dy++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int dx = 0; dx < 2; dx++) {
                    if (childIdx < node.children.length) {
                        int cx = x0 + dx * half;
                        int cy = y0 + dy * half;
                        int cz = z0 + dz * half;
                        reconstruct(nodes, (int) node.children[childIdx], out, cx, cy, cz, half);
                    }
                    childIdx++;
                }
            }
        }
    }

    /**
     * Returns the overall compression ratio achieved by SVDAG storage.
     */
    public static double compressionRatio() {
        long comp = totalCompressedBytes.get();
        if (comp <= 0) return 1.0;
        return (double) totalRawBytes.get() / comp;
    }
}
