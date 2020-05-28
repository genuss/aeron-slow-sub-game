package org.mysun;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.collections.MutableLong;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.console.ContinueBarrier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Subscriber implements Runnable
{
    private final Subscription subscription;
    private final AtomicBoolean isRunning;
    private final Settings.Speed mySpeed;

    public Subscriber(Settings settings, Subscription subscription, AtomicBoolean isRunning)
    {
        this.subscription = subscription;
        this.isRunning = isRunning;

        mySpeed = settings.getSubscribersSpeed().get(settings.getCurrentNumber());
        System.out.println("I'm " + mySpeed + " subscriber #" + settings.getCurrentNumber());
    }

    @Override
    public void run()
    {
        final SleepingMillisIdleStrategy idleStrategy = new SleepingMillisIdleStrategy(100);

        final MutableLong time = new MutableLong(-1);
        final FragmentHandler fragmentHandler = (buf, offset, length, header) -> time.set(buf.getLong(offset));

        while (isRunning.get())
        {
            time.set(-1);
            while (time.get() == -1)
            {
                subscription.poll(fragmentHandler, 16);
            }
//            System.out.println("time " + time.get());
            if (mySpeed == Settings.Speed.SLOW)
            {
                idleStrategy.idle();
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        final Settings settings = Settings.load();
        final int currentNumber = settings.decrementCurrentNumber();
        if (currentNumber < 0)
        {
            System.err.println("No more subscribers!");
            System.exit(1);
        }
        settings.save();

        final AtomicBoolean isRunning = new AtomicBoolean(true);
        SigInt.register(() -> isRunning.set(false));

        System.out.println("Connect to the Aeron...");
        try (
            final Aeron aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(Settings.AERON_DIRECTORY_NAME));
            final Subscription subscription = aeron.addSubscription(settings.getChannel(), settings.getStreamId());)
        {
            final CompletableFuture<Void> subscriberTask = CompletableFuture.runAsync(
                new Subscriber(settings, subscription, isRunning)
            );

            System.out.println("Connected!");

            ContinueBarrier barrier = new ContinueBarrier("exit?");
            barrier.await();
            isRunning.set(false);
            subscriberTask.get();
        }
    }
}
