# scripts/generate_wp0_questions.py
# Generates 108 high-quality, concept-balanced questions for Process Synchronization
# Covering:
#  1. producer-consumer-bounded-buffer (15 questions)
#  2. readers-writers (15 questions)
#  3. dining-philosophers (15 questions)
#  4. monitors-condition-variables (15 questions)
#  5. hardware-atomic-primitives (15 questions)
#  6. race-interleavings (15 questions)
#  7. peterson-software-solutions (10 questions)
#  8. critical-section-requirements (8 questions)

import json
import re

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

# ==========================================
# 1. producer-consumer-bounded-buffer (15 Qs)
# ==========================================

# Q1: Semaphore initialization (Pattern: Semaphore initialization & meaning)
questions.append(q(
    1, "producer-consumer-bounded-buffer", "MCQ", 1, "EASY",
    "In the classic bounded-buffer producer-consumer problem with a buffer of capacity $N$, which of the following represents the correct initial values of the semaphores `mutex`, `empty`, and `full` respectively?",
    options={
        "A": "mutex = 1, empty = 0, full = N",
        "B": "mutex = 1, empty = N, full = 0",
        "C": "mutex = 0, empty = N, full = 0",
        "D": "mutex = N, empty = N, full = 0"
    },
    correct_opts=["B"],
    explanation="In the standard bounded buffer algorithm, `mutex` is a binary semaphore providing mutual exclusion on the buffer pool initialized to 1. `empty` counts vacant buffer slots initialized to $N$. `full` counts filled slots initialized to 0."
))

# Q2: Invariant evaluation (Pattern: Invariant MSQ)
questions.append(q(
    2, "producer-consumer-bounded-buffer", "MSQ", 2, "MEDIUM",
    "Consider a bounded-buffer producer-consumer system with buffer capacity $N = 10$, using counting semaphores `empty` and `full`, and binary semaphore `mutex = 1`. Which of the following statements are ALWAYS TRUE at any quiescent state (no process actively in the critical section)?",
    options={
        "A": "The value of `empty` + `full` is strictly equal to N",
        "B": "0 <= full <= N and 0 <= empty <= N",
        "C": "If full == N, any producer attempting to produce will be blocked on wait(empty)",
        "D": "A consumer always signals `mutex` before signaling `empty`"
    },
    correct_opts=["A", "B", "C", "D"],
    explanation="Option A and B hold because the buffer slots are partitioned into empty and full slots at steady state: empty + full = N. Option C is true because when full == N, empty == 0, so wait(empty) blocks. Option D is true because in standard code, signal(mutex) is executed right after leaving the critical section, followed by signal(empty)."
))

# Q3: Deadlock swap trap (Pattern: Deadlock code reordering)
questions.append(q(
    3, "producer-consumer-bounded-buffer", "MCQ", 2, "MEDIUM",
    "In the bounded-buffer problem with semaphores `mutex` (initialized to 1), `empty` (initialized to $N$), and `full` (initialized to 0), a programmer erroneously swaps the two wait operations in the producer code:\n```c\nwait(mutex);\nwait(empty);\n// insert item into buffer\nsignal(mutex);\nsignal(full);\n```\nWhat is the consequence when the buffer becomes completely full?",
    options={
        "A": "Mutual exclusion is violated and buffer corruption occurs",
        "B": "A deadlock occurs because the producer holds `mutex` while waiting for `empty`, preventing any consumer from entering",
        "C": "The consumer continuously steals empty slots causing starvation of the producer",
        "D": "The producer will overwrite older items without waiting"
    },
    correct_opts=["B"],
    explanation="When the buffer is full (empty == 0), the producer executes wait(mutex) and successfully acquires the mutex, then blocks on wait(empty). The consumer needs to enter the critical section to consume an item and signal `empty`, but it gets blocked on wait(mutex). This forms a cyclic wait between producer and consumer resulting in a deadlock."
))

