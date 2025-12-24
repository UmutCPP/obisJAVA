package util;

import model.Rol;
import model.SistemKullanicisi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Basit dosya okuma/yazma işlemleri (CSV benzeri).
public class DosyaIslemleri {

    // Basit e-mail doğrulama (sıkı değil ama hatalıları yakalar).
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static boolean emailGecerliMi(String email) {
        if (email == null) return false;
        String e = email.trim();
        if (e.isEmpty()) return false;
        return EMAIL_PATTERN.matcher(e).matches();
    }

    // ogrenciler_yeni.txt içindeki maxId + 1 (boşsa 1 döner)
    public static int ogrenciNextIdV2(String dosyaYolu) throws IOException {
        int max = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                String[] p = t.split(";", 2);
                if (p.length < 1) continue;
                try {
                    int id = Integer.parseInt(p[0].trim());
                    if (id > max) max = id;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return max + 1;
    }

    // ogrenciler_yeni.txt satırı ekle (append). Dersler boşsa "" yazılır.
    // format: id;username;password;ad;soyad;email;aldigiDersler
    public static void ogrenciEkleV2(String dosyaYolu,
                                     int id,
                                     String username,
                                     String password,
                                     String ad,
                                     String soyad,
                                     String email,
                                     String aldigiDersler) throws IOException {
        String safeAldigi = (aldigiDersler == null) ? "" : aldigiDersler;
        String line = id + ";" + username + ";" + password + ";" + ad + ";" + soyad + ";" + email + ";" + safeAldigi;
        line = line.replace("\r", "").replace("\n", "");
        File f = new File(dosyaYolu);
        boolean needsNewline = f.exists() && f.length() > 0;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dosyaYolu, true))) {
            if (needsNewline) bw.newLine();
            bw.write(line);
        }
    }

    // Öğretmen seçim ekranı için: id -> "Ad Soyad <email>"
    public static Map<Integer, String> ogrenciBasitBilgiMapOkuV2(String dosyaYolu) throws IOException {
        Map<Integer, String> sonuc = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                String[] p = t.split(";", 7);
                if (p.length < 6) continue;
                int id = Integer.parseInt(p[0].trim());
                String ad = p.length > 3 ? p[3].trim() : "";
                String soyad = p.length > 4 ? p[4].trim() : "";
                String email = p.length > 5 ? p[5].trim() : "";
                sonuc.put(id, (ad + " " + soyad).trim() + " <" + email + ">");
            }
        }
        return sonuc;
    }

    // ===== Embedded ders kaydi formatı =====
    // item (v3): DERSKODU:OGRETMEN_ID:...;
    //   anahtarlar: vize, final, not, dev, prog
    //   - vize/final/not/dev: "-" veya sayısal değer
    //   - prog: haftalık program slotları (virgül ile): MON-09,MON-10,WED-14 gibi
    //
    // Geriye uyumluluk:
    // - Eski: DERSKODU:OGRETMEN_ID
    // - v2:  DERSKODU:OGRETMEN_ID:not=...;dev=...
    public static class EmbeddedDersKaydi {
        public String dersKodu;
        public int ogretmenId;
        // "-" veya sayı (string tutuyoruz, yazmayı kolaylaştırır)
        public String vizeDegeri;
        public String finalDegeri;
        public String notDegeri; // legacy/toplam not (istersen ileride kaldırılabilir)
        public String devDegeri; // "-" veya sayı
        public String programDegeri; // ör: MON-09,MON-10

        public EmbeddedDersKaydi(String dersKodu,
                                 int ogretmenId,
                                 String vizeDegeri,
                                 String finalDegeri,
                                 String notDegeri,
                                 String devDegeri,
                                 String programDegeri) {
            this.dersKodu = dersKodu;
            this.ogretmenId = ogretmenId;
            this.vizeDegeri = (vizeDegeri == null || vizeDegeri.isBlank()) ? "-" : vizeDegeri;
            this.finalDegeri = (finalDegeri == null || finalDegeri.isBlank()) ? "-" : finalDegeri;
            this.notDegeri = (notDegeri == null || notDegeri.isBlank()) ? "-" : notDegeri;
            this.devDegeri = (devDegeri == null || devDegeri.isBlank()) ? "-" : devDegeri;
            this.programDegeri = (programDegeri == null) ? "" : programDegeri.trim();
        }

        // v2 uyumluluk ctor
        public EmbeddedDersKaydi(String dersKodu, int ogretmenId, String notDegeri, String devDegeri) {
            this(dersKodu, ogretmenId, "-", "-", notDegeri, devDegeri, "");
        }

        public String toItemString() {
            // Kanonik yazım: her key tek kez.
            // not: legacy alan; vize/final varken tekrar hesaplanabilir ama geriye uyum için saklanıyor.
            String p = (programDegeri == null) ? "" : programDegeri.trim();
            // program değeri ; veya | içeremez (dosya formatını bozar). Güvenlik için temizle.
            p = p.replace(";", ",").replace("|", ",");
            return dersKodu + ":" + ogretmenId
                    + ":vize=" + vizeDegeri
                    + ";final=" + finalDegeri
                    + ";not=" + notDegeri
                    + ";dev=" + devDegeri
                    + ";prog=" + p;
        }
    }

    // aldigiDersler alanı bozulmuşsa (item içinde '|' yerine ';' sızmışsa) yeniden tokenize eder.
    // Kural: yeni item başlangıcı -> ^[A-Z]{2,5}\d{3}:\d+  (örn MAT101:1)
    private static List<String> aldigiDerslerTokenizeGuvenli(String aldigi) {
        List<String> items = new ArrayList<>();
        if (aldigi == null) return items;
        String s = aldigi.replace("\r", "").replace("\n", "").trim();
        if (s.isEmpty()) return items;

        // Normal durum: '|' ile ayrılır.
        String[] quick = s.split("\\|", -1);
        boolean supheli = false;
        java.util.regex.Pattern startPat = java.util.regex.Pattern.compile("[A-Z]{2,5}\\d{3}:[0-9]+",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        for (String q : quick) {
            // Eğer tek token içinde birden fazla dersKodu:ogretmenId örüntüsü görülüyorsa bozulmuş olabilir.
            if (q == null) continue;
            String t = q.trim();
            if (t.isEmpty()) continue;
            int count = 0;
            java.util.regex.Matcher mm = startPat.matcher(t);
            while (mm.find()) count++;
            if (count > 1) {
                supheli = true;
                break;
            }
        }
        if (!supheli) {
            for (String q : quick) {
                if (q == null) continue;
                String t = q.trim();
                if (!t.isEmpty()) items.add(t);
            }
            return items;
        }

        // Fallback: regex ile item başlangıçlarını bul.
    java.util.regex.Matcher m = startPat.matcher(s);
        List<Integer> starts = new ArrayList<>();
        while (m.find()) {
            starts.add(m.start());
        }
        if (starts.isEmpty()) {
            items.add(s);
            return items;
        }
        for (int i = 0; i < starts.size(); i++) {
            int st = starts.get(i);
            int en = (i + 1 < starts.size()) ? starts.get(i + 1) : s.length();
            String chunk = s.substring(st, en).trim();
            if (!chunk.isEmpty()) {
                // chunk içinde bir önceki item'dan kalan '|' veya ';' artifact olabilir.
                while (chunk.startsWith("|") || chunk.startsWith(";")) chunk = chunk.substring(1).trim();
                if (!chunk.isEmpty()) items.add(chunk);
            }
        }
        return items;
    }

    // Tek bir item'ı parse eder. Eski formatı da destekler.
    public static EmbeddedDersKaydi embeddedDersKaydiParse(String item) {
        if (item == null) return null;
        item = item.trim();
        if (item.isEmpty()) return null;

        // Güvenlik: dosyada istemeden satır kırılması/boşluk eklenmesi olursa parse bozulmasın.
        item = item.replace("\r", "").replace("\n", "").trim();

        // Rehber format: DERSKODU:OGRETMEN_ID[:not=...;dev=...]
        // ':' sadece iki kez beklenir; fazlaları bir hata/artifact olabilir (line-wrap vb). Biz ';' tarafını esas alırız.
        String dersKodu;
        int ogretmenId;
        String rest = null;

        int firstColon = item.indexOf(':');
        if (firstColon < 0) return null;
        dersKodu = item.substring(0, firstColon).trim();

        String afterFirst = item.substring(firstColon + 1);
        int secondColon = afterFirst.indexOf(':');
        String teacherPart;
        if (secondColon < 0) {
            teacherPart = afterFirst;
        } else {
            teacherPart = afterFirst.substring(0, secondColon);
            rest = afterFirst.substring(secondColon + 1);
        }

        teacherPart = teacherPart.trim();
        try {
            ogretmenId = Integer.parseInt(teacherPart);
        } catch (NumberFormatException e) {
            return null;
        }

        String vizeDegeri = "-";
        String finalDegeri = "-";
        String notDegeri = "-";
        String devDegeri = "-";
        String progDegeri = "";
        if (rest != null) {
            // rest: not=...;dev=... (veya bozuk veri: dev=3;dev=2 gibi)
            rest = rest.trim();
            // Bozuk veri düzeltme: birden fazla dev varsa en sondakini baz al.
            String[] kvs = rest.split(";", -1);
            for (String kv : kvs) {
                String t = kv.trim();
                if (t.isEmpty()) continue;
                String[] pair = t.split("=", 2);
                if (pair.length != 2) continue;
                String k = pair[0].trim();
                String v = pair[1].trim();
                if (k.equalsIgnoreCase("vize")) {
                    vizeDegeri = v.isEmpty() ? "-" : v;
                } else if (k.equalsIgnoreCase("final")) {
                    finalDegeri = v.isEmpty() ? "-" : v;
                } else if (k.equalsIgnoreCase("not")) {
                    notDegeri = v.isEmpty() ? "-" : v;
                } else if (k.equalsIgnoreCase("dev")) {
                    // Eğer veri bozuksa ve birden fazla dev varsa, son dev değerini al.
                    devDegeri = v.isEmpty() ? "-" : v;
                } else if (k.equalsIgnoreCase("prog")) {
                    progDegeri = v == null ? "" : v.trim();
                }
            }
        }

        // Kanonikleştir: dışarıya her zaman tek not/dev taşı.
        return new EmbeddedDersKaydi(dersKodu, ogretmenId, vizeDegeri, finalDegeri, notDegeri, devDegeri, progDegeri);
    }

    // Tek bir embedded item stringini (MAT101:1:...) kesin olarak kanonikleştirir:
    // - aynı key birden fazla geçiyorsa SONUNCUSU geçerli olur
    // - sonuç v3 formatında döner (vize/final/not/dev/prog)
    //
    // Not: Bazı bozuk dosyalarda tek bir token içinde birden fazla ders başlangıcı (DERS:OGRT)
    // ve tekrar eden key zincirleri bulunabiliyor. Bu helper, token içindeki TÜM ders başlangıçlarını
    // çıkarır ve her birini embeddedDersKaydiParse() ile (son key kazanır) parse edip döndürür.
    private static List<EmbeddedDersKaydi> embeddedKayitlariTokenIcindanCikar(String token) {
        List<EmbeddedDersKaydi> out = new ArrayList<>();
        if (token == null) return out;
        String s = token.replace("\r", "").replace("\n", "").trim();
        if (s.isEmpty()) return out;

        java.util.regex.Pattern startPat = java.util.regex.Pattern.compile("[A-Z]{2,5}\\d{3}:[0-9]+",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = startPat.matcher(s);
        List<Integer> starts = new ArrayList<>();
        while (m.find()) starts.add(m.start());

        // Normal/sağlıklı token: tek başlangıç.
        if (starts.size() <= 1) {
            EmbeddedDersKaydi dk = embeddedDersKaydiParse(s);
            if (dk != null) out.add(dk);
            return out;
        }

        for (int i = 0; i < starts.size(); i++) {
            int st = starts.get(i);
            int en = (i + 1 < starts.size()) ? starts.get(i + 1) : s.length();
            String chunk = s.substring(st, en).trim();
            while (chunk.startsWith("|") || chunk.startsWith(";")) chunk = chunk.substring(1).trim();
            if (chunk.isEmpty()) continue;
            EmbeddedDersKaydi dk = embeddedDersKaydiParse(chunk);
            if (dk != null) out.add(dk);
        }
        return out;
    }

    // ogrenciId -> (dersKodu -> EmbeddedDersKaydi)
    public static Map<Integer, Map<String, EmbeddedDersKaydi>> ogrenciDersKayitlariOkuV2(String dosyaYolu) throws IOException {
        Map<Integer, Map<String, EmbeddedDersKaydi>> sonuc = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(";", -1);
                if (p.length < 7) continue;

                int ogrId = Integer.parseInt(p[0].trim());
                String aldigi = p[6].trim();
                Map<String, EmbeddedDersKaydi> dersKayitlari = new HashMap<>();
                if (!aldigi.isBlank()) {
                    List<String> items = aldigiDerslerTokenizeGuvenli(aldigi);
                    for (String it : items) {
                        for (EmbeddedDersKaydi dk : embeddedKayitlariTokenIcindanCikar(it)) {
                            if (dk == null) continue;
                            dersKayitlari.put(dk.dersKodu, dk);
                        }
                    }
                }
                sonuc.put(ogrId, dersKayitlari);
            }
        }
        return sonuc;
    }

    // Tek bir öğrencinin tek bir ders kaydını günceller ve ogrenciler_yeni.txt dosyasını yeniden yazar.
    // not/dev null => değişiklik yok; Integer => ilgili değere set.
    // Eğer item eski formatta ise yeni formatta yazılır.
    public static void ogrenciDersKaydiGuncelleV2(String dosyaYolu,
                                                  int ogrenciId,
                                                  String dersKodu,
                                                  int ogretmenId,
                                                  Integer not,
                                                  Integer devamsizlik) throws IOException {
        List<String> lines = new ArrayList<>();
        boolean bulundu = false;

        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                String rawLine = line;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    lines.add(rawLine);
                    continue;
                }

                String[] p = trimmed.split(";", -1);
                if (p.length < 7) {
                    lines.add(rawLine);
                    continue;
                }

                int id = Integer.parseInt(p[0].trim());
                if (id != ogrenciId) {
                    lines.add(rawLine);
                    continue;
                }

                // Bu satırı güncelle
                String aldigi = p[6].trim();
                List<String> itemsRaw = aldigi.isBlank() ? new ArrayList<>() : aldigiDerslerTokenizeGuvenli(aldigi);

                boolean dersBulundu = false;
                StringBuilder aldigiYeni = new StringBuilder();

                for (String it : itemsRaw) {
                    if (it == null || it.trim().isEmpty()) continue;
                    // Bazı bozuk dosyalarda tek token içinde birden fazla kayıt olabilir.
                    for (EmbeddedDersKaydi dk : embeddedKayitlariTokenIcindanCikar(it)) {
                        if (dk == null) continue;
                        if (dk.dersKodu.equalsIgnoreCase(dersKodu) && dk.ogretmenId == ogretmenId) {
                            if (not != null) dk.notDegeri = String.valueOf(not);
                            if (devamsizlik != null) dk.devDegeri = String.valueOf(devamsizlik);
                            dersBulundu = true;
                        }
                        if (aldigiYeni.length() > 0) aldigiYeni.append("|");
                        aldigiYeni.append(dk.toItemString());
                    }
                }

                if (!dersBulundu) {
                    // Bu öğrenci o dersi/öğretmeni yoksa veri tutarlılığı bozulmasın diye eklemiyoruz.
                    // Sadece var olan kaydı güncelleriz.
                }
                // Tek satır garantisi (dosya bozulmasın): item stringlerinde newline olamaz.
                String aldigiTekSatir = aldigiYeni.toString().replace("\r", "").replace("\n", "");
                p[6] = aldigiTekSatir;

                String yeniLine = String.join(";", p).replace("\r", "").replace("\n", "");
                lines.add(yeniLine);
                bulundu = true;
            }
        }

        if (!bulundu) {
            // Öğrenci bulunamadıysa dosyayı değiştirmeyelim.
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dosyaYolu, false))) {
            for (int i = 0; i < lines.size(); i++) {
                bw.write(lines.get(i));
                if (i < lines.size() - 1) bw.newLine();
            }
        }
    }

    // v3: vize/final güncellemesi (prog dahil diğer alanları korur).
    // vize/final null => değişiklik yok.
    public static void ogrenciDersKaydiGuncelleV3_VizeFinal(String dosyaYolu,
                                                           int ogrenciId,
                                                           String dersKodu,
                                                           int ogretmenId,
                                                           Integer vize,
                                                           Integer fin) throws IOException {
        List<String> lines = new ArrayList<>();
        boolean bulundu = false;

        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                String rawLine = line;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    lines.add(rawLine);
                    continue;
                }

                String[] p = trimmed.split(";", -1);
                if (p.length < 7) {
                    lines.add(rawLine);
                    continue;
                }

                int id = Integer.parseInt(p[0].trim());
                if (id != ogrenciId) {
                    lines.add(rawLine);
                    continue;
                }

                String aldigi = p[6].trim();
                List<String> itemsRaw = aldigi.isBlank() ? new ArrayList<>() : aldigiDerslerTokenizeGuvenli(aldigi);

                StringBuilder aldigiYeni = new StringBuilder();
                for (String it : itemsRaw) {
                    if (it == null || it.trim().isEmpty()) continue;
                    for (EmbeddedDersKaydi dk : embeddedKayitlariTokenIcindanCikar(it)) {
                        if (dk == null) continue;
                        if (dk.dersKodu.equalsIgnoreCase(dersKodu) && dk.ogretmenId == ogretmenId) {
                            if (vize != null) dk.vizeDegeri = String.valueOf(vize);
                            if (fin != null) dk.finalDegeri = String.valueOf(fin);
                        }
                        if (aldigiYeni.length() > 0) aldigiYeni.append("|");
                        aldigiYeni.append(dk.toItemString());
                    }
                }

                // Ders bulunamadıysa eklemiyoruz.
                p[6] = aldigiYeni.toString().replace("\r", "").replace("\n", "");
                lines.add(String.join(";", p).replace("\r", "").replace("\n", ""));
                bulundu = true;
            }
        }

        if (!bulundu) return;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dosyaYolu, false))) {
            for (int i = 0; i < lines.size(); i++) {
                bw.write(lines.get(i));
                if (i < lines.size() - 1) bw.newLine();
            }
        }
    }

    // ===== Yeni format: idare.txt (ADMIN) =====
    // id;username;password;email;ad;soyad
    public static List<SistemKullanicisi> idareKullanicilariOku(String dosyaYolu) throws IOException {
        List<SistemKullanicisi> sonuc = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(";", -1);
                if (p.length < 6) continue;

                int id = Integer.parseInt(p[0].trim());
                String username = p[1].trim();
                String password = p[2];
                String email = p[3].trim();

                sonuc.add(new SistemKullanicisi(id, username, password, email, Rol.ADMIN));
            }
        }
        return sonuc;
    }

    // ===== Yeni format: ogretmenler.txt (OGRETMEN) =====
    // id;username;password;email;ad;soyad;dersler
    // dersler: MAT101|FIZ201|CMP203
    public static List<SistemKullanicisi> ogretmenKullanicilariOku(String dosyaYolu) throws IOException {
        List<SistemKullanicisi> sonuc = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(";", -1);
                if (p.length < 7) continue;

                int id = Integer.parseInt(p[0].trim());
                String username = p[1].trim();
                String password = p[2];
                String email = p[3].trim();

                // Dersleri login objesine gömmüyoruz (mevcut imzaları bozmamak için).
                sonuc.add(new SistemKullanicisi(id, username, password, email, Rol.OGRETMEN));
            }
        }
        return sonuc;
    }

    // ogretmenler.txt içinden öğretmenId -> dersKodları map'i
    public static Map<Integer, List<String>> ogretmenDersleriOku(String dosyaYolu) throws IOException {
        Map<Integer, List<String>> sonuc = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(";", -1);
                if (p.length < 7) continue;

                int id = Integer.parseInt(p[0].trim());
                String dersler = p[6].trim();
                List<String> list = new ArrayList<>();
                if (!dersler.isBlank()) {
                    String[] items = dersler.split("\\|", -1);
                    for (String it : items) {
                        if (!it.isBlank()) list.add(it.trim());
                    }
                }
                sonuc.put(id, list);
            }
        }
        return sonuc;
    }

    // ===== Yeni öğrenci için otomatik ders ataması =====
    // ogretmenler.txt içinden ders->öğretmenId havuzu çıkarır, rastgele N ders seçer ve
    // ogrenciler_yeni.txt embedded item formatında döndürür:
    //   DERSKODU:OGRETMEN_ID:not=-;dev=-|...
    // Not: Veri bozuk/eksik olursa güvenli şekilde daha az ders dönebilir veya "" dönebilir.
    public static String rastgeleDersAtamasiUretV2(String ogretmenlerDosyaYolu, int dersAdedi) throws IOException {
        if (dersAdedi <= 0) return "";

        Map<Integer, List<String>> ogretmenDersleri = ogretmenDersleriOku(ogretmenlerDosyaYolu);
        if (ogretmenDersleri.isEmpty()) return "";

        // dersKodu -> öğretmenId listesi
        Map<String, List<Integer>> dersOgretmenleri = new HashMap<>();
        for (Map.Entry<Integer, List<String>> e : ogretmenDersleri.entrySet()) {
            int ogretmenId = e.getKey();
            for (String ders : e.getValue()) {
                if (ders == null) continue;
                String d = ders.trim();
                if (d.isEmpty()) continue;
                dersOgretmenleri.computeIfAbsent(d, k -> new ArrayList<>()).add(ogretmenId);
            }
        }
        if (dersOgretmenleri.isEmpty()) return "";

        List<String> dersHavuzu = new ArrayList<>(dersOgretmenleri.keySet());
        java.util.Collections.shuffle(dersHavuzu);

        int secilecek = Math.min(dersAdedi, dersHavuzu.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < secilecek; i++) {
            String dersKodu = dersHavuzu.get(i);
            List<Integer> ogretmenler = dersOgretmenleri.get(dersKodu);
            if (ogretmenler == null || ogretmenler.isEmpty()) continue;
            int ogretmenId = ogretmenler.get((int) (Math.random() * ogretmenler.size()));
            EmbeddedDersKaydi dk = new EmbeddedDersKaydi(dersKodu, ogretmenId, "-", "-");
            if (sb.length() > 0) sb.append('|');
            sb.append(dk.toItemString());
        }
        return sb.toString();
    }

    // ===== Yeni format: ogrenciler_yeni.txt =====
    // id;username;ad;soyad;email;aldigiDersler
    // aldigiDersler: dersKodu:ogretmenId|dersKodu:ogretmenId
    public static Map<Integer, Map<String, Integer>> ogrenciDersOgretmenMapOku(String dosyaYolu) throws IOException {
        Map<Integer, Map<String, Integer>> sonuc = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(";", -1);
                if (p.length < 6) continue;

                int ogrId = Integer.parseInt(p[0].trim());
                String aldigi = p[5].trim();
                Map<String, Integer> dersOgretmen = new HashMap<>();
                if (!aldigi.isBlank()) {
                    String[] items = aldigi.split("\\|", -1);
                    for (String item : items) {
                        if (item.isBlank()) continue;
                        String[] kv = item.split(":", 2);
                        if (kv.length != 2) continue;
                        String dersKodu = kv[0].trim();
                        int ogretmenId = Integer.parseInt(kv[1].trim());
                        // Aynı dersin farklı öğretmeni olmaması gerekir; burada son değeri yazar (veri hatası).
                        dersOgretmen.put(dersKodu, ogretmenId);
                    }
                }
                sonuc.put(ogrId, dersOgretmen);
            }
        }
        return sonuc;
    }

    // ===== Yeni format v2: ogrenciler_yeni.txt (tek dosya: login + profil + dersler) =====
    // id;username;password;ad;soyad;email;aldigiDersler
    // aldigiDersler: dersKodu:ogretmenId|dersKodu:ogretmenId
    public static List<SistemKullanicisi> ogrenciKullanicilariOkuV2(String dosyaYolu) throws IOException {
        List<SistemKullanicisi> sonuc = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(";", -1);
                if (p.length < 7) continue;

                int id = Integer.parseInt(p[0].trim());
                String username = p[1].trim();
                String password = p[2];
                String email = p[5].trim();

                sonuc.add(new SistemKullanicisi(id, username, password, email, Rol.OGRENCI));
            }
        }
        return sonuc;
    }

    public static Map<Integer, Map<String, Integer>> ogrenciDersOgretmenMapOkuV2(String dosyaYolu) throws IOException {
        Map<Integer, Map<String, Integer>> sonuc = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(";", -1);
                if (p.length < 7) continue;

                int ogrId = Integer.parseInt(p[0].trim());
                String aldigi = p[6].trim();
                Map<String, Integer> dersOgretmen = new HashMap<>();
                if (!aldigi.isBlank()) {
                    List<String> items = aldigiDerslerTokenizeGuvenli(aldigi);
                    for (String it : items) {
                        for (EmbeddedDersKaydi dk : embeddedKayitlariTokenIcindanCikar(it)) {
                            if (dk == null) continue;
                            dersOgretmen.put(dk.dersKodu, dk.ogretmenId);
                        }
                    }
                }
                sonuc.put(ogrId, dersOgretmen);
            }
        }
        return sonuc;
    }

    // ===== Migrate: ogrenciler_yeni.txt embedded kayıtlarını v3 kanonik formata çevir =====
    // - Eski item'lar (DERS:OGRT veya :not/dev) korunur ve v3 formuna yazılır.
    // - prog boşsa ve ogretmenProgramlari verildiyse doldurulur.
    // - Dosya başlık satırları (#) korunur.
    public static boolean ogrencilerDosyasiMigrateV3(String ogrencilerDosyaYolu,
                                                     Map<Integer, Map<String, List<String>>> ogretmenProgramlari) throws IOException {
        List<String> lines = new ArrayList<>();
        boolean degisti = false;

        try (BufferedReader br = new BufferedReader(new FileReader(ogrencilerDosyaYolu))) {
            String line;
            while ((line = br.readLine()) != null) {
                String rawLine = line;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    lines.add(rawLine);
                    continue;
                }

                String[] p = trimmed.split(";", -1);
                if (p.length < 7) {
                    lines.add(rawLine);
                    continue;
                }

                String aldigi = p[6].trim();
                if (aldigi.isBlank()) {
                    lines.add(rawLine);
                    continue;
                }

                List<String> itemsRaw = aldigiDerslerTokenizeGuvenli(aldigi);
                // Aynı ders kaydı tek token içinde şişmiş olabilir. Bu durumda kayıtları dersKodu+ogretmenId bazında
                // tekilleştirip en son görülen değerleri alacağız.
                Map<String, EmbeddedDersKaydi> uniq = new HashMap<>();

                for (String it : itemsRaw) {
                    for (EmbeddedDersKaydi dk : embeddedKayitlariTokenIcindanCikar(it)) {
                        if (dk == null) continue;

                        // prog eksikse doldur
                        if ((dk.programDegeri == null || dk.programDegeri.isBlank()) && ogretmenProgramlari != null) {
                            Map<String, List<String>> dersMap = ogretmenProgramlari.get(dk.ogretmenId);
                            if (dersMap != null) {
                                List<String> slots = dersMap.get(dk.dersKodu);
                                if (slots != null && !slots.isEmpty()) dk.programDegeri = String.join(",", slots);
                            }
                        }

                        // uniq key: ders+ogretmen
                        String key = dk.dersKodu.toUpperCase() + ":" + dk.ogretmenId;
                        uniq.put(key, dk);
                    }
                }

                // uniq map'i tekrar '|' ile yaz. Sıralama: deterministik olsun diye key'e göre.
                List<String> keys = new ArrayList<>(uniq.keySet());
                java.util.Collections.sort(keys);
                StringBuilder yeni = new StringBuilder();
                for (String k : keys) {
                    EmbeddedDersKaydi dk = uniq.get(k);
                    if (dk == null) continue;
                    String canon = dk.toItemString();
                    if (yeni.length() > 0) yeni.append('|');
                    yeni.append(canon);
                }

                // Kıyas: eski aldigi ile yeni değer
                String yeniAldigi = yeni.toString().replace("\r", "").replace("\n", "");
                if (!yeniAldigi.equals(p[6])) degisti = true;
                p[6] = yeniAldigi;
                lines.add(String.join(";", p).replace("\r", "").replace("\n", ""));
            }
        }

        if (!degisti) return false;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ogrencilerDosyaYolu, false))) {
            for (int i = 0; i < lines.size(); i++) {
                bw.write(lines.get(i));
                if (i < lines.size() - 1) bw.newLine();
            }
        }
        return true;
    }

}
