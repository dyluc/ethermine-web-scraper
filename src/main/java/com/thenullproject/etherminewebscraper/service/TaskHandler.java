package com.thenullproject.etherminewebscraper.service;


import com.thenullproject.etherminewebscraper.entities.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskHandler {

    private final String SENDER_EMAIL;
    private final String SENDER_PASSWORD;
    private final int POOL_SIZE = 10000; // number of entries handled per task (thread), leave unchanged whilst using ChromeDriver.
    private List<Task> tasks;

    private class Task {

        private List<Entry> entries;
        private int failedEntries;
        private ScheduledExecutorService exec;
        private com.thenullproject.etherminewebscraper.service.PageScraperService pageScraperService;
        private com.thenullproject.etherminewebscraper.service.EmailService emailService;

        private Runnable runnable;

        private Task(Entry entry) {
            this.entries = Collections.synchronizedList(new ArrayList<>());
            entries.add(entry);
            failedEntries = 0;
            exec = Executors.newScheduledThreadPool(1);
            this.pageScraperService = new com.thenullproject.etherminewebscraper.service.PageScraperService();
            this.emailService = new com.thenullproject.etherminewebscraper.service.EmailService(SENDER_EMAIL, SENDER_PASSWORD);

            runnable = () -> {
                // create a clone of the entries first to avoid concurrent access issues

                List<Object[]> allEntryData = new ArrayList<>();
                synchronized (entries) {
                    Iterator<Entry> i = entries.iterator();
                    while (i.hasNext())
                        allEntryData.add( i.next().getData() );
                }

                long start = System.currentTimeMillis();

                System.out.println("\n\n");
                for(Object[] entryData : allEntryData) {
                    // [worker, currentHR, reportedHR, email, http]
                    String worker = (String) entryData[0];
                    float currentHR = (float) entryData[1];
                    float reportedHR = (float) entryData[2];
                    String email = (String) entryData[3];
                    String http = (String) entryData[4];

                    // scrape page

                    com.thenullproject.etherminewebscraper.service.PageScraperService.ScrapeResult result = pageScraperService.scrapePage(http, worker, currentHR, reportedHR);
                    String details = result.getDetails();

                    if(!details.equals("")) {
                        // send out email
                        boolean success = emailService.sendEmail(worker, email, "The current hash rate for " + worker + " has dropped below " + currentHR + " MH/s.");
                        System.out.printf("\nAlert: There has been a hashrate change for worker %s.\n", worker);
                        System.out.println(details);
                        if(success) System.out.println("Successfully sent email.");
                        else System.out.println("Problem occurred attempting to send email, check sender email address details.");
                    } else if(result.isFailed()) {
                        failedEntries++;
                    }

                }

                System.out.printf("Number of failed entries -> %d.\n", failedEntries);
                failedEntries = 0;

                long end = System.currentTimeMillis();
                float executionTimeInSeconds = (end-start)/1000f;
                System.out.printf("Total elapsed time for task with %d entries -> %.4f seconds.\n", allEntryData.size(), executionTimeInSeconds);

                int nextDelay = executionTimeInSeconds < 60 ? (int) Math.ceil(60 - executionTimeInSeconds) : 0;
                System.out.println("Next execution in " + nextDelay + " seconds.");
                exec.schedule(runnable, nextDelay, TimeUnit.SECONDS);
            };
        }

        private void start() {
            exec.schedule(runnable, 0, TimeUnit.SECONDS);
        }

        private void shutdown() {
            pageScraperService.shutdown();
            exec.shutdown();
            try {
                if(!exec.awaitTermination(3, TimeUnit.SECONDS))
                    exec.shutdownNow();
            } catch (InterruptedException e) {
                //...
            }
        }

    }


    public TaskHandler(List<Entry> savedEntries, String senderEmail, String senderPassword) {
        SENDER_EMAIL = senderEmail;
        SENDER_PASSWORD = senderPassword;
        tasks = new ArrayList<>();
        savedEntries.forEach(this::addNewEntry);
    }

    public void addNewEntry(Entry entry) {
        for(Task task : tasks) {
            if(task.entries.size() < POOL_SIZE) {
                task.entries.add(entry);
                return;
            }
        }
        Task newTask = new Task(entry);
        tasks.add(newTask);
        newTask.start();
    }

    public void updateEntry(Entry entry) {
        for(Task task : tasks) {
            for(Entry entry1 : task.entries) {
                if(entry1.equals(entry)) {
                    task.entries.set(task.entries.indexOf(entry1), entry);
                    return;
                }
            }
        }
    }

    public void deleteEntry(Entry entry) {
        int taskIndex = -1;
        int entryIndex = -1;
        for(Task task : tasks) {
            for(Entry entry1 : task.entries) {
                if(entry1.equals(entry)) {
                    taskIndex = tasks.indexOf(task);
                    entryIndex = task.entries.indexOf(entry1);
                }
            }
        }

        if(taskIndex != -1 && entryIndex != -1) {
            tasks.get(taskIndex).entries.remove(entryIndex);
            if(tasks.get(taskIndex).entries.isEmpty()) {
                tasks.get(taskIndex).shutdown();
                tasks.remove(taskIndex);
            }
        }

    }

    public boolean containsEntry(Entry entry) {
        for(Task task : tasks) {
            for(Entry entry1 : task.entries) {
                if(entry1.equals(entry)) return true;
            }
        }
        return false;
    }

    public ArrayList<Entry> getAllEntries() {
        ArrayList<Entry> entries = new ArrayList<>();
        tasks.forEach(task -> entries.addAll(task.entries));
        return entries;
    }

    public void shutdown() {
        tasks.forEach(Task::shutdown);
    }

}
