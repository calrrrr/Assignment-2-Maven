package com.amazonaws.samples;

import java.io.FileReader;
import java.nio.file.Paths;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MusicLoadData {

    public static void main(String[] args) throws Exception {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://localhost:8000",
                                Regions.US_EAST_1.getName()))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("music");

        JSONParser parser = new JSONParser();

        String filePath = "2026a2_songs.json";

        JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(filePath));
        JSONArray songs = (JSONArray) jsonObject.get("songs");

        System.out.println("Loading songs into music table...");

        for (Object obj : songs) {
            JSONObject song = (JSONObject) obj;

            String title = (String) song.get("title");
            String artist = (String) song.get("artist");
            String year = (String) song.get("year");
            String album = (String) song.get("album");
            String artist_image_url = (String) song.get("img_url");

            String artistYear = artist + "#" + year;
            String albumYear = album + "#" + year;

            Item item = new Item()
                    .withPrimaryKey("title", title, "artist_year", artistYear)
                    .withString("artist", artist)
                    .withString("year", year)
                    .withString("album", album)
                    .withString("image_url", artist_image_url)
                    .withString("album_year", albumYear);

            table.putItem(item);

            System.out.println("Inserted: " + title + " - " + artist + " (" + year + ")");
        }

        System.out.println("All songs loaded successfully.");
    }
}