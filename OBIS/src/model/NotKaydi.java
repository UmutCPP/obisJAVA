package model;

import java.time.LocalDateTime;

// Bir ders için not kaydını ve işlem zamanını tutar.
public class NotKaydi {
    private final String dersKodu;
    private int not;
    private LocalDateTime guncellemeZamani;

    public NotKaydi(String dersKodu, int not, LocalDateTime guncellemeZamani) {
        this.dersKodu = dersKodu;
        this.not = not;
        this.guncellemeZamani = guncellemeZamani;
    }

    public String getDersKodu() {
        return dersKodu;
    }

    public int getNot() {
        return not;
    }

    public void setNot(int not) {
        this.not = not;
        this.guncellemeZamani = LocalDateTime.now();
    }

    public LocalDateTime getGuncellemeZamani() {
        return guncellemeZamani;
    }
}
