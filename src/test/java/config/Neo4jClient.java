package config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;

public class Neo4jClient {
    public static void main(String[] args) {

        final String dbUri = "neo4j+s://423bcb80.databases.neo4j.io";
        final String dbUser = "neo4j";
        final String dbPassword = "dPLxUBqct9iyrv-35nq6x8SnQbesDy9prQnCUxRM0UE";

        try (var driver = GraphDatabase.driver(dbUri, AuthTokens.basic(dbUser, dbPassword))) {
            driver.verifyConnectivity();
            System.out.println("Connection established.");
        }
    }
}