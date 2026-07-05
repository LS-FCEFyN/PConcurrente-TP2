# Java Petri Net Concurrency Monitor

This project implements a Petri net executor with a concurrency monitor. Although the code is not yet fully functional, it makes extensive use of **Java's Stream API** (introduced in Java 8). Familiarity with the `Stream` API is therefore essential to understand the implementation.

🔗 Official documentation: [Java 8 Stream API](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)

This project relies heavily on the **mathematical** representation of Petri nets — not merely on the idea of a bipartite graph walked by concurrent threads — and on a specific methodology, proposed by Ventre & Micolini [1], for deriving the number of worker threads a net requires and *which* part of the net each thread is responsible for. Consequently, this README is written as a self-contained reference: every algorithm implemented in `PetriNetAnalysis.java` is stated here first as a formal definition, then as pseudocode, with a pointer to the method that implements it.

### Contents

1. [Petri Nets](#petri-nets)
2. [Formal Definition](#formal-definition)
3. [Pre, Post and Incidence Matrices](#pre-post-and-incidence-matrices)
4. [Firing Rule and State Equation](#firing-rule-and-state-equation)
5. [Reachability Set and Reachability Graph](#reachability-set-and-reachability-graph)
6. [Structural Invariants](#structural-invariants)
7. [Automatic Thread Determination](#automatic-thread-determination)
8. [Behavioral Properties](#behavioral-properties)
9. [Concurrency Monitor](#concurrency-monitor)
10. [References](#references)

---

## Petri Nets

A **Petri net** is a graphical and mathematical modeling tool that generalizes state machines. It provides a notation for describing stepwise processes involving **choice**, **iteration**, and **concurrent execution**. Unlike many informal models, Petri nets have a precise mathematical definition of their execution semantics, backed by a mature theory for structural and behavioral analysis.

## Formal Definition

A Petri net $N$ is defined by the quintuple:

$$
N = \langle P,\; T,\; F,\; W,\; M_0 \rangle
$$

where:

- $P$ is a finite set of **places**.
- $T$ is a finite set of **transitions**, with $P \cap T = \emptyset$.
- $F \subseteq (P \times T) \cup (T \times P)$ is the **flow relation** (directed arcs between places and transitions).
- $W : F \rightarrow \mathbb{N}^{+}$ is the **weight function**, assigning a positive integer to every arc.
- $M_0 : P \rightarrow \mathbb{N}_0$ is the **initial marking**.

For a place $p$, its **preset** $\,{}^{\bullet}p = \{t \in T : (t,p) \in F\}$ is the set of transitions that feed it, and its **postset** $p^{\bullet} = \{t \in T : (p,t) \in F\}$ is the set of transitions it feeds. Presets and postsets of a transition $t$ (${}^{\bullet}t$, $t^{\bullet}$) are defined symmetrically over places.

## Pre, Post and Incidence Matrices

From $N$, three matrices can be derived. In practice, `Pre` and `Post` are built explicitly from the model, and `C` is computed from them.

**Pre matrix** $\;\mathbf{Pre} \in \mathbb{N}_0^{\,|P| \times |T|}$:

$$
\mathbf{Pre}[p,t] =
\begin{cases}
W(p,t) & \text{if } (p,t) \in F \\
0 & \text{otherwise}
\end{cases}
$$

**Post matrix** $\;\mathbf{Post} \in \mathbb{N}_0^{\,|P| \times |T|}$:

$$
\mathbf{Post}[p,t] =
\begin{cases}
W(t,p) & \text{if } (t,p) \in F \\
0 & \text{otherwise}
\end{cases}
$$

**Incidence matrix** $\;\mathbf{C} \in \mathbb{Z}^{\,|P| \times |T|}$:

$$
\mathbf{C} = \mathbf{Post} - \mathbf{Pre}, \qquad \mathbf{C}[p,t] = \mathbf{Post}[p,t] - \mathbf{Pre}[p,t]
$$

`Pre[p][t] > 0` means firing $t$ *consumes* tokens from $p$; `Post[p][t] > 0` means firing $t$ *produces* tokens in $p$. `C[p][t]` is the net effect on $p$ of firing $t$ once.

## Firing Rule and State Equation

A transition $t$ is **enabled** at marking $M$ iff every one of its input places holds enough tokens:

$$
\text{enabled}(t, M) \iff \forall p \in P : M(p) \ge \mathbf{Pre}[p,t]
$$

If $t$ is enabled at $M$, firing it yields the marking $M'$, written $M \xrightarrow{t} M'$:

$$
M'(p) = M(p) - \mathbf{Pre}[p,t] + \mathbf{Post}[p,t], \qquad \forall p \in P
$$

More generally, for a **firing count vector** $\vec{\sigma} \in \mathbb{N}_0^{\,|T|}$ (how many times each transition fires along some sequence), the **state equation** gives the resulting marking directly from the incidence matrix, without replaying the sequence step by step:

$$
M = M_0 + \mathbf{C} \cdot \vec{\sigma}
$$

This equation is what allows the monitor to reason about reachable states algebraically instead of only by simulation, and it is the basis for every algorithm described below.

## Reachability Set and Reachability Graph

The **reachability set** $RS(N, M_0)$ is the smallest set of markings containing $M_0$ and closed under firing:

$$
RS(N,M_0) = \{\, M \; : \; M_0 \xrightarrow{t_1} M_1 \xrightarrow{t_2} \cdots \xrightarrow{t_k} M = M_k, \; k \ge 0 \,\}
$$

The **reachability graph** $RG(N,M_0) = (RS(N,M_0), E)$ additionally records every one-step firing as a labeled edge, $E \subseteq RS(N,M_0) \times T \times RS(N,M_0)$. All four behavioral properties in this document, and both projections used by the thread-determination algorithms ($MA$ and $MS_i$ below), are computed over $RG(N,M_0)$.

Because $RS(N,M_0)$ can be infinite for unbounded nets, the implementation explores it breadth-first and stops after a fixed number of states:

```
Algorithm 0 — Reachability Graph Construction
Input:  N = ⟨P,T,F,W,M0⟩, a cap K on the number of states
Output: RG = (V,E), V ⊆ ℕ0^|P|, E ⊆ V × T × V

 1. V ← {M0} ; Queue ← [M0] ; E ← ∅
 2. while Queue is not empty and |V| < K:
 3.     M ← Queue.pop()
 4.     for each t ∈ T:
 5.         if enabled(t, M):
 6.             M' ← M − Pre[:,t] + Post[:,t]
 7.             E ← E ∪ {(M, t, M')}
 8.             if M' ∉ V:
 9.                 V ← V ∪ {M'} ; Queue.push(M')
10. return (V, E)          // if |V| = K was hit, RG is a truncated approximation
```

Implementation: `PetriNetAnalysis.generateReachabilityGraphWithEdges` (cap `MAX_STATES = 50000`); `generateReachabilityGraph` returns just $V$.

## Structural Invariants

The incidence matrix $\mathbf{C}$ enables analysis through linear algebra alone, without exploring $RS(N,M_0)$.

### P-invariants (Place Invariants)

A **P-invariant** is a non-zero vector $X \in \mathbb{N}_0^{\,|P|}$ satisfying:

$$
\mathbf{C}^{T} \cdot X = \mathbf{0}_{|T|}
$$

$X[p]$ is the weight assigned to place $p$, and $\mathbf{0}_{|T|}$ is the zero vector of dimension $|T|$. P-invariants correspond to **conservation laws**: for every $M \in RS(N,M_0)$, the weighted sum $\sum_p X[p]\,M(p)$ equals $\sum_p X[p]\,M_0(p)$, regardless of which transitions fire.

### T-invariants (Transition Invariants)

A **T-invariant** is a non-zero vector $U \in \mathbb{N}_0^{\,|T|}$ satisfying:

$$
\mathbf{C} \cdot U = \mathbf{0}_{|P|}
$$

$U[t]$ is the number of times $t$ fires, and $\mathbf{0}_{|P|}$ is the zero vector of dimension $|P|$. T-invariants correspond to **firing sequences that return the marking to itself** — cyclic behaviors or equilibrium states. This is exactly the structure a long-lived worker thread executes: it fires the transitions of one T-invariant, over and over, for the lifetime of the system.

> **Note:** boundedness, safety, deadlock-freedom and liveness are *behavioral* properties: they depend on $RS(N,M_0)$, not only on $\mathbf{C}$, and are not decidable from the invariants alone. They are covered separately in [Behavioral Properties](#behavioral-properties).

### Computing the Natural Basis (Farkas / Martínez–Silva)

Both invariant equations above ($\mathbf{C}^T \cdot X = \mathbf{0}$ and $\mathbf{C}\cdot U = \mathbf{0}$) are homogeneous linear systems $M \cdot x = \mathbf{0}$ over $M \in \{\mathbf{C}, \mathbf{C}^T\}$, and in general such a system has infinitely many non-negative solutions. The **natural basis** is the finite set of solutions with *minimal support* — no surviving solution's set of non-zero entries properly contains another's — from which every other non-negative solution can be built as a non-negative combination. It is computed by eliminating $M$'s rows (its equations) one at a time, Fourier–Motzkin style, discarding at every step any combination whose support is not minimal:

```
Algorithm 1 — Natural (Minimal-Support) Invariant Basis
Input:  an m×n matrix M                          // M = C for T-invariants, M = Cᵀ for P-invariants
Output: the natural basis B ⊆ ℕ0^n of { x ≠ 0 : M·x = 0 }

 1. tableau ← { (support: e_j, value: M[:,j]) : j = 1..n }   // one row per variable, e_j the j-th unit vector
 2. for each equation i = 1..m:                              // eliminate M's rows one at a time
 3.     zero     ← { row ∈ tableau : row.value[i] = 0 }       // already satisfies equation i: keep as is
 4.     positive ← { row ∈ tableau : row.value[i] > 0 }
 5.     negative ← { row ∈ tableau : row.value[i] < 0 }
 6.     combined ← ∅
 7.     for each p ∈ positive, n ∈ negative:
 8.         a ← −n.value[i] ;  b ← p.value[i]                 // a, b > 0
 9.         newSupport ← a·p.support + b·n.support             // a*p + b*n cancels exactly at i
10.         newValue   ← a·p.value   + b·n.value
11.         g ← gcd of the non-zero entries of (newSupport, newValue)
12.         combined ← combined ∪ { (newSupport/g, newValue/g) }
13.     candidates ← zero ∪ combined
14.     tableau ← { row ∈ candidates : no other row ∈ candidates has support
                     strictly contained in row's support }    // keep only minimal-support rows
15. B ← { row.support : row ∈ tableau }                       // every row.value is now 0
16. return B
```

At every step, a row's `support` is a candidate invariant built purely as a non-negative combination of the original unit vectors, and `value[i]` is what that candidate currently evaluates to under equation $i$ — zero for every equation already eliminated. Combining a positive/negative pair in the pivot column cancels that column exactly, dividing by the gcd keeps the result in lowest integer terms, and dropping non-minimal-support rows *after every equation* (not just at the end) is what makes the final set the natural basis rather than merely some spanning set of solutions: a row whose support strictly contains a surviving row's is always decomposable into smaller invariants and is therefore redundant.

Implementation: `algebra.Matrix.getNaturalBasis`, delegating to the private `farkas` / `initialTableau` / `eliminate` / `combine` / `minimalSupportRows` methods; `forPlaces = true` transposes $\mathbf{C}$ first (P-invariants), `forPlaces = false` uses $\mathbf{C}$ directly (T-invariants) — used by Algorithm 2, step 2, below.

## Automatic Thread Determination

This section restates, as three general algorithms over $RG(N,M_0)$ and the T-invariants of $N$, the methodology proposed in [1] for S³PR nets (Simple Sequential Process Systems with Resources) to determine, from the model alone: (a) the maximum number of threads that are ever simultaneously active, and (b) the number of threads the implementation needs and which segment of the net each one is responsible for.

The methodology assumes the modeler has classified places into:

- **Action places** $P_A \subseteq P$: places whose marking corresponds to a thread actually doing work.
- **Non-action places** $P_{\bar A} = P \setminus P_A$: idle places, shared resources, and structural/mutual-exclusion places added purely for control (e.g. to remove a deadlock), which do not represent an active worker.

Each T-invariant $U_i$ corresponds to one cyclic process in the net (one "role" a thread plays, repeated indefinitely).

### Algorithm 2 — Maximum Simultaneous Active Threads

```
Algorithm 2 — Maximum Simultaneous Active Threads
Input:  N = ⟨P,T,F,W,M0⟩, non-action places P_notA ⊆ P
Output: MaxThreads ∈ ℕ0

1. C ← Post − Pre
2. {U_1, ..., U_k} ← natural (non-negative, minimal-support) basis of {U : C·U = 0}   // Algorithm 1
3. if k = 0: return 0                         // no cyclic behavior at all
4. for each invariant U_i:
5.     supp(U_i) ← { t ∈ T : U_i[t] > 0 }
6.     PI_i ← ⋃_{t ∈ supp(U_i)} ( •t ∪ t• )    // places touched by the invariant, Eq.(4)
7.     PA_i ← PI_i \ P_notA                    // action places of the invariant, Eq.(5)
8. PA ← ⋃_i PA_i
9. if PA = ∅: return 0
10. (V, E) ← ReachabilityGraph(N)              // Algorithm 0
11. MA ← { M |_PA : M ∈ V }                    // projection of every reachable marking onto PA
12. MaxThreads ← max_{m ∈ MA} Σ_{p ∈ PA} m(p)
13. return MaxThreads
```

Implementation: `PetriNetAnalysis.calculateMaxActiveThreads`.

### Algorithm 3 — Thread Responsibility Segmentation

A T-invariant is assigned to a single execution segment only if it is *strictly linear*, i.e. it never shares a fork or a join with another invariant. Where invariants share a **fork** (a place that feeds more than one transition — a structural choice), the responsibility is split into a segment before the fork and one segment per branch, so that only the policy — never a thread — decides between the branches. Where invariants share a **join** (a place fed by more than one transition), the responsibility is split into one segment per branch up to the join and a single extra segment after it, which improves parallelism because segments before and after the join can run concurrently. This is implemented generally, as a single structural rule, rather than as three separate cases:

```
Algorithm 3 — Thread Responsibility Segmentation
Input:  N = ⟨P,T,F,W,M0⟩, non-action places P_notA ⊆ P
Output: a partition {S_1, ..., S_m} of the action places P_A = P \ P_notA

1. P_A ← P \ P_notA
2. DSU ← disjoint-set structure with one singleton set per place in P_A
3. for each transition t ∈ T:
4.     In(t)  ← •t ∩ P_A
5.     Out(t) ← t• ∩ P_A
6.     if |In(t)| = 1 and |Out(t)| = 1:
7.         u ← the single element of In(t) ;  v ← the single element of Out(t)
8.         isFork(u) ← ( |u•| > 1 )           // u feeds more than one transition
9.         isJoin(v) ← ( |•v| > 1 )           // v is fed by more than one transition
10.        if not isFork(u) and not isJoin(v):
11.            DSU.union(u, v)                // strictly linear step: same segment
                                               // otherwise u ends a segment / v starts one
12.    // if |In(t)| > 1 or |Out(t)| > 1, t is itself a join/fork transition and is
       // already a structural boundary: its inputs and outputs are never merged
13. {S_1, ..., S_m} ← the equivalence classes of DSU restricted to P_A
14. return {S_1, ..., S_m}
```

Implementation: `PetriNetAnalysis.getSegments`, using the private helpers `isPlaceFork` / `isPlaceJoin` and a union–find `DisjointSet`.

### Algorithm 4 — Maximum Threads per Segment

```
Algorithm 4 — Maximum Threads per Segment
Input:  N = ⟨P,T,F,W,M0⟩, segments {S_1, ..., S_m}     // from Algorithm 3
Output: TotalThreads ∈ ℕ0

1. (V, E) ← ReachabilityGraph(N)                        // Algorithm 0
2. for each segment S_i:
3.     MS_i ← { M |_{S_i} : M ∈ V }                     // projection onto S_i's places
4.     maxThreads_i ← max_{m ∈ MS_i} Σ_{p ∈ S_i} m(p)
5. TotalThreads ← Σ_i maxThreads_i
6. return TotalThreads
```

Implementation: `PetriNetAnalysis.calculateSegmentedMaxThreads`.

`TotalThreads` (Algorithm 4) is, in general, **greater than or equal to** `MaxThreads` (Algorithm 2): Algorithm 2 takes one global peak over the union of all action places, while Algorithm 4 sums each segment's own peak as if every segment could peak at once. The gap between the two numbers is exactly the amount of *structural* — not just numerical — parallelism the design should account for when sizing a thread pool.

### Worked example

Applying the three algorithms to a modified version of the S³PR net used as a running example in [1] (a resource-sharing net with idle places $\{P_1,P_8\}$, resources $\{P_6,P_7,P_{12},P_{13}\}$, and a constraint place $P_{14}$ added to remove a pre-existing deadlock) gives three T-invariants:

$$
IT_1=\{T_1,T_2,T_4,T_6\}, \quad IT_2=\{T_1,T_3,T_5,T_6\}, \quad IT_3=\{T_7,T_8,T_9,T_{10}\}
$$

with associated action-place sets $PA_1=\{P_2,P_3,P_5\}$, $PA_2=\{P_2,P_4,P_5\}$, $PA_3=\{P_9,P_{10},P_{11}\}$. Algorithm 2, maximizing the marking sum over $PA = PA_1 \cup PA_2 \cup PA_3$, finds **3 simultaneously active threads**. $IT_3$ never shares a transition with $IT_1$/$IT_2$, so Algorithm 3 assigns it its own segment $S_E$; $IT_1$ and $IT_2$ share a fork and a later join, so Algorithm 3 splits them into a pre-fork segment $S_A$, two branch segments $S_B$ and $S_C$, and a post-join segment $S_D$. Each of the five segments needs at most 1 thread, so Algorithm 4 returns **5 threads in total** — the number the implementation must actually allocate, even though at most 3 of them are ever running at the same instant.

## Behavioral Properties

Boundedness, safety, deadlock-freedom and liveness are properties of $RS(N,M_0)$ (equivalently, of $RG(N,M_0)$) rather than of $\mathbf{C}$ alone, so all four algorithms below start from the reachability graph built by Algorithm 0. They are implemented in `PetriNetAnalysis.analyzeProperties`, which returns a single `PropertiesReport` combining the results of Algorithms 5–8.

### Boundedness

$N$ is **$k$-bounded** iff every place holds at most $k$ tokens in every reachable marking:

$$
N \text{ is } k\text{-bounded} \iff \forall M \in RS(N,M_0),\; \forall p \in P : M(p) \le k
$$

The tightest such $k$ is simply the largest token count ever observed at any place:

```
Algorithm 5 — Boundedness
Input:  RG = (V,E), places P
Output: bound k, and maxPerPlace[p] for each p ∈ P

1. maxPerPlace[p] ← 0, for every p ∈ P
2. for each M ∈ V:
3.     for each p ∈ P:
4.         maxPerPlace[p] ← max(maxPerPlace[p], M(p))
5. k ← max_{p ∈ P} maxPerPlace[p]
6. return k, maxPerPlace
```

Implementation: the `maxPerPlace` / `bound` computation inside `PetriNetAnalysis.analyzeProperties`.

### Safety

**Safety** is the special case $k \le 1$: no place ever holds more than one token, so a place can be treated as a boolean condition.

```
Algorithm 6 — Safety
Input:  bound k                          // from Algorithm 5
Output: safe ∈ {true, false}

1. safe ← (k ≤ 1)
2. return safe
```

Implementation: `boolean safe = bound <= 1;` in `PetriNetAnalysis.analyzeProperties`.

### Deadlocks

A marking $M \in RS(N,M_0)$ is a **deadlock** iff no transition is enabled at $M$ — equivalently, $M$ has no outgoing edge in $RG(N,M_0)$:

$$
\text{Deadlocks}(N) = \{\, M \in RS(N,M_0) \;:\; \nexists\, t \in T,\; \text{enabled}(t,M) \,\}
$$

```
Algorithm 7 — Deadlock Detection
Input:  RG = (V,E)
Output: D ⊆ V, the set of deadlock markings

1. D ← { M ∈ V : there is no edge (M, t, M') ∈ E for any t, M' }
2. return D
```

Implementation: `graph.entrySet().stream().filter(e -> e.getValue().isEmpty())...` in `PetriNetAnalysis.analyzeProperties`.

### Liveness

A transition $t$ is **live** (in the strongest, L4, sense) iff, no matter which reachable marking the net is in, it is always still possible to reach a marking that enables $t$ again — $t$ can never become permanently unusable:

$$
t \text{ is live} \iff \forall M \in RS(N,M_0),\; \exists M' \in RS(N,M) : \text{enabled}(t, M')
$$

$N$ itself is **live** iff every $t \in T$ is live. Checking this only requires searching $RG(N,M_0)$ *backward* from the markings that enable $t$:

```
Algorithm 8 — Liveness
Input:  RG = (V,E), transition t
Output: isLive(t) ∈ {true, false}

 1. Enabling(t) ← { M ∈ V : enabled(t, M) }
 2. if Enabling(t) = ∅:
 3.     return false                                // dead transition (L0): never enabled
 4. E_rev ← { (M', t', M) : (M, t', M') ∈ E }        // reverse every edge
 5. Visited ← Enabling(t) ; Queue ← Enabling(t)
 6. while Queue is not empty:
 7.     M ← Queue.pop()
 8.     for each predecessor P_m with (M, ·, P_m) ∈ E_rev:   // i.e. P_m → M in RG
 9.         if P_m ∉ Visited:
10.            Visited ← Visited ∪ {P_m} ; Queue.push(P_m)
11. return ( Visited = V )                           // every marking can reach Enabling(t)

Algorithm 8′ — Net Liveness
1. for each t ∈ T: live[t] ← Algorithm 8(RG, t)
2. isLive(N) ← ⋀_{t ∈ T} live[t]
3. return isLive(N), live
```

Implementation: `PetriNetAnalysis.isTransitionLive` (steps 1–3, 11), `reverseGraph` (step 4), `backwardReachable` (steps 5–10, via BFS with an `ArrayDeque`), combined per-transition in `analyzeProperties`.

> Because Algorithm 0 caps the explored graph at `MAX_STATES` states, all four properties above are exact for nets whose full reachability graph fits under the cap, and are otherwise a report on the *explored* subgraph only — `analyzeProperties` prints a warning when the cap is hit.

## Concurrency Monitor

In concurrent programming, a **monitor** is a thread-safe construct (class, object, or module) that uses **mutual exclusion** to ensure safe access to shared methods or variables by multiple threads. Its defining property is that **only one thread may execute any of its methods at a time**.

Following [1], the monitor in this project is deliberately split into two decoupled responsibilities so that neither is entangled with thread execution itself:

- **Logic** — the Petri net $N$ (and its current marking), which determines, from the state alone, which actions are structurally possible.
- **Policy** — the mechanism that resolves conflicts between transitions that are simultaneously enabled (a *structural conflict*, e.g. at a fork), deciding which one actually fires next.

Every transition owns a **queue of blocked threads** (it acts as a condition variable), since a thread that requests a firing which the current marking does not yet enable must wait until the monitor's policy schedules that transition. The monitor's own responsibility ends at firing the transition: executing the corresponding **action** is left entirely to the worker thread, which is exactly what Algorithms 1–3 above size and assign.

📚 Learn more: [Monitor (synchronization) – Wikipedia](https://en.wikipedia.org/wiki/Monitor_(synchronization))

## References

[1] Ventre, L.O., Micolini, O. *Algoritmos para determinar cantidad y responsabilidad de hilos en sistemas embebidos modelados con Redes de Petri S³PR*. Laboratorio de Arquitectura de Computadoras, FCEFyN — Universidad Nacional de Córdoba.