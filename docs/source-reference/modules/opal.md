# Project and module records: opal

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U129: `widgets/opal`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/pom.xml)
- Maven artifact: `org.eclipse.nebula:opal:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/breadcrumb` (default), `widgets/opal/commons` (default), `widgets/opal/checkboxgroup` (default), `widgets/opal/columnbrowser` (default), `widgets/opal/dialog` (default), `widgets/opal/duallist` (default), `widgets/opal/header` (default), `widgets/opal/heapmanager` (default), `widgets/opal/logindialog` (default), `widgets/opal/multichoice` (default), `widgets/opal/notifier` (default), `widgets/opal/preferencewindow` (default), `widgets/opal/promptsupport` (default), `widgets/opal/propertytable` (default), `widgets/opal/rangeslider` (default), `widgets/opal/starrating` (default), `widgets/opal/textassist` (default), `widgets/opal/tipoftheday` (default), `widgets/opal/titledseparator` (default), `widgets/opal/roundedtoolbar` (default), `widgets/opal/calculator` (default), `widgets/opal/horizontalspinner` (default), `widgets/opal/launcher` (default), `widgets/opal/panels` (default), `widgets/opal/switchbutton` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

