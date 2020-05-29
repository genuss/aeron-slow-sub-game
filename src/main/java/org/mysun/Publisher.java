package org.mysun;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.console.ContinueBarrier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mysun.Settings.AERON_DIRECTORY_NAME;

public class Publisher implements Runnable
{
    private final Publication publication;
    private final AtomicBoolean isRunning;

    public Publisher(Publication publication, AtomicBoolean isRunning)
    {
        this.publication = publication;
        this.isRunning = isRunning;
    }

    @Override
    public void run()
    {
            final IdleStrategy idleStrategy = new SleepingMillisIdleStrategy(1000);
            while (!publication.isConnected() && isRunning.get())
            {
                idleStrategy.idle();
//                System.out.println("Not connected...");
            }


            final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
            long offer = 0;
            while (isRunning.get())
            {
                buffer.putLong(0, System.nanoTime());
                offer = publication.offer(buffer);
                if (offer == Publication.NOT_CONNECTED)
                {
                    break;
                }
//                System.out.println(offer);
//                idleStrategy.idle();
            }

    }

    public static void main(String[] args) throws Exception
    {
        final AtomicBoolean isRunning = new AtomicBoolean(true);
        SigInt.register(() -> isRunning.set(false));

        final Settings settings = Settings.generateSettings();

        try (
            final MediaDriver ignored = MediaDriver.launch(
                new MediaDriver.Context()
                    .aeronDirectoryName(AERON_DIRECTORY_NAME)
                    .dirDeleteOnShutdown(true)
                    .dirDeleteOnStart(true));
            final Aeron aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(AERON_DIRECTORY_NAME));
            final Publication publication = aeron.addPublication(settings.getChannel(), settings.getStreamId());)
        {
            settings.save();

            final CompletableFuture<Void> publisherTask = CompletableFuture.runAsync(
                new Publisher(publication, isRunning)
            );

            ContinueBarrier barrier = new ContinueBarrier("exit?");
            barrier.await();
            isRunning.set(false);
            publisherTask.get();
        }

    }
}




