package az.tarlan.actorlab2.actor;

public enum Locale {
    EN("Hello"), RU("Привет"), AZ("Salam");

    private final String greeting;

    Locale(String greeting) {
        this.greeting = greeting;
    }

    public String greeting() {
        return greeting;
    }
}
