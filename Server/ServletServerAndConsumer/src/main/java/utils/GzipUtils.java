package utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipUtils {
    public static byte[] gzipCompress(byte[] data) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(data);
        }
        return byteStream.toByteArray();
    }

    public static byte[] gzipDecompress(byte[] compressedData) throws IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
        try (GZIPInputStream gzipStream = new GZIPInputStream(byteStream)) {
            return gzipStream.readAllBytes();
        }
    }
}
