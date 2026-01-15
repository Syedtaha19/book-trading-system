/**
 * ACLMessage - Agent Communication Language Message
 * Represents messages exchanged between agents following FIPA ACL standard
 */
public class ACLMessage {
    // Performatives (message types)
    public static final int CFP = 1;              // Call For Proposal
    public static final int PROPOSE = 2;          // Proposal/Offer
    public static final int REFUSE = 3;           // Refusal
    public static final int ACCEPT_PROPOSAL = 4;  // Accept Proposal
    public static final int INFORM = 5;           // Inform

    private int performative;
    private String sender;
    private String receiver;
    private String content;
    private String conversationId;
    private String replyWith;
    private String inReplyTo;

    /**
     * Constructor
     * @param performative The type of message (CFP, PROPOSE, etc.)
     */
    public ACLMessage(int performative) {
        this.performative = performative;
    }

    // Getters and Setters
    public int getPerformative() {
        return performative;
    }

    public void setPerformative(int performative) {
        this.performative = performative;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getReplyWith() {
        return replyWith;
    }

    public void setReplyWith(String replyWith) {
        this.replyWith = replyWith;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    /**
     * Create a reply message with proper conversation control fields set
     * @return A new ACLMessage prepared as a reply to this message
     */
    public ACLMessage createReply() {
        ACLMessage reply = new ACLMessage(0); // Performative to be set by sender
        reply.setReceiver(this.sender);
        reply.setSender(this.receiver);
        reply.setConversationId(this.conversationId);
        reply.setInReplyTo(this.replyWith);
        return reply;
    }

    @Override
    public String toString() {
        return "ACLMessage{" +
                "performative=" + getPerformativeName(performative) +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", conversationId='" + conversationId + '\'' +
                '}';
    }

    public String getPerformativeName(int performative) {
        switch (performative) {
            case CFP: return "CFP";
            case PROPOSE: return "PROPOSE";
            case REFUSE: return "REFUSE";
            case ACCEPT_PROPOSAL: return "ACCEPT_PROPOSAL";
            case INFORM: return "INFORM";
            default: return "UNKNOWN";
        }
    }

    public String getPerformativeName() {
        return getPerformativeName(this.performative);
    }
}
