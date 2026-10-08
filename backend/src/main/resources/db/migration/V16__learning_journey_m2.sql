-- Migration V16: Learning Journey M2 — topic ordering + topic primers (PHASE7-M2)

-- 1. Roadmap ordering (display order ONLY — PC-1: never a gate)
ALTER TABLE topics ADD COLUMN sequence INT NOT NULL DEFAULT 0;
CREATE INDEX idx_topics_subject_sequence ON topics(subject_id, sequence);

-- 2. Curated backfill: deterministic per-subject order 1..N following the standard GATE teaching sequence
-- General Aptitude
UPDATE topics SET sequence = 1 WHERE id = 'topic_apt_verbal';
UPDATE topics SET sequence = 2 WHERE id = 'topic_apt_quantitative';

-- Engineering & Discrete Mathematics
UPDATE topics SET sequence = 1 WHERE id = 'topic_dm_prop_logic';
UPDATE topics SET sequence = 2 WHERE id = 'topic_dm_relations_posets';
UPDATE topics SET sequence = 3 WHERE id = 'topic_dm_combinatorics';
UPDATE topics SET sequence = 4 WHERE id = 'topic_dm_graph_theory';
UPDATE topics SET sequence = 5 WHERE id = 'topic_em_linear_algebra';
UPDATE topics SET sequence = 6 WHERE id = 'topic_em_probability';

-- Programming & Data Structures
UPDATE topics SET sequence = 1 WHERE id = 'topic_prog_c_pointers';
UPDATE topics SET sequence = 2 WHERE id = 'topic_prog_recursion';
UPDATE topics SET sequence = 3 WHERE id = 'topic_ds_trees_bst';
UPDATE topics SET sequence = 4 WHERE id = 'topic_ds_heaps_hash';

-- Algorithms
UPDATE topics SET sequence = 1 WHERE id = 'topic_algo_asymptotic';
UPDATE topics SET sequence = 2 WHERE id = 'topic_algo_divide_conquer';
UPDATE topics SET sequence = 3 WHERE id = 'topic_algo_greedy_dp';
UPDATE topics SET sequence = 4 WHERE id = 'topic_algo_graph_search';

-- Theory of Computation
UPDATE topics SET sequence = 1 WHERE id = 'topic_toc_regular_dfa';
UPDATE topics SET sequence = 2 WHERE id = 'topic_toc_cfg_pda';
UPDATE topics SET sequence = 3 WHERE id = 'topic_toc_turing_decidability';

-- Operating Systems
UPDATE topics SET sequence = 1 WHERE id = 'topic_os_cpu_scheduling';
UPDATE topics SET sequence = 2 WHERE id = 'topic_os_process_sync';
UPDATE topics SET sequence = 3 WHERE id = 'topic_os_deadlocks';
UPDATE topics SET sequence = 4 WHERE id = 'topic_os_virtual_memory';

-- Databases (DBMS)
UPDATE topics SET sequence = 1 WHERE id = 'topic_dbms_relational_alg';
UPDATE topics SET sequence = 2 WHERE id = 'topic_dbms_sql';
UPDATE topics SET sequence = 3 WHERE id = 'topic_dbms_normalization';
UPDATE topics SET sequence = 4 WHERE id = 'topic_dbms_transactions';

-- Computer Networks
UPDATE topics SET sequence = 1 WHERE id = 'topic_cn_flow_control';
UPDATE topics SET sequence = 2 WHERE id = 'topic_cn_ip_addressing';
UPDATE topics SET sequence = 3 WHERE id = 'topic_cn_routing';
UPDATE topics SET sequence = 4 WHERE id = 'topic_cn_tcp_congestion';

-- Computer Organization & Architecture
UPDATE topics SET sequence = 1 WHERE id = 'topic_coa_addressing_modes';
UPDATE topics SET sequence = 2 WHERE id = 'topic_coa_pipelining';
UPDATE topics SET sequence = 3 WHERE id = 'topic_coa_cache_mapping';

-- Digital Logic & Compiler Design
UPDATE topics SET sequence = 1 WHERE id = 'topic_dld_kmaps_counters';
UPDATE topics SET sequence = 2 WHERE id = 'topic_cd_parsing';

-- Fallback to guarantee all existing topics have sequence >= 1
UPDATE topics SET sequence = 1 WHERE sequence = 0;

-- 3. Topic-level theory primer (one per topic, global content; M1 concept-unit shape)
CREATE TABLE topic_primers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id        VARCHAR(100) NOT NULL UNIQUE REFERENCES topics(id) ON DELETE CASCADE,
    motivation      TEXT NOT NULL,
    core_idea       TEXT NOT NULL,
    invariant       TEXT,
    worked_example  TEXT NOT NULL,          -- markdown + KaTeX; PC-1 binding (>=150 words, numbered steps)
    traps           TEXT,                   -- markdown
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT','REVIEWED','ACCEPTED','REJECTED')),
    parse_error     TEXT,
    generator_model VARCHAR(100),
    generated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_topic_primers_status ON topic_primers(status);
