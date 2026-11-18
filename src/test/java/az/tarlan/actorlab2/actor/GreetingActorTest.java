package az.tarlan.actorlab2.actor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
