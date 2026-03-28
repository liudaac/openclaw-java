package openclaw.tools.skill;

import openclaw.sdk.tool.AgentTool;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Skill manager for managing agent skills.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface SkillManager {

    /**
     * Initializes the skill manager.
     *
     * @param config the configuration
     * @return completion future
     */
    CompletableFuture<Void> initialize(SkillConfig config);

    /**
     * Loads a skill.
     *
     * @param skillId the skill ID
     * @return the loaded skill
     */
    CompletableFuture<Optional<Skill>> loadSkill(String skillId);

    /**
     * Unloads a skill.
     *
     * @param skillId the skill ID
     * @return completion future
     */
    CompletableFuture<Void> unloadSkill(String skillId);

    /**
     * Lists all loaded skills.
     *
     * @return list of skills
     */
    CompletableFuture<List<Skill>> listSkills();

    /**
     * Gets tools from a skill.
     *
     * @param skillId the skill ID
     * @return list of tools
     */
    CompletableFuture<List<AgentTool>> getTools(String skillId);

    /**
     * Installs a skill from a URL.
     *
     * @param url the skill URL
     * @return the installed skill
     */
    CompletableFuture<Skill> installSkill(String url);

    /**
     * Updates a skill.
     *
     * @param skillId the skill ID
     * @return the updated skill
     */
    CompletableFuture<Optional<Skill>> updateSkill(String skillId);

    /**
     * Skill configuration.
     *
     * @param skillsDir the skills directory
     * @param autoLoad whether to auto-load skills
     * @param allowRemote whether to allow remote skills
     */
    record SkillConfig(
            java.nio.file.Path skillsDir,
            boolean autoLoad,
            boolean allowRemote
    ) {
    }

    /**
     * Skill information.
     *
     * @param id the skill ID
     * @param name the name
     * @param version the version
     * @param description the description
     * @param author the author
     * @param tools list of tools
     * @param loaded whether loaded
     * @param source the skill source (e.g., "openclaw-bundled", "github", "local")
     */
    record Skill(
            String id,
            String name,
            String version,
            String description,
            String author,
            List<String> tools,
            boolean loaded,
            String source
    ) {
        /**
         * Creates a Skill with default source.
         */
        public Skill {
            if (source == null) {
                source = "unknown";
            }
        }
    }

    /**
     * Bundled skill sources.
     */
    Set<String> BUNDLED_SOURCES = Set.of("openclaw-bundled", "openclaw-bundled-core");

    /**
     * Checks if a skill is bundled.
     *
     * @param skill the skill to check
     * @return true if bundled
     */
    default boolean isBundledSkill(Skill skill) {
        return skill != null && skill.source() != null && BUNDLED_SOURCES.contains(skill.source());
    }
}
