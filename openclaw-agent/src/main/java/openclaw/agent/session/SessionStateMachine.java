package openclaw.agent.session;

import openclaw.agent.token.TokenCounterService;
import openclaw.memory.store.SQLiteMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session State Machine
 * 
 * <p>Manages agent session lifecycle with states:</p>
 * <ul>
 *   <li>PENDING - Session created, not yet active</li>
 *   <li>ACTIVE - Session in progress</li>
 *   <li>PAUSED - Session temporarily paused</li>
 *   <li>COMPLETED - Session finished successfully</li>
 *   <li>ARCHIVED - Session archived</li>
 *   <li>ERROR - Session encountered error</li>
 * </ul>
 */
@Service
public class SessionStateMachine {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionStateMachine.class);
    
    private final Map<String, SessionState> sessionStates;
    private final ApplicationEventPublisher eventPublisher;
    private final SQLiteMemoryStore memoryStore;
    private final TokenCounterService tokenCounter;
    
    public SessionStateMachine(
            ApplicationEventPublisher eventPublisher,
            SQLiteMemoryStore memoryStore,
            TokenCounterService tokenCounter) {
        this.sessionStates = new ConcurrentHashMap<>();
        this.eventPublisher = eventPublisher;
        this.memoryStore = memoryStore;
        this.tokenCounter = tokenCounter;
    }
    
    /**
     * Create new session in PENDING state
     */
    public CompletableFuture<SessionContext> createSession(String sessionKey, String model) {
        return CompletableFuture.supplyAsync(() -> {
            SessionContext context = new SessionContext(
                sessionKey,
                model,
                SessionState.PENDING,
                Instant.now(),
                null,
                0,
                0,
                Map.of()
            );
            
            sessionStates.put(sessionKey, SessionState.PENDING);
            
            // Publish event
            eventPublisher.publishEvent(new SessionStateChangeEvent(
                this, sessionKey, null, SessionState.PENDING, context
            ));
            
            logger.info("Session created: {} in state {}", sessionKey, SessionState.PENDING);
            return context;
        });
    }
    
    /**
     * Transition session to any state
     */
    public CompletableFuture<SessionContext> transition(String sessionKey, SessionState newState) {
        return transitionState(sessionKey, newState);
    }

    /**
     * Transition session to ACTIVE state
     */
    public CompletableFuture<SessionContext> activateSession(String sessionKey) {
        return transitionState(sessionKey, SessionState.ACTIVE);
    }
    
    /**
     * Transition session to PAUSED state
     */
    public CompletableFuture<SessionContext> pauseSession(String sessionKey) {
        return transitionState(sessionKey, SessionState.PAUSED);
    }
    
    /**
     * Transition session to COMPLETED state
     */
    public CompletableFuture<SessionContext> completeSession(String sessionKey) {
        return transitionState(sessionKey, SessionState.COMPLETED);
    }
    
    /**
     * Transition session to ERROR state
     */
    public CompletableFuture<SessionContext> errorSession(String sessionKey, String errorMessage) {
        return CompletableFuture.supplyAsync(() -> {
            SessionState currentState = sessionStates.get(sessionKey);
            if (currentState == null) {
                throw new IllegalStateException("Session not found: " + sessionKey);
            }
            
            SessionContext context = new SessionContext(
                sessionKey,
                "unknown", // Should get from existing context
                SessionState.ERROR,
                Instant.now(),
                errorMessage,
                0,
                0,
                Map.of()
            );
            
            sessionStates.put(sessionKey, SessionState.ERROR);
            
            eventPublisher.publishEvent(new SessionStateChangeEvent(
                this, sessionKey, currentState, SessionState.ERROR, context
            ));
            
            logger.error("Session {} entered ERROR state: {}", sessionKey, errorMessage);
            return context;
        });
    }
    
    /**
     * Archive completed session
     */
    public CompletableFuture<Void> archiveSession(String sessionKey) {
        return CompletableFuture.runAsync(() -> {
            SessionState currentState = sessionStates.get(sessionKey);
            
            if (currentState != SessionState.COMPLETED && currentState != SessionState.ERROR) {
                throw new IllegalStateException(
                    "Cannot archive session in state: " + currentState
                );
            }
            
            sessionStates.put(sessionKey, SessionState.ARCHIVED);
            
            eventPublisher.publishEvent(new SessionStateChangeEvent(
                this, sessionKey, currentState, SessionState.ARCHIVED, null
            ));
            
            logger.info("Session {} archived", sessionKey);
        });
    }
    
    /**
     * Update session metrics
     */
    public CompletableFuture<Void> updateSessionMetrics(
            String sessionKey, 
            int inputTokens, 
            int outputTokens) {
        
        return CompletableFuture.runAsync(() -> {
            logger.debug("Updating metrics for session {}: input={}, output={}",
                sessionKey, inputTokens, outputTokens);
            
            // In real implementation, update in database
            // For now, just log
        });
    }
    
    /**
     * Get session state
     */
    public SessionState getSessionState(String sessionKey) {
        return sessionStates.get(sessionKey);
    }

    /**
     * Get current state (alias for getSessionState)
     */
    public SessionState getCurrentState(String sessionKey) {
        return getSessionState(sessionKey);
    }
    
    /**
     * Check if session is active
     */
    public boolean isActive(String sessionKey) {
        return sessionStates.get(sessionKey) == SessionState.ACTIVE;
    }
    
    /**
     * Check if session can be resumed
     */
    public boolean canResume(String sessionKey) {
        SessionState state = sessionStates.get(sessionKey);
        return state == SessionState.PAUSED || state == SessionState.PENDING;
    }
    
    /**
     * State transition logic
     */
    private CompletableFuture<SessionContext> transitionState(
            String sessionKey, 
            SessionState newState) {
        
        return CompletableFuture.supplyAsync(() -> {
            SessionState currentState = sessionStates.get(sessionKey);
            
            if (currentState == null) {
                throw new IllegalStateException("Session not found: " + sessionKey);
            }
            
            // Validate transition
            if (!isValidTransition(currentState, newState)) {
                throw new IllegalStateException(
                    String.format("Invalid state transition: %s -> %s", currentState, newState)
                );
            }
            
            // Update state
            sessionStates.put(sessionKey, newState);
            
            // Create context
            SessionContext context = new SessionContext(
                sessionKey,
                "gpt-4", // Should be retrieved from session storage
                newState,
                Instant.now(),
                null,
                0,
                0,
                Map.of()
            );
            
            // Publish event
            eventPublisher.publishEvent(new SessionStateChangeEvent(
                this, sessionKey, currentState, newState, context
            ));
            
            logger.info("Session {} transitioned: {} -> {}", 
                sessionKey, currentState, newState);
            
            return context;
        });
    }
    
    /**
     * Validate state transition
     */
    private boolean isValidTransition(SessionState from, SessionState to) {
        return switch (from) {
            case PENDING -> to == SessionState.ACTIVE || to == SessionState.ERROR;
            case ACTIVE -> to == SessionState.PAUSED || to == SessionState.COMPLETED || to == SessionState.ERROR;
            case PAUSED -> to == SessionState.ACTIVE || to == SessionState.COMPLETED || to == SessionState.ERROR;
            case COMPLETED -> to == SessionState.ARCHIVED;
            case ERROR -> to == SessionState.ARCHIVED;
            case ARCHIVED -> false; // Terminal state
        };
    }
    
    /**
     * Session states
     */
    public enum SessionState {
        PENDING,    // Created, not yet active
        ACTIVE,     // In progress
        PAUSED,     // Temporarily paused
        COMPLETED,  // Finished successfully
        ERROR,      // Encountered error
        ARCHIVED    // Archived
    }
    
    /**
     * Session context
     */
    public record SessionContext(
        String sessionKey,
        String model,
        SessionState state,
        Instant lastActivity,
        String errorMessage,
        int totalInputTokens,
        int totalOutputTokens,
        Map<String, Object> metadata
    ) {}
    
    /**
     * Session state change event
     */
    public static class SessionStateChangeEvent extends org.springframework.context.ApplicationEvent {
        private final String sessionKey;
        private final SessionState oldState;
        private final SessionState newState;
        private final SessionContext context;
        
        public SessionStateChangeEvent(Object source, String sessionKey, 
                                       SessionState oldState, SessionState newState,
                                       SessionContext context) {
            super(source);
            this.sessionKey = sessionKey;
            this.oldState = oldState;
            this.newState = newState;
            this.context = context;
        }
        
        public String getSessionKey() { return sessionKey; }
        public SessionState getOldState() { return oldState; }
        public SessionState getNewState() { return newState; }
        public SessionContext getContext() { return context; }
    }
}
