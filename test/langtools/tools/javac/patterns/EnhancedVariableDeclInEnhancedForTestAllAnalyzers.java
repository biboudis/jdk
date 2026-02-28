/*
 * @test /nodynamiccopyright/
 * @summary
 * @enablePreview
 * @compile -XDfind=all EnhancedVariableDeclInEnhancedForTestAllAnalyzers.java
 */
public class EnhancedVariableDeclInEnhancedForTestAllAnalyzers {
    private void test(Iterable<? extends R> l) {
        for (R(Object a) : l) { }
    }
    record R(Object a) {}
}
