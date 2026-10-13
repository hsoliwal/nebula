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
