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

public class LoginServer {

    public static void main(String[] args) throws Exception {

//        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
//                .withEndpointConfiguration(
//                        new AwsClientBuilder.EndpointConfiguration(
//                                "http://localhost:8000",
//                                Regions.US_EAST_1.getName()))
//                .build();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table loginTable = dynamoDB.getTable("login");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

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

        server.start();
        System.out.println("Login server running on http://localhost:8080");
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

    private static void sendCors(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}