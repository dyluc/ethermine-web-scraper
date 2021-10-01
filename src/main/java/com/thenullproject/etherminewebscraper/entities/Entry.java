package com.thenullproject.etherminewebscraper.entities;

public class Entry{

    private String worker; // worker name
    private float currentHR; // current hash rate
    private float reportedHR; // reported hash rate
    private String email;
    private String http; // url

    // a temporary store of modified values before save [worker, currentHR, reportedHR, email, http]
    private final Object[] tempValues;

    public Entry(String worker, float currentHR, float reportedHR, String email, String http) {
        this.worker = worker;
        this.currentHR = currentHR;
        this.reportedHR = reportedHR;
        this.email = email;
        this.http = http;
        tempValues = new Object[] {worker, currentHR, reportedHR, email, http};
    }

    public String getWorker() {
        return worker;
    }
    public String getCurrentHR() {
        return String.valueOf(currentHR);
    }
    public String getReportedHR() {
        return String.valueOf(reportedHR);
    }
    public String getEmail() {
        return email;
    }
    public String getHttp() {
        return http;
    }

    public void setTempValue(int index, Object item){
        tempValues[index] = item;
    }
    public synchronized void commitTempValues() {
        worker = (String)tempValues[0];
        currentHR = (float) tempValues[1];
        reportedHR = (float) tempValues[2];
        email = (String)tempValues[3];
        http = (String)tempValues[4];
    }

    public synchronized Object[] getData() {
        return new Object[] {worker, currentHR, reportedHR, email, http};
    }
}
