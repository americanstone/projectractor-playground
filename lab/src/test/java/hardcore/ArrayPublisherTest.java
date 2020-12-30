package hardcore;


import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static java.util.concurrent.TimeUnit.SECONDS;


public class ArrayPublisherTest {
    static Long[] generate(long num){
        return LongStream.range(0, num >= Integer.MAX_VALUE ? 100000 : num)
                .boxed()
                .toArray(Long[]::new);
    }
    @Test
    public void subscribe() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<String> observedSignals = new ArrayList<>();

        ArrayPublisher<Long> arrayPublisher = new ArrayPublisher<>(generate(5));

        Subscriber<Long> mySub = new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription publisherCreatedSubscription) {
                observedSignals.add("onSubscribe()");
                // kick off the invocation
                publisherCreatedSubscription.request(10);
            }

            @Override
            public void onNext(Long o) {
                observedSignals.add("onNext("+ o + ")");
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {
                observedSignals.add("onComplete()");
                System.out.println("sub thread " + Thread.currentThread().getId());
                latch.countDown();
            }
        };

        arrayPublisher.subscribe(mySub);

        Assertions.assertThat( latch.await(1, TimeUnit.SECONDS))
                .isTrue();
       System.out.println("main thread " + Thread.currentThread().getId());


        Assertions.assertThat(observedSignals)
                .containsExactly(
                        "onSubscribe()",
                        "onNext(0)",
                        "onNext(1)",
                        "onNext(2)",
                        "onNext(3)",
                        "onNext(4)",
                        "onComplete()"
                );
    }

    @Test
    public void mustSupportBackpressureControl() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 5L;
        Long[] array = generate(toRequest);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);
        Subscription[] subscription = new Subscription[1];

        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscription[0] = s;
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        // everything is set up for now
        Assertions.assertThat(collected).isEmpty();

        // kick off the invocation by tell the publisher send one
        subscription[0].request(1);
        Assertions.assertThat(collected).containsExactly(0L);

        subscription[0].request(1);
        Assertions.assertThat(collected).containsExactly(0L, 1L);

        subscription[0].request(2);
        Assertions.assertThat(collected).containsExactly(0L, 1L, 2L, 3L);

        subscription[0].request(20);
        subscription[0].request(20);
        Assertions.assertThat(collected).containsExactly(0L, 1L, 2L, 3L, 4L);

        Assertions.assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();

        Assertions.assertThat(collected).containsExactly(array);
    }

    @org.testng.annotations.Test
    public void mustSendNPENormally() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Long[] array = new Long[] { null };
        AtomicReference<Throwable> error = new AtomicReference<>();
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(4);
            }

            @Override
            public void onNext(Long aLong) {
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
                latch.countDown();
            }

            @Override
            public void onComplete() {
            }
        });

        latch.await(1, SECONDS);

        Assertions.assertThat(error.get()).isInstanceOf(NullPointerException.class);
    }

    @org.testng.annotations.Test
    public void shouldNotDieInStackOverflow() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 10L;
        Long[] array = generate(toRequest);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);

        Subscriber<Long> sub = new Subscriber<Long>() {
            Subscription subscriptionPassToSubscriber;

            @Override
            public void onSubscribe(Subscription publisherSendSubscription) {
                this.subscriptionPassToSubscriber = publisherSendSubscription;
                subscriptionPassToSubscriber.request(1);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
                subscriptionPassToSubscriber.request(1);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

        };
        publisher.subscribe(sub);

        latch.await(5, SECONDS);

        Assertions.assertThat(collected).containsExactly(array);

        // the same subscriber still able to request the data from publisher via Subscription
        // but may in different thread. so the Subscription's request will have race condition problem

        sub.onNext(10L);

        Long[] newarray =  Arrays.copyOf(array, 11);
        newarray[10] = 10L;
        Assertions.assertThat(collected).containsExactly(newarray);

    }

    @org.testng.annotations.Test
    public void shouldBePossibleToCancelSubscription() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 1000L;
        Long[] array = generate(toRequest);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {

            @Override
            public void onSubscribe(Subscription s) {
                s.cancel();
                s.request(toRequest);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        Assertions.assertThat(latch.await(1, SECONDS)).isFalse();

        Assertions.assertThat(collected).isEmpty();
    }
}