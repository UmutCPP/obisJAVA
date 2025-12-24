package model;

// Login olabilen sistem kullanıcıları için temel (abstract) sınıf.
// Not: Mevcut Kisi/Ogrenci/Ogretmen yapısını bozmamak için ayrı tutulur.
public abstract class Kullanici {
    private final int id;
    private final String username;
    private final String password;
    private final String email;
    private final Rol rol;

    protected Kullanici(int id, String username, String password, String email, Rol rol) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.rol = rol;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public Rol getRol() {
        return rol;
    }
}
