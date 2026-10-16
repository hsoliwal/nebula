# Package declarations: effects

[All packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md) · [Coverage](../COVERAGE.md)

This is a navigation group, not an additional build module. Source revision and inventory rules are recorded in the package index. All entries remain included; a declaration witness is not whole-package semantic review.

## org.eclipse.nebula.effects.stw

- Origin: [widgets/effects/org.eclipse.nebula.effects.stw/src/org/eclipse/nebula/effects/stw](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw/src/org/eclipse/nebula/effects/stw); owner `widgets/effects/org.eclipse.nebula.effects.stw`; production/build
- Declaration witness: [ImageCapture.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw/src/org/eclipse/nebula/effects/stw/ImageCapture.java); source files: `ImageCapture.java`, `ImageTransitionable.java`, `Transition.java`, `TransitionListener.java`, `TransitionManager.java`, `Transitionable.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil)
- Export accounting: `widgets/effects/org.eclipse.nebula.effects.stw`: `org.eclipse.nebula.effects.stw; uses:="org.eclipse.swt.events,  org.eclipse.swt.custom,  org.eclipse.swt.graphics,  org.eclipse.swt.widgets"`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.effects.stw.example

- Origin: [widgets/effects/org.eclipse.nebula.effects.stw.example/src/org/eclipse/nebula/effects/stw/example](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw.example/src/org/eclipse/nebula/effects/stw/example); owner `widgets/effects/org.eclipse.nebula.effects.stw.example`; example
- Declaration witness: [AbstractSTWDemoFrame.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw.example/src/org/eclipse/nebula/effects/stw/example/AbstractSTWDemoFrame.java); source files: `AbstractSTWDemoFrame.java`, `STWDemo.java`, `STWExampleTab.java`, `TransitionTest.java`, `TransitionTest1.java`, `TransitionTest2.java`
- Observed imports of other inventoried namespaces: [`java.util`](fork-tooling-and-provenance.md#javautil), [`org.eclipse.nebula.effects.stw`](effects.md#orgeclipsenebulaeffectsstw), [`org.eclipse.nebula.effects.stw.transitions`](effects.md#orgeclipsenebulaeffectsstwtransitions), [`org.eclipse.nebula.examples`](examples.md#orgeclipsenebulaexamples)
- Export accounting: no exact Export-Package clause found in fetched manifests; Java public visibility alone does not make it an exported bundle API
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


## org.eclipse.nebula.effects.stw.transitions

- Origin: [widgets/effects/org.eclipse.nebula.effects.stw/src/org/eclipse/nebula/effects/stw/transitions](https://github.com/hsoliwal/nebula/tree/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw/src/org/eclipse/nebula/effects/stw/transitions); owner `widgets/effects/org.eclipse.nebula.effects.stw`; production/build
- Declaration witness: [CubicRotationTransition.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw/src/org/eclipse/nebula/effects/stw/transitions/CubicRotationTransition.java); source files: `CubicRotationTransition.java`, `FadeTransition.java`, `SlideTransition.java`
- Observed imports of other inventoried namespaces: [`org.eclipse.nebula.effects.stw`](effects.md#orgeclipsenebulaeffectsstw)
- Export accounting: `widgets/effects/org.eclipse.nebula.effects.stw`: `org.eclipse.nebula.effects.stw.transitions;uses:="org.eclipse.swt.graphics,org.eclipse.nebula.effects.stw"`
- Semantic status: inventory only unless explicitly reviewed in the lifecycle guide


