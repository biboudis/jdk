/*
 * @test /nodynamiccopyright/
 * @summary Ensure non-NR1S assignments stay rejected in all modes
 * @compile/fail/ref=AssignmentNR1SRegressionErrors.out -XDrawDiagnostics AssignmentNR1SRegressionErrors.java
 * @compile/fail/ref=AssignmentNR1SRegressionErrors.out --enable-preview --source ${jdk.version} -XDrawDiagnostics AssignmentNR1SRegressionErrors.java
 */
public class AssignmentNR1SRegressionErrors {
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
}
