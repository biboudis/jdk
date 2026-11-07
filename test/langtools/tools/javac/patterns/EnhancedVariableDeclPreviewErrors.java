/*
 * @test /nodynamiccopyright/
 * @bug 8372170
 * @summary Check declarations with a record pattern require preview
 * @compile/fail/ref=EnhancedVariableDeclPreviewErrors.out -XDrawDiagnostics EnhancedVariableDeclPreviewErrors.java
 */

class EnhancedVariableDeclPreviewErrors {
    record Box<T>(T value) {}

    static <T> Box<T> empty() { return new Box<>(null); }

    void test() {
        Box<String>(var value) = empty();
    }
}
