
# Java Petri Concurrency Monitor

This project implements a concurrency monitor using Java 8. Although the code is not entirely functional, it extensively uses Java's Stream API, which is a feature of Java 8. Familiarity with this API is required to understand the code. You can read the official documentation [here](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html).

## Petri Nets

A Petri net is a graphical and mathematical modeling tool that generalizes state machines. Petri nets provide a graphical notation for stepwise processes that include choice, iteration, and concurrent execution. Unlike other standard models, Petri nets have an exact mathematical definition of their execution semantics, with a well-developed mathematical theory for process analysis.

## Monitor

In concurrent programming, a monitor is a thread-safe class, object, or module that uses mutual exclusion to safely allow access to a method or variable by more than one thread. The defining characteristic of a monitor is that its methods are executed with mutual exclusion, meaning that at any given time, at most one thread may be executing any of its methods.

For further information, see:
- [Monitor in Wikipedia](https://en.wikipedia.org/wiki/Monitor_(synchronization)).

## Key Features

- Import from PNML format files (only a small subset is currently supported)
- Support for the following Petri net types:
  - Place/Transition net
  - Timed net
- Automatic generation of:
  - Incidence Matrix $$C$$
  - Solution vector for the equation $$C \times \omega = 0$$ (T-Invariants)
  - Solution vector for the equation $$\alpha \times C^{T} = 0$$ (P-Invariants)

### Transition Policy

Support for transition policies will be added in the future.
