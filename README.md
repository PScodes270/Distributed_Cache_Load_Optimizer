# Distributed Cache Load Optimizer

A Java-based simulation of a load balancing system that distributes incoming requests across multiple servers using a greedy selection strategy, priority queue scheduling, and multithreading.

## Overview

This project simulates how backend systems handle and distribute requests efficiently. It focuses on selecting the best server based on load and latency, managing incoming requests using a priority queue, handling concurrent execution using multithreading, and monitoring system performance such as throughput and server health.

## Features

- Greedy-based load balancing  
- Priority queue for request scheduling  
- Multithreaded request processing  
- Basic system monitoring  

## Project Structure

App/  
LoadBalancer/  
Queue/  
Model/  
Monitoring/  
Simulation/  
UI/  

## How It Works

1. Requests are generated continuously  
2. Requests are added to a priority queue  
3. Load balancer selects the best server based on current load and latency  
4. Requests are processed by servers  
5. Monitoring tracks system performance  

## How to Run

1. Clone the repository:
   git clone https://github.com/PScodes270/Distributed_Cache_Load_Optimizer  

2. Open the project in your IDE  

3. Run Main.java  

## Tech Stack

- Java  
- Multithreading  
- Data Structures (Priority Queue)  
