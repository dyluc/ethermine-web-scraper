package com.thenullproject.etherminewebscraper.service;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PageScraperService {
    private static ChromeOptions options;
    private static WebDriver driver ;

    public static class ScrapeResult {
        private final String worker;
        private final float currentHR;
        private final float reportedHR;
        private boolean failed; // InterruptedException
        private float activeWorkerCurrentHR;
        private float activeWorkerReportedHR;

        public ScrapeResult(String worker, float currentHR, float reportedHR) {
            this.worker = worker;
            this.currentHR = currentHR;
            this.reportedHR = reportedHR;
            failed = false;
            activeWorkerCurrentHR = -1.0f;
            activeWorkerReportedHR = -1.0f;
        }

        public void setActiveWorkerCurrentHR(float activeWorkerCurrentHR) {
            this.activeWorkerCurrentHR = activeWorkerCurrentHR;
        }


        public void setActiveWorkerReportedHR(float activeWorkerReportedHR) {
            this.activeWorkerReportedHR = activeWorkerReportedHR;
        }

        public String getDetails() {
            if(activeWorkerCurrentHR != -1.0f || activeWorkerReportedHR != -1.0f) {
                StringBuilder details = new StringBuilder();
                if(activeWorkerCurrentHR != -1.0f)
                    details.append("Current hashrate for ").append(worker).append(" has dropped below ").append(currentHR).append(" MH/s and is now at ").append(activeWorkerCurrentHR).append(" MH/s.\n");
                if(activeWorkerReportedHR != -1.0f)
                    details.append("Reported hashrate for ").append(worker).append(" has dropped below ").append(reportedHR).append(" MH/s and is now at ").append(activeWorkerReportedHR).append(" MH/s.\n");
                return details.toString();
            } else
                return "";
        }

        public boolean isFailed() {
            return failed;
        }

        public void setFailed() {
            failed = true;
        }
    }

    public PageScraperService() {
        System.setProperty("webdriver.chrome.driver", ClassLoader.getSystemResource("chromedriver").getPath());

        // turn off logger
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);


        ArrayList<String> args = new ArrayList();
        args.add("--headless");

        options = new ChromeOptions();
        options.addArguments(args);
        options.setExperimentalOption("prefs", prefs);

        driver = new ChromeDriver(options);
        driver.manage().window().setSize(new Dimension(1920, 1080));
        driver.manage().timeouts().implicitlyWait(25, TimeUnit.SECONDS);
    }

    public ScrapeResult scrapePage(String url, String worker, float currentHR, float reportedHR) {
        ScrapeResult result = new ScrapeResult(worker, currentHR, reportedHR);

        try {
            System.out.printf("Scraping the page %s for worker %s...\n", url, worker);

            // scrape document

            driver.get(url); // blocks


            int tableIndex = driver.findElements(By.xpath("//tbody")).size(); // xpath index
            WebElement input = driver.findElement(By.xpath("(//input[@id='worker-search-all'])["+tableIndex+"]"));

            input.sendKeys(worker.toLowerCase()); // blocks

            Thread.sleep(300); // sloppy but it will do

            // take first element of results (could be active or inactive worker depending on tableIndex)
            List<WebElement> activeWorkers = driver.findElements(By.xpath("(//tbody)["+tableIndex+"]/tr"));

            if(activeWorkers.size() > 0) {
                System.out.printf("Found worker %s.\n", worker);
                float activeWorkerCurrentHR = Float.parseFloat(activeWorkers.get(0).findElement(By.xpath("td[@data-label='Current Hashrate']")).getText());
                float activeWorkerReportedHR = Float.parseFloat(activeWorkers.get(0).findElement(By.xpath("td[@data-label='Reported Hashrate']")).getText());
                boolean currentHRConditionMet = activeWorkerCurrentHR < currentHR;
                boolean reportedHRConditionMet = activeWorkerReportedHR < reportedHR;

                if(currentHRConditionMet)
                    result.setActiveWorkerCurrentHR(activeWorkerCurrentHR);
                if(reportedHRConditionMet)
                    result.setActiveWorkerReportedHR(activeWorkerReportedHR);
            } else {
                System.out.println("Couldn't find worker " + worker);
            }

            return result;

        } catch (InterruptedException e) {
            result.setFailed();
            return result;
        }
    }


    public void shutdown() {
        System.out.println("closing WebClient");
        if(driver != null)
            driver.quit();
    }
}
