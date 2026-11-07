/*
 * @test /nodynamiccopyright/
 * @summary Verify errors for enhanced variable declarations in enhanced for loops
 * @enablePreview
 * @compile/fail/ref=EnhancedVariableDeclInEnhancedForErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW EnhancedVariableDeclInEnhancedForErrors.java
 */
import java.util.List;

public class EnhancedVariableDeclInEnhancedForErrors {

    static void incompatibleObjectElements(List<Object> points) {
        for (Point(var x, var y): points) {
            System.out.println();
        }
    }

    static void incompatibleRawElements(List points) {
        for (Point(var x, var y): points) {
            System.out.println();
        }
    }

    static void uncoveredComponentsFromRecordElements(List<OPoint> opoints) {
        for (OPoint(String s, String t) : opoints) {
            System.out.println(s);
        }
    }

    static void incompatibleWildcardElements(List<?> f) {
        for (Rec(var x): f){
        }
    }

    static void uncoveredComponentsFromCapturedElements(List<? extends OPoint> opoints) {
        for (OPoint(String x, String y) : opoints) {
        }
    }

    static <T extends OPoint> void uncoveredComponentsFromTypeVariableElements(List<T> opoints) {
        for (OPoint(String x, String y) : opoints) {
        }
    }

    static <T extends OPoint & Marker> void uncoveredComponentsFromIntersectionBoundElements(List<T> opoints) {
        for (OPoint(String x, String y) : opoints) {
        }
    }

    static void incompatibleSealedElements(List<IPoint> points) {
        for (Point(var x, var y): points) {
        }
    }

    static void incompatibleCapturedSealedElements(List<? extends IPoint> points) {
        for (Point(var x, var y): points) {
        }
    }

    static void incompatibleOrdinaryLoopElements(List<Object> points) {
        for (Interface p: points) {
            System.out.println(p);
        }
    }

    static <T extends IPoint> void incompatibleTypeVariableSealedElements(List<T> points) {
        for (Point(var x, var y): points) {
        }
    }

    record Rec(String x) { }
    interface Interface {}
    interface Marker { }
    sealed interface IPoint permits Point {}
    record Point(Integer x, Integer y) implements IPoint { }
    record OPoint(Object x, Object y) implements Marker { }
}
