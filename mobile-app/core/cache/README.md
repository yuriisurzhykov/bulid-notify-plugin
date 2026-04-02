# core:cache — Data / Cache Layer Architecture

Design document for the unified cache and data layer of Build Notify Mobile.

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Design Principles](#2-design-principles)
3. [Abstraction Layers](#3-abstraction-layers)
4. [Interfaces (Contracts)](#4-interfaces-contracts)
5. [Implementations](#5-implementations)
6. [Scenario A — Runtime In-Memory Storage](#6-scenario-a--runtime-in-memory-storage)
7. [Scenario B — Shared Generic Cache (JSON Table)](#7-scenario-b--shared-generic-cache-json-table)
8. [Scenario C — Per-Entity Typed Table (SQLDelight DAO)](#8-scenario-c--per-entity-typed-table-sqldelight-dao)
9. [DI Wiring](#9-di-wiring)
10. [Decision Guide](#10-decision-guide)
11. [Eviction and Future Extensibility](#11-eviction-and-future-extensibility)
12. [Step-by-Step: Adding Cache to a New Feature](#12-step-by-step-adding-cache-to-a-new-feature)

---

## 1. Purpose

This module provides the foundational abstractions and implementations for the
application's data and cache layer. The goal is to enable any feature to store
and observe data — whether in runtime memory or a persistent database — through
a uniform set of interfaces, with minimal boilerplate and zero knowledge of the
underlying storage mechanism.

The module answers a single question: **"I have data that I need to store and
reactively observe — how do I do that cleanly?"**

---

## 2. Design Principles

| Principle | How it applies here |
|---|---|
| **DIP** (Dependency Inversion) | Features depend only on `ReadableDataSource` / `MutableDataSource` interfaces. Storage engine is injected. |
| **SRP** (Single Responsibility) | Each class does one thing: `CacheStore` stores, `LocalXxxSource` adapts, `CachedReadableDataSource` composes. |
| **OCP** (Open/Closed) | New storage strategies (eviction, encryption) are added as decorators, not modifications. |
| **ISP** (Interface Segregation) | Read and write are separate interfaces (`ReadableDataSource`, `WritableDataSource`). Consumers depend only on what they need. |
| **LSP** (Liskov Substitution) | `InMemoryCacheStore` and `DatabaseCacheStore` are interchangeable behind `CacheStore<K, V>`. |
| **KISS** | A feature adds cache support with ~5 small files, each 10–20 lines. |
| **YAGNI** | No eviction, no encryption, no migration tooling until actually needed. The design allows adding them later as decorators. |
| **DRY** | Caching strategy lives in one place (`CachedReadableDataSource`). Features never rewrite it. |

---

## 3. Abstraction Layers

The architecture has three tiers. Each tier knows only about the tier directly
below it.

```
┌─────────────────────────────────────────────────────────┐
│  Tier 3 — Feature Layer                                 │
│                                                         │
│  Repository  ←  extends CachedReadableDataSource        │
│  RemoteXxxSource : ReadableDataSource                   │
│  LocalXxxSource  : MutableDataSource                    │
├─────────────────────────────────────────────────────────┤
│  Tier 2 — Data Source Contracts  (domain-level)         │
│                                                         │
│  ReadableDataSource<P, T>                               │
│  WritableDataSource<P, T>                               │
│  MutableDataSource<P, T>                                │
│  CachedReadableDataSource<P, T>  (abstract decorator)   │
├─────────────────────────────────────────────────────────┤
│  Tier 1 — Storage Engine  (infrastructure-level)        │
│                                                         │
│  CacheStore<K, V>                                       │
│  InMemoryCacheStore<K, V>                               │
│  DatabaseCacheStore<K, V>                               │
│  (or direct SQLDelight DAO for typed tables)            │
└─────────────────────────────────────────────────────────┘
```

**Tier 1 (CacheStore)** is a generic key-value storage contract. It knows
nothing about the domain. Its implementations are written once and reused across
the entire application by creating instances with different type parameters.

**Tier 2 (DataSource contracts)** defines how the domain layer reads and writes
data. `CachedReadableDataSource` is the abstract decorator that composes a
remote source and a local source into a single reactive pipeline where the local
source is the single source of truth.

**Tier 3 (Feature layer)** creates concrete `RemoteXxxSource`, `LocalXxxSource`,
and `XxxRepository` per feature. Each is a small, focused class.

---

## 4. Interfaces (Contracts)

### ReadableDataSource

Emits data reactively. This is the primary observation contract.

```kotlin
interface ReadableDataSource<P, out T> {

    /**
     * Returns a Flow that emits data for the given [params].
     * The Flow is typically long-lived and re-emits whenever the
     * underlying data changes.
     */
    fun observe(params: P): Flow<T>
}
```

- `P` — query parameters. Use `Unit` when no parameters are needed.
- `T` — the data type. Covariant (`out`) so a `ReadableDataSource<Unit, List<BuildRecord>>` satisfies `ReadableDataSource<Unit, List<Any>>`.

### WritableDataSource

Writes data. Intentionally does not extend `ReadableDataSource` (ISP).

```kotlin
interface WritableDataSource<P, in T> {

    /** Persists [data] associated with [params]. */
    suspend fun save(params: P, data: T)

    /** Removes data associated with [params]. */
    suspend fun delete(params: P)
}
```

### MutableDataSource

Combines reading and writing for local/cache data sources.

```kotlin
interface MutableDataSource<P, T> : ReadableDataSource<P, T>, WritableDataSource<P, T>
```

### CacheStore

Low-level, generic, key-value storage engine. Domain-agnostic.

```kotlin
interface CacheStore<K : Any, V : Any> {

    /** Observes a single entry by [key]. Emits null if absent. */
    fun observe(key: K): Flow<V?>

    /** Observes all entries in this store. */
    fun observeAll(): Flow<List<V>>

    /** Returns the value for [key], or null if absent. */
    suspend fun get(key: K): V?

    /** Inserts or updates the entry for [key]. */
    suspend fun put(key: K, value: V)

    /** Removes the entry for [key]. */
    suspend fun remove(key: K)

    /** Removes all entries from this store. */
    suspend fun clear()
}
```

---

## 5. Implementations

### InMemoryCacheStore

Runtime-only storage backed by a `MutableStateFlow<Map<K, V>>`. Data dies with
the process.

```kotlin
class InMemoryCacheStore<K : Any, V : Any> : CacheStore<K, V> {

    private val entries = MutableStateFlow<Map<K, V>>(emptyMap())

    override fun observe(key: K): Flow<V?> =
        entries.map { it[key] }.distinctUntilChanged()

    override fun observeAll(): Flow<List<V>> =
        entries.map { it.values.toList() }.distinctUntilChanged()

    override suspend fun get(key: K): V? = entries.value[key]

    override suspend fun put(key: K, value: V) {
        entries.update { it + (key to value) }
    }

    override suspend fun remove(key: K) {
        entries.update { it - key }
    }

    override suspend fun clear() {
        entries.value = emptyMap()
    }
}
```

### DatabaseCacheStore

Persistent storage backed by SQLDelight. Uses a single shared table where values
are serialized to JSON via `kotlinx-serialization`. Suitable for simple
cache-and-retrieve scenarios where SQL queries on individual fields are not
needed.

SQLDelight schema (`cache_entry.sq`):

```sql
CREATE TABLE cache_entry (
    store_name TEXT NOT NULL,
    key        TEXT NOT NULL,
    value      TEXT NOT NULL,
    PRIMARY KEY (store_name, key)
);

selectByKey:
SELECT value FROM cache_entry WHERE store_name = ? AND key = ?;

selectAll:
SELECT value FROM cache_entry WHERE store_name = ?;

upsert:
INSERT OR REPLACE INTO cache_entry (store_name, key, value) VALUES (?, ?, ?);

deleteByKey:
DELETE FROM cache_entry WHERE store_name = ? AND key = ?;

deleteAll:
DELETE FROM cache_entry WHERE store_name = ?;
```

Implementation:

```kotlin
class DatabaseCacheStore<K : Any, V : Any>(
    private val queries: CacheEntryQueries,
    private val storeName: String,
    private val keySerializer: KSerializer<K>,
    private val valueSerializer: KSerializer<V>,
    private val json: Json,
) : CacheStore<K, V> {

    override fun observeAll(): Flow<List<V>> =
        queries.selectAll(storeName)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { json.decodeFromString(valueSerializer, it) } }

    override fun observe(key: K): Flow<V?> =
        queries.selectByKey(storeName, json.encodeToString(keySerializer, key))
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.let { json.decodeFromString(valueSerializer, it) } }

    override suspend fun put(key: K, value: V) {
        queries.upsert(
            store_name = storeName,
            key = json.encodeToString(keySerializer, key),
            value_ = json.encodeToString(valueSerializer, value),
        )
    }

    override suspend fun get(key: K): V? =
        queries.selectByKey(storeName, json.encodeToString(keySerializer, key))
            .executeAsOneOrNull()
            ?.let { json.decodeFromString(valueSerializer, it) }

    override suspend fun remove(key: K) {
        queries.deleteByKey(storeName, json.encodeToString(keySerializer, key))
    }

    override suspend fun clear() {
        queries.deleteAll(storeName)
    }
}
```

Different instances are separated by `storeName`. The same `CacheEntryQueries`
(from a single SQLDelight database) is shared across all `DatabaseCacheStore`
instances.

### CachedReadableDataSource (Abstract Decorator)

The core composition strategy. Combines a remote `ReadableDataSource` and a
local `MutableDataSource` where the **local source is the single source of
truth**.

```kotlin
abstract class CachedReadableDataSource<P : Any, T : Any>(
    private val remote: ReadableDataSource<P, T>,
    private val local: MutableDataSource<P, T>,
) : ReadableDataSource<P, T> {

    override fun observe(params: P): Flow<T> = channelFlow {
        // Remote data flows into local storage (background)
        launch {
            remote.observe(params).collect { data ->
                local.save(params, data)
            }
        }
        // Local is the single source of truth
        local.observe(params).collect { data ->
            send(data)
        }
    }
}
```

How it works:

1. A background coroutine collects from the remote source and writes every
   emission into the local source via `save()`.
2. The main collection observes the local source and forwards its emissions to
   the caller.
3. The caller **only ever sees data from local**. Remote data appears only after
   it has been written to local and local re-emits it.
4. If the remote source fails or is unavailable, local continues to emit
   whatever it already has (stale data is better than no data).

If a feature needs a fundamentally different strategy, it implements
`ReadableDataSource<P, T>` directly and does not extend this class. OCP — extend
via new implementations, never modify the base.

---

## 6. Scenario A — Runtime In-Memory Storage

**Use case:** Cache the current connection state in memory so it is available
instantly to the UI, but there is no need to survive an app restart.

### Domain interface

```kotlin
// feature/network-status/domain/
interface IConnectionCacheRepository {
    fun observeLastState(): Flow<ConnectionState>
}
```

### Remote source

```kotlin
// feature/network-status/data/
class RemoteConnectionSource(
    private val connectionManager: ConnectionManager,
) : ReadableDataSource<Unit, ConnectionState> {

    override fun observe(params: Unit): Flow<ConnectionState> =
        connectionManager.state
}
```

### Local source

```kotlin
// feature/network-status/data/
class LocalConnectionSource(
    private val store: CacheStore<String, ConnectionState>,
) : MutableDataSource<Unit, ConnectionState> {

    override fun observe(params: Unit): Flow<ConnectionState> =
        store.observe("current").filterNotNull()

    override suspend fun save(params: Unit, data: ConnectionState) {
        store.put("current", data)
    }

    override suspend fun delete(params: Unit) = store.clear()
}
```

### Repository

```kotlin
// feature/network-status/data/
class ConnectionCacheRepository(
    remote: RemoteConnectionSource,
    local: LocalConnectionSource,
) : CachedReadableDataSource<Unit, ConnectionState>(remote, local),
    IConnectionCacheRepository {

    override fun observeLastState(): Flow<ConnectionState> = observe(Unit)
}
```

### DI

```kotlin
// feature/network-status/di/
interface NetworkStatusComponent {

    @Provides
    fun connectionStore(): CacheStore<String, ConnectionState> =
        InMemoryCacheStore()    // <-- runtime only
}
```

**What happens at runtime:**

1. `ConnectionManager.state` emits `Connected`.
2. `RemoteConnectionSource` relays it to `CachedReadableDataSource`.
3. `CachedReadableDataSource` writes it to `LocalConnectionSource`.
4. `LocalConnectionSource` calls `store.put("current", Connected)`.
5. `InMemoryCacheStore` updates its internal `StateFlow`.
6. `LocalConnectionSource.observe()` re-emits `Connected`.
7. ViewModel receives `Connected`.

If the app is killed and restarted, the store is empty — the ViewModel will wait
until the remote source delivers the first value.

---

## 7. Scenario B — Shared Generic Cache (JSON Table)

**Use case:** Persist user preferences or small configuration objects so they
survive an app restart. No SQL queries needed — just save and retrieve by key.

### Domain interface

```kotlin
// feature/settings/domain/
interface IUserPrefsRepository {
    fun observePrefs(): Flow<UserPrefs>
}
```

### Remote source

```kotlin
// feature/settings/data/
class RemoteUserPrefsSource(
    private val session: ActiveSession,
) : ReadableDataSource<Unit, UserPrefs> {

    override fun observe(params: Unit): Flow<UserPrefs> =
        session.incoming
            .filterIsInstance<UserPrefsPayload>()
            .map { it.prefs }
}
```

### Local source

```kotlin
// feature/settings/data/
class LocalUserPrefsSource(
    private val store: CacheStore<String, UserPrefs>,
) : MutableDataSource<Unit, UserPrefs> {

    override fun observe(params: Unit): Flow<UserPrefs> =
        store.observe("prefs").filterNotNull()

    override suspend fun save(params: Unit, data: UserPrefs) {
        store.put("prefs", data)
    }

    override suspend fun delete(params: Unit) = store.clear()
}
```

### Repository

```kotlin
// feature/settings/data/
class UserPrefsRepository(
    remote: RemoteUserPrefsSource,
    local: LocalUserPrefsSource,
) : CachedReadableDataSource<Unit, UserPrefs>(remote, local),
    IUserPrefsRepository {

    override fun observePrefs(): Flow<UserPrefs> = observe(Unit)
}
```

### DI

```kotlin
// feature/settings/di/
interface SettingsComponent {

    @Provides
    fun prefsStore(
        queries: CacheEntryQueries,
        json: Json,
    ): CacheStore<String, UserPrefs> =
        DatabaseCacheStore(                 // <-- persistent, shared JSON table
            queries = queries,
            storeName = "user_prefs",
            keySerializer = String.serializer(),
            valueSerializer = UserPrefs.serializer(),
            json = json,
        )
}
```

`DatabaseCacheStore` stores the `UserPrefs` object as a JSON string in the
shared `cache_entry` table with `store_name = "user_prefs"`. No per-entity
schema required.

---

## 8. Scenario C — Per-Entity Typed Table (SQLDelight DAO)

**Use case:** Persist build history with a proper database schema — typed
columns, indexes, and rich SQL queries (filter by status, sort by date, etc.).

In this scenario, `LocalXxxSource` does **not** use `CacheStore`. Instead, it
works with a SQLDelight-generated DAO directly, while still implementing
`MutableDataSource`.

### SQLDelight schema

```sql
-- feature/history/sqldelight/me/yuriisoft/.../BuildHistory.sq

CREATE TABLE build_history (
    id          TEXT PRIMARY KEY,
    project     TEXT NOT NULL,
    status      TEXT NOT NULL,
    started_at  INTEGER NOT NULL,
    finished_at INTEGER
);

CREATE INDEX idx_build_history_status ON build_history(status);

selectAll:
SELECT * FROM build_history ORDER BY started_at DESC;

selectByStatus:
SELECT * FROM build_history WHERE status = ? ORDER BY started_at DESC;

upsert:
INSERT OR REPLACE INTO build_history (id, project, status, started_at, finished_at)
VALUES (?, ?, ?, ?, ?);

deleteAll:
DELETE FROM build_history;
```

### Domain interface

```kotlin
// feature/history/domain/
interface IBuildHistoryRepository {
    fun observeHistory(): Flow<List<BuildRecord>>
}
```

### Remote source

```kotlin
// feature/history/data/
class RemoteBuildHistorySource(
    private val session: ActiveSession,
) : ReadableDataSource<Unit, List<BuildRecord>> {

    override fun observe(params: Unit): Flow<List<BuildRecord>> =
        session.incoming
            .filterIsInstance<BuildHistoryPayload>()
            .map { it.records }
}
```

### Local source (uses SQLDelight DAO, not CacheStore)

```kotlin
// feature/history/data/
class LocalBuildHistorySource(
    private val queries: BuildHistoryQueries,
) : MutableDataSource<Unit, List<BuildRecord>> {

    override fun observe(params: Unit): Flow<List<BuildRecord>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toBuildRecord() } }

    override suspend fun save(params: Unit, data: List<BuildRecord>) {
        queries.transaction {
            data.forEach { record ->
                queries.upsert(
                    id = record.id,
                    project = record.projectName,
                    status = record.status.name,
                    started_at = record.startedAt.toEpochMilliseconds(),
                    finished_at = record.finishedAt?.toEpochMilliseconds(),
                )
            }
        }
    }

    override suspend fun delete(params: Unit) {
        queries.deleteAll()
    }
}
```

### Repository

```kotlin
// feature/history/data/
class BuildHistoryRepository(
    remote: RemoteBuildHistorySource,
    local: LocalBuildHistorySource,
) : CachedReadableDataSource<Unit, List<BuildRecord>>(remote, local),
    IBuildHistoryRepository {

    override fun observeHistory(): Flow<List<BuildRecord>> = observe(Unit)
}
```

### DI

```kotlin
// feature/history/di/
interface HistoryComponent {

    @Provides
    fun buildHistoryQueries(database: AppDatabase): BuildHistoryQueries =
        database.buildHistoryQueries
}
```

**Key difference from Scenario A/B:** the DI component provides
`BuildHistoryQueries` (SQLDelight-generated), not `CacheStore`. The
`LocalBuildHistorySource` constructor takes the DAO directly. But it still
implements `MutableDataSource<Unit, List<BuildRecord>>` — so the Repository
sees no difference.

---

## 9. DI Wiring

### Storage strategy is selected in the DI component

The feature's DI component (kotlin-inject `@Component` interface) is the single
place that decides what storage engine backs each data source:

```kotlin
// In-memory — volatile, dies with process
@Provides
fun store(): CacheStore<String, Foo> = InMemoryCacheStore()

// Database — persistent, shared JSON table
@Provides
fun store(queries: CacheEntryQueries, json: Json): CacheStore<String, Foo> =
    DatabaseCacheStore(queries, "foo_cache", String.serializer(), Foo.serializer(), json)

// SQLDelight DAO — persistent, per-entity typed table
@Provides
fun fooQueries(db: AppDatabase): FooQueries = db.fooQueries
```

### AppComponent provides shared infrastructure

`CacheEntryQueries`, `AppDatabase`, and `Json` are provided once in
`AppComponent` and available to all feature components:

```kotlin
@AppScope
@Component
abstract class AppComponent(...) : HistoryComponent, SettingsComponent, ... {

    @Provides
    fun json(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    fun database(): AppDatabase = ... // platform-specific SQLDelight driver

    @Provides
    fun cacheEntryQueries(db: AppDatabase): CacheEntryQueries =
        db.cacheEntryQueries
}
```

---

## 10. Decision Guide

Use this table to choose the right storage approach for a new feature:

```
┌─────────────────────────────────┬────────────────────────────────┐
│  Question                       │  Answer → Approach             │
├─────────────────────────────────┼────────────────────────────────┤
│  Does data need to survive      │  NO  → InMemoryCacheStore      │
│  an app restart?                │  YES → continue ↓              │
├─────────────────────────────────┼────────────────────────────────┤
│  Do you need SQL queries        │  NO  → DatabaseCacheStore      │
│  (filter, sort, join, index)?   │       (shared JSON table)      │
│                                 │  YES → continue ↓              │
├─────────────────────────────────┼────────────────────────────────┤
│  Create a dedicated .sq file    │  SQLDelight DAO directly       │
│  with a typed schema.           │  (per-entity table)            │
└─────────────────────────────────┴────────────────────────────────┘
```

Summary:

| Scenario | Survives restart | SQL queries | Storage mechanism |
|---|---|---|---|
| A — Runtime cache | No | No | `InMemoryCacheStore` |
| B — Generic persistent cache | Yes | No | `DatabaseCacheStore` (JSON table) |
| C — Typed entity table | Yes | Yes | SQLDelight DAO directly |

In all three scenarios, the **Repository and everything above it (UseCase,
ViewModel) sees the same `MutableDataSource<P, T>` interface**. The storage
mechanism is invisible to the domain layer.

---

## 11. Eviction and Future Extensibility

Eviction (TTL, max size, LRU) is not implemented. Per YAGNI, it will be added
when a real use case demands it. The design supports it via the Decorator
pattern (GoF) without modifying existing code (OCP):

```kotlin
class BoundedCacheStore<K : Any, V : Any>(
    private val delegate: CacheStore<K, V>,
    private val maxSize: Int,
) : CacheStore<K, V> by delegate {

    override suspend fun put(key: K, value: V) {
        delegate.put(key, value)
        // Evict oldest entries if count exceeds maxSize
    }
}
```

Usage in DI — transparent wrapping:

```kotlin
@Provides
fun historyStore(queries: CacheEntryQueries, json: Json): CacheStore<String, BuildRecord> =
    BoundedCacheStore(
        delegate = DatabaseCacheStore(queries, "build_history", ...),
        maxSize = 500,
    )
```

The feature code is unaffected. The decorator is invisible.

Other future extensions that follow the same decorator pattern:

- **EncryptedCacheStore** — encrypts values before delegating to any `CacheStore`
- **LoggingCacheStore** — logs reads/writes for debugging
- **MigrationAwareCacheStore** — handles schema versioning for serialized data

Each is added as a decorator, composed in DI, invisible to features.

---

## 12. Step-by-Step: Adding Cache to a New Feature

This is a recipe. Follow these steps when any feature needs cached data.

### Step 1: Define the domain interface

In `feature/xxx/domain/`, create the repository interface:

```kotlin
interface IXxxRepository {
    fun observeXxx(): Flow<XxxData>
}
```

### Step 2: Create the remote source

In `feature/xxx/data/`, implement `ReadableDataSource`:

```kotlin
class RemoteXxxSource(
    private val session: ActiveSession,  // or any network source
) : ReadableDataSource<Unit, XxxData> {

    override fun observe(params: Unit): Flow<XxxData> =
        session.incoming
            .filterIsInstance<XxxPayload>()
            .map { it.data }
}
```

### Step 3: Create the local source

In `feature/xxx/data/`, implement `MutableDataSource`.

**If using CacheStore (Scenarios A or B):**

```kotlin
class LocalXxxSource(
    private val store: CacheStore<String, XxxData>,
) : MutableDataSource<Unit, XxxData> {

    override fun observe(params: Unit): Flow<XxxData> =
        store.observe("key").filterNotNull()

    override suspend fun save(params: Unit, data: XxxData) {
        store.put("key", data)
    }

    override suspend fun delete(params: Unit) = store.clear()
}
```

**If using SQLDelight DAO (Scenario C):**

```kotlin
class LocalXxxSource(
    private val queries: XxxQueries,
) : MutableDataSource<Unit, List<XxxEntity>> {

    override fun observe(params: Unit): Flow<List<XxxEntity>> =
        queries.selectAll().asFlow().mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(params: Unit, data: List<XxxEntity>) {
        queries.transaction {
            data.forEach { queries.upsert(it.id, it.name, ...) }
        }
    }

    override suspend fun delete(params: Unit) = queries.deleteAll()
}
```

### Step 4: Create the repository

In `feature/xxx/data/`:

```kotlin
class XxxRepository(
    remote: RemoteXxxSource,
    local: LocalXxxSource,
) : CachedReadableDataSource<Unit, XxxData>(remote, local),
    IXxxRepository {

    override fun observeXxx(): Flow<XxxData> = observe(Unit)
}
```

### Step 5: Wire in DI

In `feature/xxx/di/`, add provides to the component interface:

```kotlin
interface XxxComponent {

    @Provides
    fun xxxStore(): CacheStore<String, XxxData> =
        InMemoryCacheStore()          // or DatabaseCacheStore(...)

    @Provides
    fun XxxRepository.bind(): IXxxRepository = this
}
```

### Step 6: Use in ViewModel

```kotlin
class XxxViewModel(
    private val repository: IXxxRepository,
) : ViewModel() {

    val state: StateFlow<XxxData> = repository.observeXxx()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)
}
```

Done. Five small files in the feature, zero caching logic written by hand.
