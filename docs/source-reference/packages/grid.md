# Package declarations: grid

[All packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md) · [Coverage](../COVERAGE.md)

This is a navigation group, not an additional build module. Source revision and inventory rules are recorded in the package index. All entries remain included; a declaration witness is not whole-package semantic review.

## org.eclipse.nebula.jface.gridviewer

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/jface/gridviewer](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/jface/gridviewer); owner `widgets/grid/org.eclipse.nebula.widgets.grid`; production/build
- Declaration witness: [CheckEditingSupport.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/jface/gridviewer/CheckEditingSupport.java); source files: `CheckEditingSupport.java`, `GridColumnLabelProvider.java`, `GridColumnLayout.java`, `GridTableViewer.java`, `GridTreeViewer.java`, `GridViewerColumn.java`, `GridViewerEditor.java`, `GridViewerRow.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.jface.gridviewer.internal`](grid.md#orgeclipsenebulajfacegridviewerinternal), [`org.eclipse.nebula.widgets.grid`](grid.md#orgeclipsenebulawidgetsgrid)
- Export accounting: `widgets/grid/org.eclipse.nebula.widgets.grid`: `org.eclipse.nebula.jface.gridviewer`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.jface.gridviewer.internal

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/jface/gridviewer/internal](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/jface/gridviewer/internal); owner `widgets/grid/org.eclipse.nebula.widgets.grid`; internal production
- Declaration witness: [CellSelection.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/jface/gridviewer/internal/CellSelection.java); source files: `CellSelection.java`, `SelectionWithFocusRow.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil)
- Export accounting: `widgets/grid/org.eclipse.nebula.widgets.grid`: `org.eclipse.nebula.jface.gridviewer.internal;x-internal:=true`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid

- Origin: [.m3/nebula-visible-range/src/test/java/org/eclipse/nebula/widgets/grid](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/.m3/nebula-visible-range/src/test/java/org/eclipse/nebula/widgets/grid); owner `.m3/nebula-visible-range`; fork tooling/proof
- Declaration witness: [GridVisibleRangeDiffContract.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/.m3/nebula-visible-range/src/test/java/org/eclipse/nebula/widgets/grid/GridVisibleRangeDiffContract.java); source files: `GridVisibleRangeDiffContract.java`
- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid); owner `widgets/grid/org.eclipse.nebula.widgets.grid.test`; test
- Declaration witness: [Fixture.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/Fixture.java); source files: `Fixture.java`, `GridBlitScroll_Test.java`, `GridBodyDefer_Test.java`, `GridCheckLanes_Test.java`, `GridColumnGroup_Test.java`, `GridColumnWindow_Test.java`, `GridColumn_Test.java`, `GridFixedColumn_Test.java`, `GridGCProxy_Test.java`, `GridIssue700_Test.java`, `GridItem_Test.java`, `GridModelJobs_Test.java`, `GridPaintDAG_Test.java`, `GridPaintPreparation_Test.java`, `GridPinnedRows_Test.java`, `GridPrefetch_Test.java`, `GridResidency_Test.java`, `GridRowPermutation_Test.java`, `GridRowSpanIndex_Test.java`, `GridScrollbarSettle_Test.java`, `GridSetDataRedraw_Test.java`, `GridSnippet1.java`, `GridSwtScreenshotCapture.java`, `GridTestUtil.java`, `GridViewportCoordinates_Test.java`, `GridViewportDistillation_Test.java`, `GridVirtualHeights_Test.java`, `GridVirtualItemListCapacity_Test.java`, `GridVisibleRangeSupport_Test.java`, `Grid_Test.java`, `TestWithMain.java`
- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid); owner `widgets/grid/org.eclipse.nebula.widgets.grid`; production/build
- Declaration witness: [AbstractGridIdentityOccurrenceCounter.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/AbstractGridIdentityOccurrenceCounter.java); source files: `AbstractGridIdentityOccurrenceCounter.java`, `AbstractGridOccurrenceDifference.java`, `AbstractInternalWidget.java`, `AbstractRenderer.java`, `AdaptedDataVisualizer.java`, `DataVisualizer.java`, `DefaultCellRenderer.java`, `DefaultEmptyCellRenderer.java`, `FixedGridColumns.java`, `Grid.java`, `GridCellRenderer.java`, `GridCellSpanManager.java`, `GridColumn.java`, `GridColumnGroup.java`, `GridDifferenceCheckpoint.java`, `GridDifferenceIdentityPolicy.java`, `GridDragSourceEffect.java`, `GridDropTargetEffect.java`, `GridEditor.java`, `GridEqualityOccurrenceDifference.java`, `GridFooterRenderer.java`, `GridGCProxy.java`, `GridGCStateDAG.java`, `GridHeaderEditor.java`, `GridHeaderRenderer.java`, `GridIdentityOccurrenceCounter.java`, `GridIdentityOccurrenceDifference.java`, `GridIdentityOccurrenceTable.java`, `GridItem.java`, `GridItemDataVisualizer.java`, `GridLazyIntPriorityFrontier.java`, `GridModelJobs.java`, `GridOccurrenceDifference.java`, `GridPaintDAG.java`, `GridRowAddressedDataVisualizer.java`, `GridRowAddressedItemDataVisualizer.java`, `GridRowSpanIndex.java`, `GridSelectionAtom.java`, `GridSelectionType.java`, `GridTransform.java`, `GridUtils.java`, `GridViewportDamage.java`, `GridViewportProjection.java`, `GridVirtualItemList.java`, `GridVisibleRangeDiff.java`, `GridVisibleRangeDiffComposition.java`, `GridVisibleRangeEventAtom.java`, `GridVisibleRangeSupport.java`, `IInternalWidget.java`, `IRenderer.java`, `InsertMark.java`, `Win7RendererSupport.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.grid.internal`](grid.md#orgeclipsenebulawidgetsgridinternal), [`org.eclipse.nebula.widgets.grid.internal.win7`](grid.md#orgeclipsenebulawidgetsgridinternalwin7), [`org.eclipse.nebula.widgets.grid.model`](grid.md#orgeclipsenebulawidgetsgridmodel)
- Export accounting: `widgets/grid/org.eclipse.nebula.widgets.grid.test`: `org.eclipse.nebula.widgets.grid`, `widgets/grid/org.eclipse.nebula.widgets.grid`: `org.eclipse.nebula.widgets.grid`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid.bench

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/bench](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/bench); owner `widgets/grid/org.eclipse.nebula.widgets.grid.test`; test
- Declaration witness: [ClassicDriver.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/bench/ClassicDriver.java); source files: `ClassicDriver.java`, `GridModelBench.java`, `PackedDriver.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.grid`](grid.md#orgeclipsenebulawidgetsgrid), [`org.eclipse.nebula.widgets.grid.model`](grid.md#orgeclipsenebulawidgetsgridmodel)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid.css

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.css/src/org/eclipse/nebula/widgets/grid/css](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.css/src/org/eclipse/nebula/widgets/grid/css); owner `widgets/grid/org.eclipse.nebula.widgets.grid.css`; production/build
- Declaration witness: [CSSEngineHelper.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.css/src/org/eclipse/nebula/widgets/grid/css/CSSEngineHelper.java); source files: `CSSEngineHelper.java`, `GridElement.java`, `GridElementProvider.java`, `GridPropertyHandler.java`, `TestLCN.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.grid`](grid.md#orgeclipsenebulawidgetsgrid)
- Export accounting: `widgets/grid/org.eclipse.nebula.widgets.grid.css`: `org.eclipse.nebula.widgets.grid.css`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid.example

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.example/src/org/eclipse/nebula/widgets/grid/example](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example/src/org/eclipse/nebula/widgets/grid/example); owner `widgets/grid/org.eclipse.nebula.widgets.grid.example`; example
- Declaration witness: [GridExampleTab.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example/src/org/eclipse/nebula/widgets/grid/example/GridExampleTab.java); source files: `GridExampleTab.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.examples`](examples.md#orgeclipsenebulaexamples), [`org.eclipse.nebula.widgets.grid`](grid.md#orgeclipsenebulawidgetsgrid)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid.example.e4

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/src/org/eclipse/nebula/widgets/grid/example/e4](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/src/org/eclipse/nebula/widgets/grid/example/e4); owner `widgets/grid/org.eclipse.nebula.widgets.grid.example.e4`; example
- Declaration witness: [Activator.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/src/org/eclipse/nebula/widgets/grid/example/e4/Activator.java); source files: `Activator.java`
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid.example.e4.handlers

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/src/org/eclipse/nebula/widgets/grid/example/e4/handlers](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/src/org/eclipse/nebula/widgets/grid/example/e4/handlers); owner `widgets/grid/org.eclipse.nebula.widgets.grid.example.e4`; example
- Declaration witness: [AboutHandler.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/src/org/eclipse/nebula/widgets/grid/example/e4/handlers/AboutHandler.java); source files: `AboutHandler.java`, `QuitHandler.java`
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid.example.e4.parts

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/src/org/eclipse/nebula/widgets/grid/example/e4/parts](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/src/org/eclipse/nebula/widgets/grid/example/e4/parts); owner `widgets/grid/org.eclipse.nebula.widgets.grid.example.e4`; example
- Declaration witness: [FirstPart.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/src/org/eclipse/nebula/widgets/grid/example/e4/parts/FirstPart.java); source files: `FirstPart.java`, `FourthPart.java`, `SecondPart.java`, `ThirdPart.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.grid`](grid.md#orgeclipsenebulawidgetsgrid)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid.internal

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/internal](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/internal); owner `widgets/grid/org.eclipse.nebula.widgets.grid`; internal production
- Declaration witness: [BranchRenderer.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/internal/BranchRenderer.java); source files: `BranchRenderer.java`, `CheckBoxRenderer.java`, `DefaultBottomLeftRenderer.java`, `DefaultColumnFooterRenderer.java`, `DefaultColumnGroupHeaderRenderer.java`, `DefaultColumnHeaderRenderer.java`, `DefaultDropPointRenderer.java`, `DefaultEmptyColumnFooterRenderer.java`, `DefaultEmptyColumnHeaderRenderer.java`, `DefaultEmptyRowHeaderRenderer.java`, `DefaultFocusRenderer.java`, `DefaultInsertMarkRenderer.java`, `DefaultRowHeaderRenderer.java`, `DefaultTopLeftRenderer.java`, `ExpandToggleRenderer.java`, `GridToolTip.java`, `GroupToggleRenderer.java`, `IScrollBarProxy.java`, `NullScrollBarProxy.java`, `ScrollBarProxyAdapter.java`, `SortArrowRenderer.java`, `TextUtils.java`, `ToggleRenderer.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.grid`](grid.md#orgeclipsenebulawidgetsgrid)
- Export accounting: `widgets/grid/org.eclipse.nebula.widgets.grid`: `org.eclipse.nebula.widgets.grid.internal;x-internal:=true`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid.internal.win7

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/internal/win7](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/internal/win7); owner `widgets/grid/org.eclipse.nebula.widgets.grid`; internal production
- Declaration witness: [Win7ColumnGroupHeaderRenderer.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/internal/win7/Win7ColumnGroupHeaderRenderer.java); source files: `Win7ColumnGroupHeaderRenderer.java`, `Win7ColumnHeaderUtil.java`, `Win7EmptyColumnHeaderRenderer.java`, `Win7GridColumnHeaderRenderer.java`, `Win7PaletteProvider.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.grid`](grid.md#orgeclipsenebulawidgetsgrid), [`org.eclipse.nebula.widgets.grid.internal`](grid.md#orgeclipsenebulawidgetsgridinternal)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.grid.model

- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.test/gwt-gate/src/org/eclipse/nebula/widgets/grid/model](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/gwt-gate/src/org/eclipse/nebula/widgets/grid/model); owner `widgets/grid/org.eclipse.nebula.widgets.grid.test`; test
- Declaration witness: [GateEntry.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/gwt-gate/src/org/eclipse/nebula/widgets/grid/model/GateEntry.java); source files: `GateEntry.java`, `GateJava.java`, `GateWorkload.java`
- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.test/gwt-gate/src/org/eclipse/nebula/widgets/grid/model/super/org/eclipse/nebula/widgets/grid/model](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/gwt-gate/src/org/eclipse/nebula/widgets/grid/model/super/org/eclipse/nebula/widgets/grid/model); owner `widgets/grid/org.eclipse.nebula.widgets.grid.test`; test
- Declaration witness: [HeapSampler.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/gwt-gate/src/org/eclipse/nebula/widgets/grid/model/super/org/eclipse/nebula/widgets/grid/model/HeapSampler.java); source files: `HeapSampler.java`
- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/model](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/model); owner `widgets/grid/org.eclipse.nebula.widgets.grid.test`; test
- Declaration witness: [HeapGrowth_Test.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/model/HeapGrowth_Test.java); source files: `HeapGrowth_Test.java`, `RowBitLanes_Test.java`, `RowGrouping_Test.java`, `RowPermutation_Test.java`, `RowSelection_Test.java`, `RowStateLanes_Test.java`
- Origin: [widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/model](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/model); owner `widgets/grid/org.eclipse.nebula.widgets.grid`; production/build
- Declaration witness: [HeapGrowth.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/model/HeapGrowth.java); source files: `HeapGrowth.java`, `HeapSampler.java`, `IntPermutationSort.java`, `RowBitLanes.java`, `RowComparator.java`, `RowGrouping.java`, `RowPermutation.java`, `RowPredicate.java`, `RowSelection.java`, `RowSparseBits.java`, `RowStateLanes.java`, `RowView.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil)
- Export accounting: `widgets/grid/org.eclipse.nebula.widgets.grid`: `org.eclipse.nebula.widgets.grid.model`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