# Q4: Counting trace (Pattern: Semaphore arithmetic trace)
questions.append(q(
    4, "producer-consumer-bounded-buffer", "NAT", 2, "HARD",
    "A bounded buffer of size $N = 8$ is initialized with `mutex = 1`, `empty = 8`, and `full = 0`. During an execution window, 14 `produce` operations and 9 `consume` operations are attempted. None of the operations abort. Assuming all operations that can proceed do so without error, what is the final value of the semaphore `empty`?",
    num_min=3.0, num_max=3.0,
    explanation="Initially empty slots = 8, filled slots = 0. When 14 produce and 9 consume operations run: 9 items are successfully produced and consumed. Of the remaining 5 produce operations, the buffer can accommodate up to its capacity of 8. Since 9 were consumed, 9 + 5 = 14 total produced items fits if 9 are consumed (net items in buffer = 14 - 9 = 5). Since 5 items are in the buffer, vacant slots = 8 - 5 = 3. Thus final value of empty = 3."
))

# Q5: Multiple producers single consumer buffer index (Pattern: Race condition on circular buffer index)
questions.append(q(
    5, "producer-consumer-bounded-buffer", "MCQ", 2, "MEDIUM",
    "In a bounded buffer implemented using a circular array of size $N$, why is the binary semaphore `mutex` strictly required when there are multiple concurrent producer processes, even though the counting semaphore `empty` prevents buffer overflow?",
    options={
        "A": "Because `empty` only counts available slots but does not protect the shared write index `in` from concurrent update races",
        "B": "Because `empty` cannot be shared across multiple processes in modern operating systems",
        "C": "Because without `mutex`, the consumer cannot distinguish between full and empty slots",
        "D": "Because `empty` resets to zero whenever two producers execute wait() simultaneously"
    },
    correct_opts=["A"],
    explanation="The counting semaphore `empty` ensures that producers wait when the buffer is full. However, if two producers enter concurrently, both would update the shared buffer pointer `in = (in + 1) % N` simultaneously, resulting in a race condition where one overwrites the other's entry. `mutex` ensures only one process writes to the buffer array at any instant."
))

# Q6: Signal order inversion (Pattern: Code mutation analysis)
questions.append(q(
    6, "producer-consumer-bounded-buffer", "MCQ", 2, "HARD",
    "Consider the standard consumer code in the bounded-buffer problem. Suppose the order of signal operations is inverted as follows:\n```c\nwait(full);\nwait(mutex);\n// remove item\nsignal(empty); // inverted\nsignal(mutex); // inverted\n```\nWhich of the following describes the behavior of this code?",
    options={
        "A": "Deadlock will occur on the first consumer invocation",
        "B": "Mutual exclusion is compromised while removing items",
        "C": "Correctness is preserved, though a waiting producer may awaken slightly earlier and wait on `mutex`",
        "D": "The buffer will permanently underflow"
    },
    correct_opts=["C"],
    explanation="Inverting the signal operations (signaling empty before mutex) does not cause deadlock or compromise mutual exclusion. The consumer still holds the mutex while removing the item. Signaling `empty` first merely awakens a waiting producer, which then immediately contends for `mutex`. While signaling mutex first is conventional for minimal lock holding time, this swap is safe."
))

# Q7: Buffer underflow guard (Pattern: Edge case scenario)
questions.append(q(
    7, "producer-consumer-bounded-buffer", "MCQ", 1, "EASY",
    "Which semaphore in the bounded-buffer problem directly prevents a consumer process from attempting to read from an empty buffer (buffer underflow)?",
    options={
        "A": "mutex",
        "B": "full",
        "C": "empty",
        "D": "binary_flag"
    },
    correct_opts=["B"],
    explanation="The semaphore `full` tracks the count of populated slots, initialized to 0. When the buffer is empty, `full` is 0. A consumer calls `wait(full)` as its first action, which blocks the consumer until a producer adds an item and signals `full`."
))

