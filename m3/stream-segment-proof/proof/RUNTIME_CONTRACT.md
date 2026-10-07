# Prewritten real-source stream-segment contract

Pinned Nebula base: f4ebfe7edcc1f953d4dd76312f3a7bc6fbce645b. The bounded owner is widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar/src/org/eclipse/nebula/widgets/opal/roundedtoolbar/RoundedToolbar.java, SHA256 fd00029ccb595dfedd0ec1c0604856359e065c9d2ca3fcbbe86382b15bbc0c59.

Three identical MouseDown/MouseUp/MouseHover segments select the first item whose bounds contain the current Event coordinates and which is enabled. The type/callee segment facts admit the private candidate name findFirstEnabledItemContainingEvent. The ordering evidence remains bounds -> current event fields -> contains -> enabled -> first encounter; the spelling of the name is not execution-order authority.

The before/after oracle compiles the actual full RoundedToolbar, RoundedToolItem, GradientColor and AdvancedPath sources against real SWT/JFace. It loads the exact before/candidate classes in separate owner-isolated URLClassLoaders. Mockito substitutes foreign widget callees; reflection runs the real private addListeners method and captures the actual registered SWT Listener callbacks. There is no copied predicate surrogate and no rendered UI/screenshots claim.

Public/package/protected signatures and existing public method names stay unchanged. The Point-based getItem method is a different segment that intentionally does not require enabled items; it remains outside this change.

Prewritten runtime discriminators:

- Listener registration performs no item search or tooltip side effects. MouseDown/MouseUp nonprimary-button guards still return before search.
- Encounter order and overlapping bounds select the first enabled matching item, stop before later getters and skip isEnabled for out-of-bounds items.
- Empty/no-match/null item/null bounds/null Event behavior and its effect prefix are preserved.
- Externally supplied getter throwables preserve the same object and suppress later effects.
- getBounds may change the same Event coordinates. Coordinates must be read after that getter; passing Event.x/y eagerly breaks this behavior.
- Mutating the underlying ArrayList during a matching predicate still throws ConcurrentModificationException before tooltip effects. A naive enhanced-for return can miss that check.
- Mutating during a missed predicate also preserves the spliterator check, including removing the final item so an iterator's next hasNext can terminate silently.
- A callback that mutates the list and then throws retains its throwable; a later modification check must not mask it.
- State and Event changes after listener registration are observed at handling time.
- Tooltip getters keep their original two-call behavior for a nonnull first result.

The compiler lane retains JavaSE-17 source compatibility and uses -proc:none -Xlint:all -Werror. Existing warnings or absent SDK native initialization are evidence to resolve, not reasons to weaken checks or fabricate stubs.

The explicit Consumer/spliterator candidate may allocate a callback object per invocation. This contract makes no zero-allocation, universal stream-GC, rendering or canonical-promotion claim. Patternizer work is deferred.
