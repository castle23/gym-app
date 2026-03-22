# Advanced Database Architecture

## Overview

Deep-dive into the advanced PostgreSQL architecture for the Gym Platform, covering system internals, memory management, process architecture, storage engine, replication mechanisms, and advanced configuration. This guide is for experienced DBAs needing to understand low-level database internals and make informed infrastructure decisions.

## Table of Contents

- [System Architecture](#system-architecture)
- [Memory Architecture](#memory-architecture)
- [Process Architecture](#process-architecture)
- [Storage Architecture](#storage-architecture)
- [Buffer Management](#buffer-management)
- [Replication Architecture](#replication-architecture)
- [Write-Ahead Log (WAL)](#write-ahead-log-wal)
- [Vacuum Architecture](#vacuum-architecture)
- [Transaction Isolation](#transaction-isolation)
- [Advanced Configuration](#advanced-configuration)

---

## System Architecture

### PostgreSQL Architecture Overview

```
┌────────────────────────────────────────────────┐
│         PostgreSQL Database Server              │
├────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │      Shared Memory Segment                │  │
│  ├──────────────────────────────────────────┤  │
│  │ - Buffer Pool                             │  │
│  │ - Shared Buffers                          │  │
│  │ - WAL Buffer                              │  │
│  │ - Lock Manager                            │  │
│  │ - Transaction Coordinator                 │  │
│  └──────────────────────────────────────────┘  │
│                     △                          │
│                     │                          │
│          ┌──────────┴──────────┐               │
│          │                     │               │
│      ┌───┴────┐          ┌────┴────┐          │
│      │ Backend│          │ Autovac  │          │
│      │Process 1           │Process   │         │
│      └────┬───┘          └────┬────┘          │
│           │                   │               │
│      ┌────┴─────────────────┬─┴───┐           │
│      │                      │     │           │
│  ┌───▼───┐          ┌──────▼──┐  │           │
│  │ Local │          │  WAL    │  │           │
│  │Buffer │          │ Archiver   │           │
│  │(Ring) │          │        │   │           │
│  └───────┘          └────────┘   │           │
│                                   │           │
└───────────────────────────────────┼───────────┘
                                    │
                         ┌──────────▼──────────┐
                         │   Data Files        │
                         │   WAL Files         │
                         │   Index Files       │
                         └─────────────────────┘
```

### Layered Architecture

**Application Layer:**
- Client applications
- Connection pooling (PgBouncer)
- Connection management

**SQL Interface Layer:**
- SQL parser
- Query optimizer
- Query executor

**Storage Engine Layer:**
- Buffer manager
- Page manager
- Transaction manager

**Physical Storage Layer:**
- Data files (heap, indexes)
- WAL files
- Checkpoint files

---

## Memory Architecture

### Shared Memory

**Shared Buffers (shared_buffers parameter):**

```ini
# Calculate correct size
# Formula: 25% of available RAM for dedicated server
# Example: 64GB server -> 16GB shared_buffers

shared_buffers = 16GB

# Contains:
# - Buffer pool for data pages
# - Index pages
# - Query work areas
# - Lock information
```

**Effects of shared_buffers:**

```
Too Low (< 4GB):
- Frequent disk I/O
- Poor cache hit ratio
- Slower queries

Optimal (25% RAM):
- High cache hit ratio (> 99%)
- Reduced disk I/O
- Better performance

Too High (> 50% RAM):
- OS has insufficient memory
- Memory pressure increases
- Potential swap usage
```

**Monitoring shared buffers:**

```sql
-- Check buffer pool hit ratio
SELECT
    sum(heap_blks_read) as heap_read,
    sum(heap_blks_hit) as heap_hit,
    ROUND(100.0 * sum(heap_blks_hit) /
        (sum(heap_blks_hit) + sum(heap_blks_read)), 2) as ratio
FROM pg_statio_user_tables;

-- Target: > 99% hit ratio

-- Check buffer pool usage
SELECT
    buffers_clean,
    maxwritten_clean,
    buffers_backend_fsync
FROM pg_stat_bgwriter;
```

### Work Memory

**work_mem parameter:**

```ini
# Per-operation memory for sorts, hash tables, etc.
# Formula: (RAM - shared_buffers) / (max_connections * 3)
# Example: (64GB - 16GB) / (200 * 3) = ~400MB

work_mem = 400MB

# Used by:
# - Hash joins
# - Merge joins
# - Hash aggregations
# - Sort operations
```

**Impact of work_mem:**

```
Too Low (< 10MB):
- Spills to disk frequently
- Slow sorts and hash operations
- Heavy I/O usage

Optimal:
- Sorts in memory
- Fast aggregations
- Minimal disk spill

Too High:
- Multiple operations exceed available memory
- Memory pressure
- Potential system swap
```

### Maintenance Memory

**maintenance_work_mem parameter:**

```ini
# Memory for maintenance operations
# Formula: 5-10% of available RAM
# Can be higher than work_mem

maintenance_work_mem = 2GB

# Used by:
# - VACUUM
# - ANALYZE
# - CREATE INDEX
# - REINDEX
# - REPAIR
```

### WAL Buffer Memory

**wal_buffers parameter:**

```ini
# Write-Ahead Log buffer size
# Typical: 16MB (usually sufficient)
# Formula: log2(wal_buffers) * 8KB

wal_buffers = 16MB  # 2048 pages of 8KB each

# Contains:
# - Uncommitted transaction logs
# - Allows batching of WAL writes
```

---

## Process Architecture

### Backend Process Structure

```
Backend Process Memory Layout:
┌──────────────────────────────┐
│  Stack                       │  (5-10 MB)
│  - Local variables           │
│  - Function calls            │
│  - Exception frames          │
├──────────────────────────────┤
│  Heap                        │  (10-20 MB)
│  - Parsed query tree         │
│  - Query plan                │
│  - Query result              │
│  - Transaction state         │
├──────────────────────────────┤
│  Backend Private Memory      │
│  - work_mem allocations      │
│  - Buffer pool (for this     │
│    session)                  │
│  - Temp tables               │
└──────────────────────────────┘
```

### Process Lifecycle

```
1. Connection Received
   ├─ Client connects to postmaster (main process)
   └─ Port 5432 listening

2. Backend Process Forked
   ├─ Postmaster forks new backend
   ├─ Process gets unique PID
   └─ Inherits connection socket

3. Authentication
   ├─ Username verified
   ├─ Password/certificate checked
   └─ Database selected

4. Transaction Processing
   ├─ Accept SQL commands
   ├─ Parse and plan
   ├─ Execute
   └─ Return results

5. Connection Closed
   ├─ Resources released
   ├─ Process terminates
   └─ Socket closed
```

### Monitoring Processes

```sql
-- List all backend processes
SELECT
    pid,
    usename,
    application_name,
    state,
    query_start,
    state_change,
    query
FROM pg_stat_activity
ORDER BY query_start;

-- Find long-running queries
SELECT
    pid,
    usename,
    EXTRACT(EPOCH FROM (NOW() - query_start)) as duration_seconds,
    query
FROM pg_stat_activity
WHERE state != 'idle'
AND query_start < NOW() - INTERVAL '5 minutes'
ORDER BY query_start;

-- Connection limits
SELECT
    datname,
    usename,
    count(*) as conn_count
FROM pg_stat_activity
GROUP BY datname, usename
ORDER BY conn_count DESC;
```

---

## Storage Architecture

### Page Structure

```
PostgreSQL Page (8KB default):
┌─────────────────────────────────┐
│ Page Header (24 bytes)          │
│ - Page version                  │
│ - Flags (all visible, etc.)     │
│ - Free space info               │
│ - Tuple count                   │
├─────────────────────────────────┤
│ Item Pointers                   │
│ - Offset/length pairs           │
│ - Growing downward from header  │
├─────────────────────────────────┤
│  (Free Space)                   │
├─────────────────────────────────┤
│ Tuples (Rows)                   │
│ - Growing upward from end       │
│ - Variable length               │
└─────────────────────────────────┘

Total: 8192 bytes (8KB)
Overhead: ~24 bytes header
Maximum row: ~8000 bytes
```

### Heap File Organization

```
Table File Layout:
┌─────────────┐
│ Page 0      │ ← First 8KB page (page 1 of file)
├─────────────┤
│ Page 1      │
├─────────────┤
│ Page 2      │
├─────────────┤
│ ...         │
├─────────────┤
│ Page N      │
└─────────────┘

Row Identification:
- Block number (page number)
- Offset within block
- Row ID: (Block, Offset)
- Example: (100, 42) = 100th page, 42nd tuple
```

### Tuple Structure

```
Tuple Header:
┌──────────────────────┐
│ XMin (4 bytes)       │ Inserting transaction ID
├──────────────────────┤
│ XMax (4 bytes)       │ Deleting transaction ID
├──────────────────────┤
│ CTID (6 bytes)       │ Current tuple ID
├──────────────────────┤
│ Flags (2 bytes)      │ Updated, HOT, etc.
├──────────────────────┤
│ Null Bitmap          │ Which fields are NULL
├──────────────────────┤
│ Data                 │ Actual column values
└──────────────────────┘

XMin/XMax Track:
- Transaction that inserted row
- Transaction that deleted row
- Multi-version concurrency control (MVCC)
```

---

## Buffer Management

### Buffer Pool Operations

```
Query Execution:
1. Query requests page
2. Buffer manager checks cache
   ├─ Hit: Return from buffer
   └─ Miss: Read from disk

Buffer Miss Process:
1. Find empty buffer (or evict LRU)
2. Read page from disk
3. Cache the page
4. Return to query

Write Process:
1. Query modifies page in buffer
2. Mark buffer as dirty
3. Background writer flushes periodically
4. WAL ensures durability
```

### LRU (Least Recently Used) Eviction

```
Buffer Pool Ring:
┌─────────────────────────────┐
│ Most Recently Used (Head)   │ ← New queries access here
│ - Frequently accessed       │
│ - Stays in cache longer     │
├─────────────────────────────┤
│ Recently Used               │
├─────────────────────────────┤
│ Least Recently Used (Tail)  │ ← First to be evicted
│ - Not accessed recently     │
└─────────────────────────────┘

When Cache Full:
- Evict LRU page
- If dirty, flush to disk first
- Add new page at head
```

### Background Writer

```sql
-- BGWriter settings
shared_buffers = 16GB           # Size of buffer pool
bg_writer_delay = 200ms         # Check every 200ms
bg_writer_lru_maxpages = 100    # Max pages per round
bg_writer_lru_multiplier = 2.0  # Multiplier for prediction

-- Monitor BGWriter activity
SELECT
    checkpoints_timed,
    checkpoints_req,
    checkpoint_write_time,
    checkpoint_sync_time,
    buffers_checkpoint,
    buffers_clean,
    maxwritten_clean,
    buffers_backend_fsync
FROM pg_stat_bgwriter;
```

---

## Replication Architecture

### Streaming Replication Process

```
Primary Server:
┌─────────────────────────────┐
│ Write Transaction          │
├─────────────────────────────┤
│ Write to WAL                │
│ (pg_xlog or pg_wal)        │
├─────────────────────────────┤
│ Flush to Disk               │
├─────────────────────────────┤
│ Replication Slot            │
│ (Ensures replica doesn't    │
│  fall behind)              │
├─────────────────────────────┤
│ Send to Replica             │
│ (via network replication)  │
└─────────────────────────────┘
         │
         │ Network
         ▼
Replica Server:
┌─────────────────────────────┐
│ Receive WAL                 │
├─────────────────────────────┤
│ Apply to Standby            │
│ (Replay transactions)       │
├─────────────────────────────┤
│ Flush to Disk               │
├─────────────────────────────┤
│ Ready for Read/Promote      │
└─────────────────────────────┘
```

### Synchronous vs Asynchronous

**Synchronous Replication:**

```
1. Client issues transaction
2. Primary applies locally
3. Primary waits for replica(s) to confirm
   ├─ Replica receives WAL
   ├─ Replica applies (or just receives)
   └─ Replica sends confirmation
4. Primary commits to client
5. Guaranteed durability

Trade-off: Latency for durability
```

**Asynchronous Replication:**

```
1. Client issues transaction
2. Primary applies locally
3. Primary commits immediately
4. Primary sends to replica asynchronously
5. Replica catches up eventually

Trade-off: Throughput for potential data loss
```

### Replication Slots

```sql
-- View replication slots
SELECT * FROM pg_replication_slots;

-- Create slot (keeps WAL)
SELECT * FROM pg_create_physical_replication_slot('replica1');

-- Drop slot (allows WAL cleanup)
SELECT pg_drop_replication_slot('replica1');

-- Monitor slot lag
SELECT
    slot_name,
    slot_type,
    active,
    restart_lsn,
    confirmed_flush_lsn
FROM pg_replication_slots;
```

---

## Write-Ahead Log (WAL)

### WAL Segment Structure

```
WAL File Name: 000000010000000000000001
├─ 8 Hex Digits: Timeline ID (00000001)
├─ 8 Hex Digits: High part of log file (00000000)
└─ 8 Hex Digits: Low part of log file (00000001)

File Size: 16MB by default (wal_segment_size)

WAL Record Format:
┌─────────────────────────┐
│ Record Header           │ Type, length, CRC
├─────────────────────────┤
│ XLogRecord Header       │ Prev ptr, RInfo, Rmgr
├─────────────────────────┤
│ Resource Manager Data   │ Heap/Index/etc. specific
├─────────────────────────┤
│ Backup Block (optional) │ Modified block data
├─────────────────────────┤
│ Trailer                 │ CRC check
└─────────────────────────┘
```

### LSN (Log Sequence Number)

```
LSN Format: LogFileID/Offset
Example: 0/14000000

Calculation:
LogFileID = 0 → First WAL file
Offset = 14000000 (hex) → Position in file

LSN Usage:
- Track replication position
- Calculate replication lag
- Perform PITR recovery
- Identify WAL files to archive

LSN Comparison:
SELECT pg_lsn_diff('0/2000000', '0/1000000') as diff;
-- Result: 16777216 bytes
```

### WAL Archiving

```sql
-- Enable archiving
archive_mode = on
archive_command = '/usr/local/bin/archive-wal.sh "%p" "%f"'
archive_timeout = 300  -- Archive every 5 minutes

-- Monitor archiving
SELECT * FROM pg_stat_archiver;

-- Expected output:
-- archived_count: Number of WAL files archived
-- failed_count: Archive failures (should be 0)
-- last_archived_wal: Last successfully archived file
-- last_failed_wal: Last failed file (if any)
```

---

## Vacuum Architecture

### VACUUM Phases

```
Phase 1: Scan Phase
├─ Scan all pages in table
├─ Mark dead tuples as reusable
├─ Collect dead tuple visibility
└─ Build dead tuple list

Phase 2: Index Scan
├─ For each index:
│  ├─ Find entries pointing to dead tuples
│  └─ Mark index entries for removal
└─ Complete all indexes

Phase 3: Cleanup
├─ Remove marked index entries
├─ Update free space map
├─ Update visibility map
└─ Update statistics
```

### VACUUM Tuning Parameters

```ini
# Autovacuum settings
autovacuum_max_workers = 3          # Parallel workers
autovacuum_naptime = 30s            # Check frequency
autovacuum_vacuum_threshold = 50    # Min 50 tuples changed
autovacuum_analyze_threshold = 50   # Min 50 tuples for analyze

# Scale factors
autovacuum_vacuum_scale_factor = 0.1      # 10% of table
autovacuum_analyze_scale_factor = 0.05    # 5% of table

# Cost settings (lower = more aggressive)
autovacuum_vacuum_cost_delay = 2ms        # Pause between operations
autovacuum_vacuum_cost_limit = 200        # Work limit per cycle
```

---

## Transaction Isolation

### Isolation Levels

```
┌──────────────────┬─────────────┬─────────────┬──────────────┐
│ Isolation Level  │ Dirty Read  │ Non-Repeatable│ Phantom     │
├──────────────────┼─────────────┼─────────────┼──────────────┤
│ Read Uncommitted │ Possible    │ Possible    │ Possible     │
│ (Not in PG)      │             │             │              │
├──────────────────┼─────────────┼─────────────┼──────────────┤
│ Read Committed   │ No          │ Possible    │ Possible     │
│ (PostgreSQL)     │             │             │              │
├──────────────────┼─────────────┼─────────────┼──────────────┤
│ Repeatable Read  │ No          │ No          │ Possible     │
│ (PostgreSQL)     │             │             │              │
├──────────────────┼─────────────┼─────────────┼──────────────┤
│ Serializable     │ No          │ No          │ No           │
│ (PostgreSQL)     │             │             │              │
└──────────────────┴─────────────┴─────────────┴──────────────┘
```

### MVCC (Multi-Version Concurrency Control)

```
Reader 1 (txn 100)          Writer (txn 101)
        │                           │
        ├─ SELECT from table        │
        │  Sees version from txn 99  │
        │                           ├─ UPDATE row
        │                           │  Creates new version
        │                           │  Marks old version
        │                           │  Commits (txn 101)
        │                           │
        ├─ SELECT same row          │
        │  Still sees txn 99 version │
        │  (Repeatable Read)         │
        │                           │
        └─ COMMIT                   └─ Done

No locks needed because:
- Each transaction sees consistent snapshot
- Multiple versions coexist
- Old versions kept until not needed
```

---

## Advanced Configuration

### Performance Parameters

```ini
# Query Planning
random_page_cost = 1.1                    # SSD: 1.1, HDD: 4.0
effective_cache_size = 32GB               # 50-75% of RAM
effective_io_concurrency = 200            # SSD: 200, HDD: 4

# Parallelization
max_parallel_workers_per_gather = 4       # Per query
max_parallel_workers = 8                  # Total
max_parallel_maintenance_workers = 4      # Maintenance

# Checkpoint
checkpoint_timeout = 15min                # Force checkpoint
checkpoint_completion_target = 0.9        # Smooth progress
max_wal_size = 4GB                        # Trigger checkpoint
min_wal_size = 1GB                        # Keep at least 1GB
```

### Monitoring Advanced Metrics

```sql
-- Table structure analysis
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    n_live_tup,
    n_dead_tup,
    ROUND(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) as dead_ratio
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- WAL statistics
SELECT
    wal_records,
    wal_fpi,
    wal_bytes,
    pg_size_pretty(wal_bytes)
FROM pg_stat_wal;

-- Index efficiency
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    ROUND(100.0 * idx_tup_fetch / NULLIF(idx_tup_read, 0), 2) as efficiency_ratio
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

---

**Related Documentation:**
- [01-getting-started.md](01-getting-started.md) - Getting started guide
- [03-backup-recovery.md](03-backup-recovery.md) - Backup procedures
- See [docs/database/](../../docs/database/) for related guides
