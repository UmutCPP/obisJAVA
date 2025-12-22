package service;

import exception.GecersizNotException;
import exception.YetkisizIslemException;
import generic.Repository;
import model.Ders;
import model.Ogrenci;
import util.TarihUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Öğrenci işlemlerini yöneten servis sınıfı.
public class OgrenciService {
    private final Repository<Ogrenci> ogrenciRepo;
    private final NotHesaplamaService notService;

    // Örnek: en son kaydetme zamanı
    private LocalDateTime sonKaydetmeZamani;

    public OgrenciService(Repository<Ogrenci> ogrenciRepo, NotHesaplamaService notService) {
        this.ogrenciRepo = ogrenciRepo;
        this.notService = notService;
    }

    public void ogrenciEkle(Ogrenci ogrenci) {
        ogrenciRepo.save(ogrenci.getId(), ogrenci);
    }

    public List<Ogrenci> ogrenciListele() {
        return new ArrayList<>(ogrenciRepo.findAll());
    }

    public Ogrenci ogrenciBul(int id) {
        return ogrenciRepo.findById(id);
    }

    // Sadece Ogretmen rolü not girsin (custom exception kullanımı).
    public void notEkle(String rol, int ogrenciId, String dersKodu, int not) throws YetkisizIslemException, GecersizNotException {
        if (!"Ogretmen".equalsIgnoreCase(rol)) {
            throw new YetkisizIslemException("Bu işlem için 'Ogretmen' rolü gerekli.");
        }
        if (!notService.notGecerliMi(not)) {
            throw new GecersizNotException("Not 0-100 aralığında olmalı: " + not);
        }

        Ogrenci ogrenci = ogrenciRepo.findById(ogrenciId);
        if (ogrenci == null) {
            throw new IllegalArgumentException("Öğrenci bulunamadı. id=" + ogrenciId);
        }

        ogrenci.notEkle(dersKodu, not);
    }

    public void notEkle(String rol, int ogrenciId, Ders ders, int not) throws YetkisizIslemException, GecersizNotException {
        notEkle(rol, ogrenciId, ders.getKod(), not);
    }

    public void notGuncelle(String rol, int ogrenciId, String dersKodu, int yeniNot) throws YetkisizIslemException, GecersizNotException {
        notEkle(rol, ogrenciId, dersKodu, yeniNot);
    }

    public double ortalamaHesapla(int ogrenciId) throws GecersizNotException {
        Ogrenci ogrenci = ogrenciRepo.findById(ogrenciId);
        if (ogrenci == null) {
            throw new IllegalArgumentException("Öğrenci bulunamadı. id=" + ogrenciId);
        }
        return notService.ortalamaHesapla(ogrenci.getNotlar().values());
    }

    public String harfNotu(int ogrenciId) throws GecersizNotException {
        double ort = ortalamaHesapla(ogrenciId);
        return notService.harfNotuHesapla(ort);
    }

    public void sonKaydetmeZamaniGuncelle(LocalDateTime time) {
        this.sonKaydetmeZamani = time;
    }

    public String sonKaydetmeZamaniMetin() {
        return sonKaydetmeZamani == null ? "-" : TarihUtil.format(sonKaydetmeZamani);
    }
}
