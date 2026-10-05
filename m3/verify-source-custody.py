# SPDX-License-Identifier: EPL-2.0
"""Read-only exact-byte custody for the reviewed admitted owner paths."""
from hashlib import sha256
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET


def blob(revision, path):
    return subprocess.check_output(["git", "show", revision + ":" + path, "--"])


def require(condition, reason):
    if not condition:
        raise SystemExit(reason)


def profile_shape(raw):
    ns = "{http://maven.apache.org/POM/4.0.0}"
    root = ET.fromstring(raw)
    profiles = [p for p in root.findall(ns + "profiles/" + ns + "profile")
                if p.findtext(ns + "id") == "m3-offline-p2"]
    require(len(profiles) == 1, "exactly one admitted p2 profile required")
    profile = profiles[0]
    require(not profile.attrib and [c.tag for c in profile] == [ns + "id", ns + "properties"],
            "p2 profile must retain exact opt-in shape")
    properties = profile[1]
    names = ["target-platform-platform", "target-platform-gef", "target-platform-swtbot"]
    require(not properties.attrib and [c.tag for c in properties] == [ns + x for x in names],
            "p2 profile property shape drift")
    require(all(not c.attrib and len(c) == 0 and c.text == "${m3.p2.mirror.url}" for c in properties),
            "p2 mirror properties drift")


