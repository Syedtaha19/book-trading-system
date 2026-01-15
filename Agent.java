import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Agent - Base class for all agents
 * Provides basic agent functionality including message queue and lifecycle methods
 */
public abstract class Agent extends Thread {
    protected String name;
    protected BlockingQueue<ACLMessage> messageQueue;
    protected boolean running;

    /**
     * Constructor
     * @param name The agent's name
     */
    public Agent(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent name cannot be null or empty");
        }
        this.name = name;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.running = false;

        // Auto-register with the agent registry
        AgentRegistry.register(this);
    }

    /**
     * Get the agent's name
     * @return The agent name
     */
    public String getAgentName() {
        return name;
    }

    /**
     * Setup method - called when agent starts
     * Override this to initialize agent-specific resources
     */
    protected abstract void setup();

    /**
     * Takedown method - called when agent terminates
     * Override this to perform cleanup operations
     */
    protected abstract void takeDown();

    /**
     * Send a message to another agent
     * @param message The message to send
     * @param recipient The recipient agent
     */
    public void send(ACLMessage message, Agent recipient) {
        message.setSender(this.name);
        message.setReceiver(recipient.getAgentName());
        recipient.postMessage(message);
        System.out.println("[" + name + "] Sent " + message.getPerformativeName() +
                         " to " + recipient.getAgentName() + ": " + message.getContent());
    }

    /**
     * Send a message to another agent by name (looks up in registry)
     * @param message The message to send
     */
    public void send(ACLMessage message) {
        String recipientName = message.getReceiver();
        if (recipientName == null || recipientName.isEmpty()) {
            System.err.println("[" + name + "] ERROR: Cannot send message with no receiver");
            return;
        }

        Agent recipient = AgentRegistry.findAgent(recipientName);
        if (recipient == null) {
            System.err.println("[" + name + "] ERROR: Recipient not found: " + recipientName);
            return;
        }

        message.setSender(this.name);
        recipient.postMessage(message);
        System.out.println("[" + name + "] Sent " + message.getPerformativeName() +
                         " to " + recipientName + ": " + message.getContent());
    }

    /**
     * Post a message to this agent's message queue
     * @param message The message to post
     */
    public void postMessage(ACLMessage message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receive a message from the queue (blocking)
     * @return The next message in the queue
     */
    protected ACLMessage receive() {
        try {
            return messageQueue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    /**
     * Receive a message with timeout
     * @param timeout Timeout in milliseconds
     * @return The next message or null if timeout
     */
    protected ACLMessage receive(long timeout) {
        try {
            return messageQueue.poll(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    /**
     * Receive a message with specific performative (with timeout)
     * This method checks messages in the queue and returns the first one matching the performative
     * Non-matching messages are temporarily stored and re-queued
     *
     * @param performative The performative to match (e.g., ACLMessage.CFP)
     * @param timeout Timeout in milliseconds
     * @return The first matching message or null if timeout
     */
    protected ACLMessage receivePerformative(int performative, long timeout) {
        long deadline = System.currentTimeMillis() + timeout;
        java.util.List<ACLMessage> tempStorage = new java.util.ArrayList<>();

        try {
            while (System.currentTimeMillis() < deadline) {
                ACLMessage msg = messageQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);

                if (msg != null) {
                    if (msg.getPerformative() == performative) {
                        // Found matching message - re-queue non-matching ones
                        for (ACLMessage temp : tempStorage) {
                            messageQueue.put(temp);
                        }
                        return msg;
                    } else {
                        // Store non-matching message temporarily
                        tempStorage.add(msg);
                    }
                }
            }

            // Timeout - re-queue all messages
            for (ACLMessage temp : tempStorage) {
                messageQueue.put(temp);
            }
            return null;

        } catch (InterruptedException e) {
            // Re-queue all messages before returning
            for (ACLMessage temp : tempStorage) {
                try {
                    messageQueue.put(temp);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }

    /**
     * Log message with agent name prefix
     * @param message The message to log
     */
    protected void log(String message) {
        System.out.println("[" + name + "] " + message);
    }

    /**
     * Start the agent
     */
    public void startAgent() {
        running = true;
        start();
    }

    /**
     * Stop the agent
     */
    public void stopAgent() {
        running = false;
        AgentRegistry.deregister(this);
        takeDown();
        interrupt();
    }

    /**
     * Main agent execution loop
     */
    @Override
    public void run() {
        setup();

        while (running) {
            try {
                Thread.sleep(100); // Small sleep to prevent busy waiting
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
