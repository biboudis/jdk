/*
 * @test /nodynamiccopyright/
 * @summary
 * @enablePreview
 * @compile/fail/ref=EnhancedVariableDeclStatementErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW EnhancedVariableDeclStatementErrors.java
 */

public class EnhancedVariableDeclStatementErrors {

    static void exhaustivity_error1(Object point) {
        Point(var x, var y) = point;
    }

    static void exhaustivity_error2(OPoint opoint) {
        Point(var x, var y) = opoint;
    }

    static void expression_form_error(Point point) {
        Point point2;
        Point p3 = (point2 = point);                 // allowed as assignment op treated as an expression statement
        Point p4 = (Point p2 = point);               // not allowed as LVDS
        Point p5 = (Point(int x, int y) = point);    // not allowed as ELVDS
    }

    static int scope_error(Point point) {
        {
            Point(var sx, var sy) = point;
        }
        return sx;
    }

    static void shadowing_error(Point point) {
        int sx = 0;
        Point(var sx, var sy) = point;
    }

    sealed interface IPoint permits Point {}
    record Point(Integer x, Integer y) implements IPoint { }
    record OPoint(Object x, Object y) { }
}
