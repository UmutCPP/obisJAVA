# Konsol Tabanlı Öğrenci Bilgilendirme Sistemi (Java SE)

Bu proje, tamamen **konsol üzerinden** çalışan ve OOP gereksinimlerini net şekilde gösteren basit bir Öğrenci Bilgilendirme Sistemi örneğidir.

## Mimari (kısa)

- `model`: Domain sınıfları (Kişi, Öğrenci, Ders vb.)
  - `Kisi` abstract sınıfı, ortak alanları ve zorunlu davranışları tanımlar.
  - `Ogrenci` -> `LisansOgrencisi` / `YuksekLisansOgrencisi` kalıtım zinciri.
  - `Ogretmen` ayrı bir kalıtım zinciri olarak `Kisi`'den türetilir.
- `interfaces`: Arayüzler
  - `Goruntulenebilir`: Konsol çıktısı için metin üretir.
  - `Guncellenebilir`: Basit güncelleme davranışı sağlar.
- `service`: İş kuralları
  - `NotHesaplamaService` abstract: Ortalama/harf notu sözleşmesi.
  - `StandartNotHesaplamaService`: Somut not hesaplama.
  - `OgrenciService`: Öğrenci işlemleri + yetki/not doğrulama.
- `generic`: `Repository<T>` generic yapısı ile veri saklama.
- `util`: Dosya ve tarih yardımcıları.
- `exception`: Custom exception sınıfları.

## Nasıl çalıştırılır?

Bu repo herhangi bir framework kullanmaz (sadece Java SE).

### Derleme

```bash
cd /home/anonym/obisJAVA/Obis
javac $(find src -name '*.java')
```

### Çalıştırma

```bash
cd /home/anonym/obisJAVA/Obis
java -cp src app.Main
```

Program açıldığında örnek kayıtlar oluşturulur ve menü üzerinden işlem yapılabilir.

## Dosya kaydet/oku

- Menüden **6** ile `ogrenciler.txt` dosyasına kaydeder.
- Menüden **7** ile aynı dosyadan okuyup repo'ya ekler.
