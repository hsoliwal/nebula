<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original SWT/RWT virtual Table receiving route (2026-10-25)

**Product implementation:** `widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/M3VirtualTableBinding.java`.
**Recipe/provenance owner:** `hsoliwal/com.synexia` (source transformations only).
**Status:** product Java committed, real SWT/RWT API compilation and native acceptance **NOT_RUN**.

This route receives tabular data from an admitted **application-provided snapshot**, not
arbitrary live HTML/JS. The original `org.eclipse.swt.widgets.Table` owns
virtual item creation, scrollbars, selection events, native painting and
resource disposal. No replacement Table/Control hierarchy is introduced.
The `SWT.VIRTUAL` and `SWT.SetData` contracts are shared by SWT and
RAP/RWT, subject to independent RAP web-session testing.

## Why this receiver exists

The earlier bounded JSX `OrdersNativeSwt`, `ContactFormNativeSwt` and
`PreferencesNativeSwt` factories correctly instantiate native controls
for small static forms; **a million-row web Grid must not become a million
Controls**. The original Nebula Grid has advanced cell spans, frozen
columns, visualizers and viewport rendering. RAP ships a separate native
port of Nebula Grid; compiling the desktop Grid source itself against RWT
still has known GC, MouseMove and DND gaps, as measured in
`docs/m3/rwt/RWT_COLUMN.tsv`. For simple tables a stock RWT/SWT
`Table(SWT.VIRTUAL)` is the safest common receiver.

`M3GridRowBinding` also exists on a separately integrated Nebula feature
line, which depends on enhanced `org.eclipse.swt.m3.M3RowView` APIs.
This binding does not copy or replace that API and does not introduce a
runtime dependency on that feature branch. Once qualified, a canonical
Synexia recipe may fold reusable, contract-equivalent parts into the
original Grid/SWT M3 row-view owner.

## Data path

```text
Apache Wicket IDataProvider / Vaadin lazy Grid / Spring Pageable / GWT CellTable
   -> source-specific typed model/event conversion in Synexia (NOT implemented)
   -> indexed, immutable resident row snapshot (application-owned)
   -> M3VirtualTableBinding.Rows.size/columns/fetch/indexOfKey
   -> 4 × 64-row fixed hot-page array, LRU by access timestamp
   -> SWT.SetData event for requested visible index
   -> original TableItem text and stable m3.web.id (no per-cell Controls)
   -> original SWT Table / RAP RWT Table renderer
```

Provider `fetch(offset,length)` is a **synchronous UI-thread callback**,
not an invitation to run HTTP/SQL/JPA queries on the UI thread. Producers
must prefetch bounded pages asynchronously and publish an already-resident
snapshot on the SWT/RWT UI thread. A future asynchronous receiver will
require generation/version tokens, cancellation, placeholders, and a
coalesced UI `asyncExec` update; none is silently simulated here.

## Source Java usage

```java
Table table = new Table(parent, SWT.VIRTUAL | SWT.MULTI | SWT.FULL_SELECTION);
new TableColumn(table, SWT.NONE).setText("Order");
new TableColumn(table, SWT.NONE).setText("Status");

M3VirtualTableBinding binding = M3VirtualTableBinding.bind(
    table, new M3VirtualTableBinding.Rows() {
        @Override public int size() { return resident.count(); }
        @Override public int columns() { return 2; }

        @Override public M3VirtualTableBinding.Page fetch(int offset, int length) {
            // resident is an indexed, in-memory snapshot; NO network/SQL here.
            String[] ids = new String[length];
            String[] cells = new String[length * 2];
            for (int i = 0; i < length; i++) {
                int row = offset + i;
                ids[i] = resident.stableKey(row);
                cells[i * 2] = resident.orderName(row);
                cells[i * 2 + 1] = resident.status(row);
            }
            return new M3VirtualTableBinding.Page(offset, 2, ids, cells);
        }

        @Override public int indexOfKey(String key) {
            return resident.viewIndexByStableKey(key); // indexed, never O(all rows)
        }
    });

// After publishing a replacement snapshot, e.g. sort/filter change:
binding.replaceRows(newResidentRows);
// When only a bounded source interval changes:
binding.invalidate(firstChanged, lastChanged);
// Binding detaches its listeners; Table remains owned by SWT.
binding.close();
```

