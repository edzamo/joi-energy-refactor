# Project Refactoring Summary

This document outlines the key improvements and refactoring changes applied to the JOI Energy project. The primary goals were to enhance code quality, fix bugs, improve performance, and align the codebase with modern Java development best practices.

## Core Principles Applied
- **Bug Fixes & Correctness**: Addressed critical flaws in business logic.
- **Code Readability & Maintainability**: Applied principles of Clean Code for a more declarative and understandable codebase.
- **Separation of Concerns**: Decoupled components like validation from controllers.
- **Performance Optimization**: Improved the efficiency of key algorithms.
- **Modern Java Practices**: Leveraged modern Java features like Records, Streams, and `Optional` for more robust and concise code.

---

## Detailed Changes by Component

### 1. Domain Layer (`uk.tw.energy.domain`)

#### `PricePlan.java`
- **What**: Converted the `PricePlan` class and its inner `PeakTimeMultiplier` class into a single, more powerful `PricePlan` **record**. The `List<PeakTimeMultiplier>` was replaced with a `Map<DayOfWeek, BigDecimal>`.
- **Why**:
    - **Immutability & Conciseness**: `record`s provide a concise syntax for creating immutable data-carrier classes, automatically generating constructors, getters, `equals()`, `hashCode()`, and `toString()`.
    - **Performance**: Using a `Map` for `peakTimeMultipliers` improves the performance of the `getPrice` method from O(n) to O(1). The previous implementation iterated through a list for every price lookup, which is inefficient. The new version performs a direct map lookup.
    - **Simplicity**: The `PeakTimeMultiplier` inner class became redundant and was removed, simplifying the domain model.

### 2. Service Layer (`uk.tw.energy.service`)

#### `PricePlanService.java`
- **What**:
    1.  Fixed a critical bug in the `calculateCost` method. The energy consumption was being calculated by `power / time` instead of `power * time`.
    2.  Refactored the `getConsumptionCostOfElectricityReadingsForEachPricePlan` method to use `Optional.filter` and `Optional.map`.
    3.  Added `setScale(2, RoundingMode.HALF_UP)` to the final cost calculation.
- **Why**:
    1.  **Correctness**: The bug fix ensures that the core business logic for calculating energy cost is now correct, which is fundamental to the application's purpose.
    2.  **Robustness & Readability**: Using `Optional`'s functional methods makes the code more declarative and robust, elegantly handling cases where readings might be absent or empty without nested `if` checks.
    3.  **Correct Formatting**: As we are dealing with monetary values, rounding the final cost to two decimal places is the correct approach.

#### `MeterReadingService.java`
- **What**: The `storeReadings` method was refactored to use `Map.computeIfAbsent()`.
- **Why**:
    - **Conciseness**: This change replaces a multi-line `if-check-then-get-then-add` pattern with a single, expressive line of code. It clearly states the intent: "get the list for this meter, or create a new one if it doesn't exist, then add the readings."

### 3. Controller Layer (`uk.tw.energy.controller`)

#### `MeterReadingController.java`
- **What**:
    1.  Extracted validation logic into a new `MeterReadingValidator` class within a new `validator` package.
    2.  Changed the HTTP response for invalid input in `storeReadings` from `500 INTERNAL_SERVER_ERROR` to `400 BAD_REQUEST`.
    3.  Refactored the `readReadings` method to use `Optional.map().orElse()`.
- **Why**:
    1.  **Separation of Concerns**: Controllers should orchestrate requests, not perform complex validation. Moving this logic to a dedicated validator class makes the code cleaner, more modular, and easier to test and reuse.
    2.  **Correct API Semantics**: A `400` status code correctly informs the client that their request is malformed. A `500` error should be reserved for unexpected server-side failures.
    3.  **Readability**: The functional approach to handling the `Optional` is more concise and declarative than an `if/else` or ternary operator.

#### `PricePlanComparatorController.java`
- **What**:
    1.  Refactored both endpoints to use `Optional.map().orElse()` to handle the response from the service layer.
    2.  Simplified the recommendation logic in `recommendCheapestPricePlans` to use a Java Stream with `sorted()` and `limit()`.
- **Why**:
    - **Reduced Duplication & Improved Readability**: Both methods shared identical logic for handling an empty `Optional`. This refactoring removes duplication and makes the controller's logic more streamlined and easier to follow.
    - **Declarative Style**: Using a Stream to sort and limit the recommendations is more declarative and less error-prone than manually creating a list, sorting it, and then taking a sublist.

### 4. Generator Layer (`uk.tw.energy.generator`)

#### `ElectricityReadingsGenerator.java`
- **What**: The `generate` method was refactored from an imperative `for` loop to a declarative `IntStream`. The explicit `sort` call was also removed.
- **Why**:
    - **Modern & Efficient**: Using a Stream is a more modern, functional approach. It also improves efficiency by generating the readings in the correct chronological order from the start, making the final `sort` operation unnecessary.

### 5. Configuration (`uk.tw.energy`)

#### `SeedingApplicationDataConfiguration.java`
- **What**: Updated the `pricePlans` bean to instantiate `PricePlan` objects with an `emptyMap()` instead of an `emptyList()`.
- **Why**: This was a necessary change to support the refactoring of the `PricePlan` class to a `record` that now expects a `Map` for its multipliers.

---

## Conclusion

The applied refactorings have significantly improved the overall quality of the codebase. The application is now more **robust**, **efficient**, and **maintainable**. By embracing modern Java features and established software design principles, we have created a stronger foundation for future development and scalability.
