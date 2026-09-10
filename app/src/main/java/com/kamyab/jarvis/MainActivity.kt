package com.kamyab.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:AppCompatActivity(),TextToSpeech.OnInitListener{
 private lateinit var sr:SpeechRecognizer;private lateinit var si:Intent;private lateinit var tts:TextToSpeech;private lateinit var status:TextView;private lateinit var log:TextView;private lateinit var input:EditText;private lateinit var mic:Button
 private var listening=true;private var micOn=true;private var torch=false
 private val cyan=Color.rgb(65,230,255);private val bg=Color.rgb(2,8,16);private val panel=Color.rgb(6,24,36)
 override fun onCreate(b:Bundle?){super.onCreate(b);window.statusBarColor=bg;window.navigationBarColor=bg;tts=TextToSpeech(this,this);ui();speech();permissions()}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun shape(f:Int,s:Int=cyan,r:Float=20f)=GradientDrawable().apply{setColor(f);cornerRadius=dp(r.toInt()).toFloat();setStroke(dp(1),s)}
 private fun ui(){val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(14));setBackgroundColor(bg)};root.addView(TextView(this).apply{text="J.A.R.V.I.S";textSize=31f;setTextColor(cyan);gravity=Gravity.CENTER;setTypeface(Typeface.DEFAULT,Typeface.BOLD);setShadowLayer(18f,0f,0f,cyan)});root.addView(TextView(this).apply{text="NEURAL COMMAND HUD • v1.7";textSize=10f;setTextColor(Color.rgb(140,235,255));gravity=Gravity.CENTER});val hud=TextView(this).apply{text="◉\nCORE ONLINE";textSize=43f;gravity=Gravity.CENTER;setTextColor(cyan);setShadowLayer(25f,0f,0f,cyan);background=shape(Color.rgb(3,20,31),Color.rgb(30,145,180),100f);setOnClickListener{startListen()}};root.addView(hud,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(185)).apply{topMargin=dp(10)});status=TextView(this).apply{text="● READY | VOICE | CONTACTS | MEMORY";textSize=11f;setTextColor(cyan);gravity=Gravity.CENTER;setPadding(0,dp(7),0,dp(7))};root.addView(status);val quick=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("📷 دوربین" to{camera()},"🔦 چراغ" to{toggleTorch()},"⚙ تنظیمات" to{startActivity(Intent(Settings.ACTION_SETTINGS))}).forEach{(t,a)->quick.addView(Button(this).apply{text=t;textSize=11f;setOnClickListener{a()}},LinearLayout.LayoutParams(0,dp(47),1f))};root.addView(quick);val sc=ScrollView(this).apply{background=shape(panel,Color.rgb(20,80,100),16f)};log=TextView(this).apply{text="JARVIS › v1.7 آماده است.\n";textSize=14f;setTextColor(Color.WHITE);setPadding(dp(14),dp(12),dp(14),dp(12));textDirection=View.TEXT_DIRECTION_RTL};sc.addView(log);root.addView(sc,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f).apply{topMargin=dp(7)});val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};input=EditText(this).apply{hint="فرمان فارسی یا سؤال...";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setSingleLine(true)};val send=Button(this).apply{text="ارسال";setOnClickListener{input.text.toString().trim().takeIf{it.isNotEmpty()}?.let{input.setText("");handle(it)}}};row.addView(input,LinearLayout.LayoutParams(0,dp(52),1f));row.addView(send,LinearLayout.LayoutParams(dp(86),dp(52)));root.addView(row);mic=Button(this).apply{text="🎙 میکروفون روشن";setTextColor(bg);background=shape(cyan);setOnClickListener{micOn=!micOn;listening=micOn;text=if(micOn)"🎙 میکروفون روشن" else "🔇 میکروفون خاموش";if(micOn)startListen()else try{sr.cancel()}catch(_:Exception){}}};root.addView(mic,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)).apply{topMargin=dp(6)});setContentView(root)}
 private fun speech(){sr=SpeechRecognizer.createSpeechRecognizer(this);si=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR")};sr.setRecognitionListener(object:RecognitionListener{override fun onReadyForSpeech(p:Bundle?){status.text="● LISTENING"};override fun onBeginningOfSpeech(){};override fun onRmsChanged(v:Float){};override fun onBufferReceived(b:ByteArray?){};override fun onEndOfSpeech(){};override fun onError(e:Int){restart()};override fun onPartialResults(p:Bundle?){};override fun onEvent(t:Int,p:Bundle?){};override fun onResults(r:Bundle?){val x=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();if(x.isNotBlank())handle(x)else restart()}})}
 private fun clean(s:String)=s.lowercase(Locale.getDefault()).replace('ي','ی').replace('ك','ک').replace("جارویس","").replace("jarvis","",true).trim()
 private fun handle(raw:String){log.append("\nشما › $raw\n");val c=clean(raw);val parts=c.split(Regex("\\s+(?:و بعد|بعدش|سپس|و سپس)\\s+|\\s+و\\s+(?=(?:یوتیوب|گوگل|دوربین|گالری|چراغ|تنظیمات|برنامه))"));if(parts.size>1){parts.filter{it.isNotBlank()}.forEachIndexed{i,p->android.os.Handler(mainLooper).postDelayed({process(p)},i*900L)};return};process(c)}
 private fun contact(name:String):String?{if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)return null;val cols=arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,cols,null,null,null)?.use{c->val ni=c.getColumnIndex(cols[1]);val pi=c.getColumnIndex(cols[0]);while(c.moveToNext()){val n=c.getString(ni)?:"";if(clean(n).contains(clean(name))||clean(name).contains(clean(n)))return c.getString(pi)}};return null}
 private fun call(name:String){val n=contact(name);if(n==null){speak("مخاطب $name پیدا نشد یا اجازه مخاطبین داده نشده");return};startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(n))))}
 private fun sms(name:String,text:String){val n=contact(name);if(n==null){speak("مخاطب $name پیدا نشد");return};startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+Uri.encode(n))).apply{putExtra("sms_body",text)})}
 private fun remember(k:String,v:String){getSharedPreferences("jarvis_memory",MODE_PRIVATE).edit().putString(clean(k),v).apply();speak("یادم ماند")}
 private fun recall(k:String)=getSharedPreferences("jarvis_memory",MODE_PRIVATE).getString(clean(k),null)
 private fun openUrl(u:String)=try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(u)));true}catch(_:Exception){false}
 private fun camera(){try{startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))}catch(_:Exception){speak("دوربین پیدا نشد")}}
 private fun gallery(){try{startActivity(Intent(Intent.ACTION_VIEW).apply{type="image/*"})}catch(_:Exception){speak("گالری پیدا نشد")}}
 private fun toggleTorch(){try{val cm=getSystemService(CAMERA_SERVICE) as CameraManager;val id=cm.cameraIdList.firstOrNull{cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)==true}?:return;torch=!torch;cm.setTorchMode(id,torch);speak(if(torch)"چراغ قوه روشن شد" else "چراغ قوه خاموش شد")}catch(_:Exception){speak("کنترل چراغ ممکن نشد")}}
 private fun apps():List<Pair<String,String>>{val i=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);return packageManager.queryIntentActivities(i,PackageManager.MATCH_ALL).map{it.loadLabel(packageManager).toString() to it.activityInfo.packageName}.distinctBy{it.second}}
 private fun norm(s:String)=clean(s).replace("‌","").replace(" ","").replace("برنامه","").replace("اپلیکیشن","").replace("اپ","")
 private fun launch(n:String):Boolean{val q=norm(n);val aliases=mapOf("واتساپ" to "whatsapp","تلگرام" to "telegram","اینستا" to "instagram","اینستاگرام" to "instagram","یوتیوب" to "youtube","کروم" to "chrome");val a=apps().firstOrNull{norm(it.first)==q}?:apps().firstOrNull{norm(it.first).contains(q)||q.contains(norm(it.first))}?:aliases[q]?.let{k->apps().firstOrNull{it.second.contains(k,true)}}?:return false;return try{startActivity(packageManager.getLaunchIntentForPackage(a.second)!!);true}catch(_:Exception){false}}
 private fun strip(c:String,vararg w:String):String{var x=c;w.forEach{x=x.replace(it,"")};return x.trim().trim('،',',','.')}
 private fun process(c0:String){val c=clean(c0);when{
  c.startsWith("یادت باشه ")&&c.contains(" یعنی ")->{val a=c.removePrefix("یادت باشه ").split(" یعنی ",limit=2);remember(a[0],a[1])}
  c.startsWith("یادت باشه ")-> {val x=c.removePrefix("یادت باشه ");remember(x,x)}
  c.startsWith("یادت میاد ")||c.startsWith("یادت هست ")->{val k=strip(c,"یادت میاد","یادت هست","؟","?");speak(recall(k)?:"چیزی درباره $k در حافظه ندارم")}
  c.startsWith("به ")&&c.contains("زنگ بزن")->call(strip(c.removePrefix("به "),"زنگ بزن","رو","را"))
  c.startsWith("به ")&&(c.contains("پیام بده")||c.contains("پیامک بفرست"))->{val re=Regex("به\\s+(.+?)\\s+(?:پیام بده|پیامک بفرست)(?:\\s+(?:که|بگو))?\\s*(.*)").find(c);if(re!=null)sms(re.groupValues[1].trim(),re.groupValues[2].trim())else speak("نام مخاطب و متن پیام را بگو")}
  c.contains("چراغ")&&(c.contains("روشن")||c.contains("خاموش")||c.contains("قوه"))->toggleTorch()
  c.contains("دوربین")&&c.contains("باز")->camera();c.contains("گالری")&&c.contains("باز")->gallery();c.contains("تنظیمات")&&c.contains("باز")->startActivity(Intent(Settings.ACTION_SETTINGS))
  c.contains("ساعت")->speak("الان ساعت ${SimpleDateFormat("HH:mm",Locale("fa","IR")).format(Date())} است");c.contains("تاریخ")->speak("امروز ${SimpleDateFormat("yyyy/MM/dd",Locale("fa","IR")).format(Date())} است")
  c.contains("یوتیوب")&&(c.contains("سرچ")||c.contains("جستجو")||c.contains("پیدا"))->{val q=strip(c,"یوتیوب","تو","در","سرچ کن","جستجو کن","پیدا کن","رو","را");openUrl("https://www.youtube.com/results?search_query="+Uri.encode(q))}
  c.contains("گوگل")&&(c.contains("سرچ")||c.contains("جستجو")||c.contains("پیدا"))->{val q=strip(c,"گوگل","تو","در","سرچ کن","جستجو کن","پیدا کن","رو","را");openUrl("https://www.google.com/search?q="+Uri.encode(q))}
  c.contains("نقشه")||c.contains("آدرس")->openUrl("geo:0,0?q="+Uri.encode(strip(c,"نقشه","آدرس","باز کن","نشون بده","نشان بده","رو","را")))
  listOf("باز کن","بازش کن","اجرا کن","بیار بالا").any{c.contains(it)}->{val n=strip(c,"لطفا","لطفاً","برنامه","اپلیکیشن","باز کن","بازش کن","اجرا کن","بیار بالا","رو","را");if(!launch(n))speak("برنامه $n پیدا نشد")}
  c.contains("سلام")->speak("سلام. جارویس نسخه یک هفت در خدمت شماست")
  else->{status.text="● SEARCHING";openUrl("https://www.google.com/search?q="+Uri.encode(c))}
 }}
 private fun speak(x:String){log.append("\nJARVIS › $x\n");val resume=micOn;listening=false;try{sr.cancel()}catch(_:Exception){};tts.speak(x,TextToSpeech.QUEUE_FLUSH,null,"jarvis");android.os.Handler(mainLooper).postDelayed({if(resume&&micOn){listening=true;startListen()}},1600L+x.length*28L)}
 private fun permissions(){val p=mutableListOf<String>();if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)p+=Manifest.permission.RECORD_AUDIO;if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)p+=Manifest.permission.READ_CONTACTS;if(p.isNotEmpty())ActivityCompat.requestPermissions(this,p.toTypedArray(),100)else startListen()}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==100)startListen()}
 private fun startListen(){if(!micOn||!listening||ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return;try{sr.startListening(si)}catch(_:Exception){}}
 private fun restart(){if(micOn&&listening)android.os.Handler(mainLooper).postDelayed({startListen()},650)}
 override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS){tts.language=Locale("fa","IR");tts.setSpeechRate(.95f)}}
 override fun onDestroy(){try{sr.destroy()}catch(_:Exception){};tts.stop();tts.shutdown();super.onDestroy()}
}
