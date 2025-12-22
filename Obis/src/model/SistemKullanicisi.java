package model;

// Basit sistem kullanıcısı (login için). Rol testi burada yapılır.
public class SistemKullanicisi extends Kullanici {

    public SistemKullanicisi(int id, String username, String password, String email, Rol rol) {
        super(id, username, password, email, rol);
    }

    // Şifre kontrolünü tek yerde tutmak için küçük yardımcı.
    public boolean sifreDogruMu(String girilenSifre) {
        return getPassword() != null && getPassword().equals(girilenSifre);
    }
}
