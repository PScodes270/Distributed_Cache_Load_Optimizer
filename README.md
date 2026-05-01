# 🚀 Distributed Cache Load Optimizer (Load Balancing Simulation)

A system-level project that simulates how real-world backend systems distribute incoming requests across multiple servers using optimized load balancing strategies, caching, and concurrency.

---

## 📌 Overview

This project demonstrates a scalable backend architecture where requests are routed efficiently using a **greedy load balancing algorithm**, managed through a **priority queue**, and processed concurrently using **multithreading**.

It also includes a **real-time monitoring dashboard** to track system performance.

---

## 🧠 Key Concepts Used

- Load Balancing Algorithms (Greedy Selection)
- Priority Queue (Request Scheduling)
- Multithreading & Synchronization
- System Design (Modular Architecture)
- Caching Mechanism
- Real-time Monitoring

---

## ⚙️ System Architecture

![Architecture Diagram](./assets/architecture.png)

### Flow:
1. Requests are generated
2. Added to a **priority queue**
3. Load balancer selects best server using:
   - Current load
   - Latency
4. Requests are processed by servers
5. Monitoring system tracks:
   - Throughput
   - Latency
   - Server health

---

## 🔧 Features

- ⚡ Greedy-based server selection
- 📊 Real-time system monitoring dashboard
- 🔁 Concurrent request handling using threads
- 🧠 Intelligent request scheduling (priority queue)
- 📦 Modular code structure (easy to extend)

---

## 💡 My Contribution (Team Lead)

- Designed **core load balancing logic**
- Implemented **greedy scoring mechanism** (latency + load)
- Built **priority queue system** for request handling
- Managed **multithreading and synchronization**
- Led integration of all modules into a working system

---

## 👥 Team Members

- Esheta Chugh  
- Nishandeep Singh  
- Tejas Joshi  

---

## 🛠️ Tech Stack

- Language: Java  
- Concepts: Data Structures, Multithreading, System Design  
- Tools: IntelliJ / VS Code  

---

## 📷 Screenshots

### Dashboard
![Dashboard](./assets/dashboard.png)

### Flow Diagram
![Flow](./assets/flow.png)

### Core Logic
![Code](./assets/code.png)

---

## 🚀 How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/PScodes270/Distributed_Cache_Load_Optimizer
