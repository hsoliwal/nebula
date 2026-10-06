import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2resolver.DefaultTargetDefinitionVariableResolver;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolver;
import org.eclipse.tycho.targetplatform.TargetDefinitionSyntaxException;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;

/** Calls the installed Tycho resolver; no resolver algorithm is duplicated. */
public final class TargetUriProbe {
    private TargetUriProbe() { }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(URI.create(args[0]));
        if (!root.toFile().isDirectory()) {
            throw new AssertionError("Oracle root does not exist: " + args[0]);
        }
        Properties properties = new Properties();
        properties.setProperty("maven.multiModuleProjectDirectory", root.toAbsolutePath().toString());
        String rootUri = root.toUri().toASCIIString();
        properties.setProperty("m3.nebula.checkout.uri", rootUri.endsWith("/")
                ? rootUri.substring(0, rootUri.length() - 1) : rootUri);
        MavenContext context = (MavenContext) Proxy.newProxyInstance(MavenContext.class.getClassLoader(),
                new Class<?>[] { MavenContext.class }, (proxy, method, values) -> switch (method.getName()) {
                    case "getSessionProperties" -> properties;
                    case "getProjects" -> List.of();
                    case "getLogger" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        DefaultTargetDefinitionVariableResolver variables = new DefaultTargetDefinitionVariableResolver(context, null);
        Object resolver = TargetDefinitionResolver.class.getConstructors()[0]
                .newInstance(List.of(), null, null, null, context, null, variables);
        Method resolve = TargetDefinitionResolver.class.getDeclaredMethod("resolveRepositoryLocation", String.class);
        resolve.setAccessible(true);
        String raw = "file:///${system_property:maven.multiModuleProjectDirectory}/m3/qualified-mixed-original435-target";
        try {
            resolve.invoke(resolver, raw);
            throw new AssertionError("Raw Windows root unexpectedly admitted as repository URI");
        } catch (InvocationTargetException failure) {
            if (!(failure.getCause() instanceof TargetDefinitionSyntaxException)) {
                throw failure;
            }
        }
        TargetDefinitionFile definition = TargetDefinitionFile.read(
                root.resolve("m3/qualified-mixed-original435-target/mixed.target").toUri());
        TargetDefinition.InstallableUnitLocation unitLocation =
                (TargetDefinition.InstallableUnitLocation) definition.getLocations().get(0);
        if (unitLocation.getUnits().size() != 125 || unitLocation.getRepositories().size() != 1) {
            throw new AssertionError("Qualified target selection changed");
        }
        String sourceLocation = unitLocation.getRepositories().get(0).getLocation();
        if (!sourceLocation.equals(args[1])) {
            throw new AssertionError("Relocated target file no longer matches sealed source location");
        }
        URI observed = (URI) resolve.invoke(resolver, sourceLocation);
        URI expected = root.resolve("m3/qualified-mixed-original435-target").toUri();
        if (!observed.isAbsolute() || !"file".equals(observed.getScheme())) {
            throw new AssertionError(observed + " expected " + expected);
        }
        if (!Path.of(observed).equals(root.resolve("m3/qualified-mixed-original435-target"))) {
            throw new AssertionError("URI round trip lost root identity");
        }
        try (var input = Path.of(observed).resolve("content.xml").toUri().toURL().openStream()) {
            String contentSha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
            if (!contentSha.equals(args[2])) {
                throw new AssertionError("Resolved owned metadata differs: " + contentSha);
            }
        }
        System.out.println("EXACT_TYCHO_URI_PASS\t" + observed.toASCIIString());
    }
}