The example assumes an application-specific `resident` object. It does
not claim that any framework already supplies that class or a compiled
Wicket/Vaadin/Spring adapter. Those source frameworks have different
lifecycle, model-detach, query, selection and security contracts.

## Invariants and limits

- At most **4 cached pages × 64 rows**, even for 1,000,000 logical
  rows. Pages contain flat `String[]` cell lanes plus one `String[]`
  of stable keys; no per-entry `Map.Entry`, `Control` or cell object.
  `SetData` calls `TableItem.setText(column,value)` directly, without
  allocating a temporary row array.
- Each page validates exact offset/row length/column count, unique keys
  within the page, nulls, individual cell and aggregate size limits.
  Invalid provider responses fail without entering the cache.
- Stable keys are stored on native `TableItem.setData("m3.web.id", key)`.
  Only the bounded selected-key set (up to 4096) survives sorting and
  filtering. `Rows.indexOfKey` must use an indexed reverse lookup;
  hidden/removed keys return -1 but are retained for later reappearance.
- On a snapshot replacement, clear the four hot pages, set logical
  item count, clear stale TableItem data and restore visible selection.
  Reverse key lookup is **preflighted** before any Table mutation so an
  out-of-range source mapping cannot leave a half-installed snapshot.
  Restored selections use one `Table.select(int[])` batch rather than
  one UI/RWT request per key.
  On a bounded mutation, evict only intersecting pages and
  `Table.clear(first,last)`, never copy all rows.
- All SWT calls, selection and cache mutation happen on the SWT UI thread.
  Nested/reentrant `SetData` and mutation during fetch are refused.
  Closing detaches handlers. The binding never disposes the Table.
- `Table` cell/text native behavior, wide DPI, accessibility, sorting,
  keyboard and RAP browser session must be measured before qualification.

## Framework semantic mapping

| Original | Donor contract | Required source adaptation |
| --- | --- | --- |
| GWT `CellTable` | row source, keys, visible range | typed UiBinder + CellTable model mapping |
| Vaadin `Grid<T>` | `offset`, `limit`, sort/filter and stable IDs | application-provided resident page snapshot; UI lifecycle/refresh |
| Apache Wicket `DataTable/IDataProvider` | paginated provider and detachable row models | retain key, release request-local objects, avoid cross-session leakage |
| Spring Data `Pageable` | page window, `Sort`, repository filters | async fetch outside SWT UI callback, cancellation and publication |
| RAP/RWT `Table(SWT.VIRTUAL)` | `SWT.SetData`, client-visible row requests | compile and run in RAP browser; server request/latency qualification |
| Nebula `Grid`/RAP Grid | richer cell spans, frozen columns, GridItems | separate existing original Grid-row binding; **not** this Table class |

Wicket detach/reload semantics and Vaadin/Spring paging are kept
distinct; a generic facade does **not** replace their runtime
transactions, security, renderer or model lifecycle.

## Tests and verification

- `M3VirtualTablePageTest`: pure source/page shape, copy ownership,
  duplicate key and aggregate-memory budget.
- `M3VirtualTableBindingTest`: real SWT display-backed test source;
  1,000,000 logical rows, bounded page residency, selection-key
  preservation after reversal, damaged-range refresh, invalid-style
  refusal and listener disposal. **NOT_RUN** on native SWT.
- `.github/workflows/m3-web-native-rwt-api-20261025.yml`:
  real SHA-256-pinned Eclipse RAP/RWT 4.8.0 Java API compile;
  includes `M3VirtualTableBinding.java`. **PENDING**.
- `m3/proof/OriginalVirtualPageProof.java` adds a display-free,
  no-JUnit executable of the actual compiled SWT/RWT Table and Tree
  page owners; the RWT API workflow runs it if that job can start.
- Full native SWT Win32/GTK/Cocoa screen, RWT browser session,
  memory/GC/latency and Tycho reactor qualification **NOT_RUN**.

Primary API documentation:
- https://www.eclipse.org/articles/Article-SWT-Virtual/Virtual-in-SWT.html
- https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/swt/widgets/Table.html
- https://vaadin.com/docs/latest/flow/binding-data/data-provider
- https://wicket.apache.org/docs/about-models.html
- https://wicket.apache.org/docs/detachable-models.html
- https://eclipse.dev/rap/developers-guide/rwt.html
