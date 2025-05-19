# Event-Driven Architecture

A reference implementation of Event-Driven Architecture (EDA) using modern Java features (Java 17+). This implementation showcases the three core components of EDA:

1. **Event Producers** - Services that generate events when something happens
2. **Event Bus/Broker** - Central mechanism for distributing events
3. **Event Consumers** - Services that listen for and process events

The implementation takes advantage of modern Java features like:

- Records for immutable event objects
- Functional interfaces for event handlers
- Stream API for processing collections
- Text blocks for formatted strings
- The var keyword for local type inference

Key components in the reference implementation:

- **Event and EventType** - Define the structure of events and common event types
- **EventBus** - Acts as the central message broker with publish/subscribe capabilities
- **EventProducer** - Interface and implementation for services that generate events
- **EventConsumer** - Interface and implementation for services that process events
- **Application example** - Shows how these components work together

I've also included some advanced patterns that are often used with EDA:

- **Event Sourcing** - Using events as the source of truth
- **CQRS** - Command-Query Responsibility Segregation pattern
---
Event-Driven Architecture reference implementation to include comprehensive support for the Saga pattern, along with resilience and monitoring capabilities essential for production-ready event-driven systems. Here's what I've added:
## 1.Saga Pattern Implementation
Two types of Saga pattern implementations:

**Choreography-based Saga**:
 * Services communicate through events without a central coordinator
 * Each service performs its local transaction and publishes events that trigger the next steps
 * Great for simpler workflows with fewer steps
* Implemented with OrderService, PaymentService, and InventoryService that react to each other's events

**Orchestration-based Saga**:
* Uses a central coordinator (OrderSagaOrchestrator) that manages the entire workflow
* The orchestrator directs all participating services and handles the compensation logic
* Better for complex workflows with many steps
* Includes explicit state tracking and compensation handling

## 2. Resilience Patterns

Critical resilience patterns that work hand-in-hand with a Saga pattern:

**Circuit Breaker Pattern**:
  * Prevents cascading failures when dependent services are failing
  * Tracks service health and temporarily blocks requests when failures exceed a threshold
  * Implements states: CLOSED (normal operation), OPEN (failing), and HALF-OPEN (recovery testing)

**Retry Pattern:**
* Automatically retries operations that fail due to transient issues
* Implements exponential backoff to avoid overwhelming recovering services
* Works with a Circuit Breaker pattern for comprehensive resilience

**Timeout Pattern:**
* Prevents operations from hanging indefinitely
* Enforces maximum execution time for service calls

## 3.Monitoring and Observability
Monitoring capabilities essential for debugging distributed transactions:
   
**Distributed Tracing:**
* Follows transactions across multiple services and events
* Tracks parent-child relationships between operations
* Records timing and custom events during execution

**Health Monitoring:**
* Reports the health status of services in the system
* Publishes health events when service status changes
* Monitors services periodically

## 4.Integration Example
The ResilientSagaExample demonstrates how to use all these patterns together in a real-world scenario:

* Creates a payment service with circuit breaker, retry, and timeout patterns
* Adds distributed tracing to track the transaction across services
* Includes health monitoring to detect service degradation
* Shows how to handle failures and compensating transactions

This implementation now provides a comprehensive reference to implementing reliable, resilient, and observable event-driven architectures with the Saga pattern in modern Java applications.