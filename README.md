# Hybrid Graph-Document Medical System

![Java](https://img.shields.io/badge/LANGUAGE-JAVA%2017%2B-orange?style=for-the-badge) ![Neo4j](https://img.shields.io/badge/DATABASE-NEO4J%20GRAPH-blue?style=for-the-badge) ![MongoDB](https://img.shields.io/badge/DATABASE-MONGODB-green?style=for-the-badge)

## Overview

A specialized **Command Line Interface (CLI)** application designed to handle complex medical data structures. This system implements a hybrid architecture leveraging **Neo4j** to map intricate doctor-patient graph relationships and **MongoDB** to handle high-throughput messaging and logs.

## Key Features

* **Hybrid Database Architecture:**
    * **Neo4j (Graph):** Models complex topological relationships between Practitioners, Patients, and Departments.
    * **MongoDB (Document):** optimises write-heavy operations for system messaging and unstructured logs.
* **Complex Relationship Mapping:** Efficiently queries depth-based relationships (e.g., "Find all patients treated by doctors in the Cardiology department").
* **High-Performance Messaging:** Decouples message storage from the graph to ensure system responsiveness during peak loads.
* **Tech Stack:** Java, Neo4j Driver, MongoDB Driver.
