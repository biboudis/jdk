/*
 * @test /nodynamiccopyright/
 * @summary Tighten exchaustive checks for constant expressions of primitive types as the selector of a switch
 * @enablePreview
 * @compile/fail/ref=PrimitivePatternsSwitchConstantSelector.out -XDrawDiagnostics -XDshould-stop.at=FLOW PrimitivePatternsSwitchConstantSelector.java
 */
public class PrimitivePatternsSwitchConstantSelector {
    void testConstExpressions() {

        // narrowing primitive conversion
        switch (42) {
            case byte _ :   // const int to byte, exhaustive
        }

        switch (42l) {
            case byte _ :   // const long to byte, exhaustive
        }

        switch (123456) {
            case byte _ :   // const int to byte, // error  (we have const which is *not* unconditionally exact)
        }

        switch (16_777_216) { // 2^24
            case float _ :  // const int to float, exhaustive
        }

        // inexact widening primitive conversion
        switch (16_777_217) { // 2^24 + 1
            case float _ :  // const int to float,// error  (we have const which is *not* unconditionally exact)
        }

        switch (42d) {
            case float _ :  // const double to float, exhaustive
        }

        // exact widening primitive conversion
        switch (1) {
            case long _ :  // const int to long, exhaustive
        }
    }
}
