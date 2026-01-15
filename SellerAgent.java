import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SellerAgent - Agent that sells books
 * Continuously waits for requests from buyer agents and serves them
 */
public class SellerAgent extends Agent {
    private Map<String, Integer> catalogue; // Book catalogue: title -> price (thread-safe)
    private Map<String, Integer> soldBooks; // Track sold books with their prices (thread-safe)
    private SellerGui gui;
    private boolean showGui; // Flag to control GUI display

    /**
     * Constructor - creates seller with GUI enabled
     * @param name The seller agent's name
     */
    public SellerAgent(String name) {
        this(name, true); // Default: show GUI
    }

    /**
     * Constructor with GUI control
     * @param name The seller agent's name
     * @param showGui Whether to display the GUI
     */
    public SellerAgent(String name, boolean showGui) {
        super(name);
        this.showGui = showGui;
        // Use ConcurrentHashMap for thread safety (GUI thread + 2 server threads access this)
        this.catalogue = new ConcurrentHashMap<>();
        this.soldBooks = new ConcurrentHashMap<>(); // Track sold books with prices
    }

    /**
     * Setup method - initialize the seller agent
     */
    @Override
    protected void setup() {
        log("Seller agent is ready.");

        // Create and show the GUI only if enabled
        if (showGui) {
            gui = new SellerGui(this);
            gui.setVisible(true);
        }

        // Start the request server behaviours
        new Thread(new OfferRequestsServer(), name + "-OfferServer").start();
        new Thread(new PurchaseOrdersServer(), name + "-PurchaseServer").start();
    }

    /**
     * Takedown method - cleanup when agent terminates
     */
    @Override
    protected void takeDown() {
        if (gui != null) {
            gui.dispose();
        }
        log("Seller agent terminating.");
    }

    /**
     * Update the catalogue with a new book
     * Warns if book already exists (will update price)
     * @param title The book title
     * @param price The book price
     * @return 1 if new book added, 0 if price updated, -1 if book was sold (cannot re-add)
     */
    public int updateCatalogue(String title, int price) {
        // Check if book was already sold
        if (soldBooks.containsKey(title)) {
            log("ERROR: Cannot add '" + title + "' - this book was already sold");
            return -1;
        }

        Integer oldPrice = catalogue.put(title, price);
        int result = (oldPrice == null) ? 1 : 0;

        if (result == 0) {
            log("WARNING: Updated price for '" + title + "' from " + oldPrice + " to " + price);
        } else {
            log("Added to catalogue: " + title + " at price " + price);
        }

        // Notify GUI to refresh if it exists
        if (gui != null) {
            gui.refreshCatalogue();
        }

        return result;
    }

    /**
     * Get all books (available and sold)
     * @return Map of all books with their prices
     */
    public Map<String, Integer> getAllBooks() {
        Map<String, Integer> allBooks = new ConcurrentHashMap<>();
        // Add available books
        allBooks.putAll(catalogue);
        // Add sold books with their original prices
        allBooks.putAll(soldBooks);
        return allBooks;
    }

    /**
     * Check if a book has been sold
     * @param title Book title
     * @return true if book was sold
     */
    public boolean isBookSold(String title) {
        return soldBooks.containsKey(title);
    }

    /**
     * Get the price of a book
     * @param title Book title
     * @return Price from catalogue or soldBooks, or null if not exists
     */
    public Integer getBookPrice(String title) {
        Integer price = catalogue.get(title);
        if (price == null) {
            price = soldBooks.get(title);
        }
        return price;
    }

    /**
     * Remove a book from the catalogue
     * @param title The book title to remove
     */
    public void removeFromCatalogue(String title) {
        Integer removed = catalogue.remove(title);
        if (removed != null) {
            log("Removed from catalogue: " + title);
        }
    }

    /**
     * Inner class - Handles requests for offers (CFP messages)
     * Uses receivePerformative() to filter only CFP messages
     */
    private class OfferRequestsServer implements Runnable {
        @Override
        public void run() {
            log("OfferRequestsServer started");

            while (running) {
                // Use receivePerformative to get only CFP messages (avoids competition with PurchaseOrdersServer)
                ACLMessage msg = receivePerformative(ACLMessage.CFP, 1000);

                if (msg != null) {
                    // CFP message received - check if book is in catalogue
                    String requestedTitle = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    if (catalogue.containsKey(requestedTitle)) {
                        // Book is available - send proposal with price
                        int price = catalogue.get(requestedTitle);
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(String.valueOf(price));
                        log("Book '" + requestedTitle + "' available. Proposing price: " + price);
                    } else {
                        // Book not available - refuse
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("not-available");
                        log("Book '" + requestedTitle + "' not available. Refusing.");
                    }

                    // Send reply using proper send() method (looks up buyer in registry)
                    send(reply);
                }
            }

            log("OfferRequestsServer stopped");
        }
    }

    /**
     * Inner class - Handles purchase orders (ACCEPT_PROPOSAL messages)
     * Uses receivePerformative() to filter only ACCEPT_PROPOSAL messages
     */
    private class PurchaseOrdersServer implements Runnable {
        @Override
        public void run() {
            log("PurchaseOrdersServer started");

            while (running) {
                // Use receivePerformative to get only ACCEPT_PROPOSAL messages
                ACLMessage msg = receivePerformative(ACLMessage.ACCEPT_PROPOSAL, 1000);

                if (msg != null) {
                    // Purchase order received
                    String requestedTitle = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    // Double-check availability (another buyer might have purchased it)
                    if (catalogue.containsKey(requestedTitle)) {
                        // Process the purchase order
                        Integer price = catalogue.remove(requestedTitle); // Remove from catalogue
                        soldBooks.put(requestedTitle, price); // Track as sold with price

                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("purchase-successful");
                        log("Purchase order for '" + requestedTitle + "' served. Price: " + price);

                        // Notify GUI to refresh
                        if (gui != null) {
                            gui.refreshCatalogue();
                        }
                    } else {
                        // Book no longer available (race condition - another buyer got it)
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("no-longer-available");
                        log("Book '" + requestedTitle + "' no longer available (already sold).");
                    }

                    // Send reply using proper send() method
                    send(reply);
                }
            }

            log("PurchaseOrdersServer stopped");
        }
    }
}
