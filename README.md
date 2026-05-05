# Distributed Food Ordering Platform (Java, TCP, MapReduce)

## Overview

This project is a distributed food ordering and management system built in Java. It simulates a real-world backend architecture using a **Master–Worker model**, supporting multiple clients, concurrent requests, and distributed data processing.

The system allows:
- Managers to register and manage stores
- Customers to search, filter, and purchase products
- Distributed processing of queries using a MapReduce-inspired approach

The goal of the project is to demonstrate concepts from:
- Distributed systems
- Multithreading and synchronization
- Network programming (TCP sockets)
- Data partitioning and aggregation

---

## System Architecture

The system is composed of four main components:

### Master Node
The Master acts as the central coordinator of the system.

Responsibilities:
- Accepts client and manager requests via TCP
- Distributes stores across workers using hashing
- Executes MapReduce-style queries
- Coordinates purchase operations
- Handles concurrency between multiple clients and workers

---

### Worker Nodes
Workers are responsible for storing and managing store data.

Responsibilities:
- Store restaurant and product data (in memory)
- Handle product updates and stock changes
- Process purchase requests
- Return results to the Master

Each worker runs as a multi-threaded server and communicates only with the Master.

---

### Manager Application (Console)
A command-line interface used to manage the system.

Features:
- Add new stores (via JSON input)
- Add / remove products
- Update product availability
- View aggregated sales statistics

---

### Customer Application (Android)
A mobile interface that allows users to interact with the system.

Features:
- Search for nearby stores (within 5 km)
- Apply filters:
  - Food category
  - Rating
  - Price range ($, $$, $$$)
- View store details and products
- Purchase products
- Rate stores

All communication is done asynchronously using TCP sockets.

---

## Data Distribution

Stores are distributed across workers using a hash function:

```

NodeId = H(storeName) % NumberOfWorkers

````

This ensures:
- Load balancing
- Scalable data distribution

---

## MapReduce Processing

The system uses a MapReduce-inspired model to process queries across workers.

### Map Phase
Each worker processes its local data and returns partial results.

### Reduce Phase
The Master aggregates results from all workers into a final response.

Used for:
- Store search filtering
- Sales aggregation queries

---

## Store Data Format

Stores are defined using JSON:

```json
{
  "StoreName": "Pizza Fun",
  "Latitude": 37.9932963,
  "Longitude": 23.733413,
  "FoodCategory": "pizzeria",
  "Stars": 3,
  "NoOfVotes": 15,
  "StoreLogo": "/path/to/logo.png",
  "Products": [
    {
      "ProductName": "margarita",
      "ProductType": "pizza",
      "Available Amount": 5000,
      "Price": 9.2
    }
  ]
}
````

---

## Pricing Categories

Price category is calculated automatically:

* `$` → average price ≤ 5€
* `$$` → average price ≤ 15€
* `$$$` → average price > 15€

---

## Purchase Flow

1. User searches for stores
2. Master retrieves matching results using MapReduce
3. User selects products
4. Purchase request is sent to Master
5. Master forwards request to the responsible worker
6. Worker updates:

   * product stock
   * store revenue

Thread synchronization ensures correct handling of concurrent purchases.

---

## Aggregation Queries

Managers can retrieve statistics such as:

### Sales per Store (by category)

Example:

```
Input: pizzeria
Pizza Fun: 100
Pizza Hat: 50
Total: 150
```

### Sales per Product Category

Example:

```
Input: salad
Pizza Fun: 10
Pizza Hat: 5
Other: 75
Total: 90
```

---

## Concurrency & Synchronization

The system is fully multi-threaded:

* Master handles multiple clients simultaneously
* Workers process requests in parallel
* Shared data is protected using:

  * synchronized blocks
  * wait / notify

No external concurrency libraries are used.

---

## Constraints

* No database (all data stored in memory)
* Communication strictly via TCP sockets
* No use of external frameworks for networking or concurrency

---

## Bonus (Optional)

Support for **active replication**:

* Data is replicated across multiple workers
* Fault tolerance in case of node failure
* Automatic request redirection to replicas

---

## Technologies

* Java
* TCP Sockets
* Multithreading
* JSON
* Android (client application)
* MapReduce concepts

---

## Author

Joanna Papadakaki
