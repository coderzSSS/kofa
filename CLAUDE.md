# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
KOFA (Kotlin for All) is a high-performance, event-driven application framework built entirely in Kotlin. It enforces a strict 3-tier architecture separating business logic from infrastructure concerns through compiler plugins and code generation.

## Architecture
- **Application Layer**: Configuration, dependency injection, runtime setup
- **Business Layer**: Domain logic, event handlers, business rules  
- **Platform Layer**: Infrastructure, lifecycle management, event routing, performance optimization

## Build Commands
```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Run specific example applications
./gradlew examples:carnival:run --args='-c application-carnival.conf'
./gradlew examples:mds:run --args='-c application-mds.conf'

# Build with documentation
./gradlew dokkaHtmlMultiModule

# Check dependency updates
./gradlew dependencyUpdates
```

## Development Workflow
1. **Define domain messages** in XML files (e.g., `carnival-master.xml`)
2. **Implement event handlers** as Kotlin classes with handle() methods
3. **Create component modules** annotated with `@DomainModule`
4. **Configure runtime** via HOCON config files (e.g., `application-carnival.conf`)
5. **Run application** through platform launcher

## Key Annotations & DSL
- `@DomainModule(componentType, handlerClass)`: Marks component configuration
- `@DomainMessage`: Marks message classes (generated)
- `@DomainMessageHandler`: Marks handler methods (generated)
- `ComponentModuleDeclaration`: Koin DSL for dependency configuration

## Configuration Structure
```hocon
domain = "your-domain-name"
timezone = "Asia/Shanghai"

event-loop {
    shutdown-timeout-seconds = 10
    execution-policy = "wait" // or "busy"
    sleepCycles = 10
    durationMillis = 1000
}

event-stream {
    mode = "live" // "replay", "record", "auto"
    url = "aeron://your-app" // or "ipc://name" for local
}

component {
    YourComponentType {
        enabled = true
        maxValue = 10 // component-specific config
    }
}
```

## Domain Message Definition
Messages are defined in XML files placed in `src/main/resources/`:
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

## Event Handler Pattern
```kotlin
class YourHandler(val config: YourConfig) {
    fun handle(event: YourEvent) {
        // Business logic here
        println("Processing ${event.field}")
    }
}
```

## Module Declaration Pattern
```kotlin
@DomainModule(componentType = "YourComponent", handlerClass = YourHandler::class)
object YourModule {
    fun config(): ComponentModuleDeclaration = {
        scoped {
            val config = get<Config>()
            YourConfig(config.getInt("maxValue"))
        }
    }
}
```

## Technology Stack
- **Language**: Kotlin 2.1.0
- **Build**: Gradle with Kotlin DSL
- **Messaging**: Aeron (high-performance messaging)
- **Dependency Injection**: Koin
- **Configuration**: Typesafe Config (HOCON)
- **Testing**: JUnit 5, Kotest, MockK
- **Code Generation**: KSP (Kotlin Symbol Processing)

## Module Structure
```
platform/
├── platform-api/          # Public APIs, annotations, DSL
├── platform-core/         # Runtime implementation
├── platform-codegen/      # Code generation via KSP
└── platform-launcher/     # Application entry point

examples/
├── carnival/              # Simple event handling demo
└── mds/                   # Market data system example
```

## Important Files
- **Launcher Entry**: `platform/platform-launcher/src/main/kotlin/io/kofa/platform/launcher/LauncherMain.kt`
- **Config Loading**: `platform/platform-core/src/main/kotlin/io/kofa/platform/core/launcher/PlatformLauncher.kt`
- **Annotations**: `platform/platform-api/src/main/kotlin/io/kofa/platform/api/annotation/`
- **Examples**: `examples/carnival/` and `examples/mds/` directories