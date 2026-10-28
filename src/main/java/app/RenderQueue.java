package app;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class RenderQueue<Input, Output> implements AutoCloseable {
    private record Request<Input>(long revision, Input input) {}
    private final ExecutorService worker;
    private final Executor delivery;
    private final Function<Input, Output> renderer;
    private final Function<Throwable, Output> onFailure;
    private final BiConsumer<Long, Output> receiver;
    private Request<Input> pending;
    private long revision;
    private boolean running;
    private boolean closed;

    public RenderQueue(Function<Input, Output> renderer, Function<Throwable, Output> onFailure,
            Executor delivery, BiConsumer<Long, Output> receiver) {
        this.renderer = renderer;
        this.onFailure = onFailure;
        this.delivery = delivery;
        this.receiver = receiver;
        worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "plantuml-render");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized long invalidate() {
        pending = null;
        return ++revision;
    }

    public synchronized void submit(long version, Input input) {
        if (closed || version != revision) {
            return;
        }
        pending = new Request<>(version, input);
        if (!running) {
            running = true;
            worker.execute(this::drain);
        }
    }

    private void drain() {
        while (true) {
            Request<Input> request;
            synchronized (this) {
                request = pending;
                pending = null;
                if (closed || request == null) {
                    running = false;
                    return;
                }
            }
            Output completed;
            try {
                completed = renderer.apply(request.input());
            } catch (RuntimeException | LinkageError failure) {
                completed = onFailure.apply(failure);
            }
            Output output = completed;
            delivery.execute(() -> {
                synchronized (RenderQueue.this) {
                    if (closed || request.revision() != revision) {
                        return;
                    }
                }
                receiver.accept(request.revision(), output);
            });
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        pending = null;
        ++revision;
        worker.shutdownNow();
    }
}


