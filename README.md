# Book Trading Multi-Agent System

A Java implementation of autonomous agents that negotiate book purchases using the Contract-Net Protocol.

## What is this?

This project simulates a simple book marketplace where buyer and seller agents communicate through messages to complete transactions. Sellers maintain catalogues through a GUI, and buyers automatically find the best deals.

The system uses FIPA-style ACL messages and implements concurrent agent operations within a single Java process.

## Requirements

- JDK 17 or higher
- No external libraries needed

## Running the simulation

**Windows:**
```
run-gui.bat
```

**Other platforms:**
```
javac *.java
java BookTradingSimulation
```

## How it works

1. Two seller GUIs open where you can add books with prices
2. After adding books, press Enter in the console
3. Enter target books for the buyers (or use defaults)
4. Buyers send requests to all sellers, compare prices, and purchase from the cheapest one
5. The GUI updates to show which books have been sold

**Important:** Book titles must use hyphens instead of spaces (e.g., `The-Kite-Runner`, not `The Kite Runner`)

## Example books to try

- The-Kite-Runner
- Life-of-Pi
- The-Alchemist
- A-Thousand-Splendid-Suns

## Architecture

The system has these main components:

- **Agent.java** - Base class with message queue and threading
- **BuyerAgent.java** - Autonomous buyer that compares prices
- **SellerAgent.java** - Seller with two concurrent server behaviors
- **SellerGui.java** - Swing interface for managing catalogues
- **ACLMessage.java** - Message structure based on FIPA ACL
- **AgentRegistry.java** - Registry pattern for agent lookup
- **BookTradingSimulation.java** - Main entry point

## Protocol

The negotiation follows these steps:

1. Buyer sends CFP (Call for Proposal) to all sellers
2. Sellers respond with PROPOSE (if book available) or REFUSE
3. Buyer selects the cheapest proposal and sends ACCEPT_PROPOSAL
4. Seller confirms with INFORM and updates catalogue
5. GUI shows the book as sold

## Technical notes

- Uses ConcurrentHashMap for thread safety
- Message filtering prevents agents from reading each other's messages
- Timeout handling (10 seconds for proposals, 15 for purchases)
- Retry logic if purchase fails
- Real-time GUI updates using SwingUtilities

## Testing scenarios

Try adding the same book to both sellers with different prices - the buyer should always choose the cheaper one. You can also test what happens when only one seller has the book, or when both buyers want the same limited stock.

## Author

Syed Muhammad Taha Najam
