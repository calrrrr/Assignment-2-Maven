package com.amazonaws.samples;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;

public class LoginServer {

    public static void main(String[] args) throws Exception {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://localhost:8000",
                                Regions.US_EAST_1.getName()))
                .build();

//        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
//                .withRegion(Regions.US_EAST_1)
//                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table loginTable = dynamoDB.getTable("login");
        Table musicTable = dynamoDB.getTable("music");
        Table subscriptionsTable = dynamoDB.getTable("subscriptions");

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);

        server.createContext("/login", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 200, "");
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 405, "{\"success\":false}");
                return;
            }

            String body = readBody(exchange);

            String email = extractValue(body, "email");
            String password = extractValue(body, "password");

            Item item = loginTable.getItem("email", email);

            if (item != null && password.equals(item.getString("password"))) {
                String userName = item.getString("user_name");

                String response = "{"
                        + "\"success\":true,"
                        + "\"user_name\":\"" + userName + "\""
                        + "}";

                sendCors(exchange, 200, response);
            } else {
                sendCors(exchange, 200, "{\"success\":false}");
            }
        });

        server.createContext("/register", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 200, "");
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 405, "{\"success\":false}");
                return;
            }

            String body = readBody(exchange);

            String email = extractValue(body, "email");
            String userName = extractValue(body, "user_name");
            String password = extractValue(body, "password");

            Item existingUser = loginTable.getItem("email", email);

            if (existingUser != null) {
                sendCors(exchange, 200, "{\"success\":false}");
            } else {
                loginTable.putItem(new Item()
                        .withPrimaryKey("email", email)
                        .withString("user_name", userName)
                        .withString("password", password)
                );

                sendCors(exchange, 200, "{\"success\":true}");
            }
        });

        server.createContext("/getSubscriptions", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 200, "");
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String email = getQueryParam(exchange.getRequestURI().getQuery(), "email");

                if (email == null || email.isEmpty()) {
                    sendCors(exchange, 400, "{\"error\":\"email query param required\"}");
                    return;
                }

                ItemCollection<QueryOutcome> items = subscriptionsTable.query("email", email);
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                for (Item item : items) {
                    if (!first) json.append(",");
                    json.append(item.toJSON());
                    first = false;
                }
                json.append("]");

                sendCors(exchange, 200, json.toString());

            }
            catch (Exception e) {
                System.out.println("Error in /getSubscriptions: " + e.getMessage());
                e.printStackTrace();
                sendCors(exchange, 500, "{\"Server error\":\"" + e.getMessage() + "\"}");
            }
        });

        server.createContext("/queryMusic", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 200, "");
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String title = getQueryParam(query, "title").trim();
                String year = getQueryParam(query, "year").trim();
                String artist = getQueryParam(query, "artist").trim();
                String album = getQueryParam(query, "album").trim();

                // At least one field must be completed
                if (title.isEmpty() && year.isEmpty() && artist.isEmpty() && album.isEmpty()) {
                    sendCors(exchange, 400, "{\"error\":\"At least one field must be completed\"}");
                    return;
                }

                System.out.println("Query: title=" + title + " year=" + year + " artist=" + artist + " album=" + album);

                ItemCollection<ScanOutcome> items = musicTable.scan();
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                int count = 0;

                for (Item item : items) {
                    String songTitle = item.getString("title");
                    String songYear = item.getString("year");
                    String songArtist = item.getString("artist");
                    String songAlbum = item.getString("album");

                    // AND logic: all provided fields must match
                    boolean match = true;

                    if (!title.isEmpty() &&!songTitle.toLowerCase().contains(title.toLowerCase())) {
                        match = false;
                    }
                    if (!year.isEmpty() &&!songYear.equals(year)) {
                        match = false;
                    }
                    if (!artist.isEmpty() &&!songArtist.toLowerCase().contains(artist.toLowerCase())) {
                        match = false;
                    }
                    if (!album.isEmpty() &&!songAlbum.toLowerCase().contains(album.toLowerCase())) {
                        match = false;
                    }

                    if (match) {
                        if (!first) json.append(",");
                        json.append("{");
                        json.append("\"title\":\"").append(escapeJson(songTitle)).append("\",");
                        json.append("\"artist\":\"").append(escapeJson(songArtist)).append("\",");
                        json.append("\"year\":\"").append(escapeJson(songYear)).append("\",");
                        json.append("\"album\":\"").append(escapeJson(songAlbum)).append("\",");
                        json.append("\"artist_image_url\":\"").append(escapeJson(item.getString("image_url"))).append("\"");
                        json.append("}");
                        first = false;
                        count++;
                    }
                }

                json.append("]");

                System.out.println("Found " + count + " results");
                sendCors(exchange, 200, json.toString());

            } catch (Exception e) {
                System.out.println("Error in /queryMusic: " + e.getMessage());
                e.printStackTrace();
                sendCors(exchange, 500, "{\"error\":\"Server error\"}");
            }
        });

        server.createContext("/addSubscription", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 200, "");
                return;
            }
            String body = readBody(exchange);
            String title = extractValue(body, "title");
            String artist = extractValue(body, "artist");

            subscriptionsTable.putItem(new Item()
                    .withPrimaryKey("email", extractValue(body, "email"), "title", title + "#" + artist)
                    .withString("artist", artist)
                    .withString("title", title)
                    .withString("year", extractValue(body, "year"))
                    .withString("album", extractValue(body, "album"))
                    .withString("artist_image_url", extractValue(body, "artist_image_url"))
            );
            sendCors(exchange, 200, "{\"success\":true}");
        });

        server.createContext("/removeSubscription", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendCors(exchange, 200, "");
                return;
            }
            String body = readBody(exchange);
            String title = extractValue(body, "title");
            String artist = extractValue(body, "artist");

            subscriptionsTable.deleteItem(
                    "email", extractValue(body, "email"),
                    "title", title
            );
            sendCors(exchange, 200, "{\"success\":true}");
        });

        server.start();
        System.out.println("Login server running on AWS");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );

        StringBuilder body = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            body.append(line);
        }

        return body.toString();
    }

    private static String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);

        if (start == -1) {
            return "";
        }

        start += search.length();
        int end = json.indexOf("\"", start);

        if (end == -1) {
            return "";
        }

        return json.substring(start, end);
    }

    private static String getQueryParam(String query, String key) {
        if (query == null) return "";
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    return kv[1];
                }
            }
        }
        return "";
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static void sendCors(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}