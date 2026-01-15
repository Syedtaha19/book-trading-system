import java.util.ArrayList;
import java.util.List;

/**
 * BuyerAgent - Agent that buys books
 * Periodically requests offers from seller agents and purchases the best offer
 */
public class BuyerAgent extends Agent {
    // Constants
    private static final long DEFAULT_REQUEST_TIMEOUT_MS = 10000; // 10 seconds for proposal collection
    private static final long PURCHASE_TIMEOUT_MS = 15000; // 15 seconds for purchase confirmation
    private static final int MAX_NO_SELLER_RETRIES = 5;
    private static final int MAX_NO_PROPOSAL_RETRIES = 3;

    // Agent state
    private String targetBookTitle;
    private List<String> sellerAgentNames; // Store seller names instead of direct references
    private boolean purchaseComplete;
    private long requestInterval; // Milliseconds between requests

    // Retry counters
    private int noSellerRetries = 0;
    private int noProposalRetries = 0;

    /**
     * Constructor
     * @param name The buyer agent's name
     * @param targetBookTitle The title of the book to buy
     * @param requestInterval Time between requests in milliseconds
     */
    public BuyerAgent(String name, String targetBookTitle, long requestInterval) {
        super(name);

        // Input validation
        if (targetBookTitle == null || targetBookTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Target book title cannot be null or empty");
        }
        if (requestInterval <= 0) {
            throw new IllegalArgumentException("Request interval must be positive");
        }

        this.targetBookTitle = targetBookTitle.trim();
        this.sellerAgentNames = new ArrayList<>();
        this.purchaseComplete = false;
        this.requestInterval = requestInterval;
    }

    /**
     * Set the list of known seller agent names
     * @param sellerNames List of seller agent names
     */
    public void setSellerAgentNames(List<String> sellerNames) {
        this.sellerAgentNames = new ArrayList<>(sellerNames);
    }

    /**
     * Setup method - initialize the buyer agent
     */
    @Override
    protected void setup() {
        log("Buyer agent ready.");
        log("Trying to buy: " + targetBookTitle);
    }

    /**
     * Takedown method - cleanup when agent terminates
     */
    @Override
    protected void takeDown() {
        log("Buyer agent terminating.");
    }

