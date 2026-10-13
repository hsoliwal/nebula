<!-- SPDX-License-Identifier: EPL-2.0 -->
# Tests to run (queued while the user is in an interview)

Paused 2026-10-13 by the user ("please stop running the tests"; then "fix it all, we'll test after the interview").
Run these in order, one JVM at a time, each behind the memory gate (`harness/memwait.sh`: free >= 4 GB, commit
headroom >= 8 GB). Scripts live in `harness/` (copies of `C:\m3-work-c\viz-build\scripts`); `B=/c/m3-work-c/viz-build`.

| # | what | command | why pending |
|---|---|---|---|
| 1 | recompile after classes | `sh $B/scripts/compile.sh after /c/m3-work-c/nebula-viz` | `ColorMapRaster` band split changed to int-only after the last full compile (quick javac of that file: rc=0) |
| 2 | xygraph: core + differential (incl. new `noDecimationWorkWhileHidden`, `hotListAfterReplacementMatchesLoop`) + existing | `sh $B/scripts/junit.sh after <xygraph.test/src> xyt "-Dorg.eclipse.nebula.visualization.xygraph.decimation.minSamples=2 -Dorg.eclipse.nebula.visualization.xygraph.decimation.waitMillis=60000" ...DecimationCoreTest ...TraceDecimationDifferentialTest ...CircularBufferTest ...AxisTest ...TickFactoryTest` | the hidden-work and replacement tests and `Trace.dataChanged` release were written after the last run (quick javac rc=0) |
| 3 | oscilloscope tests | `sh $B/scripts/junit.sh after <oscilloscope.tests/src> osct "" ...OscilloscopeTest ...multichannel.OscilloscopeEnvelopeTest` | re-run on final code |
| 4 | widgets.tests, one JVM per class, before and after | loop in `evidence/tests-visualization-widgets-perclass.tsv` header | re-run on final code (last run 9/9 both) |
| 5 | examples + snippets, before and after (capture mode display_copyArea) | `sh $B/scripts/examples.sh base $B/base-src; sh $B/scripts/examples.sh after /c/m3-work-c/nebula-viz` | interrupted; runner now prints at capture time because snippets call `System.exit` |
| 6 | bytecode API listing cross-check | `sh $B/scripts/javap_all.sh base; sh $B/scripts/javap_all.sh after; python -I harness/api_coverage.py $B/base/api.javap.txt $B/after/api.javap.txt $B/base/classes $B/after/classes harness/api_evidence.tsv NEBULA_VIZ_API_COVERAGE.javap.tsv` | `NEBULA_VIZ_API_COVERAGE.tsv` is the source census (`harness/api_census_source.py`, CPU only); javap confirms it from class files |
| 7 | XYGraph final bench (screen hash + worker timings) | `bench.sh base XYGraphBench ... 1000000 circular 30`, `bench.sh after ... 1000000 packed 30`, `bench.sh after ... 10000000 packed 30` | adds display_copyArea hash and pyramid/decimation worker ms |
| 8 | IntensityGraph final bench (screen hash) | `bench.sh base IntensityBench ... 3162 30 base-3162 4096`, same for after | adds display_copyArea hash; re-measures after the int band split |
| 9 | Synexia crate SDK run | `mvn -B -o -ntp -f synexia-openrewrite-recipes/recipe-crates/nebula-viz-decimation-20261013/pom.xml verify` | exact postimages, fixed point, drift and missing-source refusal |
| 10 | regenerate crate after any code change | `python -I harness/make_crate.py /c/m3-work-c/nebula-viz eab2a65c451c8e66df1004dcbcb8d892356d8862 HEAD <crate resource dir>` | crate must match the final Nebula head |
| 11 | shared viewport matrix | `run-matrix.sh` from the SWT fork examples, once it lands | not in the SWT fork yet |
| 12 | GWT gate for the shared atoms | SWT #117 `gwt-gate/run_gate.py` over `AxisMapping`, `PackedMinMaxPyramid`, `PolylineBuilder`, `TraceDecimation`, `ColorMapRaster.band` | not run |

GTK and Cocoa: NOT_RUN (Win32 host only).
