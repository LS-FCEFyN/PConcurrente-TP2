# Java Petri Net Concurrency Monitor

This project implements a Petri net executor with a concurrency monitor.  
Although the code is not yet fully functional, it makes extensive use of **Java's Stream API** (introduced in Java 8). Therefore, familiarity with the `Stream` API is essential to understand the implementation.

🔗 Official documentation: [Java 8 Stream API](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)

---

## Petri Nets

A **Petri net** is a graphical and mathematical modeling tool that generalizes state machines. It provides a notation for describing stepwise processes involving **choice**, **iteration**, and **concurrent execution**. Unlike many standard models, Petri nets have a precise mathematical definition of their execution semantics, supported by a mature theoretical foundation for process analysis.

> 💡 **Note**: This project relies heavily on the *mathematical representation* of Petri nets—not merely treating them as bipartite graphs modified by concurrent threads. We strongly recommend that you, the reader, become familiar with the formal notation before diving into the code.

---

## Formal Definition

A Petri net $ N $ is defined by the quintuple:  
$$
N = \langle P, T, F, W, M_0 \rangle
$$

Where:

- $ P $ is a finite set of **places**
- $ T $ is a finite set of **transitions**, with $ P \cap T = \emptyset $ (disjoint sets)
- $ F \subseteq (P \times T) \cup (T \times P) $ is the **flow relation** (i.e., directed arcs between places and transitions)
- $ W: F \rightarrow \mathbb{N}^+ $ is the **weight function**, assigning a positive integer to each arc
- $ M_0: P \rightarrow \mathbb{N}_0 $ is the **initial marking**

From this, three key matrices can be derived (in practice, two are constructed explicitly, and the third is computed from them):

### Pre Matrix
The **pre matrix** $\mathbf{Pre} \in \mathbb{N}^{|P| \times |T|}$ is defined as:

$$
\mathbf{Pre}[p,t] = \begin{cases}
W(p,t) & \text{if } (p,t) \in F \\
0 & \text{otherwise}
\end{cases}
$$

### Post Matrix
The **post matrix** $\mathbf{Post} \in \mathbb{N}^{|P| \times |T|}$ is defined as:

$$
\mathbf{Post}[p,t] = \begin{cases}
W(t,p) & \text{if } (t,p) \in F \\
0 & \text{otherwise}
\end{cases}
$$

### Incidence Matrix
The **incidence matrix** $\mathbf{C} \in \mathbb{Z}^{|P| \times |T|}$ is defined as:

$$\mathbf{C} = \mathbf{Post} - \mathbf{Pre}$$

or equivalently:

$$
\mathbf{C}[p,t] = \mathbf{Post}[p,t] - \mathbf{Pre}[p,t]
$$

---

## Structural Properties and Invariants

The incidence matrix $\mathbf{C}$ enables the analysis of structural properties through linear algebra. Two fundamental equations define the **invariants** of a Petri net:

### P-invariants (Place Invariants)
A **P-invariant** is a non-zero vector $X \in \mathbb{N}^{|P|}$ that satisfies:
$$
\mathbf{C}^T \cdot X = \mathbf{0}
$$

Where:
- $X[p]$ represents the weight assigned to place $p$
- $\mathbf{C}^T$ is the transpose of the incidence matrix
- $\mathbf{0}$ is the zero vector of dimension $|T|$

P-invariants correspond to **conservation laws** - sets of places where the weighted sum of tokens remains constant regardless of transition firings.

### T-invariants (Transition Invariants)
A **T-invariant** is a non-zero vector $U \in \mathbb{N}^{|T|}$ that satisfies:
$$
\mathbf{C} \cdot U = \mathbf{0}
$$

Where:
- $U[t]$ represents the number of times transition $t$ fires
- $\mathbf{0}$ is the zero vector of dimension $|P|$

T-invariants correspond to **firing sequences** that leave the marking unchanged, representing cyclic behaviors or equilibrium states.

> 💡 **Additional Properties**: This section covers fundamental structural properties through linear algebra. However, Petri net theory includes other important behavioral properties not detailed here, such as **liveness** (absence of deadlocks), **boundedness** (limiting token accumulation), **safety** (a special case of boundedness where places never contain more than one token). These properties are crucial for analyzing system behavior but require different analytical approaches of which I'm not yet familiar.

---

## Monitor

In concurrent programming, a **monitor** is a thread-safe construct (class, object, or module) that uses **mutual exclusion** to ensure safe access to shared methods or variables by multiple threads. Its defining property is that **only one thread may execute any of its methods at a time**.

📚 Learn more: [Monitor (synchronization) – Wikipedia](https://en.wikipedia.org/wiki/Monitor_(synchronization))

---