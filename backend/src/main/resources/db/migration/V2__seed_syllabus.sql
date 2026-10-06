-- V2: Seed GATE CSE Subjects and Core Topics

INSERT INTO subjects (id, name, slug, cluster, avg_weightage, priority_tier, standard_book_ref) VALUES
('subj_aptitude', 'General Aptitude', 'general-aptitude', 'MATH_APTITUDE', 15.0, 1, 'RS Aggarwal / Wren & Martin'),
('subj_discrete_math', 'Engineering & Discrete Mathematics', 'discrete-mathematics', 'MATH_APTITUDE', 14.0, 1, 'Kenneth Rosen / Erwin Kreyszig'),
('subj_prog_ds', 'Programming & Data Structures', 'programming-and-ds', 'CONCEPTUAL_CORE', 11.0, 1, 'Kernighan & Ritchie / CLRS'),
('subj_algo', 'Algorithms', 'algorithms', 'CONCEPTUAL_CORE', 9.0, 1, 'Thomas H. Cormen (CLRS)'),
('subj_toc', 'Theory of Computation', 'theory-of-computation', 'CONCEPTUAL_CORE', 9.0, 1, 'Michael Sipser'),
('subj_os', 'Operating Systems', 'operating-systems', 'SYSTEMS_CORE', 8.0, 2, 'Silberschatz, Galvin & Gagne'),
('subj_dbms', 'Databases (DBMS)', 'databases', 'SYSTEMS_CORE', 8.0, 2, 'Silberschatz (Database System Concepts)'),
('subj_cn', 'Computer Networks', 'computer-networks', 'SYSTEMS_CORE', 8.0, 2, 'Kurose & Ross / Forouzan'),
('subj_coa', 'Computer Organization & Architecture', 'computer-organization', 'SYSTEMS_CORE', 8.0, 2, 'Patterson & Hennessy / Carl Hamacher'),
('subj_dld_cd', 'Digital Logic & Compiler Design', 'digital-and-compilers', 'SYSTEMS_CORE', 9.0, 2, 'Morris Mano / Aho & Ullman');

-- Core Atomic Topics
INSERT INTO topics (id, subject_id, name, slug, book_chapter_ref, mastery_level) VALUES
-- Discrete Math
('topic_dm_prop_logic', 'subj_discrete_math', 'Propositional & Predicate Logic', 'propositional-logic', 'Rosen §1.1-§1.6', 0),
('topic_dm_combinatorics', 'subj_discrete_math', 'Combinatorics & Generating Functions', 'combinatorics', 'Rosen §6.1-§6.5', 0),
('topic_dm_graph_theory', 'subj_discrete_math', 'Graph Theory & Trees', 'graph-theory', 'Rosen §10.1-§10.8', 0),
('topic_dm_relations_posets', 'subj_discrete_math', 'Relations, Functions & Lattices (POSETs)', 'relations-posets', 'Rosen §9.1-§9.6', 0),
('topic_em_linear_algebra', 'subj_discrete_math', 'Linear Algebra (Eigenvalues, Ranks)', 'linear-algebra', 'Kreyszig §7.1-§8.5', 0),
('topic_em_probability', 'subj_discrete_math', 'Probability & Distributions (Bayes, Poisson)', 'probability', 'Kreyszig §24.1-§24.8', 0),

-- Programming & Data Structures
('topic_prog_c_pointers', 'subj_prog_ds', 'C Pointers, Memory & Structs', 'c-pointers', 'K&R Chapter 5', 0),
('topic_prog_recursion', 'subj_prog_ds', 'Recursion & Call Stack Tracing', 'recursion-stacks', 'K&R Chapter 4', 0),
('topic_ds_trees_bst', 'subj_prog_ds', 'Binary Trees & BSTs', 'binary-trees-bst', 'CLRS Chapter 12', 0),
('topic_ds_heaps_hash', 'subj_prog_ds', 'Heaps & Hashing', 'heaps-hashing', 'CLRS Chapters 6, 11', 0),

