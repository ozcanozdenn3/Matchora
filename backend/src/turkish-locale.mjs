const TURKISH_CITIES = (
  'Adana|Adıyaman|Afyonkarahisar|Ağrı|Amasya|Ankara|Antalya|Artvin|Aydın|Balıkesir|' +
  'Bilecik|Bingöl|Bitlis|Bolu|Burdur|Bursa|Çanakkale|Çankırı|Çorum|Denizli|Diyarbakır|' +
  'Edirne|Elazığ|Erzincan|Erzurum|Eskişehir|Gaziantep|Giresun|Gümüşhane|Hakkâri|Hatay|' +
  'Isparta|Mersin|İstanbul|İzmir|Kars|Kastamonu|Kayseri|Kırklareli|Kırşehir|Kocaeli|' +
  'Konya|Kütahya|Malatya|Manisa|Kahramanmaraş|Mardin|Muğla|Muş|Nevşehir|Niğde|Ordu|' +
  'Rize|Sakarya|Samsun|Siirt|Sinop|Sivas|Tekirdağ|Tokat|Trabzon|Tunceli|Şanlıurfa|Uşak|' +
  'Van|Yozgat|Zonguldak|Aksaray|Bayburt|Karaman|Kırıkkale|Batman|Şırnak|Bartın|Ardahan|' +
  'Iğdır|Yalova|Karabük|Kilis|Osmaniye|Düzce'
).split('|');

const FOLD_MAP = {
  'İ': 'i', 'I': 'i', 'ı': 'i',
  'Ş': 's', 'ş': 's',
  'Ğ': 'g', 'ğ': 'g',
  'Ü': 'u', 'ü': 'u',
  'Ö': 'o', 'ö': 'o',
  'Ç': 'c', 'ç': 'c',
  'Â': 'a', 'â': 'a',
  'Î': 'i', 'î': 'i',
  'Û': 'u', 'û': 'u'
};

/**
 * Folds Turkish letters and combining dot marks so "İstanbul", "Istanbul", and "istanbul"
 * compare identically regardless of host locale.
 */
export function canonicalTurkish(value) {
  return String(value ?? '')
    .trim()
    .replace(/\u0307/g, '')
    .replace(/[İIıŞşĞğÜüÖöÇçÂâÎîÛû]/g, ch => FOLD_MAP[ch] ?? ch)
    .toLowerCase();
}

const CITY_BY_FOLDED = new Map(TURKISH_CITIES.map(name => [canonicalTurkish(name), name]));

export function canonicalTurkishCity(value) {
  const trimmed = String(value ?? '').trim();
  if (!trimmed) return '';
  return CITY_BY_FOLDED.get(canonicalTurkish(trimmed)) ?? trimmed;
}

export function sameTurkishPlace(left, right) {
  const a = canonicalTurkish(left);
  const b = canonicalTurkish(right);
  return Boolean(a && b && a === b);
}

