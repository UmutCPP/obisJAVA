package model;

import interfaces.Goruntulenebilir;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

// Tüm öğrenci tiplerinin ortak sınıfı.
public class Ogrenci extends Kisi implements Goruntulenebilir {
    private final LocalDate dogumTarihi;

    // DersKodu -> Not (0-100)
    private final Map<String, Integer> notlar = new HashMap<>();

    public Ogrenci(int id, String ad, String soyad, String email, LocalDate dogumTarihi) {
        super(id, ad, soyad, email);
        this.dogumTarihi = dogumTarihi;
    }

    public LocalDate getDogumTarihi() {
        return dogumTarihi;
    }

    public Map<String, Integer> getNotlar() {
        return notlar;
    }

    // Overload örneği: not ekleme (ders kodu)
    public void notEkle(String dersKodu, int not) {
        notlar.put(dersKodu, not);
    }

    // Overload örneği: not ekleme (Ders nesnesi)
    public void notEkle(Ders ders, int not) {
        notEkle(ders.getKod(), not);
    }

    public void notGuncelle(String dersKodu, int yeniNot) {
        notlar.put(dersKodu, yeniNot);
    }

    @Override
    public String getRol() {
        return "Ogrenci";
    }

    @Override
    public String bilgiGoster() {
        return String.format("[%s] id=%d, %s %s, email=%s", getRol(), getId(), getAd(), getSoyad(), getEmail());
    }

    @Override
    public String goruntule() {
        return bilgiGoster();
    }
}
