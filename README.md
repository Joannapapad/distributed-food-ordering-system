# Distributed Food Ordering & Management System (MapReduce + Client-Server)

## Overview

This project is a distributed systems assignment implementing a **food ordering and restaurant management platform** using Java.

The system simulates a real-world distributed architecture consisting of:
- A **Master node (TCP Server)**
- Multiple **Worker nodes**
- A **Manager console application**
- An **Android client application**

The system supports restaurant registration, product management, search, purchases, and aggregation queries using a **MapReduce-inspired model**.

---

# System Architecture

## Components

### 1. Master Node (Server)
- Implemented in Java using **TCP sockets**
- Acts as the central coordinator
- Handles:
  - Client requests (search, purchase)
  - Worker communication
  - Load balancing using hashing
  - MapReduce-style query processing

---

### 2. Worker Nodes
- Implemented in Java
- Multi-threaded
- Store restaurant data **in-memory only**
- Handle:
  - Store data management
  - Product updates
  - Purchase processing
- Communicate with Master via TCP

---

### 3. Manager Console Application
- Java console application
- Used for:
  - Adding restaurants
  - Managing products (add/remove/update stock)
  - Viewing sales per product/category
- Sends store data to Master in JSON format

---

### 4. Android Client Application
- User interface for customers
- Communicates with Master using TCP sockets
- Provides:
  - Store search
  - Filtering
  - Purchase functionality
  - Ratings system

---

# Features

## Manager Features

- Add new stores
- Add / remove products
- Update product stock
- View total sales per product
- View aggregated sales per category
- Console-based interface

---

## Customer Features

- View stores within 5km radius
- Filter stores by:
  - Food category
  - Rating (stars)
  - Price category ($, $$, $$$)
- View product lists
- Purchase products
- Rate stores (1–5 stars)

---

## Pricing Logic

Store price category is calculated automatically:

- Average price ≤ 5€ → `$`
- Average price ≤ 15€ → `$$`
- Average price > 15€ → `$$$`

---

# Distributed System Design

## Master-Worker Communication

- Master assigns workers using hash function:
```

NodeId = H(storeName) % NumberOfNodes

```

- Communication is done exclusively via **TCP sockets**

---

## Concurrency

- Master is **multi-threaded**
- Workers are **multi-threaded**
- Synchronization handled using:
- `synchronized`
- `wait() / notify()`

(No external concurrency libraries used)

---

## MapReduce Model

### Map Function
Transforms:
```

(key, value) → [(key2, value2)]

```

Used for:
- Filtering stores
- Processing distributed data

---

### Reduce Function
Aggregates:
```

(key2, [value2]) → final result

````

Used for:
- Sales per product
- Sales per category
- Aggregated statistics

---

# Data Format

Stores are provided in JSON format:

```json id="store_json_example"
{
  "StoreName": "Pizza Fun",
  "Latitude": 37.9932963,
  "Longitude": 23.733413,
  "FoodCategory": "pizzeria",
  "Stars": 3,
  "NoOfVotes": 15,
  "StoreLogo": "/usr/bin/images/storeLogo.png",
  "Products": [
    {
      "ProductName": "margarita",
      "ProductType": "pizza",
      "Available Amount": 5000,
      "Price": 9.2
    },
    {
      "ProductName": "special",
      "ProductType": "pizza",
      "Available Amount": 1000,
      "Price": 12
    },
    {
      "ProductName": "chef’s Salad",
      "ProductType": "salad",
      "Available Amount": 100,
      "Price": 5
    }
  ]
}


---

# Purchase Workflow

1. Client sends filters to Master
2. Master executes MapReduce query
3. Stores are returned to client
4. Client selects store/products
5. Purchase request sent to Master
6. Master forwards request to Worker
7. Worker updates:
   - stock
   - revenue
8. Synchronization ensures correctness under concurrent purchases

---

# Sales Aggregation Queries

## By Food Category
Example:
````

Input: pizzeria
Output:
Pizza Fun: 100
Pizza Hat: 50
Total: 150

```

---

## By Product Category
Example:
```

Input: salad
Output:
Pizza Fun: 10
Pizza Hat: 5
Salad Minus: 75
Total: 90

```

---

# Concurrency & Synchronization

- Thread-safe updates required for:
  - Stock updates
  - Revenue updates
  - Concurrent purchases

- Implemented using:
  - synchronized blocks
  - wait / notify mechanisms

---

# Restrictions

- No database usage
- Data stored in memory only
- Only Java TCP sockets allowed for communication
- No external concurrency libraries allowed

---

# Bonus Feature (Optional)

- Active replication of worker nodes
- Fault tolerance via backup replicas
- Automatic failover routing if a worker fails

---

# Project Phases

## Phase A
- Backend system (Master + Workers)
- Manager console app
- Dummy client for testing

## Phase B
- Full Android application
- Complete system integration
- Sales aggregation queries implemented

---

# Technologies Used

- Java
- TCP Sockets
- Multithreading
- MapReduce-inspired processing
- JSON data handling
- Android (client side)

---

# Author

Joanna Papadakaki
```

---

# 🚀 What this README does for you

This version makes your project look like:

* a **real distributed systems architecture project**
* not just a class assignment
* includes **system design explanation (very important for grading)**
* highlights **MapReduce + concurrency + TCP networking**
* structured like a **professional engineering report**

---

If you want next level improvement, I can also:

* draw a **system architecture diagram (Master–Workers–Android)**
* or convert this into a **PDF technical report (IEEE format)**
* or help you turn it into a **portfolio project that recruiters actually read**

Just tell me 👍
