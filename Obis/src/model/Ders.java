package model;

// Ders bilgisini tutar.
public class Ders {
    private final String kod;
    private final String ad;

    public Ders(String kod, String ad) {
        this.kod = kod;
        this.ad = ad;
    }

    public String getKod() {
        return kod;
    }

    public String getAd() {
        return ad;
    }

    @Override
    public String toString() {
        return kod + " - " + ad;
    }
}
