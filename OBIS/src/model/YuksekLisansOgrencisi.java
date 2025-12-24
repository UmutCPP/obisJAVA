package model;

import java.time.LocalDate;

// Yüksek lisans öğrencisi: Ogrenci'den türetilir.
public class YuksekLisansOgrencisi extends Ogrenci {
    private final String tezKonusu;

    public YuksekLisansOgrencisi(int id, String ad, String soyad, String email, LocalDate dogumTarihi, String tezKonusu) {
        super(id, ad, soyad, email, dogumTarihi);
        this.tezKonusu = tezKonusu;
    }

    public String getTezKonusu() {
        return tezKonusu;
    }

    @Override
    public String getRol() {
        return "YuksekLisansOgrencisi";
    }

    // Polimorfizm: override
    @Override
    public String bilgiGoster() {
        return super.bilgiGoster() + String.format(", tezKonusu=%s", tezKonusu);
    }
}
