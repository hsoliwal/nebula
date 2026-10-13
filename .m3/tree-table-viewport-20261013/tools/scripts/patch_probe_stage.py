import sys

h = sys.argv[1]


def patch(rel, pairs):
    p = h + "/" + rel
    s = open(p, encoding="utf-8").read()
    for a, b in pairs:
        assert s.count(a) == 1, (rel, a[:80], s.count(a))
        s = s.replace(a, b)
    open(p, "w", encoding="utf-8", newline="\n").write(s)
    print("patched", rel)


patch("probe/org/eclipse/nebula/m3/tt/ApiProbe.java", [
    ("""		Display display = host.getDisplay();
		Runnable closer = new ModalCloser(display, host);""",
     """		Display display = host.getDisplay();
		host.setLayout(new FillLayout());
		host.setBounds(40, 40, 480, 360);
		if (visible) {
			host.open();
		} else {
			host.setVisible(false);
		}
		Runnable closer = new ModalCloser(display, host);"""),
    ("""		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());
		shell.setBounds(40, 40, 480, 360);
		Image image = new Image(display, 8, 8);
		List<GC> gcs = new ArrayList<>();
		TtFixture fx;
		try {
			fx = (TtFixture) Class.forName(fixtureClass, true, ApiProbe.class.getClassLoader())
					.getDeclaredConstructor().newInstance();
			fx.build(shell);
			if (visible) {
				shell.open();
			} else {
				shell.layout(true, true);
			}
			pump(display);
		} catch (Throwable ex) {
			shell.dispose();
			image.dispose();""",
     """		// one host shell for all rows; each row's fixture lives in a fresh child composite
		Shell shell = new Shell(display);
		Composite stage = new Composite(host, SWT.NONE);
		stage.setLayout(new FillLayout());
		Image image = new Image(display, 8, 8);
		List<GC> gcs = new ArrayList<>();
		TtFixture fx;
		try {
			fx = (TtFixture) Class.forName(fixtureClass, true, ApiProbe.class.getClassLoader())
					.getDeclaredConstructor().newInstance();
			fx.build(stage);
			host.layout(true, true);
			pump(display);
		} catch (Throwable ex) {
			stage.dispose();
			shell.dispose();
			image.dispose();"""),
    ("""			image.dispose();
			pump(display);
		}
	}

	private static String field(""",
     """			if (!stage.isDisposed()) {
				stage.dispose();
			}
			image.dispose();
			pump(display);
		}
	}

	private static String field("""),
    ("""import org.eclipse.swt.layout.FillLayout;
""", """import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
"""),
])
patch("probe/org/eclipse/nebula/m3/tt/TtFixture.java", [
    ("import org.eclipse.swt.widgets.Shell;", "import org.eclipse.swt.widgets.Composite;"),
    ("""	/** Builds the widget and its sample content inside {@code shell}. */
	void build(Shell shell);""", """	/** Builds the widget and its sample content inside {@code parent} (FillLayout). */
	void build(Composite parent);"""),
])
patch("fixtures/xviewer/org/eclipse/nebula/m3/tt/XViewerFixture.java", [
    ("""	public void build(Shell shell) {
		Composite parent = new Composite(shell, SWT.NONE);""", """	public void build(Composite stage) {
		Composite parent = new Composite(stage, SWT.NONE);"""),
    ("import org.eclipse.swt.widgets.Shell;\n", ""),
])
