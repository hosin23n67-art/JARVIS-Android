package com.kamyab.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
    private lateinit var sr: SpeechRecognizer
    private lateinit var si: Intent
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var input: EditText
    private lateinit var orb: TextView
    private lateinit var mic: Button
    private var waiting = false
    private var listening = true
    private var micOn = true
    private val cyan = Color.rgb(65,230,255)
    private val soft = Color.rgb(135,239,255)
    private val bg = Color.rgb(3,9,17)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this,this)
        ui(); speech(); requestMic()
    }

    private fun startAlwaysOn() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        val i = Intent(this,JarvisForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun round(f:Int,s:Int=cyan,r:Float=22f)=GradientDrawable().apply {
        shape=GradientDrawable.RECTANGLE; setColor(f); cornerRadius=dp(r.toInt()).toFloat(); setStroke(dp(1),s)
    }

    private fun ui() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(20),dp(18),dp(16));setBackgroundColor(bg);gravity=Gravity.CENTER_HORIZONTAL }
        root.addView(TextView(this).apply { text="J.A.R.V.I.S";textSize=29f;setTextColor(cyan);setTypeface(Typeface.DEFAULT,Typeface.BOLD);gravity=Gravity.CENTER })
        root.addView(TextView(this).apply { text="PERSONAL AI ASSISTANT • v1.4 • APP CONTROL FIX";textSize=10f;setTextColor(soft);gravity=Gravity.CENTER;setPadding(0,0,0,dp(14)) })
        orb=TextView(this).apply { text="◉";textSize=100f;gravity=Gravity.CENTER;setTextColor(cyan);setShadowLayer(28f,0f,0f,cyan);background=round(Color.rgb(4,19,29),Color.rgb(31,151,180),100f);setOnClickListener{startListen()} }
        root.addView(orb,LinearLayout.LayoutParams(dp(190),dp(190)).apply{gravity=Gravity.CENTER_HORIZONTAL})
        status=TextView(this).apply { text="ALWAYS ON • منتظر «جارویس»";setTextColor(soft);gravity=Gravity.CENTER;setPadding(0,dp(12),0,dp(12)) }
        root.addView(status,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        val sc=ScrollView(this).apply { background=round(Color.rgb(8,22,34),Color.rgb(21,71,88),18f) }
        log=TextView(this).apply { text="JARVIS › v1.4 آماده است. بازکردن برنامه‌ها اصلاح شد.\n";textSize=15f;setTextColor(Color.WHITE);setPadding(dp(16),dp(14),dp(16),dp(14));textDirection=View.TEXT_DIRECTION_RTL }
        sc.addView(log);root.addView(sc,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        input=EditText(this).apply{hint="مثلاً: واتساپ رو باز کن";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setSingleLine(true)}
        val send=Button(this).apply{text="ارسال";setOnClickListener{val c=input.text.toString().trim();if(c.isNotEmpty()){input.setText("");process(c)}}}
        box.addView(input,LinearLayout.LayoutParams(0,dp(52),1f));box.addView(send,LinearLayout.LayoutParams(dp(90),dp(52)));root.addView(box)
        mic=Button(this).apply{text="🎙 میکروفون روشن — لمس برای قطع";setTextColor(bg);background=round(cyan);setOnClickListener{toggle()}}
        root.addView(mic,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)).apply{topMargin=dp(8)})
        setContentView(root)
    }

    private fun toggle(){micOn=!micOn;if(micOn){listening=true;mic.text="🎙 میکروفون روشن — لمس برای قطع";startListen()}else{listening=false;waiting=false;try{sr.cancel()}catch(_:Exception){};mic.text="🔇 میکروفون خاموش — لمس برای وصل";status.text="MIC OFF"}}

    private fun speech(){
        sr=SpeechRecognizer.createSpeechRecognizer(this)
        si=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR")}
        sr.setRecognitionListener(object:RecognitionListener{
            override fun onReadyForSpeech(p:Bundle?){status.text="ALWAYS ON • در حال شنیدن"}
            override fun onBeginningOfSpeech(){}
            override fun onRmsChanged(v:Float){}
            override fun onBufferReceived(b:ByteArray?){}
            override fun onEndOfSpeech(){}
            override fun onError(e:Int){restart()}
            override fun onPartialResults(p:Bundle?){}
            override fun onEvent(t:Int,p:Bundle?){}
            override fun onResults(r:Bundle?){val x=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();if(x.isNotBlank())handle(x)else restart()}
        })
    }

    private fun handle(x:String){log.append("\nشما › $x\n");val l=x.lowercase();if(waiting){waiting=false;process(x);return};if(l.contains("جارویس")||l.contains("jarvis")){val c=l.replace("جارویس","").replace("jarvis","").trim();if(c.isBlank()){waiting=true;speak("بله؟")}else process(c)}else restart()}
    private fun apps():List<Pair<String,String>>{val i=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);return packageManager.queryIntentActivities(i,PackageManager.MATCH_ALL).map{it.loadLabel(packageManager).toString() to it.activityInfo.packageName}.distinctBy{it.second}}
    private fun norm(s:String)=s.lowercase(Locale.getDefault()).replace('ي','ی').replace('ك','ک').replace("‌","").replace(" ","").replace("برنامه","").replace("اپلیکیشن","").replace("اپ","")
    private fun aliases(q:String):List<String>{val m=mapOf("واتساپ" to listOf("whatsapp"),"واتسآپ" to listOf("whatsapp"),"تلگرام" to listOf("telegram"),"اینستاگرام" to listOf("instagram"),"اینستا" to listOf("instagram"),"یوتیوب" to listOf("youtube"),"کروم" to listOf("chrome"),"گوگل" to listOf("google"),"گالری" to listOf("gallery","photos"),"عکس" to listOf("gallery","photos"),"دوربین" to listOf("camera"),"تنظیمات" to listOf("settings"),"فروشگاه" to listOf("vending","market","store"));return m[q]?:emptyList()}
    private fun app(n:String):Pair<String,String>?{
        val q=norm(n);if(q.isBlank())return null
        val all=apps()
        return all.firstOrNull{norm(it.first)==q}
            ?:all.firstOrNull{norm(it.first).contains(q)||q.contains(norm(it.first))}
            ?:all.firstOrNull{a->aliases(q).any{key->a.second.contains(key,true)||norm(a.first).contains(norm(key))}}
    }
    private fun launch(n:String):Boolean{val a=app(n)?:return false;val i=packageManager.getLaunchIntentForPackage(a.second)?:return false;return try{startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));log.append("\nJARVIS › ${a.first} باز شد.\n");true}catch(_:Exception){false}}
    private fun cleanAppName(c:String):String{var x=c.replace("لطفا","").replace("لطفاً","").replace("برنامه","").replace("اپلیکیشن","").trim();listOf("رو باز کن","را باز کن","بازش کن","باز کن","رو اجرا کن","را اجرا کن","اجراش کن","اجرا کن","بیارش بالا","بیار بالا","رو بیار","را بیار").forEach{x=x.replace(it,"")};return x.trim().trim('،',',','.')}
    private fun youtube(q:String){try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(q))))}catch(_:Exception){}}
    private fun process(cmd:String){val c=cmd.lowercase(Locale.getDefault()).replace('ي','ی').replace('ك','ک').trim();val y=Regex("یوتیوب.*(?:باز کن و|بگرد|جستجو کن|سرچ کن|پیدا کن)\\s*(.+)").find(c);if(y!=null){youtube(y.groupValues[1].replace(Regex("\\s*(رو|را)?\\s*(بیار|باز کن)$"),"").trim());return};val wantsOpen=listOf("باز کن","بازش کن","اجرا کن","اجراش کن","بیار بالا","بیارش بالا").any{c.contains(it)};if(wantsOpen){val n=cleanAppName(c);if(n.isNotBlank()){if(!launch(n))speak("برنامه $n را بین برنامه‌های نصب شده پیدا نکردم");return}};val a=when{c.contains("سلام")->"سلام. جارویس در خدمت شماست.";c.contains("ساعت")->"الان ساعت ${SimpleDateFormat("HH:mm",Locale("fa","IR")).format(Date())} است.";else->"دستور شما دریافت شد: $cmd"};log.append("\nJARVIS › $a\n");speak(a)}
    private fun speak(x:String){val resume=micOn;listening=false;try{sr.cancel()}catch(_:Exception){};tts.speak(x,TextToSpeech.QUEUE_FLUSH,null,"jarvis");android.os.Handler(mainLooper).postDelayed({if(resume&&micOn){listening=true;startListen()}},1800L+x.length*35L)}
    private fun requestMic(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){startAlwaysOn();startListen()}else ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),100)}
    override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==100&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED){startAlwaysOn();startListen()}}
    private fun startListen(){if(!micOn||!listening||ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return;try{sr.startListening(si)}catch(_:Exception){}}
    private fun restart(){if(micOn&&listening)android.os.Handler(mainLooper).postDelayed({startListen()},650)}
    override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS){tts.language=Locale("fa","IR");tts.setSpeechRate(.95f)}}
    override fun onDestroy(){try{sr.destroy()}catch(_:Exception){};tts.stop();tts.shutdown();super.onDestroy()}
}
