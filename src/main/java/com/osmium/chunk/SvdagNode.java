package com.osmium.chunk;

import java.util.Arrays;
import java.util.Objects;

/**
 * Node in the Sparse Voxel Directed Acyclic Graph (SVDAG).
 * Deduplicated across identical subtrees to achieve ultra-dense chunk compression.
 */
public final class SvdagNode {
    public final byte level;         // 0 = 1x1x1 leaf, 4 = 16x16x16 root
    public final int childMask;      // 8-bit mask indicating non-empty / non-homogeneous octants
    public final long[] children;    // Offsets into node pool
    public final short value;        // Block state raw ID for homogeneous/leaf nodes
    public final boolean isLeaf;

    public SvdagNode(byte level, int childMask, long[] children, short value, boolean isLeaf) {
        this.level = level;
        this.childMask = childMask;
        this.children = children != null ? children : new long[0];
        this.value = value;
        this.isLeaf = isLeaf;
    }

    public static SvdagNode createLeaf(short value) {
        return new SvdagNode((byte) 0, 0, null, value, true);
    }

    public static SvdagNode createHomogeneous(byte level, short value) {
        return new SvdagNode(level, 0, null, value, true);
    }

    public static SvdagNode createBranch(byte level, int childMask, long[] children) {
        return new SvdagNode(level, childMask, children, (short) 0, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SvdagNode other)) return false;
        return level == other.level
                && childMask == other.childMask
                && value == other.value
                && isLeaf == other.isLeaf
                && Arrays.equals(children, other.children);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(level, childMask, value, isLeaf);
        result = 31 * result + Arrays.hashCode(children);
        return result;
    }
}
