package config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

public class Neo4jConfig {
    private static final String DB_URI = "neo4j+s://423bcb80.databases.neo4j.io";
    private static final String DB_USER = "neo4j";
    private static final String DB_PASSWORD = "dPLxUBqct9iyrv-35nq6x8SnQbesDy9prQnCUxRM0UE";

    private static Driver driver;

    public static Driver getDriver() {
        if (driver == null) {
            driver = GraphDatabase.driver(DB_URI, AuthTokens.basic(DB_USER, DB_PASSWORD));
            driver.verifyConnectivity();
        }
        return driver;
    }

    public static void close() {
        if (driver != null) {
            driver.close();
            driver = null;
        }
    }
}