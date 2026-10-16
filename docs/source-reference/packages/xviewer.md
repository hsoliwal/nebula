# Package declarations: xviewer

[All packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md) · [Coverage](../COVERAGE.md)

This is a navigation group, not an additional build module. Source revision and inventory rules are recorded in the package index. All entries remain included; a declaration witness is not whole-package semantic review.

## org.eclipse.nebula.widgets.xviewer

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; production/build
- Declaration witness: [Activator.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/Activator.java); source files: `Activator.java`, `IAltLeftClickProvider.java`, `IMultiColumnEditProvider.java`, `IXViewerFactory.java`, `IXViewerLabelProvider.java`, `IXViewerPreComputedColumn.java`, `IXViewerValueColumn.java`, `XPromptChange.java`, `XSubMenuManager.java`, `XViewer.java`, `XViewerCells.java`, `XViewerColumnLabelProvider.java`, `XViewerColumnSorter.java`, `XViewerComputedColumn.java`, `XViewerFactory.java`, `XViewerGradient.java`, `XViewerHtmlReport.java`, `XViewerLabelProvider.java`, `XViewerLoadingReport.java`, `XViewerPreComputedColumnAdapter.java`, `XViewerSorter.java`, `XViewerStyledTextLabelProvider.java`, `XViewerText.java`, `XViewerTextFilter.java`, `XViewerTreeReport.java`, `XViewerValueColumn.java`, `XViewerViewportContentProvider.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer.action`](xviewer.md#orgeclipsenebulawidgetsxvieweraction), [`org.eclipse.nebula.widgets.xviewer.column`](xviewer.md#orgeclipsenebulawidgetsxviewercolumn), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.core.util`](xviewer.md#orgeclipsenebulawidgetsxviewercoreutil), [`org.eclipse.nebula.widgets.xviewer.customize`](xviewer.md#orgeclipsenebulawidgetsxviewercustomize), [`org.eclipse.nebula.widgets.xviewer.customize.dialog`](xviewer.md#orgeclipsenebulawidgetsxviewercustomizedialog), [`org.eclipse.nebula.widgets.xviewer.edit`](xviewer.md#orgeclipsenebulawidgetsxvieweredit), [`org.eclipse.nebula.widgets.xviewer.util`](xviewer.md#orgeclipsenebulawidgetsxviewerutil), [`org.eclipse.nebula.widgets.xviewer.util.internal`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternal), [`org.eclipse.nebula.widgets.xviewer.util.internal.dialog`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternaldialog)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`: `org.eclipse.nebula.widgets.xviewer`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.action

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/action](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/action); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; production/build
- Declaration witness: [ColumnMultiEditAction.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/action/ColumnMultiEditAction.java); source files: `ColumnMultiEditAction.java`, `TableCustomizationAction.java`, `TableCustomizationCustomizeDataAction.java`, `TableCustomizationDropDownAction.java`, `ViewLoadingReportAction.java`, `ViewSelectedCellDataAction.java`, `ViewTableReportAction.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.util`](xviewer.md#orgeclipsenebulawidgetsxviewerutil), [`org.eclipse.nebula.widgets.xviewer.util.internal`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternal), [`org.eclipse.nebula.widgets.xviewer.util.internal.dialog`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternaldialog), [`org.eclipse.nebula.widgets.xviewer.util.internal.images`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternalimages)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`: `org.eclipse.nebula.widgets.xviewer.action`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.column

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/column](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/column); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; production/build
- Declaration witness: [XViewerDaysTillTodayColumn.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/column/XViewerDaysTillTodayColumn.java); source files: `XViewerDaysTillTodayColumn.java`, `XViewerDiffsBetweenColumnsColumn.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.util.internal`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternal)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`: `org.eclipse.nebula.widgets.xviewer.column`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.core.model

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core/src/org/eclipse/nebula/widgets/xviewer/core/model](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core/src/org/eclipse/nebula/widgets/xviewer/core/model); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core`; production/build
- Declaration witness: [ColumnData.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core/src/org/eclipse/nebula/widgets/xviewer/core/model/ColumnData.java); source files: `ColumnData.java`, `ColumnDateFilter.java`, `ColumnFilterData.java`, `CustomizeData.java`, `DateRangeType.java`, `FilterData.java`, `SortDataType.java`, `SortingData.java`, `XViewerAlign.java`, `XViewerColumn.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer.core.util`](xviewer.md#orgeclipsenebulawidgetsxviewercoreutil)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core`: `org.eclipse.nebula.widgets.xviewer.core.model`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.core.util

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core/src/org/eclipse/nebula/widgets/xviewer/core/util](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core/src/org/eclipse/nebula/widgets/xviewer/core/util); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core`; production/build
- Declaration witness: [CollectionsUtil.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core/src/org/eclipse/nebula/widgets/xviewer/core/util/CollectionsUtil.java); source files: `CollectionsUtil.java`, `Strings.java`, `XViewerUtil.java`, `XmlUtil.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core`: `org.eclipse.nebula.widgets.xviewer.core.util`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.customize

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/customize](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/customize); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; production/build
- Declaration witness: [ColumnFilterDataUI.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/customize/ColumnFilterDataUI.java); source files: `ColumnFilterDataUI.java`, `CustomizeDataLabelProvider.java`, `CustomizeManager.java`, `FileStoreCustomizations.java`, `FilterDataUI.java`, `IXViewerCustomizations.java`, `SearchDataUI.java`, `XViewerCustomMenu.java`, `XViewerCustomizations.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.action`](xviewer.md#orgeclipsenebulawidgetsxvieweraction), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.core.util`](xviewer.md#orgeclipsenebulawidgetsxviewercoreutil), [`org.eclipse.nebula.widgets.xviewer.util`](xviewer.md#orgeclipsenebulawidgetsxviewerutil), [`org.eclipse.nebula.widgets.xviewer.util.internal`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternal), [`org.eclipse.nebula.widgets.xviewer.util.internal.dialog`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternaldialog)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`: `org.eclipse.nebula.widgets.xviewer.customize`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.customize.dialog

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/customize/dialog](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/customize/dialog); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; production/build
- Declaration witness: [CustomizationDataSelectionDialog.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/customize/dialog/CustomizationDataSelectionDialog.java); source files: `CustomizationDataSelectionDialog.java`, `XViewerCustDialogDragDrop.java`, `XViewerCustomizeDialog.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.core.util`](xviewer.md#orgeclipsenebulawidgetsxviewercoreutil), [`org.eclipse.nebula.widgets.xviewer.customize`](xviewer.md#orgeclipsenebulawidgetsxviewercustomize), [`org.eclipse.nebula.widgets.xviewer.util`](xviewer.md#orgeclipsenebulawidgetsxviewerutil), [`org.eclipse.nebula.widgets.xviewer.util.internal`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternal), [`org.eclipse.nebula.widgets.xviewer.util.internal.dialog`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternaldialog)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`: `org.eclipse.nebula.widgets.xviewer.customize.dialog`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.edit

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/edit](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/edit); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; production/build
- Declaration witness: [CellEditDescriptor.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/edit/CellEditDescriptor.java); source files: `CellEditDescriptor.java`, `DefaultXViewerControlFactory.java`, `ExtendedViewerColumn.java`, `IExtendedViewerColumn.java`, `XViewerControlFactory.java`, `XViewerConverter.java`, `XViewerEditAdapter.java`, `XViewerMultiEditAdapter.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`: `org.eclipse.nebula.widgets.xviewer.edit`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.example

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example`; example
- Declaration witness: [Activator.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/Activator.java); source files: `Activator.java`, `MyDefaultCustomizations.java`, `MyPreComputedColumn.java`, `MyXViewer.java`, `MyXViewerContentProvider.java`, `MyXViewerConverter.java`, `MyXViewerCustomizations.java`, `MyXViewerFactory.java`, `MyXViewerLabelProvider.java`, `MyXViewerStyledTextLabelProvider.java`, `MyXViewerTest.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.customize`](xviewer.md#orgeclipsenebulawidgetsxviewercustomize), [`org.eclipse.nebula.widgets.xviewer.customize.dialog`](xviewer.md#orgeclipsenebulawidgetsxviewercustomizedialog), [`org.eclipse.nebula.widgets.xviewer.edit`](xviewer.md#orgeclipsenebulawidgetsxvieweredit), [`org.eclipse.nebula.widgets.xviewer.example.images`](xviewer.md#orgeclipsenebulawidgetsxviewerexampleimages), [`org.eclipse.nebula.widgets.xviewer.example.model`](xviewer.md#orgeclipsenebulawidgetsxviewerexamplemodel), [`org.eclipse.nebula.widgets.xviewer.example.util`](xviewer.md#orgeclipsenebulawidgetsxviewerexampleutil), [`org.eclipse.nebula.widgets.xviewer.util`](xviewer.md#orgeclipsenebulawidgetsxviewerutil)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example`: `org.eclipse.nebula.widgets.xviewer.example`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.example.images

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/images](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/images); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example`; example
- Declaration witness: [MyImageCache.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/images/MyImageCache.java); source files: `MyImageCache.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.example.model

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/model](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/model); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example`; example
- Declaration witness: [ISomeTask.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/model/ISomeTask.java); source files: `ISomeTask.java`, `SomeTask.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.example.styledExample

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/styledExample](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/styledExample); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example`; example
- Declaration witness: [XViewerStyledStringLableProviderTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/styledExample/XViewerStyledStringLableProviderTest.java); source files: `XViewerStyledStringLableProviderTest.java`, `XViewerTestStyledStringLabelProvider.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.example`](xviewer.md#orgeclipsenebulawidgetsxviewerexample), [`org.eclipse.nebula.widgets.xviewer.example.images`](xviewer.md#orgeclipsenebulawidgetsxviewerexampleimages), [`org.eclipse.nebula.widgets.xviewer.example.model`](xviewer.md#orgeclipsenebulawidgetsxviewerexamplemodel), [`org.eclipse.nebula.widgets.xviewer.util`](xviewer.md#orgeclipsenebulawidgetsxviewerutil)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.example.util

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/util](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/util); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example`; example
- Declaration witness: [FileUtil.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/src/org/eclipse/nebula/widgets/xviewer/example/util/FileUtil.java); source files: `FileUtil.java`, `MatchFilter.java`, `MyLib.java`, `MyLog.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.util`](xviewer.md#orgeclipsenebulawidgetsxviewerutil)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.integration

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer.integration/src/org/eclipse/nebula/widgets/xviewer/integration](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.integration/src/org/eclipse/nebula/widgets/xviewer/integration); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.integration`; production/build
- Declaration witness: [Activator.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.integration/src/org/eclipse/nebula/widgets/xviewer/integration/Activator.java); source files: `Activator.java`, `ExampleTab.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.examples`](examples.md#orgeclipsenebulaexamples), [`org.eclipse.nebula.widgets.xviewer.example`](xviewer.md#orgeclipsenebulawidgetsxviewerexample)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.tests

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests/src/org/eclipse/nebula/widgets/xviewer/tests](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests/src/org/eclipse/nebula/widgets/xviewer/tests); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests`; test
- Declaration witness: [AllXViewerTestSuite.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests/src/org/eclipse/nebula/widgets/xviewer/tests/AllXViewerTestSuite.java); source files: `AllXViewerTestSuite.java`, `DummyTest.java`, `XViewerViewport_Test.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests`: `org.eclipse.nebula.widgets.xviewer.tests`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.util

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; production/build
- Declaration witness: [EnumStringMultiSelectionDialog.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/EnumStringMultiSelectionDialog.java); source files: `EnumStringMultiSelectionDialog.java`, `EnumStringSingleSelectionDialog.java`, `Pair.java`, `XViewerDisplay.java`, `XViewerException.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.util.internal`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternal)
- Export accounting: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`: `org.eclipse.nebula.widgets.xviewer.util`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.util.internal

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/internal](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/internal); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; internal production
- Declaration witness: [ArrayTreeContentProvider.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/internal/ArrayTreeContentProvider.java); source files: `ArrayTreeContentProvider.java`, `CustomizeDataImages.java`, `DateUtil.java`, `ElapsedTime.java`, `FileUtil.java`, `FilteredTreeComposite.java`, `HtmlUtil.java`, `MatchFilter.java`, `OverlayUtil.java`, `PatternFilter.java`, `Result.java`, `StringLabelProvider.java`, `StringMatcher.java`, `StringNameComparator.java`, `XViewerFilteredTree.java`, `XViewerLib.java`, `XViewerLog.java`, `XViewerMenuDetectListener.java`, `XViewerMouseListener.java`, `XViewerPatternFilter.java`, `XViewerTextWidget.java`, `XViewerUIJob.java`, `XViewerWidget.java`, `XViewerWidgetModifiedListener.java`, `XViewerWorkbenchJob.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.core.util`](xviewer.md#orgeclipsenebulawidgetsxviewercoreutil), [`org.eclipse.nebula.widgets.xviewer.util`](xviewer.md#orgeclipsenebulawidgetsxviewerutil), [`org.eclipse.nebula.widgets.xviewer.util.internal.images`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternalimages)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.util.internal.dialog

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/internal/dialog](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/internal/dialog); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; internal production
- Declaration witness: [ColumnFilterDialog.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/internal/dialog/ColumnFilterDialog.java); source files: `ColumnFilterDialog.java`, `DateSelectionDialog.java`, `DialogWithEntry.java`, `HtmlDialog.java`, `ListDialogSortable.java`, `ListDialogSortableFiltered.java`, `XCheckFilteredTreeDialog.java`, `XCheckedFilteredTree.java`, `XFilteredTree.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.xviewer`](xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.core.model`](xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.util.internal`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternal), [`org.eclipse.nebula.widgets.xviewer.util.internal.images`](xviewer.md#orgeclipsenebulawidgetsxviewerutilinternalimages)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.xviewer.util.internal.images

- Origin: [widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/internal/images](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/internal/images); owner `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`; internal production
- Declaration witness: [XViewerImageCache.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/util/internal/images/XViewerImageCache.java); source files: `XViewerImageCache.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


