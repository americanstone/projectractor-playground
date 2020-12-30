package hardcore;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
/*
https://www.youtube.com/watch?v=OdSZ6mOQDcY
 */
public class ArrayPublisher<T> implements Publisher<T> {
    private final T[] array;
    public ArrayPublisher(T[] array){
        this.array = array;
    }
    @Override
    public void subscribe(Subscriber mySub) {

        Subscription subscriptionPassToSubscriber = new Subscription() {
            int index = 0;
            boolean cancelled;
            @Override
            public void request(long n) {
                if(cancelled || n < 0){
                    return;
                }
                // i defines how many times the noNext will be called
                /// index point to current element
                for (int i = 0; i < n && index < array.length; i++) {
                    T element = array[index];
                    index++;
                    if(element == null){
                       mySub.onError(new NullPointerException());
                       return;
                    }
                    mySub.onNext(element);
                }

                if(n == array.length){
                    mySub.onComplete();
                }
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        };

        mySub.onSubscribe(subscriptionPassToSubscriber);


    }
}
