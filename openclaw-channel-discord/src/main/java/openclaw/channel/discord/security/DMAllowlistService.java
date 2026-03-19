package openclaw.channel.discord.security;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.Component;
import openclaw.channel.discord.DiscordConfigAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Discord DM allowlist service for strict component authorization.
 *
 * <p>Ported from original: 0f0cecd2e8 - enforce strict DM component allowlist auth</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.19
 */
@Service
public class DMAllowlistService {

    private static final Logger logger = LoggerFactory.getLogger(DMAllowlistService.class);
    
    private final DiscordConfigAdapter configAdapter;
    
    @Autowired
    public DMAllowlistService(DiscordConfigAdapter configAdapter) {
        this.configAdapter = configAdapter;
    }
    
    /**
     * Checks if a user is allowed to interact with DM components.
     *
     * @param user the Discord user
     * @param component the component being interacted with
     * @return true if the user is allowed
     */
    public boolean isAllowed(User user, Component component) {
        // Check if DM allowlist is enabled
        if (!isDMAllowlistEnabled()) {
            logger.debug("DM allowlist not enabled, allowing user: {}", user.getId());
            return true;
        }
        
        Set<String> allowedUsers = getDMAllowlist();
        boolean allowed = allowedUsers.contains(user.getId());
        
        if (!allowed) {
            logger.warn("User {} not in DM allowlist for component: {}", 
                    user.getId(), component.getId());
        }
        
        return allowed;
    }
    
    /**
     * Checks if a user ID is in the DM allowlist.
     *
     * @param userId the user ID
     * @return true if allowed
     */
    public boolean isUserAllowed(String userId) {
        if (!isDMAllowlistEnabled()) {
            return true;
        }
        
        return getDMAllowlist().contains(userId);
    }
    
    /**
     * Validates component interaction in DM context.
     *
     * @param userId the user ID
     * @param componentId the component ID
     * @throws SecurityException if the user is not allowed
     */
    public void validateComponentInteraction(String userId, String componentId) {
        if (!isUserAllowed(userId)) {
            throw new SecurityException(
                    String.format("User %s is not authorized to interact with component %s in DM", 
                            userId, componentId)
            );
        }
    }
    
    /**
     * Checks if DM allowlist is enabled.
     *
     * @return true if enabled
     */
    private boolean isDMAllowlistEnabled() {
        return configAdapter.isDmAllowlistEnabled();
    }
    
    /**
     * Gets the DM allowlist user IDs.
     *
     * @return set of allowed user IDs
     */
    private Set<String> getDMAllowlist() {
        return configAdapter.getDmAllowlist();
    }
}
