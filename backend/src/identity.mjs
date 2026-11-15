import { ensure, text, integer, choice, keys, json } from './errors.mjs';
import { legalConfiguration } from './legal.mjs';
import { id } from './auth.mjs';
import { SPORTS } from './sports.mjs';
import { canonicalTurkishCity } from './turkish-locale.mjs';
import { row, profile, reward, encode, audit } from './model.mjs';
const editable = ['name','city','area','bio','birthYear','phone','email','skills','campus','company','dominantSide','availability','jerseyNumber','avatarUploadId','expectedVersion'];
function editableProfile(p, now) {
  keys(p, editable); const year = new Date(now).getUTCFullYear();
  const skills = p.skills;
  ensure(Array.isArray(skills) && skills.length >= 1 && skills.length <= 10);
  ensure(new Set(skills.map(s => s.sport)).size === skills.length);
  const availability = p.availability ?? []; ensure(Array.isArray(availability) && availability.length <= 14 && availability.every(v => typeof v === 'string' && v.length <= 100));
  return {
    name: text(p.name, 'İsim', 2, 40), city: canonicalTurkishCity(text(p.city, 'İl', 2, 60)), area: text(p.area, 'İlçe', 2, 60),
    bio: text(p.bio ?? '', 'Biyografi', 0, 280), birthYear: integer(p.birthYear, 'Doğum yılı', year - 100, year - 13),
    phone: text(p.phone ?? '', 'Telefon', 0, 24), email: text(p.email ?? '', 'E-posta', 0, 100),
    skills: skills.map(s => ({ sport: choice(s.sport, SPORTS, 'Branş'), level: choice(s.level, ['BEGINNER','INTERMEDIATE','ADVANCED','PRO'], 'Seviye'), position: text(s.position ?? '', 'Rol', 0, 80) })),
    campus: p.campus ? text(p.campus, 'Kampüs', 2, 80) : null, company: p.company ? text(p.company, 'Şirket', 2, 80) : null,
    dominantSide: text(p.dominantSide ?? '', 'Baskın taraf', 0, 80), availability,
    jerseyNumber: p.jerseyNumber == null ? null : integer(p.jerseyNumber, 'Forma numarası', 0, 99), avatarUploadId: p.avatarUploadId ?? null
  };
}
export const identityHandlers = {
  'profile.save': async (ctx, p) => {
    const fields = editableProfile(p, ctx.now), old = await ctx.db.get('SELECT * FROM profiles WHERE user_id=?', [ctx.actor.id]);
    if(ctx.config.production){const legal=await legalConfiguration(ctx);ensure(legal.configured,503,'LEGAL_DOCUMENTS_PENDING','Belgeler henüz yayınlanmadı.');ensure(await ctx.db.get('SELECT id FROM legal_acceptances WHERE user_id=? AND terms_version=? AND privacy_version=?',[ctx.actor.id,legal.documents.terms.version,legal.documents.privacy.version]),403,'LEGAL_ACCEPTANCE_REQUIRED','Güncel kullanım koşulları ve gizlilik politikasını incelemelisin.');}
    if (old) ensure(p.expectedVersion === old.version, 409, 'VERSION_CONFLICT', 'Profil başka bir yerde güncellendi. Yenile ve tekrar dene.');
    if (fields.email) ensure(/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(fields.email), 400, 'INVALID_INPUT', 'Geçerli bir e-posta adresi gir.');
    if (fields.avatarUploadId) { const upload = await row(ctx, 'uploads', 'id', fields.avatarUploadId); ensure(upload.owner_id === ctx.actor.id && upload.status === 'ready', 403, 'FORBIDDEN', 'Fotoğraf kullanılamıyor.'); }
    const previous = old ? json(old.payload) : {};
    const user = await row(ctx, 'users', 'id', ctx.actor.id);
    ensure(!fields.phone||fields.phone===user.phone,409,'PHONE_VERIFICATION_REQUIRED','Telefon numaranı değiştirmek için hesap ayarlarından telefon doğrulamasını kullan.');
    const archive = { ...(previous.skillArchive ?? {}) };
    for (const skill of previous.skills ?? []) archive[skill.sport] = skill;
    for (const skill of previous.skills ?? []) if (!fields.skills.some(s => s.sport === skill.sport)) {
      const unsettled = await ctx.db.get("SELECT e.id FROM events e JOIN event_members m ON m.event_id=e.id LEFT JOIN results r ON r.event_id=e.id WHERE m.user_id=? AND m.status='joined' AND e.sport=? AND e.status NOT IN ('VERIFIED','CANCELLED','ARCHIVED') AND (r.status IN ('pending','disputed') OR (r.event_id IS NULL AND e.ends_at>?)) LIMIT 1", [ctx.actor.id, skill.sport, ctx.now-72*3600000]);
      ensure(!unsettled,409,'SPORT_IN_USE','Bu branşta sonuçlanmamış maçın var. Branşı maç kapanınca kaldırabilirsin.');
    }
    const next = { ...fields, skillArchive: archive, phone:user.phone??'', verifications: [...(user.phone ? ['BASIC'] : []), ...(previous.avatarUploadId===fields.avatarUploadId && previous.verifications?.includes('PHOTO') ? ['PHOTO'] : [])], reliability: previous.reliability ?? 100, fairPlay: previous.fairPlay ?? 0,
      verifiedMatches: previous.verifiedMatches ?? 0, streakWeeks: previous.streakWeeks ?? 0,
      skills: fields.skills.map(skill => ({ ...skill, rating: archive[skill.sport]?.rating ?? 1000,
        wonMatches: archive[skill.sport]?.wonMatches ?? 0, verifiedMatches: archive[skill.sport]?.verifiedMatches ?? 0, winRate: archive[skill.sport]?.winRate ?? 0, localRank: null })) };
    await ctx.db.run('INSERT INTO profiles(user_id,payload,version,updated_at) VALUES(?,?,1,?) ON CONFLICT(user_id) DO UPDATE SET payload=?,version=profiles.version+1,updated_at=?', [ctx.actor.id, encode(next), ctx.now, encode(next), ctx.now]);
    await reward(ctx, ctx.actor.id, 'welcome', 20);
    if (fields.bio && fields.availability.length && fields.dominantSide) await reward(ctx, ctx.actor.id, 'passport_complete', 20);
    await audit(ctx, 'profile.save', ctx.actor.id); return { profile: next, version: old ? old.version + 1 : 1 };
  },
  'preferences.save': async (ctx, p) => {
    keys(p, ['privacy','notifications']); const old = await row(ctx, 'preferences', 'user_id', ctx.actor.id);
    const privacy = { ...json(old.privacy) }, notifications = { ...json(old.notifications) };
    if (p.privacy) {
      keys(p.privacy, ['showReliability','showRatingHistory','showInRankings','dmPolicy','showActivityHeatmap','approximateAreaOnly','chemistrySignals']);
      for (const [key,value] of Object.entries(p.privacy)) { if (key === 'dmPolicy') choice(value, ['Ortak etkinlik sonrası','Herkes','Kapalı'], key); else ensure(typeof value === 'boolean'); privacy[key] = value; }
      privacy.approximateAreaOnly = true;
    }
    if (p.notifications) {
      keys(p.notifications, ['emergencyFill','upcomingMatch','challenges','waitlist','rematch','social','weeklyDigest','marketing','quietHours','quietFrom','quietTo','dailyCap','emergencyRadiusKm']);
      for (const [key,value] of Object.entries(p.notifications)) {
        if (['quietFrom','quietTo'].includes(key)) ensure(/^([01]\d|2[0-3]):[0-5]\d$/.test(value));
        else if (['dailyCap','emergencyRadiusKm'].includes(key)) integer(value, key, 1, key === 'dailyCap' ? 100 : 50);
        else ensure(typeof value === 'boolean'); notifications[key] = value;
      }
    }
    await ctx.db.run('UPDATE preferences SET privacy=?,notifications=? WHERE user_id=?', [encode(privacy), encode(notifications), ctx.actor.id]); return { privacy, notifications };
  },
  'discovery.location':async(ctx,p)=>{
    keys(p,['latitude','longitude']);const athlete=await profile(ctx),old=await ctx.db.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[ctx.actor.id]);
    const value=old?json(old.payload):{city:athlete.city,district:athlete.area,travelMode:false};
    if(p.latitude==null&&p.longitude==null)delete value.location;
    else{ensure(Number.isFinite(p.latitude)&&Math.abs(p.latitude)<=90&&Number.isFinite(p.longitude)&&Math.abs(p.longitude)<=180,400,'INVALID_LOCATION','Konum geçersiz.');value.location={latitude:Math.round(p.latitude*100)/100,longitude:Math.round(p.longitude*100)/100,expiresAt:ctx.now+15*60000};}
    await ctx.db.run('INSERT INTO discovery_preferences(user_id,payload,updated_at) VALUES(?,?,?) ON CONFLICT(user_id) DO UPDATE SET payload=?,updated_at=?',[ctx.actor.id,encode(value),ctx.now,encode(value),ctx.now]);return{saved:true,expiresAt:value.location?.expiresAt??null};
  },
  'discovery.save':async(ctx,p)=>{keys(p,['city','district','travelMode']);ensure(typeof p.travelMode==='boolean');const old=await ctx.db.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[ctx.actor.id]),previous=old?json(old.payload):{};const value={city:text(p.city,'İl',2,60),district:text(p.district,'İlçe',2,60),travelMode:p.travelMode,...(previous.location?.expiresAt>ctx.now?{location:previous.location}:{})};await ctx.db.run('INSERT INTO discovery_preferences(user_id,payload,updated_at) VALUES(?,?,?) ON CONFLICT(user_id) DO UPDATE SET payload=?,updated_at=?',[ctx.actor.id,encode(value),ctx.now,encode(value),ctx.now]);return value;},
  'support.create': async (ctx,p) => {
    keys(p, ['subject','message','category']); const requestId = id();
    await ctx.db.run('INSERT INTO support_requests(id,user_id,category,subject,message,status,created_at) VALUES(?,?,?,?,?,?,?)', [requestId, ctx.actor.id, choice(p.category, ['Teknik sorun','Hesap','Ödeme','Maç ve güvenlik','Fotoğraf doğrulaması'], 'Kategori'), text(p.subject,'Başlık',3,100),text(p.message,'Açıklama',10,1000),'open',ctx.now]); return { id: requestId };
  },
  'verification.photo': async (ctx,p) => {
    keys(p,['uploadId']); const upload = await row(ctx, 'uploads','id',p.uploadId); ensure(upload.owner_id === ctx.actor.id && upload.status === 'ready',403,'FORBIDDEN','Fotoğraf sana ait olmalı.');
    const existing=await ctx.db.get("SELECT id FROM support_requests WHERE user_id=? AND category='Fotoğraf doğrulaması' AND status='open' AND message=?",[ctx.actor.id,`İnceleme için fotoğraf: ${upload.id}`]);if(existing)return{id:existing.id};
    return identityHandlers['support.create'](ctx,{subject:'Fotoğraf doğrulaması',message:`İnceleme için fotoğraf: ${upload.id}`,category:'Fotoğraf doğrulaması'});
  },
  'account.delete': async (ctx,p) => {
    keys(p,[]);
    const pending = await ctx.db.get("SELECT * FROM deletion_requests WHERE user_id=? AND status='pending'",[ctx.actor.id]);
    if (!pending) await ctx.db.run('INSERT INTO deletion_requests(user_id,status,execute_after,created_at) VALUES(?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET status=?,execute_after=?', [ctx.actor.id,'pending',ctx.now+ctx.config.deletionDelay,ctx.now,'pending',ctx.now+ctx.config.deletionDelay]);
    await audit(ctx,'account.deletion.request',ctx.actor.id); return {status:'pending',executeAfter: pending ? Number(pending.execute_after) : ctx.now+ctx.config.deletionDelay};
  },
  'account.delete.cancel': async (ctx,p) => { keys(p,[]); await ctx.db.run("UPDATE deletion_requests SET status='cancelled' WHERE user_id=? AND status='pending'",[ctx.actor.id]); return {status:'cancelled'}; },
  'notification.read': async (ctx,p) => { keys(p,['notificationId']); if (p.notificationId) await ctx.db.run('UPDATE notifications SET read_at=? WHERE id=? AND user_id=?',[ctx.now,p.notificationId,ctx.actor.id]); else await ctx.db.run('UPDATE notifications SET read_at=? WHERE user_id=? AND read_at IS NULL',[ctx.now,ctx.actor.id]); return {read:true}; },
  'device.register': async (ctx,p) => {
    keys(p,['platform','token']); const platform = choice(p.platform,['ios','android'],'Platform'), token = text(p.token,'Cihaz tokenı',16,4096);
    const old = await ctx.db.get('SELECT * FROM push_devices WHERE token=?',[token]);
    if (old) await ctx.db.run('UPDATE push_devices SET user_id=?,platform=?,updated_at=? WHERE id=?',[ctx.actor.id,platform,ctx.now,old.id]);
    else await ctx.db.run('INSERT INTO push_devices(id,user_id,platform,token,updated_at) VALUES(?,?,?,?,?)',[id(),ctx.actor.id,platform,token,ctx.now]); return {registered:true};
  },
  'device.remove': async(ctx,p)=>{ keys(p,['token']); await ctx.db.run('DELETE FROM push_devices WHERE token=? AND user_id=?',[p.token,ctx.actor.id]); return {removed:true}; }
};
