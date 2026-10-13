<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula visualization + oscilloscope: packed decimation viewport (leaf nebula-viz-decimation-20261013)

Catalog rows (Synexia #10864, `docs/M3-SCALE/runs/20261013-all-widget-viewport/WIDGET_IMPLEMENTATION_LEDGER.tsv`):
`chart_xy` (wave 2, score 16), `heatmap_intensity` (wave 3, 12), `oscilloscope_stream` (wave 3, 12).
Receiver: `hsoliwal/nebula` master `eab2a65c451c8e66df1004dcbcb8d892356d8862`. Owned paths: `widgets/visualization/**`,
`widgets/oscilloscope/**`, and this directory. Donor decisions: `FOSS_REUSE_DECISION.tsv` (written before any code).

## 1. Laws applied

| law | how it lands here |
|---|---|
| widget = viewport | no wrapper control: `Trace`, `IntensityGraphFigure.GraphArea` and `Oscilloscope` decide what is visible themselves |
| bits/ints, objects on demand | samples in packed `double[]` columns; decimated output in `int[]` polylines; `ISample` objects only from `getSample(i)` / `getHotSampleList()` |
| CPU/GPU split | pyramid build and per-view decimation on worker threads, published as immutable snapshots; the UI thread draws one `drawPolyline(int[])` per trace (retainable by the GPU layer under GC, owned by another agent) |
| public API results unchanged | the decimated vertex list is the vertex list the existing paint loop builds (section 2); any case not proven equal falls back to the existing loop |
| no hidden work | pyramid/decimation are requested from paint only; data changes while hidden only bump a version; oscilloscope dispatcher parks while hidden and resumes on the first paint |
| GWT-safe shared code | decimation core uses `int`/`double` only, no `long` state, no int-overflow reliance, no `String[].clone()`, no `Objects.checkIndex` |
| Java authority, JNI optional | the Java min/max is the authority; a JNI kernel is optional and must fail closed; not built in this leaf unless measured necessary |

## 2. chart_xy (XYGraph `Trace`)

### 2.1 What the receiver already does

`Trace.paintInternalFigure` (blob `3d7f0551113b03dd81433f1b886218a144f2d3f7`) walks every sample in the visible index
range on every paint. For chronological line traces it already reduces each pixel column: the first point of a column
is added to a `NoRepeatsPointsList`; following points with the same pixel x are folded into `minInRegion`,
`maxInRegion`, `lastInRegion`, which are flushed (min, max, last) when the next column starts. Cost per paint is
O(visible samples) with one `Point` per sample, a `HashSet<Point>` insert per in-range sample and one `hotSampleist`
add per in-range sample.

### 2.2 Exact vertex rule (derived from the loop, proven by differential tests)

For a chronological line trace with every visible y finite and inside the y range, the loop produces exactly one
polyline. Let the virtual point sequence be: the left clip point (the loop's `getIntersection` of the first
out-of-range and first in-range sample, when that exists), the in-range samples, and the right clip point. Group it
into maximal runs of equal pixel x (columns). For every column except the last:

1. add `q1` (the column's first point);
2. let `L` be the first index after `q1` whose pixel y differs from `q1.y`, and `T` the last index before the column's
   last point whose pixel y differs from the last point's y; if `L <= T`, add `(x, min py[L..T])` then
   `(x, max py[L..T])`;
3. if the column has any point whose pixel y differs from `q1.y` or more than one distinct consecutive point,
   add the column's last point.

For the last column only `q1` is added (the loop never flushes the final region). Every add goes through the exact
`NoRepeatsPointsList.addPoint` rule (drop an exact repeat; drop a point within 1 px of the last that equals the one
before it). Pixel mapping is `LinearScale.getValuePrecisePosition` reproduced operation for operation from values
captured on the UI thread, then checked against `Axis.getValuePosition` for the range ends and the edge samples; any
mismatch, a subclass that overrides the mapping, a NaN, an out-of-y-range sample, a step/bar/area/point type, point
markers, a point style provider or error bars send the paint to the existing loop.

### 2.3 Packed model

- `PackedXYDataProvider` (new, additive, implements `IDataProvider`): `double[]` x and y columns, chronological,
  append-only with doubling growth, `setData` bulk replace. A snapshot is (x array, y array, size); appends write past
  `size` and then publish a new size, so a snapshot never changes. `getSample(i)` creates a `Sample` on demand.
  `getXDataMinMax`/`getYDataMinMax` are O(1) from running min/max.
- `PackedMinMaxPyramid` (new): level 1 = min/max of blocks of 16 samples, each further level folds 8 blocks. Only
  complete blocks are written, and a complete block is never rewritten, so readers of an older snapshot see stable
  values. Range min/max, "first index whose pixel differs" and "last index whose pixel differs" are O(log N) descents.
- `TraceDecimator` (new): runs on a shared daemon worker pool; input = (data snapshot, pyramid snapshot, immutable
  view key with every mapping parameter); output = an exact-length `int[]` polyline. Results are cached by view key,
  so repaints without a view change allocate nothing and pans back to a cached view reuse it.

### 2.4 Paint path

`Trace.paintInternalFigure` checks eligibility; if eligible it asks the decimator for the current view key, waits a
bounded time for the worker, draws `graphics.drawPolyline(int[])` through the same `drawPolyline(Graphics, PointList)`
state settings (line width, line style), and records the visible index range. `getHotSampleList()` then materializes
the in-range samples from the recorded snapshot on first call (identical content to the eager list for the packed
provider, whose snapshots are immutable). Every other provider keeps the existing eager loop unchanged.

## 3. heatmap_intensity (`IntensityGraphFigure`, `ColorMap`)

The 256-entry `pixelLookupTable` and the nearest-neighbour shrink in `ColorMap.drawImage` are the byte authority. The
packed path computes the same LUT index per output pixel and writes the three bytes straight into
`ImageData.data` (24-bit, palette masks `0xff/0xff00/0xff0000`) instead of `setPixel` per pixel, reads `double[]`
directly when the wrapper exposes it, and splits rows across workers. The output is compared byte for byte with the
existing `drawImage`. Cropping keeps the copy only when profile listeners need a stable array; otherwise a strided
view is used, so a zoom on a 4K x 4K image does not copy 16M doubles.

## 4. oscilloscope_stream (`multichannel.Oscilloscope`)

- Existing `setValue`/`setValues`/paint results are unchanged; per-frame garbage from `calculate` (two `int[]` per
  progression step plus an `Object[]`) is removed by computing only the final step's lines into reused buffers.
- New additive streaming API: `setSamplesPerColumn(channel, n)` and `pushSamples(channel, int[], offset, count)`
  fold any number of samples into per-column envelopes (first, min, max, last) held in a packed `int` ring of
  `width` columns; no per-sample storage, no objects. Drawing an envelope column emits the same vertices as the
  per-pixel rule of section 2.2, so it is pixel-equal to drawing every sample at that column.
- Hidden: the dispatcher does not re-arm its timer while the scope is not effectively visible; the first paint after
  it becomes visible re-arms it once. `-Dorg.eclipse.nebula.widgets.oscilloscope.hiddenWork=run` restores the old loop.

## 4a. Batch-scope thresholds (all overridable for tests and A/B runs)

| knob | default | effect |
|---|---|---|
| `org.eclipse.nebula.visualization.xygraph.decimation.minSamples` | 4096 | visible samples below this use the per-sample loop (cheaper than a worker round trip) |
| `org.eclipse.nebula.visualization.xygraph.decimation.waitMillis` | 250 | longest UI wait for a worker view before the previous polyline is drawn and one repaint is scheduled |
| `TraceDecimator` pool | min(4, cores/2) daemon threads | decimation and pyramid work; one task at a time per trace |
| `TraceDecimator` view cache | 4 views per trace | pans back to a cached view reuse its polyline |
| `PackedMinMaxPyramid` | 16-sample leaves, fan-out 8 | pyramid memory ~0.14 x samples x 16 bytes |
| `ColorMapRaster` | serial below 65,536 output pixels, else min(8, cores) row bands | LUT raster |
| oscilloscope envelope ring | widget width columns x 3 ints | same capacity rule as the existing value stack |

## 5. Proof plan

| lane | what |
|---|---|
| differential | recording `Graphics` captures every `drawPolyline` of the unmodified `Trace` and of the new `Trace` over seeded random data, zoom/pan views, inverted and log axes; vertex arrays must be equal |
| pixel parity | `Control.print` hashes of decimated vs full rendering at the same size, AA on and off, plus `GC.copyArea` screen captures labelled separately |
| scale | 10M-point XY, 4096x4096 intensity image, 1M samples/s oscilloscope stream |
| existing tests | `xygraph.test`, `widgets.tests`, `oscilloscope.tests` before and after |
| harness | Nebula visualization/oscilloscope examples and snippets launched before and after |
| measurements | load, first paint, pan/zoom frame p50/p95/max, CPU per frame, heap, allocations per frame (ThreadMXBean) before and after |

GTK and Cocoa: NOT_RUN (Win32 host only).

## 6. Results (Win32, JDK 21, SWT 3.135.0, draw2d 3.23.100; javac against the jars, Tycho NOT_RUN)

Before = unmodified receiver classes (`eab2a65c`), after = this branch. One JVM at a time, `-Xmx2g`, memory gate
(free >= 4 GB, commit headroom >= 8 GB) before every JVM. Process CPU uses `getProcessCpuTime` (15.6 ms Windows
granularity); allocations are `ThreadMXBean.getThreadAllocatedBytes` summed over all threads.

### 6.1 chart_xy (XYGraph, canvas 1400x800, antialias on, 30 pan + 30 zoom + 30 same-view frames)

| metric | 1M before (circular) | 1M after (packed) | 10M before (circular) | 10M after (packed) |
|---|---|---|---|---|
| load ms | 224.8 | 40.5 | 1041.5 | 138.0 |
| first paint ms | 378.3 | 134.6 | 3818.8 | 191.2 |
| heap after load MB | 121.7 | 19.2 | 1187.2 | 157.2 |
| pan frame p50/p95/max ms | 283.9 / 377.4 / 597.2 | 60.6 / 126.2 / 156.7 | 1378.1 / 1777.6 / 1908.2 | 135.0 / 154.0 / 169.9 |
| zoom frame p50/p95/max ms | 203.2 / 283.0 / 383.1 | 37.1 / 57.4 / 66.6 | 1128.6 / 1293.8 / 1375.7 | 125.8 / 144.0 / 157.1 |
| same-view repaint p50 ms | 200.1 | 46.1 | 1137.4 | 102.7 |
| process CPU per pan frame p50 ms | 328.1 | 93.8 | 1500.0 | 125.0 |
| allocated per pan frame p50 | 42.1 MB | 0.99 MB | 405.3 MB | 1.05 MB |
| allocated per same-view frame | 33.9 MB | 83.8 KB | 333.8 MB | 91.7 KB |
| Control.print SHA-256 full / pan / zoom | `d0aa6fec` / `6533527f` / `fc396b11` | identical | `c9a21198` / `8afb5a1b` / `4850e3a3` | identical |

The 10M before run held 10M `Sample` objects (118 bytes each measured at 1M, 1187 MB retained), sized to fit -Xmx2g.
The remaining after-cost per frame is the rest of XYGraph (axes, tick labels, grid) and the GDI+ antialiased polyline.

### 6.2 heatmap_intensity (ColorMap, IntensityGraphFigure)

| metric | before | after |
|---|---|---|
| 48 `drawImage` outputs (every predefined map x autoscale x interpolate x shrink, double and int wrappers) | `8682b776...` | identical |
| 4096x4096 LUT -> 1200x900 (shrink) ms p50/p95/max | 5.21 / 6.07 / 6.18 | 1.07 / 1.53 / 4.37 |
| 4096x4096 LUT -> 4096x4096 ms p50/p95/max | 108.3 / 123.3 / 139.8 | 9.55 / 10.3 / 11.7 |
| figure 3162x3162 crop (zoom) frame ms p50/p95/max | 98.6 / 140.6 / 169.9 | 71.2 / 95.2 / 105.0 |
| allocated per crop frame p50 | 53.1 MB | 8.19 MB |
| heap peak during 30 crops | 1002 MB | 145 MB |
| Control.print SHA-256 at crop 0 / mid / end | `87f27a30` / `e6b16e90` / `da6d62c8` | identical |

`IntensityGraphFigure.setDataWidth/Height` refuse more than 10,000,000 elements (`MAX_ARRAY_SIZE`, an
`IllegalArgumentException` callers can observe), so the figure-level run uses 3162x3162 and the 4096x4096 scale test
runs at `ColorMap.drawImage`. The limit is kept: lifting it would change a public result.

### 6.3 oscilloscope_stream

| metric | before | after |
|---|---|---|
| 8 configs x 120 frames (setValue/setValues; steady, fade, connect, percentage, progression 1-3, tail sizes, antialias, line width 1-2), paint-dispatch SHA-256 over all frames | `6905dfdb...` (2 runs) | identical (2 runs) |
| dispatcher hooks while minimized, 2 s | 64 | 0 |
| hooks while `setVisible(false)`, 2 s | 65 | 0 |
| hooks in the first 1 s after restore / after `setVisible(true)` (cumulative counters) | 94 / 88 | 31 / 18 |
| 1M samples/s for 10 s: samples pushed | 10,010,000 via `setValues` (FIFO keeps `width` values; one value drawn per column, the rest dropped) | 10,040,000 via `pushSamples` (every sample folded into a column envelope) |
| producer CPU ms per s | 43.8 | 81.3 |
| UI CPU ms per s | 48.4 | 207.8 |
| frame (redraw + paint) ms p50/p95/max | 3.65 / 5.87 / 12.9 | 10.0 / 16.2 / 25.4 |
| UI allocations per frame p50 | 44.7 KB | 30.4 KB |

After draws the envelope of every sample (four vertices per column), so its frames draw more than the before frames, which draw one value per column.

Envelope pixel parity (`OscilloscopeEnvelopeTest`, same GC settings as the scope): drawing first/min/max/last per column against drawing every sample at that x is identical for aliased width-1 lines in 24/24 random cases. Antialiased lines and width 2 differ (per case 0 to 536 pixels of a 50..590 x 220 image), because overlapping vertical runs blend and joins differ; that is the stated tolerance.
The widget-level test finds 287 drawn columns equal to the folds of the pushed samples.

### 6.4 Capture modes

| mode | where | why |
|---|---|---|
| `control_print` (`Control.print(GC)` into an Image) | XYGraph and IntensityGraph pixel parity, the differential tests | deterministic, off-screen |
| `paint_dispatch` (one `SWT.Paint` with a GC on a black Image) | oscilloscope frame parity | the scope advances its animation inside paint; `Control.print` of the unmodified scope gave different hashes in 222 of 960 frames between two identical runs |
| `display_copyArea` (shell forced active and topmost, `new GC(display).copyArea`) | on-screen check of the oscilloscope, XYGraph and IntensityGraph benches and every example/snippet | verifies on-screen pixels; recorded, not used as parity proof where the frame count is OS-paint dependent |

### 6.5 Tests

| suite | before | after |
|---|---|---|
| xygraph.test existing (CircularBufferTest, AxisTest, TickFactoryTest) | 15/15 | 15/15 |
| DecimationCoreTest (new) | n/a | 6/6 |
| TraceDecimationDifferentialTest (new): 384 random views (246 decimated), 40 log-axis views (30 decimated), 20 append steps, 24 Control.print comparisons, hidden-work test | n/a | pass |
| visualization widgets.tests, one JVM per class (9 classes) | 9/9 | 9/9 |
| visualization widgets.tests, all 9 classes in one JVM | native crash in `swt-gdip-win32` (GDI+ font create/delete under `LinearScale.calcMargin`, hs_err pid 26828) | same crash (hs_err pid 25856); pre-existing |
| oscilloscope.tests OscilloscopeTest | 5/5 | 5/5 |
| OscilloscopeEnvelopeTest (new) | n/a | 2/2 |
| GTK, Cocoa | NOT_RUN | NOT_RUN |

### 6.6 API coverage

`NEBULA_VIZ_API_COVERAGE.tsv` lists every public/protected declaration of the xygraph, visualization widgets and
oscilloscope main bundles (source census, `harness/api_census_source.py`, base `eab2a65c` vs head): 2,343
declarations in 149 types. 1,907 are in files whose git blob is identical; 338 are existing declarations of the 6
changed types, each mapped to its differential/parity evidence; 98 are additive (22 outside the `internal` packages, one of them a member of the package-private `TraceDecimationPainter`; the API additions are
`PackedXYDataProvider`, `Trace.setDecimationEnabled/isDecimationEnabled`, `Oscilloscope.setSamplesPerColumn/
getSamplesPerColumn/pushSamples`, `OscilloscopeDispatcher.isParked`); 0 removed. The javap listing
(`harness/api_coverage.py`) re-derives it from class files (queued in `TESTS_TO_RUN.md`).

## 7. Not done / not claimed

- JNI min/max kernel: NOT_BUILT. The Java worker builds the 10M pyramid and decimates a view in the times listed in
  `evidence/` (worker timings); there was no measured need, so the Java twin is the only implementation.
- GWT/RWT: the shared atoms follow the GWT rules (int/double state; the one `long` is `Math.round`'s result cast at
  once, as in `LinearScale`; `ColorMapRaster` partitions rows with a local `long` product), but the SWT #117 GWT gate
  was NOT_RUN for these files.
- Views with NaN or out-of-y-range samples in the visible range, step/bar/area/point traces, point markers, point style
  providers and error bars use the unchanged per-sample loop (exact, O(visible samples)).
- If a worker result takes longer than `waitMillis` (250 ms), the trace shows the previous decimated polyline for that
  frame and repaints once when the result arrives; never observed in the runs above (first paint of 10M included the
  trace).
- Oscilloscope scrolling frames still allocate the two exact-length `int[]` lines `GC.drawPolyline` needs (about 30 KB
  per frame for a 1200-px scope); steady scopes reuse them.
- The shared viewport test command `run-matrix.sh` had not landed in the SWT fork when these numbers were taken (master
  tree and open PRs checked); the leaf's own harness (`harness/`) was used.
