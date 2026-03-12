/*
 * @test /nodynamiccopyright/
 * @summary
 * @compile/fail/ref=AssignmentNR1SNoPreview.out -XDrawDiagnostics AssignmentNR1S.java
 * @compile/ref=AssignmentNR1S.preview.out -XDrawDiagnostics -Xlint:preview --enable-preview --source ${jdk.version} AssignmentNR1S.java
 * @run main/othervm --enable-preview AssignmentNR1S
 */
import java.util.Objects;
import java.util.List;

public class AssignmentNR1S {
    public static void main(String[] args) {
        assignmentFromSealedSupertype();
        arrayStoreFromSealedSupertype();
        returnFromSealedSupertype();
        genericAssignmentWithTypeVariable();
        interfaceToRecordAssignment();
        reassignmentFromSealedInterface();
        enhancedForElementNarrowing();
        returnFromParameterSealedSupertype(new SB1());
    }

    static sealed abstract class SA1 permits SB1 {}
    static final class SB1 extends SA1 {}
    static void assignmentFromSealedSupertype() {
        SA1 sa = new SB1();
        SB1 sb = sa;         // OK
    }
    static void arrayStoreFromSealedSupertype() {
        SA1 sa = new SB1();
        SB1[] sb = new SB1[1];
        sb[0] = sa;  // ok
    }
    static SB1 returnFromSealedSupertype() {
        SA1 sa = new SB1();
        return sa;   // ok
    }

    static sealed abstract class SA2<T> permits SB2 {}
    static final class SB2<T> extends SA2<T> {}
    static <T> void genericAssignmentWithTypeVariable() {
        SA2<T> sa = new SB2<>();  // WRC, OK
        SB2<T> sb = sa;
    }

    static record R1(int x) implements IR {}
    static sealed interface IR {}
    static void interfaceToRecordAssignment() {
        IR ir = new R1(42);
        R1 r1 = ir;             // OK
    }

    static sealed interface IFoo permits FooImpl {}
    static final class FooImpl implements IFoo {}
    static void reassignmentFromSealedInterface() {
        IFoo f = new FooImpl();
        FooImpl fi = new FooImpl();
        fi = f;      // OK
    }

    static void enhancedForElementNarrowing() {
        List<IFoo> fs = List.of(new FooImpl(), new FooImpl());
        int count = 0;
        for (FooImpl fi : fs) {
            count++;
        }
        assertEquals(2, count);
    }

    static SB1 returnFromParameterSealedSupertype(SA1 sa) {
        return sa; // OK
    }

    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + "," +
                    "got: " + actual);
        }
    }
}
