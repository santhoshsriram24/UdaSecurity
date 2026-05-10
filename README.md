# UdaSecurity

Multi-module Java Maven project for a home security monitoring application.

## Project Structure

- securityservice: Core security/alarm logic and Swing application
- imageservice: Image analysis service abstractions and implementations

## Tech Stack

- Java 17
- Maven (multi-module)
- JUnit 5 + Mockito
- Gson, Guava
- AWS SDK v2 (Rekognition support)

## Implemented Requirements

All 11 Udacity application requirements are implemented and covered by unit tests in:

- securityservice/src/test/java/com/udacity/catpoint/service/SecurityServiceTest.java

Current status:

- 23 tests passing
- Build commands verified
- Executable JAR verified

## Build and Run

From project root:

```powershell
mvn compile
mvn test
mvn install
mvn site
```

Run executable JAR:

```powershell
java -jar .\securityservice\target\UdaSecurity.jar
```

## Notes

- SpotBugs is configured but set to skip by default due environment compatibility constraints.
- The executable JAR is created in securityservice/target/UdaSecurity.jar.
