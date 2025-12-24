package app;

import exception.GecersizNotException;
import exception.YetkisizIslemException;
import generic.Repository;
import model.Ders;
import model.LisansOgrencisi;
import model.Ogrenci;
import model.Rol;
import model.SistemKullanicisi;
import model.YuksekLisansOgrencisi;
import service.OgrenciService;
import service.StandartNotHesaplamaService;
import util.DosyaIslemleri;

import java.io.IOException;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

// Menü tabanlı konsol uygulaması.
public class Main {

    // Sorun ayıklama için (varsayılan kapalı)
    private static final boolean DEBUG_DERS_PARSE = false;

    // Legacy (artık kullanılmayacak): kullanicilar.txt / ogrenci_kullanicilar.txt / kayitlar.txt

    // Yeni format dosyaları (ilişki zinciri)
    private static final String ADMIN_FILE = "idare.txt";
    private static final String TEACHERS_FILE = "ogretmenler.txt";
    private static final String STUDENTS_NEW_FILE = "ogrenciler_yeni.txt";

    // Sabit 10 ders havuzu (kodlar)
    private static final String[] DERS_HAVUZU = {
            "MAT101", "TUR101", "CMP203", "FIZ201", "INK101",
            "MAT201", "CMP201", "BIL103", "KIM101", "ENG101"
    };

    // ===== Kurallar =====
    // Devamsızlık > 3 ise dersten kalır
    private static final int DEVAMSIZLIK_LIMIT = 3;

    // Vize %40, Final %60
    private static final double VIZE_AGIRLIK = 0.40;
    private static final double FINAL_AGIRLIK = 0.60;

    // "Ortalaması 2'nin altında ise FF" için 100'lük ortalamayı 4'lük sisteme çeviriyoruz.
    // Basit (yaygın) dönüşüm: 90-100=4.0, 85-89=3.5, 80-84=3.0, 75-79=2.5,
    // 70-74=2.0, 65-69=1.5, 60-64=1.0, 50-59=0.5, 0-49=0.0
    private static double yuzluktenDortluge(double ort100) {
        if (ort100 >= 90) return 4.0;
        if (ort100 >= 85) return 3.5;
        if (ort100 >= 80) return 3.0;
        if (ort100 >= 75) return 2.5;
        if (ort100 >= 70) return 2.0;
        if (ort100 >= 65) return 1.5;
        if (ort100 >= 60) return 1.0;
        if (ort100 >= 50) return 0.5;
        return 0.0;
    }

    private static double dersOrtalama100(Integer vize, Integer fin) {
        if (vize == null || fin == null) return Double.NaN;
        return (vize * VIZE_AGIRLIK) + (fin * FINAL_AGIRLIK);
    }

    private static String harfNotuHesapla(double ort100) {
        // Harf notlarını 4'lük karşılığa göre türetiyoruz.
        // Ayrıca: 4'lük < 2.0 ise FF.
        double ort4 = yuzluktenDortluge(ort100);
        if (Double.isNaN(ort4)) return "-";
        if (ort4 < 2.0) return "FF";
        // 2.0 ve üzerini basitçe yaygın aralıklara bölelim.
        if (ort4 >= 3.5) return "AA";
        if (ort4 >= 3.0) return "BA";
        if (ort4 >= 2.5) return "BB";
        if (ort4 >= 2.0) return "CB";
        return "FF";
    }

    private static boolean devamsizliktenKaldi(Integer devamsizlik) {
        if (devamsizlik == null) return false;
        return devamsizlik > DEVAMSIZLIK_LIMIT;
    }

    // ===== Basit ANSI renkler (konsol) =====
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private static String red(String s) {
        return ANSI_RED + s + ANSI_RESET;
    }

    private static String green(String s) {
        return ANSI_GREEN + s + ANSI_RESET;
    }

    private static String yellow(String s) {
        return ANSI_YELLOW + s + ANSI_RESET;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || t.equals("-")) return null;
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Haftalık program üretimi için basit slot havuzu (gerçeğe yakın: hafta içi 09-16)
    private static final String[] PROGRAM_GUNLER = {"MON", "TUE", "WED", "THU", "FRI"};
    private static final int PROGRAM_SAAT_BASLA = 9;
    private static final int PROGRAM_SAAT_BITIS_DAHIL = 16;

    public static void main(String[] args) {
        // Yeni sistem dosyaları yoksa otomatik üret
        ensureSeedFiles();

        // Var olan öğrenciler dosyası eski formatta olabilir: v3 (vize/final/prog) kanonik hale getir.
        try {
            migrateStudentFileToV3IfNeeded();
        } catch (Exception e) {
            System.out.println("Migrate uyarısı: " + e.getMessage());
        }

        Repository<Ogrenci> repo = new Repository<>();
        OgrenciService ogrenciService = new OgrenciService(repo, new StandartNotHesaplamaService());

        // Örnek veri (uygulama ilk açıldığında çalışsın)
        seedData(ogrenciService);

        Scanner sc = new Scanner(System.in);

        // Login: menüden önce zorunlu
        SistemKullanicisi aktifKullanici = login(sc);
        if (aktifKullanici == null) {
            System.out.println("Giriş yapılamadı. Program sonlandırılıyor.");
            return;
        }

        boolean devam = true;

        while (devam) {
            menuYazdir(ogrenciService, aktifKullanici);
            System.out.print("Seçiminiz: ");
            String secim = sc.nextLine().trim();

            switch (secim) {
                case "1":
                    if (aktifKullanici.getRol() == Rol.ADMIN) {
                        ogrenciEkle(sc, ogrenciService);
                    } else {
                        System.out.println("Bu seçenek rolünüz için kapalı.");
                    }
                    break;
                case "2":
                    if (aktifKullanici.getRol() == Rol.OGRENCI) {
                        ogrenciPaneliGoruntule(aktifKullanici);
                    } else if (aktifKullanici.getRol() == Rol.OGRETMEN) {
                        ogretmenOgrenciListele(aktifKullanici);
                    } else {
                        ogrenciListele(ogrenciService);
                    }
                    break;
                case "3":
                    if (aktifKullanici.getRol() == Rol.OGRETMEN) {
                        ogretmenNotDevamsizlikGir(sc, aktifKullanici, true);
                    } else {
                        System.out.println("Bu seçenek rolünüz için kapalı.");
                    }
                    break;
                case "4":
                    if (aktifKullanici.getRol() == Rol.OGRETMEN) {
                        ogretmenNotDevamsizlikGir(sc, aktifKullanici, false);
                    } else {
                        System.out.println("Bu seçenek rolünüz için kapalı.");
                    }
                    break;
                case "5":
                    if (aktifKullanici.getRol() == Rol.OGRENCI) {
                        System.out.println("Bu seçenek rolünüz için kapalı.");
                    } else {
                        ortalamaHesapla(sc, ogrenciService);
                    }
                    break;
                case "6":
                    devam = false;
                    System.out.println("Çıkış yapıldı.");
                    break;
                default:
                    System.out.println("Geçersiz seçim.");
                    break;
            }
        }
    }