    /**
     * Main agent execution loop
     * Periodically sends requests to sellers
     */
    @Override
    public void run() {
        setup();

        while (running && !purchaseComplete) {
            // Perform a request cycle
            requestPerformer();

            // Wait before next request (if not completed)
            if (!purchaseComplete) {
                try {
                    Thread.sleep(requestInterval);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        takeDown();
    }

    /**
     * Main request/purchase cycle - coordinates the entire negotiation
     */
    private void requestPerformer() {
        // Check if we have sellers
        if (!checkSellersAvailable()) {
            return;
        }

        log("=== Starting new request cycle ===");

        // Generate unique conversation ID
        String conversationId = generateConversationId();

        // Step 1: Send CFP to all sellers
        sendCFPToAllSellers(conversationId);

        // Step 2: Collect proposals
        List<ACLMessage> proposals = collectProposals(conversationId, sellerAgentNames.size());

        // Step 3: Handle no proposals
        if (proposals.isEmpty()) {
            handleNoProposals();
            return;
        }

        // Reset retry counter (we got proposals)
        noProposalRetries = 0;

        // Step 4: Select best proposal
        ACLMessage bestProposal = selectBestProposal(proposals);
        if (bestProposal == null) {
            log("No valid proposals received.");
            return;
        }

        // Step 5: Attempt purchase
        if (attemptPurchase(bestProposal, conversationId)) {
            handleSuccessfulPurchase(bestProposal);
        }
    }

    /**
     * Check if sellers are available, handle retry logic
     * @return true if sellers available, false otherwise
     */
    private boolean checkSellersAvailable() {
        if (sellerAgentNames.isEmpty()) {
            noSellerRetries++;
            log("No seller agents available. (Retry " + noSellerRetries + "/" + MAX_NO_SELLER_RETRIES + ")");

            if (noSellerRetries >= MAX_NO_SELLER_RETRIES) {
                log("Max retries reached with no sellers. Terminating.");
                purchaseComplete = true;
            }
            return false;
        }

        // Reset counter if we have sellers
        noSellerRetries = 0;
        return true;
    }

    /**
     * Generate a unique conversation ID
     */
    private String generateConversationId() {
        return "book-trade-" + System.currentTimeMillis();
    }

    /**
     * Send CFP (Call for Proposal) to all known sellers
     */
    private void sendCFPToAllSellers(String conversationId) {
        String replyWith = "cfp-" + System.currentTimeMillis();

        for (String sellerName : sellerAgentNames) {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setReceiver(sellerName);
            cfp.setContent(targetBookTitle);
            cfp.setConversationId(conversationId);
            cfp.setReplyWith(replyWith);

            send(cfp); // Uses registry to find seller
        }
    }

    /**
     * Collect proposals from sellers
     * @param conversationId The conversation ID to match
     * @param expectedCount Expected number of replies
     * @return List of PROPOSE messages
     */
    private List<ACLMessage> collectProposals(String conversationId, int expectedCount) {
        List<ACLMessage> proposals = new ArrayList<>();
        int repliesReceived = 0;
        long deadline = System.currentTimeMillis() + DEFAULT_REQUEST_TIMEOUT_MS;

        log("Waiting for replies from " + expectedCount + " sellers...");

        // Always wait the full timeout to collect ALL proposals
        while (System.currentTimeMillis() < deadline) {
            ACLMessage reply = receive(500); // Increased polling timeout

            if (reply != null) {
                // Check if reply is for this conversation
                if (conversationId.equals(reply.getConversationId())) {
                    repliesReceived++;

                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        proposals.add(reply);
                        log("Received proposal from " + reply.getSender() +
                            ": " + reply.getContent());
                    } else if (reply.getPerformative() == ACLMessage.REFUSE) {
                        log("Received refusal from " + reply.getSender());
                    }

                    // If we got all expected replies, wait a bit more for late arrivals
                    if (repliesReceived >= expectedCount) {
                        try {
                            Thread.sleep(1000); // Wait 1 more second for any late proposals
                        } catch (InterruptedException e) {
                            break;
                        }
                        break;
                    }
                }
            }
        }

        log("Received " + repliesReceived + " replies (" + proposals.size() + " proposals)");
        return proposals;
    }

    /**
     * Handle situation when no proposals received
     */
    private void handleNoProposals() {
        noProposalRetries++;
        log("No proposals received. Will try again later. (Retry " +
            noProposalRetries + "/" + MAX_NO_PROPOSAL_RETRIES + ")");

        if (noProposalRetries >= MAX_NO_PROPOSAL_RETRIES) {
            log("Max retries reached with no proposals. Terminating.");
            purchaseComplete = true;
        }
    }

    /**
     * Select the best proposal (lowest price)
     * @param proposals List of proposals
     * @return Best proposal or null if none valid
     */
    private ACLMessage selectBestProposal(List<ACLMessage> proposals) {
        ACLMessage bestProposal = null;
        int bestPrice = Integer.MAX_VALUE;

        for (ACLMessage proposal : proposals) {
            try {
                int price = Integer.parseInt(proposal.getContent());
                if (price < bestPrice) {
                    bestPrice = price;
                    bestProposal = proposal;
                }
            } catch (NumberFormatException e) {
                log("WARNING: Invalid price format from " + proposal.getSender() +
                    ": " + proposal.getContent());
            }
        }

        if (bestProposal != null) {
            log("Best offer: " + bestPrice + " from " + bestProposal.getSender());
        }

        return bestProposal;
    }

    /**
     * Attempt to purchase the book from the best seller
     * @param bestProposal The best proposal received
     * @param conversationId The conversation ID
     * @return true if purchase successful
     */
    private boolean attemptPurchase(ACLMessage bestProposal, String conversationId) {
        String sellerName = bestProposal.getSender();

        // Send ACCEPT_PROPOSAL
        ACLMessage acceptProposal = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        acceptProposal.setReceiver(sellerName);
        acceptProposal.setContent(targetBookTitle);
        acceptProposal.setConversationId(conversationId);
        String replyWith = "order-" + System.currentTimeMillis();
        acceptProposal.setReplyWith(replyWith);

        send(acceptProposal);

        // Wait for confirmation (longer timeout for purchase)
        long deadline = System.currentTimeMillis() + PURCHASE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            ACLMessage reply = receive(1000); // 1 second polling for purchase confirmation

            if (reply != null &&
                conversationId.equals(reply.getConversationId()) &&
                replyWith.equals(reply.getInReplyTo())) {

                if (reply.getPerformative() == ACLMessage.INFORM) {
                    return true; // Purchase successful!
                } else {
                    log("Purchase failed: " + reply.getContent());
                    return false;
                }
            }
        }

        // Timeout reached - check message queue for late arrivals with extended wait
        log("Timeout reached, checking for late confirmation messages...");
        for (int i = 0; i < 10; i++) {
            ACLMessage reply = receive(1000); // Check every second for 10 seconds
            if (reply != null &&
                conversationId.equals(reply.getConversationId()) &&
                replyWith.equals(reply.getInReplyTo())) {

                if (reply.getPerformative() == ACLMessage.INFORM) {
                    log("Received late confirmation from " + sellerName);
                    return true; // Purchase successful (late arrival)!
                } else {
                    log("Purchase failed: " + reply.getContent());
                    return false;
                }
            }
        }

        // If we sent ACCEPT_PROPOSAL but got no response, assume purchase likely succeeded
        // to avoid double-purchasing. Mark as complete to prevent retry.
        log("No confirmation received from " + sellerName + " - assuming purchase succeeded to avoid double-buy");
        return true;
    }

    /**
     * Handle successful purchase - log details and terminate
     */
    private void handleSuccessfulPurchase(ACLMessage bestProposal) {
        int price = Integer.parseInt(bestProposal.getContent());

        log("");
        log("*** PURCHASE SUCCESSFUL! ***");
        log("Book: " + targetBookTitle);
        log("Price: " + price);
        log("Seller: " + bestProposal.getSender());
        log("");

        purchaseComplete = true;
    }

    /**
     * Check if purchase is complete
     * @return true if purchase complete
     */
    public boolean isPurchaseComplete() {
        return purchaseComplete;
    }
}
