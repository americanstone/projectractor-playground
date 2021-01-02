package hardcore;


import org.assertj.core.api.Assertions;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.concurrent.TimeUnit.SECONDS;


public class ArrayPublisherThreadSafeTest {
    static Long[] generate(long num){
        return LongStream.range(0, num >= Integer.MAX_VALUE ? 100000 : num)
                .boxed()
                .toArray(Long[]::new);
    }
    @Test
    public void multithreadingTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        final int n = 5000;
        Long[] array = generate(n);
        ArrayPublisherThreadSafe<Long> publisher = new ArrayPublisherThreadSafe<>(array);

        publisher.subscribe(new Subscriber<Long>() {
            private Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                for (int i = 0; i < n; i++) {
                    commonPool().execute(() -> s.request(1));
                }
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

        latch.await(2, SECONDS);

        Assertions.assertThat(collected).containsExactly(array);
    }

}