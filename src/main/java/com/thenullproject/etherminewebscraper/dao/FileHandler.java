package com.thenullproject.etherminewebscraper.dao;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.thenullproject.etherminewebscraper.entities.Entry;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FileHandler {

    /**
     * Update existing config json file containing past entries. Returns false if an IOException occurred or true if
     * successful.
     * @param entries a List of all entries to write to file
     * @return boolean indicating if write to file was a success
     */
    public static boolean updateEntries(List<Entry> entries) {
        JSONArray allEntries = new JSONArray();

        entries.forEach(entry -> {
            JSONObject entryObject = new JSONObject();
            entryObject.put("worker", entry.getWorker());
            entryObject.put("currentHR", entry.getCurrentHR());
            entryObject.put("reportedHR", entry.getReportedHR());
            entryObject.put("email", entry.getEmail());
            entryObject.put("http", entry.getHttp());
            allEntries.add(entryObject);
        });

        try(FileWriter fileWriter = new FileWriter("entries.json")) {
            fileWriter.write(allEntries.toString());
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    /**
     * Loads existing config json file containing past entries. Returns an ArrayList<Entry> containing the loaded
     * entries, or null if there was an IOException or ParseException.
     * @return
     */
    public static ArrayList<Entry> loadEntries() {
        ArrayList<Entry> entries = new ArrayList<>();
        try(FileReader fileReader = new FileReader("entries.json")) {
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(fileReader);

            jsonArray.forEach(entryItem -> {
                JSONObject entryObject = (JSONObject) entryItem;
                entries.add(
                        new Entry(
                                (String)entryObject.get("worker"),
                                Float.parseFloat((String)entryObject.get("currentHR")),
                                Float.parseFloat((String)entryObject.get("reportedHR")),
                                (String)entryObject.get("email"),
                                (String)entryObject.get("http"))
                );
            });

        } catch(FileNotFoundException e) {
            try (FileWriter fileWriter = new FileWriter("entries.json")){
                fileWriter.write("[]");
            } catch(IOException e1) {
                return entries;
            }
        } catch(IOException | ParseException e) {
            return null;
        }
        return entries;
    }

    public static Properties loadEmailDetails() {
        // Properties
        try(FileReader fileReader = new FileReader("sender_email_details.properties")) {
            Properties props = new Properties();
            props.load(fileReader);
            return props;
        } catch(FileNotFoundException e) {
            // create file
            try(FileWriter fileWriter = new FileWriter("sender_email_details.properties")) {
                Properties props = new Properties();
                props.setProperty("sender.email", "YOUR_EMAIL_HERE");
                props.setProperty("sender.password", "YOUR_PASSWORD_HERE");
                props.store(fileWriter, "Sender login details - Gmail");
                return props;
            } catch(IOException ignored) {
                return null;
            }
        } catch(IOException e) {
            return null;
        }
    }

}