    private static void migrateStudentFileToV3IfNeeded() throws Exception {
        String teachersPath = resolveTeachersFilePath();
        String studentsPath = resolveStudentsNewFilePath();
        if (teachersPath == null || studentsPath == null) return;

        // Öğretmen programı: bu sürümde ogretmenler.txt'de program yok, ama seed üretiminde programlar mevcut.
        // Mevcut dosyalarda prog alanı boşsa en azından seed'de üretilen programı öğrenci kayıtlarına basmak istiyoruz.
        // Bu yüzden öğretmenler.txt ders listesi üzerinden deterministik bir program üretip migrate'e veriyoruz.
        java.util.Map<Integer, java.util.Map<String, java.util.List<String>>> ogretmenProgramlari = new java.util.HashMap<>();
        java.util.Map<Integer, java.util.List<String>> ogretmenDersleri = DosyaIslemleri.ogretmenDersleriOku(teachersPath);
        java.util.Random rnd = new java.util.Random(4242);
        for (var e : ogretmenDersleri.entrySet()) {
            int ogretmenId = e.getKey();
            java.util.Set<String> kullanilan = new java.util.HashSet<>();
            java.util.Map<String, java.util.List<String>> dersProg = new java.util.HashMap<>();
            for (String ders : e.getValue()) {
                // her ders 2 slot
                dersProg.put(ders, rastgeleSlotSec(rnd, kullanilan, 2));
            }
            ogretmenProgramlari.put(ogretmenId, dersProg);
        }

        boolean degisti = DosyaIslemleri.ogrencilerDosyasiMigrateV3(studentsPath, ogretmenProgramlari);
        if (degisti) {
            System.out.println(yellow("[INFO] ogrenciler_yeni.txt v3 formata güncellendi."));
        }
    }

