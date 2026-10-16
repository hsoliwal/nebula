// SPDX-License-Identifier: EPL-2.0
// Standalone Java 21 model proving native GC lease lifetime, not SWT native parity.
import java.util.Random;

public final class BadgedLabelGcOwnershipProof {
    private static int asserts;
    private static final class NativeCounter {
        int opened;
        int disposed;
        int measured;
        boolean throwFont;
        boolean throwMeasure;
        boolean throwDispose;
        MockGC allocate() { opened++; return new MockGC(this); }
    }
    private static final class MockGC {
        final NativeCounter owner;
        boolean dead;
        MockGC(NativeCounter owner) { this.owner = owner; }
        void setFont(int font) {
            if (owner.throwFont) throw new IllegalArgumentException("font disposed");
        }
        int[] stringExtent(String text) {
            owner.measured++;
            if (owner.throwMeasure) throw new IllegalStateException("extent unavailable");
            return new int[] {text.length() * 7, 12};
        }
        void dispose() {
            if (dead) throw new AssertionError("double native GC disposal");
            dead=true;owner.disposed++;
            if (owner.throwDispose) throw new IllegalStateException("dispose failed");
        }
    }
    private static int[] oldGeometry(NativeCounter owner, int font, String text) {
        MockGC gc = owner.allocate();
        gc.setFont(font);
        int[] result = gc.stringExtent(text);
        gc.dispose();
        return result;
    }
    private static int[] newGeometry(NativeCounter owner, int font, String text) {
        MockGC gc = owner.allocate();
        Throwable primary = null;
        try {
            gc.setFont(font);
            return gc.stringExtent(text);
        } catch (RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            if (primary == null) {
                gc.dispose();
            } else {
                try { gc.dispose(); }
                catch (RuntimeException | Error disposal) { primary.addSuppressed(disposal); }
            }
        }
    }
    private static void check(boolean outcome, String message) {
        asserts++;
        if (!outcome) throw new AssertionError(message + " at " + asserts);
    }
    private record Failure(String typeAndMessage, int suppressed) {}
    private static Failure failure(Runnable run) {
        try { run.run(); return new Failure("none", 0); }
        catch (RuntimeException error) {
            return new Failure(error.getClass().getName() + ":" + error.getMessage(),
                    error.getSuppressed().length);
        }
    }
    public static void main(String[] args) {
        Random rng = new Random(0x4d334241444745L);
        for (int i=0;i<160000;i++) {
            int type = i % 7;
            boolean fontFailure = type==1 || type==5;
            boolean measureFailure = type==2 || type==6;
            String text = "#".repeat(rng.nextInt(65));
            var oldOwner = new NativeCounter();
            var newOwner = new NativeCounter();
            oldOwner.throwFont=newOwner.throwFont=fontFailure;
            oldOwner.throwMeasure=newOwner.throwMeasure=measureFailure;
            oldOwner.throwDispose=newOwner.throwDispose=(type>=4);
            Failure oldException=failure(()->oldGeometry(oldOwner,4,text));
            Failure newException=failure(()->newGeometry(newOwner,4,text));
            check(oldException.typeAndMessage().equals(newException.typeAndMessage()),
                    "original failure primary exception family and message");
            check(newException.suppressed()==(type==5 || type==6 ? 1 : 0),
                    "close failure must be suppressed under original font/measurement error");
            check(oldOwner.opened==newOwner.opened && oldOwner.measured==newOwner.measured,"same native allocation and measurement order");
            check(newOwner.opened==newOwner.disposed,"all native GC leases released");
            if (!fontFailure && !measureFailure) {
                check(oldOwner.disposed==newOwner.disposed,"successful path same disposal");
                check(newOwner.measured==1,"exactly one measurement");
            } else {
                check(oldOwner.disposed==0,"original failure witness does not close GC");
                check(newOwner.disposed==1,"candidate closes GC after failure");
            }
        }
        // Normal path equivalence including null/empty measure guards is factored into
        // widget's original private cache source, which the recipe never changes.
        System.out.println("BADGEDLABEL_GC_OWNER_PASS assertions=" + asserts);
    }
}
