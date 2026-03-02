/*
 * @test /nodynamiccopyright/
 * @summary Ensure --enable-preview is required for NR1S assignment
 * @compile/fail/ref=AssignmentNR1SRequirePreview.out -XDrawDiagnostics AssignmentNR1SRequirePreview.java
 * @compile/ref=AssignmentNR1SRequirePreview.preview.out -XDrawDiagnostics -Xlint:preview --enable-preview --source ${jdk.version} AssignmentNR1SRequirePreview.java
 */
public class AssignmentNR1SRequirePreview {
    static sealed abstract class SA permits SB {}
    static final class SB extends SA {}

    void test() {
        SA sa = new SB();
        SB sb = sa;
    }
}
