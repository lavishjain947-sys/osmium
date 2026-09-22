package com.osmium.chunk;

import com.github.luben.zstd.Zstd;
import com.osmium.OsmiumConstants;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class ChunkDataCompressor {
    private static final byte TAG_RAW       = 0x00;
    private static final byte TAG_ZSTD      = 0x10;
    private static final byte STAGE_PALETTE = 0x01;
    private static final byte STAGE_RLE     = 0x02;
    private static final byte STAGE_RAW     = 0x03;

    private static final AtomicLong totalUncompressed = new AtomicLong(0);
    private static final AtomicLong totalCompressed = new AtomicLong(0);

    private ChunkDataCompressor() {}

    public static byte[] compress(short[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        int uncompressedBytes = data.length * 2;
        totalUncompressed.addAndGet(uncompressedBytes);

        // Stage 1 & 2: Palette vs RLE
        byte[] stageOutput = stageEncode(data);

        // Stage 3: Zstd Compression wrapper
        byte[] finalOutput;
        try {
            byte[] zstdCompressed = Zstd.compress(stageOutput, 3);
            ByteBuffer buf = ByteBuffer.allocate(1 + 4 + zstdCompressed.length);
            buf.put(TAG_ZSTD);
            buf.putInt(stageOutput.length);
            buf.put(zstdCompressed);
            finalOutput = buf.array();
        } catch (Throwable t) {
            // Fallback: raw wrapper
            ByteBuffer buf = ByteBuffer.allocate(1 + stageOutput.length);
            buf.put(TAG_RAW);
            buf.put(stageOutput);
            finalOutput = buf.array();
        }

        totalCompressed.addAndGet(finalOutput.length);
        return finalOutput;
    }

    private static byte[] stageEncode(short[] data) {
        // Collect unique values
        Map<Short, Integer> uniqueMap = new HashMap<>();
        for (short val : data) {
            if (!uniqueMap.containsKey(val)) {
                uniqueMap.put(val, uniqueMap.size());
                if (uniqueMap.size() > 256) break;
            }
        }

        if (uniqueMap.size() <= 256) {
            // STAGE 1: Palette encoding
            short[] palette = new short[uniqueMap.size()];
            for (Map.Entry<Short, Integer> entry : uniqueMap.entrySet()) {
                palette[entry.getValue()] = entry.getKey();
            }

            ByteBuffer buf = ByteBuffer.allocate(1 + 2 + (palette.length * 2) + data.length);
            buf.put(STAGE_PALETTE);
            buf.putShort((short) palette.length);
            for (short p : palette) {
                buf.putShort(p);
            }
            for (short val : data) {
                buf.put((byte) (int) uniqueMap.get(val));
            }
            return buf.array();
        } else {
            // STAGE 2: RLE encoding
            List<short[]> runs = new ArrayList<>();
            short currentVal = data[0];
            int currentLen = 1;

            for (int i = 1; i < data.length; i++) {
                if (data[i] == currentVal && currentLen < Short.MAX_VALUE) {
                    currentLen++;
                } else {
                    runs.add(new short[]{currentVal, (short) currentLen});
                    currentVal = data[i];
                    currentLen = 1;
                }
            }
            runs.add(new short[]{currentVal, (short) currentLen});

            ByteBuffer buf = ByteBuffer.allocate(1 + 4 + (runs.size() * 4));
            buf.put(STAGE_RLE);
            buf.putInt(runs.size());
            for (short[] run : runs) {
                buf.putShort(run[0]);
                buf.putShort(run[1]);
            }
            return buf.array();
        }
    }

    public static short[] decompress(byte[] data) {
        if (data == null || data.length == 0) {
            return new short[4096];
        }
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(data);
        byte wrapper = buf.get();

        byte[] stagePayload;
        if (wrapper == TAG_ZSTD) {
            int originalStageLen = buf.getInt();
            byte[] zstdData = new byte[buf.remaining()];
            buf.get(zstdData);
            stagePayload = com.github.luben.zstd.Zstd
                    .decompress(zstdData, originalStageLen);
        } else if (wrapper == TAG_RAW) {
            stagePayload = new byte[buf.remaining()];
            buf.get(stagePayload);
        } else {
            // Legacy / unknown: treat full buffer as stage payload
            buf.position(0);
            stagePayload = new byte[buf.remaining()];
            buf.get(stagePayload);
        }

        if (stagePayload.length == 0) return new short[4096];
        byte stageTag = stagePayload[0];
        java.nio.ByteBuffer sb = java.nio.ByteBuffer.wrap(stagePayload);
        sb.get(); // consume stage tag

        if (stageTag == STAGE_PALETTE) {
            int paletteSize = sb.getShort() & 0xFFFF;
            short[] palette = new short[paletteSize];
            for (int i = 0; i < paletteSize; i++) palette[i] = sb.getShort();
            short[] out = new short[4096];
            for (int i = 0; i < 4096; i++) {
                int idx = sb.get() & 0xFF;
                out[i] = (idx < paletteSize) ? palette[idx] : 0;
            }
            return out;
        } else if (stageTag == STAGE_RLE) {
            short[] out = new short[4096];
            int i = 0;
            while (i < 4096 && sb.remaining() >= 4) {
                short v = sb.getShort();
                short len = sb.getShort();
                for (int k = 0; k < len && i < 4096; k++) out[i++] = v;
            }
            return out;
        }
        // Unknown stage tag -> return empty
        return new short[4096];
    }

    public static double ratio() {
        long uncomp = totalUncompressed.get();
        long comp = totalCompressed.get();
        if (comp == 0 || uncomp == 0) return 1.0;
        return (double) uncomp / (double) comp;
    }

    public static void resetStats() {
        totalUncompressed.set(0);
        totalCompressed.set(0);
    }
}
