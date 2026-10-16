# Project and module ownership
> Source snapshot: `3cf2169433572c96d4b65c83a16c2f907eb37c67` in `hsoliwal/nebula`. Status: source/manifest observation, not a build or runtime qualification.

[Reference home](README.md) · [Declared packages](PACKAGES.md) · [Lifecycle contracts](LIFECYCLE.md) · [Coverage ledger](COVERAGE.md)

The records below account for 346 distinct metadata-backed paths, including 345 build/bundle/feature units and 262 Eclipse project paths. Those sets overlap. A Maven aggregator, Eclipse workspace project, OSGi bundle, and feature are different identities even when they share a path. `default-reactor` means reachable through parsed, non-profile `<modules>` edges from the root; it does not mean this build has resolved or passed. Profile-only and unreferenced paths remain in scope.

The Tycho build extension in [.mvn/extensions.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/.mvn/extensions.xml) explains why reactor entries need not each contain a checked-in POM. Imports and exports below are manifest declarations, not proof that every exported type is public API: retain `x-internal`, `x-friends`, version and fragment qualifiers. Feature inclusion controls packaging; it is not a Java call dependency.

## Project navigation

| Navigation group | Metadata-backed paths |
| --- | ---: |
| [badgedlabel](modules/badgedlabel.md) | 4 |
| [bidilayout](modules/bidilayout.md) | 2 |
| [calendarcombo](modules/calendarcombo.md) | 5 |
| [carousel](modules/carousel.md) | 4 |
| [cdatetime](modules/cdatetime.md) | 10 |
| [chips](modules/chips.md) | 4 |
| [collapsiblebuttons](modules/collapsiblebuttons.md) | 4 |
| [compositetable](modules/compositetable.md) | 4 |
| [ctree](modules/ctree.md) | 3 |
| [ctreecombo](modules/ctreecombo.md) | 5 |
| [cwt](modules/cwt.md) | 5 |
| [datechooser](modules/datechooser.md) | 4 |
| [effects](modules/effects.md) | 4 |
| [examples](modules/examples.md) | 3 |
| [floatingtext](modules/floatingtext.md) | 4 |
| [fontawesome](modules/fontawesome.md) | 4 |
| [fork-tooling-and-provenance](modules/fork-tooling-and-provenance.md) | 10 |
| [formattedtext](modules/formattedtext.md) | 4 |
| [gallery](modules/gallery.md) | 6 |
| [ganttchart](modules/ganttchart.md) | 4 |
| [geomap](modules/geomap.md) | 6 |
| [grid](modules/grid.md) | 8 |
| [led](modules/led.md) | 4 |
| [nebulaslider](modules/nebulaslider.md) | 4 |
| [nebulatoolbar](modules/nebulatoolbar.md) | 2 |
| [opal](modules/opal.md) | 1 |
| [opal-breadcrumb](modules/opal-breadcrumb.md) | 4 |
| [opal-calculator](modules/opal-calculator.md) | 4 |
| [opal-checkboxgroup](modules/opal-checkboxgroup.md) | 4 |
| [opal-columnbrowser](modules/opal-columnbrowser.md) | 4 |
| [opal-commons](modules/opal-commons.md) | 3 |
| [opal-dialog](modules/opal-dialog.md) | 4 |
| [opal-duallist](modules/opal-duallist.md) | 4 |
| [opal-header](modules/opal-header.md) | 4 |
| [opal-heapmanager](modules/opal-heapmanager.md) | 4 |
| [opal-horizontalspinner](modules/opal-horizontalspinner.md) | 4 |
| [opal-launcher](modules/opal-launcher.md) | 4 |
| [opal-logindialog](modules/opal-logindialog.md) | 4 |
| [opal-multichoice](modules/opal-multichoice.md) | 4 |
| [opal-notifier](modules/opal-notifier.md) | 4 |
| [opal-panels](modules/opal-panels.md) | 4 |
| [opal-preferencewindow](modules/opal-preferencewindow.md) | 4 |
| [opal-promptsupport](modules/opal-promptsupport.md) | 4 |
| [opal-propertytable](modules/opal-propertytable.md) | 4 |
| [opal-rangeslider](modules/opal-rangeslider.md) | 4 |
| [opal-roundedtoolbar](modules/opal-roundedtoolbar.md) | 4 |
| [opal-starrating](modules/opal-starrating.md) | 4 |
| [opal-switchbutton](modules/opal-switchbutton.md) | 4 |
| [opal-textassist](modules/opal-textassist.md) | 4 |
| [opal-tipoftheday](modules/opal-tipoftheday.md) | 4 |
| [opal-titledseparator](modules/opal-titledseparator.md) | 4 |
| [oscilloscope](modules/oscilloscope.md) | 9 |
| [other-components](modules/other-components.md) | 3 |
| [pagination](modules/pagination.md) | 6 |
| [paperclips](modules/paperclips.md) | 7 |
| [passwordrevealer](modules/passwordrevealer.md) | 4 |
| [pgroup](modules/pgroup.md) | 4 |
| [picture](modules/picture.md) | 5 |
| [progresscircle](modules/progresscircle.md) | 4 |
| [pshelf](modules/pshelf.md) | 6 |
| [radiogroup](modules/radiogroup.md) | 5 |
| [releng](modules/releng.md) | 8 |
| [richtext](modules/richtext.md) | 5 |
| [roundedcheckbox](modules/roundedcheckbox.md) | 4 |
| [roundedswitch](modules/roundedswitch.md) | 7 |
| [segmentedbar](modules/segmentedbar.md) | 4 |
| [splitbutton](modules/splitbutton.md) | 4 |
| [stepbar](modules/stepbar.md) | 4 |
| [tablecombo](modules/tablecombo.md) | 9 |
| [tiles](modules/tiles.md) | 4 |
| [timeline](modules/timeline.md) | 6 |
| [treemapper](modules/treemapper.md) | 5 |
| [visualization](modules/visualization.md) | 8 |
| [xviewer](modules/xviewer.md) | 8 |

[All default/profile reactor edges](REACTOR.md)
