package com.amazonaws.samples;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;

public class SubscriptionsCreateTable {

    public static void main(String[] args) throws Exception {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://localhost:8000",
                                Regions.US_EAST_1.getName()))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        String tableName = "subscriptions";

        try {
            System.out.println("Creating subscriptions table...");

            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(tableName)
                    .withKeySchema(
                            new KeySchemaElement("email", KeyType.HASH),
                            new KeySchemaElement("title", KeyType.RANGE)
                    )
                    .withAttributeDefinitions(
                            new AttributeDefinition("email", ScalarAttributeType.S),
                            new AttributeDefinition("title", ScalarAttributeType.S)
                    )
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

            Table table = dynamoDB.createTable(request);
            table.waitForActive();
            System.out.println("Success. Table status: " + table.getDescription().getTableStatus());

        } catch (Exception e) {
            System.err.println("Unable to create subscriptions table:");
            System.err.println(e.getMessage());
        }
    }
}