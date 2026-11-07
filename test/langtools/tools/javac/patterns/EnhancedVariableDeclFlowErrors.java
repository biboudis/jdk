/*
 * @test /nodynamiccopyright/
 * @bug 8372170
 * @summary Check definite assignment and capture errors in declarations with a record pattern
 * @enablePreview
 * @compile/fail/ref=EnhancedVariableDeclFlowErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW EnhancedVariableDeclFlowErrors.java
 */

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class EnhancedVariableDeclFlowErrors {
    record Box<T>(T value) {}

    void unassignedInitializer() {
        int source;
        Box(var value) = new Box<>(source);
    }

    void conditionallyAssignedInitializer(boolean first) {
        int source;
        Box(var value) = first ? new Box<>(source = 1) : new Box<>(0);
        int use = source;
    }

    void possiblyEmptyLoop(List<Box<Integer>> rows) {
        int assigned;
        for (Box(var value) : rows) { assigned = value; }
        int use = assigned;
    }

    void finalAssignmentInLoop(List<Box<Integer>> rows) {
        final int assigned;
        for (Box(var value) : rows) { assigned = value; }
    }

    void mutatedStatementBinding() {
        Box(var value) = new Box<>(1);
        Supplier<Integer> capture = () -> value;
        value++;
    }

    void mutatedLoopBinding(List<Box<Integer>> rows) {
        for (Box(var value) : rows) {
            Supplier<Integer> capture = () -> value;
            value++;
        }
    }

    void uncaughtStatementException() {
        Box(var value) = checkedBox();
    }

    void uncaughtLoopException() {
        for (Box(var value) : checkedRows()) {}
    }

    static Box<Integer> checkedBox() throws IOException {
        return new Box<>(1);
    }

    static Iterable<Box<Integer>> checkedRows() throws IOException {
        return List.of(new Box<>(1));
    }
}
