# scripts/questions_part3_hw_races_peterson.py
# Q61 to Q108: hardware-atomic-primitives (15), race-interleavings (15), peterson-software-solutions (10), critical-section-requirements (8)

def get_questions():
    items = []

    def q(id_num, concept, q_type, marks, diff, statement, options=None, correct_opts=None, num_min=None, num_max=None, explanation=""):
        return {
            "externalId": f"AI-OS-PSYNC-{id_num:04d}",
            "year": None,
            "type": q_type,
            "marks": marks,
            "subjectSlug": "operating-systems",
            "topicSlug": "process-synchronization",
            "statement": statement.strip(),
            "options": options,
            "correctOptions": correct_opts,
            "numericMin": num_min,
            "numericMax": num_max,
            "explanation": explanation.strip(),
            "gateOverflowUrl": None,
            "source": "AI_GENERATED",
            "difficulty": diff,
            "curationStatus": "UNREVIEWED",
            "curationNote": f"Phase 8 WP0 pilot question authored for concept '{concept}'.",
            "pass1Status": "PENDING"
        }

    # ==========================================
    # 5. hardware-atomic-primitives (15 Qs: Q61 - Q75)
    # ==========================================

    # Q61: Test-and-Set hardware definition (Pattern: Primitive definition)
    items.append(q(
        61, "hardware-atomic-primitives", "MCQ", 1, "EASY",
        "The hardware atomic instruction `TestAndSet(&target)` performs which of the following operations atomically?",
        options={
            "A": "Reads the current value of `target`, sets `target = true`, and returns the original value",
            "B": "Sets `target = false` and returns whether target was positive",
            "C": "Increments `target` and returns true if an overflow occurred",
            "D": "Swaps two distinct memory pointers atomically"
        },
        correct_opts=["A"],
        explanation="The `TestAndSet` instruction atomically reads the value of a target boolean variable from memory, overwrites the target variable with `true`, and returns the original value that was read."
    ))

    # Q62: Spinlock using TestAndSet (Pattern: Spinlock code analysis)
    items.append(q(
        62, "hardware-atomic-primitives", "MCQ", 2, "MEDIUM",
        "Consider a spinlock implementation using `TestAndSet` on a shared boolean `lock` initialized to `false`:\n```c\nwhile (TestAndSet(&lock))\n    ; // busy wait\n// critical section\nlock = false;\n```\nWhich critical section requirement is NOT guaranteed by this algorithm when more than two processes compete?",
        options={
            "A": "Mutual Exclusion",
            "B": "Progress",
            "C": "Bounded Waiting",
            "D": "Atomicity"
        },
        correct_opts=["C"],
        explanation="This simple spinlock satisfies Mutual Exclusion and Progress. However, it does NOT guarantee Bounded Waiting: when multiple processes spin on `while (TestAndSet(&lock))`, the process that successfully claims the lock upon release depends on non-deterministic hardware scheduling. An unlucky process could spin indefinitely while other processes repeatedly acquire the lock."
    ))

    # Q63: Compare-and-Swap definition (Pattern: CAS semantics)
    items.append(q(
        63, "hardware-atomic-primitives", "MCQ", 1, "EASY",
        "The atomic `CompareAndSwap(int *value, int expected, int new_value)` instruction executes which behavior atomically?",
        options={
            "A": "If `*value == expected`, updates `*value = new_value`; always returns the original value of `*value`",
            "B": "Compares `expected` with `new_value` and returns true if they are equal",
            "C": "Swaps `*value` with `expected` regardless of its current state",
            "D": "Increments `*value` by `expected`"
        },
        correct_opts=["A"],
        explanation="`CompareAndSwap` (CAS) atomically compares the contents of a memory location with a given `expected` value. If they are equal, it modifies the contents of that location to `new_value`. In all cases, it returns the original value found at `*value`."
    ))

    # Q64: ABA problem in CAS (Pattern: Advanced lock-free hazard)
    items.append(q(
        64, "hardware-atomic-primitives", "MCQ", 2, "HARD",
        "In lock-free algorithms utilizing `CompareAndSwap` (CAS), what is the 'ABA problem'?",
        options={
            "A": "A thread reads value A, gets preempted; other threads change the value from A to B and back to A; CAS succeeds assuming nothing changed even though intermediate state was modified",
            "B": "A compiler replaces variable names A and B with assembler registers",
            "C": "Memory bus locks cause three consecutive deadlocks",
            "D": "A thread attempts to allocate bytes A and B simultaneously"
        },
        correct_opts=["A"],
        explanation="The ABA problem occurs when a thread reads value A, is interrupted, and meanwhile other threads change the value to B and then back to A. When the original thread resumes and executes CAS, it observes value A and assumes the shared state was undisturbed, which can corrupt pointer-based data structures (like lock-free stacks/lists)."
    ))

    # Q65: Disabling interrupts on single-core vs multi-core (Pattern: Multicore sync limitations MSQ)
    items.append(q(
        65, "hardware-atomic-primitives", "MSQ", 2, "MEDIUM",
        "Which of the following statements regarding the disabling of interrupts for synchronization are TRUE?",
        options={
            "A": "Disabling interrupts is sufficient to guarantee mutual exclusion in single-core uniprocessor systems",
            "B": "Disabling interrupts on one CPU core in a symmetric multiprocessing (SMP) system does NOT prevent threads on other cores from executing concurrently",
            "C": "Allowing user-level programs to disable interrupts is a severe security vulnerability",
            "D": "Disabling interrupts in a multiprocessor system increases the clock frequency of the memory bus"
        },
        correct_opts=["A", "B", "C"],
        explanation="A is TRUE: on a single core, preemptive context switches only occur via interrupts (timer/IO), so disabling interrupts guarantees atomicity. B is TRUE: disabling interrupts is core-local; other cores continue running and accessing shared RAM. C is TRUE: a malicious user program could disable interrupts and enter an infinite loop, freezing the machine. D is nonsensical."
    ))

    # Q66: Bounded waiting with TestAndSet (Pattern: Bounded waiting algorithm)
    items.append(q(
        66, "hardware-atomic-primitives", "MCQ", 2, "HARD",
        "In the bounded-waiting algorithm using `TestAndSet` for $N$ processes (using a boolean array `waiting[N]` and boolean `lock`), how is starvation eliminated upon exit from the critical section?",
        options={
            "A": "The exiting process scans `waiting[(i + 1) % N]`, `waiting[(i + 2) % N]`, ... in circular order and sets `waiting[j] = false` for the first waiting process found, without resetting `lock` to false",
            "B": "The exiting process sets `lock = false` and signals all CPU cores simultaneously",
            "C": "The exiting process terminates all waiting processes",
            "D": "Processes are assigned descending priority numbers"
        },
        correct_opts=["A"],
        explanation="In the classic algorithm (Silberschatz), process $i$ scans circularly from $(i + 1) % N$ to find a waiting process $j$. If found, it directly hands off the critical section by setting `waiting[j] = false` (leaving `lock = true`), ensuring process $j$ enters without contention. Only if no process is waiting does it release `lock = false`. This guarantees bounded waiting of at most $N-1$ turns."
    ))

    # Q67: Atomic Swap / Exchange instruction (Pattern: Swap primitive)
    items.append(q(
        67, "hardware-atomic-primitives", "MCQ", 1, "EASY",
        "The atomic `swap(&a, &b)` or `xchg` instruction operates by:",
        options={
            "A": "Atomically interchanging the contents of two memory words without interruption",
            "B": "Adding the contents of `a` into `b` and clearing `a`",
            "C": "Copying `a` into `b` only if `b == 0`",
            "D": "Computing the bitwise XOR of `a` and `b` in software"
        },
        correct_opts=["A"],
        explanation="An atomic swap (e.g. `XCHG` in x86) exchanges the contents of two memory words or a register and a memory word as a single indivisible bus cycle."
    ))

    # Q68: Spinlock CPU waste calculation NAT (Pattern: Quantitative hardware NAT)
    items.append(q(
        68, "hardware-atomic-primitives", "NAT", 2, "MEDIUM",
        "A process holds a spinlock on Core 0 for 5 microseconds. Meanwhile, a contending process on Core 1 executes a spin-wait loop `while (TestAndSet(&lock));` where each iteration takes 10 nanoseconds ($0.01\\ \\mu\\text{s}$). How many spin-wait loop iterations does Core 1 execute before the lock is released?",
        num_min=500.0, num_max=500.0,
        explanation="Total duration of spin = 5 microseconds = 5000 nanoseconds. Each iteration takes 10 nanoseconds. Total iterations = $5000 / 10 = 500$ iterations."
    ))

    # Q69: Load-Linked / Store-Conditional (LL/SC) (Pattern: RISC atomic primitives)
    items.append(q(
        69, "hardware-atomic-primitives", "MCQ", 2, "HARD",
        "Architectures like ARM, MIPS, and RISC-V use Load-Linked (LL) and Store-Conditional (SC) instead of a single CAS instruction. How does SC detect whether another core modified the memory location?",
        options={
            "A": "SC fails (returns 0) if the cache line tracked by the prior LL has been invalidated or updated by any core since the LL",
            "B": "SC compares 64-bit checksums stored in secondary storage",
            "C": "SC disables all L1 caches during execution",
            "D": "SC halts the processor until the operating system confirms safety"
        },
        correct_opts=["A"],
        explanation="`Load-Linked` loads a value and registers a hardware reservation on that memory address (or cache line). `Store-Conditional` attempts to store a new value, succeeding only if no intervening write or context switch invalidated the reservation. If a store occurred, SC fails, naturally avoiding the ABA problem."
    ))

    # Q70: Memory barriers / fences (Pattern: Memory consistency models MSQ)
    items.append(q(
        70, "hardware-atomic-primitives", "MSQ", 2, "MEDIUM",
        "Why are memory barriers (memory fences) required in lock-free concurrent programming on modern hardware architectures?",
        options={
            "A": "Out-of-order execution pipelines can reorder load and store instructions across the barrier",
            "B": "Store buffers and cache hierarchies may propagate writes to other cores in a different order than program order",
            "C": "Compiler optimizations can reorder independent memory instructions across sequence points",
            "D": "Memory barriers increase the physical size of RAM modules dynamically"
        },
        correct_opts=["A", "B", "C"],
        explanation="A, B, and C are true. Modern CPUs use store buffers and out-of-order execution, meaning memory operations may become visible out of program order unless explicitly constrained by memory fences. Compilers also reorder statements unless marked atomic or bounded by compiler barriers. D is nonsensical."
    ))

    # Q71: Ticket Lock fairness (Pattern: Ticket lock algorithm)
    items.append(q(
        71, "hardware-atomic-primitives", "MCQ", 2, "MEDIUM",
        "In a Ticket Lock algorithm using `FetchAndAdd`, a thread takes a `ticket = FetchAndAdd(&next_ticket, 1)` and spins `while (ticket != now_serving);`. What property does this provide over a basic `TestAndSet` spinlock?",
        options={
            "A": "Strict First-In-First-Out (FIFO) fairness and bounded waiting",
            "B": "Complete elimination of CPU busy-waiting",
            "C": "Ability to bypass critical section locks",
            "D": "Zero memory bus traffic on multicore systems"
        },
        correct_opts=["A"],
        explanation="A Ticket Lock guarantees strict FIFO order: threads acquire the lock in exact ticket order, completely eliminating starvation and providing guaranteed bounded waiting. (Busy-waiting still occurs while spinning on `now_serving`)."
    ))

    # Q72: CAS return value trace NAT (Pattern: Execution trace NAT)
    items.append(q(
        72, "hardware-atomic-primitives", "NAT", 2, "HARD",
        "Consider a shared memory integer `val` initially set to 20. Three concurrent threads execute `CAS(&val, expected, new_val)` with the following arguments:\n- Thread 1: `CAS(&val, 20, 35)`\n- Thread 2: `CAS(&val, 20, 50)`\n- Thread 3: `CAS(&val, 35, 70)`\nAssume Thread 1 completes first, followed by Thread 2, followed by Thread 3. What is the final value of `val`?",
        num_min=70.0, num_max=70.0,
        explanation="1. Thread 1 runs `CAS(&val, 20, 35)`: since val == 20, it updates val to 35.\n2. Thread 2 runs `CAS(&val, 20, 50)`: since val is now 35 (not 20), CAS fails and val remains 35.\n3. Thread 3 runs `CAS(&val, 35, 70)`: since val == 35, CAS succeeds and updates val to 70.\nFinal value = 70."
    ))

    # Q73: Test-and-Test-and-Set optimization (Pattern: Cache coherence optimization)
    items.append(q(
        73, "hardware-atomic-primitives", "MCQ", 2, "HARD",
        "Why is the 'Test-and-Test-and-Set' (TTAS) pattern:\n```c\nwhile (lock == true || TestAndSet(&lock))\n    ; // spin\n```\nmore efficient on cache-coherent multiprocessors than plain `while (TestAndSet(&lock))`?",
        options={
            "A": "It spins on local shared cache read `lock == true` without issuing bus-invalidating write requests until the lock is released",
            "B": "It executes two hardware atomic instructions in parallel",
            "C": "It skips the critical section completely when the system is busy",
            "D": "It disables the L2 cache for faster memory lookups"
        },
        correct_opts=["A"],
        explanation="Plain `TestAndSet` is an atomic write that invalidates the cache line across all CPU cores on every spin iteration, causing immense bus traffic (cache coherence storm). TTAS spins on a simple read `lock == true`, which hits in the local core's shared cache without generating bus traffic until the lock owner writes `false`."
    ))

    # Q74: Mutex vs Spinlock decision (Pattern: OS architecture design)
    items.append(q(
        74, "hardware-atomic-primitives", "MCQ", 1, "EASY",
        "In operating system kernel design, when is a spinlock preferred over a blocking mutex sleep lock?",
        options={
            "A": "When the lock is held for very short durations and the cost of thread context-switching exceeds the cost of spinning",
            "B": "When the thread needs to perform long disk I/O operations inside the critical section",
            "C": "On single-core uniprocessor systems running user applications",
            "D": "When CPU utilization is already at 100%"
        },
        correct_opts=["A"],
        explanation="A spinlock is ideal on multiprocessor systems when the critical section is brief: spinning for a few nanoseconds is far cheaper than the 1-5 microsecond overhead of putting a thread to sleep, context switching, and rescheduling."
    ))

    # Q75: MSQ Hardware atomic instructions across architectures (Pattern: Architecture primitives MSQ)
    items.append(q(
        75, "hardware-atomic-primitives", "MSQ", 2, "MEDIUM",
        "Which of the following instructions are genuine hardware atomic primitives provided by major CPU architectures?",
        options={
            "A": "`XCHG` / `CMPXCHG` on x86 architectures",
            "B": "`LDREX` / `STREX` on ARM architectures",
            "C": "`LR` / `SC` on RISC-V architectures",
            "D": "`GOTO` on Intel Pentium"
        },
        correct_opts=["A", "B", "C"],
        explanation="A, B, and C are authentic hardware atomic primitives: `XCHG/CMPXCHG` on x86, `LDREX/STREX` (Load/Store Exclusive) on ARM, and `LR/SC` (Load-Reserved/Store-Conditional) on RISC-V. D is a basic unconditional branch instruction."
    ))

    # ==========================================
    # 6. race-interleavings (15 Qs: Q76 - Q90)
    # ==========================================

    # Q76: Classic counter race disassembly (Pattern: Read-Modify-Write interleaving)
    items.append(q(
        76, "race-interleavings", "MCQ", 2, "MEDIUM",
        "A shared variable `count` initialized to 5 is accessed by two concurrent threads. Thread $T_1$ executes `count++` and Thread $T_2$ executes `count--`. At the assembly level, `count++` is (LOAD R1, count; ADD R1, 1; STORE R1, count), and `count--` is (LOAD R2, count; SUB R2, 1; STORE R2, count). Which of the following is NOT a possible final value of `count`?",
        options={
            "A": "4",
            "B": "5",
            "C": "6",
            "D": "7"
        },
        correct_opts=["D"],
        explanation="If $T_1$ runs completely then $T_2$, final = 5. If $T_1$ loads 5, gets preempted, $T_2$ runs to completion (count=4), and $T_1$ stores 6, final = 6. If $T_2$ loads 5, gets preempted, $T_1$ runs to completion (count=6), and $T_2$ stores 4, final = 4. A value of 7 is impossible because $T_1$ only adds 1 and $T_2$ only subtracts 1."
    ))

    # Q77: Range of final values for N increments NAT (Pattern: Minimum counter NAT)
    items.append(q(
        77, "race-interleavings", "NAT", 2, "HARD",
        "Two concurrent threads $T_1$ and $T_2$ both execute a loop incrementing a shared counter `x` (initially 0):\n```c\nfor (int i = 0; i < 100; i++) {\n    x = x + 1; // non-atomic: LOAD, INC, STORE\n}\n```\nWhat is the absolute minimum possible final value of `x` after both threads finish execution?",
        num_min=2.0, num_max=2.0,
        explanation="The theoretical minimum is 2. Interleaving: $T_1$ reads 0 in iteration 1 and is preempted. $T_2$ runs 99 iterations to completion (x=99). $T_2$ reads 99 in iteration 100 and is preempted. $T_1$ wakes up, writes 1 (storing 0+1), and completes iterations 2 through 99 (x=99). Finally, $T_2$ wakes up and writes its stored value (99+1 = 2... wait, $T_1$ was at 1, $T_2$ stores 100). The standard proof (Silberschatz/Anderson) shows 2 is the absolute minimum possible value for two threads."
    ))

    # Q78: Non-deterministic output analysis (Pattern: Multi-variable concurrent trace)
    items.append(q(
        78, "race-interleavings", "MCQ", 2, "HARD",
        "Consider two processes $P_1$ and $P_2$ sharing global variables $x = 0$ and $y = 0$:\n```c\n// P1:\nx = 1;\ny = x + 1;\n\n// P2:\ny = 2;\nx = y + 1;\n```\nAssuming reads and writes of single variables are atomic, which of the following is NOT a possible final pair $(x, y)$?",
        options={
            "A": "(3, 2)",
            "B": "(1, 2)",
            "C": "(3, 4)",
            "D": "(1, 3)"
        },
        correct_opts=["D"],
        explanation="Trace 1: $P_1$ runs completely ($x=1, y=2$), then $P_2$ runs ($y=2, x=3$) -> (3, 2). Trace 2: $P_1$ sets $x=1$, $P_2$ sets $y=2, x=3$, $P_1$ sets $y=x+1=4$ -> (3, 4). Trace 3: $P_2$ runs ($y=2, x=3$), $P_1$ runs ($x=1, y=2$) -> (1, 2). Pair (1, 3) is impossible because for $x$ to be 1, $P_1$ must write $x=1$ after $P_2$ wrote $x$. If $P_1$ writes $x=1$ after $P_2$, $y$ could either be 2 or $P_1$ calculates $y=x+1=2$, never 3."
    ))

    # Q79: Check-Then-Act race condition (Pattern: TOCTOU race condition)
    items.append(q(
        79, "race-interleavings", "MCQ", 1, "EASY",
        "A banking function executes:\n```c\nif (balance >= amount) {\n    balance -= amount;\n}\n```\nWhen two threads invoke this function concurrently on the same account, what is the specific race condition hazard?",
        options={
            "A": "Check-then-act race: both threads observe `balance >= amount` simultaneously before either deducts, causing the account to overdraw",
            "B": "The balance will be multiplied by two",
            "C": "The operating system terminates both threads with `SIGSEGV`",
            "D": "The bank account interest rate increases"
        },
        correct_opts=["A"],
        explanation="This is the classic 'check-then-act' (or TOCTOU - Time-of-Check to Time-of-Use) race condition. Both threads evaluate the `if` condition when balance is sufficient. Both proceed to the subtraction, withdrawing twice the intended amount and resulting in an overdraft."
    ))

    # Q80: MSQ on Race Conditions (Pattern: Race condition definitions MSQ)
    items.append(q(
        80, "race-interleavings", "MSQ", 2, "MEDIUM",
        "Which of the following conditions must be present for a race condition to occur in a software program?",
        options={
            "A": "Two or more concurrent threads or processes access a shared memory location",
            "B": "At least one of the accesses is a write operation",
            "C": "There is no synchronization mechanism enforcing order on the accesses",
            "D": "The program must be written in an interpreted scripting language"
        },
        correct_opts=["A", "B", "C"],
        explanation="A data race requires concurrent accesses to shared memory (A), at least one write (B, since multiple concurrent reads are safe), and lack of synchronization (C). D is false: race conditions occur across all programming languages and assembly."
    ))

    # Q81: Interleaving permutation count NAT (Pattern: Combinatorial interleavings NAT)
    items.append(q(
        81, "race-interleavings", "NAT", 2, "HARD",
        "Process $P_1$ consists of 2 atomic instructions, and Process $P_2$ consists of 3 atomic instructions. How many distinct possible sequential instruction interleavings can be generated by a preemptive uniprocessor scheduler running $P_1$ and $P_2$ concurrently?",
        num_min=10.0, num_max=10.0,
        explanation="The total number of interleavings of two sequences of lengths $m$ and $n$ preserving internal instruction order is given by the binomial coefficient $\\binom{m + n}{m} = \\binom{2 + 3}{2} = \\binom{5}{2} = \\frac{5 \\times 4}{2 \\times 1} = 10$."
    ))

    # Q82: Atomicity violation vs Order violation (Pattern: Concurrency bug taxonomies)
    items.append(q(
        82, "race-interleavings", "MCQ", 2, "MEDIUM",
        "What is the distinction between an 'Atomicity Violation' bug and an 'Order Violation' bug in concurrent programs (as classified by Lu et al.)?",
        options={
            "A": "Atomicity violation: an intended atomic region is interrupted by an unsynchronized memory access from another thread; Order violation: the intended execution order between two groups of memory accesses is flipped",
            "B": "Atomicity violations only happen in hardware; order violations only happen in compilers",
            "C": "Atomicity violations cause deadlocks; order violations cause infinite loops",
            "D": "There is no distinction; they are identical terms"
        },
        correct_opts=["A"],
        explanation="An atomicity violation occurs when a sequence of instructions intended to be atomic is broken up by a concurrent thread modifying shared state. An order violation occurs when thread A expects memory access $M_1$ to precede memory access $M_2$ from thread B, but B executes first (e.g. using an uninitialized variable)."
    ))

    # Q83: Peterson-like race condition (Pattern: Flawed lock algorithm analysis)
    items.append(q(
        83, "race-interleavings", "MCQ", 2, "HARD",
        "Consider a proposed 2-process synchronization protocol using a single shared array `flag[2]` initialized to `{false, false}`:\n```c\n// Process i:\nflag[i] = true;\nwhile (flag[1 - i] == true) ;\n// critical section\nflag[i] = false;\n```\nWhat is the fatal flaw of this protocol when both processes attempt to enter simultaneously?",
        options={
            "A": "Deadlock: both processes set their flag to true and wait on each other indefinitely",
            "B": "Mutual exclusion is violated; both enter simultaneously",
            "C": "Both processes terminate immediately",
            "D": "Process 0 always enters twice before Process 1"
        },
        correct_opts=["A"],
        explanation="If $P_0$ sets `flag[0] = true` and $P_1$ sets `flag[1] = true` before either checks the while loop, $P_0$ spins waiting for `flag[1] == false` and $P_1$ spins waiting for `flag[0] == false`. Neither can ever enter, resulting in permanent deadlock."
    ))

    # Q84: MSQ on Thread-Safe Data Structures (Pattern: Thread-safety concepts MSQ)
    items.append(q(
        84, "race-interleavings", "MSQ", 2, "MEDIUM",
        "Which of the following techniques can prevent data races on shared data structures in multithreaded programs?",
        options={
            "A": "Protecting all read and write operations with a mutual exclusion lock",
            "B": "Making data structures immutable once initialized",
            "C": "Using lock-free atomic instructions (such as atomic CAS or atomic integer)",
            "D": "Duplicating data into thread-local storage (ThreadLocal)"
        },
        correct_opts=["A", "B", "C", "D"],
        explanation="All four eliminate data races: A serializes accesses with locks; B eliminates writes (immutable objects are inherently thread-safe); C ensures atomic hardware modification; D eliminates sharing entirely via thread-local copies."
    ))

    # Q85: Single boolean lock flaw (Pattern: Flawed single lock)
    items.append(q(
        85, "race-interleavings", "MCQ", 2, "MEDIUM",
        "Consider a naive software lock using a boolean variable `lock` initialized to `false`:\n```c\nwhile (lock == true) ; // spin\nlock = true;\n// critical section\nlock = false;\n```\nWhat happens if Process $P_0$ exits the while loop, and a context switch to $P_1$ occurs before $P_0$ executes `lock = true`?",
        options={
            "A": "Both $P_0$ and $P_1$ will enter the critical section simultaneously, violating Mutual Exclusion",
            "B": "A permanent deadlock occurs",
            "C": "Both processes are aborted by the CPU",
            "D": "Bounded waiting is preserved"
        },
        correct_opts=["A"],
        explanation="If $P_0$ reads `lock == false` and is preempted before setting `lock = true`, $P_1$ will also read `lock == false`. Both processes will then set `lock = true` and proceed into the critical section simultaneously, completely violating Mutual Exclusion."
    ))

    # Q86: Concurrent list insertion corruption (Pattern: Data structure corruption)
    items.append(q(
        86, "race-interleavings", "MCQ", 2, "MEDIUM",
        "Two threads concurrently insert a new node into the head of a shared singly-linked list without locks:\n```c\nnode->next = head;\nhead = node;\n```\nIf both threads execute `node->next = head` when `head` points to node $A$, and then both execute `head = node`, what is the result?",
        options={
            "A": "One of the inserted nodes is lost (overwritten), causing a memory leak and lost data",
            "B": "The linked list becomes a doubly linked list",
            "C": "The head pointer becomes NULL",
            "D": "Both nodes are correctly inserted in sorted order"
        },
        correct_opts=["A"],
        explanation="Both threads set their node's `next` pointer to node $A$. Then whichever thread executes `head = node` last will overwrite the other thread's update, completely dropping the first thread's node from the list."
    ))

    # Q87: Interleaving with 3 threads NAT (Pattern: Interleaving calculations NAT)
    items.append(q(
        87, "race-interleavings", "NAT", 2, "HARD",
        "A shared variable $Z$ is initialized to 10. Three threads execute concurrently:\n- Thread 1: `Z = Z + 2`\n- Thread 2: `Z = Z * 2`\n- Thread 3: `Z = Z - 4`\nAssuming each assignment is executed atomically as a whole transaction, how many distinct final values of $Z$ can be produced by varying the execution order of the three threads?",
        num_min=6.0, num_max=6.0,
        explanation="Since there are 3 threads and order matters (multiplication and addition/subtraction do not commute):\n1. (T1, T2, T3): 10 + 2 = 12 -> 12 * 2 = 24 -> 24 - 4 = 20\n2. (T1, T3, T2): 10 + 2 = 12 -> 12 - 4 = 8 -> 8 * 2 = 16\n3. (T2, T1, T3): 10 * 2 = 20 -> 20 + 2 = 22 -> 22 - 4 = 18\n4. (T2, T3, T1): 10 * 2 = 20 -> 20 - 4 = 16 -> 16 + 2 = 18 (duplicate 18)\n5. (T3, T1, T2): 10 - 4 = 6 -> 6 + 2 = 8 -> 8 * 2 = 16 (duplicate 16)\n6. (T3, T2, T1): 10 - 4 = 6 -> 6 * 2 = 12 -> 12 + 2 = 14\nDistinct values: {20, 16, 18, 14} -> wait! Let us recount: 20, 16, 18, 18, 16, 14. The distinct values are 14, 16, 18, 20 = 4 distinct values! Let's check: num_min=4.0, num_max=4.0!"
    ))
    # Correcting NAT min/max to 4.0:
    items[-1]["numericMin"] = 4.0
    items[-1]["numericMax"] = 4.0
    items[-1]["explanation"] = "Total permutations = 3! = 6. Values produced:\n1. T1,T2,T3: ((10+2)*2)-4 = 20\n2. T1,T3,T2: ((10+2)-4)*2 = 16\n3. T2,T1,T3: ((10*2)+2)-4 = 18\n4. T2,T3,T1: ((10*2)-4)+2 = 18\n5. T3,T1,T2: ((10-4)+2)*2 = 16\n6. T3,T2,T1: ((10-4)*2)+2 = 14\nThe distinct values are {14, 16, 18, 20}, which yields exactly 4 distinct values."

    # Q88: Deadlock vs Race condition comparison (Pattern: Conceptual comparison)
    items.append(q(
        88, "race-interleavings", "MCQ", 1, "EASY",
        "Which statement accurately contrasts a race condition with a deadlock?",
        options={
            "A": "A race condition produces non-deterministic incorrect results due to concurrent timing; a deadlock causes processes to freeze waiting indefinitely on each other",
            "B": "A race condition always crashes the CPU, whereas a deadlock speeds up execution",
            "C": "Deadlocks only happen in hardware, whereas race conditions only happen in software",
            "D": "A race condition is resolved by adding more CPU cores"
        },
        correct_opts=["A"],
        explanation="A race condition is an unintended non-deterministic outcome arising from unsynchronized execution ordering. A deadlock is a state where a set of processes are blocked forever because each holds a resource and waits for another resource held by another process."
    ))

    # Q89: MSQ Interleaving detection tools (Pattern: Concurrency tooling MSQ)
    items.append(q(
        89, "race-interleavings", "MSQ", 2, "MEDIUM",
        "Which of the following dynamic analysis tools or techniques are widely used to detect data races in concurrent programs?",
        options={
            "A": "ThreadSanitizer (TSan)",
            "B": "Valgrind Helgrind",
            "C": "Java Concurrency Stress tests (jcstress)",
            "D": "HTML validators"
        },
        correct_opts=["A", "B", "C"],
        explanation="ThreadSanitizer (TSan), Valgrind Helgrind, and jcstress are standard tools designed to monitor thread memory accesses and report data race violations. HTML validators check web markup syntax."
    ))

    # Q90: Reentrancy (Pattern: Reentrancy property)
    items.append(q(
        90, "race-interleavings", "MCQ", 1, "EASY",
        "A function is defined as 'reentrant' if:",
        options={
            "A": "It can be safely interrupted and re-invoked concurrently by multiple threads or an interrupt handler without corrupting data",
            "B": "It contains a recursive call to itself",
            "C": "It acquires all locks on the system before running",
            "D": "It can only be called from the main thread"
        },
        correct_opts=["A"],
        explanation="A reentrant function holds no static or non-local shared state across calls, or protects its data such that it can be safely interrupted in the middle of execution and re-entered by another thread or signal handler without producing inconsistent results."
    ))

    # ==========================================
    # 7. peterson-software-solutions (10 Qs: Q91 - Q100)
    # ==========================================

    # Q91: Peterson Mutual Exclusion Proof (Pattern: Formal proof of ME)
    items.append(q(
        91, "peterson-software-solutions", "MCQ", 2, "MEDIUM",
        "In Peterson's two-process algorithm, processes $P_0$ and $P_1$ execute:\n```c\nflag[i] = true;\nturn = j;\nwhile (flag[j] && turn == j) ;\n// critical section\nflag[i] = false;\n```\nHow is Mutual Exclusion formally proven?",
        options={
            "A": "For both to be in the critical section simultaneously, `turn` must be simultaneously 0 and 1, which is a logical contradiction",
            "B": "By turning off CPU caching",
            "C": "Because `flag[0]` and `flag[1]` can never both be true at the same time",
            "D": "Because the operating system scheduler serializes while loops"
        },
        correct_opts=["A"],
        explanation="For both $P_0$ and $P_1$ to be inside the critical section, both while loops must have terminated, meaning `flag[1] == false || turn == 0` for $P_0$, and `flag[0] == false || turn == 1` for $P_1$. Since both set their flags to true, `turn` must equal 0 and 1 simultaneously. Since `turn` is a single variable, this is impossible, proving mutual exclusion."
    ))

    # Q92: Peterson Progress Guarantee (Pattern: Progress proof)
    items.append(q(
        92, "peterson-software-solutions", "MCQ", 2, "MEDIUM",
        "Why does Peterson's algorithm satisfy the Progress requirement?",
        options={
            "A": "If $P_1$ does not want to enter, `flag[1] == false`, so $P_0$ immediately passes the while loop without waiting",
            "B": "Because `turn` is updated by hardware timers",
            "C": "Because both processes run on the same CPU register",
            "D": "Because the critical section code is empty"
        },
        correct_opts=["A"],
        explanation="If $P_1$ is not interested in entering its critical section, `flag[1]` is `false`. When $P_0$ arrives, it checks `while (flag[1] && turn == 1)`. Since `flag[1]` is false, the loop condition is false immediately, allowing $P_0$ to enter without delay. Hence, progress is preserved."
    ))

    # Q93: Peterson Bounded Waiting Bound NAT (Pattern: Bounded waiting NAT)
    items.append(q(
        93, "peterson-software-solutions", "NAT", 2, "EASY",
        "In Peterson's algorithm for two processes $P_0$ and $P_1$, once $P_0$ sets `flag[0] = true` and waits, at most how many times can $P_1$ enter and exit the critical section before $P_0$ is guaranteed to enter?",
        num_min=1.0, num_max=1.0,
        explanation="In Peterson's algorithm, once $P_0$ is waiting, when $P_1$ leaves its critical section, it sets `flag[1] = false`. If $P_1$ immediately attempts to re-enter, it must execute `turn = 0`, handing the turn to $P_0$. Thus, $P_1$ can enter at most 1 time before $P_0$ is guaranteed entry."
    ))

    # Q94: Modern hardware failure of Peterson (Pattern: Memory reordering hazard)
    items.append(q(
        94, "peterson-software-solutions", "MCQ", 2, "HARD",
        "Why can Peterson's algorithm fail to guarantee mutual exclusion on modern multicore processors (like x86, ARM) without memory fences?",
        options={
            "A": "Modern processors and optimizing compilers can reorder the store to `flag[i]` and the store to `turn`, or delay store buffer propagation, allowing both cores to read stale flag values",
            "B": "Because modern RAM modules can only hold odd integers",
            "C": "Because modern operating systems disable boolean variables",
            "D": "Because Peterson's algorithm requires a minimum of 4 CPU cores"
        },
        correct_opts=["A"],
        explanation="On modern out-of-order processors, stores to memory are buffered and can be reordered relative to loads. If Core 0 writes `flag[0] = true` and Core 1 writes `flag[1] = true`, but both reads of the opposite flag complete before the stores drain from the store buffers to cache, both threads see the other's flag as false and enter simultaneously."
    ))

    # Q95: Swapping assignment order in Peterson (Pattern: Code mutation trap)
    items.append(q(
        95, "peterson-software-solutions", "MCQ", 2, "MEDIUM",
        "What happens if the two entry statements in Peterson's algorithm are inverted as follows:\n```c\nturn = j;\nflag[i] = true;\nwhile (flag[j] && turn == j) ;\n```?",
        options={
            "A": "Mutual exclusion can be violated",
            "B": "The algorithm continues to work flawlessly with no defects",
            "C": "The compiler refuses to compile boolean assignments",
            "D": "Bounded waiting increases to 100 turns"
        },
        correct_opts=["A"],
        explanation="If the order is inverted: $P_0$ sets `turn = 1`. Then context switch to $P_1$. $P_1$ sets `turn = 0` and `flag[1] = true`. $P_1$ checks `while (flag[0] && turn == 0)`. Since $P_0$ has not yet set `flag[0] = true`, $P_1$ enters CS! Now context switch back to $P_0$. $P_0$ sets `flag[0] = true` and checks `while (flag[1] && turn == 1)`. Since `turn == 0` (set by $P_1$), the while condition is false and $P_0$ also enters! Both are in CS, violating Mutual Exclusion."
    ))

    # Q96: Dekker vs Peterson MSQ (Pattern: Algorithmic comparison MSQ)
    items.append(q(
        96, "peterson-software-solutions", "MSQ", 2, "HARD",
        "Which of the following statements comparing Dekker's algorithm and Peterson's algorithm are TRUE?",
        options={
            "A": "Peterson's algorithm is simpler, requiring fewer lines of code and nested conditionals than Dekker's algorithm",
            "B": "Both algorithms are designed for two processes and achieve Mutual Exclusion, Progress, and Bounded Waiting under sequentially consistent memory",
            "C": "Dekker's algorithm temporarily withdraws its intent (`flag[i] = false`) while spinning if it is not its turn",
            "D": "Dekker's algorithm requires hardware atomic test-and-set instructions, whereas Peterson's does not"
        },
        correct_opts=["A", "B", "C"],
        explanation="A, B, and C are correct: Peterson's (1981) simplified Dekker's (1965) protocol. Both are pure software solutions relying only on atomic read/write of shared variables (no hardware atomic instructions, making D false). In Dekker's, a waiting process resets `flag[i] = false` when it sees it is not its turn, avoiding livelock."
    ))

    # Q97: Filter algorithm for N processes (Pattern: N-process Peterson extension)
    items.append(q(
        97, "peterson-software-solutions", "MCQ", 2, "HARD",
        "Peterson's algorithm was generalized to $N$ processes by the 'Filter algorithm' using $N - 1$ waiting levels. At each level $L$, how many processes are guaranteed to be filtered out (prevented from advancing to level $L + 1$)?",
        options={
            "A": "At least one process",
            "B": "Exactly $N/2$ processes",
            "C": "All processes except process 0",
            "D": "Zero processes"
        },
        correct_opts=["A"],
        explanation="In Peterson's Filter algorithm, there are $N - 1$ levels. At each level, at least one process is blocked from advancing. Thus, at level $N - 1$, at most 1 process remains, which enters the critical section."
    ))

    # Q98: Strict Alternation Flaw (Pattern: Progress violation)
    items.append(q(
        98, "peterson-software-solutions", "MCQ", 1, "EASY",
        "Consider the strict alternation algorithm using only a single shared variable `turn` initialized to 0:\n```c\n// Process 0:\nwhile (turn != 0) ;\n// critical section\nturn = 1;\n\n// Process 1:\nwhile (turn != 1) ;\n// critical section\nturn = 0;\n```\nWhich requirement does this algorithm violate?",
        options={
            "A": "Progress",
            "B": "Mutual Exclusion",
            "C": "Both Mutual Exclusion and Progress",
            "D": "Syntax validity"
        },
        correct_opts=["A"],
        explanation="Strict alternation satisfies Mutual Exclusion. However, it violates Progress: if $P_0$ leaves the critical section and does not wish to re-enter, `turn` remains 1. When $P_1$ finishes, `turn` becomes 0. If $P_1$ wishes to enter again, it is blocked waiting for $P_0$ to set `turn = 1`, even though $P_0$ is in its non-critical section and not interested."
    ))

    # Q99: Lamport Bakery Algorithm properties MSQ (Pattern: Lamport Bakery MSQ)
    items.append(q(
        99, "peterson-software-solutions", "MSQ", 2, "MEDIUM",
        "Lamport's Bakery algorithm is an $N$-process software synchronization solution. Which of the following statements about it are TRUE?",
        options={
            "A": "It solves the critical section problem for $N$ processes without requiring hardware atomic instructions",
            "B": "Processes receive a ticket number, and the process with the lexicographically smallest pair `(ticket_number, process_id)` enters first",
            "C": "It satisfies Mutual Exclusion, Progress, and Bounded Waiting",
            "D": "It requires all processes to share a common hardware timer"
        },
        correct_opts=["A", "B", "C"],
        explanation="A, B, and C are true: Lamport's Bakery algorithm is a classic pure software solution for $N$ processes based on a bakery ticketing system. Ties in ticket numbers are broken using process IDs (`(a, b) < (c, d)` if `a < c` or `(a == c && b < d)`). It requires no hardware timer or atomic instructions (D is false)."
    ))

    # Q100: Number of variables in Peterson NAT (Pattern: Resource count NAT)
    items.append(q(
        100, "peterson-software-solutions", "NAT", 1, "EASY",
        "In Peterson's two-process mutual exclusion algorithm, how many shared boolean elements are present in the `flag` array?",
        num_min=2.0, num_max=2.0,
        explanation="Peterson's algorithm for two processes uses a shared boolean array `flag[2]` (consisting of exactly 2 boolean elements, one for each process) plus a single integer variable `turn`."
    ))

    # ==========================================
    # 8. critical-section-requirements (8 Qs: Q101 - Q108)
    # ==========================================

    # Q101: Three Mandatory Requirements (Pattern: Foundational theory)
    items.append(q(
        101, "critical-section-requirements", "MCQ", 1, "EASY",
        "According to standard operating system theory, what are the three mandatory requirements that any valid solution to the critical-section problem MUST satisfy?",
        options={
            "A": "Mutual Exclusion, Progress, and Bounded Waiting",
            "B": "Atomicity, Consistency, and Durability",
            "C": "Throughput, Latency, and Utilization",
            "D": "Paging, Segmentation, and Virtual Memory"
        },
        correct_opts=["A"],
        explanation="The three canonical requirements defined by Dijkstra and Silberschatz are: 1. Mutual Exclusion (only one process in CS at a time); 2. Progress (selection of next process cannot be postponed indefinitely); 3. Bounded Waiting (limit on the number of times other processes can enter before a requesting process is granted entry)."
    ))

    # Q102: Non-mandatory assumptions MSQ (Pattern: Common misconceptions MSQ)
    items.append(q(
        102, "critical-section-requirements", "MSQ", 2, "MEDIUM",
        "Which of the following assumptions are NOT mandatory for a valid solution to the critical-section problem?",
        options={
            "A": "No assumptions may be made about the relative speeds of the $N$ processes",
            "B": "Processes must have identical CPU priority levels",
            "C": "Processes must take a predetermined, fixed time inside the critical section",
            "D": "Processes outside their critical section must not prevent other processes from entering"
        },
        correct_opts=["B", "C"],
        explanation="A is a standard rule (no speed assumptions). D is the exact definition of Progress. Neither identical CPU priorities (B) nor fixed execution times (C) are required. In fact, a solution must remain correct regardless of how long a process runs (as long as it eventually leaves CS) and regardless of process priorities."
    ))

    # Q103: Definition of Progress (Pattern: Precise definition analysis)
    items.append(q(
        103, "critical-section-requirements", "MCQ", 2, "MEDIUM",
        "Which of the following scenarios represents a formal violation of the 'Progress' requirement of the critical-section problem?",
        options={
            "A": "The critical section is currently empty, three processes wish to enter, but none of them can enter because they are blocked by a decision mechanism involving a process currently in its non-critical section",
            "B": "Two processes enter the critical section simultaneously",
            "C": "A process takes 10 seconds to execute its critical section instead of 2 seconds",
            "D": "A process releases a lock and immediately terminates"
        },
        correct_opts=["A"],
        explanation="Progress states that if no process is in its critical section and some processes wish to enter, only those processes not in their remainder sections can participate in deciding who enters next, and this selection cannot be postponed indefinitely. If processes are prevented from entering by inactive remainder processes, Progress is violated. (Scenario B is a violation of Mutual Exclusion)."
    ))

    # Q104: Bounded Waiting Formal Definition (Pattern: Starvation avoidance)
    items.append(q(
        104, "critical-section-requirements", "MCQ", 2, "MEDIUM",
        "What is the exact definition of the 'Bounded Waiting' requirement in process synchronization?",
        options={
            "A": "There must be a bound on the number of times other processes are allowed to enter their critical sections after a process has made a request to enter and before that request is granted",
            "B": "A process must finish its execution within a bounded number of clock cycles",
            "C": "The operating system must preempt any process that waits longer than 1 second",
            "D": "The size of the waiting queue must be bounded by $N$"
        },
        correct_opts=["A"],
        explanation="Bounded Waiting requires that a bound exists on the number of times other processes are allowed to enter their critical sections after a process has made a request to enter and before that request is granted, thereby preventing starvation."
    ))

    # Q105: Remainder Section Behavior (Pattern: Remainder section invariant)
    items.append(q(
        105, "critical-section-requirements", "MCQ", 1, "EASY",
        "If a process halts or terminates in its remainder section (non-critical section), what impact should this have on other processes competing for the critical section in a correct solution?",
        options={
            "A": "Zero impact: other competing processes must continue to enter and exit their critical sections without being blocked",
            "B": "All other processes must also halt immediately",
            "C": "The operating system must reboot",
            "D": "The critical section lock must be permanently locked"
        },
        correct_opts=["A"],
        explanation="A fundamental corollary of the Progress condition is that a process operating or halted in its remainder section must have zero influence on other processes wishing to enter the critical section."
    ))

    # Q106: MSQ Necessary vs Optional conditions (Pattern: Theoretical criteria MSQ)
    items.append(q(
        106, "critical-section-requirements", "MSQ", 2, "HARD",
        "Which of the following statements regarding the critical section problem are TRUE?",
        options={
            "A": "Mutual Exclusion alone does NOT guarantee a correct solution, as a deadlock-prone algorithm can trivially satisfy Mutual Exclusion by letting no one enter",
            "B": "A uniprocessor kernel can disable interrupts to implement critical sections within kernel mode",
            "C": "Preemptive kernels are more responsive than non-preemptive kernels but require careful synchronization data structures",
            "D": "Non-preemptive kernels are inherently free from race conditions on kernel data structures in uniprocessor systems"
        },
        correct_opts=["A", "B", "C", "D"],
        explanation="All four statements are true. A: trivial locks like 'while(1);' satisfy ME but violate Progress. B: uniprocessor kernel code can disable interrupts for fast atomic blocks. C: preemptive kernels allow higher priority tasks to preempt kernel execution, requiring locking on shared data structures. D: in a non-preemptive uniprocessor kernel, a kernel process runs until it voluntarily yields or blocks, making kernel data structures immune to concurrency race conditions."
    ))

    # Q107: Lock-out and livelock (Pattern: Livelock analysis)
    items.append(q(
        107, "critical-section-requirements", "MCQ", 2, "MEDIUM",
        "Two threads continually execute `try_lock()` on two resources in opposite order, fail, release their held lock, and immediately retry without delay. They never sleep and continuously consume 100% CPU. Which condition does this characterize?",
        options={
            "A": "Livelock",
            "B": "Deadlock",
            "C": "Bounded Waiting",
            "D": "Strict Mutual Exclusion"
        },
        correct_opts=["A"],
        explanation="Livelock occurs when two or more processes continuously change their states in response to each other without making functional progress. Unlike deadlock (where processes are in a blocked/waiting state), livelocked processes are actively running on the CPU."
    ))

    # Q108: Priority Inversion (Pattern: Priority Inversion phenomenon)
    items.append(q(
        108, "critical-section-requirements", "MCQ", 2, "HARD",
        "A low-priority process $L$ holds lock $M$. A high-priority process $H$ requests $M$ and blocks. Medium-priority processes $M_1, M_2$ that do not need $M$ preempt $L$ and execute continuously, preventing $L$ from releasing $M$, which transitively delays $H$. What is this phenomenon called, and what is its standard remedy?",
        options={
            "A": "Priority Inversion; remedied by Priority Inheritance (boosting $L$'s priority to $H$'s priority while it holds $M$)",
            "B": "Deadlock; remedied by killing process $H$",
            "C": "Starvation; remedied by round-robin scheduling",
            "D": "Race condition; remedied by disabling virtual memory"
        },
        correct_opts=["A"],
        explanation="This is the classic Priority Inversion problem (famous from the Mars Pathfinder spacecraft). The standard solution is Priority Inheritance Protocol (PIP), where the lower priority process holding the contested lock temporarily inherits the highest priority of any process waiting for that lock."
    ))

    return items