# Q8: MSQ on Bounded buffer properties (Pattern: System properties MSQ)
questions.append(q(
    8, "producer-consumer-bounded-buffer", "MSQ", 2, "MEDIUM",
    "In the standard solution to the bounded buffer problem using counting semaphores and a mutex, which of the following statements are FALSE?",
    options={
        "A": "The producer can produce infinitely fast even if the consumer is halted, without blocking",
        "B": "The mutex lock must be held by the producer while preparing the data item in local memory",
        "C": "The value of the `full` semaphore can never exceed the buffer capacity N",
        "D": "A single shared binary semaphore without counting semaphores is sufficient to coordinate bounded buffer synchronization for arbitrary capacity N"
    },
    correct_opts=["A", "B", "D"],
    explanation="A is FALSE: when the buffer fills up to capacity N, the producer blocks on wait(empty). B is FALSE: preparing the item occurs outside the critical section to maximize concurrency. D is FALSE: a binary semaphore alone cannot keep track of slot counts from 0 to N without additional state variables and synchronization logic. C is TRUE (full <= N)."
))

# Q9: NAT Max concurrent processes (Pattern: Concurrency bounds NAT)
questions.append(q(
    9, "producer-consumer-bounded-buffer", "NAT", 2, "MEDIUM",
    "Consider a bounded buffer of capacity $N = 16$. There are 10 active producer threads and 10 active consumer threads. At most how many threads (producers + consumers combined) can be inside the critical section (accessing the buffer array) at any single point in time?",
    num_min=1.0, num_max=1.0,
    explanation="Mutual exclusion on the shared buffer is governed by the binary semaphore `mutex = 1`. Regardless of the buffer capacity $N$ or how many producers and consumers are active, at most 1 thread can hold `mutex` and be inside the critical section at any single instant."
))

# Q10: Unbounded buffer variant (Pattern: Theoretical variant)
questions.append(q(
    10, "producer-consumer-bounded-buffer", "MCQ", 1, "EASY",
    "In an unbounded buffer producer-consumer problem (where the buffer capacity is infinite), which semaphore becomes unnecessary?",
    options={
        "A": "The `full` semaphore",
        "B": "The `empty` semaphore",
        "C": "The `mutex` semaphore",
        "D": "Both `mutex` and `full`"
    },
    correct_opts=["B"],
    explanation="If buffer capacity is infinite, the buffer never runs out of empty slots. Therefore, producers never need to block waiting for space, making the `empty` semaphore redundant. The `full` semaphore is still required so consumers block when the buffer is empty, and `mutex` is still required to serialize updates."
))

# Q11: Code completion snippet (Pattern: Code analysis & completion)
questions.append(q(
    11, "producer-consumer-bounded-buffer", "MCQ", 2, "MEDIUM",
    "Consider the following skeleton for the producer process in a bounded buffer of size $N$:\n```c\nwhile (true) {\n    item = produce_item();\n    [Line 1];\n    [Line 2];\n    buffer[in] = item;\n    in = (in + 1) % N;\n    [Line 3];\n    [Line 4];\n}\n```\nTo ensure correct synchronization and avoid deadlock, what should Line 1, Line 2, Line 3, and Line 4 be?",
    options={
        "A": "Line 1: wait(mutex); Line 2: wait(empty); Line 3: signal(full); Line 4: signal(mutex);",
        "B": "Line 1: wait(empty); Line 2: wait(mutex); Line 3: signal(mutex); Line 4: signal(full);",
        "C": "Line 1: wait(full); Line 2: wait(mutex); Line 3: signal(empty); Line 4: signal(mutex);",
        "D": "Line 1: wait(empty); Line 2: wait(mutex); Line 3: signal(empty); Line 4: signal(full);"
    },
    correct_opts=["B"],
    explanation="The correct sequence is: wait on counting semaphore `empty` first, then acquire `mutex`. Inside the critical section, insert the item. Then release `mutex`, and finally signal counting semaphore `full` to awaken any waiting consumer."
))

