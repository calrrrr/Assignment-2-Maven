package com.amazonaws.samples;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

public class LoginLoadData {

    public static void main(String[] args) throws Exception {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://localhost:8000",
                                Regions.US_EAST_1.getName()))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        Table table = dynamoDB.getTable("login");

        String studentId = "s4076955";
        String firstName = "Calvier";
        String lastName = "Koh";

        String[] passwords = {
                "012345", "123456", "234567", "345678", "456789",
                "567890", "678901", "789012", "890123", "901234"
        };

        try {
            System.out.println("Adding 10 login users...");

            for (int i = 0; i < 10; i++) {
                String email = studentId + i + "@student.rmit.edu.au";
                String username = firstName + lastName + i;

                table.putItem(new Item()
                        .withPrimaryKey("email", email)
                        .withString("user_name", username)
                        .withString("password", passwords[i])
                );

                System.out.println("Inserted user: " + email);
            }

            System.out.println("All login users inserted successfully.");

        } catch (Exception e) {
            System.err.println("Unable to insert login users:");
            System.err.println(e.getMessage());
        }
    }
}