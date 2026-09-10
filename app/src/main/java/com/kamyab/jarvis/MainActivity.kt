package com.kamyab.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var input: EditText
    private lateinit var orb: TextView
    private lateinit var micButton: Button
    private var waitingForCommand = false
    private var keepListening = true
    private var micEnabled = true
    private val cyan = Color.rgb(65,230,255); private val cyanSoft=Color.rgb(135,239,255); private val bg=Color.rgb(3,9,17); private val panel=Color.rgb(8,22,34)

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); window.statusBarColor=bg; window.navigationBarColor=bg; tts=TextToSpeech(this,this); buildUi(); setupSpeech(); requestMicAndStart() }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun rounded(fill:Int,stroke:Int=cyan,radius:Float=22f)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;setColor(fill);cornerRadius=dp(radius.toInt()).toFloat();setStroke(dp(1),stroke)}
    private fun label(s:String)=TextView(this).apply{text=s;textSize=11f;setTextColor(cyanSoft);gravity=Gravity.CENTER;setPadding(dp(10),dp(7),dp(10),dp(7));background=rounded(Color.rgb(6,27,39),Color.rgb(25,105,125),14f)}

    private fun buildUi(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(20),dp(18),dp(16));setBackgroundColor(bg);gravity=Gravity.CENTER_HORIZONTAL}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}; val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        brand.addView(TextView(this).apply{text="J.A.R.V.I.S";textSize=29f;setTextColor(cyan);setTypeface(Typeface.DEFAULT,Typeface.BOLD);letterSpacing=.18f});brand.addView(TextView(this).apply{text="PERSONAL AI ASSISTANT  •  v1.2";textSize=9f;setTextColor(Color.rgb(90,150,170))});top.addView(brand,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));top.addView(label("●  ONLINE"));root.addView(top,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        val chips=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(0,dp(18),0,dp(12))};listOf("AI CORE READY","APP CONTROL","FA-IR").forEachIndexed{i,s->val p=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);if(i>0)p.marginStart=dp(7);chips.addView(label(s),p)};root.addView(chips,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        orb=TextView(this).apply{text="◉";textSize=100f;gravity=Gravity.CENTER;setTextColor(cyan);setShadowLayer(28f,0f,0f,cyan);background=rounded(Color.rgb(4,19,29),Color.rgb(31,151,180),100f);setOnClickListener{if(micEnabled)startListening()}};root.addView(orb,LinearLayout.LayoutParams(dp(190),dp(190)).apply{gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=dp(12)})
        status=TextView(this).apply{text="LISTENING  •  منتظر فرمان شما";textSize=13f;setTextColor(cyanSoft);gravity=Gravity.CENTER;setTypeface(Typeface.DEFAULT,Typeface.BOLD);setPadding(0,dp(4),0,dp(12))};root.addView(status,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        val scroll=ScrollView(this).apply{background=rounded(panel,Color.rgb(21,71,88),18f)};log=TextView(this).apply{text="JARVIS  ›  آماده‌ام. بگو «جارویس».\n";textSize=15f;setTextColor(Color.rgb(220,246,250));setPadding(dp(16),dp(14),dp(16),dp(14));textDirection=View.TEXT_DIRECTION_RTL};scroll.addView(log);root.addView(scroll,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(7),dp(7),dp(7));background=rounded(Color.rgb(7,18,28),Color.rgb(25,95,115),20f)};input=EditText(this).apply{hint="فرمانت را بنویس...";setHintTextColor(Color.rgb(85,128,140));setTextColor(Color.WHITE);setSingleLine(true);background=null;textDirection=View.TEXT_DIRECTION_RTL};val send=Button(this).apply{text="ارسال ›";setTextColor(bg);background=rounded(cyan,cyan,16f);setOnClickListener{val c=input.text.toString().trim();if(c.isNotEmpty()){input.setText("");processCommand(c)}}};box.addView(input,LinearLayout.LayoutParams(0,dp(48),1f));box.addView(send,LinearLayout.LayoutParams(dp(92),dp(48)));root.addView(box,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{topMargin=dp(12)})
        micButton=Button(this).apply{text="🎙  میکروفون روشن — لمس برای قطع";textSize=14f;setTextColor(bg);background=rounded(cyan,cyan,18f);setOnClickListener{toggleMicrophone()}};root.addView(micButton,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)).apply{topMargin=dp(9)});setContentView(root)
    }

    private fun toggleMicrophone(){micEnabled=!micEnabled;if(micEnabled){keepListening=true;micButton.text="🎙  میکروفون روشن — لمس برای قطع";micButton.setTextColor(bg);micButton.background=rounded(cyan,cyan,18f);startListening()}else{keepListening=false;waitingForCommand=false;try{speechRecognizer.cancel()}catch(_:Exception){};micButton.text="🔇  میکروفون خاموش — لمس برای وصل";micButton.setTextColor(cyanSoft);micButton.background=rounded(Color.rgb(35,18,24),Color.rgb(180,70,85),18f);setState("MIC OFF  •  میکروفون خاموش",false)}}
    private fun setState(s:String,active:Boolean=true){status.text=s;orb.setTextColor(if(active)cyan else Color.rgb(80,135,150))}
    private fun setupSpeech(){speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this);speechIntent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)};speechRecognizer.setRecognitionListener(object:RecognitionListener{override fun onReadyForSpeech(p:Bundle?){if(micEnabled)setState("LISTENING  •  در حال شنیدن")};override fun onBeginningOfSpeech(){};override fun onRmsChanged(v:Float){};override fun onBufferReceived(b:ByteArray?){};override fun onEndOfSpeech(){};override fun onError(e:Int){restartListening()};override fun onPartialResults(p:Bundle?){};override fun onEvent(t:Int,p:Bundle?){};override fun onResults(r:Bundle?){if(!micEnabled)return;val text=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();if(text.isNotBlank())handleSpeech(text)else restartListening()}})}
    private fun handleSpeech(text:String){append("شما  ›  $text");val l=text.lowercase(Locale.getDefault());if(waitingForCommand){waitingForCommand=false;processCommand(text);return};if(l.contains("جارویس")||l.contains("jarvis")){val c=l.replace("جارویس","").replace("jarvis","").trim();if(c.isBlank()){waitingForCommand=true;speak("بله؟")}else processCommand(c)}else restartListening()}

    private fun installedApps():List<Pair<String,String>> { val i=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);return packageManager.queryIntentActivities(i,PackageManager.MATCH_ALL).map{it.loadLabel(packageManager).toString() to it.activityInfo.packageName}.distinctBy{it.second} }
    private fun normalize(s:String)=s.lowercase(Locale.getDefault()).replace("‌","").replace(" ","").replace("اپلیکیشن","").replace("برنامه","")
    private fun findApp(name:String):Pair<String,String>? { val n=normalize(name);val aliases=mapOf("تلگرام" to listOf("telegram","تلگرام"),"واتساپ" to listOf("whatsapp","واتساپ"),"اینستاگرام" to listOf("instagram","اینستا","اینستاگرام"),"یوتیوب" to listOf("youtube","یوتیوب"),"کروم" to listOf("chrome","کروم"));return installedApps().firstOrNull{(label,pkg)->normalize(label).contains(n)||n.contains(normalize(label))||aliases[n]?.any{a->normalize(label).contains(normalize(a))||pkg.contains(a,true)}==true} }
    private fun launchApp(name:String):Boolean { val app=findApp(name)?:return false;val intent=packageManager.getLaunchIntentForPackage(app.second)?:return false;return try{startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));true}catch(_:Exception){false} }
    private fun webSearch(query:String){startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(query))))}
    private fun youtubeSearch(query:String){val u=Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(query));val i=Intent(Intent.ACTION_VIEW,u);try{startActivity(i)}catch(_:Exception){webSearch(query)}}
    private fun searchInApp(appName:String,query:String):Boolean { val app=findApp(appName)?:return false; if(normalize(appName).contains("یوتیوب")||app.second.contains("youtube",true)){youtubeSearch(query);return true}; val intent=Intent(Intent.ACTION_WEB_SEARCH).apply{setPackage(app.second);putExtra("query",query);addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)};return try{startActivity(intent);true}catch(_:Exception){launchApp(appName);false} }

    private fun processCommand(command:String){setState("THINKING  •  پردازش دستور");val c=command.lowercase(Locale.getDefault()).trim()
        val openRegex=Regex("(?:برنامه\\s+)?(.+?)\\s*(?:رو|را)?\\s*(?:باز کن|اجرا کن)$")
        val searchRegex=Regex("(?:تو|داخل|در)\\s+(.+?)\\s+(?:باز کن و|بگرد|جستجو کن|سرچ کن|پیدا کن)\\s*(?:برای|دنبال)?\\s*(.+)")
        val youtubeRegex=Regex("(?:یوتیوب).*(?:باز کن و|بگرد|جستجو کن|سرچ کن|پیدا کن)\\s*(?:برای|دنبال)?\\s*(.+)")
        val ym=youtubeRegex.find(c);if(ym!=null){youtubeSearch(ym.groupValues[1]);append("JARVIS  ›  جست‌وجوی یوتیوب باز شد.");return}
        val sm=searchRegex.find(c);if(sm!=null){val app=sm.groupValues[1].trim();val q=sm.groupValues[2].trim();searchInApp(app,q);append("JARVIS  ›  $app برای «$q» باز شد.");return}
        val om=openRegex.find(c);if(om!=null){val name=om.groupValues[1].trim();val ok=launchApp(name);val a=if(ok)"$name باز شد." else "برنامه $name را بین برنامه‌های نصب‌شده پیدا نکردم.";append("JARVIS  ›  $a");if(!ok)speak(a);return}
        val answer=when{c.contains("سلام")->"سلام. جارویس در خدمت شماست.";c.contains("ساعت")->"الان ساعت ${SimpleDateFormat("HH:mm",Locale("fa","IR")).format(Date())} است.";c.contains("اسمت")||c.contains("کی هستی")->"من جارویس هستم، دستیار صوتی شما.";else->"دستور شما دریافت شد: $command"};append("JARVIS  ›  $answer");speak(answer)
    }
    private fun speak(text:String){setState("SPEAKING  •  در حال پاسخ");val resume=micEnabled;keepListening=false;try{speechRecognizer.cancel()}catch(_:Exception){};tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"jarvis");android.os.Handler(mainLooper).postDelayed({if(resume&&micEnabled){keepListening=true;startListening()}},1800L+text.length*35L)}
    private fun append(s:String){log.append("\n$s\n")};private fun requestMicAndStart(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startListening()else ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),100)}
    override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==100&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED&&micEnabled)startListening()}
    private fun startListening(){if(!micEnabled||!keepListening||ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return;try{speechRecognizer.startListening(speechIntent)}catch(_:Exception){}}
    private fun restartListening(){if(micEnabled&&keepListening)android.os.Handler(mainLooper).postDelayed({startListening()},650)}
    override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS){tts.language=Locale("fa","IR");tts.setSpeechRate(.95f)}}
    override fun onDestroy(){micEnabled=false;keepListening=false;speechRecognizer.destroy();tts.stop();tts.shutdown();super.onDestroy()}
}
