package model;

import java.time.LocalDate;

// Lisans öğrencisi: Ogrenci'den türetilir.
public class LisansOgrencisi extends Ogrenci {
    private final int sinif;

    public LisansOgrencisi(int id, String ad, String soyad, String email, LocalDate dogumTarihi, int sinif) {
        super(id, ad, soyad, email, dogumTarihi);
        this.sinif = sinif;
    }

    public int getSinif() {
        return sinif;
    }

    @Override
    public String getRol() {
        return "LisansOgrencisi";
    }

    // Polimorfizm: override
    @Override
    public String bilgiGoster() {
        return super.bilgiGoster() + String.format(", sinif=%d", sinif);
    }
}
