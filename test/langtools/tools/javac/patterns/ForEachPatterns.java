/*
 * @test /nodynamiccopyright/
 * @summary
 * @compile --enable-preview -source ${jdk.version} ForEachPatterns.java
 * @run main/othervm --enable-preview ForEachPatterns
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ForEachPatterns {
    public static void main(String[] args) {

        List<Point>   in                  = List.of(new Point(1, 2), new Point(2, 3));
        List<IPoint>  in_iface            = List.of(new Point(1, 2), new Point(2, 3));
        List          inRaw               = List.of(new Point(1, 2), new Point(2, 3), new Frog(3, 4));
        List<PointEx> inWithPointEx       = List.of(new PointEx(1, 2));
        byte[]        inBytes             = { (byte) 127, (byte) 127 };
        List<Point>   inWithNullComponent = List.of(new Point(1, null), new Point(2, 3));
        List<Point>   inWithNull          = new ArrayList<>();
        Point[]       inArray             = in.toArray(Point[]::new);
        List<WithPrimitives>   inWithPrimitives
                                          = List.of(new WithPrimitives(1, 2),
                                                    new WithPrimitives(2, 3));

        inWithNull.add(new Point(2, 3));
        inWithNull.add(null);

        assertEquals(8, iteratorEnhancedFor(in));
        assertEquals(8, arrayEnhancedFor(inArray));
        assertEquals(8, simpleDecostructionPatternWithAccesses(in));
        assertEx(ForEachPatterns::simpleDecostructionPatternWithAccesses, null, NullPointerException.class);
        assertMatchExceptionWithNested(ForEachPatterns::simpleDecostructionPatternWithAccesses, inWithNull, NullPointerException.class);
        assertEx(ForEachPatterns::simpleDecostructionPatternWithAccesses, inWithNullComponent, NullPointerException.class);
        assertMatchExceptionWithNested(ForEachPatterns::simpleDecostructionPatternException, inWithPointEx, TestPatternFailed.class);
        assertEx(ForEachPatterns::simpleDecostructionPatternWithAccesses, (List<Point>) inRaw, ClassCastException.class);
        assertEquals(2, simpleDecostructionPatternNoComponentAccess(in));
        assertMatchExceptionWithNested(ForEachPatterns::simpleDecostructionPatternNoComponentAccess, inWithNull, NullPointerException.class);
        assertEquals(2, simpleDecostructionPatternNoComponentAccess(inWithNullComponent));
        assertEquals(8, varAndConcrete(in));
        assertEquals(3, returnFromEnhancedFor(in));
        assertEquals(0, breakFromEnhancedFor(in));
        assertEquals(254, primitiveWidening(inBytes));
        assertEquals(8, sealedRecordPassBaseType(in_iface));
        assertEquals(8, withPrimitives(inWithPrimitives));
        assertEquals(List.of(Color.RED), test_jep_example());
    }

    static int iteratorEnhancedFor(List<Point> points) {
        int result = 0;
        for (Point(Integer a, Integer b) : points) {
            result += a + b;
        }
        return result;
    }

    static int arrayEnhancedFor(Point[] points) {
        int result = 0;
        for (Point(Integer a, Integer b) : points) {
            result += a + b;
        }
        return result;
    }

    static int simpleDecostructionPatternWithAccesses(List<Point> points) {
        int result = 0;
        for (Point(var a, var b): points) {
            result += a + b;
        }
        return result;
    }

    static int simpleDecostructionPatternException(List<PointEx> points) {
        int result = 0;
        for (PointEx(var a, var b): points) {
            result += a + b;
        }
        return result;
    }

    static int simpleDecostructionPatternNoComponentAccess(List<Point> points) {
        int result = 0;
        for (Point(var a, var b): points) {
            result += 1;
        }
        return result;
    }

    static int varAndConcrete(List<Point> points) {
        int result = 0;
        for (Point(Integer a, var b): points) {
            result += a + b;
        }
        return result;
    }

    static int returnFromEnhancedFor(List<Point> points) {
        for (Point(var a, var b): points) {
            return a + b;
        }
        return -1;
    }

    static int breakFromEnhancedFor(List<Point> points) {
        int i = 1;
        int result = 0;
        for (Point(var a, var b): points) {
            if (i == 1) break;
            else result += a + b;
        }
        return result;
    }

    static int sealedRecordPassBaseType(List<IPoint> points) {
        int result = 0;

        for(Point(var x, var y) : points) {
            result += (x + y);
        }

        return result;
    }

    static int withPrimitives(List<WithPrimitives> points) {
        int result = 0;
        for (WithPrimitives(int a, double b): points) {
            result += a + (int) b;
        }
        return result;
    }

    // Simpler pos tests with local variable declarations
    // Should pass now and in the future if local variable
    // declaration is subsumed by patterns (not just record patterns)
    static int primitiveWidening(byte[] inBytes) {
        int acc = 0;
        for (int i: inBytes) {
            acc += i;
        }
        return acc;
    }

    static int applicability1(List<Point> points) {
        for (IPoint p: points) {
            System.out.println(p);
        }
        return -1;
    }

    static int applicability2(List<Object> points) {
        for (Object p: points) {
            System.out.println(p);
        }
        return -1;
    }

    static <T> void method() {}

    static void for_parsing(int i) {
        List<Point>                 points = null;
        List<GPoint<Integer>>       generic_points = null;
        List<GPoint<Point>>         generic_points_nested = null;
        List<GPoint<VoidPoint>>     generic_vpoints_nested = null;

        for (Point(Integer a, Integer b) : points) { }
        for (ForEachPatterns.Point(Integer a, Integer b) : points) { }
        for (GPoint<Integer>(Integer a, Integer b) : generic_points) { }
        for (@Annot(field = "test") Point p : points) {}
        for (method(); i == 0;) { i++; }
        for (method(), method(); i == 0;) { i++; }
        for (ForEachPatterns.<Integer>method(); i == 0;) { i++; }
        for (GPoint<Point>(Point(Integer a, Integer b), Point c) : generic_points_nested) { }
        for (GPoint<Point>(Point(var a, Integer b), Point c) : generic_points_nested) { }
        for (GPoint<VoidPoint>(VoidPoint(), VoidPoint()) : generic_vpoints_nested) { }
    }

    static List<Color> test_jep_example() {
        Rectangle rect = new Rectangle(
                new ColoredPoint(new Point(1,2), Color.RED),
                new ColoredPoint(new Point(3,4), Color.GREEN)
        );
        Rectangle[] rArr = {rect};
        return printUpperLeftColors(rArr);
    }

    enum Color { RED, GREEN, BLUE }
    record ColoredPoint(Point p, Color c) {}
    record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}

    static List<Color> printUpperLeftColors(Rectangle[] r) {
        List<Color> ret = new ArrayList<>();
        for (Rectangle(ColoredPoint(Point p, Color c), ColoredPoint lr): r) {
            ret.add(c);
        }
        return ret;
    }

    static void fail(String message) {
        throw new AssertionError(message);
    }

    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + "," +
                    "got: " + actual);
        }
    }

    static <T> void assertMatchExceptionWithNested(Function<List<T>, Integer> f, List<T> points, Class<?> nestedExceptionClass) {
        try {
            f.apply(points);
            fail("Expected an exception, but none happened!");
        }
        catch(Exception ex) {
            assertEquals(MatchException.class, ex.getClass());

            MatchException me = (MatchException) ex;

            assertEquals(nestedExceptionClass, me.getCause().getClass());
        }
    }

    static <T> void assertEx(Function<List<T>, Integer> f, List<T> points, Class<?> exceptionClass) {
        try {
            f.apply(points);
            fail("Expected an exception, but none happened!");
        }
        catch(Exception ex) {
            assertEquals(exceptionClass, ex.getClass());
        }
    }

    sealed interface IPoint permits Point {}
    record Point(Integer x, Integer y) implements IPoint { }
    record GPoint<T>(T x, T y) { }
    record VoidPoint() { }

    @interface Annot {
        String field();
    }
    record Frog(Integer x, Integer y) { }
    record PointEx(Integer x, Integer y) {
        @Override
        public Integer x() {
            throw new TestPatternFailed(EXCEPTION_MESSAGE);
        }
    }
    record WithPrimitives(int x, double y) { }
    static final String EXCEPTION_MESSAGE = "exception-message";
    public static class TestPatternFailed extends AssertionError {
        public TestPatternFailed(String message) {
            super(message);
        }
    }
}
