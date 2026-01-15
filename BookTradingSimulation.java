import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * BookTradingSimulation - Main class to run the book trading simulation
 * Coordinates buyer and seller agents
 */
public class BookTradingSimulation {
    private List<SellerAgent> sellers;
    private List<BuyerAgent> buyers;

    /**
     * Constructor
     */
    public BookTradingSimulation() {
        sellers = new ArrayList<>();
        buyers = new ArrayList<>();
    }

    /**
     * Create and register a seller agent
     * @param name The seller's name
     * @return The created seller agent
     */
    public SellerAgent createSeller(String name) {
        SellerAgent seller = new SellerAgent(name);
        sellers.add(seller);
        return seller;
    }

    /**
     * Create and register a buyer agent
     * @param name The buyer's name
     * @param targetBook The book to buy
     * @param requestInterval Time between requests in milliseconds
     * @return The created buyer agent
     */
    public BuyerAgent createBuyer(String name, String targetBook, long requestInterval) {
        BuyerAgent buyer = new BuyerAgent(name, targetBook, requestInterval);

        // Give buyer the names of all sellers (not direct references)
        List<String> sellerNames = new ArrayList<>();
        for (SellerAgent seller : sellers) {
            sellerNames.add(seller.getAgentName());
        }
        buyer.setSellerAgentNames(sellerNames);

        buyers.add(buyer);
        return buyer;
    }

    /**
     * Start all agents
     */
    public void startAgents() {
        System.out.println("\n=== Starting Book Trading Simulation ===\n");

        // Start all seller agents
        for (SellerAgent seller : sellers) {
            seller.startAgent();
        }

        // Give sellers time to initialize
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Start all buyer agents
        for (BuyerAgent buyer : buyers) {
            buyer.startAgent();
        }
    }

    /**
     * Stop all agents
     */
    public void stopAgents() {
        System.out.println("\n=== Stopping all agents ===\n");

        for (BuyerAgent buyer : buyers) {
            buyer.stopAgent();
        }

        for (SellerAgent seller : sellers) {
            seller.stopAgent();
        }
    }

    /**
     * Wait for all buyer agents to complete their purchases or timeout
     * @param timeoutSeconds Maximum time to wait in seconds
     */
    public void waitForCompletion(int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeout) {
            boolean allComplete = true;

            for (BuyerAgent buyer : buyers) {
                if (!buyer.isPurchaseComplete() && buyer.isAlive()) {
                    allComplete = false;
                    break;
                }
            }

            if (allComplete) {
                System.out.println("\n=== All buyer agents completed ===\n");
                return;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("\n=== Timeout reached ===\n");
    }

    /**
     * Main method - Entry point of the simulation
     *
     * Usage:
     *   java BookTradingSimulation
     *   java BookTradingSimulation [book1] [book2] [intervalMs]
     *
     * Example:
     *   java BookTradingSimulation "The-Kite-Runner" "Life-of-Pi" 10000
     */
    public static void main(String[] args) {
        BookTradingSimulation simulation = new BookTradingSimulation();

        // Create seller agents (they auto-register via Agent constructor)
        SellerAgent seller1 = simulation.createSeller("Seller1");
        SellerAgent seller2 = simulation.createSeller("Seller2");

        // Start agents (this will show the seller GUIs)
        simulation.startAgents();

        System.out.println("\n*** Seller Agent GUIs are now displayed ***");
        System.out.println("*** Please add books to the sellers' catalogues ***");
        System.out.println("\nSuggested book titles (copy these exactly):");
        System.out.println("  - The-Kite-Runner");
        System.out.println("  - A-Thousand-Splendid-Suns");
        System.out.println("  - Life-of-Pi");
        System.out.println("  - The-Alchemist");
        System.out.println("\n*** Press Enter when ready to specify buyer targets ***\n");

        // Wait for user to add books via GUI
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        long requestInterval = 10000; // 10 seconds

        // Optional: request interval from args
        if (args.length > 2) {
            try {
                requestInterval = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid request interval, using default: 10000ms");
            }
        }

        // Main loop - allows multiple transaction runs
        boolean continueRunning = true;
        while (continueRunning) {
            // Get buyer targets from user input or command line
            String targetBook1 = "The-Kite-Runner";
            String targetBook2 = "Life-of-Pi";

            // Check if command line arguments provided (only first time)
            if (args.length > 0) {
                targetBook1 = args[0];
                targetBook2 = args.length > 1 ? args[1] : "Life-of-Pi";
                System.out.println("Using command-line arguments:");
                System.out.println("  Buyer1 target: " + targetBook1);
                System.out.println("  Buyer2 target: " + targetBook2);
                args = new String[0]; // Clear args so next iteration is interactive
            } else {
                // Interactive input
                System.out.print("Enter target book for Buyer1 [press Enter for default]: ");
                String input1 = scanner.nextLine().trim();
                if (!input1.isEmpty()) {
                    targetBook1 = input1;
                }

                System.out.print("Enter target book for Buyer2 [press Enter for default]: ");
                String input2 = scanner.nextLine().trim();
                if (!input2.isEmpty()) {
                    targetBook2 = input2;
                }

                System.out.println("\nBuyer targets set:");
                System.out.println("  Buyer1 will search for: " + targetBook1);
                System.out.println("  Buyer2 will search for: " + targetBook2);
                System.out.println("\nNote: Titles are case-sensitive and must match exactly!");
            }

            // Create buyer agents (they auto-register via Agent constructor)
            System.out.println("\n=== Creating Buyer Agents ===\n");
            BuyerAgent buyer1 = simulation.createBuyer("Buyer1", targetBook1, requestInterval);
            BuyerAgent buyer2 = simulation.createBuyer("Buyer2", targetBook2, requestInterval);

            // Start buyer agents
            buyer1.startAgent();
            buyer2.startAgent();

            // Wait for completion or timeout (60 seconds)
            simulation.waitForCompletion(60);

            // Give extra time for any late messages to be processed
            try {
                Thread.sleep(3000); // 3 seconds buffer for message delivery
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Stop buyer agents
            buyer1.stopAgent();
            buyer2.stopAgent();

            // Remove buyers from list
            simulation.buyers.clear();

            // Ask user if they want to continue
            System.out.println("\n*** Transaction complete ***");
            System.out.print("Run another transaction? (y/n): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (!choice.equals("y") && !choice.equals("yes")) {
                continueRunning = false;
            } else {
                System.out.println("\n*** Starting new transaction cycle ***");
                System.out.println("*** Books remain in seller catalogues (unless sold) ***\n");
            }
        }

        // Stop all agents (sellers)
        simulation.stopAgents();

        System.out.println("\n=== Simulation Complete ===\n");
        scanner.close();
        System.exit(0);
    }
}
