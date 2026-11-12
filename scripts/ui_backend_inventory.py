#!/usr/bin/env python3
"""Index UI declarations, popup call sites and literal API references without running the app."""
from pathlib import Path
import argparse
import json
import re

ROOT = Path(__file__).resolve().parents[1]
DATE = "2026-11-10"
FUNCTION = re.compile(r"\bfun\s+(?:<[^>\n]+>\s*)?(?:[\w<>?.]+\.)?(\w+)\s*\(")
POPUP = re.compile(r"\b(ModalBottomSheet|AlertDialog|DatePickerDialog|Dialog|Popup|DropdownMenu|ExposedDropdownMenu)\s*\(")
COMMAND = re.compile(r"['\"]([a-z]+(?:\.[a-z]+)+)['\"]")


def location(text, offset):
    return text.count("\n", 0, offset) + 1


def build():
    common = ROOT / "composeApp/src/commonMain/kotlin/com/matchora/app"
    paths = sorted([common / "App.kt", *common.joinpath("ui").rglob("*.kt")])
    files = []
    for path in paths:
        source = path.read_text()
        files.append({
            "file": str(path.relative_to(ROOT)),
            "functions": [{"name": m[1], "line": location(source, m.start())} for m in FUNCTION.finditer(source)],
            "popupSites": [{"type": m[1], "line": location(source, m.start())} for m in POPUP.finditer(source)],
        })
    native = sorted([*ROOT.joinpath("composeApp/src/androidMain").rglob("*.kt"),
                     *ROOT.joinpath("composeApp/src/iosMain").rglob("*.kt")])
    native_files = [{"file": str(p.relative_to(ROOT)), "functions": [m[1] for m in FUNCTION.finditer(p.read_text())]} for p in native]
    app_sources = sorted(common.rglob("*.kt"))
    commands = []
    for path in sorted(ROOT.joinpath("backend/src").glob("*.mjs")):
        source = path.read_text()
        for match in re.finditer(r"['\"]([a-z]+(?:\.[a-z]+)+)['\"]\s*:\s*async\b", source):
            name = match[1]
            refs = []
            for app_path in app_sources:
                text = app_path.read_text()
                for ref in COMMAND.finditer(text):
                    if ref[1] == name:
                        refs.append({"file": str(app_path.relative_to(ROOT)), "line": location(text, ref.start())})
            commands.append({"name": name, "file": str(path.relative_to(ROOT)), "line": location(source, match.start()), "literalMobileReferences": refs})
    return {"date": DATE, "method": "Static declaration and call-site inventory; not device execution or semantic coverage.",
            "counts": {"commonUiFiles": len(files), "functionDeclarations": sum(len(f["functions"]) for f in files),
                       "popupCallSites": sum(len(f["popupSites"]) for f in files), "nativeKotlinFiles": len(native_files), "commands": len(commands)},
            "commonUi": files, "native": native_files, "commands": sorted(commands, key=lambda c: c["name"])}


