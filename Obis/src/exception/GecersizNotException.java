package exception;

// Not aralığı geçersiz olduğunda fırlatılır.
public class GecersizNotException extends Exception {
    public GecersizNotException(String message) {
        super(message);
    }
}
