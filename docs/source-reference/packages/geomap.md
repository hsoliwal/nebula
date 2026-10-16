# Package declarations: geomap

[All packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md) · [Coverage](../COVERAGE.md)

This is a navigation group, not an additional build module. Source revision and inventory rules are recorded in the package index. All entries remain included; a declaration witness is not whole-package semantic review.

## org.eclipse.nebula.widgets.geomap

- Origin: [widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap); owner `widgets/geomap/org.eclipse.nebula.widgets.geomap`; production/build
- Declaration witness: [GeoMap.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/GeoMap.java); source files: `GeoMap.java`, `GeoMapListener.java`, `GeoMapUtil.java`, `GoogleTileServer.java`, `OsmTileServer.java`, `PointD.java`, `TileServer.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.geomap.internal`](geomap.md#orgeclipsenebulawidgetsgeomapinternal)
- Export accounting: `widgets/geomap/org.eclipse.nebula.widgets.geomap`: `org.eclipse.nebula.widgets.geomap`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.geomap.draw2d

- Origin: [widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/draw2d](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/draw2d); owner `widgets/geomap/org.eclipse.nebula.widgets.geomap`; production/build
- Declaration witness: [MapFigure.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/draw2d/MapFigure.java); source files: `MapFigure.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.geomap`](geomap.md#orgeclipsenebulawidgetsgeomap), [`org.eclipse.nebula.widgets.geomap.internal`](geomap.md#orgeclipsenebulawidgetsgeomapinternal)
- Export accounting: `widgets/geomap/org.eclipse.nebula.widgets.geomap`: `org.eclipse.nebula.widgets.geomap.draw2d`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.geomap.internal

- Origin: [widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/internal](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/internal); owner `widgets/geomap/org.eclipse.nebula.widgets.geomap`; internal production
- Declaration witness: [AsyncImage.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/internal/AsyncImage.java); source files: `AsyncImage.java`, `DefaultMouseHandler.java`, `GeoMapHelper.java`, `GeoMapHelperListener.java`, `GeoMapPositioned.java`, `InternalGeoMap.java`, `InternalGeoMapListener.java`, `TileRef.java`, `URLService.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.geomap`](geomap.md#orgeclipsenebulawidgetsgeomap)
- Export accounting: `widgets/geomap/org.eclipse.nebula.widgets.geomap`: `org.eclipse.nebula.widgets.geomap.internal;x-internal:=true`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.geomap.internal.geomapbrowser

- Origin: [widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/internal/geomapbrowser](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/internal/geomapbrowser); owner `widgets/geomap/org.eclipse.nebula.widgets.geomap`; internal production
- Declaration witness: [AbstractPage.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/internal/geomapbrowser/AbstractPage.java); source files: `AbstractPage.java`, `GeoMapBrowser.java`, `HeaderControl.java`, `InfoPage.java`, `Page.java`, `PageContainer.java`, `ResultsPage.java`, `SearchPage.java`, `TitleControl.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.geomap`](geomap.md#orgeclipsenebulawidgetsgeomap), [`org.eclipse.nebula.widgets.geomap.internal`](geomap.md#orgeclipsenebulawidgetsgeomapinternal)
- Export accounting: `widgets/geomap/org.eclipse.nebula.widgets.geomap`: `org.eclipse.nebula.widgets.geomap.internal.geomapbrowser;x-internal:=true`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.geomap.jface

- Origin: [widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/jface](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/jface); owner `widgets/geomap/org.eclipse.nebula.widgets.geomap`; production/build
- Declaration witness: [GeoMapViewer.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/src/org/eclipse/nebula/widgets/geomap/jface/GeoMapViewer.java); source files: `GeoMapViewer.java`, `GoogleIconDescriptor.java`, `GoogleMapsSearchServer.java`, `IPinPointProvider.java`, `IToolTipProvider.java`, `LabelImageProvider.java`, `Located.java`, `LocationProvider.java`, `OsmSearchServer.java`, `SearchServer.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.geomap`](geomap.md#orgeclipsenebulawidgetsgeomap), [`org.eclipse.nebula.widgets.geomap.internal`](geomap.md#orgeclipsenebulawidgetsgeomapinternal)
- Export accounting: `widgets/geomap/org.eclipse.nebula.widgets.geomap`: `org.eclipse.nebula.widgets.geomap.jface`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.geomap.jface.example

- Origin: [widgets/geomap/org.eclipse.nebula.widgets.geomap.example/src/org/eclipse/nebula/widgets/geomap/jface/example](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.example/src/org/eclipse/nebula/widgets/geomap/jface/example); owner `widgets/geomap/org.eclipse.nebula.widgets.geomap.example`; example
- Declaration witness: [GeoMapViewerExampleTab.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.example/src/org/eclipse/nebula/widgets/geomap/jface/example/GeoMapViewerExampleTab.java); source files: `GeoMapViewerExampleTab.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.examples`](examples.md#orgeclipsenebulaexamples), [`org.eclipse.nebula.widgets.geomap`](geomap.md#orgeclipsenebulawidgetsgeomap), [`org.eclipse.nebula.widgets.geomap.jface`](geomap.md#orgeclipsenebulawidgetsgeomapjface)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.geomap.snippets

- Origin: [widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets/src/org/eclipse/nebula/widgets/geomap/snippets](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets/src/org/eclipse/nebula/widgets/geomap/snippets); owner `widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets`; example
- Declaration witness: [GeoMapSnippet01.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets/src/org/eclipse/nebula/widgets/geomap/snippets/GeoMapSnippet01.java); source files: `GeoMapSnippet01.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.widgets.geomap`](geomap.md#orgeclipsenebulawidgetsgeomap)
- Export accounting: `widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets`: `org.eclipse.nebula.widgets.geomap.snippets`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.widgets.geomap.tests

- Origin: [widgets/geomap/org.eclipse.nebula.widgets.geomap.tests/src/org/eclipse/nebula/widgets/geomap/tests](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.tests/src/org/eclipse/nebula/widgets/geomap/tests); owner `widgets/geomap/org.eclipse.nebula.widgets.geomap.tests`; test
- Declaration witness: [GeoMapTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.tests/src/org/eclipse/nebula/widgets/geomap/tests/GeoMapTest.java); source files: `GeoMapTest.java`, `GeoMapViewerTest.java`, `SWTBotCanvas.java`, `SWTBotGeoMap.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.widgets.geomap`](geomap.md#orgeclipsenebulawidgetsgeomap), [`org.eclipse.nebula.widgets.geomap.internal`](geomap.md#orgeclipsenebulawidgetsgeomapinternal), [`org.eclipse.nebula.widgets.geomap.jface`](geomap.md#orgeclipsenebulawidgetsgeomapjface)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


