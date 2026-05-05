package com.amazonaws.samples;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class UploadArtistImagesToS3 {

    public static void main(String[] args) throws Exception {

        String bucketName = "s4076955-group-music-images";
        String filePath = "2026a2_songs.json";

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        Files.createDirectories(Paths.get("artist_images"));

        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(filePath));
        JSONArray songs = (JSONArray) jsonObject.get("songs");

        Set<String> uploadedImages = new HashSet<>();

        for (Object obj : songs) {
            JSONObject song = (JSONObject) obj;

            String artist = (String) song.get("artist");
            String imageUrl = (String) song.get("img_url");

            if (uploadedImages.contains(imageUrl)) {
                continue;
            }

            String cleanArtistName = artist.replaceAll("[^a-zA-Z0-9]", "");
            String fileName = cleanArtistName + ".jpg";
            String localFilePath = "artist_images/" + fileName;

            System.out.println("Downloading: " + imageUrl);

            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, Paths.get(localFilePath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("Uploading to S3: " + fileName);

            s3Client.putObject(new PutObjectRequest(
                    bucketName,
                    fileName,
                    new File(localFilePath)
            ));

            uploadedImages.add(imageUrl);
        }

        System.out.println("All unique artist images uploaded to S3 successfully.");
    }
}