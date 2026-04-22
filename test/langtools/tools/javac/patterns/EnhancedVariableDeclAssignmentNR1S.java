/*
 * @test /nodynamiccopyright/
 * @summary Verify preview NR1S assignment conversions
 * @compile/fail/ref=EnhancedVariableDeclAssignmentNR1SNoPreview.out -XDrawDiagnostics EnhancedVariableDeclAssignmentNR1S.java
 * @compile/ref=EnhancedVariableDeclAssignmentNR1S.preview.out -XDrawDiagnostics -Xlint:preview --enable-preview --source ${jdk.version} EnhancedVariableDeclAssignmentNR1S.java
 * @run main/othervm --enable-preview EnhancedVariableDeclAssignmentNR1S
 */
import java.util.Objects;
import java.util.List;

public class EnhancedVariableDeclAssignmentNR1S {
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

    sealed interface I permits Mid {}
    non-sealed interface Mid extends I {}
    static final class Leaf implements Mid {}
    static void nonSealed1(I i) {
        Mid m = i;          // OK

        // equivalent exhaustive switch
        // int ret = switch (i) {
        //     case Mid m2 -> 0;
        // };
    }

    // taken from test/langtools/tools/javac/patterns/Exhaustiveness.java
    sealed interface Base permits Special, Value {}
    non-sealed interface Value extends Base {}
    sealed interface Special extends Base permits SpecialValue {}
    non-sealed interface SpecialValue extends Value, Special {}
    static void nonSealed2(final Base base) {
        Value value = base; // OK

        // equivalent exhaustive switch
        // int ret = switch (base) {
        //     case Value value2 -> 0;
        // };
    }

    static sealed interface GI1<T> permits GA1, GB1 {}
    static final class GA1 implements GI1<String> {}
    static final class GB1 implements GI1<Integer> {}

    static void genericDirectLeafInterface(GI1<String> i) {
        GA1 a = i; // OK

        // equivalent exhaustive switch
        // int x = switch (i) {
        //     case GA1 a2 -> 0;
        // };
    }

    static sealed interface GI2<T> permits GMid2, GOther2 {}
    static non-sealed interface GMid2<T> extends GI2<T> {}
    static final class GMid2String implements GMid2<String> {}
    static final class GOther2 implements GI2<Integer> {}

    static void genericNonSealedTarget(GI2<String> i) {
        GMid2<?> m = i; // OK

        // equivalent exhaustive switch
        // int x = switch (i) {
        //     case GMid2<?> m2 -> 0;
        // };
    }

    interface J {}
    static sealed interface II<T> permits A2, B2 {}
    static final class A2 implements II<Long>, J {}
    static final class B2 implements II<Long>, J {}
    static void assignmentToCommonInterface(II<Long> i) {
        J j = i;          // OK

        // equivalent should be exhaustive switch
        // int ret = switch (i) {
        //     case J j2 -> 0;
        // };
    }


    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + "," +
                    "got: " + actual);
        }
    }
}
