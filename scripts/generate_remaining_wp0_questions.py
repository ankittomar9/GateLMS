# scripts/generate_remaining_wp0_questions.py
# Authors 22 high-quality questions to bring all 10 concepts to >= 15 questions each
# Concepts:
#  1. critical-section-requirements (+4 Qs: 0109..0112) -> total 15
#  2. peterson-software-solutions (+4 Qs: 0113..0116) -> total 15
#  3. semaphore-operations (+7 Qs: 0117..0123) -> total 15
#  4. semaphore-state-tracing (+7 Qs: 0124..0130) -> total 15

questions = []

def q(id_num, concept, q_type, marks, diff, statement, options=None, correct_opts=None, num_min=None, num_max=None, explanation=""):
    assert q_type in ["MCQ", "MSQ", "NAT"], f"Invalid type: {q_type}"
    assert marks in [1, 2], f"Invalid marks: {marks}"
    assert diff in ["EASY", "MEDIUM", "HARD"], f"Invalid diff: {diff}"
    if q_type == "MCQ":
        assert options is not None and len(options) == 4
        assert correct_opts is not None and len(correct_opts) == 1
        assert num_min is None and num_max is None
    elif q_type == "MSQ":
        assert options is not None and len(options) == 4
        assert correct_opts is not None and len(correct_opts) >= 1
        assert num_min is None and num_max is None
    elif q_type == "NAT":
        assert options is None and correct_opts is None
        assert num_min is not None and num_max is not None
        assert num_min <= num_max

    item = {
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
    return item

# ========================================================
# 1. critical-section-requirements (4 Qs: 0109..0112)
# ========================================================

# Q109: Progress definition distinction (Pattern: Progress definition scenario)
questions.append(q(
    109, "critical-section-requirements", "MCQ", 1, "MEDIUM",
    "In the formal definition of the critical-section problem, which of the following processes are allowed to participate in deciding which process will enter its critical section next to satisfy the Progress requirement?",
    options={
        "A": "Only processes currently executing in their remainder sections",
        "B": "Only processes not executing in their remainder sections (i.e., those wishing to enter)",
        "C": "All processes in the system regardless of their execution state",
        "D": "Only processes that have completed their critical section at least once"
    },
    correct_opts=["B"],
    explanation="By definition, the Progress condition specifies that if no process is in its critical section and some processes wish to enter, only those processes that are NOT executing in their remainder section can participate in the decision of who enters next, and this selection cannot be postponed indefinitely."
))

# Q110: Strict alternation violates progress (Pattern: Strict alternation analysis)
questions.append(q(
    110, "critical-section-requirements", "MCQ", 2, "MEDIUM",
    "Consider the strict alternation algorithm for two processes $P_0$ and $P_1$ using a shared integer variable `turn` initialized to 0:\n```c\n// Process Pi (i = 0, 1; j = 1 - i)\nwhile (turn != i);\n// Critical Section\nturn = j;\n// Remainder Section\n```\nIf process $P_0$ completes its critical section, sets `turn = 1`, and then executes an infinite loop inside its remainder section without ever requesting the critical section again, what happens when $P_1$ attempts to enter its critical section for a second time?",
    options={
        "A": "$P_1$ enters immediately because $P_0$ is inactive",
        "B": "$P_1$ is blocked indefinitely waiting for `turn == 1` because $P_0$ never resets `turn = 0` and never re-enters",
        "C": "$P_1$ experiences bounded waiting but progresses after a timeout",
        "D": "Mutual exclusion is violated between $P_0$ and $P_1$"
    },
    correct_opts=["B"],
    explanation="After $P_1$ exits its first critical section, it sets `turn = 0`. To enter a second time, $P_1$ waits for `turn == 1`. However, $P_0$ is stuck in its remainder section and never enters the critical section again to set `turn = 1`. Therefore, $P_1$ is blocked forever by a process in its remainder section, which is a direct violation of the Progress requirement."
))

# Q111: Bounded waiting requirement (Pattern: Bounded waiting invariant MSQ)
questions.append(q(
    111, "critical-section-requirements", "MSQ", 2, "HARD",
    "Which of the following statements regarding the Bounded Waiting condition for synchronization solutions are TRUE?",
    options={
        "A": "Bounded waiting requires that there exists a bound on the number of times other processes are allowed to enter their critical sections after a process has made a request to enter",
        "B": "Bounded waiting prevents starvation of a requesting process",
        "C": "Bounded waiting assumes that the relative execution speeds of the concurrent processes are strictly identical",
        "D": "A solution that guarantees mutual exclusion and progress may still fail bounded waiting if a process can be repeatedly overtaken indefinitely"
    },
    correct_opts=["A", "B", "D"],
    explanation="Option A is the formal textbook definition of Bounded Waiting. Option B is true because limiting overtaking guarantees absence of starvation. Option D is true because a solution can be deadlock-free (progress) but allow unfair scheduling where one process is continually bypassed. Option C is false because no assumptions may be made regarding the relative speeds of the $N$ processes."
))

# Q112: Preemption in OS kernels (Pattern: Preemptive vs Nonpreemptive kernels)
questions.append(q(
    112, "critical-section-requirements", "MCQ", 1, "EASY",
    "In operating system kernel design, why is a non-preemptive kernel inherently free from race conditions on kernel data structures when executing on a single-processor (uniprocessor) architecture?",
    options={
        "A": "Because it disables all hardware interrupts permanently",
        "B": "Because a kernel-mode process runs until it voluntarily yields, blocks for I/O, or exits, so no other kernel process can interleave",
        "C": "Because kernel structures are duplicated in private address spaces for each process",
        "D": "Because it utilizes Peterson's algorithm at every kernel function call"
    },
    correct_opts=["B"],
    explanation="In a non-preemptive kernel on a uniprocessor system, a process running in kernel mode cannot be preempted by the scheduler. It continues execution until it exits kernel mode, blocks, or voluntarily yields the CPU. Thus, only one process is active in the kernel at any given time, eliminating kernel data race conditions."
))

# ========================================================
# 2. peterson-software-solutions (4 Qs: 0113..0116)
# ========================================================

# Q113: Peterson's turn assignment order (Pattern: Code line swap bug)
questions.append(q(
    113, "peterson-software-solutions", "MCQ", 2, "HARD",
    "In Peterson's two-process algorithm, the entry code for process $P_i$ ($i \\in \\{0, 1\\}$, $j = 1 - i$) is:\n```c\nflag[i] = true;\nturn = j;\nwhile (flag[j] && turn == j);\n```\nSuppose an inexperienced programmer swaps the first two lines:\n```c\nturn = j;\nflag[i] = true;\nwhile (flag[j] && turn == j);\n```\nWhich of the following scenarios can occur?",
    options={
        "A": "Mutual exclusion is violated; both processes can enter the critical section simultaneously",
        "B": "Progress is violated; neither process can ever enter the critical section",
        "C": "Deadlock always occurs immediately upon execution",
        "D": "The solution remains completely correct because variable updates are atomic"
    },
    correct_opts=["A"],
    explanation="Consider the interleaving: $P_0$ executes `turn = 1`. Before $P_0$ sets `flag[0] = true`, a context switch occurs to $P_1$. $P_1$ executes `turn = 0`, sets `flag[1] = true`, and checks `while (flag[0] && turn == 0)`. Since $P_0$ has not yet set `flag[0]`, `flag[0]` is false, so $P_1$ enters the critical section! Now $P_0$ resumes, sets `flag[0] = true`, and evaluates `while (flag[1] && turn == 1)`. Since $P_1$ set `turn = 0`, `turn == 1` is false! Hence $P_0$ also enters the critical section! Thus Mutual Exclusion is violated."
))

# Q114: Peterson modern out-of-order execution (Pattern: Memory barriers in software solutions)
questions.append(q(
    114, "peterson-software-solutions", "MCQ", 2, "MEDIUM",
    "Why does Peterson's algorithm fail to guarantee mutual exclusion on modern out-of-order superscalar multi-core processors unless explicit memory barriers (fences) are used?",
    options={
        "A": "Because the boolean data type is not supported at the hardware level",
        "B": "Because modern processor architectures and optimizing compilers may reorder the store to `flag[i]` and the store to `turn` (Store-Store or Store-Load reordering)",
        "C": "Because multi-core systems do not permit shared memory across cores",
        "D": "Because integer operations like `turn = j` require 64-bit alignment"
    },
    correct_opts=["B"],
    explanation="Peterson's algorithm fundamentally relies on sequential consistency where the write to `flag[i] = true` is guaranteed to be visible before reading `flag[j]` in the while loop. Modern processors (e.g. x86, ARM) allow Store-Load reordering (relaxed memory consistency) and compilers may reorder independent memory accesses, causing both threads to enter the critical section unless memory barrier instructions are inserted."
))

# Q115: Peterson multi-process generalization (Pattern: Filter / Bakery algorithm)
questions.append(q(
    115, "peterson-software-solutions", "MCQ", 1, "EASY",
    "Which classical software synchronization algorithm generalizes the concept of Peterson's algorithm to $N$ concurrent processes by having processes advance through $N-1$ distinct waiting stages (levels)?",
    options={
        "A": "Peterson's Filter Algorithm",
        "B": "Dekker's Algorithm",
        "C": "Lamport's Bakery Algorithm",
        "D": "Szymanski's Algorithm"
    },
    correct_opts=["A"],
    explanation="Peterson's Filter algorithm generalizes Peterson's 2-process solution to $N$ processes by using $N-1$ levels, where at each level at least one process is kept from advancing, ensuring that exactly one process reaches the critical section."
))

# Q116: Peterson properties MSQ (Pattern: Comprehensive Peterson properties MSQ)
questions.append(q(
    116, "peterson-software-solutions", "MSQ", 2, "MEDIUM",
    "Which of the following properties are guaranteed by the standard Peterson's algorithm for two processes running under a sequentially consistent memory model?",
    options={
        "A": "Mutual exclusion is strictly satisfied",
        "B": "Progress is strictly satisfied",
        "C": "Bounded waiting is strictly satisfied (a process waits at most one entry by the other)",
        "D": "It completely eliminates busy waiting (CPU spinning)"
    },
    correct_opts=["A", "B", "C"],
    explanation="Peterson's algorithm satisfies Mutual Exclusion (A), Progress (B), and Bounded Waiting (C) with a bound of 1 turn. However, it does NOT eliminate busy waiting (D) — processes spin in a `while (flag[j] && turn == j);` loop consuming CPU cycles."
))

# ========================================================
# 3. semaphore-operations (7 Qs: 0117..0123)
# ========================================================

# Q117: Semaphore negative value interpretation (Pattern: Negative counting semaphore interpretation)
questions.append(q(
    117, "semaphore-operations", "MCQ", 1, "EASY",
    "In an operating system implementation where `wait(S)` decrements the semaphore counter before checking its sign and `signal(S)` increments it, if the current value of counting semaphore $S$ is $-k$ ($k > 0$), what does the magnitude $|-k| = k$ represent?",
    options={
        "A": "There are $k$ resources currently available in the system",
        "B": "There are $k$ processes currently blocked on the waiting queue of semaphore $S$",
        "C": "The semaphore has experienced an underflow error and must be reset",
        "D": "There are $k$ processes currently executing in their critical sections"
    },
    correct_opts=["B"],
    explanation="Under the standard OS implementation of semaphores with a waiting list (such as in Tanenbaum or Silberschatz), when $S < 0$, its absolute value $|S|$ represents the exact number of processes waiting/blocked in the semaphore's queue."
))

# Q118: Counting semaphore range NAT (Pattern: Counting semaphore value range calculation)
questions.append(q(
    118, "semaphore-operations", "NAT", 2, "MEDIUM",
    "A counting semaphore $S$ is initialized to 12. During the execution of a concurrent system, 18 `wait` operations and 9 `signal` operations are executed on $S$. What is the final value of $S$?",
    num_min=3.0,
    num_max=3.0,
    explanation="The value of a counting semaphore is given by:\n$$\\text{Final Value} = \\text{Initial Value} + \\text{Signals} - \\text{Waits}$$\n$$\\text{Final Value} = 12 + 9 - 18 = 3.$$\nTherefore, the final value of $S$ is 3."
))

# Q119: Counting semaphore negative NAT (Pattern: Blocked processes calculation)
questions.append(q(
    119, "semaphore-operations", "NAT", 2, "MEDIUM",
    "A system has a counting semaphore $S$ initialized to 4. Over a period of time, 25 `wait(S)` operations and 15 `signal(S)` operations are invoked on $S$. Assuming the implementation decrements $S$ below zero to record queued processes, how many processes are currently blocked waiting on $S$?",
    num_min=6.0,
    num_max=6.0,
    explanation="Using the formula:\n$$S_{\\text{final}} = S_{\\text{initial}} + \\text{Signals} - \\text{Waits} = 4 + 15 - 25 = -6.$$\nThe number of blocked processes is $|S_{\\text{final}}| = |-6| = 6$."
))

# Q120: Binary semaphore equivalence (Pattern: Mutex vs binary semaphore)
questions.append(q(
    120, "semaphore-operations", "MCQ", 1, "MEDIUM",
    "What is the key functional difference between a mutex lock and a binary semaphore in modern operating systems?",
    options={
        "A": "A mutex can only take values 0, 1, or 2, whereas a binary semaphore is strictly 0 or 1",
        "B": "A mutex has the concept of ownership (only the thread that locked it can unlock it), whereas a binary semaphore can be signaled by any thread",
        "C": "A binary semaphore provides mutual exclusion, but a mutex cannot",
        "D": "A mutex is implemented in software, while a binary semaphore is strictly hardware"
    },
    correct_opts=["B"],
    explanation="The defining difference is ownership: a mutex must be unlocked by the exact thread that acquired (locked) it. A binary semaphore has no ownership semantics; one thread can wait on it and another thread (or ISR) can signal it, making semaphores suitable for signaling and synchronization in addition to mutual exclusion."
))

# Q121: Semaphore atomicity requirement (Pattern: Semaphore atomicity MSQ)
questions.append(q(
    121, "semaphore-operations", "MSQ", 2, "MEDIUM",
    "Which of the following statements regarding the atomicity of semaphore operations `wait(S)` and `signal(S)` are TRUE?",
    options={
        "A": "The modification of the integer value of $S$ and the testing/blocking must be executed as a single indivisible atomic action",
        "B": "On uniprocessor systems, atomicity can be achieved by temporarily disabling hardware interrupts during the execution of `wait` and `signal`",
        "C": "On multi-core architectures, disabling interrupts on one core is sufficient to guarantee atomicity across all cores",
        "D": "Hardware atomic instructions such as Test-and-Set or Compare-and-Swap, or spinlocks, are required to protect semaphore code on multi-core systems"
    },
    correct_opts=["A", "B", "D"],
    explanation="Option A is true by the definition of semaphores. Option B is true because disabling interrupts on a single CPU prevents preemption. Option C is false because disabling interrupts on one CPU does not prevent instructions from executing concurrently on other CPUs. Option D is true because multi-core systems require hardware atomic locking primitives or spinlocks to achieve multiprocessor atomicity."
))

# Q122: Minimum semaphore value NAT (Pattern: Minimum semaphore value trace)
questions.append(q(
    122, "semaphore-operations", "NAT", 2, "HARD",
    "A counting semaphore $S$ is initialized to 7. A total of $m$ processes execute concurrently. Each process executes exactly one `wait(S)` followed by one `signal(S)`. If the maximum number of processes that can be simultaneously blocked on $S$ at any instant is 15, what is the value of $m$?",
    num_min=22.0,
    num_max=22.0,
    explanation="To achieve the maximum number of blocked processes simultaneously, all $m$ processes must invoke `wait(S)` before any process executes `signal(S)`.\nAfter the first 7 processes execute `wait(S)`, the semaphore value reaches 0, and all 7 acquire the resource.\nEvery subsequent process that executes `wait(S)` will be blocked.\nFor 15 processes to be blocked, there must be $7 + 15 = 22$ processes in total.\nThus, $m = 22$."
))

# Q123: Semaphore signaling pattern (Pattern: Signaling rendezvous)
questions.append(q(
    123, "semaphore-operations", "MCQ", 1, "EASY",
    "Suppose process $P_1$ needs to execute statement $S_1$ before process $P_2$ executes statement $S_2$. Using a shared semaphore `synch`, what should be the initial value of `synch` and the code structure in $P_1$ and $P_2$?",
    options={
        "A": "Initialize `synch = 0`; $P_1$ executes `S1; signal(synch);` and $P_2$ executes `wait(synch); S2;`",
        "B": "Initialize `synch = 1`; $P_1$ executes `wait(synch); S1;` and $P_2$ executes `S2; signal(synch);`",
        "C": "Initialize `synch = 0`; $P_1$ executes `wait(synch); S1;` and $P_2$ executes `signal(synch); S2;`",
        "D": "Initialize `synch = 1`; $P_1$ executes `S1; wait(synch);` and $P_2$ executes `signal(synch); S2;`"
    },
    correct_opts=["A"],
    explanation="To enforce precedence $S_1 \\to S_2$, we initialize `synch = 0`. In $P_1$: statement $S_1$ runs first, then `signal(synch)` unblocks $P_2$. In $P_2$: `wait(synch)` blocks until $P_1$ signals, after which $S_2$ executes."
))

# ========================================================
# 4. semaphore-state-tracing (7 Qs: 0124..0130)
# ========================================================

# Q124: Multi-semaphore resource allocation trace (Pattern: Multi-semaphore allocation trace)
questions.append(q(
    124, "semaphore-state-tracing", "MCQ", 2, "MEDIUM",
    "Three processes $P_1, P_2, P_3$ share two semaphores $S_1 = 1$ and $S_2 = 1$. The processes execute the following sequences:\n- $P_1$: `wait(S1); wait(S2); signal(S1); signal(S2);`\n- $P_2$: `wait(S2); wait(S1); signal(S2); signal(S1);`\n- $P_3$: `wait(S1); signal(S1);`\nWhich of the following interleavings results in a deadlock?",
    options={
        "A": "$P_3$ executes `wait(S1)`, then $P_1$ executes `wait(S1)`, then $P_3$ executes `signal(S1)`",
        "B": "$P_1$ executes `wait(S1)`, context switch to $P_2$ which executes `wait(S2)`, then $P_1$ attempts `wait(S2)` and $P_2$ attempts `wait(S1)`",
        "C": "$P_2$ executes `wait(S2); wait(S1); signal(S2); signal(S1);` to completion, followed by $P_1$",
        "D": "$P_1$ executes `wait(S1); wait(S2);` to completion, followed by $P_2$"
    },
    correct_opts=["B"],
    explanation="In option B, $P_1$ acquires $S_1$ and $P_2$ acquires $S_2$. Then $P_1$ requests $S_2$ (held by $P_2$) and blocks, while $P_2$ requests $S_1$ (held by $P_1$) and blocks. This creates a cyclic wait condition $P_1 \\to P_2 \\to P_1$, which is a classic deadlock."
))

# Q125: Trace final value with concurrent loop (Pattern: Loop iteration trace NAT)
questions.append(q(
    125, "semaphore-state-tracing", "NAT", 2, "MEDIUM",
    "A semaphore $S$ is initialized to 10. Process $A$ runs a loop calling `wait(S)` 7 times and `signal(S)` 2 times. Simultaneously, Process $B$ calls `wait(S)` 5 times and `signal(S)` 4 times. Both processes run to completion. What is the final value of semaphore $S$?",
    num_min=4.0,
    num_max=4.0,
    explanation="Total wait operations = $7 + 5 = 12$.\nTotal signal operations = $2 + 4 = 6$.\nInitial value = 10.\nFinal value = $10 + 6 - 12 = 4$."
))

# Q126: Available resources trace NAT (Pattern: Available resources NAT)
questions.append(q(
    126, "semaphore-state-tracing", "NAT", 2, "HARD",
    "A pool of 8 identical tape drives is managed by a counting semaphore `drives` initialized to 8. Five processes each request 2 tape drives by calling `wait(drives)` twice. If 3 of these processes successfully acquire both tape drives, while the 4th process acquires 1 tape drive and blocks on the second, how many tape drives are currently allocated (in use)?",
    num_min=7.0,
    num_max=7.0,
    explanation="3 processes have acquired 2 drives each: $3 \\times 2 = 6$ drives.\nThe 4th process has acquired 1 drive and is waiting for the second: $1$ drive.\nThe 5th process has acquired 0 drives.\nTotal drives allocated and in use = $6 + 1 = 7$ drives."
))

# Q127: Semaphore state sequence MSQ (Pattern: Valid trace state MSQ)
questions.append(q(
    127, "semaphore-state-tracing", "MSQ", 2, "HARD",
    "Consider two processes $P_0$ and $P_1$ executing the following code with binary semaphores `mutex = 1` and `sync = 0`:\n```c\n// P0\nwait(mutex);\nsignal(sync);\nsignal(mutex);\n\n// P1\nwait(sync);\nwait(mutex);\nsignal(mutex);\n```\nWhich of the following intermediate states $(mutex, sync)$ can legitimately occur at some point during execution?",
    options={
        "A": "$(0, 0)$ when $P_0$ is between its `wait(mutex)` and `signal(sync)`",
        "B": "$(0, 1)$ when $P_0$ is between its `signal(sync)` and `signal(mutex)`",
        "C": "$(1, 0)$ when $P_1$ has executed `wait(sync)` and $P_0$ has completed",
        "D": "$(0, 0)$ when $P_1$ is inside its critical section (between `wait(mutex)` and `signal(mutex)`)"
    },
    correct_opts=["A", "B", "C", "D"],
    explanation="Let us trace:\n- Initially: (1, 0).\n- $P_0$ executes `wait(mutex)`: mutex becomes 0, sync is 0 -> state (0, 0), so Option A is valid.\n- $P_0$ executes `signal(sync)`: mutex is 0, sync becomes 1 -> state (0, 1), so Option B is valid.\n- $P_0$ executes `signal(mutex)`: mutex becomes 1, sync is 1. Then $P_1$ executes `wait(sync)`: sync becomes 0, mutex is 1 -> state (1, 0), so Option C is valid.\n- Next, $P_1$ executes `wait(mutex)`: mutex becomes 0, sync is 0 -> state (0, 0), so Option D is valid."
))

# Q128: Barber shop waiting room trace NAT (Pattern: Sleeping barber capacity trace NAT)
questions.append(q(
    128, "semaphore-state-tracing", "NAT", 2, "MEDIUM",
    "In the Sleeping Barber problem with 5 waiting chairs, semaphores are defined as: `customers = 0` (counting customers waiting), `barber = 0` (barber readiness), and `mutex = 1`. If 8 customers arrive while the barber is cutting hair of customer 1 (not in the waiting chairs), how many customers will leave without getting a haircut due to lack of empty chairs?",
    num_min=3.0,
    num_max=3.0,
    explanation="The waiting room has a capacity of 5 chairs. When 8 customers arrive, the first 5 take the 5 waiting chairs. The remaining $8 - 5 = 3$ customers find all chairs occupied and balk (leave without a haircut)."
))

# Q129: Barrier synchronization trace (Pattern: Cyclic barrier phase trace)
questions.append(q(
    129, "semaphore-state-tracing", "MCQ", 2, "HARD",
    "Three threads $T_1, T_2, T_3$ synchronize at a barrier using a shared counter `count = 0`, a mutex `mtx = 1`, and a semaphore `barrier = 0`:\n```c\nwait(mtx);\ncount++;\nif (count == 3) {\n    signal(barrier);\n}\nsignal(mtx);\nwait(barrier);\nsignal(barrier);\n```\nWhat is the purpose of the final `signal(barrier)` after `wait(barrier)`?",
    options={
        "A": "To reset `count` back to zero",
        "B": "To propagate the wake-up signal in a turnstile fashion so that all waiting threads can pass through the barrier",
        "C": "To re-lock the barrier against incoming threads from the next iteration",
        "D": "To signal the operating system scheduler to raise the priority of the threads"
    },
    correct_opts=["B"],
    explanation="When thread 3 reaches `count == 3`, it signals `barrier` once. The first thread waking up from `wait(barrier)` immediately executes `signal(barrier)` to wake up the next thread, which in turn wakes up the third thread. This cascading signal pattern is known as a turnstile, ensuring all 3 threads pass through."
))

# Q130: Maximum concurrency NAT (Pattern: Max concurrent execution trace NAT)
questions.append(q(
    130, "semaphore-state-tracing", "NAT", 1, "EASY",
    "A database allows at most 4 simultaneous reader connections. A counting semaphore `read_slots` is initialized to 4. Each reader executes `wait(read_slots)` upon connecting and `signal(read_slots)` upon disconnecting. What is the maximum number of reader processes that can be concurrently executing queries inside the database?",
    num_min=4.0,
    num_max=4.0,
    explanation="The semaphore is initialized to 4. Each active reader consumes 1 slot. When 4 readers are active, `read_slots` equals 0, preventing any further readers from entering until an active reader signals. Thus, the maximum concurrent readers is 4."
))

print(f"Total remaining questions authored: {len(questions)}")
assert len(questions) == 22
