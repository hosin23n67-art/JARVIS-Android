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

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
 private lateinit var sr:SpeechRecognizer; private lateinit var si:Intent; private lateinit var tts:TextToSpeech
 private lateinit var status:TextView; private lateinit var log:TextView; private lateinit var input:EditText; private lateinit var mic:Button
 private var listening=true; private var micOn=true; private var torch=false
 private val cyan=Color.rgb(65,230,255); private val bg=Color.rgb(2,8,16); private val panel=Color.rgb(6,24,36)
 override fun onCreate(b:Bundle?){super.onCreate(b);window.statusBarColor=bg;window.navigationBarColor=bg;tts=TextToSpeech(this,this);ui();speech();requestMic()}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun shape(fill:Int,stroke:Int=cyan,r:Float=20f)=GradientDrawable().apply{setColor(fill);cornerRadius=dp(r.toInt()).toFloat();setStroke(dp(1),stroke)}
 private fun ui(){
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(14));setBackgroundColor(bg)}
  root.addView(TextView(this).apply{text="J.A.R.V.I.S";textSize=31f;setTextColor(cyan);gravity=Gravity.CENTER;setTypeface(Typeface.DEFAULT,Typeface.BOLD);setShadowLayer(18f,0f,0f,cyan)})
  root.addView(TextView(this).apply{text="TACTICAL HUD • v1.6 • COMMAND CENTER";textSize=10f;setTextColor(Color.rgb(140,235,255));gravity=Gravity.CENTER})
  val hud=TextView(this).apply{text="◉\nSYSTEM ONLINE";textSize=46f;gravity=Gravity.CENTER;setTextColor(cyan);setShadowLayer(25f,0f,0f,cyan);background=shape(Color.rgb(3,20,31),Color.rgb(30,145,180),100f);setOnClickListener{startListen()}}
  root.addView(hud,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(190)).apply{topMargin=dp(12)})
  status=TextView(this).apply{text="● READY  |  MIC ONLINE  |  FA-IR";textSize=12f;setTextColor(cyan);gravity=Gravity.CENTER;setPadding(0,dp(8),0,dp(8))};root.addView(status)
  val quick=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
  listOf("📷 دوربین" to {camera()},"🔦 چراغ" to {toggleTorch()},"⚙ تنظیمات" to {startActivity(Intent(Settings.ACTION_SETTINGS))}).forEach{(t,a)->quick.addView(Button(this).apply{text=t;textSize=11f;setOnClickListener{a()}},LinearLayout.LayoutParams(0,dp(48),1f))};root.addView(quick)
  val sc=ScrollView(this).apply{background=shape(panel,Color.rgb(20,80,100),16f)};log=TextView(this).apply{text="JARVIS › v1.6 آنلاین است. فرمان بدهید.\n";textSize=14f;setTextColor(Color.WHITE);setPadding(dp(14),dp(12),dp(14),dp(12));textDirection=View.TEXT_DIRECTION_RTL};sc.addView(log);root.addView(sc,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f).apply{topMargin=dp(8)})
  val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};input=EditText(this).apply{hint="مثلاً: در گوگل هواشناسی رودان رو سرچ کن";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setSingleLine(true)};val send=Button(this).apply{text="ارسال";setOnClickListener{input.text.toString().trim().takeIf{it.isNotEmpty()}?.let{input.setText("");process(it)}}};row.addView(input,LinearLayout.LayoutParams(0,dp(52),1f));row.addView(send,LinearLayout.LayoutParams(dp(86),dp(52)));root.addView(row)
  mic=Button(this).apply{text="🎙 میکروفون روشن";setTextColor(bg);background=shape(cyan);setOnClickListener{micOn=!micOn;listening=micOn;if(micOn){text="🎙 میکروفون روشن";startListen()}else{text="🔇 میکروفون خاموش";try{sr.cancel()}catch(_:Exception){}}}};root.addView(mic,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)).apply{topMargin=dp(6)});setContentView(root)
 }
 private fun speech(){sr=SpeechRecognizer.createSpeechRecognizer(this);si=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR")};sr.setRecognitionListener(object:RecognitionListener{override fun onReadyForSpeech(p:Bundle?){status.text="● LISTENING  |  FA-IR"};override fun onBeginningOfSpeech(){};override fun onRmsChanged(v:Float){};override fun onBufferReceived(b:ByteArray?){};override fun onEndOfSpeech(){};override fun onError(e:Int){restart()};override fun onPartialResults(p:Bundle?){};override fun onEvent(t:Int,p:Bundle?){};override fun onResults(r:Bundle?){val x=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();if(x.isNotBlank()){log.append("\nشما › $x\n");process(x.replace("جارویس","").replace("jarvis","",true).trim())}else restart()}})}
 private fun clean(s:String)=s.lowercase(Locale.getDefault()).replace('ي','ی').replace('ك','ک').trim()
 private fun openUrl(u:String)=try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(u)));true}catch(_:Exception){false}
 private fun camera(){try{startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))}catch(_:Exception){speak("دوربین پیدا نشد")}}
 private fun gallery(){try{startActivity(Intent(Intent.ACTION_VIEW).apply{type="image/*"})}catch(_:Exception){speak("گالری پیدا نشد")}}
 private fun toggleTorch(){try{val cm=getSystemService(CAMERA_SERVICE) as CameraManager;val id=cm.cameraIdList.firstOrNull{cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)==true}?:return;torch=!torch;cm.setTorchMode(id,torch);speak(if(torch)"چراغ قوه روشن شد" else "چراغ قوه خاموش شد")}catch(_:Exception){speak("کنترل چراغ قوه ممکن نشد")}}
 private fun apps():List<Pair<String,String>>{val i=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);return packageManager.queryIntentActivities(i,PackageManager.MATCH_ALL).map{it.loadLabel(packageManager).toString() to it.activityInfo.packageName}.distinctBy{it.second}}
 private fun norm(s:String)=clean(s).replace("‌","").replace(" ","").replace("برنامه","").replace("اپلیکیشن","").replace("اپ","")
 private fun aliases(q:String)=mapOf("واتساپ" to listOf("whatsapp"),"تلگرام" to listOf("telegram"),"اینستاگرام" to listOf("instagram"),"اینستا" to listOf("instagram"),"یوتیوب" to listOf("youtube"),"کروم" to listOf("chrome"),"گالری" to listOf("gallery","photos"))[q]?:emptyList()
 private fun launch(n:String):Boolean{val q=norm(n);val a=apps().firstOrNull{norm(it.first)==q}?:apps().firstOrNull{norm(it.first).contains(q)||q.contains(norm(it.first))}?:apps().firstOrNull{x->aliases(q).any{x.second.contains(it,true)}}?:return false;return try{startActivity(packageManager.getLaunchIntentForPackage(a.second)!!);true}catch(_:Exception){false}}
 private fun strip(c:String,vararg words:String):String{var x=c;words.forEach{x=x.replace(it,"")};return x.trim().trim('،',',','.')}
 private fun process(raw:String){val c=clean(raw);when{
  c.isBlank()->restart()
  c.contains("چراغ")&&(c.contains("روشن")||c.contains("خاموش")||c.contains("قوه"))->toggleTorch()
  c.contains("دوربین")&&c.contains("باز")->camera()
  c.contains("گالری")&&c.contains("باز")->gallery()
  c.contains("تنظیمات")&&c.contains("باز")->startActivity(Intent(Settings.ACTION_SETTINGS))
  c.contains("ساعت")->speak("الان ساعت ${SimpleDateFormat("HH:mm",Locale("fa","IR")).format(Date())} است")
  c.contains("تاریخ")->speak("امروز ${SimpleDateFormat("yyyy/MM/dd",Locale("fa","IR")).format(Date())} است")
  c.contains("یوتیوب")&&(c.contains("سرچ")||c.contains("جستجو")||c.contains("پیدا"))-> {val q=strip(c,"یوتیوب","تو","در","سرچ کن","جستجو کن","پیدا کن","رو","را");openUrl("https://www.youtube.com/results?search_query="+Uri.encode(q))}
  c.contains("گوگل")&&(c.contains("سرچ")||c.contains("جستجو")||c.contains("پیدا"))-> {val q=strip(c,"گوگل","تو","در","سرچ کن","جستجو کن","پیدا کن","رو","را");openUrl("https://www.google.com/search?q="+Uri.encode(q))}
  c.contains("نقشه")||c.contains("مپ")||c.contains("آدرس")-> {val q=strip(c,"نقشه","مپ","آدرس","باز کن","نشون بده","نشان بده","رو","را");openUrl("geo:0,0?q="+Uri.encode(q))}
  c.startsWith("به ")&&c.contains("زنگ بزن")-> {val target=strip(c.removePrefix("به "),"زنگ بزن");try{startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(target))))}catch(_:Exception){speak("شماره را پیدا نکردم")}}
  c.contains("پیامک")||c.contains("اس ام اس")-> {val text=strip(c,"پیامک","اس ام اس","بفرست","ارسال کن");try{startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:")).apply{putExtra("sms_body",text)})}catch(_:Exception){speak("برنامه پیامک پیدا نشد")}}
  listOf("باز کن","بازش کن","اجرا کن","بیار بالا").any{c.contains(it)}->{val n=strip(c,"لطفا","لطفاً","برنامه","اپلیکیشن","باز کن","بازش کن","اجرا کن","بیار بالا","رو","را");if(!launch(n))speak("برنامه $n پیدا نشد")}
  c.contains("سلام")->speak("سلام. جارویس نسخه یک شش در خدمت شماست")
  else->speak("این فرمان را هنوز یاد نگرفته‌ام")
 }}
 private fun speak(x:String){log.append("\nJARVIS › $x\n");val resume=micOn;listening=false;try{sr.cancel()}catch(_:Exception){};tts.speak(x,TextToSpeech.QUEUE_FLUSH,null,"jarvis");android.os.Handler(mainLooper).postDelayed({if(resume&&micOn){listening=true;startListen()}},1800L+x.length*30L)}
 private fun requestMic(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startListen()else ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),100)}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==100&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)startListen()}
 private fun startListen(){if(!micOn||!listening||ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return;try{sr.startListening(si)}catch(_:Exception){}}
 private fun restart(){if(micOn&&listening)android.os.Handler(mainLooper).postDelayed({startListen()},650)}
 override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS){tts.language=Locale("fa","IR");tts.setSpeechRate(.95f)}}
 override fun onDestroy(){try{sr.destroy()}catch(_:Exception){};tts.stop();tts.shutdown();super.onDestroy()}
}
