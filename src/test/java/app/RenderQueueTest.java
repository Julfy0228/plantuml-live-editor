package app;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RenderQueueTest {
    @Test
    void coalescesPendingWorkAndSuppressesStaleResults() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BlockingQueue<Runnable> deliveries = new LinkedBlockingQueue<>();
        List<String> rendered = new CopyOnWriteArrayList<>();
        List<String> received = new CopyOnWriteArrayList<>();
        try (RenderQueue<String, String> queue = new RenderQueue<>(source -> {
            rendered.add(source);
            if (source.equals("old")) {
                started.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out");
                    }
                } catch (InterruptedException exception) {
                    throw new IllegalStateException(exception);
                }
            }
            return source;
        }, failure -> "failed", deliveries::add, (version, result) -> received.add(result))) {
            queue.submit(queue.invalidate(), "old");
            assertTrue(started.await(5, TimeUnit.SECONDS));
            queue.submit(queue.invalidate(), "middle");
            queue.submit(queue.invalidate(), "latest");
            release.countDown();
            Runnable first = deliveries.poll(5, TimeUnit.SECONDS);
            Runnable second = deliveries.poll(5, TimeUnit.SECONDS);
            assertNotNull(first);
            assertNotNull(second);
            first.run();
            second.run();
            assertEquals(List.of("old", "latest"), rendered);
            assertEquals(List.of("latest"), received);
        } finally {
            release.countDown();
        }
    }

    @Test
    void invalidationAndCloseDiscardAlreadyScheduledDelivery() throws Exception {
        BlockingQueue<Runnable> deliveries = new LinkedBlockingQueue<>();
        List<String> received = new CopyOnWriteArrayList<>();
        RenderQueue<String, String> queue = new RenderQueue<>(source -> source, failure -> "failed", deliveries::add,
                (version, result) -> received.add(result));
        queue.submit(queue.invalidate(), "result");
        Runnable delivery = deliveries.poll(5, TimeUnit.SECONDS);
        assertNotNull(delivery);
        queue.invalidate();
        delivery.run();
        assertTrue(received.isEmpty());
        queue.submit(queue.invalidate(), "closed");
        Runnable closedDelivery = deliveries.poll(5, TimeUnit.SECONDS);
        assertNotNull(closedDelivery);
        queue.close();
        closedDelivery.run();
        assertTrue(received.isEmpty());
    }
    @Test
    void recoversAfterUnexpectedRendererException() throws Exception {
        BlockingQueue<Runnable> deliveries = new LinkedBlockingQueue<>();
        List<String> received = new CopyOnWriteArrayList<>();
        try (RenderQueue<String, String> queue = new RenderQueue<>(source -> {
            if (source.equals("bad")) {
                throw new IllegalStateException("parser failure");
            }
            return source;
        }, failure -> "failed", deliveries::add, (version, result) -> received.add(result))) {
            queue.submit(queue.invalidate(), "bad");
            Runnable failed = deliveries.poll(5, TimeUnit.SECONDS);
            assertNotNull(failed);
            failed.run();
            queue.submit(queue.invalidate(), "good");
            Runnable recovered = deliveries.poll(5, TimeUnit.SECONDS);
            assertNotNull(recovered);
            recovered.run();
            assertEquals(List.of("failed", "good"), received);
        }
    }
}



