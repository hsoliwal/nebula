import sys

# (file, accessor-call line, field-name of the drop-down control, createPopup anchor)
JOBS = [
    ("tablecombo/org.eclipse.nebula.widgets.tablecombo/src/org/eclipse/nebula/widgets/tablecombo/TableCombo.java",
     "\t\ttable().getAccessible().addAccessibleListener(accessibleAdapter);\n", "table",
     "\t\ttable = new Table(popup, SWT.SINGLE | SWT.FULL_SELECTION | (virtualTable ? SWT.VIRTUAL : SWT.NONE));\n"),
    ("tablecombo/org.eclipse.nebula.widgets.tablecombo/src/org/eclipse/nebula/widgets/checktablecombo/CheckTableCombo.java",
     "\t\ttable().getAccessible().addAccessibleListener(accessibleAdapter);\n", "table", None),
    ("ctreecombo/org.eclipse.nebula.widgets.ctreecombo/src/org/eclipse/nebula/widgets/ctreecombo/CTreeCombo.java",
     "\t\ttree().getAccessible().addAccessibleListener(accessibleAdapter);\n", "tree",
     "\t\ttree.addTreeListener(hookListener);\n"),
    ("calendarcombo/org.eclipse.nebula.widgets.calendarcombo/src/org/eclipse/nebula/widgets/calendarcombo/CustomCombo.java",
     "\t\tlist().getAccessible().addAccessibleListener(accessibleAdapter);\n", "list", None),
]

root = sys.argv[1]
for rel, call, field, anchor in JOBS:
    p = root + "/" + rel
    s = open(p, encoding="utf-8").read()
    assert s.count(call) == 1, (rel, s.count(call))
    s = s.replace(call, "\t\t//FLLDCIM: lazy — the drop-down's accessible listener is attached when the drop-down is first built\n"
                  "\t\tpopupAccessible = accessibleAdapter;\n"
                  "\t\tif (" + field + " != null) {\n"
                  "\t\t\tattachPopupAccessible();\n"
                  "\t\t}\n")
    if anchor is None:
        # the drop-down control is assigned by "<field> = new <Type>(popup, ...);" inside createPopup
        import re
        m = re.search(r"\n(\t\t" + field + r" = new [A-Za-z]+\(popup, [^;]*\);\n)", s)
        assert m, rel
        anchor = m.group(1)
    assert s.count(anchor) == 1, (rel, anchor, s.count(anchor))
    s = s.replace(anchor, anchor + "\t\tattachPopupAccessible();\n")
    # helper + field, placed before createPopup's javadoc-less declaration of the lazy accessor
    helper = ("\t/** accessible listener for the first drop-down control only, exactly as the eager widget attached it */\n"
              "\tprivate org.eclipse.swt.accessibility.AccessibleAdapter popupAccessible;\n\n"
              "\tprivate void attachPopupAccessible() {\n"
              "\t\tif (popupAccessible != null && " + field + " != null) {\n"
              "\t\t\t" + field + ".getAccessible().addAccessibleListener(popupAccessible);\n"
              "\t\t\tpopupAccessible = null;\n"
              "\t\t}\n"
              "\t}\n\n")
    marker = "\t/**\n\t * @return the drop-down "
    assert s.count(marker) == 1, (rel, s.count(marker))
    s = s.replace(marker, helper + marker)
    open(p, "w", encoding="utf-8", newline="\n").write(s)
    print("patched", rel)
