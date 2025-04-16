package bloomfilter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BloomFilterUtils {
    public static byte[] serializeAndCompress(BloomFilter<String> filter) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteStream)) {
            filter.writeTo(gzipOut);
            gzipOut.flush();  // ensure all data is written
        }
        return byteStream.toByteArray();
    }

    public static BloomFilter<String> decompressAndDeserialize(byte[] compressedData) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(compressedData);
        try (GZIPInputStream gzipIn = new GZIPInputStream(byteIn)) {
            return BloomFilter.readFrom(gzipIn, Funnels.stringFunnel(StandardCharsets.UTF_8));
        }
    }

    public static byte[] serialize(BloomFilter<String> filter) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        filter.writeTo(out);
        return out.toByteArray();
    }

    public static BloomFilter<String> deserialize(byte[] data) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        return BloomFilter.readFrom(in, Funnels.stringFunnel(StandardCharsets.UTF_8));
    }
}