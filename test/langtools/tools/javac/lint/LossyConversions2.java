/*
 * @test /nodynamiccopyright/
 * @summary Test for inexact primitive widening conversions in assignments and method invocations
 * @compile/fail/ref=LossyConversions2.out -XDrawDiagnostics -Xmaxwarns 300 -Xlint:lossy-conversions -Werror LossyConversions2.java
 */

public class LossyConversions2 {
    public static final int INT2FLOAT_SAFE   = 0x10000000;
    public static final int INT2FLOAT_UNSAFE = 0x10000001;

    public static final long LONG2FLOAT_SAFE   = 0x10000000L;
    public static final long LONG2FLOAT_UNSAFE = 0x10000001L;

    public static final long LONG2DOUBLE_SAFE   = 0x1000000000000000L;
    public static final long LONG2DOUBLE_UNSAFE = 0x1000000000000001L;

    public void lossyAssignmentsPrimitiveConstants() {
        float f;
        double d;

        f = INT2FLOAT_SAFE;             // no warning
        f = INT2FLOAT_UNSAFE;
        f = Integer.MIN_VALUE;          // no warning
        f = Integer.MAX_VALUE;

        f = LONG2FLOAT_SAFE;            // no warning
        f = LONG2FLOAT_UNSAFE;
        f = Long.MIN_VALUE;             // no warning
        f = Long.MAX_VALUE;

        d = LONG2DOUBLE_SAFE;           // no warning
        d = LONG2DOUBLE_UNSAFE;
        d = Long.MIN_VALUE;             // no warning
        d = Long.MAX_VALUE;

        floatMethod(INT2FLOAT_SAFE);    // no warning
        floatMethod(INT2FLOAT_UNSAFE);
        floatMethod(Integer.MIN_VALUE); // no warning
        floatMethod(Integer.MAX_VALUE);

        floatMethod(LONG2FLOAT_SAFE);   // no warning
        floatMethod(LONG2FLOAT_UNSAFE);
        floatMethod(Long.MIN_VALUE);    // no warning
        floatMethod(Long.MAX_VALUE);

        doubleMethod(LONG2DOUBLE_SAFE); // no warning
        doubleMethod(LONG2DOUBLE_UNSAFE);
        doubleMethod(Long.MIN_VALUE);   // no warning
        doubleMethod(Long.MAX_VALUE);
    }

    public void lossyAssignmentsWithoutConstants() {
        float f;
        double d;
        int i1 = INT2FLOAT_SAFE;
        int i2 = INT2FLOAT_UNSAFE;
        long l1 = LONG2FLOAT_SAFE;
        long l2 = LONG2FLOAT_UNSAFE;
        long d1 = LONG2DOUBLE_SAFE;
        long d2 = LONG2DOUBLE_UNSAFE;
        // general warnings, no constant expressions
        // int to float
        f = i1;
        f = i2;
        // long to float
        f = l1;
        f = l2;
        // long to double
        d = d1;
        d = d2;
        // int to float
        floatMethod(i1);
        // long to float
        floatMethod(l1);
        // long to double
        doubleMethod(l1);
    }

    public void lossyAssignmentsWithoutFromReference() {
        float f;
        double d;
        Integer i1 = INT2FLOAT_SAFE;
        Integer i2 = INT2FLOAT_UNSAFE;
        Long l1 = LONG2FLOAT_SAFE;
        Long l2 = LONG2FLOAT_UNSAFE;
        Long d1 = LONG2DOUBLE_SAFE;
        Long d2 = LONG2DOUBLE_UNSAFE;
        // general warnings, no constant expressions
        // int to float
        f = i1;
        f = i2;
        // long to float
        f = l1;
        f = l2;
        // long to double
        d = d1;
        d = d2;
        // int to float
        floatMethod(i1);
        // long to float
        floatMethod(l1);
        // long to double
        doubleMethod(l1);
    }

    public static <T extends Integer> void wideningReferenceConversionUnboxingAndWideningPrimitive(T i) {
        float f = i;
    }
    public static <T extends Long> void wideningReferenceConversionUnboxingAndWideningPrimitive2(T i) {
        float f = i;
        double d = i;
    }

    public void floatMethod(float x) {
    }

    public void doubleMethod(double x) {
    }
}
