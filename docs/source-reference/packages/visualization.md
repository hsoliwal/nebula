# Package declarations: visualization

[All packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md) · [Coverage](../COVERAGE.md)

This is a navigation group, not an additional build module. Source revision and inventory rules are recorded in the package index. All entries remain included; a declaration witness is not whole-package semantic review.

## org.eclipse.nebula.visualization.example

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.example/src/org/eclipse/nebula/visualization/example](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.example/src/org/eclipse/nebula/visualization/example); owner `widgets/visualization/org.eclipse.nebula.visualization.example`; example
- Declaration witness: [ExampleTab.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.example/src/org/eclipse/nebula/visualization/example/ExampleTab.java); source files: `ExampleTab.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.examples`](examples.md#orgeclipsenebulaexamples)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.internal.widgets.introspection

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/internal/widgets/introspection](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/internal/widgets/introspection); owner `widgets/visualization/org.eclipse.nebula.visualization.widgets`; internal production
- Declaration witness: [DefaultWidgetIntrospector.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/internal/widgets/introspection/DefaultWidgetIntrospector.java); source files: `DefaultWidgetIntrospector.java`, `GenericBeanInfo.java`, `Introspectable.java`, `MeterIntrospector.java`, `ScaleWidgetIntrospector.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.widgets.figures`](visualization.md#orgeclipsenebulavisualizationwidgetsfigures)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.widgets`: `org.eclipse.nebula.visualization.internal.widgets.introspection`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.internal.widgets.raster

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/internal/widgets/raster](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/internal/widgets/raster); owner `widgets/visualization/org.eclipse.nebula.visualization.widgets`; internal production
- Declaration witness: [ColorMapRaster.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/internal/widgets/raster/ColorMapRaster.java); source files: `ColorMapRaster.java`, `WindowArrayWrapper.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.widgets.datadefinition`](visualization.md#orgeclipsenebulavisualizationwidgetsdatadefinition)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.widgets`: `org.eclipse.nebula.visualization.internal.widgets.raster;x-internal:=true`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.internal.xygraph.decimation

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/internal/xygraph/decimation](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/internal/xygraph/decimation); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph.test`; test
- Declaration witness: [DecimationCoreTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/internal/xygraph/decimation/DecimationCoreTest.java); source files: `DecimationCoreTest.java`
- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/decimation](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/decimation); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; internal production
- Declaration witness: [AxisMapping.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/decimation/AxisMapping.java); source files: `AxisMapping.java`, `DecimatedTrace.java`, `PackedColumns.java`, `PackedMinMaxPyramid.java`, `PolylineBuilder.java`, `TraceDecimation.java`, `TraceDecimator.java`, `TraceView.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.xygraph.dataprovider`](visualization.md#orgeclipsenebulavisualizationxygraphdataprovider), [`org.eclipse.nebula.visualization.xygraph.figures`](visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.linearscale`](visualization.md#orgeclipsenebulavisualizationxygraphlinearscale)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.xygraph`: `org.eclipse.nebula.visualization.internal.xygraph.decimation;x-internal:=true`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.internal.xygraph.toolbar

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/toolbar](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/toolbar); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; internal production
- Declaration witness: [AddAnnotationDialog.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/toolbar/AddAnnotationDialog.java); source files: `AddAnnotationDialog.java`, `AnnotationConfigPage.java`, `AxisConfigPage.java`, `DoubleInputText.java`, `GraphConfigPage.java`, `GrayableButton.java`, `ITraceConfigPage.java`, `RemoveAnnotationDialog.java`, `TraceConfigPage.java`, `WrappableToolbarLayout.java`, `XYGraphConfigDialog.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.internal.xygraph.undo`](visualization.md#orgeclipsenebulavisualizationinternalxygraphundo), [`org.eclipse.nebula.visualization.xygraph`](visualization.md#orgeclipsenebulavisualizationxygraph), [`org.eclipse.nebula.visualization.xygraph.figures`](visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.linearscale`](visualization.md#orgeclipsenebulavisualizationxygraphlinearscale), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.xygraph`: `org.eclipse.nebula.visualization.internal.xygraph.toolbar`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.internal.xygraph.undo

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/undo](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/undo); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; internal production
- Declaration witness: [AddAnnotationCommand.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/undo/AddAnnotationCommand.java); source files: `AddAnnotationCommand.java`, `AnnotationMemento.java`, `AxisMemento.java`, `AxisPanOrZoomCommand.java`, `IOperationsManagerListener.java`, `IUndoableCommand.java`, `MovingAnnotationCommand.java`, `MovingAnnotationLabelCommand.java`, `OperationsManager.java`, `RemoveAnnotationCommand.java`, `SaveStateCommand.java`, `SizeLimitedStack.java`, `TraceMemento.java`, `XYGraphConfigCommand.java`, `XYGraphMemento.java`, `XYGraphMementoUtil.java`, `ZoomCommand.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.xygraph`](visualization.md#orgeclipsenebulavisualizationxygraph), [`org.eclipse.nebula.visualization.xygraph.dataprovider`](visualization.md#orgeclipsenebulavisualizationxygraphdataprovider), [`org.eclipse.nebula.visualization.xygraph.figures`](visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.linearscale`](visualization.md#orgeclipsenebulavisualizationxygraphlinearscale)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.xygraph`: `org.eclipse.nebula.visualization.internal.xygraph.undo`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.internal.xygraph.utils

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/utils](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/utils); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; internal production
- Declaration witness: [LargeNumberUtils.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/internal/xygraph/utils/LargeNumberUtils.java); source files: `LargeNumberUtils.java`
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.widgets.datadefinition

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/datadefinition](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/datadefinition); owner `widgets/visualization/org.eclipse.nebula.visualization.widgets`; production/build
- Declaration witness: [ByteArrayWrapper.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/datadefinition/ByteArrayWrapper.java); source files: `ByteArrayWrapper.java`, `ColorMap.java`, `ColorTuple.java`, `DoubleArrayWrapper.java`, `FloatArrayWrapper.java`, `IManualStringValueChangeListener.java`, `IManualValueChangeListener.java`, `IPrimaryArrayWrapper.java`, `IntArrayWrapper.java`, `LongArrayWrapper.java`, `ShortArrayWrapper.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.internal.widgets.raster`](visualization.md#orgeclipsenebulavisualizationinternalwidgetsraster)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.widgets`: `org.eclipse.nebula.visualization.widgets.datadefinition`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.widgets.examples

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.widgets/examples/org/eclipse/nebula/visualization/widgets/examples](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/examples/org/eclipse/nebula/visualization/widgets/examples); owner `widgets/visualization/org.eclipse.nebula.visualization.widgets`; example
- Declaration witness: [ColorMapRampExample.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/examples/org/eclipse/nebula/visualization/widgets/examples/ColorMapRampExample.java); source files: `ColorMapRampExample.java`, `GaugeExample.java`, `IntensityGraphExample.java`, `KnobExample.java`, `MeterExample.java`, `MultipleWidgetsExample.java`, `ProgressBarExample.java`, `ScaledSliderExample.java`, `TankExample.java`, `ThermometerExample.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.widgets.datadefinition`](visualization.md#orgeclipsenebulavisualizationwidgetsdatadefinition), [`org.eclipse.nebula.visualization.widgets.figureparts`](visualization.md#orgeclipsenebulavisualizationwidgetsfigureparts), [`org.eclipse.nebula.visualization.widgets.figures`](visualization.md#orgeclipsenebulavisualizationwidgetsfigures), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.widgets.figureparts

- Origin: [m3/openrewrite-line-terminators/src/test/resources/original-nebula/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figureparts](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/m3/openrewrite-line-terminators/src/test/resources/original-nebula/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figureparts); owner `m3/openrewrite-line-terminators`; fork tooling/proof
- Declaration witness: [RoundScale.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/m3/openrewrite-line-terminators/src/test/resources/original-nebula/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figureparts/RoundScale.java); source files: `RoundScale.java`
- Origin: [widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figureparts](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figureparts); owner `widgets/visualization/org.eclipse.nebula.visualization.widgets`; production/build
- Declaration witness: [AlphaLabel.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figureparts/AlphaLabel.java); source files: `AlphaLabel.java`, `Bulb.java`, `ColorMapRamp.java`, `PolarPoint.java`, `ROIFigure.java`, `RoundScale.java`, `RoundScaleTickLabels.java`, `RoundScaleTickMarks.java`, `RoundScaledRamp.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.widgets.datadefinition`](visualization.md#orgeclipsenebulavisualizationwidgetsdatadefinition), [`org.eclipse.nebula.visualization.widgets.figures`](visualization.md#orgeclipsenebulavisualizationwidgetsfigures), [`org.eclipse.nebula.visualization.widgets.util`](visualization.md#orgeclipsenebulavisualizationwidgetsutil), [`org.eclipse.nebula.visualization.xygraph.linearscale`](visualization.md#orgeclipsenebulavisualizationxygraphlinearscale), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.widgets`: `org.eclipse.nebula.visualization.widgets.figureparts`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.widgets.figures

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.widgets.tests/src/org/eclipse/nebula/visualization/widgets/figures](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets.tests/src/org/eclipse/nebula/visualization/widgets/figures); owner `widgets/visualization/org.eclipse.nebula.visualization.widgets.tests`; test
- Declaration witness: [AbstractMarkedWidgetTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets.tests/src/org/eclipse/nebula/visualization/widgets/figures/AbstractMarkedWidgetTest.java); source files: `AbstractMarkedWidgetTest.java`, `AbstractRoundRampedWidgetTest.java`, `AbstractScaledWidgetTest.java`, `AbstractWidgetTest.java`, `CircularBufferTest.java`, `GaugeTest.java`, `IntensityGraphManualTest.java`, `IntensityGraphTest.java`, `KnobTest.java`, `MeterTest.java`, `ProgressBarTest.java`, `ScaledSliderTest.java`, `TankTest.java`, `ThermometerTest.java`
- Origin: [widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figures](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figures); owner `widgets/visualization/org.eclipse.nebula.visualization.widgets`; production/build
- Declaration witness: [AbstractLinearMarkedFigure.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figures/AbstractLinearMarkedFigure.java); source files: `AbstractLinearMarkedFigure.java`, `AbstractMarkedWidgetFigure.java`, `AbstractRoundRampedFigure.java`, `AbstractScaledWidgetFigure.java`, `GaugeFigure.java`, `IntensityGraphFigure.java`, `KnobFigure.java`, `MeterFigure.java`, `ProgressBarFigure.java`, `ScaledSliderFigure.java`, `TankFigure.java`, `ThermometerFigure.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.internal.widgets.introspection`](visualization.md#orgeclipsenebulavisualizationinternalwidgetsintrospection), [`org.eclipse.nebula.visualization.internal.widgets.raster`](visualization.md#orgeclipsenebulavisualizationinternalwidgetsraster), [`org.eclipse.nebula.visualization.widgets.datadefinition`](visualization.md#orgeclipsenebulavisualizationwidgetsdatadefinition), [`org.eclipse.nebula.visualization.widgets.figureparts`](visualization.md#orgeclipsenebulavisualizationwidgetsfigureparts), [`org.eclipse.nebula.visualization.widgets.util`](visualization.md#orgeclipsenebulavisualizationwidgetsutil), [`org.eclipse.nebula.visualization.xygraph.dataprovider`](visualization.md#orgeclipsenebulavisualizationxygraphdataprovider), [`org.eclipse.nebula.visualization.xygraph.figures`](visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.linearscale`](visualization.md#orgeclipsenebulavisualizationxygraphlinearscale), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.widgets`: `org.eclipse.nebula.visualization.widgets.figures`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.widgets.util

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/util](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/util); owner `widgets/visualization/org.eclipse.nebula.visualization.widgets`; production/build
- Declaration witness: [GraphicsUtil.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/util/GraphicsUtil.java); source files: `GraphicsUtil.java`, `PointsUtil.java`, `RepeatFiringBehavior.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.widgets`: `org.eclipse.nebula.visualization.widgets.util`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.xygraph

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; production/build
- Declaration witness: [Messages.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/Messages.java); source files: `Messages.java`
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.xygraph.dataprovider

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/xygraph/dataprovider](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/xygraph/dataprovider); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph.test`; test
- Declaration witness: [CircularBufferTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/xygraph/dataprovider/CircularBufferTest.java); source files: `CircularBufferTest.java`
- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/dataprovider](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/dataprovider); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; production/build
- Declaration witness: [AbstractDataProvider.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/dataprovider/AbstractDataProvider.java); source files: `AbstractDataProvider.java`, `CircularBuffer.java`, `CircularBufferDataProvider.java`, `ClippedCircularBufferDataProvider.java`, `IDataProvider.java`, `IDataProviderListener.java`, `IMetaData.java`, `ISample.java`, `PackedXYDataProvider.java`, `Sample.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.internal.xygraph.decimation`](visualization.md#orgeclipsenebulavisualizationinternalxygraphdecimation), [`org.eclipse.nebula.visualization.xygraph.linearscale`](visualization.md#orgeclipsenebulavisualizationxygraphlinearscale)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.xygraph`: `org.eclipse.nebula.visualization.xygraph.dataprovider`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.xygraph.examples

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/examples/org/eclipse/nebula/visualization/xygraph/examples](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/examples/org/eclipse/nebula/visualization/xygraph/examples); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; example
- Declaration witness: [BarChartExample.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/examples/org/eclipse/nebula/visualization/xygraph/examples/BarChartExample.java); source files: `BarChartExample.java`, `ComprehensiveExample.java`, `GraphDemo.java`, `LargeNumberOfTracesAnnotationsAxes.java`, `SimpleExample.java`, `SimpleToolbarArmedXYGraphExample.java`, `StaircaseExample.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.xygraph.dataprovider`](visualization.md#orgeclipsenebulavisualizationxygraphdataprovider), [`org.eclipse.nebula.visualization.xygraph.figures`](visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.linearscale`](visualization.md#orgeclipsenebulavisualizationxygraphlinearscale), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.xygraph.exampleview

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph.exampleview/src/org/eclipse/nebula/visualization/xygraph/exampleview](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.exampleview/src/org/eclipse/nebula/visualization/xygraph/exampleview); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph.exampleview`; example
- Declaration witness: [PureJavaExample.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.exampleview/src/org/eclipse/nebula/visualization/xygraph/exampleview/PureJavaExample.java); source files: `PureJavaExample.java`, `WidgetsExampleView.java`, `XYGraphExampleView.java`, `XYGraphStyledBoolExampleView.java`, `XYGraphStyledDoubleExampleView.java`, `XYGraphStyledStringExampleView.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.visualization.widgets.datadefinition`](visualization.md#orgeclipsenebulavisualizationwidgetsdatadefinition), [`org.eclipse.nebula.visualization.widgets.figures`](visualization.md#orgeclipsenebulavisualizationwidgetsfigures), [`org.eclipse.nebula.visualization.xygraph.dataprovider`](visualization.md#orgeclipsenebulavisualizationxygraphdataprovider), [`org.eclipse.nebula.visualization.xygraph.figures`](visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.styleprovider`](visualization.md#orgeclipsenebulavisualizationxygraphstyleprovider), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.xygraph.figures

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/xygraph/figures](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/xygraph/figures); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph.test`; test
- Declaration witness: [TraceDecimationDifferentialTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/xygraph/figures/TraceDecimationDifferentialTest.java); source files: `TraceDecimationDifferentialTest.java`
- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/figures](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/figures); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; production/build
- Declaration witness: [Annotation.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/figures/Annotation.java); source files: `Annotation.java`, `Axis.java`, `CircleAnnotation.java`, `CrossAnnotation.java`, `DAxesFactory.java`, `DAxis.java`, `DefaultAxesFactory.java`, `Grid.java`, `IAnnotationLabelProvider.java`, `IAnnotationListener.java`, `IAxesFactory.java`, `IAxisListener.java`, `ITraceListener.java`, `IXYGraph.java`, `Legend.java`, `NoRepeatsPointsList.java`, `PlotArea.java`, `ToolbarArmedXYGraph.java`, `Trace.java`, `TraceDecimationPainter.java`, `XYGraph.java`, `XYGraphFlags.java`, `XYGraphToolbar.java`, `ZoomType.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.internal.xygraph.decimation`](visualization.md#orgeclipsenebulavisualizationinternalxygraphdecimation), [`org.eclipse.nebula.visualization.internal.xygraph.toolbar`](visualization.md#orgeclipsenebulavisualizationinternalxygraphtoolbar), [`org.eclipse.nebula.visualization.internal.xygraph.undo`](visualization.md#orgeclipsenebulavisualizationinternalxygraphundo), [`org.eclipse.nebula.visualization.internal.xygraph.utils`](visualization.md#orgeclipsenebulavisualizationinternalxygraphutils), [`org.eclipse.nebula.visualization.xygraph`](visualization.md#orgeclipsenebulavisualizationxygraph), [`org.eclipse.nebula.visualization.xygraph.dataprovider`](visualization.md#orgeclipsenebulavisualizationxygraphdataprovider), [`org.eclipse.nebula.visualization.xygraph.linearscale`](visualization.md#orgeclipsenebulavisualizationxygraphlinearscale), [`org.eclipse.nebula.visualization.xygraph.styleprovider`](visualization.md#orgeclipsenebulavisualizationxygraphstyleprovider), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.xygraph`: `org.eclipse.nebula.visualization.xygraph.figures`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.xygraph.linearscale

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/xygraph/linearscale](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/xygraph/linearscale); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph.test`; test
- Declaration witness: [AxisTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/src/org/eclipse/nebula/visualization/xygraph/linearscale/AxisTest.java); source files: `AxisTest.java`, `TickFactoryTest.java`
- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/linearscale](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/linearscale); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; production/build
- Declaration witness: [AbstractScale.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/linearscale/AbstractScale.java); source files: `AbstractScale.java`, `IScaleProvider.java`, `ITicksProvider.java`, `LinearScale.java`, `LinearScaleTickLabels.java`, `LinearScaleTickLabels2.java`, `LinearScaleTickMarks.java`, `LinearScaleTickMarks2.java`, `LinearScaleTicks.java`, `LinearScaleTicks2.java`, `LinearScaledMarker.java`, `Range.java`, `Tick.java`, `TickFactory.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.internal.xygraph.utils`](visualization.md#orgeclipsenebulavisualizationinternalxygraphutils), [`org.eclipse.nebula.visualization.xygraph.figures`](visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.xygraph`: `org.eclipse.nebula.visualization.xygraph.linearscale`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.xygraph.styleprovider

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/styleprovider](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/styleprovider); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; production/build
- Declaration witness: [BasePointStyleProvider.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/styleprovider/BasePointStyleProvider.java); source files: `BasePointStyleProvider.java`, `BooleanStyleProvider.java`, `DoubleStyleProvider.java`, `IPointStyleProvider.java`, `StringStyleProvider.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.xygraph.dataprovider`](visualization.md#orgeclipsenebulavisualizationxygraphdataprovider), [`org.eclipse.nebula.visualization.xygraph.figures`](visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.util`](visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.xygraph`: `org.eclipse.nebula.visualization.xygraph.styleprovider`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.visualization.xygraph.util

- Origin: [widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/util](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/util); owner `widgets/visualization/org.eclipse.nebula.visualization.xygraph`; production/build
- Declaration witness: [GraphicsUtil.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/org/eclipse/nebula/visualization/xygraph/util/GraphicsUtil.java); source files: `GraphicsUtil.java`, `ImplementationLoader.java`, `Log10.java`, `Preferences.java`, `SWTConstants.java`, `SingleSourceHelper.java`, `SingleSourceHelper2.java`, `SingleSourceHelperImpl.java`, `XYGraphMediaFactory.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.visualization.xygraph.figures`](visualization.md#orgeclipsenebulavisualizationxygraphfigures)
- Export accounting: `widgets/visualization/org.eclipse.nebula.visualization.xygraph`: `org.eclipse.nebula.visualization.xygraph.util`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