# Q12: Blocked thread calculation (Pattern: State calculation NAT)
questions.append(q(
    12, "producer-consumer-bounded-buffer", "NAT", 2, "HARD",
    "In a bounded-buffer system with capacity $N = 5$, `empty` is initialized to 5, `full` to 0, and `mutex` to 1. Initially the buffer is empty. Seven consumer processes arrive sequentially and execute `wait(full)`. Then, three producer processes arrive and each successfully executes one complete production cycle. How many consumer processes remain blocked in the waiting queue of `full`?",
    num_min=4.0, num_max=4.0,
    explanation="When 7 consumers arrive to an empty buffer, each executes wait(full). Since full starts at 0, all 7 consumers block, and the value of full becomes -7 (or 7 threads queued). When 3 producers complete their cycle, each signals `full` once. Each signal unblocks one consumer. Thus, 7 - 3 = 4 consumers remain blocked."
))

# Q13: Starvation analysis (Pattern: Starvation vs Progress)
questions.append(q(
    13, "producer-consumer-bounded-buffer", "MCQ", 2, "MEDIUM",
    "Suppose counting semaphores are implemented using FIFO queues for blocked processes. Which of the following statements regarding starvation in the bounded-buffer problem is TRUE?",
    options={
        "A": "Neither producers nor consumers can suffer starvation",
        "B": "Producers may starve if consumers continuously run, but consumers will never starve",
        "C": "Consumers may starve if producers continually overwhelm the buffer",
        "D": "Both producers and consumers will inevitably deadlock after $N$ cycles"
    },
    correct_opts=["A"],
    explanation="When semaphores maintain a first-in, first-out (FIFO) queue for blocked processes (known as strong semaphores), every process that blocks on `wait()` will eventually be awakened in the order it arrived as soon as the corresponding `signal()` operations are issued. Hence, starvation is completely prevented."
))

# Q14: MSQ Concurrency flaws in buffer (Pattern: Flaw identification MSQ)
questions.append(q(
    14, "producer-consumer-bounded-buffer", "MSQ", 2, "HARD",
    "A student attempts to solve the bounded buffer problem using only a shared integer variable `count` (number of items in buffer) and busy-wait loops without hardware atomic primitives:\n```c\n// Producer:\nwhile (count == N); // wait\nbuffer[in] = item;\nin = (in + 1) % N;\ncount++;\n```\nWhich synchronization problems can occur with this implementation when executed by concurrent threads?",
    options={
        "A": "Race condition on the `count` variable leading to lost updates",
        "B": "Possibility of buffer overflow where a producer writes to an occupied slot",
        "C": "Excessive CPU wastage due to busy-waiting",
        "D": "Guarantee of bounded waiting for all contending threads"
    },
    correct_opts=["A", "B", "C"],
    explanation="`count++` and `count--` are non-atomic read-modify-write operations, resulting in race conditions (A). If `count` is inconsistent, a producer may evaluate `count == N` as false when the buffer is actually full, overwriting data (B). Busy-waiting consumes CPU cycles while spinning (C). D is false as busy waiting offers no bounded waiting guarantees."
))

# Q15: Memory barrier / volatile visibility (Pattern: Hardware memory visibility)
questions.append(q(
    15, "producer-consumer-bounded-buffer", "MCQ", 1, "EASY",
    "In a single-producer single-consumer lock-free ring buffer on a modern multiprocessor, why are memory barriers or atomic memory fences required even if only one thread writes to `in` and one thread writes to `out`?",
    options={
        "A": "To prevent CPU and compiler memory reordering from publishing the updated index before the written data is visible in RAM",
        "B": "To ensure that the consumer executes twice as fast as the producer",
        "C": "To prevent the operating system from scheduling other processes",
        "D": "To convert the ring buffer into a linked list dynamically"
    },
    correct_opts=["A"],
    explanation="On modern out-of-order CPUs, writes to the data buffer and the write to the index pointer `in` can be reordered. A memory barrier (release-acquire semantics) guarantees that the payload bytes are fully committed and visible to cache before the updated pointer index is observed by the consumer."
))

print(f"Generated {len(questions)} questions so far.")

with open("scripts/temp_psync_part1.json", "w", encoding="utf-8") as f:
    json.dump(questions, f, indent=2)
