package com.github.monitor.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DemoApplication {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Demo Application Starting...");
        
        // Start a thread to generate exceptions
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    processRequest();
                } catch (Exception e) {
                    System.err.println("Caught exception in main loop: " + e.getMessage());
                }
            }
        }, "request-processor").start();
        
        // Keep main thread alive
        Thread.currentThread().join();
    }

    private static void processRequest() {
        int r = ThreadLocalRandom.current().nextInt(100);
        if (r < 30) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", 123);
            data.put("name", "test-user");
            data.put("sensitive", "password123"); // Should be masked
            
            throw new IllegalArgumentException("Invalid argument with data: " + data);
        } else if (r < 60) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < 100; i++) list.add("item-" + i); // Should be truncated
            
            throw new RuntimeException("Runtime error with large list", new Exception("Cause"));
        } else {
            System.out.println("Processed successfully.");
        }
    }
}
