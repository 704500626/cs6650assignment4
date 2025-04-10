package edu.northeastern.common;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class CsvWriterThread implements Runnable {
    private final BlockingQueue<String[]> queue;
    private final String filePath;

    public CsvWriterThread(BlockingQueue<String[]> queue, String filePath) {
        this.queue = queue;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            writer.writeNext(new String[]{"StartTime", "RequestType", "Latency", "ResponseCode"});

            while (true) {
                String[] record = queue.take();
                if (record.length == 1 && "EOF".equals(record[0])) {
                    break;
                }
                writer.writeNext(record);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void calculateMetrics(String filePath) throws IOException {
        List<Long> latencies = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            reader.readNext(); // Skip header
            while ((nextLine = reader.readNext()) != null) {
                latencies.add(Long.parseLong(nextLine[2]));
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }

        Collections.sort(latencies);
        double total = latencies.stream().mapToLong(Long::longValue).sum();
        double mean = total / latencies.size();
        double median = latencies.get(latencies.size() / 2);
        double min = latencies.get(0);
        double max = latencies.get(latencies.size() - 1);
        double p99 = latencies.get((int) (latencies.size() * 0.99));

        System.out.println("Mean response time: " + mean + " ms");
        System.out.println("Median response time: " + median + " ms");
        System.out.println("Min response time: " + min + " ms");
        System.out.println("Max response time: " + max + " ms");
        System.out.println("P99 response time: " + p99 + " ms");
    }

    // "StartTime","RequestType","Latency","ResponseCode"
    //"1739395196540","POST","146","201"
    //"1739395196540","POST","146","201"

    public static void calculateThroughputOverTime(String filePath) throws IOException {
        List<Map.Entry<Long, Long>> startTimeAndLatency = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            reader.readNext(); // Skip header
            while ((nextLine = reader.readNext()) != null) {
                startTimeAndLatency.add(Map.entry(Long.parseLong(nextLine[0]), Long.parseLong(nextLine[2])));
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }

        startTimeAndLatency.sort(Map.Entry.comparingByKey());
        long initialStartTime = startTimeAndLatency.get(0).getKey();

        for (int i = 0; i < startTimeAndLatency.size(); i++) {
            long endTime = startTimeAndLatency.get(i).getKey() + startTimeAndLatency.get(i).getValue();
            System.out.println("Timestamp: " + endTime + ", Throughput: " + (double)i * 1000 / (endTime - initialStartTime) + "requests/s");
        }
    }

    public static void calculateThroughputOverTime(String inputFilePath, String outputFilePath) throws IOException {
        List<Map.Entry<Long, Long>> startTimeAndLatency = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(inputFilePath))) {
            String[] nextLine;
            reader.readNext(); // Skip header
            while ((nextLine = reader.readNext()) != null) {
                startTimeAndLatency.add(Map.entry(Long.parseLong(nextLine[0]), Long.parseLong(nextLine[2])));
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }

        startTimeAndLatency.sort(Map.Entry.comparingByKey());
        long initialStartTime = startTimeAndLatency.get(0).getKey();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("Timestamp,Throughput\n");
            for (int i = 0; i < startTimeAndLatency.size(); i++) {
                long endTime = startTimeAndLatency.get(i).getKey() + startTimeAndLatency.get(i).getValue();
                double throughput = (double) i * 1000 / (endTime - initialStartTime);
                writer.write(endTime + "," + throughput + "\n");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        calculateThroughputOverTime("request_metrics.csv", "throughput.txt");
    }
}

