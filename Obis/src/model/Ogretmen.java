package model;

import interfaces.Guncellenebilir;

// Öğretmen: Kisi'den türetilir ve güncellenebilir.
public class Ogretmen extends Kisi implements Guncellenebilir {
    private final String brans;

    public Ogretmen(int id, String ad, String soyad, String email, String brans) {
        super(id, ad, soyad, email);
        this.brans = brans;
    }

    public String getBrans() {
        return brans;
    }

    @Override
    public void emailGuncelle(String yeniEmail) {
        setEmail(yeniEmail);
    }

    @Override
    public String getRol() {
        return "Ogretmen";
    }

    @Override
    public String bilgiGoster() {
        return String.format("[%s] id=%d, %s %s, brans=%s, email=%s", getRol(), getId(), getAd(), getSoyad(), brans, getEmail());
    }
}
