# Contributing to OpenClaw Java

Thank you for your interest in contributing to OpenClaw Java Edition!

## Development Setup

### Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- Git

### Clone and Build

```bash
git clone https://github.com/openclaw/openclaw-java.git
cd openclaw-java
mvn clean install
```

## Project Structure

Each module follows standard Maven structure:

```
module-name/
├── src/
│   ├── main/java/openclaw/...    # Source code
│   └── test/java/openclaw/...    # Test code
├── pom.xml                        # Module POM
└── README.md                      # Module documentation
```

## Coding Standards

### Java Style

- Follow Google Java Style Guide
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Always use braces for control structures

### Naming Conventions

- Classes: PascalCase (e.g., `PluginRuntime`)
- Methods: camelCase (e.g., `getRuntime`)
- Constants: UPPER_SNAKE_CASE (e.g., `DEFAULT_PORT`)
- Packages: lowercase (e.g., `openclaw.sdk.core`)

### Documentation

- All public APIs must have Javadoc
- Use `@author` and `@version` tags
- Document exceptions with `@throws`

Example:
```java
/**
 * Sends a message to a channel.
 *
 * @param request the send request
 * @return a future containing the send result
 * @throws IllegalArgumentException if request is invalid
 * @author OpenClaw Team
 * @version 2026.3.9
 */
CompletableFuture<SendResult> sendMessage(SendMessageRequest request);
```

## Testing

### Writing Tests

- Use JUnit 5
- Use AssertJ for assertions
- Use Mockito for mocking
- Aim for >80% code coverage

Example:
```java
@Test
void testSendMessage() {
    // Given
    SendMessageRequest request = SendMessageRequest.builder()
            .channelId("test")
            .message("Hello")
            .build();
    
    // When
    SendResult result = adapter.sendMessage(request).join();
    
    // Then
    assertThat(result.success()).isTrue();
    assertThat(result.messageId()).isPresent();
}
```

### Running Tests

```bash
# All tests
mvn test

# Specific module
mvn test -pl openclaw-plugin-sdk

# With coverage
mvn jacoco:report
```

## Adding New Modules

1. Create directory: `openclaw-module-name`
2. Create `pom.xml` with parent reference
3. Add module to parent `pom.xml`
4. Follow existing module structure
5. Add README.md

## Adding New Channels

1. Create module: `openclaw-channel-name`
2. Implement `ChannelPlugin` interface
3. Create required adapters:
   - `ConfigAdapter`
   - `OutboundAdapter`
   - `SecurityAdapter`
4. Register in `META-INF/services/openclaw.sdk.channel.ChannelPlugin`
5. Add tests

## Pull Request Process

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Add tests
5. Update documentation
6. Run full test suite: `mvn clean test`
7. Commit with clear message
8. Push to your fork
9. Create Pull Request

### PR Checklist

- [ ] Tests pass
- [ ] Code follows style guide
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] No merge conflicts

## Commit Messages

Use conventional commits:

```
type(scope): subject

body (optional)

footer (optional)
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Tests
- `chore`: Maintenance

Example:
```
feat(telegram): add webhook support

- Implement webhook verification
- Add signature validation
- Update documentation

Closes #123
```

## Release Process

1. Update version in parent POM
2. Update CHANGELOG.md
3. Create git tag: `git tag v2026.3.9`
4. Push tag: `git push origin v2026.3.9`
5. GitHub Actions will build and publish

## Getting Help

- GitHub Issues: Bug reports and feature requests
- GitHub Discussions: Questions and ideas
- Discord: Real-time chat (link in README)

## Code of Conduct

- Be respectful and inclusive
- Welcome newcomers
- Focus on constructive feedback
- Respect different viewpoints

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
