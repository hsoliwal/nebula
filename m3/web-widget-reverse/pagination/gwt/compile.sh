#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
: "${GWT_CP:?Set GWT_CP to the approved offline gwt-dev/gwt-user dependency closure}"
here=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
source_root="$here/../../../../widgets/pagination/org.eclipse.nebula.widgets.pagination/src"
# Exact GWT module source include excludes SWT, RWT, JNI and the rest of the bundle.
exec java -Xmx512m -cp "$GWT_CP:$here:$source_root" com.google.gwt.dev.Compiler   -strict -localWorkers 1 -war "$here/target/war" org.eclipse.nebula.widgets.pagination.PageWindow
