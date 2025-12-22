package service;

import exception.GecersizNotException;

import java.util.Collection;

// Abstract sınıfın somut implementasyonu.
public class StandartNotHesaplamaService extends NotHesaplamaService {

    @Override
    public double ortalamaHesapla(Collection<Integer> notlar) throws GecersizNotException {
        if (notlar == null || notlar.isEmpty()) {
            throw new GecersizNotException("Hesaplama için en az 1 not olmalı.");
        }
        int toplam = 0;
        int sayac = 0;
        for (int n : notlar) {
            if (!notGecerliMi(n)) {
                throw new GecersizNotException("Geçersiz not görüldü: " + n);
            }
            toplam += n;
            sayac++;
        }
        return (double) toplam / sayac;
    }

    @Override
    public String harfNotuHesapla(double ortalama) {
        if (ortalama >= 90) return "AA";
        if (ortalama >= 85) return "BA";
        if (ortalama >= 80) return "BB";
        if (ortalama >= 75) return "CB";
        if (ortalama >= 70) return "CC";
        if (ortalama >= 65) return "DC";
        if (ortalama >= 60) return "DD";
        if (ortalama >= 50) return "FD";
        return "FF";
    }
}
