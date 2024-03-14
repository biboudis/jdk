/*
 * @test /nodynamiccopyright/
 * @summary javac erroneously allows widening conversion followed by boxing to Short in assignment context
 * @compile/fail/ref=T8328178.out -XDrawDiagnostics -XDshould-stop.at=FLOW T8328178.java
 */
public class T8328178 {

    void test() {
        Integer t = (byte) 0;       // error, no widening p.c. from byte to int and boxing
        Short s = (byte) 0;         // error, no widening p.c. from byte to short and boxing

        Byte B1 = (char)0;          // ok, narrowing p.c. from char to byte and boxing when target is Byte
        Byte B2 = (short)0;         // ok, narrowing p.c. from short to byte and boxing when target is Byte
        Character C1 = (short)0;    // ok, narrowing p.c. from short to char and boxing when target is Character
        Short S1 = (int)0;          // ok, narrowing p.c. from int to char and boxing to Short
    }

    void testWithSwitches() {
        Integer i = 42;
        var s1 = switch (i) {
            case (byte)0 -> true;   // error
            default -> false;
        };

        Short s = 42;
        var s2 = switch (s) {
            case (byte)0 -> true;   // error
            default -> false;
        };
    }
}
