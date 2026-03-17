# Dependencies

> All dependencies are managed via **Apache Maven** and declared in `pom.xml`.
> Maven automatically downloads and manages all required libraries.

---

## Runtime Dependencies

### 1. Google Gson

| Property    | Value                                                            |
|-------------|------------------------------------------------------------------|
| Group ID    | `com.google.code.gson`                                           |
| Artifact ID | `gson`                                                          |
| Version     | **2.10.1**                                                       |
| License     | Apache License 2.0                                               |
| Repository  | https://github.com/google/gson                                   |
| Purpose     | JSON serialization/deserialization for data persistence           |

**Usage in project**: `JsonUtil.java` uses Gson to read/write `User`, `Job`, and `Application` objects to/from JSON files in the `data/` directory.

### 2. FlatLaf (FormDev)

| Property    | Value                                                            |
|-------------|------------------------------------------------------------------|
| Group ID    | `com.formdev`                                                    |
| Artifact ID | `flatlaf`                                                       |
| Version     | **3.2.5**                                                        |
| License     | Apache License 2.0                                               |
| Repository  | https://github.com/JFormDesigner/FlatLaf                         |
| Purpose     | Modern flat Look and Feel for Java Swing applications            |

**Usage in project**: `Main.java` applies `FlatLightLaf` as the application's Look and Feel to provide a clean, modern UI appearance across all platforms.

---

## Test Dependencies

### 3. JUnit 4

| Property    | Value                                                            |
|-------------|------------------------------------------------------------------|
| Group ID    | `junit`                                                          |
| Artifact ID | `junit`                                                         |
| Version     | **4.13.2**                                                       |
| License     | Eclipse Public License 1.0                                       |
| Repository  | https://github.com/junit-team/junit4                             |
| Scope       | `test` (not included in production JAR)                          |
| Purpose     | Unit testing framework                                           |

**Usage in project**: 17 unit tests across `UserServiceTest`, `JobServiceTest`, and `ApplicationServiceTest`.

---

## Build Plugins

### 4. Maven JAR Plugin

| Property    | Value                          |
|-------------|--------------------------------|
| Group ID    | `org.apache.maven.plugins`     |
| Artifact ID | `maven-jar-plugin`            |
| Version     | **3.3.0**                      |
| Purpose     | Configures the JAR manifest with the main class entry point (`com.recruitment.Main`) |

### 5. Maven Shade Plugin

| Property    | Value                          |
|-------------|--------------------------------|
| Group ID    | `org.apache.maven.plugins`     |
| Artifact ID | `maven-shade-plugin`          |
| Version     | **3.5.1**                      |
| Purpose     | Creates a fat/uber JAR that bundles all dependencies into a single executable JAR file |

---

## Java Platform Requirements

| Requirement          | Version  | Notes                                        |
|----------------------|----------|----------------------------------------------|
| Java JDK             | 11+      | Compilation target is Java 11                |
| Apache Maven         | 3.6+     | Build tool for dependency management         |

> **Note**: The project uses `maven.compiler.source=11` and `maven.compiler.target=11`, ensuring compatibility with Java 11 and above. It has been tested with Java 21.

---

## Dependency Tree

```
ta-recruitment-system-1.0-SNAPSHOT.jar
├── com.google.code.gson:gson:2.10.1                (compile)
├── com.formdev:flatlaf:3.2.5                        (compile)
└── junit:junit:4.13.2                               (test)
    └── org.hamcrest:hamcrest-core:1.3               (test)
```

To view the full dependency tree, run:
```bash
mvn dependency:tree
```

---

## How to Add New Dependencies

1. Add the dependency to `pom.xml` under `<dependencies>`:
   ```xml
   <dependency>
       <groupId>group.id</groupId>
       <artifactId>artifact-id</artifactId>
       <version>x.y.z</version>
   </dependency>
   ```
2. Run `mvn clean compile` to download and verify the new dependency.
3. Update this file with the new dependency details.
