import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AgentRegistry - Central registry for all agents in the simulation
 * Enables agents to find each other by name for message routing
 * Thread-safe for concurrent access
 */
public class AgentRegistry {
    private static final Map<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * Register an agent in the registry
     * @param agent The agent to register
     */
    public static void register(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("Cannot register null agent");
        }
        agents.put(agent.getAgentName(), agent);
        System.out.println("[Registry] Registered agent: " + agent.getAgentName());
    }

    /**
     * Deregister an agent from the registry
     * @param agent The agent to deregister
     */
    public static void deregister(Agent agent) {
        if (agent != null) {
            agents.remove(agent.getAgentName());
            System.out.println("[Registry] Deregistered agent: " + agent.getAgentName());
        }
    }

    /**
     * Find an agent by name
     * @param name The agent's name
     * @return The agent, or null if not found
     */
    public static Agent findAgent(String name) {
        return agents.get(name);
    }

    /**
     * Check if an agent is registered
     * @param name The agent's name
     * @return true if agent is registered
     */
    public static boolean isRegistered(String name) {
        return agents.containsKey(name);
    }

    /**
     * Get count of registered agents
     * @return Number of registered agents
     */
    public static int getAgentCount() {
        return agents.size();
    }

    /**
     * Clear all agents (useful for testing)
     */
    public static void clear() {
        agents.clear();
        System.out.println("[Registry] Cleared all agents");
    }
}
