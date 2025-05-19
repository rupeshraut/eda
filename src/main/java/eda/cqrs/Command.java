package eda.cqrs;

import java.util.Map;

/**
 * The type Command.
 */
// Command-Query Responsibility Segregation (CQRS)
public record Command(String name, Map<String, Object> parameters) {
}