package az.tarlan.actorlab2.actor;

import java.util.concurrent.CompletableFuture;

public class GreetingActor extends Actor<GreetingActor.Command> {

    public record Command(GreetingRequest request, CompletableFuture<GreetingResponse> replyTo) {}

    public GreetingActor() {
        super("greeting-actor", 100);
    }

    @Override
    protected void onMessage(Command cmd) throws Exception {
        try {
            GreetingRequest request = cmd.request();
            String greeting = request.locale().greeting();
            cmd.replyTo().complete(new GreetingResponse(request.messageId(), request.locale(), request.name(), greeting));
        } catch (Exception e) {
            cmd.replyTo.completeExceptionally(e);
        }
    }

    public CompletableFuture<GreetingResponse> ask(GreetingRequest request) throws InterruptedException {
        var future = new CompletableFuture<GreetingResponse>();
        tell(new Command(request, future));
        return future;
    }
}
