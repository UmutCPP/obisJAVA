package exception;

// Yetkisiz rolün işlem denemesinde fırlatılır.
public class YetkisizIslemException extends Exception {
    public YetkisizIslemException(String message) {
        super(message);
    }
}
