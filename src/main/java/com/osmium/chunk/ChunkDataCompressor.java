package com.osmium.chunk;

import com.github.luben.zstd.Zstd;
import com.osmium.OsmiumConstants;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class ChunkDataCompressor {
    private static final byte TAG_RAW = 0x00;
    private static final byte TAG_PALETTE = 0x01;
    private static final byte TAG_RLE = 0x02;

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

        // Stage 3: Zstd Compression
        byte[] finalOutput;
        try {
            byte[] zstdCompressed = Zstd.compress(stageOutput, 3);
            ByteBuffer buf = ByteBuffer.allocate(1 + 4 + zstdCompressed.length);
            buf.put((byte) 0x10); // Marker for Zstd-compressed payload
            buf.putInt(stageOutput.length);
            buf.put(zstdCompressed);
            finalOutput = buf.array();
        } catch (Throwable t) {
            // Fallback if Zstd-jni fails
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
            buf.put(TAG_PALETTE);
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
            buf.put(TAG_RLE);
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
            return new short[0];
        }

        ByteBuffer buf = ByteBuffer.wrap(data);
        byte wrapperTag = buf.get();

        byte[] stagePayload;
        if (wrapperTag == 0x10) {
            int originalStageLen = buf.getInt();
            byte[] zstdData = new byte[buf.remaining()];
            buf.get(zstdData);
            stagePayload = Zstd.decompress(zstdData, originalStageLen);
        } else {
            stagePayload = new byte[buf.remaining()];
            buf.get(stagePayload);
        }

        ByteBuffer stageBuf = ByteBuffer.wrap(stagePayload);
        byte stageTag = stageBuf.get();

        if (stageTag == TAG_PALETTE) {
            int paletteSize = stageBuf.getShort() & 0xFFFF;
            short[] palette = new short[paletteSize];
            for (int i = 0; i < paletteSize; i++) {
                palette[i] = stageBuf.getShort();
            }
            int dataLen = stageBuf.remaining();
            short[] out = new short[dataLen];
            for (int i = 0; i < dataLen; i++) {
                int idx = stageBuf.get() & 0xFF;
                out[i] = palette[idx];
            }
            return out;
        } else if (stageTag == TAG_RLE) {
            int runCount = stageBuf.getInt();
            List<Short> outList = new ArrayList<>();
            for (int i = 0; i < runCount; i++) {
                short val = stageBuf.getShort();
                int len = stageBuf.getShort() & 0xFFFF;
                for (int j = 0; j < len; j++) {
                    outList.add(val);
                }
            }
            short[] out = new short[outList.size()];
            for (int i = 0; i < outList.size(); i++) {
                out[i] = outList.get(i);
            }
            return out;
        } else {
            // Raw short array
            int shortCount = stageBuf.remaining() / 2;
            short[] out = new short[shortCount];
            for (int i = 0; i < shortCount; i++) {
                out[i] = stageBuf.getShort();
            }
            return out;
        }
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
