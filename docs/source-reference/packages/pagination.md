# Package declarations: pagination

[All packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md) · [Coverage](../COVERAGE.md)

This is a navigation group, not an additional build module. Source revision and inventory rules are recorded in the package index. All entries remain included; a declaration witness is not whole-package semantic review.

## org.eclipse.nebula.widgets.pagination

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [AbstractPageControllerComposite.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/AbstractPageControllerComposite.java); source files: `AbstractPageControllerComposite.java`, `AbstractPageControllerSelectionListener.java`, `AbstractPaginationWidget.java`, `AbstractSortColumnSelectionListener.java`, `IPageChangedListener.java`, `IPageContentProvider.java`, `IPageLoader.java`, `IPageLoaderHandler.java`, `LazyItemsSelectionListener.java`, `PageChangedAdapter.java`, `PageLoaderStrategyHelper.java`, `PageableController.java`, `PaginationHelper.java`, `Resources.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination.collections`](pagination.md#orgeclipsenebulawidgetspaginationcollections), [`org.eclipse.nebula.widgets.pagination.renderers`](pagination.md#orgeclipsenebulawidgetspaginationrenderers)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.collections

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/collections](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/collections); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [BeanComparator.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/collections/BeanComparator.java); source files: `BeanComparator.java`, `BeanUtils.java`, `DefaultSortProcessor.java`, `PageListHelper.java`, `PageLoaderList.java`, `PageResult.java`, `PageResultContentProvider.java`, `PageResultLoaderList.java`, `SortProcessor.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination.collections`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.example

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.example/src/org/eclipse/nebula/widgets/pagination/example](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.example/src/org/eclipse/nebula/widgets/pagination/example); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.example`; example
- Declaration witness: [PaginationExampleTab.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.example/src/org/eclipse/nebula/widgets/pagination/example/PaginationExampleTab.java); source files: `PaginationExampleTab.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.examples`](examples.md#orgeclipsenebulaexamples), [`org.eclipse.nebula.widgets.pagination.collections`](pagination.md#orgeclipsenebulawidgetspaginationcollections), [`org.eclipse.nebula.widgets.pagination.example.model`](pagination.md#orgeclipsenebulawidgetspaginationexamplemodel), [`org.eclipse.nebula.widgets.pagination.example.services`](pagination.md#orgeclipsenebulawidgetspaginationexampleservices), [`org.eclipse.nebula.widgets.pagination.renderers.navigation`](pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigation), [`org.eclipse.nebula.widgets.pagination.renderers.navigation.graphics`](pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigationgraphics), [`org.eclipse.nebula.widgets.pagination.table`](pagination.md#orgeclipsenebulawidgetspaginationtable)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.example.model

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.example/src/org/eclipse/nebula/widgets/pagination/example/model](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.example/src/org/eclipse/nebula/widgets/pagination/example/model); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.example`; example
- Declaration witness: [NebulaWidget.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.example/src/org/eclipse/nebula/widgets/pagination/example/model/NebulaWidget.java); source files: `NebulaWidget.java`, `Person.java`
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.example.services

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.example/src/org/eclipse/nebula/widgets/pagination/example/services](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.example/src/org/eclipse/nebula/widgets/pagination/example/services); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.example`; example
- Declaration witness: [NebulaWidgetServices.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.example/src/org/eclipse/nebula/widgets/pagination/example/services/NebulaWidgetServices.java); source files: `NebulaWidgetServices.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.collections`](pagination.md#orgeclipsenebulawidgetspaginationcollections), [`org.eclipse.nebula.widgets.pagination.example.model`](pagination.md#orgeclipsenebulawidgetspaginationexamplemodel)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.renderers

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [ICompositeRendererFactory.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/ICompositeRendererFactory.java); source files: `ICompositeRendererFactory.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination.renderers`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.renderers.navigation

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/navigation](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/navigation); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [NavigationPageComboRenderer.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/navigation/NavigationPageComboRenderer.java); source files: `NavigationPageComboRenderer.java`, `NavigationPageComboRendererFactory.java`, `NavigationPageScaleRenderer.java`, `NavigationPageScaleRendererFactory.java`, `ResultAndNavigationPageGraphicsRenderer.java`, `ResultAndNavigationPageGraphicsRendererFactory.java`, `ResultAndNavigationPageLinksRenderer.java`, `ResultAndNavigationPageLinksRendererFactory.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.renderers`](pagination.md#orgeclipsenebulawidgetspaginationrenderers), [`org.eclipse.nebula.widgets.pagination.renderers.navigation.graphics`](pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigationgraphics)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination.renderers.navigation`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.renderers.navigation.graphics

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/navigation/graphics](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/navigation/graphics); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [BlackNavigationPageGraphicsConfigurator.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/navigation/graphics/BlackNavigationPageGraphicsConfigurator.java); source files: `BlackNavigationPageGraphicsConfigurator.java`, `BlueNavigationPageGraphicsConfigurator.java`, `GreenNavigationPageGraphicsConfigurator.java`, `INavigationPageGraphicsConfigurator.java`, `NavigationPageGraphics.java`, `NavigationPageGraphicsItem.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination.renderers.navigation.graphics`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.renderers.pagesize

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/pagesize](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/pagesize); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [PageSizeComboRenderer.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/renderers/pagesize/PageSizeComboRenderer.java); source files: `PageSizeComboRenderer.java`, `PageSizeComboRendererFactory.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.renderers`](pagination.md#orgeclipsenebulawidgetspaginationrenderers)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination.renderers.pagesize`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.snippets

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets`; example
- Declaration witness: [DisplayNavigationPageGraphics.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/DisplayNavigationPageGraphics.java); source files: `DisplayNavigationPageGraphics.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.renderers.navigation.graphics`](pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigationgraphics)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.snippets.model

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/model](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/model); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets`; example
- Declaration witness: [Address.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/model/Address.java); source files: `Address.java`, `Bug469441Person.java`, `Person.java`, `Team.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.snippets.table

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/table](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/table); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets`; example
- Declaration witness: [Bug469441.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/table/Bug469441.java); source files: `Bug469441.java`, `ModelSortPageableTableAndTotalChangedExample.java`, `ModelSortPageableTableExample.java`, `ModelSortPageableTableWorkInProcessExample.java`, `StringPageableTableExample.java`, `StringSortPageableTableExample.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.collections`](pagination.md#orgeclipsenebulawidgetspaginationcollections), [`org.eclipse.nebula.widgets.pagination.snippets.model`](pagination.md#orgeclipsenebulawidgetspaginationsnippetsmodel), [`org.eclipse.nebula.widgets.pagination.table`](pagination.md#orgeclipsenebulawidgetspaginationtable)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.snippets.table.lazy

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/table/lazy](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/table/lazy); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets`; example
- Declaration witness: [LazyPageTableExample.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/table/lazy/LazyPageTableExample.java); source files: `LazyPageTableExample.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.collections`](pagination.md#orgeclipsenebulawidgetspaginationcollections)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.snippets.table.renderers

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/table/renderers](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/table/renderers); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets`; example
- Declaration witness: [AllRenderersPaginationTableAtHandExample.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/table/renderers/AllRenderersPaginationTableAtHandExample.java); source files: `AllRenderersPaginationTableAtHandExample.java`, `ComboPageableTableExample.java`, `GraphicsPageableTableAndTotalChangedExample.java`, `GraphicsPageableTableExample.java`, `OneRendererPaginationTableAtHandExample.java`, `OneRendererPaginationTableWithPageableTableExample.java`, `ScalePageableTableExample.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.collections`](pagination.md#orgeclipsenebulawidgetspaginationcollections), [`org.eclipse.nebula.widgets.pagination.renderers.navigation`](pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigation), [`org.eclipse.nebula.widgets.pagination.renderers.navigation.graphics`](pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigationgraphics), [`org.eclipse.nebula.widgets.pagination.renderers.pagesize`](pagination.md#orgeclipsenebulawidgetspaginationrendererspagesize), [`org.eclipse.nebula.widgets.pagination.snippets.model`](pagination.md#orgeclipsenebulawidgetspaginationsnippetsmodel), [`org.eclipse.nebula.widgets.pagination.table`](pagination.md#orgeclipsenebulawidgetspaginationtable)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.snippets.tree

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/tree](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/tree); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets`; example
- Declaration witness: [ModelSortPageableTreeExample.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/src/org/eclipse/nebula/widgets/pagination/snippets/tree/ModelSortPageableTreeExample.java); source files: `ModelSortPageableTreeExample.java`, `TeamContentProvider.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.pagination.collections`](pagination.md#orgeclipsenebulawidgetspaginationcollections), [`org.eclipse.nebula.widgets.pagination.snippets.model`](pagination.md#orgeclipsenebulawidgetspaginationsnippetsmodel), [`org.eclipse.nebula.widgets.pagination.tree`](pagination.md#orgeclipsenebulawidgetspaginationtree)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.table

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/table](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/table); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [PageableTable.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/table/PageableTable.java); source files: `PageableTable.java`, `SortTableColumnSelectionListener.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.collections`](pagination.md#orgeclipsenebulawidgetspaginationcollections), [`org.eclipse.nebula.widgets.pagination.renderers`](pagination.md#orgeclipsenebulawidgetspaginationrenderers), [`org.eclipse.nebula.widgets.pagination.renderers.navigation`](pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigation)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination.table`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.table.forms

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/table/forms](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/table/forms); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [FormPageableTable.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/table/forms/FormPageableTable.java); source files: `FormPageableTable.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.renderers`](pagination.md#orgeclipsenebulawidgetspaginationrenderers), [`org.eclipse.nebula.widgets.pagination.table`](pagination.md#orgeclipsenebulawidgetspaginationtable)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination.table.forms`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.tests

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination.tests/src/org/eclipse/nebula/widgets/pagination/tests](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.tests/src/org/eclipse/nebula/widgets/pagination/tests); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination.tests`; test
- Declaration witness: [PaginationHelperTestCase.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.tests/src/org/eclipse/nebula/widgets/pagination/tests/PaginationHelperTestCase.java); source files: `PaginationHelperTestCase.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.tree

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/tree](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/tree); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [PageableTree.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/tree/PageableTree.java); source files: `PageableTree.java`, `SortTreeColumnSelectionListener.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.collections`](pagination.md#orgeclipsenebulawidgetspaginationcollections), [`org.eclipse.nebula.widgets.pagination.renderers`](pagination.md#orgeclipsenebulawidgetspaginationrenderers), [`org.eclipse.nebula.widgets.pagination.renderers.navigation`](pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigation)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination.tree`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.pagination.tree.forms

- Origin: [widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/tree/forms](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/tree/forms); owner `widgets/pagination/org.eclipse.nebula.widgets.pagination`; production/build
- Declaration witness: [FormPageableTree.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/tree/forms/FormPageableTree.java); source files: `FormPageableTree.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.pagination`](pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.renderers`](pagination.md#orgeclipsenebulawidgetspaginationrenderers), [`org.eclipse.nebula.widgets.pagination.tree`](pagination.md#orgeclipsenebulawidgetspaginationtree)
- Export accounting: `widgets/pagination/org.eclipse.nebula.widgets.pagination`: `org.eclipse.nebula.widgets.pagination.tree.forms`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


