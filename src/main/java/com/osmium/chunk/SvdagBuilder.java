package com.osmium.chunk;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a Sparse Voxel Directed Acyclic Graph (SVDAG) from a 16x16x16 chunk section.
 * Deduplicates identical octree subtrees to compress homogeneous sections to ~0.08 bits per voxel.
 */
public final class SvdagBuilder {

    private final short[] data;
    private final Map<SvdagNode, Long> nodeDeduplicationMap = new HashMap<>();
    private final List<SvdagNode> nodePool = new ArrayList<>();

    public SvdagBuilder(short[] sectionData) {
        this.data = sectionData;
    }

    public static byte[] build(short[] sectionData) {
        if (sectionData == null || sectionData.length != 4096) {
            return new byte[0];
        }
        SvdagBuilder builder = new SvdagBuilder(sectionData);
        long rootOffset = builder.buildOctree(0, 0, 0, 16, (byte) 4);
        return builder.serialize(rootOffset);
    }

    private long buildOctree(int x0, int y0, int z0, int size, byte level) {
        // Base case: check if entire volume is homogeneous
        short firstVal = getVoxel(x0, y0, z0);
        boolean homogeneous = true;

        outer:
        for (int y = y0; y < y0 + size; y++) {
            for (int z = z0; z < z0 + size; z++) {
                for (int x = x0; x < x0 + size; x++) {
                    if (getVoxel(x, y, z) != firstVal) {
                        homogeneous = false;
                        break outer;
                    }
                }
            }
        }

        if (homogeneous) {
            SvdagNode node = SvdagNode.createHomogeneous(level, firstVal);
            return getOrInsert(node);
        }

        // Subdivide into 8 octants
        int half = size / 2;
        byte nextLevel = (byte) (level - 1);
        long[] children = new long[8];
        int childMask = 0;

        int childIdx = 0;
        for (int dy = 0; dy < 2; dy++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int dx = 0; dx < 2; dx++) {
                    int cx = x0 + dx * half;
                    int cy = y0 + dy * half;
                    int cz = z0 + dz * half;
                    long childOffset = buildOctree(cx, cy, cz, half, nextLevel);
                    children[childIdx] = childOffset;
                    childMask |= (1 << childIdx);
                    childIdx++;
                }
            }
        }

        SvdagNode branch = SvdagNode.createBranch(level, childMask, children);
        return getOrInsert(branch);
    }

    private short getVoxel(int x, int y, int z) {
        int idx = (y * 16 + z) * 16 + x;
        return (idx >= 0 && idx < data.length) ? data[idx] : 0;
    }

    private long getOrInsert(SvdagNode node) {
        Long existing = nodeDeduplicationMap.get(node);
        if (existing != null) {
            return existing;
        }
        long offset = nodePool.size();
        nodePool.add(node);
        nodeDeduplicationMap.put(node, offset);
        return offset;
    }

    private byte[] serialize(long rootOffset) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Header: root offset + node count
            dos.writeInt((int) rootOffset);
            dos.writeInt(nodePool.size());

            for (SvdagNode node : nodePool) {
                dos.writeByte(node.level);
                dos.writeBoolean(node.isLeaf);
                dos.writeShort(node.value);
                dos.writeByte(node.childMask);
                dos.writeByte(node.children.length);
                for (long child : node.children) {
                    dos.writeInt((int) child);
                }
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