def verify(base):
    # Exact postimages of the already-reviewed statement distillation in 48846be.
    # This branch admits preservation only; new edits still require their own recipe.
    distilled = {
        'org.eclipse.nebula.presentations.shelf/src/org/eclipse/swt/nebula/presentations/shelf/CTabFolderStackPresentation.java': 'd10871e9c8f9e80898e6f64bbf5dc38b9e2d0609cddbc9bace84074be30bc980',
        'org.eclipse.nebula.presentations.shelf/src/org/eclipse/swt/nebula/presentations/shelf/EmptyStandaloneStackPresentation.java': '0533cb90fb266489732524ad7064c64f10dba881f9d7f5774dbb2b08e8ca1d5b',
        'org.eclipse.nebula.presentations.shelf/src/org/eclipse/swt/nebula/presentations/shelf/PShelfStackPresentation.java': '34c50c2edc234f28cfe120f289b0a33634885fd3f2ac6bc16692ce20c3378967',
        'org.eclipse.nebula.presentations.shelf/src/org/eclipse/swt/nebula/presentations/shelf/PresentationFactory.java': '5575fa46a6867ab57fabaf7bd5f1d9d31476c7942760abed0e51ba32943cdd7b',
    }
    rows = Path(__file__).with_name("source-custody.tsv").read_text(encoding="utf-8").splitlines()
    require(len(rows) == 4, "exactly four copyright paths required")
    for row in rows:
        path, before_hash, after_hash = row.split("\t")
        before, after = blob(base, path), blob("HEAD", path)
        if sha256(after).hexdigest() == distilled[path]:
            require(before == after, "distilled copyright source changed from base: " + path)
            print(path + "\tPASS_UNCHANGED_DISTILLED\t" + distilled[path])
            continue
        require(sha256(after).hexdigest() == after_hash, "copyright postimage drift: " + path)
        if sha256(before).hexdigest() == after_hash:
            require(before == after, "already-admitted copyright bytes changed: " + path)
        else:
            require(sha256(before).hexdigest() == before_hash, "copyright preimage drift: " + path)
            require(before.count(b"\xa9") == 1 and all(byte < 128 or byte == 169 for byte in before),
                    "copyright input must contain exactly one non-ASCII copyright byte: " + path)
            require(b"// \xa9 Copyright IBM Corp. 2005" in before, "copyright context absent: " + path)
            require(before.replace(b"\xa9", b"\xc2\xa9") == after, "copyright bytes escaped header-only replacement: " + path)
            require(before.decode("latin-1") == after.decode("utf-8"), "copyright decoded text changed: " + path)
        print(path + "\tPASS\t" + sha256(before).hexdigest() + "\t" + sha256(after).hexdigest())
    parent = "releng/org.eclipse.nebula.nebula-parent/pom.xml"
    pre, post = blob(base, parent), blob("HEAD", parent)
    expected = "1b30a4af10a3a3b5731e45ae36afa7c07faaf7d721c4b7bf15f88c3a481fc6f4"
    profile_hash = "7bd62f0f528e3955a14aaa79fa693f758d6e2f779ec569d837aa410cec9ab408"
    ci_hash = "7598a2672ff8ccdf31589d1528daca3fe22ef5881fc98ea17daff23d5d444d00"
    post_hash = sha256(post).hexdigest()
    require(post_hash in {expected, profile_hash, ci_hash}, "admitted parent postimage drift")
    if post_hash in {profile_hash, ci_hash}:
        profile_shape(post)
    require(sha256(pre).hexdigest() in {"255bd1e72e6e6368fdfbd76c6cdcf392d28857a97678290acb8ebbefd2766103", expected, profile_hash, ci_hash},
            "parent aggregation preimage drift")
    print(parent + "\tPASS\t" + sha256(pre).hexdigest() + "\t" + post_hash)

    css = "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/src/org/eclipse/nebula/widgets/cdatetime/css/CDateTimePropertyHandler.java"
    css_pre, css_post = blob(base, css), blob("HEAD", css)
    css_hash = "9b95b52df265443f996fc67e0316025bce167281d175478b0070edf56c54d87d"
    require(sha256(css_post).hexdigest() == css_hash, "CDateTime reviewed postimage drift")
    require(sha256(css_pre).hexdigest() in {
        "8b0542080ffeab65e94dc0e05182866559718e5b2c7187bce4835e717f40556a",
        "04f07a9bc03ab54dd4635b7dd24548fa2d39035c51f5923181d5c5cd62785fa6",
        css_hash}, "CDateTime reviewed preimage drift")
    css_attrs = "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/.gitattributes"
    require(sha256(blob("HEAD", css_attrs)).hexdigest() == "ba79ee72cb77bf047e87fb268de12ccedd33773d7175d9bebe6bb2b9617cf9ec",
            "CDateTime exact source checkout attributes drift")
    print(css + "\tPASS\t" + sha256(css_pre).hexdigest() + "\t" + css_hash)


    provider_test = "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/css/BaseCSSThemingTest.java"
    provider_test_pre, provider_test_post = blob(base, provider_test), blob("HEAD", provider_test)
    require(sha256(provider_test_post).hexdigest() == "44dd7b7a3f606a5b72d1d2ba2c194d03fc4303795394cd59adc12b4cc87b275f",
            "CDateTime test provider initializer postimage drift")
    require(sha256(provider_test_pre).hexdigest() in {
        "79a0f8e1a6c46e87b6f49ece1389867da540a626c1d4fa43138df0e181158462",
        "d8eed0c9bb58fe73c2782499693fede24513c0c3f2b6caec7fcd4531114c8e2b",
        "4cc46cffd07c97efd11780afd11aea6c422458f183ebcc72627a8c27d9756493",
        "cba91a328b94861c5c1d5b31c097825b1eeecefbf1716235fc918770024da859", "44dd7b7a3f606a5b72d1d2ba2c194d03fc4303795394cd59adc12b4cc87b275f"}, "CDateTime test provider initializer preimage drift")
    provider = "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/tests/css/CSSPropertyHandlerSimpleProviderImpl.java"
    provider_rows = subprocess.check_output(["git", "ls-tree", "-z", base, "--", provider])
    require(not provider_rows or sha256(blob(base, provider)).hexdigest() == "234eff7ce055796a8ea7f1d266dc69a63a280e0d4eb36ade24563e18b784fa48",
            "CDateTime retained provider preimage drift")
    require(sha256(blob("HEAD", provider)).hexdigest() == "234eff7ce055796a8ea7f1d266dc69a63a280e0d4eb36ade24563e18b784fa48",
            "CDateTime retained provider postimage drift")
    attributes_rows = subprocess.check_output(["git", "ls-tree", "-z", base, "--", ".gitattributes"])
    require(not attributes_rows or sha256(blob(base, ".gitattributes")).hexdigest() in {
        "131a858ec9b08390eb732a50bfeeab3a9a2a98b74b4da0b6580996977b8071d2", "270049a324217550a8efac809c73faa3db04846463c4fc4314d8b2063c48a8f3", "3cc00b7e012e3a9f991b246db558dd6591952620a807aca5352e5be9b29f31e6"}, "CDateTime exact root attribute preimage drift")
    require(sha256(blob("HEAD", ".gitattributes")).hexdigest() == "3cc00b7e012e3a9f991b246db558dd6591952620a807aca5352e5be9b29f31e6",
            "CDateTime exact root attribute postimage drift")
    print(provider_test + "\tPASS\t" + sha256(provider_test_pre).hexdigest() + "\t44dd7b7a3f606a5b72d1d2ba2c194d03fc4303795394cd59adc12b4cc87b275f")
    print(provider + "\tPASS\t" + (sha256(blob(base, provider)).hexdigest() if provider_rows else "ABSENT") + "\t234eff7ce055796a8ea7f1d266dc69a63a280e0d4eb36ade24563e18b784fa48")


    e4_annotation_rows = [
        ('widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/SimpleWidgetsPart.java', '88553ed64c0dfb7c08a5d7c64380886e65ef855b72774bcb3a8b5e52273a299b', '58d9bc6330c8670147254693dcb395cd50e88b7d3e780aacb4806494f05a6c29', '471072428aa3cc6ff0e19567fab7199ca7f31333a58905a1c86489cc3b5e3007', '08d0d45568b0d895607b8ebe74be1c9975dd1321f63f27e76b75170ae2404a99'),
        ('widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/BigWidgetsPart.java', 'a0b890d25d9c7cdd433f00e49cecd87083ec12b1244a96f2ebc4903d826acc16', '7cd7179eb49e606fca108dba962e1a0fb4c201f8d2517dc77032786424da95ef', 'f7cf28894b872ed09dc009ae59632b3faa777b502a0fd73168ca85e46c62543c', 'f7cf28894b872ed09dc009ae59632b3faa777b502a0fd73168ca85e46c62543c'),
        ('widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/META-INF/MANIFEST.MF', '1bd3b08226fbc0ad9f0586c60cc71debe027992fc55f75fd0f4eff57664369cf', 'd7a11ebfd717b59c10860315e2b0a1f5954428ab5500f59c968b552cc3d48893', '99ec161a425ef2a974b02a1986eb4185b44c515d26642f58a9c962a656841323', '99ec161a425ef2a974b02a1986eb4185b44c515d26642f58a9c962a656841323'),
        ('widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/org.eclipse.nebula.widgets.cdatetime.example.e4.product', '4ff3163f0a625e1adab50bf3e216026bd0fbb0353aa84012c6d8092cc61956c1', '10fdec2a327f17c60d817c4e675382505695c0d6a8945a9e1e78c3c550ad7d3c', 'ef4669a264a851ad0e3eeb767f4846a08940621b205e959872e030f2811333fe', 'ef4669a264a851ad0e3eeb767f4846a08940621b205e959872e030f2811333fe'),
    ]
    for path, git_before, raw_before, previous_after, exact_after in e4_annotation_rows:
        before, after = blob(base, path), blob("HEAD", path)
        require(sha256(before).hexdigest() in {git_before, raw_before, previous_after, exact_after},
                "E4 annotation exact preimage drift: " + path)
        require(sha256(after).hexdigest() == exact_after, "E4 annotation exact postimage drift: " + path)
        print(path + "\tPASS\t" + sha256(before).hexdigest() + "\t" + exact_after)
    e4_attrs = 'widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/.gitattributes'
    e4_attr_rows = subprocess.check_output(["git", "ls-tree", "-z", base, "--", e4_attrs])
    require(not e4_attr_rows or sha256(blob(base, e4_attrs)).hexdigest() == "f5808c25747545be15ebbbbdc23d24355fe66f3b4ee17dabf64cf239466ea7ab",
            "E4 exact attributes preimage drift")
    require(sha256(blob("HEAD", e4_attrs)).hexdigest() == "f5808c25747545be15ebbbbdc23d24355fe66f3b4ee17dabf64cf239466ea7ab",
            "E4 exact attributes postimage drift")
    print(e4_attrs + "\tPASS\t" + (sha256(blob(base, e4_attrs)).hexdigest() if e4_attr_rows else "ABSENT") + "\tf5808c25747545be15ebbbbdc23d24355fe66f3b4ee17dabf64cf239466ea7ab")


if __name__ == "__main__":
    require(len(sys.argv) == 2, "BASE_REVISION required")
    verify(sys.argv[1])
