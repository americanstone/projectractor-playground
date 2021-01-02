package hardcore;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
/*
    implement
     https://www.reactive-streams.org/
    https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.3/README.md#specification
    https://www.youtube.com/watch?v=OdSZ6mOQDcY
    https://codeburst.io/reactive-streams-are-so-simple-4105378f4c59

 */
public class ArrayPublisherNotThreadSafe<T> implements Publisher<T> {
    private final T[] array;
    public ArrayPublisherNotThreadSafe(T[] array){
        this.array = array;
    }
    @Override
    public void subscribe(Subscriber mySub) {
        // when subscriber subscribe the publisher
        // publisher create/give a Subscription back to subscriber
        // this subscription will have race problem
        // the goal is implementing Subscription in thread safe manner

        Subscription subscription  = getNotThreadSafeSubscription2(mySub);
        // if mySub call request method in multi-threads will have problem
        mySub.onSubscribe(subscription);

    }

    public Subscription getNotThreadSafeSubscription(Subscriber mySub){

        Subscription notThreadSafeSubscription = new Subscription() {
            int index = 0;
            boolean cancelled;
            /*
            specification
            implementations of request must be reentrant, to avoid stack overflows
            in the case of mutual recursion between request and onNext
             */
            @Override
            public void request(long n) {
                if(cancelled || n < 0){
                    return;
                }
                // "i" defines how many times the noNext will be called
                /// "index" point to current element
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
        return notThreadSafeSubscription;
    }

    public Subscription getNotThreadSafeSubscription2(Subscriber mySub){

        Subscription notThreadSafeSubscription = new Subscription() {
            int index = 0;
            boolean cancelled;
            long requested;
            /*
            specification
            implementations of request must be reentrant, to avoid stack overflows
            in the case of mutual recursion between request and onNext
             */
            @Override
            public void request(long n) {
                if(cancelled || n < 0){
                    return;
                }
                long initRequested = requested;

                requested += n;
                // working in progress order to reentrant exist because the onNext will call request
                // recursively
                if(initRequested != 0){
                    return;
                }
                // "send" defines how many times the noNext will be called
                /// "index" point to current element
                int send = 0;
                for (; send < requested && index < array.length; send++, index++) {
                    T element = array[index];
                    if(element == null){
                        mySub.onError(new NullPointerException());
                        return;
                    }
                    //reentrant
                    mySub.onNext(element);
                }

                if(n == array.length){
                    mySub.onComplete();
                }
                requested -= send;
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        };
        return notThreadSafeSubscription;
    }
}
