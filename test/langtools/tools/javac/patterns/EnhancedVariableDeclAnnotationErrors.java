/*
 * @test /nodynamiccopyright/
 * @summary Check annotation errors on declarations with a record pattern
 * @enablePreview
 * @compile/fail/ref=EnhancedVariableDeclAnnotationErrors.out -XDrawDiagnostics EnhancedVariableDeclAnnotationErrors.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public class EnhancedVariableDeclAnnotationErrors {
    @Target(ElementType.LOCAL_VARIABLE)
    @interface Local {}
    @Target(ElementType.FIELD)
    @interface Wrong {}

    record Box<T>(T value) {}

    void wrongTarget() {
        @Wrong Box<String>(var value) = new Box<>("wrong");
    }

    void duplicateAnnotation() {
        @Local @Local Box<String>(_) = new Box<>("duplicate");
    }

}
