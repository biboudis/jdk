/*
 * @test /nodynamiccopyright/
 * @summary
 * @enablePreview
 * @compile -XDfind=all PatternAssignmentEnhancedForTestAllAnalyzers.java
 */
public class PatternAssignmentEnhancedForTestAllAnalyzers {
    private void test(Iterable<? extends R> l) {
        for (R(Object a) : l) { }
    }
    record R(Object a) {}
}
