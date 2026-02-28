# 🛒 Bidding Engine (Core Storefront)

*Note: This is a microservice within the larger **[Real-Time Bidding & Fraud Detection Platform](https://github.com/walker-systems/auction-system)**. For the full system architecture and Docker orchestration, please visit the main repository.*

This is the user-facing storefront and the core transaction engine of the auction platform. It handles incoming bids, manages the active countdown timers, and instantly broadcasts updates to all connected users.

## 🧠 Component Context

Unlike traditional web apps that force you to refresh the page to see new bids, this engine uses **Server-Sent Events (SSE)**. 

When a user places a bid, the Bidding Engine saves it and broadcasts it to Redis. It then immediately pushes that new price out to every open browser tab in milliseconds, creating a truly live auction experience. 

**Key Technologies:** Java 25, Spring Boot WebFlux, Server-Sent Events (SSE), Vanilla JavaScript, TailwindCSS.

---

## 🚀 How to Run (Standalone)

*To run the full platform including the AI Sentinel and Database, use the `docker-compose.yml` in the [main repository](https://github.com/walker-systems/auction-system).*

If you want to run just this service locally (requires a running Redis instance on port 6379):

```bash
# 1. Navigate to this directory
cd bidding-engine

# 2. Start the application
./mvnw spring-boot:run
```
The storefront will be available at: **`http://localhost:8080`**

---

## 📬 Let's Connect

**Justin Walker**
* 📧 **Email:** [justinwalker.contact@gmail.com](mailto:justinwalker.contact@gmail.com)
* 💼 **LinkedIn:** [Justin Walker](https://www.linkedin.com/in/justin-walker-0403923b1/)
* 🌐 **Portfolio:** [justin-castillo.github.io](https://justin-castillo.github.io/)
