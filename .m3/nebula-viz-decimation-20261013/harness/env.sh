# Shared environment for the nebula-viz decimation leaf (sourced).
J="/c/Program Files/Java/jdk-21/bin"
B=/c/m3-work-c/viz-build
N=/c/m3-work-c/nebula-viz
R=~/.m2/repository/org/eclipse/platform
M=~/.m2/repository
jar_of(){ ls $R/$1/$2/$1-$2.jar; }
CP_LIST="$(jar_of org.eclipse.swt.win32.win32.x86_64 3.135.0)
/c/Users/hsoliwal/.p2/pool/plugins/org.eclipse.draw2d_3.23.100.202608071137.jar
$(jar_of org.eclipse.jface 3.40.0)
$(jar_of org.eclipse.core.commands 3.13.0)
$(jar_of org.eclipse.equinox.common 3.21.0)
$(jar_of org.eclipse.ui.workbench 3.140.0)
$(jar_of org.eclipse.core.runtime 3.35.0)
$(jar_of org.eclipse.core.jobs 3.15.900)
$(jar_of org.eclipse.osgi 3.24.300)
$(jar_of org.eclipse.equinox.registry 3.12.600)
$(jar_of org.eclipse.equinox.preferences 3.12.100)
$M/org/osgi/org.osgi.service.prefs/1.1.2/org.osgi.service.prefs-1.1.2.jar
$M/junit/junit/4.13.2/junit-4.13.2.jar
$M/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
CP=$(echo "$CP_LIST" | while read f; do cygpath -w "$f"; done | paste -sd';')
