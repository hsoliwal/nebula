# Package declarations: paperclips

[All packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md) · [Coverage](../COVERAGE.md)

This is a navigation group, not an additional build module. Source revision and inventory rules are recorded in the package index. All entries remain included; a declaration witness is not whole-package semantic review.

## org.eclipse.nebula.paperclips.core

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; production/build
- Declaration witness: [AbstractIterator.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/AbstractIterator.java); source files: `AbstractIterator.java`, `AbstractPiece.java`, `AlignPrint.java`, `BackgroundPrint.java`, `BigPrint.java`, `BreakPrint.java`, `ColumnPrint.java`, `CompositeEntry.java`, `CompositePiece.java`, `DebugPrint.java`, `EmptyPrint.java`, `ImagePrint.java`, `LayerEntry.java`, `LayerEntryIterator.java`, `LayerPrint.java`, `LinePrint.java`, `Margins.java`, `Messages.java`, `NoBreakPrint.java`, `NullPrintPiece.java`, `PageEnumeration.java`, `PaperClips.java`, `Print.java`, `PrintIterator.java`, `PrintJob.java`, `PrintPiece.java`, `RotatePrint.java`, `ScalePrint.java`, `SeriesPrint.java`, `SidewaysPrint.java`
- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/core](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/core); owner `widgets/paperclips/org.eclipse.nebula.paperclips.tests`; test
- Declaration witness: [AlignPrintTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/core/AlignPrintTest.java); source files: `AlignPrintTest.java`, `BackgroundPrintTest.java`, `BigPrintTest.java`, `BorderPrintTest.java`, `BorderStub.java`, `BreakPrintTest.java`, `CellBackgroundProviderStub.java`, `ColumnPrintTest.java`, `DefaultCellBackgroundProviderTest.java`, `DefaultGridLookTest.java`, `DefaultPageNumberFormatTest.java`, `EmptyPrintTest.java`, `GapBorderTest.java`, `ImagePrintTest.java`, `LayerEntryTest.java`, `LayerPrintTest.java`, `LineBorderTest.java`, `LineBreakPrintTest.java`, `LinePrintTest.java`, `MarginsTest.java`, `NoBreakPrintTest.java`, `PageNumberFormatStub.java`, `PageNumberPageDecorationTest.java`, `PageNumberPrintTest.java`, `PagePrintTest.java`, `PrintJobTest.java`, `PrintStub.java`, `RotatePrintTest.java`, `ScalePrintTest.java`, `SeriesPrintTest.java`, `SidewaysPrintTest.java`, `SimplePageDecorationTest.java`, `StyledTextPrintTest.java`, `TextPrintTest.java`, `TextStyleTest.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.paperclips.core.border`](paperclips.md#orgeclipsenebulapaperclipscoreborder), [`org.eclipse.nebula.paperclips.core.grid`](paperclips.md#orgeclipsenebulapaperclipscoregrid), [`org.eclipse.nebula.paperclips.core.internal`](paperclips.md#orgeclipsenebulapaperclipscoreinternal), [`org.eclipse.nebula.paperclips.core.internal.piece`](paperclips.md#orgeclipsenebulapaperclipscoreinternalpiece), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil), [`org.eclipse.nebula.paperclips.core.page`](paperclips.md#orgeclipsenebulapaperclipscorepage), [`org.eclipse.nebula.paperclips.core.text`](paperclips.md#orgeclipsenebulapaperclipscoretext)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core`
- Existing package prose: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/package.html](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/package.html)
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.border

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/border](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/border); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; production/build
- Declaration witness: [AbstractBorderPainter.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/border/AbstractBorderPainter.java); source files: `AbstractBorderPainter.java`, `Border.java`, `BorderPainter.java`, `BorderPrint.java`, `GapBorder.java`, `LineBorder.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.border.internal`](paperclips.md#orgeclipsenebulapaperclipscoreborderinternal), [`org.eclipse.nebula.paperclips.core.grid`](paperclips.md#orgeclipsenebulapaperclipscoregrid), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.border`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.border.internal

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/border/internal](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/border/internal); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; internal production
- Declaration witness: [BorderIterator.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/border/internal/BorderIterator.java); source files: `BorderIterator.java`, `BorderPiece.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.border`](paperclips.md#orgeclipsenebulapaperclipscoreborder), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.border.internal`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.grid

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/grid](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/grid); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; production/build
- Declaration witness: [BasicGridLookPainter.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/grid/BasicGridLookPainter.java); source files: `BasicGridLookPainter.java`, `CellBackgroundProvider.java`, `DefaultCellBackgroundProvider.java`, `DefaultGridLook.java`, `GridCell.java`, `GridColumn.java`, `GridLook.java`, `GridLookPainter.java`, `GridMargins.java`, `GridPrint.java`
- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/core/grid](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/core/grid); owner `widgets/paperclips/org.eclipse.nebula.paperclips.tests`; test
- Declaration witness: [GridCellTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/core/grid/GridCellTest.java); source files: `GridCellTest.java`, `GridColumnTest.java`, `GridPrintTest.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.border`](paperclips.md#orgeclipsenebulapaperclipscoreborder), [`org.eclipse.nebula.paperclips.core.grid.internal`](paperclips.md#orgeclipsenebulapaperclipscoregridinternal), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.grid`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.grid.internal

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/grid/internal](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/grid/internal); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; internal production
- Declaration witness: [DefaultGridLookPainter.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/grid/internal/DefaultGridLookPainter.java); source files: `DefaultGridLookPainter.java`, `DefaultGridMargins.java`, `GridCellImpl.java`, `GridCellIterator.java`, `GridIterator.java`, `GridLookPainterPiece.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.border`](paperclips.md#orgeclipsenebulapaperclipscoreborder), [`org.eclipse.nebula.paperclips.core.grid`](paperclips.md#orgeclipsenebulapaperclipscoregrid), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.grid.internal;x-internal:=true`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.internal

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/internal](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/internal); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; internal production
- Declaration witness: [LayerEntryImpl.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/internal/LayerEntryImpl.java); source files: `LayerEntryImpl.java`, `LayerEntryIteratorImpl.java`, `LayerIterator.java`, `RotatePiece.java`
- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/core/internal](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/core/internal); owner `widgets/paperclips/org.eclipse.nebula.paperclips.tests`; test
- Declaration witness: [SWTUtilTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/core/internal/SWTUtilTest.java); source files: `SWTUtilTest.java`, `UtilTest.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.internal;x-internal:=true`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.internal.piece

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/internal/piece](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/internal/piece); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; internal production
- Declaration witness: [EmptyPiece.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/internal/piece/EmptyPiece.java); source files: `EmptyPiece.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.internal.piece`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.internal.util

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/internal/util](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/internal/util); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; internal production
- Declaration witness: [PaperClipsUtil.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/internal/util/PaperClipsUtil.java); source files: `PaperClipsUtil.java`, `PrintSizeStrategy.java`, `ResourcePool.java`, `SWTUtil.java`, `Util.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.internal.util;x-internal:=true`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.page

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/page](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/page); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; production/build
- Declaration witness: [DefaultPageNumberFormat.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/page/DefaultPageNumberFormat.java); source files: `DefaultPageNumberFormat.java`, `PageDecoration.java`, `PageNumber.java`, `PageNumberFormat.java`, `PageNumberPageDecoration.java`, `PageNumberPrint.java`, `PagePrint.java`, `SimplePageDecoration.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil), [`org.eclipse.nebula.paperclips.core.text`](paperclips.md#orgeclipsenebulapaperclipscoretext)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.page`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.text

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/text](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/text); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; production/build
- Declaration witness: [LineBreakPrint.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/text/LineBreakPrint.java); source files: `LineBreakPrint.java`, `StyledTextPrint.java`, `TextPrint.java`, `TextStyle.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.internal.piece`](paperclips.md#orgeclipsenebulapaperclipscoreinternalpiece), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil), [`org.eclipse.nebula.paperclips.core.text.internal`](paperclips.md#orgeclipsenebulapaperclipscoretextinternal)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.text`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.core.text.internal

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/text/internal](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/text/internal); owner `widgets/paperclips/org.eclipse.nebula.paperclips.core`; internal production
- Declaration witness: [TextPiece.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/src/org/eclipse/nebula/paperclips/core/text/internal/TextPiece.java); source files: `TextPiece.java`, `TextPrintPiece.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.internal.util`](paperclips.md#orgeclipsenebulapaperclipscoreinternalutil), [`org.eclipse.nebula.paperclips.core.text`](paperclips.md#orgeclipsenebulapaperclipscoretext)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.core`: `org.eclipse.nebula.paperclips.core.text.internal`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.snippets

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.snippets/src/org/eclipse/nebula/paperclips/snippets](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.snippets/src/org/eclipse/nebula/paperclips/snippets); owner `widgets/paperclips/org.eclipse.nebula.paperclips.snippets`; example
- Declaration witness: [AlignPrintExample.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.snippets/src/org/eclipse/nebula/paperclips/snippets/AlignPrintExample.java); source files: `AlignPrintExample.java`, `BreakPrintExample.java`, `ColumnPrintExample.java`, `GridPrintCellClippingAllowClipFirstRow.java`, `GridPrintCellClippingExample.java`, `GridPrintExample.java`, `GridPrintVerticalAlignmentExample.java`, `ImageCaptureExample.java`, `NoBreakPrintExample.java`, `PagePrintExample.java`, `PaperClipsExample.java`, `Snippet1.java`, `Snippet2.java`, `Snippet3.java`, `Snippet4.java`, `Snippet5.java`, `Snippet6.java`, `Snippet7.java`, `Snippet8.java`, `StyledTextPrintExample.java`, `TutorialExample1.java`, `TutorialExample2.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.border`](paperclips.md#orgeclipsenebulapaperclipscoreborder), [`org.eclipse.nebula.paperclips.core.grid`](paperclips.md#orgeclipsenebulapaperclipscoregrid), [`org.eclipse.nebula.paperclips.core.page`](paperclips.md#orgeclipsenebulapaperclipscorepage), [`org.eclipse.nebula.paperclips.core.text`](paperclips.md#orgeclipsenebulapaperclipscoretext), [`org.eclipse.nebula.paperclips.widgets`](paperclips.md#orgeclipsenebulapaperclipswidgets)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.snippets`: `org.eclipse.nebula.paperclips.snippets`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.tests.benchmark

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/tests/benchmark](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/tests/benchmark); owner `widgets/paperclips/org.eclipse.nebula.paperclips.tests`; test
- Declaration witness: [Benchmark.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/tests/benchmark/Benchmark.java); source files: `Benchmark.java`, `BenchmarkTest.java`, `Clock.java`, `StopClock.java`, `StopClockTest.java`, `SystemClock.java`, `SystemClockTest.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.tests`: `org.eclipse.nebula.paperclips.tests.benchmark`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.tests.main

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/tests/main](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/tests/main); owner `widgets/paperclips/org.eclipse.nebula.paperclips.tests`; test
- Declaration witness: [SnippetBenchmarks.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/src/org/eclipse/nebula/paperclips/tests/main/SnippetBenchmarks.java); source files: `SnippetBenchmarks.java`, `TestWhetherBorderPrintHoldsSomeContentForLastPage.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.border`](paperclips.md#orgeclipsenebulapaperclipscoreborder), [`org.eclipse.nebula.paperclips.core.grid`](paperclips.md#orgeclipsenebulapaperclipscoregrid), [`org.eclipse.nebula.paperclips.core.text`](paperclips.md#orgeclipsenebulapaperclipscoretext), [`org.eclipse.nebula.paperclips.snippets`](paperclips.md#orgeclipsenebulapaperclipssnippets), [`org.eclipse.nebula.paperclips.tests.benchmark`](paperclips.md#orgeclipsenebulapaperclipstestsbenchmark)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.paperclips.widgets

- Origin: [widgets/paperclips/org.eclipse.nebula.paperclips.widgets/src/org/eclipse/nebula/paperclips/widgets](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.widgets/src/org/eclipse/nebula/paperclips/widgets); owner `widgets/paperclips/org.eclipse.nebula.paperclips.widgets`; production/build
- Declaration witness: [PrintPieceCanvas.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.widgets/src/org/eclipse/nebula/paperclips/widgets/PrintPieceCanvas.java); source files: `PrintPieceCanvas.java`, `PrintPreview.java`, `PrintViewer.java`, `RotateClockwisePrintPiece.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore)
- Export accounting: `widgets/paperclips/org.eclipse.nebula.paperclips.widgets`: `org.eclipse.nebula.paperclips.widgets`
- Existing package prose: [widgets/paperclips/org.eclipse.nebula.paperclips.widgets/src/org/eclipse/nebula/paperclips/widgets/package.html](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.widgets/src/org/eclipse/nebula/paperclips/widgets/package.html)
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.paperclips.example

- Origin: [widgets/paperclips/org.eclipse.nebula.widgets.paperclips.example/src/org/eclipse/nebula/widgets/paperclips/example](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.widgets.paperclips.example/src/org/eclipse/nebula/widgets/paperclips/example); owner `widgets/paperclips/org.eclipse.nebula.widgets.paperclips.example`; example
- Declaration witness: [PaperclipsExampleTab.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.widgets.paperclips.example/src/org/eclipse/nebula/widgets/paperclips/example/PaperclipsExampleTab.java); source files: `PaperclipsExampleTab.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.examples`](examples.md#orgeclipsenebulaexamples), [`org.eclipse.nebula.paperclips.core`](paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.grid`](paperclips.md#orgeclipsenebulapaperclipscoregrid), [`org.eclipse.nebula.paperclips.core.text`](paperclips.md#orgeclipsenebulapaperclipscoretext), [`org.eclipse.nebula.paperclips.widgets`](paperclips.md#orgeclipsenebulapaperclipswidgets)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


