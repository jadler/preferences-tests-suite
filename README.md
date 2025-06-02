# Preferences Test Suite

A reusable test suite for validating custom implementations of the `java.util.prefs.Preferences` API.

This project is intended to help me (and other developers) ensure that their `Preferences`-based storage backends comply with the expected behavior defined by the Java API.

## Purpose

This JUnit 5-based test suite provides ready-to-use test classes that verify core API contracts such as:

- Node creation and removal
- Key/value storage
- Type-specific operations (e.g., `getInt`, `getBoolean`)
- Persistence across sessions
- Scope handling (`user` vs `system`)


## Technologies Used

- **Java 17+**
- **JUnit Jupiter (JUnit 5)**


## How to Use

Add this project as a `test`-scoped dependency to the Preferences implementation project:

```xml
<dependency>
  <groupId>br.dev.jadl.preferences</groupId>
  <artifactId>preferences-tests-suite</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

Then, extend the provided test classes or use them as references in your own test cases.

Example
```java
class MyPreferencesTest extends AbstractPreferencesComplianceTest {

    @BeforeEach
    void setup() {
        System.setProperty("java.util.prefs.PreferencesFactory", "com.example.MyPreferencesFactory");
    }
}
```

## Known Limitations
This suite currently focuses on testing basic and intermediate API features.

Advanced scenarios (e.g., concurrent access, ACL enforcement) may need custom extensions.

Per-user storage behavior is assumed consistent across different backends but not enforced.

## Contributing
Feel free to open issues or pull requests to report bugs or suggest improvements.
You can also contribute new test modules for different storage backends.

## License
This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
