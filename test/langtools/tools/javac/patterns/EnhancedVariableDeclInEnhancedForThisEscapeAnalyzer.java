/*
 * @test /nodynamiccopyright/
 * @bug 8372170
 * @summary Check this escape warnings for declarations with a record pattern in enhanced for headers
 * @enablePreview
 * @compile/ref=EnhancedVariableDeclInEnhancedForThisEscapeAnalyzer.out -XDrawDiagnostics -Xlint:this-escape EnhancedVariableDeclInEnhancedForThisEscapeAnalyzer.java
 */
import java.util.Iterator;
import java.util.List;

public class EnhancedVariableDeclInEnhancedForThisEscapeAnalyzer implements Iterable<EnhancedVariableDeclInEnhancedForThisEscapeAnalyzer.R> {
    record R(int x) {}

    public EnhancedVariableDeclInEnhancedForThisEscapeAnalyzer() {
        for (R value : this) {
        }
    }

    public EnhancedVariableDeclInEnhancedForThisEscapeAnalyzer(int ignored) {
        for (R(var x) : this) {
        }
    }

    public EnhancedVariableDeclInEnhancedForThisEscapeAnalyzer(String ignored) {
        for (R(_) : this) {
        }
    }

    public EnhancedVariableDeclInEnhancedForThisEscapeAnalyzer(boolean ignored) {
        for (R(var _) : this) {
        }
    }

    @Override
    public Iterator<R> iterator() {
        return List.of(new R(1)).iterator();
    }
}