    // Öğretmen için: sadece kendi verdiği dersleri alan öğrencileri listeler.
    private static void ogretmenOgrenciListele(SistemKullanicisi aktifKullanici) {
        try {
            if (aktifKullanici.getRol() != Rol.OGRETMEN) {
                throw new YetkisizIslemException("Bu işlem için yetkiniz yok");
            }

            String teachersPath = resolveTeachersFilePath();
            if (teachersPath == null) {
                System.out.println("Öğretmen dosyası bulunamadı: " + TEACHERS_FILE);
                return;
            }
            Map<Integer, List<String>> ogretmenDersleri = DosyaIslemleri.ogretmenDersleriOku(teachersPath);
            List<String> derslerim = ogretmenDersleri.get(aktifKullanici.getId());
            if (derslerim == null || derslerim.isEmpty()) {
                System.out.println("Bu öğretmene atanmış ders yok.");
                return;
            }

            String studentsPath = resolveStudentsNewFilePath();
            if (studentsPath == null) {
                System.out.println("Öğrenci dosyası bulunamadı: " + STUDENTS_NEW_FILE);
                return;
            }

            System.out.println("--- Öğrenci Listesi (Öğretmen Filtreli) ---");
            System.out.println("ÖğretmenId=" + aktifKullanici.getId() + " | Dersler=" + String.join(", ", derslerim));

            boolean herhangi = false;
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(studentsPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String t = line.trim();
                    if (t.isEmpty() || t.startsWith("#")) continue;

                    // aldigiDersler alanı ';' içerebilir, bu yüzden limit 7
                    String[] p = t.split(";", 7);
                    if (p.length < 6) continue;

                    int ogrId = Integer.parseInt(p[0].trim());
                    String username = p.length > 1 ? p[1].trim() : "";
                    String ad = p.length > 3 ? p[3].trim() : "";
                    String soyad = p.length > 4 ? p[4].trim() : "";
                    String email = p.length > 5 ? p[5].trim() : "";
                    String aldigi = p.length >= 7 ? p[6].trim() : "";

                    if (aldigi.isBlank()) continue;

                    // Bu öğrencinin, bu öğretmenden aldığı dersleri topla
                    List<DosyaIslemleri.EmbeddedDersKaydi> ilgiliKayitlar = new ArrayList<>();
                    String[] items = aldigi.split("\\|", -1);
                    for (String item : items) {
                        DosyaIslemleri.EmbeddedDersKaydi dk = DosyaIslemleri.embeddedDersKaydiParse(item);
                        if (dk == null) continue;
                        if (dk.ogretmenId != aktifKullanici.getId()) continue;
                        // Ek güvence: öğretmenin verdiği dersler listesinde olmalı
                        boolean dersUygun = false;
                        for (String d : derslerim) {
                            if (d != null && d.equalsIgnoreCase(dk.dersKodu)) {
                                dersUygun = true;
                                break;
                            }
                        }
                        if (!dersUygun) continue;
                        ilgiliKayitlar.add(dk);
                    }

                    if (ilgiliKayitlar.isEmpty()) continue;
                    herhangi = true;

                    System.out.println("ID=" + ogrId + " | " + ad + " " + soyad + " | user=" + username + " | email=" + email);
                    System.out.println("  Bu öğretmenden aldığı dersler:");
                    for (DosyaIslemleri.EmbeddedDersKaydi dk : ilgiliKayitlar) {
                        String notGoster = (dk.notDegeri == null || dk.notDegeri.equals("-")) ? "-" : dk.notDegeri;
                        String devGoster = (dk.devDegeri == null || dk.devDegeri.equals("-")) ? "-" : dk.devDegeri;
                        System.out.println("    - " + dk.dersKodu + " (ÖğretmenId=" + dk.ogretmenId + ") not=" + notGoster + ", dev=" + devGoster);
                    }
                }
            }

            if (!herhangi) {
                System.out.println("Bu öğretmenden ders alan öğrenci bulunamadı.");
            }

        } catch (YetkisizIslemException e) {
            System.out.println("İşlem hatası: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Listeleme hatası: " + e.getMessage());
        }
    }

    private static SistemKullanicisi login(Scanner sc) {
        try {
            // Kullanıcı tipi seçimi (yeni sistem: İdari / Öğretmen / Öğrenci)
            System.out.println("\n==== Giriş Türü ====");
            System.out.println("1) İdari (Admin)");
            System.out.println("2) Öğretmen");
            System.out.println("3) Öğrenci");
            System.out.print("Seçiminiz: ");
            String tip = sc.nextLine().trim();

            switch (tip) {
                case "1":
                    return adminLogin(sc);
                case "2":
                    return ogretmenLogin(sc);
                case "3":
                    return ogrenciLogin(sc);
                default:
                    System.out.println("Geçersiz giriş türü.");
                    return null;
            }
        } catch (IOException e) {
            System.out.println("Login dosyası okunamadı: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("Login hatası: " + e.getMessage());
            return null;
        }
    }

    private static SistemKullanicisi adminLogin(Scanner sc) throws IOException {
        String adminPath = resolveAdminFilePath();
        if (adminPath == null) {
            System.out.println("Admin dosyası bulunamadı: " + ADMIN_FILE);
            return null;
        }
        List<SistemKullanicisi> admins = DosyaIslemleri.idareKullanicilariOku(adminPath);
        if (admins.isEmpty()) {
            System.out.println("Admin dosyası boş: " + adminPath);
            return null;
        }

        for (int i = 1; i <= 3; i++) {
            System.out.println("\n==== Login (İdari) ====");
            System.out.print("Kullanıcı adı: ");
            String u = sc.nextLine().trim();
            System.out.print("Şifre: ");
            String p = sc.nextLine();

            for (SistemKullanicisi a : admins) {
                if (a.getUsername().equals(u) && a.sifreDogruMu(p)) {
                    System.out.println("Giriş başarılı. Rol: " + a.getRol());
                    return a;
                }
            }
            System.out.println("Hatalı kullanıcı adı/şifre. (Deneme " + i + "/3)");
        }
        return null;
    }

    private static SistemKullanicisi ogretmenLogin(Scanner sc) throws IOException {
        String teacherPath = resolveTeachersFilePath();
        if (teacherPath == null) {
            System.out.println("Öğretmen dosyası bulunamadı: " + TEACHERS_FILE);
            return null;
        }
        List<SistemKullanicisi> ogretmenler = DosyaIslemleri.ogretmenKullanicilariOku(teacherPath);
        if (ogretmenler.isEmpty()) {
            System.out.println("Öğretmen dosyası boş: " + teacherPath);
            return null;
        }

        for (int i = 1; i <= 3; i++) {
            System.out.println("\n==== Login (Öğretmen) ====");
            System.out.print("Kullanıcı adı: ");
            String u = sc.nextLine().trim();
            System.out.print("Şifre: ");
            String p = sc.nextLine();

            for (SistemKullanicisi k : ogretmenler) {
                if (k.getUsername().equals(u) && k.sifreDogruMu(p)) {
                    System.out.println("Giriş başarılı. Rol: " + k.getRol());
                    return k;
                }
            }
            System.out.println("Hatalı kullanıcı adı/şifre. (Deneme " + i + "/3)");
        }
        return null;
    }

    private static SistemKullanicisi ogrenciLogin(Scanner sc) throws IOException {
        String studentsPath = resolveStudentsNewFilePath();
        if (studentsPath == null) {
            System.out.println("Öğrenci dosyası bulunamadı: " + STUDENTS_NEW_FILE);
            return null;
        }

        List<SistemKullanicisi> ogrenciler = DosyaIslemleri.ogrenciKullanicilariOkuV2(studentsPath);
        if (ogrenciler.isEmpty()) {
            System.out.println("Öğrenci dosyası boş: " + studentsPath);
            return null;
        }

        for (int i = 1; i <= 3; i++) {
            System.out.println("\n==== Login (Öğrenci) ====");
            System.out.print("Kullanıcı adı: ");
            String u = sc.nextLine().trim();
            System.out.print("Şifre: ");
            String p = sc.nextLine();

            for (SistemKullanicisi o : ogrenciler) {
                if (o.getUsername().equals(u) && o.sifreDogruMu(p)) {
                    System.out.println("Giriş başarılı. Rol: " + o.getRol());
                    return o;
                }
            }
            System.out.println("Hatalı kullanıcı adı/şifre. (Deneme " + i + "/3)");
        }
        return null;
    }

    private static String resolveAdminFilePath() {
        File f1 = new File(ADMIN_FILE);
        if (f1.exists() && f1.isFile()) return f1.getPath();
        File f2 = new File("Obis" + File.separator + ADMIN_FILE);
        if (f2.exists() && f2.isFile()) return f2.getPath();
        return null;
    }

    private static String resolveTeachersFilePath() {
        File f1 = new File(TEACHERS_FILE);
        if (f1.exists() && f1.isFile()) return f1.getPath();
        File f2 = new File("Obis" + File.separator + TEACHERS_FILE);
        if (f2.exists() && f2.isFile()) return f2.getPath();
        return null;
    }

    private static String resolveStudentsNewFilePath() {
        File f1 = new File(STUDENTS_NEW_FILE);
        if (f1.exists() && f1.isFile()) return f1.getPath();
        File f2 = new File("Obis" + File.separator + STUDENTS_NEW_FILE);
        if (f2.exists() && f2.isFile()) return f2.getPath();
        return null;
    }

    private static void menuYazdir(OgrenciService ogrenciService, SistemKullanicisi aktifKullanici) {
        System.out.println();
        System.out.println("==== Öğrenci Bilgilendirme Sistemi ====");
        System.out.println("Aktif kullanıcı: " + aktifKullanici.getUsername() + " (" + aktifKullanici.getRol() + ")");
        System.out.println("Son kaydetme: " + ogrenciService.sonKaydetmeZamaniMetin());
        if (aktifKullanici.getRol() == Rol.ADMIN) {
            System.out.println("1. Öğrenci Ekle");
        }
        if (aktifKullanici.getRol() == Rol.OGRENCI) {
            System.out.println("2. Derslerim / Notlarım");
        } else {
            System.out.println("2. Öğrenci Listele");
        }
        if (aktifKullanici.getRol() == Rol.OGRETMEN) {
            System.out.println("3. Not Gir (Seçimli)");
            System.out.println("4. Devamsızlık Gir (Seçimli)");
        }
        if (aktifKullanici.getRol() != Rol.OGRENCI) {
            System.out.println("5. Ortalama Hesapla");
        }
        System.out.println("6. Çıkış");
    }

    private static void seedData(OgrenciService ogrenciService) {
        Ogrenci o1 = new LisansOgrencisi(1, "Ayse", "Yilmaz", "ayse@yuni.edu", LocalDate.of(2003, 5, 10), 2);
        Ogrenci o2 = new YuksekLisansOgrencisi(2, "Mehmet", "Demir", "mehmet@yuni.edu", LocalDate.of(1999, 11, 3), "Yapay Zeka");
        ogrenciService.ogrenciEkle(o1);
        ogrenciService.ogrenciEkle(o2);

        try {
            ogrenciService.notEkle("Ogretmen", 1, new Ders("MAT101", "Matematik"), 85);
            ogrenciService.notEkle("Ogretmen", 1, new Ders("FZK101", "Fizik"), 75);
            ogrenciService.notEkle("Ogretmen", 2, new Ders("BLM501", "Makine Öğrenmesi"), 92);
        } catch (Exception ignored) {
            // Örnek veride hata olmasını beklemiyoruz.
        }
    }

    // ===== Seed / Dosya Üretimi =====

    private static void ensureSeedFiles() {
        try {
            String adminPath = resolveAdminFilePath();
            String teacherPath = resolveTeachersFilePath();
            String studentPath = resolveStudentsNewFilePath();

            boolean adminVar = adminPath != null;
            boolean teacherVar = teacherPath != null;
            boolean studentVar = studentPath != null;

            if (adminVar && teacherVar && studentVar) {
                return; // hepsi var, dokunma
            }

            // hedef konumları belirle (dosyalar yoksa Obis/ altında üret)
            String baseDir = resolveBaseDataDir();
            if (!adminVar) adminPath = baseDir + File.separator + ADMIN_FILE;
            if (!teacherVar) teacherPath = baseDir + File.separator + TEACHERS_FILE;
            if (!studentVar) studentPath = baseDir + File.separator + STUDENTS_NEW_FILE;

            // 1) Admin dosyası
            if (!adminVar) {
                writeAdminFile(adminPath);
            }

            // 2) Öğretmen + ders atamaları
            SeedData seed = generateSeed(20, 30);
            if (!teacherVar) {
                writeTeacherFile(teacherPath, seed);
            }

            // 3) Öğrenciler (tek dosya: login + ders ilişkileri)
            if (!studentVar) {
                writeStudentFileV2(studentPath, seed);
            }

        } catch (Exception e) {
            // Seed başarısız olsa da uygulama açılabilsin.
            System.out.println("Seed oluşturma uyarısı: " + e.getMessage());
        }
    }

    private static String resolveBaseDataDir() {
        // Uygulama Obis/ içinden çalışıyorsa burada dosya üret.
        File obisDir = new File(".");
        // En güvenlisi: içinde src/ varsa burası Obis/ klasörüdür.
        if (new File(obisDir, "src").exists()) {
            return obisDir.getAbsolutePath();
        }
        // Üstten çalıştırılıyorsa Obis/ altına yaz.
        File f = new File("Obis");
        if (f.exists() && f.isDirectory()) {
            return f.getPath();
        }
        return obisDir.getAbsolutePath();
    }

    private static void writeAdminFile(String path) throws IOException {
        try (var bw = new java.io.BufferedWriter(new java.io.FileWriter(path))) {
            bw.write("# id;username;password;email;ad;soyad");
            bw.newLine();
            bw.write("1;admin;admin123;admin@yuni.edu;Admin;User");
            bw.newLine();
        }
    }

    private static void writeTeacherFile(String path, SeedData seed) throws IOException {
        try (var bw = new java.io.BufferedWriter(new java.io.FileWriter(path))) {
            bw.write("# id;username;password;email;ad;soyad;dersler");
            bw.newLine();
            bw.write("# dersler: MAT101|FIZ201|CMP203");
            bw.newLine();
            for (TeacherSeed t : seed.teachers) {
                bw.write(t.id + ";" + t.username + ";" + t.password + ";" + t.email + ";" + t.ad + ";" + t.soyad + ";" + String.join("|", t.dersler));
                bw.newLine();
            }
        }
    }

    private static void writeStudentFileV2(String path, SeedData seed) throws IOException {
        try (var bw = new java.io.BufferedWriter(new java.io.FileWriter(path))) {
            bw.write("# id;username;password;ad;soyad;email;aldigiDersler");
            bw.newLine();
            bw.write("# aldigiDersler: dersKodu:ogretmenId|dersKodu:ogretmenId");
            bw.newLine();
            for (StudentSeed s : seed.students) {
                bw.write(s.id + ";" + s.username + ";" + s.password + ";" + s.ad + ";" + s.soyad + ";" + s.email + ";" + s.aldigiDersler);
                bw.newLine();
            }
        }
    }

    private static SeedData generateSeed(int teacherCount, int studentCount) {
        Random rnd = new Random(42); // deterministik

        // öğretmenler: 2-4 ders
        List<TeacherSeed> teachers = new ArrayList<>();
        for (int i = 1; i <= teacherCount; i++) {
            TeacherSeed t = new TeacherSeed();
            t.id = i;
            t.username = "ogrt" + i;
            t.password = "ogrt" + i;
            t.email = "ogrt" + i + "@yuni.edu";
            t.ad = "Ogretmen" + i;
            t.soyad = "Soyad" + i;

            int dersSayisi = 2 + rnd.nextInt(3); // 2-4
            List<String> havuz = new ArrayList<>();
            Collections.addAll(havuz, DERS_HAVUZU);
            Collections.shuffle(havuz, rnd);
            t.dersler = new ArrayList<>(havuz.subList(0, dersSayisi));

            // Her öğretmenin kendi dersleri için çakışmasız haftalık program üret.
            // Basit yaklaşım: her ders 2 saat/hafta.
            t.dersProgramlari = new java.util.HashMap<>();
            java.util.Set<String> kullanilan = new java.util.HashSet<>();
            for (String dersKodu : t.dersler) {
                List<String> slotlar = rastgeleSlotSec(rnd, kullanilan, 2);
                t.dersProgramlari.put(dersKodu, slotlar);
            }
            teachers.add(t);
        }

        // ders -> uygun öğretmenler listesi
        java.util.Map<String, List<Integer>> dersOgretmenleri = new java.util.HashMap<>();
        for (String d : DERS_HAVUZU) {
            dersOgretmenleri.put(d, new ArrayList<>());
        }
        for (TeacherSeed t : teachers) {
            for (String d : t.dersler) {
                dersOgretmenleri.get(d).add(t.id);
            }
        }

    // Öğrenciler: 5 ders, her ders için o dersi veren öğretmenlerden 1 tane seç
        List<StudentSeed> students = new ArrayList<>();
        for (int i = 1; i <= studentCount; i++) {
            StudentSeed s = new StudentSeed();
            s.id = 1000 + i;
            s.username = "ogr" + i;
            s.password = "ogr" + i;
            s.ad = "Ogrenci" + i;
            s.soyad = "Soyad" + i;
            s.email = "ogr" + i + "@yuni.edu";

            List<String> havuz = new ArrayList<>();
            Collections.addAll(havuz, DERS_HAVUZU);
            Collections.shuffle(havuz, rnd);
            List<String> secilen = havuz.subList(0, 5);

            StringBuilder dersler = new StringBuilder();
            for (String d : secilen) {
                List<Integer> ogIds = dersOgretmenleri.get(d);
                if (ogIds == null || ogIds.isEmpty()) {
                    // bazı dersler hiçbir öğretmene düşebilir; o zaman rastgele öğretmen ata
                    int ogretmenId = 1 + rnd.nextInt(teacherCount);
                    String prog = programBul(teachers, ogretmenId, d);
                    // v3: vize/final/not/dev/prog alanları boş başlar
                    appendDers(dersler, d + ":" + ogretmenId + ":vize=-;final=-;not=-;dev=-;prog=" + prog);
                } else {
                    int ogretmenId = ogIds.get(rnd.nextInt(ogIds.size()));
                    String prog = programBul(teachers, ogretmenId, d);
                    appendDers(dersler, d + ":" + ogretmenId + ":vize=-;final=-;not=-;dev=-;prog=" + prog);
                }
            }
            s.aldigiDersler = dersler.toString();
            students.add(s);
        }

        SeedData seed = new SeedData();
        seed.teachers = teachers;
        seed.students = students;
        return seed;
    }

    private static void appendDers(StringBuilder sb, String part) {
        if (sb.length() > 0) sb.append('|');
        sb.append(part);
    }

    // Öğretmenin ders programını Embedded kayda yazmak için: "MON-09,MON-10" gibi
    private static String programBul(List<TeacherSeed> teachers, int ogretmenId, String dersKodu) {
        if (teachers == null) return "";
        for (TeacherSeed t : teachers) {
            if (t.id != ogretmenId) continue;
            if (t.dersProgramlari == null) return "";
            List<String> slots = t.dersProgramlari.get(dersKodu);
            if (slots == null || slots.isEmpty()) return "";
            return String.join(",", slots);
        }
        return "";
    }

    // Bir öğretmen için kullanılan slot setine çakışmayacak şekilde N slot seç.
    private static List<String> rastgeleSlotSec(Random rnd, java.util.Set<String> kullanilan, int adet) {
        List<String> secilen = new ArrayList<>();
        int maxDeneme = 200;
        for (int i = 0; i < adet; i++) {
            String slot = null;
            for (int d = 0; d < maxDeneme; d++) {
                String gun = PROGRAM_GUNLER[rnd.nextInt(PROGRAM_GUNLER.length)];
                int saat = PROGRAM_SAAT_BASLA + rnd.nextInt(PROGRAM_SAAT_BITIS_DAHIL - PROGRAM_SAAT_BASLA + 1);
                String cand = gun + "-" + String.format("%02d", saat);
                if (kullanilan.contains(cand)) continue;
                slot = cand;
                break;
            }
            if (slot == null) {
                // Çok düşük ihtimal: havuz dolu oldu. En azından tekrar etmesin diye kır.
                break;
            }
            kullanilan.add(slot);
            secilen.add(slot);
        }
        java.util.Collections.sort(secilen);
        return secilen;
    }

    private static class SeedData {
        List<TeacherSeed> teachers;
        List<StudentSeed> students;
    }

    private static class TeacherSeed {
        int id;
        String username;
        String password;
        String email;
        String ad;
        String soyad;
        List<String> dersler;
        // dersKodu -> slot listesi (MON-09 gibi)
        java.util.Map<String, List<String>> dersProgramlari;
    }

    private static class StudentSeed {
        int id;
        String username;
        String password;
        String ad;
        String soyad;
        String email;
        String aldigiDersler;
    }

    private static void ogrenciEkle(Scanner sc, OgrenciService service) {
        try {
            String studentsPath = resolveStudentsNewFilePath();
            if (studentsPath == null) {
                System.out.println("Öğrenci dosyası bulunamadı: " + STUDENTS_NEW_FILE);
                return;
            }
            int id = DosyaIslemleri.ogrenciNextIdV2(studentsPath);
            System.out.println("Öğrenci ID (otomatik): " + id);

            // Yeni öğrenci eklendiğinde seeded öğrencilerdeki gibi otomatik ders+öğretmen ataması yap.
            // Format kanonik olmalı: DERSKODU:OGRETMEN_ID:not=-;dev=-|...
            String dersAtamasi = "";
            try {
                String teachersPath = resolveTeachersFilePath();
                if (teachersPath != null) {
                    dersAtamasi = DosyaIslemleri.rastgeleDersAtamasiUretV2(teachersPath, 5);
                }
            } catch (Exception ignored) {
                // Ders atamasında sorun olursa öğrenci yine de eklenebilsin.
                dersAtamasi = "";
            }

            System.out.print("Kullanıcı adı (username): ");
            String username = sc.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username boş olamaz.");
                return;
            }

            System.out.print("Şifre: ");
            String password = sc.nextLine();
            if (password.isEmpty()) {
                System.out.println("Şifre boş olamaz.");
                return;
            }

            System.out.print("Ad: ");
            String ad = sc.nextLine().trim();

            System.out.print("Soyad: ");
            String soyad = sc.nextLine().trim();

            System.out.print("Email: ");
            String email = sc.nextLine().trim();
            if (!DosyaIslemleri.emailGecerliMi(email)) {
                System.out.println("Geçersiz e-mail formatı.");
                return;
            }

            System.out.print("Doğum Tarihi (yyyy-MM-dd): ");
            LocalDate dt = LocalDate.parse(sc.nextLine().trim());

            System.out.print("Tür (1=Lisans, 2=Yüksek Lisans): ");
            String tur = sc.nextLine().trim();

            Ogrenci ogr;
            if ("2".equals(tur)) {
                System.out.print("Tez Konusu: ");
                String tez = sc.nextLine().trim();
                ogr = new YuksekLisansOgrencisi(id, ad, soyad, email, dt, tez);
            } else {
                System.out.print("Sınıf (1-4): ");
                int sinif = Integer.parseInt(sc.nextLine().trim());
                ogr = new LisansOgrencisi(id, ad, soyad, email, dt, sinif);
            }

            service.ogrenciEkle(ogr);
            System.out.println("Öğrenci eklendi: " + ogr.goruntule());

            // Kalıcı yazım (3 txt kuralı): ogrenciler_yeni.txt
            // Yeni öğrenciye otomatik ders ataması (mümkünse) yapılır.
            DosyaIslemleri.ogrenciEkleV2(studentsPath, id, username, password, ad, soyad, email, dersAtamasi);
            System.out.println("Öğrenci dosyaya kaydedildi.");
        } catch (Exception e) {
            System.out.println("Öğrenci eklenemedi: " + e.getMessage());
        }
    }

    private static void ogrenciListele(OgrenciService service) {
        // Admin/öğretmen listesi: güncel veriyi ogrenciler_yeni.txt'den oku.
        try {
            String studentsPath = resolveStudentsNewFilePath();
            if (studentsPath == null) {
                System.out.println("Öğrenci dosyası bulunamadı: " + STUDENTS_NEW_FILE);
                return;
            }
            System.out.println(yellow("--- Öğrenci Listesi (Dosyadan) ---"));
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(studentsPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String t = line.trim();
                    if (t.isEmpty() || t.startsWith("#")) continue;

                    // aldigiDersler alanı ';' içerebilir, bu yüzden limit 7
                    String[] p = t.split(";", 7);
                    if (p.length < 6) continue;

                    int id = Integer.parseInt(p[0].trim());
                    String username = p.length > 1 ? p[1].trim() : "";
                    String ad = p.length > 3 ? p[3].trim() : "";
                    String soyad = p.length > 4 ? p[4].trim() : "";
                    String email = p.length > 5 ? p[5].trim() : "";
                    String aldigi = p.length >= 7 ? p[6].trim() : "";

                    System.out.println("ID=" + id + " | " + ad + " " + soyad + " | user=" + username + " | email=" + email);
                    if (aldigi.isBlank()) {
                        System.out.println("  Dersler: -");
                        continue;
                    }
                    String[] items = aldigi.split("\\|", -1);
                    System.out.println(yellow("  Dersler:"));
                    for (String item : items) {
                        DosyaIslemleri.EmbeddedDersKaydi dk = DosyaIslemleri.embeddedDersKaydiParse(item);
                        if (dk == null) continue;

                        Integer vize = parseIntOrNull(dk.vizeDegeri);
                        Integer fin = parseIntOrNull(dk.finalDegeri);
                        Integer dev = parseIntOrNull(dk.devDegeri);

                        double ort100 = dersOrtalama100(vize, fin);
                        String ortStr = Double.isNaN(ort100) ? "-" : String.format("%.1f", ort100);
                        String harf = Double.isNaN(ort100) ? "-" : harfNotuHesapla(ort100);

                        boolean kaldiNot = (!Double.isNaN(ort100) && "FF".equals(harf));
                        boolean kaldiDev = devamsizliktenKaldi(dev);
                        boolean kaldi = kaldiNot || kaldiDev;

                        String prog = (dk.programDegeri == null || dk.programDegeri.isBlank()) ? "-" : dk.programDegeri;
                        String devGoster = (dev == null) ? "-" : String.valueOf(dev);
                        if (dev != null) {
                            devGoster = (dev <= DEVAMSIZLIK_LIMIT) ? green(devGoster) : red(devGoster);
                        }

                        String durum = "GEÇTİ";
                        String aciklama = "";
                        if (kaldi) {
                            durum = red("KALDI");
                            if (kaldiDev) {
                                aciklama = " (" + red("gelmedi") + ")";
                            } else {
                                aciklama = " (" + red("FF") + ")";
                            }
                        }

                        System.out.println(
                                "    - " + dk.dersKodu
                                        + " | ÖğretmenId=" + dk.ogretmenId
                                        + " | Program=" + prog
                                        + " | Vize=" + (vize == null ? "-" : vize)
                                        + " | Final=" + (fin == null ? "-" : fin)
                                        + " | Ort=" + ortStr
                                        + " | Harf=" + harf
                                        + " | Dev=" + devGoster
                                        + " | Durum=" + durum + aciklama
                        );
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Listeleme hatası: " + e.getMessage());
        }
    }

    private static void notEkle(Scanner sc, OgrenciService service, SistemKullanicisi aktifKullanici) {
        System.out.println("Not ekleme" );
        try {
            // Minimal yetki kontrolü (istenen format)
            if (aktifKullanici.getRol() != Rol.OGRETMEN) {
                throw new YetkisizIslemException("Bu işlem için yetkiniz yok");
            }

            System.out.print("Öğrenci ID: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Ders Kodu: ");
            String dersKodu = sc.nextLine().trim();

            System.out.print("Not (0-100): ");
            int not = Integer.parseInt(sc.nextLine().trim());

            // Mevcut servis imzasını bozmuyoruz (string rol parametresi korunur).
            service.notEkle("Ogretmen", id, dersKodu, not);
            System.out.println("Not eklendi.");
        } catch (YetkisizIslemException | GecersizNotException e) {
            System.out.println("İşlem hatası: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Beklenmeyen hata: " + e.getMessage());
        }
    }

    private static void notGuncelle(Scanner sc, OgrenciService service, SistemKullanicisi aktifKullanici) {
        System.out.println("Not güncelleme" );
        try {
            // Minimal yetki kontrolü (istenen format)
            if (aktifKullanici.getRol() != Rol.OGRETMEN) {
                throw new YetkisizIslemException("Bu işlem için yetkiniz yok");
            }

            System.out.print("Öğrenci ID: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Ders Kodu: ");
            String dersKodu = sc.nextLine().trim();

            System.out.print("Yeni Not (0-100): ");
            int not = Integer.parseInt(sc.nextLine().trim());

            // Mevcut servis imzasını bozmuyoruz (string rol parametresi korunur).
            service.notGuncelle("Ogretmen", id, dersKodu, not);
            System.out.println("Not güncellendi.");
        } catch (YetkisizIslemException | GecersizNotException e) {
            System.out.println("İşlem hatası: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Beklenmeyen hata: " + e.getMessage());
        }
    }

    // ===== Yeni Akışlar =====

    private static void ogrenciPaneliGoruntule(SistemKullanicisi aktifKullanici) {
        try {
            String studentsPath = resolveStudentsNewFilePath();
            if (studentsPath == null) {
                System.out.println("Yeni öğrenci dosyası bulunamadı: " + STUDENTS_NEW_FILE);
                return;
            }

            // Dosyadaki sırayı korumak için: öğrencinin satırını bulup aldigiDersler kısmını sırayla parse ediyoruz.
            String ogrLine = null;
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(studentsPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String t = line.trim();
                    if (t.isEmpty() || t.startsWith("#")) continue;
                    String[] p = t.split(";", 7);
                    if (p.length < 7) continue;
                    int id = Integer.parseInt(p[0].trim());
                    if (id == aktifKullanici.getId()) {
                        ogrLine = t;
                        break;
                    }
                }
            }

            if (ogrLine == null) {
                System.out.println("Bu öğrenci için ders kaydı bulunamadı.");
                return;
            }

            // aldigiDersler alanı ';' içerebilir (not/dev ayracı), bu yüzden limit kullanıyoruz.
            String[] p = ogrLine.split(";", 7);
            String aldigi = p[6].trim();
            if (aldigi.isBlank()) {
                System.out.println("Bu öğrenci için ders kaydı bulunamadı.");
                return;
            }

            if (DEBUG_DERS_PARSE) {
                System.out.println("[DEBUG] aldigiDersler=" + aldigi);
            }

            System.out.println(yellow("--- Derslerim / Notlarım ---"));
            int i = 1;
            String[] items = aldigi.split("\\|", -1);
            if (DEBUG_DERS_PARSE) {
                System.out.println("[DEBUG] itemCount=" + items.length);
            }
            int atlanan = 0;
            for (String item : items) {
                if (DEBUG_DERS_PARSE) {
                    System.out.println("[DEBUG] item=" + item);
                }
                DosyaIslemleri.EmbeddedDersKaydi dk = DosyaIslemleri.embeddedDersKaydiParse(item);
                if (dk == null) {
                    atlanan++;
                    continue;
                }

                Integer vize = parseIntOrNull(dk.vizeDegeri);
                Integer fin = parseIntOrNull(dk.finalDegeri);
                Integer dev = parseIntOrNull(dk.devDegeri);

                double ort100 = dersOrtalama100(vize, fin);
                String ortStr = Double.isNaN(ort100) ? "-" : String.format("%.1f", ort100);
                String harf = Double.isNaN(ort100) ? "-" : harfNotuHesapla(ort100);

                boolean kaldiNot = (!Double.isNaN(ort100) && "FF".equals(harf));
                boolean kaldiDev = devamsizliktenKaldi(dev);
                boolean kaldi = kaldiNot || kaldiDev;

                String prog = (dk.programDegeri == null || dk.programDegeri.isBlank()) ? "-" : dk.programDegeri;
                String devGoster = (dev == null) ? "Girilmedi" : String.valueOf(dev);
                if (dev != null) {
                    devGoster = (dev <= DEVAMSIZLIK_LIMIT) ? green(devGoster) : red(devGoster);
                }

                String durum = kaldi ? red("KALDI") : "GEÇTİ";
                String aciklama = "";
                if (kaldi) {
                    if (kaldiDev) aciklama = " (" + red("gelmedi") + ")";
                    else aciklama = " (" + red("FF") + ")";
                }

                System.out.println(
                        i + ") " + dk.dersKodu
                                + " | ÖğretmenId=" + dk.ogretmenId
                                + " | Program=" + prog
                                + " | Vize=" + (vize == null ? "Girilmedi" : vize)
                                + " | Final=" + (fin == null ? "Girilmedi" : fin)
                                + " | Ort=" + ortStr
                                + " | Harf=" + harf
                                + " | Devamsızlık=" + devGoster
                                + " | Durum=" + durum + aciklama
                );
                i++;
            }
            if (i == 1) {
                System.out.println("Ders kaydı bulunamadı.");
            } else if (atlanan > 0) {
                System.out.println("(Uyarı: " + atlanan + " hatalı ders kaydı atlandı)");
            }
        } catch (Exception e) {
            System.out.println("Öğrenci paneli hatası: " + e.getMessage());
        }
    }

    // mode: true => Vize/Final girişi, false => Devamsızlık girişi
    private static void ogretmenNotDevamsizlikGir(Scanner sc, SistemKullanicisi aktifKullanici, boolean yeniKayitGibi) {
        try {
            if (aktifKullanici.getRol() != Rol.OGRETMEN) {
                throw new YetkisizIslemException("Bu işlem için yetkiniz yok");
            }

            String teachersPath = resolveTeachersFilePath();
            if (teachersPath == null) {
                System.out.println("Öğretmen dosyası bulunamadı: " + TEACHERS_FILE);
                return;
            }
            Map<Integer, List<String>> ogretmenDersleri = DosyaIslemleri.ogretmenDersleriOku(teachersPath);
            List<String> dersler = ogretmenDersleri.get(aktifKullanici.getId());
            if (dersler == null || dersler.isEmpty()) {
                System.out.println("Bu öğretmene atanmış ders yok.");
                return;
            }

            // Ders seçimi (listeden)
            String seciliDers = listedenSec(sc, "Ders seçin", dersler);
            if (seciliDers == null) return;
            if (!dersKoduGecerliMi(seciliDers)) {
                System.out.println("Uyarı: Ders havuzunda olmayan bir ders kodu seçildi: " + seciliDers);
            }

            // O dersi bu öğretmenden alan öğrenciler
            String studentsPath = resolveStudentsNewFilePath();
            if (studentsPath == null) {
                System.out.println("Yeni öğrenci dosyası bulunamadı: " + STUDENTS_NEW_FILE);
                return;
            }
            Map<Integer, Map<String, Integer>> ogrenciDersOgretmen = DosyaIslemleri.ogrenciDersOgretmenMapOkuV2(studentsPath);
            List<Integer> ogrenciler = new ArrayList<>();
            for (Map.Entry<Integer, Map<String, Integer>> e : ogrenciDersOgretmen.entrySet()) {
                Integer ogrId = e.getKey();
                Integer ogId = e.getValue().get(seciliDers);
                if (ogId != null && ogId == aktifKullanici.getId()) {
                    ogrenciler.add(ogrId);
                }
            }
            if (ogrenciler.isEmpty()) {
                System.out.println("Seçilen ders için bu öğretmenden ders alan öğrenci yok.");
                return;
            }

            Map<Integer, String> ogrBilgi = DosyaIslemleri.ogrenciBasitBilgiMapOkuV2(studentsPath);
            Integer seciliOgrId = listedenSecOgrenciDetay(sc, "Öğrenci seçin", ogrenciler, ogrBilgi);
            if (seciliOgrId == null) return;

            Integer vize = null;
            Integer fin = null;
            Integer dev = null;

            if (yeniKayitGibi) {
                // Vize + Final girişi
                System.out.print("Vize (0-100, boş=iptal): ");
                String vizeStr = sc.nextLine().trim();
                if (vizeStr.isEmpty()) {
                    System.out.println("Vize girilmedi. İşlem iptal.");
                    return;
                }
                vize = Integer.parseInt(vizeStr);
                if (vize < 0 || vize > 100) {
                    throw new GecersizNotException("Vize 0-100 aralığında olmalı");
                }

                System.out.print("Final (0-100, boş=iptal): ");
                String finStr = sc.nextLine().trim();
                if (finStr.isEmpty()) {
                    System.out.println("Final girilmedi. İşlem iptal.");
                    return;
                }
                fin = Integer.parseInt(finStr);
                if (fin < 0 || fin > 100) {
                    throw new GecersizNotException("Final 0-100 aralığında olmalı");
                }
            } else {
                // Devamsızlık girişi
                System.out.print("Devamsızlık (0+, boş=iptal): ");
                String devStr = sc.nextLine().trim();
                if (devStr.isEmpty()) {
                    System.out.println("Devamsızlık girilmedi. İşlem iptal.");
                    return;
                }
                dev = Integer.parseInt(devStr);
                if (dev < 0) {
                    System.out.println("Devamsızlık negatif olamaz. İşlem iptal.");
                    return;
                }
            }

            // Yeni sadeleştirilmiş sistem: ayrı kayitlar.txt yok. (Not/devamsızlık dosyası kaldırıldı.)
            // Şimdilik bu ekranda sadece ders/öğrenci seçimini ve girilen değerleri doğruluyoruz.
            // Buraya gelindiyse ilgili mod alanı dolu.
            // Yetki: öğretmen sadece kendi ID'si ile eşleşen ders kaydını güncelleyebilir.
            // Buradaki öğrenci listesi zaten filtrelenmiş olsa da, zorunlu kural gereği tekrar doğruluyoruz.
            Map<Integer, Map<String, DosyaIslemleri.EmbeddedDersKaydi>> kayitlar = DosyaIslemleri.ogrenciDersKayitlariOkuV2(studentsPath);
            Map<String, DosyaIslemleri.EmbeddedDersKaydi> ogrKayitlari = kayitlar.get(seciliOgrId);
            DosyaIslemleri.EmbeddedDersKaydi hedef = (ogrKayitlari == null) ? null : ogrKayitlari.get(seciliDers);
            if (hedef == null || hedef.ogretmenId != aktifKullanici.getId()) {
                throw new YetkisizIslemException("Bu ders için yetkiniz yok");
            }

            DosyaIslemleri.ogrenciDersKaydiGuncelleV2(
                    studentsPath,
                    seciliOgrId,
                    seciliDers,
                    aktifKullanici.getId(),
                    // legacy not alanını da dolduralım (vize/finalden türetilmiş 100'lük ortalama)
                    (vize != null && fin != null) ? (int) Math.round(dersOrtalama100(vize, fin)) : null,
                    dev
            );

            // vize/final/prog gibi yeni alanları kaydetmek için aynı update fonksiyonunu genişletmedik
            // (şimdilik). Bunun yerine satırı kanonik yazan DosyaIslemleri, embedded parse/toString'de
            // vize/final alanlarını da taşıyor. Bu yüzden vize/final güncellemesi için ayrı bir metot çağırıyoruz.
            // Not: dosya tek kaynak olduğu için iyileştirmeyi util'e taşıyacağız.
            // Şimdilik hızlı çözüm: vize/final güncellemesini tekrar utility ile yap.
            try {
                DosyaIslemleri.ogrenciDersKaydiGuncelleV3_VizeFinal(
                        studentsPath,
                        seciliOgrId,
                        seciliDers,
                        aktifKullanici.getId(),
                        vize,
                        fin
                );
            } catch (Exception ignored) {
                // vize/final yazılamadıysa bile legacy not alanı tutulduğu için sistem bozulmasın.
            }
            if (yeniKayitGibi) {
                System.out.println("Not kaydedildi: " + seciliDers + ", öğrenciId=" + seciliOgrId + ", vize=" + vize + ", final=" + fin);
            } else {
                System.out.println("Devamsızlık kaydedildi: " + seciliDers + ", öğrenciId=" + seciliOgrId + ", devamsızlık=" + dev);
            }

        } catch (YetkisizIslemException | GecersizNotException e) {
            System.out.println("İşlem hatası: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Sayı formatı hatası: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Beklenmeyen hata: " + e.getMessage());
        }
    }

    private static String listedenSec(Scanner sc, String baslik, List<String> items) {
        System.out.println("--- " + baslik + " ---");
        for (int i = 0; i < items.size(); i++) {
            System.out.println((i + 1) + ") " + items.get(i));
        }
        System.out.print("Seçim (1-" + items.size() + ", boş=iptal): ");
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return null;
        int idx = Integer.parseInt(s);
        if (idx < 1 || idx > items.size()) {
            System.out.println("Geçersiz seçim.");
            return null;
        }
        return items.get(idx - 1);
    }

    private static Integer listedenSecInt(Scanner sc, String baslik, List<Integer> items) {
        System.out.println("--- " + baslik + " ---");
        for (int i = 0; i < items.size(); i++) {
            System.out.println((i + 1) + ") " + items.get(i));
        }
        System.out.print("Seçim (1-" + items.size() + ", boş=iptal): ");
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return null;
        int idx = Integer.parseInt(s);
        if (idx < 1 || idx > items.size()) {
            System.out.println("Geçersiz seçim.");
            return null;
        }
        return items.get(idx - 1);
    }

    private static Integer listedenSecOgrenciDetay(Scanner sc, String baslik, List<Integer> ogrenciIdleri, Map<Integer, String> ogrBilgi) {
        System.out.println("--- " + baslik + " ---");
        for (int i = 0; i < ogrenciIdleri.size(); i++) {
            Integer id = ogrenciIdleri.get(i);
            String info = (ogrBilgi == null) ? null : ogrBilgi.get(id);
            if (info == null) info = "(bilgi yok)";
            System.out.println((i + 1) + ") " + id + " | " + info);
        }
        System.out.print("Seçim (1-" + ogrenciIdleri.size() + ", boş=iptal): ");
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return null;
        int idx = Integer.parseInt(s);
        if (idx < 1 || idx > ogrenciIdleri.size()) {
            System.out.println("Geçersiz seçim.");
            return null;
        }
        return ogrenciIdleri.get(idx - 1);
    }

    private static boolean dersKoduGecerliMi(String dersKodu) {
        for (String d : DERS_HAVUZU) {
            if (d.equalsIgnoreCase(dersKodu)) return true;
        }
        return false;
    }

    private static void ortalamaHesapla(Scanner sc, OgrenciService service) {
        try {
            System.out.print("Öğrenci ID: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            double ort = service.ortalamaHesapla(id);
            String harf = service.harfNotu(id);
            System.out.printf("Ortalama: %.2f, Harf Notu: %s%n", ort, harf);
        } catch (GecersizNotException e) {
            System.out.println("Not hatası: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    // Legacy dosya kaydet/oku kaldırıldı (3 txt ile sade sistem).
}
