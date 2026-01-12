# Contributing to GRIN

Thank you for your interest in contributing to GRIN (Graphic Readdressable Indexed Nodes). This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Style Guidelines](#style-guidelines)

---

## Code of Conduct

Be respectful, inclusive, and constructive. We're building a technical standard—focus on the work, not personalities.

---

## Getting Started

### Prerequisites

**For Web Development:**
- Node.js 18+ (LTS recommended)
- npm 9+
- Modern browser with ES6+ support (for demos)

**For Android Development:**
- JDK 17+
- Android Studio (latest stable) or command-line tools
- Android SDK (API 21-34)

**For CLI Tools:**
- Node.js 18+

### Quick Start

```bash
# Clone the repository
git clone https://github.com/grin-format/grin.git
cd grin

# Web development
cd web
npm install
npm run build
npm test

# Android development
cd android
./gradlew build

# CLI tools
cd tools
npm install
```

---

## Development Setup

### Web Implementation

```bash
cd web
npm install

# Development commands
npm run build         # Build library
npm run build:watch   # Build with watch mode
npm run test          # Run tests
npm run test:watch    # Run tests in watch mode
npm run lint          # Run linter
npm run typecheck     # TypeScript type checking
```

### Android Implementation

```bash
cd android

# Build library
./gradlew :lib:assembleRelease

# Run tests
./gradlew :lib:testReleaseUnitTest

# Build demo app
./gradlew :demo:assembleDebug

# Install demo on connected device
./gradlew :demo:installDebug
```

### IDE Setup

**VS Code (Web):**
- Install ESLint extension
- Install TypeScript extension
- Workspace settings are pre-configured

**Android Studio:**
- Open the `android/` directory as a project
- Sync Gradle files when prompted
- Use the built-in code formatter

---

## Project Structure

```
grin/
├── spec/                    # Specification documents
│   └── grin_technical_specification_v_2.md
├── core/                    # Core reference implementation
│   ├── src/                 # Source files
│   └── tests/               # Core tests
├── android/                 # Android implementation
│   ├── lib/                 # Library module
│   └── demo/                # Demo application
├── web/                     # JavaScript implementation
│   ├── lib/                 # Library source
│   ├── demo/                # Demo web app
│   └── tests/               # JavaScript tests
├── tools/                   # CLI utilities
│   ├── encoder/             # PNG → GRIN encoder
│   ├── decoder/             # GRIN → PNG decoder
│   └── validator/           # File validator
├── samples/                 # Sample .grin files
├── docs/                    # Documentation
├── todo.md                  # Build plan
├── agents.txt               # Agentic contract
└── README.md                # Project overview
```

---

## Making Changes

### Branch Naming

- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation changes
- `refactor/description` - Code refactoring

### Commit Messages

Follow this format:

```
[PHASE.SECTION] Brief description (max 72 chars)

Longer description if needed.

- Bullet points for multiple changes
- Reference spec section if applicable

Refs: #issue-number (if applicable)
```

Example:

```
[1.2] Implement header byte layout structure

- Define all 128 header bytes per spec section 7.3
- Add endianness handling for multi-byte fields
- Include reserved field zero-initialization

Refs: spec section 7.2, 7.3
```

### Checklist Before Committing

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] New code has tests
- [ ] Documentation is updated
- [ ] Commit message follows format
- [ ] No spec violations introduced

---

## Testing

### Test Requirements

- **Unit tests**: Required for all new functions/methods
- **Coverage**: Aim for >80% coverage
- **Edge cases**: Test boundary values and error conditions

### Running Tests

```bash
# Web
cd web && npm test

# Android
cd android && ./gradlew test

# CLI tools
cd tools && npm test
```

### Test File Naming

- Web: `*.test.ts` or `*.test.js`
- Android: `*Test.kt` in `src/test/`
- Integration: `*.integration.test.*`

---

## Submitting Changes

### Pull Request Process

1. **Create a branch** from `develop`
2. **Make your changes** following the guidelines
3. **Write/update tests**
4. **Update documentation**
5. **Submit PR** against `develop`
6. **Address review feedback**
7. **Merge** (maintainers only)

### PR Description Template

```markdown
## Summary
Brief description of changes.

## Changes
- Change 1
- Change 2

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing performed
- [ ] All tests passing

## Spec Compliance
- References spec section(s): X.Y
- No spec violations: Yes/No

## Breaking Changes
None / Description of breaking changes
```

---

## Style Guidelines

### TypeScript/JavaScript

- Use TypeScript for all new code
- Strict mode enabled
- No `any` types (use `unknown` if needed)
- Prefer `const` over `let`
- Use explicit return types

```typescript
// Good
function getGroupId(controlByte: number): number {
  return controlByte & 0x0F;
}

// Avoid
function getGroupId(c) {
  return c & 0x0F;
}
```

### Kotlin/Java

- Kotlin preferred for new Android code
- Use `val` over `var` where possible
- Null safety: Use nullable types explicitly
- Follow Android coding standards

```kotlin
// Good
fun getGroupId(controlByte: Int): Int = controlByte and 0x0F

// Avoid
fun getGroupId(controlByte: Int): Int {
    return controlByte.and(0x0F)
}
```

### Documentation

- All public APIs must have documentation comments
- Use JSDoc for TypeScript, KDoc for Kotlin
- Include parameter descriptions and return values
- Add examples for complex functions

---

## Security Considerations

GRIN is designed to be secure by construction. When contributing:

1. **No executable code** in GRIN files
2. **Bounded resource consumption** always
3. **Validate all input** before processing
4. **No external resource access** during playback
5. **Fail safely** with clear error messages

Report security issues privately to the maintainers.

---

## Questions?

- Open an issue for questions
- Check existing issues before creating new ones
- Reference the specification for format questions

Thank you for contributing to GRIN!
