// SPDX-License-Identifier: Apache-2.0
package com.synexia.ai.m3scale;

import static com.synexia.ai.m3scale.M3BindingModel.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;

/** Parallel syntax inventory, then module-context attribution. Hashes are evidence, never equivalence proofs. */
public final class M3AttributedAtomizer {
    private M3AttributedAtomizer() { }
    public static Snapshot analyze(List<Source> sources, List<Context> contexts, int workers) throws Exception {
        if (workers < 1 || workers > 32 || sources.isEmpty() || sources.size() > MAX_FILES
                || sources.stream().mapToLong(s -> bytes(s.text()).length).sum() > MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("ANALYSIS_BUDGET");
        }
        TreeMap<String,Context> byModule = new TreeMap<>();
        for (Context c : contexts) if (byModule.putIfAbsent(c.module(), c) != null) throw new IllegalArgumentException("DUPLICATE_CONTEXT");
        Set<String> unique = new HashSet<>();
        for (Source s : sources) if (!unique.add(s.key()) || !byModule.containsKey(s.module())) throw new IllegalArgumentException("SOURCE_CONTEXT");
        Map<String,String> stamps = new TreeMap<>();
        for (Context c : contexts) stamps.put(c.module(), c.fingerprint());
        ThreadPoolExecutor pool = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_FILES), new ThreadPoolExecutor.AbortPolicy());
        List<FileFact> facts = new ArrayList<>(); List<Future<FileFact>> jobs = new ArrayList<>();
        try {
            for (Source source : sources) jobs.add(pool.submit(() -> syntax(source, byModule.get(source.module()))));
            for (Future<FileFact> job : jobs) facts.add(job.get());
        } finally {
            pool.shutdown();
            try { if (!pool.awaitTermination(60, TimeUnit.SECONDS)) { pool.shutdownNow(); throw new IllegalStateException("PARSER_NOT_DRAINED"); } }
            catch (InterruptedException stop) { pool.shutdownNow(); Thread.currentThread().interrupt(); throw stop; }
        }
        List<Atom> atoms = new ArrayList<>(); List<Reference> refs = new ArrayList<>(); List<Slice> slices = new ArrayList<>();
        TreeSet<String> obligations = new TreeSet<>(); Map<String,Set<String>> dependencies = new TreeMap<>();
        for (Context context : byModule.values()) {
            dependencies.put(context.module(), context.dependencies());
            List<Source> input = sources.stream().filter(s -> s.module().equals(context.module())).toList();
            if (input.isEmpty()) continue;
            try (M3CompilerSession session = new M3CompilerSession(input, context, false)) {
                List<CompilationUnitTree> trees = new ArrayList<>(); session.task.parse().forEach(trees::add);
                session.task.analyze(); Trees api = Trees.instance(session.task);
                for (CompilationUnitTree tree : trees) {
                    Source source = session.sources.get(tree.getSourceFile().toUri());
                    Walker walker = new Walker(source, tree, api, session.task.getTypes(), atoms, refs, obligations);
                    walker.scan(tree, null); slices.addAll(walker.slices());
                }
                obligations.addAll(session.diagnosticCodes(context.module()));
            } catch (RuntimeException | IOException failed) {
                obligations.add("INCOMPLETE:" + context.module() + ":" + failed.getClass().getSimpleName());
            }
        }
        for (FileFact fact : facts) if (!fact.parsed()) obligations.add("ERROR:PARSE:" + fact.file());
        // Stable compiler input is required, including classpath contents, not just file timestamps.
        for (Context c : contexts) if (!c.fingerprint().equals(stamps.get(c.module()))) throw new IOException("COMPILATION_CONTEXT_CHANGED");
        facts.sort(Comparator.comparing(FileFact::file)); atoms.sort(Comparator.comparing(Atom::id));
        refs.sort(Comparator.comparing(Reference::fromFile).thenComparingLong(Reference::startByte).thenComparing(Reference::targetSymbol));
        slices.sort(Comparator.comparing(Slice::file).thenComparingLong(Slice::startByte));
        List<String> identity = new ArrayList<>(); stamps.forEach((k,v) -> { identity.add(k); identity.add(v); });
        for (FileFact f : facts) identity.add(f.file() + ":" + f.exactHash());
        for (Atom a : atoms) { identity.add(a.id()); identity.add(a.contractHash()); identity.add(a.boundLogicHash()); }
        for (Reference r : refs) { identity.add(r.fromAtom()); identity.add(r.targetSymbol()); identity.add(r.dispatch()); }
        identity.addAll(obligations);
        return new Snapshot(facts, atoms, refs, slices, stamps, dependencies, List.copyOf(obligations),
                M3Hash.canonical("M3_BOUND_SNAPSHOT_V1", identity));
    }
    private static FileFact syntax(Source source, Context context) throws IOException {
        try (M3CompilerSession session = new M3CompilerSession(List.of(source), context, false)) {
            List<String> shapes = new ArrayList<>(); String pkg = "";
            for (CompilationUnitTree tree : session.task.parse()) {
                pkg = tree.getPackageName() == null ? "" : tree.getPackageName().toString();
                new TreeScanner<Void,Void>() {
                    @Override public Void scan(Tree node, Void p) {
                        if (node == null) return null;
                        if (shapes.size() > 200_000) throw new IllegalArgumentException("AST_NODE_BUDGET");
                        shapes.add(node.getKind().name()); super.scan(node, p); shapes.add("END"); return null;
                    }
                }.scan(tree, null);
            }
            return new FileFact(source.key(), source.module(), pkg, source.exactHash(),
                    M3Hash.canonical("M3_SYNTAX_V1", shapes), shapes.size()/2, !session.errors());
        }
    }
    private static final class Walker extends TreePathScanner<Void,Void> {
        final Source source; final CompilationUnitTree unit; final Trees trees; final Types types;
        final List<Atom> atoms; final List<Reference> refs; final Set<String> obligations;
        final int[] positions; final TreeSet<Integer> cuts = new TreeSet<>();
        final Deque<String> owners = new ArrayDeque<>(); final Map<String,Integer> occurrences = new HashMap<>();
        Walker(Source source, CompilationUnitTree unit, Trees trees, Types types, List<Atom> atoms,
               List<Reference> refs, Set<String> obligations) {
            this.source=source;this.unit=unit;this.trees=trees;this.types=types;this.atoms=atoms;this.refs=refs;this.obligations=obligations;
            positions = utf8Positions(source.text()); cuts.add(0); cuts.add(positions[source.text().length()]);
            owners.push(source.key());
        }
        @Override public Void scan(Tree node, Void p) {
            if (node == null) return null;
            long start=trees.getSourcePositions().getStartPosition(unit,node), end=trees.getSourcePositions().getEndPosition(unit,node);
            if (start>=0 && end>=start && end<=source.text().length()) { cuts.add(positions[(int)start]); cuts.add(positions[(int)end]); }
            return super.scan(node,p);
        }
        @Override public Void visitClass(ClassTree n,Void p) { return atom(n, () -> super.visitClass(n,p)); }
        @Override public Void visitMethod(MethodTree n,Void p) { return atom(n, () -> super.visitMethod(n,p)); }
        @Override public Void visitVariable(VariableTree n,Void p) {
            Element e=element(getCurrentPath());
            return e!=null && (e.getKind().isField() || e.getKind()==ElementKind.RECORD_COMPONENT)
                    ? atom(n, () -> super.visitVariable(n,p)) : super.visitVariable(n,p);
        }
        @Override public Void visitLambdaExpression(LambdaExpressionTree n,Void p) { return atom(n, () -> super.visitLambdaExpression(n,p)); }
        @Override public Void visitBlock(BlockTree n,Void p) {
            Tree parent=getCurrentPath().getParentPath().getLeaf();
            return parent instanceof ClassTree ? atom(n, () -> super.visitBlock(n,p)) : super.visitBlock(n,p);
        }
        @Override public Void visitIdentifier(IdentifierTree n,Void p) { reference(n,"NAME");return super.visitIdentifier(n,p); }
        @Override public Void visitMemberSelect(MemberSelectTree n,Void p) { reference(n,"MEMBER");return super.visitMemberSelect(n,p); }
        @Override public Void visitNewClass(NewClassTree n,Void p) { reference(n,"CONSTRUCTOR");return super.visitNewClass(n,p); }
        @Override public Void visitMemberReference(MemberReferenceTree n,Void p) { reference(n,"METHOD_REFERENCE");return super.visitMemberReference(n,p); }
        private void reference(Tree n,String kind) {
            Element e=element(getCurrentPath()); long at=trees.getSourcePositions().getStartPosition(unit,n);
            if(at<0 || at>source.text().length()) return;
            boolean resolved=e!=null && e.asType().getKind()!=TypeKind.ERROR;
            if(!resolved) obligations.add("UNRESOLVED:"+source.key()+":"+n.getKind()+":"+at);
            String target=resolved?symbol(e):n.toString();String dispatch="DIRECT";
            if(e instanceof ExecutableElement method && method.getKind()!=ElementKind.CONSTRUCTOR
                    && !method.getModifiers().contains(Modifier.STATIC) && !method.getModifiers().contains(Modifier.PRIVATE)
                    && !method.getModifiers().contains(Modifier.FINAL)) dispatch="VIRTUAL_OPEN_WORLD";
            if(target.startsWith("java.lang.reflect.") || target.startsWith("java.lang.invoke.")
                    || target.startsWith("java.util.ServiceLoader") || target.startsWith("java.lang.Class#forName")) {
                obligations.add("DYNAMIC_LINKAGE:"+source.key()+":"+target);
            }
            if(resolved && (e.getKind()==ElementKind.PACKAGE || e.getKind()==ElementKind.MODULE)) return;
            refs.add(new Reference(owners.peek(),source.key(),target,kind,dispatch,positions[(int)at],resolved));
        }
        private Void atom(Tree n, java.util.function.Supplier<Void> children) {
            long start=trees.getSourcePositions().getStartPosition(unit,n), end=trees.getSourcePositions().getEndPosition(unit,n);
            if(start<0 || end<start || end>source.text().length()) return children.get(); // implicit compiler members
            Element e=element(getCurrentPath());String symbol=e==null?owners.peek()+"/"+n.getKind():symbol(e);
            int occurrence=occurrences.merge(symbol,1,Integer::sum);
            String id=M3Hash.canonical("M3_MEMBER_ID_V1",List.of(source.key(),symbol,Integer.toString(occurrence)));
            List<String> structure=new ArrayList<>(),logic=new ArrayList<>(); TreePath atomPath=getCurrentPath();
            new TreePathScanner<Void,Void>() {
                @Override public Void scan(Tree node,Void unused) {
                    if(node==null)return null;
                    if(structure.size()>400_000)throw new IllegalArgumentException("ATOM_BUDGET");
                    structure.add(node.getKind().name());logic.add(node.getKind().name());
                    super.scan(node,unused);structure.add("END");logic.add("END");return null;
                }
                @Override public Void visitLiteral(LiteralTree node,Void unused) {
                    Object value=node.getValue();logic.add(value==null?"null":value.getClass().getName());logic.add(String.valueOf(value));return null;
                }
                @Override public Void visitIdentifier(IdentifierTree node,Void unused) { bound();return super.visitIdentifier(node,unused); }
                @Override public Void visitMemberSelect(MemberSelectTree node,Void unused) { bound();return super.visitMemberSelect(node,unused); }
                @Override public Void visitVariable(VariableTree node,Void unused) { bound();return super.visitVariable(node,unused); }
                @Override public Void visitMethod(MethodTree node,Void unused) { bound();return super.visitMethod(node,unused); }
                @Override public Void visitClass(ClassTree node,Void unused) { bound();return super.visitClass(node,unused); }
                @Override public Void visitMemberReference(MemberReferenceTree node,Void unused) { bound();return super.visitMemberReference(node,unused); }
                @Override public Void visitNewClass(NewClassTree node,Void unused) { bound();return super.visitNewClass(node,unused); }
                private void bound() {
                    Element value=element(getCurrentPath());logic.add(value==null?"UNRESOLVED:"+getCurrentPath().getLeaf():contract(value));
                    var type=trees.getTypeMirror(getCurrentPath());if(type!=null)logic.add("TYPE:"+type);
                }
            }.scan(atomPath,null);
            String contract=e==null?n.getKind().name():contract(e);
            String visibility=e==null?"LOCAL":visibility(e);
            byte[] all=bytes(source.text());int a=positions[(int)start], b=positions[(int)end];
            atoms.add(new Atom(id,source.key(),owners.peek(),symbol,n.getKind().name(),a,b,
                    M3Hash.sha256(Arrays.copyOfRange(all,a,b)),M3Hash.canonical("M3_BOUND_CONTRACT_V1",List.of(contract)),
                    M3Hash.canonical("M3_AST_SHAPE_V1",structure),M3Hash.canonical("M3_BOUND_LOGIC_V1",logic),M3Hash.simHash(structure),visibility));
            owners.push(id);try{return children.get();}finally{owners.pop();}
        }
        private Element element(TreePath path) { try{return trees.getElement(path);}catch(IllegalArgumentException e){return null;} }
        private String symbol(Element e) {
            if(e instanceof TypeElement type)return type.getQualifiedName().toString();
            if(e instanceof ExecutableElement m) return symbol(m.getEnclosingElement())+"#"+m.getSimpleName()+"("
                    +String.join(",",m.getParameters().stream().map(v->types.erasure(v.asType()).toString()).toList())+")";
            if(e instanceof VariableElement v) {
                if(v.getKind()==ElementKind.PARAMETER && v.getEnclosingElement() instanceof ExecutableElement m)
                    return symbol(m)+"/parameter:"+m.getParameters().indexOf(v);
                return symbol(v.getEnclosingElement())+"/"+v.getKind()+":"+v.getSimpleName();
            }
            return e.getKind()+":"+e;
        }
        private String contract(Element e) {
            List<String> fields=new ArrayList<>(List.of(e.getKind().name(),symbol(e),e.asType().toString()));
            e.getModifiers().stream().map(Enum::name).sorted().forEach(fields::add);
            e.getAnnotationMirrors().stream().map(Object::toString).sorted().forEach(fields::add);
            if(e instanceof VariableElement v) fields.add("CONSTANT:"+v.getConstantValue());
            if(e instanceof ExecutableElement m) {
                fields.add("VARARGS:"+m.isVarArgs());fields.add("DEFAULT:"+m.getDefaultValue());
                m.getThrownTypes().forEach(t->fields.add("THROWS:"+t));
                m.getParameters().forEach(v->{fields.add(v.getSimpleName().toString());fields.add(v.getAnnotationMirrors().toString());});
            }
            if(e instanceof TypeElement t) {
                fields.add("SUPER:"+t.getSuperclass());t.getInterfaces().forEach(i->fields.add("INTERFACE:"+i));
                t.getPermittedSubclasses().forEach(i->fields.add("PERMITS:"+i));
                t.getRecordComponents().forEach(r->fields.add("RECORD:"+r.getSimpleName()+":"+r.asType()));
            }
            StringBuilder encoded=new StringBuilder();fields.forEach(s->M3Hash.append(encoded,s));return encoded.toString();
        }
        private static String visibility(Element e) {
            if(e.getModifiers().contains(Modifier.PRIVATE))return "PRIVATE";
            if(e.getModifiers().contains(Modifier.PUBLIC)||e.getModifiers().contains(Modifier.PROTECTED))return "API";
            return "PACKAGE";
        }
        List<Slice> slices() {
            List<Slice> values=new ArrayList<>();byte[] all=bytes(source.text());Integer previous=null;
            for(Integer next:cuts){if(previous!=null&&next>previous)values.add(new Slice(source.key(),previous,next,
                    M3Hash.sha256(Arrays.copyOfRange(all,previous,next))));previous=next;}return values;
        }
    }
    private static int[] utf8Positions(String text) {
        int[] offsets=new int[text.length()+1];int total=0;
        for(int i=0;i<text.length();){int cp=text.codePointAt(i), chars=Character.charCount(cp);
            offsets[i]=total;if(chars==2)offsets[i+1]=total;
            total+=cp<=0x7f?1:cp<=0x7ff?2:cp<=0xffff?3:4;i+=chars;offsets[i]=total;}
        return offsets;
    }
}
