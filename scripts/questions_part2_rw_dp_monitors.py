# scripts/questions_part2_rw_dp_monitors.py
# Q16 to Q60: readers-writers (15), dining-philosophers (15), monitors-condition-variables (15)

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
    # 2. readers-writers (15 Qs: Q16 - Q30)
    # ==========================================

    # Q16: First Readers-Writers logic (Pattern: Reader preference semaphore roles)
    items.append(q(
        16, "readers-writers", "MCQ", 1, "EASY",
        "In the first readers-writers problem (readers-preference solution), what is the primary role of the `wrt` (or `rw_mutex`) binary semaphore?",
        options={
            "A": "It counts the number of active reader processes currently reading the database",
            "B": "It provides mutual exclusion for writers and blocks subsequent writers when the first reader enters",
            "C": "It serializes multiple concurrent readers so that only one reader reads at a time",
            "D": "It prevents readers from starving the writer processes"
        },
        correct_opts=["B"],
        explanation="In the first readers-writers problem, `wrt` is a binary semaphore (initialized to 1) common to both readers and writers. The first reader to arrive executes `wait(wrt)` to lock the resource against writers. Subsequent readers bypass `wrt`. Writers also execute `wait(wrt)` before writing, ensuring mutual exclusion between writers and readers."
    ))

    # Q17: Readcount race condition trap (Pattern: Synchronization flaw analysis)
    items.append(q(
        17, "readers-writers", "MCQ", 2, "MEDIUM",
        "In the readers-preference solution, what happens if the statements protecting `readcount` (`wait(mutex)` and `signal(mutex)`) are omitted from the reader code?",
        options={
            "A": "A writer process may enter the database concurrently while multiple readers are reading",
            "B": "A deadlock immediately occurs between the first two readers",
            "C": "Writers will starve readers permanently",
            "D": "The `wrt` semaphore will automatically turn into a counting semaphore"
        },
        correct_opts=["A"],
        explanation="If `mutex` is omitted, multiple readers executing `readcount++` or `readcount--` concurrently will cause a race condition on `readcount`. For instance, if two readers enter simultaneously when `readcount == 0`, both may fail to execute `wait(wrt)`, or `readcount` may erroneously evaluate to 0 upon exit, causing a reader to signal `wrt` prematurely while another reader is still active. This allows a writer to enter concurrently with an active reader."
    ))

    # Q18: Reader preference starvation (Pattern: Starvation scenario MSQ)
    items.append(q(
        18, "readers-writers", "MSQ", 2, "MEDIUM",
        "Consider the classic reader-preference solution to the readers-writers problem. Which of the following statements are TRUE?",
        options={
            "A": "Writer processes may suffer from starvation if a continuous stream of readers arrives",
            "B": "Multiple reader processes can access the shared resource concurrently",
            "C": "A writer and a reader can access the shared resource simultaneously without conflicts",
            "D": "The first reader locks `wrt`, and the last reader leaving releases `wrt`"
        },
        correct_opts=["A", "B", "D"],
        explanation="A is TRUE: as long as at least one reader is active, `readcount > 0` and `wrt` remains locked, starving arriving writers. B is TRUE: readers read concurrently. C is FALSE: mutual exclusion between readers and writers is strictly enforced. D is TRUE: only the first reader acquires `wrt` and the last reader (`if (--readcount == 0)`) releases it."
    ))

    # Q19: Maximum concurrent readers NAT (Pattern: Quantitative concurrency NAT)
    items.append(q(
        19, "readers-writers", "NAT", 2, "EASY",
        "In a system with 12 reader processes and 4 writer processes implementing the standard first readers-writers algorithm, what is the maximum number of reader processes that can simultaneously read from the shared database?",
        num_min=12.0, num_max=12.0,
        explanation="In the readers-writers problem, there is no upper limit on the number of concurrent readers as long as no writer holds the lock. Thus, all 12 reader processes can simultaneously read the database."
    ))

    # Q20: Second readers-writers / writer preference (Pattern: Writer preference protocol)
    items.append(q(
        20, "readers-writers", "MCQ", 2, "HARD",
        "In the second readers-writers problem (writer-preference solution), how is writer starvation prevented when readers arrive in large volumes?",
        options={
            "A": "By forcing readers to sleep for a random interval before every read operation",
            "B": "By having arriving writers set a flag/semaphore that prevents new readers from acquiring the read lock",
            "C": "By converting all readers into writers dynamically",
            "D": "By running writers at a higher CPU scheduling priority"
        },
        correct_opts=["B"],
        explanation="In the writer-preference solution, when a writer arrives and announces its intention to write, newly arriving readers are blocked from acquiring the resource (often using a `readTry` or `queue` semaphore). Once active readers finish, the waiting writer gets immediate access, preventing writer starvation."
    ))

    # Q21: Semaphores required in writer-preference (Pattern: Semaphore count MSQ)
    items.append(q(
        21, "readers-writers", "MSQ", 2, "HARD",
        "Which of the following semaphores or mutexes are typically utilized in an implementation of the writer-preference readers-writers problem?",
        options={
            "A": "A semaphore to ensure mutual exclusion on updating the `readcount` variable",
            "B": "A semaphore to ensure mutual exclusion on updating the `writecount` variable",
            "C": "A semaphore to ensure exclusive access to the shared resource data",
            "D": "A barrier semaphore to block newly arriving readers when a writer is queued"
        },
        correct_opts=["A", "B", "C", "D"],
        explanation="All four are utilized: `rmutex` protects `readcount` (A), `wmutex` protects `writecount` (B), `resource` (or `wrt`) protects the database (C), and `readTry` (or `order`) blocks arriving readers when `writecount > 0` (D)."
    ))

    # Q22: Reader exit sequence trace (Pattern: Code trace & invariant)
    items.append(q(
        22, "readers-writers", "MCQ", 2, "MEDIUM",
        "Consider the reader exit protocol in the first readers-writers problem:\n```c\nwait(mutex);\nreadcount--;\nif (readcount == 0)\n    signal(wrt);\nsignal(mutex);\n```\nIf an active reader forgets to execute `signal(mutex)` upon leaving, what is the consequence?",
        options={
            "A": "Subsequent readers and writers will both be unable to enter, causing deadlock across all arriving processes",
            "B": "Only writers will be blocked; future readers can still read unhindered",
            "C": "The database will be deleted",
            "D": "Multiple writers will enter the critical section simultaneously"
        },
        correct_opts=["A"],
        explanation="If `signal(mutex)` is omitted, `mutex` remains 0. Any subsequent reader attempting to enter or exit will block on `wait(mutex)`. Any subsequent writer will either block on `wait(wrt)` if `wrt` was not released, or on `mutex` if relevant. The entire system deadlocks."
    ))

    # Q23: Reader-Writer Lock API semantics (Pattern: Standard API semantics)
    items.append(q(
        23, "readers-writers", "MCQ", 1, "EASY",
        "In POSIX pthreads `pthread_rwlock_t`, which behavior occurs when a thread calls `pthread_rwlock_wrlock()` while another thread currently holds a read lock via `pthread_rwlock_rdlock()`?",
        options={
            "A": "The calling thread immediately acquires the write lock and preempts the reader",
            "B": "The calling thread blocks until all current read locks are released",
            "C": "An error code `EBUSY` is returned and the process terminates",
            "D": "The reader thread is aborted immediately"
        },
        correct_opts=["B"],
        explanation="A write lock (`pthread_rwlock_wrlock`) requires exclusive access. If one or more threads hold a read lock, the calling thread blocks until all read locks have been unlocked."
    ))

    # Q24: Concurrent writers bound NAT (Pattern: Invariant NAT)
    items.append(q(
        24, "readers-writers", "NAT", 1, "EASY",
        "In any correct solution to the readers-writers problem, what is the maximum number of writer processes that can simultaneously modify the shared resource?",
        num_min=1.0, num_max=1.0,
        explanation="By definition of the critical section and mutual exclusion, at most 1 writer process can write to the shared resource at any given time to avoid inconsistent data states."
    ))

    # Q25: Reader-Writer Fairness (Pattern: Fair R/W scheduling)
    items.append(q(
        25, "readers-writers", "MCQ", 2, "MEDIUM",
        "A fair (starvation-free) solution to the readers-writers problem is designed using a FIFO turnstile semaphore. What guarantee does this fairness protocol provide?",
        options={
            "A": "Readers and writers access the resource strictly in the order of their arrival without starving either group",
            "B": "Writers always execute in half the time of readers",
            "C": "Readers are completely prohibited from reading concurrently",
            "D": "The resource can be accessed without acquiring any locks"
        },
        correct_opts=["A"],
        explanation="In a fair solution (such as using an arrival queue or turnstile semaphore), requests are served in FIFO arrival order. A continuous stream of readers cannot bypass a previously queued writer, guaranteeing starvation freedom for both readers and writers."
    ))

    # Q26: MSQ on Reader-Writer Deadlocks (Pattern: Deadlock vulnerabilities MSQ)
    items.append(q(
        26, "readers-writers", "MSQ", 2, "HARD",
        "Which of the following programming errors in the reader or writer protocols can directly lead to a deadlock?",
        options={
            "A": "A reader calls `wait(wrt)` without ever calling `signal(wrt)` when `readcount == 0`",
            "B": "A writer executes `wait(wrt)` and subsequently blocks indefinitely inside the critical section (e.g. waiting for I/O that never completes)",
            "C": "A reader executes `signal(mutex)` before `wait(mutex)`",
            "D": "A writer executes `signal(wrt)` without calling `wait(wrt)`"
        },
        correct_opts=["A", "B"],
        explanation="If the first reader acquires `wrt` but the last reader never signals `wrt`, future writers block forever on `wrt` (A). If a writer holds `wrt` and blocks indefinitely, all subsequent readers and writers remain blocked (B). Option C causes a protocol violation (semaphore value corruption) rather than a clean deadlock. Option D prematurely signals an unheld lock."
    ))

    # Q27: Reader preference trace NAT (Pattern: Semaphore state trace NAT)
    items.append(q(
        27, "readers-writers", "NAT", 2, "HARD",
        "In the first readers-writers problem, semaphores `mutex` and `wrt` are both initialized to 1. Six readers ($R_1$ to $R_6$) arrive sequentially while no writers are present, and all 6 are currently reading. A writer $W_1$ arrives and executes its entry code. Then, two more readers $R_7$ and $R_8$ arrive and execute their entry code. In the standard reader-preference algorithm, how many processes are actively reading in the database at this moment?",
        num_min=8.0, num_max=8.0,
        explanation="In the first readers-writers (readers-preference) algorithm, once the first reader locks `wrt`, subsequent readers check `readcount++` under `mutex` and enter the database directly without checking `wrt`. When $W_1$ arrives, it calls `wait(wrt)` and blocks. When $R_7$ and $R_8$ arrive, because readers have preference, they increment `readcount` and join the active readers. Thus all $6 + 2 = 8$ readers are actively reading."
    ))

    # Q28: Upgrading read lock to write lock (Pattern: Lock upgrade hazard)
    items.append(q(
        28, "readers-writers", "MCQ", 2, "MEDIUM",
        "In a database management system using shared (S) and exclusive (X) locks, two concurrent transactions $T_1$ and $T_2$ both hold an S lock on data item $A$. Both transactions subsequently attempt to upgrade their S lock to an X lock on $A$. What is the immediate outcome?",
        options={
            "A": "Both upgrades succeed immediately and both transactions write concurrently",
            "B": "A deadlock occurs because $T_1$ waits for $T_2$ to release its S lock, while $T_2$ waits for $T_1$ to release its S lock",
            "C": "The transaction with higher timestamp is immediately aborted by hardware",
            "D": "The lock manager merges both transactions into one"
        },
        correct_opts=["B"],
        explanation="An exclusive (X) lock requires that no other transaction holds any lock on that item. $T_1$ cannot upgrade until $T_2$ releases its S lock, and $T_2$ cannot upgrade until $T_1$ releases its S lock. This circular wait causes an immediate deadlock."
    ))

    # Q29: Reader preference code identification (Pattern: Code structure analysis)
    items.append(q(
        29, "readers-writers", "MCQ", 1, "EASY",
        "In the readers-preference algorithm, the code executed by a writer process is:\n```c\nwait(wrt);\n// writing is performed\nsignal(wrt);\n```\nWhy does the writer NOT need a `writecount` or a separate `mutex` variable in this solution?",
        options={
            "A": "Because writers execute in user space while readers execute in kernel space",
            "B": "Because `wrt` alone enforces mutual exclusion among multiple writers as well as between writers and readers",
            "C": "Because the compiler automatically inserts semaphores for writers",
            "D": "Because there can never be more than one writer in any operating system"
        },
        correct_opts=["B"],
        explanation="The `wrt` semaphore is a binary semaphore initialized to 1. When a writer executes `wait(wrt)`, any other contending writer blocks on `wrt`. Likewise, if readers are active, the first reader holds `wrt`. Thus `wrt` alone guarantees mutual exclusion between writers and all other processes."
    ))

    # Q30: Read-Copy-Update (RCU) vs RW Locks (Pattern: Modern OS synchronization)
    items.append(q(
        30, "readers-writers", "MCQ", 2, "HARD",
        "How does the Linux kernel's Read-Copy-Update (RCU) mechanism improve upon traditional reader-writer spinlocks for read-mostly data structures?",
        options={
            "A": "Readers incur zero lock acquisition overhead and execute concurrently with writers without blocking",
            "B": "Writers never need to allocate memory for modified data",
            "C": "RCU eliminates the need for garbage collection or quiescent state tracking",
            "D": "RCU permits multiple writers to modify the same memory word without synchronization"
        },
        correct_opts=["A"],
        explanation="RCU allows readers to traverse shared data structures without acquiring locks, atomic instructions, or memory barriers, achieving near-zero read overhead. Writers create a new copy of the structure, update it, and atomically publish the pointer, deferring the reclamation of old memory until all pre-existing readers have completed a quiescent grace period."
    ))

    # ==========================================
    # 3. dining-philosophers (15 Qs: Q31 - Q45)
    # ==========================================

    # Q31: Classic Deadlock scenario (Pattern: Deadlock manifestation)
    items.append(q(
        31, "dining-philosophers", "MCQ", 1, "EASY",
        "Consider 5 philosophers sitting around a circular table with 5 chopsticks, where each philosopher $i$ executes:\n```c\nwait(chopstick[i]); // pick left\nwait(chopstick[(i + 1) % 5]); // pick right\neat();\nsignal(chopstick[i]);\nsignal(chopstick[(i + 1) % 5]);\n```\nUnder what condition does this system deadlock?",
        options={
            "A": "When all 5 philosophers become hungry simultaneously and each picks up their left chopstick",
            "B": "When philosopher 0 eats for an unusually long duration",
            "C": "When exactly 2 philosophers eat concurrently",
            "D": "Only when the table rotates in a clockwise direction"
        },
        correct_opts=["A"],
        explanation="If all 5 philosophers simultaneously pick up their left chopstick (`chopstick[i]`), every chopstick semaphore drops to 0. When each philosopher then attempts to pick up their right chopstick (`chopstick[(i + 1) % 5]`), each philosopher blocks indefinitely, resulting in a circular wait and complete deadlock."
    ))

    # Q32: Asymmetric solution (Pattern: Deadlock prevention via asymmetric rule)
    items.append(q(
        32, "dining-philosophers", "MCQ", 2, "MEDIUM",
        "Which of the following strategies successfully prevents deadlock in the 5-philosopher dining philosophers problem without reducing the number of seated philosophers?",
        options={
            "A": "An odd-numbered philosopher picks up the left chopstick first, while an even-numbered philosopher picks up the right chopstick first",
            "B": "Each philosopher picks up both chopsticks simultaneously without any synchronization primitive",
            "C": "Doubling the thinking time of all philosophers",
            "D": "Allowing philosophers to eat using only one chopstick"
        },
        correct_opts=["A"],
        explanation="The asymmetric solution breaks the Circular Wait condition. For instance, philosopher 0 picks right then left, while philosopher 1 picks left then right. Since at least two adjacent philosophers contend for the same first chopstick, circular wait cannot form, guaranteeing that at least one philosopher can acquire both chopsticks and eat."
    ))

    # Q33: Coffman conditions in Dining Philosophers (Pattern: Deadlock conditions MSQ)
    items.append(q(
        33, "dining-philosophers", "MSQ", 2, "MEDIUM",
        "Which of the following Coffman conditions are present when the classic naive dining philosophers algorithm deadlocks?",
        options={
            "A": "Mutual Exclusion: each chopstick can be held by at most one philosopher at a time",
            "B": "Hold and Wait: each philosopher holds one chopstick while waiting for another",
            "C": "No Preemption: a chopstick cannot be forcibly taken from a philosopher",
            "D": "Circular Wait: philosopher $P_i$ waits for chopstick held by $P_{(i+1)\\%5}$"
        },
        correct_opts=["A", "B", "C", "D"],
        explanation="All four Coffman conditions are satisfied simultaneously when deadlock occurs: each chopstick is non-shareable (Mutual Exclusion), each philosopher holds one chopstick while requesting another (Hold and Wait), chopsticks are released only voluntarily (No Preemption), and there is a closed cycle of dependency (Circular Wait)."
    ))

    # Q34: Max concurrent eating philosophers NAT (Pattern: Concurrency calculation NAT)
    items.append(q(
        34, "dining-philosophers", "NAT", 1, "EASY",
        "In a dining philosophers problem with $N = 7$ philosophers sitting at a circular table, what is the maximum number of philosophers that can eat simultaneously without any conflict?",
        num_min=3.0, num_max=3.0,
        explanation="To eat, a philosopher requires both their left and right chopsticks. Two adjacent philosophers cannot eat simultaneously. Therefore, at most $\\lfloor N / 2 \\rfloor = \\lfloor 7 / 2 \\rfloor = 3$ non-adjacent philosophers can eat simultaneously."
    ))

    # Q35: Limiting seated philosophers (Pattern: Resource limitation solution)
    items.append(q(
        35, "dining-philosophers", "MCQ", 2, "MEDIUM",
        "A standard deadlock-prevention technique for $N$ dining philosophers is to introduce a counting semaphore `seats` initialized to $N - 1$, where a philosopher must acquire a seat before picking up any chopstick. Why does this guarantee deadlock freedom?",
        options={
            "A": "By Pigeonhole Principle, if at most $N-1$ philosophers sit at the table with $N$ chopsticks, at least one philosopher is guaranteed to acquire both adjacent chopsticks",
            "B": "It eliminates the need for chopsticks entirely",
            "C": "It forces philosophers to eat in strict numerical order",
            "D": "It makes all chopsticks shareable resources"
        },
        correct_opts=["A"],
        explanation="If at most $N-1$ philosophers are allowed at the table, there are $N$ chopsticks available for at most $N-1$ contenders. By the Pigeonhole Principle, at least one seated philosopher will have both neighboring chopsticks available, will eat, and will release them, ensuring continuous progress and preventing deadlock."
    ))

    # Q36: Starvation despite deadlock freedom (Pattern: Deadlock-free vs Starvation-free)
    items.append(q(
        36, "dining-philosophers", "MCQ", 2, "HARD",
        "Consider a dining philosophers solution that successfully prevents deadlock. Can a philosopher still suffer from starvation in this solution?",
        options={
            "A": "No, deadlock freedom mathematically guarantees starvation freedom in all systems",
            "B": "Yes, two fast-eating neighbors can alternate eating such that the middle philosopher never finds both chopsticks free simultaneously",
            "C": "Yes, but only if the operating system crashes",
            "D": "No, because semaphores always schedule threads in strictly random order"
        },
        correct_opts=["B"],
        explanation="Deadlock freedom does NOT imply starvation freedom. If philosopher $P_1$ and $P_3$ alternate eating in perfect synchronization, philosopher $P_2$ between them may continually find at least one chopstick busy every time they check, starving indefinitely while the system as a whole makes continuous progress."
    ))

    # Q37: Chandy-Misra hygienic solution (Pattern: Distributed dining philosophers)
    items.append(q(
        37, "dining-philosophers", "MCQ", 2, "HARD",
        "In the Chandy-Misra distributed solution to the dining philosophers problem, chopsticks are labeled as either 'clean' or 'dirty'. How is starvation completely eliminated?",
        options={
            "A": "Philosophers send chopsticks to requesting neighbors only if the chopstick is dirty, cleaning it upon transfer, which ensures acyclic request priority",
            "B": "Chopsticks are duplicated so every philosopher has two private chopsticks",
            "C": "Philosophers are preempted after eating for 1 millisecond",
            "D": "A central coordinator decides eating order via atomic clock timestamps"
        },
        correct_opts=["A"],
        explanation="In the Chandy-Misra algorithm, when a philosopher eats, their chopsticks become dirty. If a neighbor requests a chopstick, the philosopher yields it if it is dirty (cleaning it in the process). A clean chopstick is never yielded until used. This guarantees priority flows acyclically toward hungry philosophers, completely preventing starvation and deadlock."
    ))

    # Q38: MSQ on Dining Philosophers Deadlock Prevention (Pattern: Prevention alternatives MSQ)
    items.append(q(
        38, "dining-philosophers", "MSQ", 2, "MEDIUM",
        "Which of the following modifications to the classic dining philosophers problem will successfully PREVENT deadlock?",
        options={
            "A": "Allow a philosopher to pick up chopsticks only if both chopsticks are simultaneously available (atomic multi-resource acquisition)",
            "B": "Use an asymmetric solution where odd philosophers pick left-first and even philosophers pick right-first",
            "C": "Limit the maximum number of simultaneously seated philosophers to $N - 1$",
            "D": "Require each philosopher to release their first chopstick if the second chopstick cannot be acquired immediately (`trywait` with backoff)"
        },
        correct_opts=["A", "B", "C", "D"],
        explanation="All four are valid deadlock avoidance/prevention strategies: A eliminates Hold and Wait by testing both chopsticks atomically (monitor solution); B eliminates Circular Wait via hierarchical ordering; C breaks the resource-to-process bottleneck; D eliminates No Preemption / Hold and Wait using non-blocking attempts."
    ))

    # Q39: NAT Max concurrent philosophers for general N (Pattern: General formula calculation)
    items.append(q(
        39, "dining-philosophers", "NAT", 2, "MEDIUM",
        "In a dining philosophers table with $N = 10$ philosophers, what is the maximum number of philosophers that can eat concurrently?",
        num_min=5.0, num_max=5.0,
        explanation="For an even number of philosophers $N = 10$, non-adjacent philosophers (e.g. philosophers 0, 2, 4, 6, 8) can eat simultaneously without chopstick contention. Thus, maximum concurrent eating philosophers = $10 / 2 = 5$."
    ))

    # Q40: Tanenbaum Monitor solution state array (Pattern: State tracking monitor)
    items.append(q(
        40, "dining-philosophers", "MCQ", 2, "MEDIUM",
        "In Tanenbaum's monitor-based solution to the dining philosophers, an array `state[5]` tracks each philosopher as `THINKING`, `HUNGRY`, or `EATING`. Under what exact condition is philosopher $i$ allowed to transition to `EATING`?",
        options={
            "A": "`state[i] == HUNGRY && state[(i + 4) % 5] != EATING && state[(i + 1) % 5] != EATING`",
            "B": "`state[i] == THINKING && state[(i + 1) % 5] == EATING`",
            "C": "`state[(i + 4) % 5] == HUNGRY && state[(i + 1) % 5] == HUNGRY`",
            "D": "`state[i] == EATING && state[(i + 4) % 5] == EATING`"
        },
        correct_opts=["A"],
        explanation="Philosopher $i$ can eat if and only if they are `HUNGRY` and neither their left neighbor (`(i + 4) % 5`) nor their right neighbor (`(i + 1) % 5`) is currently in the `EATING` state."
    ))

    # Q41: Livelock in Dining Philosophers (Pattern: Livelock scenario)
    items.append(q(
        41, "dining-philosophers", "MCQ", 2, "HARD",
        "If philosophers pick up their left chopstick, check if the right chopstick is available, and if not, put down the left chopstick and retry after a fixed, identical delay, what failure mode can occur?",
        options={
            "A": "Livelock: all philosophers repeatedly pick up and drop chopsticks in lockstep without making progress",
            "B": "Data corruption of the chopstick pointers",
            "C": "Deadlock where threads are suspended in the operating system wait queue",
            "D": "Priority inversion"
        },
        correct_opts=["A"],
        explanation="If all philosophers act concurrently in lockstep with identical fixed delays, they will all pick up left, see right is busy, drop left, wait, and repeat indefinitely. Because processes are actively running and changing states without making functional progress, this is a classic livelock."
    ))

    # Q42: Resource hierarchy / Dijkstra solution (Pattern: Partial ordering)
    items.append(q(
        42, "dining-philosophers", "MCQ", 1, "EASY",
        "Dijkstra's resource hierarchy solution assigns a global numbering $1, 2, \\dots, N$ to all chopsticks and mandates that every philosopher must acquire the lower-numbered chopstick before requesting the higher-numbered one. Which Coffman condition does this directly eliminate?",
        options={
            "A": "Circular Wait",
            "B": "Mutual Exclusion",
            "C": "Hold and Wait",
            "D": "Preemption"
        },
        correct_opts=["A"],
        explanation="Imposing a strict global total ordering on resource allocation mathematically guarantees that no cycle of requests can exist. Hence, the Circular Wait condition is completely eliminated."
    ))

    # Q43: MSQ Tanenbaum state inspection (Pattern: Monitor invariant MSQ)
    items.append(q(
        43, "dining-philosophers", "MSQ", 2, "HARD",
        "In the monitor solution for 5 dining philosophers maintaining `state[5]`, which of the following global state configurations are VALID (can occur in a correct execution)?",
        options={
            "A": "`state[0] == EATING`, `state[1] == THINKING`, `state[2] == EATING`, `state[3] == THINKING`, `state[4] == THINKING`",
            "B": "`state[0] == EATING`, `state[1] == EATING`, `state[2] == THINKING`, `state[3] == THINKING`, `state[4] == THINKING`",
            "C": "`state[0] == HUNGRY`, `state[1] == HUNGRY`, `state[2] == HUNGRY`, `state[3] == HUNGRY`, `state[4] == HUNGRY`",
            "D": "`state[0] == EATING`, `state[2] == EATING`, `state[4] == EATING`"
        },
        correct_opts=["A", "C"],
        explanation="In A, non-adjacent philosophers 0 and 2 eat simultaneously (valid). In B, adjacent philosophers 0 and 1 eat simultaneously, which violates mutual exclusion on chopstick 1 (invalid). In C, all 5 philosophers can be hungry simultaneously while waiting (valid). In D, philosophers 0, 2, and 4 cannot all eat simultaneously because 0 and 4 are adjacent on a 5-person circular table (invalid)."
    ))

    # Q44: Minimum chopsticks for N eating (Pattern: Pigeonhole bound NAT)
    items.append(q(
        44, "dining-philosophers", "NAT", 2, "MEDIUM",
        "Suppose $K$ philosophers wish to eat simultaneously in a dining philosophers setup. Each philosopher requires 2 chopsticks. What is the minimum total number of chopsticks the system must contain to guarantee that all $K = 4$ philosophers can eat simultaneously?",
        num_min=8.0, num_max=8.0,
        explanation="Since each philosopher requires 2 distinct, mutually exclusive chopsticks, for 4 philosophers to eat concurrently without sharing any chopstick at that moment, the system must contain at least $4 \\times 2 = 8$ chopsticks."
    ))

    # Q45: Signal upon finishing eating (Pattern: Semaphore signaling protocol)
    items.append(q(
        45, "dining-philosophers", "MCQ", 1, "EASY",
        "When philosopher $i$ finishes eating in the classic semaphore solution, in what order should the chopsticks be released?",
        options={
            "A": "Both chopsticks must be signaled (`signal(chopstick[left]); signal(chopstick[right]);`)",
            "B": "Only the left chopstick should be signaled",
            "C": "Only the right chopstick should be signaled",
            "D": "Neither chopstick is signaled until the next philosopher awakens"
        },
        correct_opts=["A"],
        explanation="A philosopher holds two chopstick semaphores while eating. Upon finishing, both semaphores must be signaled so that neighboring philosophers waiting on those chopsticks can be unblocked."
    ))

    # ==========================================
    # 4. monitors-condition-variables (15 Qs: Q46 - Q60)
    # ==========================================

    # Q46: Monitor Mutual Exclusion concept (Pattern: Monitor language construct)
    items.append(q(
        46, "monitors-condition-variables", "MCQ", 1, "EASY",
        "How is mutual exclusion guaranteed inside a monitor in programming languages like Java or Concurrent Pascal?",
        options={
            "A": "The compiler and runtime automatically ensure that only one thread can be executing an active monitor procedure at any given time",
            "B": "Programmers must explicitly write assembly spinlocks at the start of every function",
            "C": "By turning off CPU interrupts permanently",
            "D": "By running all monitor threads on a single core with timeslicing disabled"
        },
        correct_opts=["A"],
        explanation="A monitor is a high-level synchronization construct. The compiler and runtime automatically serialize access, ensuring that only one thread at a time can be actively executing inside any of the monitor's methods or procedures."
    ))

    # Q47: Hoare vs Mesa semantics (Pattern: Signaling semantics)
    items.append(q(
        47, "monitors-condition-variables", "MCQ", 2, "MEDIUM",
        "What is the fundamental difference between Hoare semantics (Signal-and-Wait) and Mesa semantics (Signal-and-Continue) in monitor condition variables?",
        options={
            "A": "Under Hoare semantics, the signaling thread immediately yields the monitor to the awakened thread; under Mesa semantics, the signaler retains the monitor and the awakened thread is placed in the ready queue",
            "B": "Under Mesa semantics, condition variables have integer counters like semaphores",
            "C": "Under Hoare semantics, condition variables cannot be signaled more than once",
            "D": "Under Mesa semantics, mutual exclusion inside the monitor is disabled"
        },
        correct_opts=["A"],
        explanation="Under Hoare semantics (Signal-and-Wait), when thread A signals condition variable `x`, the awakened thread B immediately acquires the monitor while thread A waits. Under Mesa semantics (Signal-and-Continue, used in Java/POSIX), thread A continues running to completion or yield, while thread B moves to the ready/entry queue."
    ))

    # Q48: While loop recheck necessity (Pattern: Mesa while loop requirement)
    items.append(q(
        48, "monitors-condition-variables", "MCQ", 2, "MEDIUM",
        "Why is it mandatory in Mesa-style monitors (such as Java `synchronized` methods with `wait()`) to check condition predicates in a `while` loop rather than an `if` statement:\n```java\nwhile (!condition) {\n    wait();\n}\n```?",
        options={
            "A": "Because another thread may acquire the monitor and alter the condition predicate before the awakened thread gets scheduled to run",
            "B": "Because `if` statements are not supported inside synchronized methods in Java",
            "C": "Because `wait()` always throws a compilation error if placed inside an `if` block",
            "D": "Because Mesa monitors execute threads in reverse chronological order"
        },
        correct_opts=["A"],
        explanation="Under Mesa semantics, between the time a thread is awakened by `signal()/notify()` and the time it actually re-acquires the monitor lock and executes, another thread may enter the monitor and invalidate the condition. Testing in a `while` loop re-verifies the predicate and guards against spurious wakeups."
    ))

    # Q49: Semaphore vs Condition Variable difference (Pattern: Comparison MSQ)
    items.append(q(
        49, "monitors-condition-variables", "MSQ", 2, "MEDIUM",
        "Which of the following statements correctly distinguish a condition variable in a monitor from a classic counting semaphore?",
        options={
            "A": "A condition variable has no integer value or history; a signal on a condition variable with no waiting threads is lost",
            "B": "A signal (V operation) on a semaphore is remembered by incrementing the semaphore counter",
            "C": "`wait()` on a condition variable always blocks the calling thread, whereas `wait()` on a semaphore only blocks if the counter is <= 0",
            "D": "Semaphores cannot be used outside of monitors"
        },
        correct_opts=["A", "B", "C"],
        explanation="A, B, and C are correct fundamental distinctions. Condition variables are memoryless synchronization points: if no thread is waiting on `c`, `c.signal()` has no effect (is lost). A semaphore increments its counter and remembers the signal. `c.wait()` unconditionally suspends the caller and releases the monitor lock. D is false: semaphores are standalone primitives."
    ))

    # Q50: Monitor Queue counting NAT (Pattern: Queue state tracking NAT)
    items.append(q(
        50, "monitors-condition-variables", "NAT", 2, "HARD",
        "In a Mesa-style monitor, thread $T_1$ enters and calls `cond.wait()`. Threads $T_2$ and $T_3$ also enter sequentially and each call `cond.wait()`. Thread $T_4$ then enters the monitor and executes two consecutive `cond.signal()` calls, but does not exit the monitor yet. At this exact moment, how many threads are in the condition variable's waiting queue?",
        num_min=1.0, num_max=1.0,
        explanation="Initially 3 threads ($T_1, T_2, T_3$) are in `cond`'s wait queue. When $T_4$ executes two `cond.signal()` calls, exactly two threads are removed from the condition variable queue and moved to the monitor entry/ready queue. Thus, $3 - 2 = 1$ thread remains in `cond`'s wait queue."
    ))

    # Q51: Spurious wakeups (Pattern: Modern threading concepts)
    items.append(q(
        51, "monitors-condition-variables", "MCQ", 1, "EASY",
        "What is a 'spurious wakeup' in thread synchronization with condition variables?",
        options={
            "A": "A thread is awakened from `wait()` even though no thread explicitly called `signal()` or `broadcast()`",
            "B": "A thread wakes up and immediately crashes the operating system",
            "C": "A hardware interrupt causes an infinite loop in the monitor",
            "D": "The compiler replaces the thread with a daemon thread"
        },
        correct_opts=["A"],
        explanation="A spurious wakeup occurs when a waiting thread unblocks from a condition variable without any explicit signal, notify, interrupt, or timeout. This is permitted by operating system thread implementations (like POSIX and Java) for performance reasons, necessitating the use of `while` loop predicate re-checks."
    ))

    # Q52: Nested monitor lockout (Pattern: Architectural hazards)
    items.append(q(
        52, "monitors-condition-variables", "MCQ", 2, "HARD",
        "What is the 'nested monitor lockout' problem?",
        options={
            "A": "A thread holding lock $M_1$ calls a method in monitor $M_2$, where it waits on condition variable $C$. $C.wait()$ releases lock $M_2$ but retains lock $M_1$, blocking any other thread that needs $M_1$ to signal $C$",
            "B": "A monitor nested inside another monitor causes stack overflow during compilation",
            "C": "Two monitors cannot exist in the same Java package",
            "D": "A monitor locks all physical CPU cores permanently"
        },
        correct_opts=["A"],
        explanation="Nested monitor lockout occurs when thread $T$ acquires outer monitor $M_1$, calls inner monitor $M_2$, and blocks on `wait()` in $M_2$. `wait()` releases $M_2$ but does not release outer lock $M_1$. If the thread that can signal the condition must pass through outer monitor $M_1$, it is blocked, resulting in permanent deadlock."
    ))

    # Q53: Broadcast vs Signal MSQ (Pattern: Condition signaling semantics MSQ)
    items.append(q(
        53, "monitors-condition-variables", "MSQ", 2, "MEDIUM",
        "Which of the following scenarios specifically require `broadcast()` (or Java `notifyAll()`) instead of a single `signal()` (or `notify()`) on a condition variable?",
        options={
            "A": "When multiple waiting threads have different condition predicates (e.g., requesting different amounts of memory in a memory manager)",
            "B": "When a single condition change could satisfy multiple waiting threads simultaneously (e.g., readers in a readers-writers monitor)",
            "C": "When exactly one thread is waiting and its predicate is guaranteed to be satisfied",
            "D": "When all waiting threads are identical and exactly one item is produced in a bounded buffer"
        },
        correct_opts=["A", "B"],
        explanation="`broadcast()/notifyAll()` is necessary when awakened threads have varying resource requirements (A) or multiple threads can proceed concurrently (B, like multiple readers). If single `signal()` is used, the runtime might awaken a thread whose predicate is not met, which goes back to sleep while threads that could have proceeded remain blocked. For C and D, single `signal()` is sufficient."
    ))

    # Q54: Active threads in monitor NAT (Pattern: Invariant NAT)
    items.append(q(
        54, "monitors-condition-variables", "NAT", 1, "EASY",
        "In a properly functioning monitor with 8 concurrent threads executing monitor procedures, what is the maximum number of threads actively executing instructions inside the monitor at any single clock cycle on a single-core CPU?",
        num_min=1.0, num_max=1.0,
        explanation="The fundamental invariant of a monitor is mutual exclusion: at most 1 thread can be actively executing inside the monitor at any instant."
    ))

    # Q55: Hoare urgent queue (Pattern: Hoare scheduling details)
    items.append(q(
        55, "monitors-condition-variables", "MCQ", 2, "HARD",
        "In Hoare's original monitor implementation, where does the signaling thread wait after executing `signal(c)` if an awakened thread takes immediate control of the monitor?",
        options={
            "A": "In the 'urgent queue' (or signal queue), which has higher priority for re-entering the monitor than the regular entry queue",
            "B": "At the tail of the main monitor entry queue",
            "C": "In the condition variable queue `c`",
            "D": "It is terminated by the operating system"
        },
        correct_opts=["A"],
        explanation="In Hoare's monitor design, when thread A signals condition `c`, thread A yields immediately and enters the 'urgent queue'. When the awakened thread leaves or waits, threads on the urgent queue have priority over newly arriving threads on the external entry queue, preserving Hoare semantics."
    ))

    # Q56: Java ReentrantLock and Condition (Pattern: Modern API mapping)
    items.append(q(
        56, "monitors-condition-variables", "MCQ", 1, "EASY",
        "In Java's `java.util.concurrent.locks` framework, which method on a `Condition` object corresponds to POSIX `pthread_cond_wait()`?",
        options={
            "A": "`await()`",
            "B": "`sleep()`",
            "C": "`yield()`",
            "D": "`suspend()`"
        },
        correct_opts=["A"],
        explanation="In Java, `Condition.await()` causes the current thread to wait until it is signaled or interrupted, atomically releasing the associated `ReentrantLock`."
    ))

    # Q57: MSQ Monitor programming invariants (Pattern: Theoretical properties MSQ)
    items.append(q(
        57, "monitors-condition-variables", "MSQ", 2, "HARD",
        "Which of the following statements about monitors and condition variables are TRUE?",
        options={
            "A": "A condition variable must always be associated with a mutex or monitor lock",
            "B": "Calling `wait()` on a condition variable without holding the associated monitor lock typically throws an illegal monitor state exception or undefined behavior",
            "C": "Condition variables maintain a state history across process restarts",
            "D": "Monitors completely prevent all deadlocks automatically without programmer effort"
        },
        correct_opts=["A", "B"],
        explanation="A is TRUE: condition variables rely on an enclosing lock to maintain predicate atomicity. B is TRUE: releasing the lock during `wait()` requires holding it first; attempting to wait without the lock throws `IllegalMonitorStateException`. C is FALSE: condition variables are in-memory primitives. D is FALSE: programmers can still cause deadlocks by ordering monitor calls circular-wise."
    ))

    # Q58: Signal without waiters NAT (Pattern: Counter behavior NAT)
    items.append(q(
        58, "monitors-condition-variables", "NAT", 2, "MEDIUM",
        "In a Mesa monitor, condition variable `cv` currently has 0 waiting threads. A thread enters the monitor, executes `cv.signal()` 5 times consecutively, and exits. Subsequently, two new threads arrive and each executes `cv.wait()`. How many threads will be blocked in the `cv` queue after this sequence?",
        num_min=2.0, num_max=2.0,
        explanation="Unlike semaphores, condition variables have no memory or counter. The 5 `cv.signal()` operations executed when no threads were waiting are lost (no-op). When the 2 new threads execute `cv.wait()`, both block unconditionally. Thus, 2 threads are blocked."
    ))

    # Q59: Monitor Entry Queue vs Wait Queue (Pattern: Structural queuing)
    items.append(q(
        59, "monitors-condition-variables", "MCQ", 1, "EASY",
        "In a monitor implementation with multiple condition variables $C_1, C_2$, how many distinct waiting queues are managed by the monitor subsystem?",
        options={
            "A": "One entry queue for the monitor lock, plus one dedicated queue for each condition variable",
            "B": "Exactly one shared queue for everything",
            "C": "Zero queues, because monitors use spinlocks",
            "D": "One queue per CPU core regardless of condition variables"
        },
        correct_opts=["A"],
        explanation="A monitor manages one primary entry queue where threads wait to acquire the mutual exclusion lock, plus a separate, dedicated waiting queue for each defined condition variable ($C_1, C_2, \\dots$)."
    ))

    # Q60: Priority condition variable (Pattern: Real-time monitor extension)
    items.append(q(
        60, "monitors-condition-variables", "MCQ", 2, "MEDIUM",
        "Some monitor specifications support a conditional wait with an integer priority parameter `c.wait(priority)`. When `c.signal()` is executed, which thread is awakened?",
        options={
            "A": "The thread that called `wait()` with the smallest (or highest) priority number",
            "B": "The thread with the largest thread ID",
            "C": "A randomly chosen thread",
            "D": "All threads simultaneously"
        },
        correct_opts=["A"],
        explanation="In a priority condition wait (such as in Concurrent Pascal), threads are queued in order of their specified priority values. When `signal()` is invoked, the thread with the highest priority (often indicated by the smallest integer value) is awakened first."
    ))

    return items
