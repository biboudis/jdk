/*
 * @test /nodynamiccopyright/
 * @summary
 * @enablePreview
 * @compile AssignmentNR1S.java
 * @run main AssignmentNR1S
 */
import java.util.Objects;

public class AssignmentNR1S {
    public static void main(String[] args) {
        method0();
        method1();
        method2();
        method4();
        method5();
    }

    static sealed abstract class SA1 permits SB1 {}
    static final class SB1 extends SA1 {}
    static void method0() {
        SA1 sa = new SB1();
        SB1 sb = sa;         // OK
    }
    static void method1() {
        SA1 sa = new SB1();
        SB1[] sb = new SB1[1];
        sb[0] = sa;  // ok
    }
    static SB1 method2() {
        SA1 sa = new SB1();
        return sa;   // ok
    }
//    <T extends SA1> void method3(T t) { // TODO
//        SB1 sb = t;  // ok
//    }

    static sealed abstract class SA2<T> permits SB2 {}
    static final class SB2<T> extends SA2<T> {}
    static <T> void method4() {
        SA2<T> sa = new SB2<>();  // WRC, OK
        SB2<T> sb = sa;
    }

    static record R1(int x) implements IR {}
    static sealed interface IR {}
    static void method5() {
        IR ir = new R1(42);
        R1(int x) = ir;         // OK
        R1 r1 = ir;             // OK
    }

    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + "," +
                    "got: " + actual);
        }
    }
}

