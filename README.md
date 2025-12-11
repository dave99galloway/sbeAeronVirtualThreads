# Aeron with SBE, Protobuf, JSON and Virtual Threads

A comprehensive demonstration of Aeron low-latency messaging with different serialization formats (SBE, Protobuf, JSON) and Java 21 Virtual Threads performance comparison.

## 📋 Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Build Commands](#build-commands)
- [Requirements](#requirements)

## 🎯 Overview

This project demonstrates:

- ✅ **Aeron IPC messaging** - Low-latency inter-process communication
- ✅ **Embedded Media Driver** - Tests run autonomously without external driver
- ✅ **Three serialization formats**:
  - **SBE (Simple Binary Encoding)** - Fastest, most compact
  - **Protobuf** - Medium speed, good interoperability
  - **JSON** - Human-readable, largest size
- ✅ **Virtual Threads (Java 21)** - Lightweight concurrency comparison
- ✅ **Performance benchmarking** - JMH and custom benchmarks
- ✅ **Resource monitoring** - Memory and thread usage tracking
- ✅ Comprehensive JUnit 5 tests with AssertJ assertions
- ✅ Cucumber BDD tests for behavior verification

## 📁 Project Structure

```
sbeAeronVirtualThreads/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/playground/sbeaeronvirtualthreads/
│   │   │       ├── Main.java              # Demo application
│   │   │       ├── aeron/                 # Aeron pub/sub
│   │   │       ├── model/                 # Domain models
│   │   │       ├── serialization/         # SBE/Protobuf/JSON serializers
│   │   │       ├── util/                  # Utilities (embedded driver)
│   │   │       └── monitoring/            # Resource monitoring
│   │   ├── proto/                         # Protobuf schemas
│   │   └── resources/
│   │       └── sbe-schema.xml             # SBE schema
│   ├── test/java/                         # JUnit tests
│   │   └── com/playground/sbeaeronvirtualthreads/
│   │       ├── aeron/                     # Aeron messaging tests
│   │       ├── serialization/             # Serialization tests
│   │       └── benchmark/                 # Performance benchmarks
│   ├── cucumber/java/                     # Step definitions
│   └── cucumber/resources/                # Feature files
├── build.gradle                           # Build configuration
└── settings.gradle                        # Project settings
```

## 🚀 Getting Started

### Prerequisites

- Java 21 or higher
- Gradle 8.x (or use included wrapper)
- **No external Aeron Media Driver needed** - Tests use embedded driver

### Building the Project

```bash
# Build and generate SBE/Protobuf code
./gradlew build

# Run the demo application
./gradlew run
```

> **Note**: The first build generates code from SBE schemas and Protobuf definitions. All tests run autonomously with an embedded Aeron Media Driver - no external setup required!

## 🧪 Running Tests

### Run all tests (JUnit + Cucumber)

```bash
./gradlew check
```

### Run only JUnit tests

```bash
./gradlew unitTest
```

### Run only Cucumber tests

```bash
./gradlew cucumber
```

### Run performance benchmarks

```bash
# Run PerformanceBenchmarkTest (shows console output)
./gradlew test --tests "*PerformanceBenchmarkTest"

# View HTML test reports
open build/reports/tests/test/index.html
```

### Run JMH benchmarks

```bash
# Run JMH benchmarks (automatically generates HTML report)
./gradlew jmh

# View HTML report (opens in browser)
open build/reports/jmh-results.html

# Or view JSON results
cat build/reports/jmh-results.json

# Generate HTML report manually (if needed)
./gradlew jmhReport
```

> **Note**: The HTML report provides a visual comparison of benchmark results with charts showing relative performance between serialization formats and threading models.

### View test results

```bash
# Console output during test run (enabled by default)
./gradlew test --tests "*PerformanceBenchmarkTest"

# HTML reports (after running tests)
open build/reports/tests/test/index.html          # All tests
open build/reports/tests/unitTest/index.html      # Unit tests only
open build/reports/tests/cucumber/index.html      # Cucumber tests
open build/reports/cucumber/cucumber.html         # Cucumber HTML report
```

## 📊 Performance Results

### Benchmark Output Examples

When running performance tests, you'll see output like:

```
=== Performance Benchmark Results ===
Format: SBE
Average Latency: 12,345 ns
Min Latency: 8,901 ns
Max Latency: 23,456 ns
Throughput: 81,234.56 msgs/sec

=== Multiple Concurrent Streams with Virtual Threads ===
Concurrent streams: 5
Messages per stream: 200
Total messages: 1000
Duration: 123 ms
Throughput: 8,130.08 msgs/sec
```

### Message Size Comparison

| Format   | Typical Size   | Relative | Notes                                |
| -------- | -------------- | -------- | ------------------------------------ |
| SBE      | ~40-80 bytes   | 1.0x     | Fixed-size binary, fastest           |
| Protobuf | ~40-100 bytes  | 0.8-1.3x | Variable-length, data-dependent size |
| JSON     | ~120-180 bytes | 2.0-3.0x | Human-readable text                  |

> **Note**: Protobuf can be smaller than SBE for certain data patterns due to variable-length encoding.

### Throughput Comparison

| Format   | Messages/sec | Relative |
| -------- | ------------ | -------- |
| SBE      | ~1M+         | 1.0x     |
| Protobuf | ~500K-800K   | 0.5-0.8x |
| JSON     | ~200K-500K   | 0.2-0.5x |

### Virtual Threads vs Platform Threads

**Virtual Threads excel when:**

- Running multiple concurrent subscribers
- Application involves I/O or blocking operations
- Need to scale to thousands of concurrent tasks

**Platform Threads excel when:**

- Pure CPU-bound tight loops
- Maximum single-threaded performance needed
- Very low latency requirements

## 🔑 Key Components

### Aeron Publisher/Subscriber

- `AeronPublisher` - Publishes messages to Aeron channels
- `AeronSubscriber` - Subscribes and polls for messages
- Supports both platform and virtual threads

### Serializers

- `TradeSbeSerializer` - SBE binary encoding (fastest)
- `TradeProtobufSerializer` - Protocol Buffers (balanced)
- `TradeJsonSerializer` - JSON with Jackson (readable)

### Models

- `Trade` - Trade message record
- `MarketData` - Market data snapshot
- `PerformanceMetrics` - Performance measurement data

### Resource Monitoring

- `ResourceMonitor` - Tracks memory and thread usage

### Testing Infrastructure

- `EmbeddedMediaDriverManager` - Reference-counted media driver for tests
- `EmbeddedMediaDriver` - Media driver wrapper for main application
- All tests run autonomously without external dependencies

## 📚 What You'll Learn

1. **Aeron Concepts**

   - IPC transport with shared memory
   - Embedded vs external media driver
   - Publisher/Subscriber pattern
   - Fragment handlers and polling
   - Idle strategies and back pressure

2. **Serialization Trade-offs**

   - Size efficiency (SBE < Protobuf < JSON)
   - Speed (SBE > Protobuf > JSON)
   - Flexibility and interoperability

3. **Virtual Threads Benefits**

   - Lower memory overhead
   - Simplified concurrent programming
   - When to use vs platform threads

4. **Performance Testing**
   - JMH microbenchmarks
   - Custom throughput testing
   - Resource usage monitoring

## 📊 Build Commands

| Command                  | Description                                                 |
| ------------------------ | ----------------------------------------------------------- |
| `./gradlew build`        | Compile and build the project (generates SBE/Protobuf code) |
| `./gradlew run`          | Run the demo application                                    |
| `./gradlew clean`        | Clean build artifacts                                       |
| `./gradlew test`         | Run all tests                                               |
| `./gradlew unitTest`     | Run JUnit tests only                                        |
| `./gradlew cucumber`     | Run Cucumber tests only                                     |
| `./gradlew check`        | Run all verification tasks                                  |
| `./gradlew jmhBenchmark` | Run JMH performance benchmarks                              |

## 📦 Requirements

- **Java**: 21 (LTS)
- **Gradle**: 8.x or higher
- **Key Libraries**:
  - Aeron 1.44.1
  - SBE 1.32.0
  - Protobuf 3.25.1
  - Jackson 2.16.0
  - JMH 1.37
  - JUnit 5.10.1
  - Cucumber 7.14.1
  - AssertJ 3.24.2

## 🔗 References

- [Aeron Documentation](https://github.com/real-logic/aeron/wiki)
- [SBE Documentation](https://github.com/real-logic/simple-binary-encoding/wiki)
- [Virtual Threads (JEP 444)](https://openjdk.org/jeps/444)
- [JMH Documentation](https://github.com/openjdk/jmh)

## 📄 License

This project is provided as-is for learning and exploration purposes.
