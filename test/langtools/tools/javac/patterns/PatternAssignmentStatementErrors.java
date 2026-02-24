/*
 * @test /nodynamiccopyright/
 * @summary
 * @enablePreview
 * @compile/fail/ref=PatternAssignmentStatementErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW PatternAssignmentStatementErrors.java
 */

public class PatternAssignmentStatementErrors {

    static void exhaustivity_error1(Object point) {
        Point(var x, var y) = point;
    }

    static void exhaustivity_error2(OPoint opoint) {
        Point(var x, var y) = opoint;
    }

    static void parsing_error(Object point) {
        Point p = point;
    }

    sealed interface IPoint permits Point {}
    record Point(Integer x, Integer y) implements IPoint { }
    record OPoint(Object x, Object y) { }
}