---
description: Development standards, coding style, and best practices for Java projects.
globs: [“**/*.java”]
---

# Guidelines for Java Development

You must strictly adhere to the standards of the Java ecosystem (based on Google and Oracle conventions) as well as the following modern design principles.

## 1. Naming Conventions
- **Classes & Interfaces:** `PascalCase` (e.g., `UserService`, `UserRepository`). Interfaces should use adjectives whenever possible (e.g., `Serializable`, `Runnable`).
- **Methods & Variables:** `camelCase` (e.g., `calculateTotal()`, `userId`).
- **Constants:** `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_ATTEMPTS`).
- **Packages:** All lowercase, separated by periods (e.g., `com.company.project.controller`).

## 2. Code Style and Structure
- **Indentation:** Use 4 spaces per indentation level. Do not use tabs.
- **Braces:** Use the Egyptian style (the opening brace `{` remains at the end of the line, and the closing brace `}` is on its own line).
- **Line length:** Do not exceed 100 to 120 characters per line. Use clean line breaks when necessary.
- **Explicit blocks:** ALWAYS use curly braces `{}` for control structures (`if`, `else`, `for`, `while`), even for a single line of code.

## 3. Modern Best Practices (Java 17+)
- **Immutability:** Prefer using the `final` keyword for local variables and parameters that must not change. Use `record` for DTOs and immutable data structures.
- **Resource Management:** Always use the `try-with-resources` block to automatically release streams (I/O, JDBC connections).
- **Control Structures:** Use modern `switch` expressions (with the `->` arrow) instead of long chains of `if-else` statements.
- **Functional Programming:** Use the `Streams` API and lambda expressions wisely to manipulate collections.

## 4. Robustness and Error Handling
- **🚨 Strictly prohibited:** NEVER leave `catch` blocks empty. If an exception is intentionally ignored, explicitly document this with a comment.
- **Null Safety:** Use `Optional<T>` for method return types that may be absent instead of returning `null`.
- **Guard Clauses:** Validate arguments at the beginning of a method (e.g., `Objects.requireNonNull()`) to fail as early as possible (*Fail-Fast*).

## 5. Logging and Testing
- **Logging:** NEVER use `System.out.println()` or `e.printStackTrace()` in application code. Use an official logger (e.g., SLF4J with Logback or Log4j2).
- **Testing:** Every new feature or bug fix must be accompanied by unit tests (JUnit 5 / Mockito).


## 6. Deployment
- **Github Action** Every deployment must be done via github action. You do changes, you commit, you push, you do the PR to main branch, you approve the PR and merge them. Once merged github action script will deploy.