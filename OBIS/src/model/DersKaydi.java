package model;

import java.time.LocalDateTime;

// Öğrenci - Ders - Öğretmen ilişkisini ve not/devamsızlık bilgisini tutar.
public class DersKaydi {
    private final int ogrenciId;
    private final String dersKodu;
    private final int ogretmenId;

    // Not girilmemiş olabilir.
    private Integer not;

    // Devamsızlık sayısı (girilmeyebilir).
    private Integer devamsizlik;

    private LocalDateTime guncellemeZamani;

    public DersKaydi(int ogrenciId, String dersKodu, int ogretmenId) {
        this.ogrenciId = ogrenciId;
        this.dersKodu = dersKodu;
        this.ogretmenId = ogretmenId;
    }

    public DersKaydi(int ogrenciId, String dersKodu, int ogretmenId, Integer not, Integer devamsizlik, LocalDateTime guncellemeZamani) {
        this.ogrenciId = ogrenciId;
        this.dersKodu = dersKodu;
        this.ogretmenId = ogretmenId;
        this.not = not;
        this.devamsizlik = devamsizlik;
        this.guncellemeZamani = guncellemeZamani;
    }

    public int getOgrenciId() {
        return ogrenciId;
    }

    public String getDersKodu() {
        return dersKodu;
    }

    public int getOgretmenId() {
        return ogretmenId;
    }

    public Integer getNot() {
        return not;
    }

    public void setNot(Integer not) {
        this.not = not;
        this.guncellemeZamani = LocalDateTime.now();
    }

    public Integer getDevamsizlik() {
        return devamsizlik;
    }

    public void setDevamsizlik(Integer devamsizlik) {
        this.devamsizlik = devamsizlik;
        this.guncellemeZamani = LocalDateTime.now();
    }

    public LocalDateTime getGuncellemeZamani() {
        return guncellemeZamani;
    }
}
