package service;

import exception.GecersizNotException;

import java.util.Collection;

// Not hesaplamaları için abstract servis.
public abstract class NotHesaplamaService {

    public abstract double ortalamaHesapla(Collection<Integer> notlar) throws GecersizNotException;

    public abstract String harfNotuHesapla(double ortalama);

    // Concrete metot: not aralığını kontrol eder.
    public boolean notGecerliMi(int not) {
        return not >= 0 && not <= 100;
    }
}
