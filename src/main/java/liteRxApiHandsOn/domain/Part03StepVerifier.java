package liteRxApiHandsOn.domain;

import java.time.Duration;
import java.util.function.*;

import org.assertj.core.api.Assertions;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;


/**
 * Learn how to use StepVerifier to test Mono, Flux or any other kind of Reactive Streams Publisher.
 *
 * @author Sebastien Deleuze
 * @see <a href="https://projectreactor.io/docs/test/release/api/reactor/test/StepVerifier.html">StepVerifier Javadoc</a>
 */
public class Part03StepVerifier {

//========================================================================================

    // TODO Use StepVerifier to check that the flux parameter emits "foo" and "bar" elements then completes successfully.
    void expectFooBarComplete(Flux<String> flux) {
        StepVerifier.create(Flux.just("foo", "bar"))
                .expectNext("foo", "bar")
                .verifyComplete();
    }

//========================================================================================

    // TODO Use StepVerifier to check that the flux parameter emits "foo" and "bar" elements then a RuntimeException error.
    void expectFooBarError(Flux<String> flux) {
       StepVerifier.create(Flux.just("foo","bar").error(new RuntimeException()))
               .expectNext("foo", "bar")
               .expectError(RuntimeException.class)
               .verify();
    }

//========================================================================================

    // TODO Use StepVerifier to check that the flux parameter emits a User with "swhite"username
    // and another one with "jpinkman" then completes successfully.
    void expectSkylerJesseComplete(Flux<User> flux) {
        StepVerifier.create(Flux.just(User.SKYLER, User.JESSE))
                .expectNextMatches(user -> user.getUsername().equals("swhite"))
                .assertNext(u -> Assertions.assertThat(u.getFirstname()).isEqualTo("Jesse"))
                .verifyComplete();
    }

//========================================================================================

    // TODO Expect 10 elements then complete and notice how long the test takes.
    void expect10Elements(Flux<Long> flux) {
        StepVerifier.create(Flux.interval(Duration.ofSeconds(1)).take(10))
                .expectNext(0L,1L,2L,3L,4L,5L,6L,7L,8L,9L)
                .verifyComplete();
    }

//========================================================================================

    // TODO Expect 3600 elements at intervals of 1 second, and verify quicker than 3600s
    // by manipulating virtual time thanks to StepVerifier#withVirtualTime, notice how long the test takes
    void expect3600Elements(Supplier<Flux<Long>> supplier) {
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofSeconds(1)).take(3600))
                .thenAwait(Duration.ofSeconds(3600))
                .expectNextCount(3600)
                .expectComplete()
                .verify();
    }

    private void fail() {
        throw new AssertionError("workshop not implemented");
    }

}