def markdown(data):
    c = data["counts"]
    lines = ["# Matchora — kaynak envanteri", "", f"Tarih: {DATE}. `python3 scripts/ui_backend_inventory.py` ile üretildi.", "",
             f"**{c['commonUiFiles']} ortak UI dosyası**, **{c['functionDeclarations']} fonksiyon bildirimi**, **{c['popupCallSites']} pencere/menü açılış noktası**, **{c['nativeKotlinFiles']} native Kotlin dosyası**, **{c['commands']} sunucu komutu**.", "",
             "Fonksiyon sayısı ekran sayısı değildir; yardımcı çizim, yerel callback ve `expect` bildirimleri de dahildir. Pencere sayısı farklı kullanıcı akışlarının sayısı değildir; ortak bir pencere birçok yerde kullanılır. Bu tarama koddan envanter çıkarır, cihaz testi veya bütün davranışların tamamlandığı iddiası değildir.", "",
             "Kullanıcıya görünen alanlar ve yapılmış/yapılmamış bağlantılar: [tam ekran envanteri](TUM_EKRANLAR_BACKEND_ENVANTERI.md).", "",
             "## Ortak UI dosyaları ve tüm fonksiyonlar", "", "| Dosya | Fonksiyonlar | Pencere/menü noktaları |", "|---|---|---|"]
    for f in data["commonUi"]:
        declarations = ", ".join(f"`{x['name']}`:{x['line']}" for x in f["functions"]) or "Fonksiyon bildirimi yok"
        popups = ", ".join(f"`{x['type']}`:{x['line']}" for x in f["popupSites"]) or "Yok"
        lines.append(f"| [{Path(f['file']).name}](../{f['file']}) | {declarations} | {popups} |")
    lines += ["", "## Native Kotlin uygulamaları", "", "| Dosya | Fonksiyonlar |", "|---|---|"]
    for f in data["native"]:
        lines.append(f"| [{f['file']}](../{f['file']}) | {', '.join('`'+x+'`' for x in f['functions']) or 'Sınıf / callback uygulaması'} |")
    lines += ["", "## Komut dışındaki HTTP uç noktaları", "",
              f"Kaynak: `backend/src/server.mjs`. Bu yollar {c['commands']} komut sayısına dahil değildir.", "",
              "| Yöntem | Yol / grup | Amaç |", "|---|---|---|",
              "| GET | `/health`, `/openapi.json` | Sağlık ve komut sözleşmesi |",
              "| GET | `/v1/config`, `/v1/bootstrap` | Yapılandırma ve kapsamlı başlangıç verisi |",
              "| GET | `/v1/events`, `/v1/events/:id` | Maç listesi ve erişim kontrollü ayrıntı |",
              "| GET | `/v1/players/:id?sport=...` | Gizlilik kontrollü sporcu formu/uyum |",
              "| GET | `/v1/catalog/:kind`, `/v1/notifications` | Katalog ve kişisel bildirim sayfaları |",
              "| GET | `/v1/memories`, `/v1/rankings` | Küresel vitrin ve filtrelenmiş sıralama sayfaları |",
              "| GET | `/v1/threads/:id/messages` | Üyeliğe bağlı mesaj sayfalaması |",
              "| GET / POST | `/v1/media/:id` / `/v1/media` | Yetkili görsel okuma / yükleme |",
              "| GET | `/v1/passports/verify?code=...` | İmzalı pasaport doğrulama |",
              "| GET | `/v1/account/export`, `/v1/account/sessions` | Kendi verisi ve oturumlar |",
              "| GET | `/v1/admin/inbox` | Güncel yönetici yetkisiyle moderasyon kuyruğu |",
              "| GET | `/v1/operator/venues`, `/v1/venues/:id/reviews` | Tesis yönetimi / erişim kontrollü değerlendirmeler |",
              "| POST | `/v1/auth/oauth/start`, `/v1/auth/oauth/finish`, `/v1/auth/exchange` | Sağlayıcı girişi |",
              "| POST | `/v1/auth/otp/start`, `/v1/auth/otp/verify` | Telefonla giriş |",
              "| POST | `/v1/auth/contact/start`, `/v1/auth/contact/verify` | Mevcut hesap telefonu |",
              "| POST | `/v1/auth/refresh`, `/v1/auth/logout`, `/v1/auth/dev` | Oturum; dev yalnız yerel ortam |",
              "| POST | `/v1/billing/revenuecat`, `/v1/billing/webhook` | Doğrulanmış mağaza bildirimi |",
              "| POST | `/v1/commands/:operation` | Aşağıdaki yetkili işlemler |",
              "| OPTIONS | İzinli CORS kaynakları | Preflight; işlem yetkisi vermez |"]
    lines += ["", "Swift uygulama delegesi, plist/manifest ve entitlements ayrıca ana envanterin cihaz/yayın bölümündedir.", "",
              "## Bütün sunucu komutları", "", "İstemci referansı yalnız sabit metin taramasıdır; dinamik çağrıya veya başarıya kanıt değildir. Referanssız komut otomatik olarak eksik sayılmaz; operatör veya sunucu işi olabilir.", "",
              "| Komut | Sunucu kaynağı | Mobil sabit referansları |", "|---|---|---|"]
    for command in data["commands"]:
        refs = ", ".join(f"`{Path(r['file']).name}`:{r['line']}" for r in command["literalMobileReferences"]) or "Sabit mobil referans yok — API/operatör/sunucu akışı kontrol edilmeli"
        lines.append(f"| `{command['name']}` | [{Path(command['file']).name}:{command['line']}](../{command['file']}) | {refs} |")
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    data = build()
    outputs = {ROOT / f"docs/validation/{DATE}/ui-inventory.json": json.dumps(data, ensure_ascii=False, indent=2) + "\n",
               ROOT / "docs/UI_KAYNAK_DIZINI.md": markdown(data)}
    for path, content in outputs.items():
        if args.check:
            if not path.exists() or path.read_text() != content:
                raise SystemExit(f"Envanter güncel değil: {path.relative_to(ROOT)}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
    report = ROOT / "docs/TUM_EKRANLAR_BACKEND_ENVANTERI.md"
    if args.check:
        text = report.read_text()
        missing = [Path(f["file"]).name for f in data["commonUi"] if Path(f["file"]).name not in text]
        if missing:
            raise SystemExit("Ana raporda adı olmayan UI dosyaları: " + ", ".join(missing))
    print(json.dumps(data["counts"], ensure_ascii=False))
