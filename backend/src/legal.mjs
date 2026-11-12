import { id } from './auth.mjs';
import { ensure,keys,text,choice } from './errors.mjs';
import { adminRequired,audit } from './model.mjs';
export async function legalConfiguration(ctx){
 const docs={
   privacy:{url:ctx.config.privacyUrl??null,version:ctx.config.privacyVersion??null},
   terms:{url:ctx.config.termsUrl??null,version:ctx.config.termsVersion??null},
   terms_ios:{url:ctx.config.termsIosUrl??ctx.config.termsUrl??null,version:ctx.config.termsVersion??null},
   terms_android:{url:ctx.config.termsAndroidUrl??ctx.config.termsUrl??null,version:ctx.config.termsVersion??null},
   guidelines:{url:ctx.config.guidelinesUrl??null,version:null},
   deletion:{url:ctx.config.deletionUrl??null,version:null},
   support:{email:ctx.config.supportEmail??null}
 };
 for(const d of await ctx.db.all('SELECT * FROM legal_documents WHERE active=1 ORDER BY published_at,version'))docs[d.kind]={url:d.url,version:d.version};
 return{documents:docs,requiresAcceptance:ctx.config.production,configured:!!(docs.privacy.url&&docs.privacy.version&&docs.terms.url&&docs.terms.version)};
}
export const legalHandlers={
 'legal.accept':async(ctx,p)=>{keys(p,['termsVersion','privacyVersion']);const config=await legalConfiguration(ctx);ensure(config.configured,503,'LEGAL_DOCUMENTS_PENDING','Kullanım koşulları ve gizlilik politikası henüz yayınlanmadı.');ensure(p.termsVersion===config.documents.terms.version&&p.privacyVersion===config.documents.privacy.version,409,'LEGAL_VERSION_CHANGED','Belgeler güncellendi. Tekrar incelemelisin.');await ctx.db.run('INSERT INTO legal_acceptances(id,user_id,terms_version,privacy_version,created_at) VALUES(?,?,?,?,?) ON CONFLICT(user_id,terms_version,privacy_version) DO NOTHING',[id(),ctx.actor.id,p.termsVersion,p.privacyVersion,ctx.now]);await audit(ctx,'legal.accept',ctx.actor.id,{termsVersion:p.termsVersion,privacyVersion:p.privacyVersion});return{accepted:true};},
 'admin.legal.publish':async(ctx,p)=>{adminRequired(ctx);keys(p,['kind','version','url']);const kind=choice(p.kind,['privacy','terms','guidelines','deletion'],'Belge türü'),version=text(p.version,'Sürüm',1,80),url=text(p.url,'URL',8,2000);let parsed;try{parsed=new URL(url);}catch{ensure(false,400,'INVALID_URL','Geçerli HTTPS bağlantısı gerekiyor.');}ensure(parsed.protocol==='https:'&&!parsed.username&&!parsed.password&&!['localhost','127.0.0.1'].includes(parsed.hostname),400,'INVALID_URL','Geçerli HTTPS bağlantısı gerekiyor.');ensure(!await ctx.db.get('SELECT version FROM legal_documents WHERE kind=? AND version=?',[kind,version]),409,'VERSION_EXISTS','Yayınlanan sürümün bağlantısı değiştirilemez. Yeni sürüm oluştur.');await ctx.db.run('UPDATE legal_documents SET active=0 WHERE kind=?',[kind]);await ctx.db.run('INSERT INTO legal_documents(kind,version,url,active,published_at) VALUES(?,?,?,1,?)',[kind,version,url,ctx.now]);await audit(ctx,'admin.legal.publish',kind,{version});return{published:true};}
};
