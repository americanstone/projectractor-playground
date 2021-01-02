package liteRxApiHandsOn.domain;

import junit.framework.TestCase;
import org.junit.Test;
import reactor.core.publisher.Flux;

public class Part03StepVerifierTest {
    Part03StepVerifier p3 = new Part03StepVerifier();

   @Test
   public void testExpect10Elements(){
       p3.expect10Elements(null);
   }

    @Test
    public void testExpect3600Elements() {
        p3.expect3600Elements(() -> Flux.just(1L));
    }
}