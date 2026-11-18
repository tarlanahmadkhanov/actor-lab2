package az.tarlan.actorlab2.actor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreetingActorTest {

    private static final String MESSAGE_ID = "05a79a10-6a1a-4cff-b157-d6e488c7fe76";

    @Test
    void greetsInEnglish() throws Exception {
        try (var actor = new GreetingActor()) {
            actor.start();

            var response = actor
                    .ask(new GreetingRequest(MESSAGE_ID, Locale.EN, "Tarlan"))
                    .get(1, TimeUnit.SECONDS);   // таймаут, чтобы тест не завис, если актор не ответит

            assertEquals(new GreetingResponse(MESSAGE_ID, Locale.EN, "Tarlan", "Hello"), response);
        }
    }

    @Test
    void failedMessageDoesNotKillActor() throws Exception {
        try (var actor = new GreetingActor()) {
            actor.start();

            var bad = actor.ask(new GreetingRequest(MESSAGE_ID, null, "Tarlan"));   // locale == null -> NPE в акторе
            var ex = assertThrows(ExecutionException.class, () -> bad.get(1, TimeUnit.SECONDS));
            assertInstanceOf(NullPointerException.class, ex.getCause());

            var good = actor
                    .ask(new GreetingRequest(MESSAGE_ID, Locale.RU, "Tarlan"))
                    .get(1, TimeUnit.SECONDS);
            assertEquals("Привет", good.greeting());
        }
    }

    @Test
    void processesMessagesInOrderOfSending() throws Exception {
        int total = 1000;
        var processed = new ArrayList<Integer>();    // пишет только поток актора, синхронизация не нужна
        var allDone = new CountDownLatch(total);

        try (var actor = new Actor<Integer>("recorder", 100) {
            @Override
            protected void onMessage(Integer n) {
                processed.add(n);
                allDone.countDown();
            }
        }) {
            actor.start();
            for (int i = 0; i < total; i++) {
                actor.tell(i);
            }
            assertTrue(allDone.await(5, TimeUnit.SECONDS));
        }

        // latch.await() даёт happens-before: все add() актора видны здесь
        assertEquals(IntStream.range(0, total).boxed().toList(), processed);
    }

    @Test
    void fullMailboxBlocksSender() throws Exception {
        var firstMessageTaken = new CountDownLatch(1);
        var release = new CountDownLatch(1);

        try (var actor = new Actor<Integer>("slow", 2) {
            @Override
            protected void onMessage(Integer n) throws Exception {
                firstMessageTaken.countDown();
                release.await();                     // притворяемся медленным обработчиком
            }
        }) {
            actor.start();

            actor.tell(1);                           // актор забрал его и застрял внутри onMessage
            assertTrue(firstMessageTaken.await(1, TimeUnit.SECONDS));
            actor.tell(2);                           // в ящике 1 из 2
            actor.tell(3);                           // в ящике 2 из 2, он полон

            var fourth = CompletableFuture.runAsync(() -> {
                try {
                    actor.tell(4);                   // должен заблокироваться
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            assertThrows(TimeoutException.class, () -> fourth.get(300, TimeUnit.MILLISECONDS));

            release.countDown();                     // актор освобождается и разбирает ящик
            fourth.get(1, TimeUnit.SECONDS);         // теперь tell(4) проходит
        }
    }
}
