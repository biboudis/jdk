/*
 * @test /nodynamiccopyright/
 * @summary
 * @compile/fail/ref=AssignmentNR1S.out -XDrawDiagnostics AssignmentNR1S.java
 * @compile --enable-preview --source ${jdk.version} AssignmentNR1S.java
 * @run main/othervm --enable-preview AssignmentNR1S
 */
import java.util.Objects;
import java.util.List;

public class AssignmentNR1S {
    public static void main(String[] args) {
        method0();
        method1();
        method2();
        method3();
        method4();
        method5();
        method6();
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

    static sealed abstract class SA2<T> permits SB2 {}
    static final class SB2<T> extends SA2<T> {}
    static <T> void method3() {
        SA2<T> sa = new SB2<>();  // WRC, OK
        SB2<T> sb = sa;
    }

    static record R1(int x) implements IR {}
    static sealed interface IR {}
    static void method4() {
        IR ir = new R1(42);
        R1 r1 = ir;             // OK
    }

    static sealed interface IFoo permits FooImpl {}
    static final class FooImpl implements IFoo {}
    static void method5() {
        IFoo f = new FooImpl();
        FooImpl fi = new FooImpl();
        fi = f;      // OK
    }

    static void method6() {
        List<IFoo> fs = List.of(new FooImpl(), new FooImpl());
        int count = 0;
        for (FooImpl fi : fs) {
            count++;
        }
        assertEquals(2, count);
    }

    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + "," +
                    "got: " + actual);
        }
    }
}
