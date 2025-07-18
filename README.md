# KOFA - Kotlin for All 🚀

A high-performance, event-driven application framework built entirely in Kotlin. KOFA leverages reactive programming, metaprogramming, and compiler plugins to separate business logic completely from infrastructure concerns.

## ✨ Key Features

- **Pure Kotlin**: 100% Kotlin implementation with native DSL support
- **Event-Driven**: Built for reactive, high-throughput event processing
- **Zero Boilerplate**: Compiler plugins generate all infrastructure code
- **Modular**: Components deploy independently at runtime
- **High Performance**: Optimized for low-latency applications with Aeron messaging

## 🏗️ Architecture

KOFA enforces a clean 3-tier architecture:

| Layer | Responsibility |
|-------|----------------|
| **Application** | Configuration, dependency declaration, runtime setup |
| **Business** | Domain logic, event handlers, business rules |
| **Platform** | Infrastructure, lifecycle management, event routing, performance optimization |

## 🚀 Quick Start

### 1. Define Your Domain
Create message definitions by XML file, annotation or Kotlin DSL, currently only xml domain definition is supported

```xml
<domain name="carnival">
    <message name="Ping">
        <field name="num" type="int"/>
    </message>
    <message name="Pong">
        <field name="num" type="int"/>
    </message>
</domain>
```

### 2. Implement Event Handlers 
```kotlin
class MonkeyHandler(val config: MonkeyConfig) {
    fun handle(event: PongEvent) {
        // Your business logic here
        println("Processing ${event.num}")
    }
}
```

### 3. Create component module
```kotlin
@DomainModule(componentType="Monkey", handler = MonkeyHandler::class)
class MonkeyModule {
    fun config() = {
        scoped{
            MonkeyConfig()
        }
    }
}
```

### 4. Configure Runtime
Create `application.conf`:
```hocon
kofa {
  domain = carnival
  timezone = Asia/Shanghai

  event-loop {
    shutdown-timeout-seconds = 10
    execution-policy = "wait"
    sleepCycles = "10"
    durationMillis = "1000"
  }

  event-stream {
    mode = "live"
    url = "aeron://carnival?embedded=true"
    aeron {
      channel = "aeron:ipc"
      sessionId = 1
    }
  }
  
  component {
    Monkey: {
      enabled = true
    } 
  }
}

```

### 5. Run Your Application
```bash
./gradlew examples:carnival:run --args='-c application-carnival.conf'
```

## 📁 Project Structure

```
kofa/
├── platform/          # Core framework
│   ├── platform-api/      # Public APIs and annotations
│   ├── platform-core/     # Runtime implementation
│   ├── platform-codegen/  # Code generation
│   └── platform-launcher/ # Application launcher
├── examples/          # Sample applications
│   ├── carnival/          # Simple event handling demo
│   └── mds/              # Market data system example
└── build.gradle.kts   # Kotlin DSL build configuration
```

## 📚 Examples

Explore working examples in the `examples/` directory:

- **Carnival**: Basic event handling with clowns and monkeys 🎪
- **MDS**: Real-world portfolio management system 📈

## 🔧 Build & Development

```bash
# Build all modules
./gradlew build

# Run specific example
./gradlew examples:mds:run --args='-c application-mds.conf'
```

## 📖 Resources

- [中文教程](https://pv2sgxx0xup.feishu.cn/docx/LqLgdaNoeoNkhexMMjkcgJX1n4g) - Detailed tutorial (Chinese)
- [Examples](./examples) - Complete working examples

## 🎯 Use Cases

- High-frequency trading systems
- Real-time data processing pipelines
- Event-driven microservices
- Low-latency messaging applications
- Backtesting and simulation engines