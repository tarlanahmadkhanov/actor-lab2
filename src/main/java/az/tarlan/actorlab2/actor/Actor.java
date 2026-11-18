package az.tarlan.actorlab2.actor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class Actor<M> implements AutoCloseable {

    private final BlockingQueue<M> mailbox;
    private final Thread thread;

    protected Actor(String name, int mailboxCapacity) {
        this.mailbox = new ArrayBlockingQueue<>(mailboxCapacity);
        this.thread = Thread.ofVirtual().name(name).unstarted(this::loop);
    }

    public void start() {
        thread.start();
    }

    public void tell(M message) throws InterruptedException {
        mailbox.put(message);
    }

    protected abstract void onMessage(M message) throws Exception;

    private void loop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                M message = mailbox.take();
                try {
                    onMessage(message);
                } catch (Exception e) {
                    System.err.println("[" + thread.getName() + "] failed on " + message + ": " + e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws Exception {
        thread.interrupt();
    }
}