-- Algorithms
('topic_algo_asymptotic', 'subj_algo', 'Asymptotic Notation & Recurrences', 'asymptotic-analysis', 'CLRS Chapters 3, 4', 0),
('topic_algo_divide_conquer', 'subj_algo', 'Divide & Conquer, Sorting Algorithms', 'divide-and-conquer', 'CLRS Chapters 7, 8', 0),
('topic_algo_greedy_dp', 'subj_algo', 'Greedy Techniques & Dynamic Programming', 'greedy-dp', 'CLRS Chapters 15, 16', 0),
('topic_algo_graph_search', 'subj_algo', 'Graph Algorithms (BFS/DFS, Dijkstra, MST)', 'graph-algorithms', 'CLRS Chapters 22, 23, 24', 0),

-- Theory of Computation
('topic_toc_regular_dfa', 'subj_toc', 'Regular Languages, DFA/NFA Minimization', 'regular-languages-dfa', 'Sipser Chapter 1', 0),
('topic_toc_cfg_pda', 'subj_toc', 'Context-Free Grammars & Pushdown Automata', 'cfg-pda', 'Sipser Chapter 2', 0),
('topic_toc_turing_decidability', 'subj_toc', 'Turing Machines & Undecidability (Halting)', 'turing-decidability', 'Sipser Chapters 3, 4', 0),

-- Operating Systems
('topic_os_process_sync', 'subj_os', 'Process Synchronization (Semaphores, Mutex)', 'process-synchronization', 'Galvin Chapter 6, 7', 0),
('topic_os_cpu_scheduling', 'subj_os', 'CPU Scheduling Algorithms', 'cpu-scheduling', 'Galvin Chapter 5', 0),
('topic_os_deadlocks', 'subj_os', 'Deadlocks (Banker''s Algorithm, Prevention)', 'deadlocks', 'Galvin Chapter 8', 0),
('topic_os_virtual_memory', 'subj_os', 'Paging, TLB, Page Replacement & Virtual Memory', 'virtual-memory-paging', 'Galvin Chapter 9, 10', 0),

-- Databases (DBMS)
('topic_dbms_relational_alg', 'subj_dbms', 'Relational Algebra & Tuple Relational Calculus', 'relational-algebra', 'Silberschatz Chapter 6', 0),
('topic_dbms_sql', 'subj_dbms', 'Complex SQL Queries & Aggregations', 'sql-queries', 'Silberschatz Chapter 3, 4', 0),
('topic_dbms_normalization', 'subj_dbms', 'Functional Dependencies & Normalization (BCNF/3NF)', 'normalization', 'Silberschatz Chapter 7', 0),
('topic_dbms_transactions', 'subj_dbms', 'Transactions, Serializability & Concurrency (2PL)', 'transactions-concurrency', 'Silberschatz Chapters 14, 15', 0),

-- Computer Networks
('topic_cn_ip_addressing', 'subj_cn', 'IPv4/IPv6 Addressing, Subnetting & CIDR', 'ip-subnetting', 'Kurose & Ross Chapter 4', 0),
('topic_cn_flow_control', 'subj_cn', 'Data Link Protocols (Stop & Wait, Go-Back-N, SR)', 'sliding-window-protocols', 'Kurose & Ross Chapter 3, 6', 0),
('topic_cn_tcp_congestion', 'subj_cn', 'TCP Congestion Control, 3-Way Handshake & Flow', 'tcp-flow-congestion', 'Kurose & Ross Chapter 3', 0),
('topic_cn_routing', 'subj_cn', 'Routing Algorithms (Distance Vector, Link State)', 'routing-algorithms', 'Kurose & Ross Chapter 5', 0),

-- Computer Organization & Architecture
('topic_coa_cache_mapping', 'subj_coa', 'Cache Memory Mapping (Direct, Associative, Set-Assoc)', 'cache-memory', 'Hamacher Chapter 8', 0),
('topic_coa_pipelining', 'subj_coa', 'Instruction Pipelining, Branch & Data Hazards', 'instruction-pipelining', 'Hamacher Chapter 6', 0),
('topic_coa_addressing_modes', 'subj_coa', 'Instruction Formats & Addressing Modes', 'addressing-modes', 'Hamacher Chapter 2', 0),

-- Digital Logic & Compilers
('topic_dld_kmaps_counters', 'subj_dld_cd', 'Boolean Algebra, K-Maps, Multiplexers & Counters', 'kmaps-counters', 'Mano Chapters 3, 5, 6', 0),
('topic_cd_parsing', 'subj_dld_cd', 'Lexical Analysis, LL(1), LR Parsers & SDT', 'parsers-sdt', 'Dragon Book Chapters 3, 4, 5', 0);
