# sbeAeronVirtualThreads

## Project Overview

An Aeron low-latency messaging demonstration comparing serialization formats (SBE, Protobuf, JSON) and Java 21 Virtual Threads performance.

## Key Technologies

- **Aeron 1.44.1** - Low-latency IPC messaging
- **SBE 1.32.0** - Simple Binary Encoding (fastest serialization)
- **Protobuf 3.25.1** - Protocol Buffers (balanced)
- **Jackson 2.16.0** - JSON serialization (human-readable)
- **Java 21** - Virtual Threads support
- **JMH 1.37** - Performance benchmarking

## Project Structure

- `src/main/java` - Main source code
  - `aeron/` - Publisher/Subscriber implementations
  - `serialization/` - SBE, Protobuf, JSON serializers
  - `util/` - EmbeddedMediaDriver for autonomous tests
- `src/test/java` - JUnit tests with embedded driver
- `src/cucumber/java` - Cucumber step definitions
- `src/cucumber/resources` - Cucumber feature files
- `build/generated-src/` - SBE generated code
- `build/generated/source/proto/` - Protobuf generated code

## Build Commands

- `./gradlew build` - Build entire project (generates SBE/Protobuf code)
- `./gradlew test` - Run all JUnit tests
- `./gradlew unitTest` - Run unit tests only
- `./gradlew cucumber` - Run Cucumber BDD tests
- `./gradlew check` - Run all verification tasks
- `./gradlew jmhBenchmark` - Run JMH performance benchmarks

## Code Standards

- Use Java 21 features (Virtual Threads, Records, Pattern Matching)
- Write tests with AssertJ assertions
- Follow BDD principles for Cucumber tests
- Tests use embedded Aeron Media Driver - no external setup needed
- Keep code clean and well-documented

## Important Notes

- All tests run autonomously with `EmbeddedMediaDriverManager`
- JVM args required for Aeron: `--add-opens java.base/sun.nio.ch=ALL-UNNAMED`
- Code generation happens automatically during build (SBE and Protobuf)
- Use `publisher.publish(length)` when data is already in buffer
- Use `publishWithRetry(data, offset, length)` to copy and publish external data
