/*
 * @test /nodynamiccopyright/
 * @summary Ensure non-NR1S assignments stay rejected in all modes
 * @compile/fail/ref=EnhancedVariableDeclAssignmentNR1SRegressionErrors.out -XDrawDiagnostics EnhancedVariableDeclAssignmentNR1SRegressionErrors.java
 * @compile/fail/ref=EnhancedVariableDeclAssignmentNR1SRegressionErrors.out --enable-preview --source ${jdk.version} -XDrawDiagnostics EnhancedVariableDeclAssignmentNR1SRegressionErrors.java
 */
public class EnhancedVariableDeclAssignmentNR1SRegressionErrors {
    static sealed abstract class SA2<T> permits SB2 {}
    static final class SB2<T> extends SA2<T> {}
    static void rejectMismatchedGenericArguments() {
        SA2<Integer> sa = new SB2<>();
        SB2<String> sb = sa; // always error
    }

    static sealed interface SI permits Mid {}
    static non-sealed interface Mid extends SI {}
    static final class Leaf implements Mid {}
    static void rejectIndirectPermittedSubtypeAssignment() {
        SI si = new Leaf();
        Leaf leaf = si; // always error
    }

    static sealed class ConcreteSA permits ConcreteSB {}
    static final class ConcreteSB extends ConcreteSA {}
    static void rejectConcreteSealedSuperclassAssignment() {
        ConcreteSA sa = new ConcreteSA();
        ConcreteSB sb = sa; // always error
    }

    static sealed abstract class MultiSA permits MultiSB1, MultiSB2 {}
    static final class MultiSB1 extends MultiSA {}
    static final class MultiSB2 extends MultiSA {}
    static void rejectAbstractSealedSuperclassWithMultipleSubclasses() {
        MultiSA sa = new MultiSB1();
        MultiSB1 sb = sa; // always error
    }

    static sealed interface MultiSI permits MultiImpl1, MultiImpl2 {}
    static final class MultiImpl1 implements MultiSI {}
    static final class MultiImpl2 implements MultiSI {}
    static void rejectSealedInterfaceWithMultipleSubtypes() {
        MultiSI si = new MultiImpl1();
        MultiImpl1 impl = si; // always error
    }

    static sealed interface I<T> permits A, B {}
    static final class A implements I<String> {}
    static final class B implements I<Integer> {}
    static void rejectSealedTypeWithNonApplicableSubtypes() {
        I<Long> i = null;
        A a = i;             // always error
    }

    class E {}
    sealed interface A1 permits B1, C1, D1 {}
    static non-sealed abstract class B1 implements A1 {}
    static non-sealed abstract class C1 extends B1 implements A1 {}
    static final class D1 extends C1 implements A1 {}
    static void rejectSealedTypeWithUnrelated() {
        E e = null;
        B1 b = e;            // always error
    }

    interface J {}
    static sealed interface II<T> permits A2, B2 {}
    static final class A2 implements II<String>, J {}
    static final class B2 implements II<Integer>, J {}
    static void f(II<Long> i) {
        J j = i;            // always error
    }

    static interface JG<T> {}
    static sealed interface III<T> permits A3, B3 {}
    static final class A3 implements III<String>, JG<String> {}
    static final class B3 implements III<Integer>, JG<Integer> {}
    static void rejectUncheckedGenericTarget(III<String> i) {
        JG<String> j = i;   // always error
    }

}
