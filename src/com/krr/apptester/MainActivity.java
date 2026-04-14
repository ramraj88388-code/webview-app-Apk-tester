package com.krr.apptester;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.view.*;
import android.net.Uri;
import android.content.*;
import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import android.util.Base64;
import android.util.TypedValue;
import java.lang.reflect.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;

public class MainActivity extends Activity {
    private FrameLayout root;
    private ScrollView listView;
    private FrameLayout webContainer;
    private WebView webView, popupWebView;
    private long backPressedTime=0;
    private int notifId=100;
    private boolean inWebView=false;
    private String curAppName="";
    private static final String CH="apptester_ch";
    private static final String P="krr_apptester";
    private static final String AK="saved_apps";
    private ValueCallback<Uri[]> fuCb;
    private static final int FC=1001,CP=2001,LP=2002,AC=3001;
    private BroadcastReceiver batRx;
    private int curTab=0; // 0=apps, 1=guide, 2=settings
    private float dp;

    // ═══ SETTINGS DEFAULTS ═══
    private boolean sBiometric=false, sKeepScreen=true, sRefreshBtn=true, sCacheClear=true;
    private boolean sChromeUA=true, sOrientLock=false, sDarkSync=true, sBatSaver=true;
    private boolean sNotif=true, sGPS=true, sCamera=true, sDownBridge=true, sToast=true;
    private int sRefreshSize=38, sSplashMs=800;
    private String sRefreshPos="top-right-30";
    private String sCustomJS="";

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);
        dp=getResources().getDisplayMetrics().density;
        loadSettings();
        if(!sOrientLock)setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        else setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if(Build.VERSION.SDK_INT>=21){getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#0f172a"));getWindow().setNavigationBarColor(Color.parseColor("#0f172a"));}
        createCh();
        if(sBiometric){KeyguardManager km=(KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
            if(km!=null&&km.isKeyguardSecure()){Intent ai=km.createConfirmDeviceCredentialIntent("KRR App Tester","Verify");
                if(ai!=null){startActivityForResult(ai,AC);return;}}}
        initApp();
    }

    private void initApp(){
        
        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            try { if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 4001);
            } catch (Exception e) {}
        }

        root=new FrameLayout(this);root.setBackgroundColor(Color.parseColor("#0f172a"));setContentView(root);
        batRx=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
            if(webView==null||!sBatSaver)return;int l=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);int s=i.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            int p=(int)((l/(float)s)*100);boolean lo=p<=20;
            webView.evaluateJavascript("(function(){window.__BATTERY__={level:"+p+",low:"+lo+"};try{window.dispatchEvent(new CustomEvent('batteryUpdate',{detail:{level:"+p+",low:"+lo+"}}))}catch(e){}})();",null);}};
        registerReceiver(batRx,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        showTab(0);
    }

    // ═══ TAB SWITCHER ═══
    private void showTab(int tab){
        curTab=tab;inWebView=false;root.removeAllViews();
        if(Build.VERSION.SDK_INT>=21){getWindow().setStatusBarColor(Color.parseColor("#0f172a"));getWindow().setNavigationBarColor(Color.parseColor("#0f172a"));}
        LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.VERTICAL);main.setBackgroundColor(Color.parseColor("#0f172a"));
        // Tab bar
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setBackgroundColor(Color.parseColor("#111827"));
        tabs.setPadding((int)(8*dp),(int)(10*dp),(int)(8*dp),(int)(10*dp));
        String[][] tabInfo={{"📱","Apps"},{"📖","Guide"},{"⚙️","Settings"}};
        for(int i=0;i<3;i++){final int ti=i;
            TextView tb=new TextView(this);tb.setText(tabInfo[i][0]+" "+tabInfo[i][1]);tb.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);
            tb.setTypeface(Typeface.DEFAULT_BOLD);tb.setGravity(Gravity.CENTER);tb.setPadding((int)(12*dp),(int)(8*dp),(int)(12*dp),(int)(8*dp));
            GradientDrawable tbg=new GradientDrawable();tbg.setCornerRadius(8*dp);
            if(i==tab){tbg.setColor(Color.parseColor("#6366f1"));tb.setTextColor(Color.WHITE);}
            else{tbg.setColor(Color.TRANSPARENT);tb.setTextColor(Color.parseColor("#64748b"));}
            tb.setBackground(tbg);LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);tlp.setMargins((int)(3*dp),0,(int)(3*dp),0);
            tb.setLayoutParams(tlp);tb.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){showTab(ti);}});tabs.addView(tb);}
        main.addView(tabs);
        // Content
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setBackgroundColor(Color.parseColor("#0f172a"));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding((int)(16*dp),(int)(16*dp),(int)(16*dp),(int)(100*dp));
        if(tab==0)buildAppsTab(content);
        else if(tab==1)buildGuideTab(content);
        else buildSettingsTab(content);
        sv.addView(content);main.addView(sv,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        root.addView(main,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        // FAB
        if(tab==0){TextView fab=new TextView(this);fab.setText("+");fab.setTextColor(Color.WHITE);fab.setTextSize(TypedValue.COMPLEX_UNIT_SP,28);
            fab.setGravity(Gravity.CENTER);fab.setTypeface(Typeface.DEFAULT_BOLD);GradientDrawable fg=new GradientDrawable();fg.setShape(GradientDrawable.OVAL);
            fg.setColor(Color.parseColor("#6366f1"));fab.setBackground(fg);int fs=(int)(56*dp);
            FrameLayout.LayoutParams fl=new FrameLayout.LayoutParams(fs,fs);fl.gravity=Gravity.BOTTOM|Gravity.RIGHT;fl.rightMargin=(int)(20*dp);fl.bottomMargin=(int)(24*dp);
            fab.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){showAddEdit(-1);}});root.addView(fab,fl);}
    }

    // ═══ APPS TAB ═══
    private void buildAppsTab(LinearLayout c){
        JSONArray apps=getApps();
        if(apps.length()==0){
            LinearLayout e=new LinearLayout(this);e.setOrientation(LinearLayout.VERTICAL);e.setGravity(Gravity.CENTER);e.setPadding(0,(int)(60*dp),0,0);
            tv(e,"📱",48,Gravity.CENTER,"#ffffff",0);tv(e,"No apps added yet",16,Gravity.CENTER,"#475569",(int)(12*dp));
            tv(e,"Tap + to add your first web app",12,Gravity.CENTER,"#334155",(int)(4*dp));c.addView(e);return;}
        for(int i=0;i<apps.length();i++){try{c.addView(appCard(apps.getJSONObject(i),i));}catch(Exception ex){}}
    }

    private View appCard(final JSONObject app,final int idx){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding((int)(16*dp),(int)(14*dp),(int)(16*dp),(int)(14*dp));
        GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(14*dp);bg.setColor(Color.parseColor("#111827"));bg.setStroke((int)(1*dp),Color.parseColor("#1e293b"));
        card.setBackground(bg);LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);clp.bottomMargin=(int)(10*dp);card.setLayoutParams(clp);
        String accent=app.optString("accent","#6366f1");
        // Name row
        LinearLayout tr=new LinearLayout(this);tr.setOrientation(LinearLayout.HORIZONTAL);tr.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot=new TextView(this);GradientDrawable dg=new GradientDrawable();dg.setShape(GradientDrawable.OVAL);try{dg.setColor(Color.parseColor(accent));}catch(Exception e){dg.setColor(Color.parseColor("#6366f1"));}
        dot.setBackground(dg);LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams((int)(10*dp),(int)(10*dp));dlp.rightMargin=(int)(10*dp);dot.setLayoutParams(dlp);tr.addView(dot);
        TextView nm=new TextView(this);nm.setText(app.optString("name","App"));nm.setTextColor(Color.WHITE);nm.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);nm.setTypeface(Typeface.DEFAULT_BOLD);
        nm.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));tr.addView(nm);card.addView(tr);
        // URL
        TextView url=new TextView(this);url.setText(app.optString("url",""));url.setTextColor(Color.parseColor("#475569"));url.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);
        url.setPadding(0,(int)(4*dp),0,(int)(10*dp));url.setSingleLine(true);card.addView(url);
        // Buttons
        LinearLayout br=new LinearLayout(this);br.setOrientation(LinearLayout.HORIZONTAL);
        br.addView(mkBtn("▶ Launch",accent,Color.WHITE,new View.OnClickListener(){@Override public void onClick(View v){launchApp(app);}}));
        br.addView(mkBtn("✏️","#1e293b",Color.parseColor("#94a3b8"),new View.OnClickListener(){@Override public void onClick(View v){showAddEdit(idx);}}));
        br.addView(mkBtn("🗑","#1e293b",Color.parseColor("#ef4444"),new View.OnClickListener(){@Override public void onClick(View v){
            new AlertDialog.Builder(MainActivity.this).setTitle("Delete?").setMessage("Remove this app?")
                .setPositiveButton("Delete",new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface d,int w){delApp(idx);showTab(0);}})
                .setNegativeButton("Cancel",null).show();}}));
        card.addView(br);return card;
    }

    // ═══ GUIDE TAB ═══
    private void buildGuideTab(LinearLayout c){
        tv(c,"📖 Web App Compatibility Guide",18,Gravity.LEFT,"#ffffff",0);
        tv(c,"Change these in your web app code for APK to work correctly",12,Gravity.LEFT,"#64748b",(int)(4*dp));
        addSep(c,(int)(16*dp));
        // Google Login
        guideSection(c,"🔐","Google Login","#ef4444",
            "signInWithPopup(auth, provider)","signInWithRedirect(auth, provider)\n\n+ Add in AuthProvider useEffect:\ngetRedirectResult(auth).catch(console.error)");
        // PDF Downloads
        guideSection(c,"📄","PDF Downloads (jsPDF)","#f97316",
            "doc.save(\"filename.pdf\")",
            "if(window.__IS_APK__){\n  var b64=doc.output('datauristring')\n    .split(',')[1];\n  AndroidDownload.save(\n    b64,'filename.pdf',\n    'application/pdf');\n} else doc.save('filename.pdf');");
        // XLSX Downloads
        guideSection(c,"📊","Excel Downloads (SheetJS)","#f97316",
            "XLSX.writeFile(wb, \"file.xlsx\")",
            "if(window.__IS_APK__){\n  var b64=XLSX.write(wb,\n    {bookType:'xlsx',type:'base64'});\n  AndroidDownload.save(\n    b64,'file.xlsx',\n    'application/vnd.openxml...');\n} else XLSX.writeFile(wb,'file.xlsx');");
        // WhatsApp
        guideSection(c,"💬","WhatsApp Links","#22c55e",
            "https://wa.me/?text=...","whatsapp://send?text=...");
        // OAuth popup
        guideSection(c,"🌐","Google Sheets OAuth","#818cf8",
            "window.open(url,'oauth','width=600')","window.location.href = url\n\n+ Handle token from URL hash\non page load (useEffect)");
        addSep(c,(int)(12*dp));
        // Supported links
        tv(c,"🔗 Supported Link Types",14,Gravity.LEFT,"#ffffff",(int)(8*dp));
        String[][] links={{"tel:+919876543210","Opens phone dialer"},{"whatsapp://send?phone=91...","Opens WhatsApp"},
            {"mailto:email@example.com","Opens Gmail"},{"sms:+919876543210","Opens messaging"},{"geo:15.123,76.456","Opens Google Maps"}};
        for(String[] l:links){tv(c,"✓ "+l[0],11,Gravity.LEFT,"#22c55e",(int)(3*dp));tv(c,"   "+l[1],10,Gravity.LEFT,"#475569",0);}
        addSep(c,(int)(12*dp));
        // JS Variables
        tv(c,"📊 JS Variables (auto-injected by APK)",14,Gravity.LEFT,"#ffffff",(int)(8*dp));
        String[] vars={"window.__IS_APK__ = true","window.__APK_VERSION__ = \"1.0\"","window.__THEME__ = \"dark\" / \"light\"",
            "window.__BATTERY__ = {level: 85, low: false}","AndroidDownload.save(base64, filename, mime)","AndroidNotification.show(title, body)"};
        for(String v:vars)tv(c,"• "+v,10,Gravity.LEFT,"#818cf8",(int)(3*dp));
        addSep(c,(int)(12*dp));
        // Don't use
        tv(c,"❌ Don't Use in WebView",14,Gravity.LEFT,"#ef4444",(int)(8*dp));
        String[] donts={"signInWithPopup — blocked by WebView","window.open() for OAuth — use redirect","blob: URLs for downloads — use AndroidDownload bridge",
            "Browser Push API — not supported","https://wa.me/ — use whatsapp:// protocol","XLSX.writeFile() — use XLSX.write + bridge"};
        for(String d:donts)tv(c,"✗ "+d,10,Gravity.LEFT,"#f87171",(int)(3*dp));
        addSep(c,(int)(20*dp));
        // Download PDF button
        LinearLayout pdfBox=new LinearLayout(this);pdfBox.setOrientation(LinearLayout.VERTICAL);pdfBox.setPadding((int)(16*dp),(int)(14*dp),(int)(16*dp),(int)(14*dp));
        GradientDrawable pbg=new GradientDrawable();pbg.setCornerRadius(12*dp);pbg.setColor(Color.parseColor("#111827"));pbg.setStroke((int)(1*dp),Color.parseColor("#1e293b"));pdfBox.setBackground(pbg);
        tv(pdfBox,"📄 Download Full Guide",13,Gravity.CENTER,"#ffffff",(int)(4*dp));
        tv(pdfBox,"Get PDF with all code changes and examples",10,Gravity.CENTER,"#64748b",(int)(4*dp));
        LinearLayout btnRow=new LinearLayout(this);btnRow.setOrientation(LinearLayout.HORIZONTAL);btnRow.setGravity(Gravity.CENTER);
        btnRow.setPadding(0,(int)(10*dp),0,0);
        Button pdfBtn=styledBtn("📥 Download PDF","#6366f1");pdfBtn.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){generateGuidePDF();}});btnRow.addView(pdfBtn);
        Button cpBtn=styledBtn("📋 Copy Text","#1e293b");cpBtn.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){copyGuideText();}});btnRow.addView(cpBtn);
        pdfBox.addView(btnRow);c.addView(pdfBox);
    }

    private void guideSection(LinearLayout c,String icon,String title,String color,String before,String after){
        tv(c,icon+" "+title,14,Gravity.LEFT,color,(int)(12*dp));
        // Before
        tv(c,"BEFORE (broken):",9,Gravity.LEFT,"#ef4444",(int)(6*dp));
        TextView bv=new TextView(this);bv.setText(before);bv.setTextColor(Color.parseColor("#f87171"));bv.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
        bv.setTypeface(Typeface.MONOSPACE);bv.setPadding((int)(10*dp),(int)(8*dp),(int)(10*dp),(int)(8*dp));
        GradientDrawable bbg=new GradientDrawable();bbg.setCornerRadius(6*dp);bbg.setColor(Color.parseColor("#1a0505"));bbg.setStroke((int)(1*dp),Color.parseColor("#3f1111"));bv.setBackground(bbg);
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);blp.topMargin=(int)(4*dp);bv.setLayoutParams(blp);c.addView(bv);
        // After
        tv(c,"AFTER (works):",9,Gravity.LEFT,"#22c55e",(int)(8*dp));
        TextView av=new TextView(this);av.setText(after);av.setTextColor(Color.parseColor("#4ade80"));av.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
        av.setTypeface(Typeface.MONOSPACE);av.setPadding((int)(10*dp),(int)(8*dp),(int)(10*dp),(int)(8*dp));
        GradientDrawable abg=new GradientDrawable();abg.setCornerRadius(6*dp);abg.setColor(Color.parseColor("#051a0a"));abg.setStroke((int)(1*dp),Color.parseColor("#113f1a"));av.setBackground(abg);
        LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);alp.topMargin=(int)(4*dp);av.setLayoutParams(alp);c.addView(av);
    }

    // ═══ SETTINGS TAB ═══
    private void buildSettingsTab(LinearLayout c){
        tv(c,"⚙️ APK Feature Controls",18,Gravity.LEFT,"#ffffff",0);
        tv(c,"Toggle features on/off for all apps",12,Gravity.LEFT,"#64748b",(int)(4*dp));
        addSep(c,(int)(16*dp));
        tv(c,"🔒 Security",12,Gravity.LEFT,"#818cf8",(int)(8*dp));
        addToggle(c,"Biometric Lock","Fingerprint/PIN on app open",sBiometric,"biometric");
        addToggle(c,"Keep Screen On","Prevent screen sleep",sKeepScreen,"keepscreen");
        addSep(c,(int)(12*dp));
        tv(c,"🌐 WebView",12,Gravity.LEFT,"#818cf8",(int)(8*dp));
        addToggle(c,"Refresh Button","Floating ↻ reload button",sRefreshBtn,"refresh");
        addToggle(c,"Cache Clear on Load","Clear SW cache for fresh content",sCacheClear,"cacheclear");
        addToggle(c,"Chrome User-Agent","Spoof Chrome instead of WebView",sChromeUA,"chromeua");
        addToggle(c,"Portrait Lock","Lock to portrait only",sOrientLock,"orientlock");
        addSep(c,(int)(12*dp));
        tv(c,"📱 Device",12,Gravity.LEFT,"#818cf8",(int)(8*dp));
        addToggle(c,"Dark Mode Sync","Inject phone theme to web app",sDarkSync,"darksync");
        addToggle(c,"Battery Saver","Monitor battery + inject level",sBatSaver,"batsaver");
        addToggle(c,"Notifications","Browser→Android notification bridge",sNotif,"notif");
        addToggle(c,"GPS / Location","Allow geolocation access",sGPS,"gps");
        addToggle(c,"Camera","Allow camera for file upload",sCamera,"camera");
        addSep(c,(int)(12*dp));
        tv(c,"📥 Downloads",12,Gravity.LEFT,"#818cf8",(int)(8*dp));
        addToggle(c,"Download Bridge","Handle blob/data URI downloads",sDownBridge,"downbridge");
        addToggle(c,"Show Download Toast","Show filename toast on download",sToast,"toast");
        addSep(c,(int)(12*dp));
        tv(c,"🎨 Appearance",12,Gravity.LEFT,"#818cf8",(int)(8*dp));
        addSlider(c,"Refresh Button Size",sRefreshSize,24,56,"px","refreshsize");
        addSlider(c,"Splash Duration",sSplashMs,0,2000,"ms","splashms");
        addSep(c,(int)(12*dp));
        // Reset
        Button rst=styledBtn("🔄 Reset All to Default","#1e293b");rst.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){resetSettings();showTab(2);}});
        LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rlp.topMargin=(int)(12*dp);rst.setLayoutParams(rlp);c.addView(rst);
    }

    private void addToggle(LinearLayout c,String label,String desc,boolean val,final String key){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding((int)(12*dp),(int)(10*dp),(int)(12*dp),(int)(10*dp));
        GradientDrawable rbg=new GradientDrawable();rbg.setCornerRadius(10*dp);rbg.setColor(Color.parseColor("#111827"));row.setBackground(rbg);
        LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rlp.bottomMargin=(int)(6*dp);row.setLayoutParams(rlp);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        TextView lt=new TextView(this);lt.setText(label);lt.setTextColor(Color.WHITE);lt.setTextSize(TypedValue.COMPLEX_UNIT_SP,13);lt.setTypeface(Typeface.DEFAULT_BOLD);info.addView(lt);
        TextView dt=new TextView(this);dt.setText(desc);dt.setTextColor(Color.parseColor("#64748b"));dt.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);info.addView(dt);row.addView(info);
        final TextView tog=new TextView(this);tog.setText(val?"ON":"OFF");tog.setTextColor(val?Color.parseColor("#22c55e"):Color.parseColor("#ef4444"));
        tog.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);tog.setTypeface(Typeface.DEFAULT_BOLD);tog.setPadding((int)(12*dp),(int)(6*dp),(int)(12*dp),(int)(6*dp));
        GradientDrawable tg=new GradientDrawable();tg.setCornerRadius(6*dp);tg.setColor(val?Color.parseColor("#052e16"):Color.parseColor("#2e0505"));tog.setBackground(tg);
        tog.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){
            boolean nv=tog.getText().toString().equals("OFF");setSetting(key,nv);
            tog.setText(nv?"ON":"OFF");tog.setTextColor(nv?Color.parseColor("#22c55e"):Color.parseColor("#ef4444"));
            GradientDrawable ng=new GradientDrawable();ng.setCornerRadius(6*dp);ng.setColor(nv?Color.parseColor("#052e16"):Color.parseColor("#2e0505"));tog.setBackground(ng);
        }});row.addView(tog);c.addView(row);
    }

    private void addSlider(LinearLayout c,String label,int val,int min,int max,final String unit,final String key){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding((int)(12*dp),(int)(10*dp),(int)(12*dp),(int)(10*dp));
        GradientDrawable rbg=new GradientDrawable();rbg.setCornerRadius(10*dp);rbg.setColor(Color.parseColor("#111827"));row.setBackground(rbg);
        LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rlp.bottomMargin=(int)(6*dp);row.setLayoutParams(rlp);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
        TextView lt=new TextView(this);lt.setText(label);lt.setTextColor(Color.WHITE);lt.setTextSize(TypedValue.COMPLEX_UNIT_SP,13);lt.setTypeface(Typeface.DEFAULT_BOLD);
        lt.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));top.addView(lt);
        final TextView vt=new TextView(this);vt.setText(val+unit);vt.setTextColor(Color.parseColor("#f59e0b"));vt.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);vt.setTypeface(Typeface.DEFAULT_BOLD);top.addView(vt);row.addView(top);
        SeekBar sb=new SeekBar(this);sb.setMax(max-min);sb.setProgress(val-min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar s,int p,boolean u){int v=p+min;vt.setText(v+unit);setIntSetting(key,v);}
            @Override public void onStartTrackingTouch(SeekBar s){}@Override public void onStopTrackingTouch(SeekBar s){}});
        row.addView(sb);c.addView(row);
    }

    // ═══ SETTINGS STORAGE ═══
    private void loadSettings(){SharedPreferences sp=getSharedPreferences(P,0);
        sBiometric=sp.getBoolean("biometric",false);sKeepScreen=sp.getBoolean("keepscreen",true);sRefreshBtn=sp.getBoolean("refresh",true);
        sCacheClear=sp.getBoolean("cacheclear",true);sChromeUA=sp.getBoolean("chromeua",true);sOrientLock=sp.getBoolean("orientlock",false);
        sDarkSync=sp.getBoolean("darksync",true);sBatSaver=sp.getBoolean("batsaver",true);sNotif=sp.getBoolean("notif",true);
        sGPS=sp.getBoolean("gps",true);sCamera=sp.getBoolean("camera",true);sDownBridge=sp.getBoolean("downbridge",true);sToast=sp.getBoolean("toast",true);
        sRefreshSize=sp.getInt("refreshsize",38);sSplashMs=sp.getInt("splashms",800);}
    private void setSetting(String k,boolean v){getSharedPreferences(P,0).edit().putBoolean(k,v).apply();loadSettings();}
    private void setIntSetting(String k,int v){getSharedPreferences(P,0).edit().putInt(k,v).apply();loadSettings();}
    private void resetSettings(){getSharedPreferences(P,0).edit().remove("biometric").remove("keepscreen").remove("refresh").remove("cacheclear")
        .remove("chromeua").remove("orientlock").remove("darksync").remove("batsaver").remove("notif").remove("gps").remove("camera")
        .remove("downbridge").remove("toast").remove("refreshsize").remove("splashms").apply();loadSettings();
        Toast.makeText(this,"Settings reset to default",Toast.LENGTH_SHORT).show();}

    // ═══ PDF GENERATION ═══
    private void generateGuidePDF(){
        try{String guide=getGuideText();byte[] data=guide.getBytes("UTF-8");
            File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);if(!dir.exists())dir.mkdirs();
            File f=new File(dir,"APK_Compatibility_Guide.txt");FileOutputStream fos=new FileOutputStream(f);fos.write(data);fos.close();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(f)));
            showDlNotif(f,"text/plain");
            Toast.makeText(this,"Downloaded: APK_Compatibility_Guide.txt",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"Failed: "+e.getMessage(),Toast.LENGTH_SHORT).show();}}

    private void copyGuideText(){
        android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("guide",getGuideText()));
        Toast.makeText(this,"Guide copied to clipboard",Toast.LENGTH_SHORT).show();}

    private String getGuideText(){return "WEB APP → APK COMPATIBILITY GUIDE\n"+
        "==================================\n\n"+
        "1. GOOGLE LOGIN\n"+
        "   BEFORE: signInWithPopup(auth, provider)\n"+
        "   AFTER:  signInWithRedirect(auth, provider)\n"+
        "   + Add getRedirectResult(auth).catch() in AuthProvider\n\n"+
        "2. PDF DOWNLOADS (jsPDF)\n"+
        "   BEFORE: doc.save('file.pdf')\n"+
        "   AFTER:  if(window.__IS_APK__){\n"+
        "     var b64=doc.output('datauristring').split(',')[1];\n"+
        "     AndroidDownload.save(b64,'file.pdf','application/pdf');\n"+
        "   } else doc.save('file.pdf');\n\n"+
        "3. EXCEL DOWNLOADS (SheetJS)\n"+
        "   BEFORE: XLSX.writeFile(wb, 'file.xlsx')\n"+
        "   AFTER:  if(window.__IS_APK__){\n"+
        "     var b64=XLSX.write(wb,{bookType:'xlsx',type:'base64'});\n"+
        "     AndroidDownload.save(b64,'file.xlsx',\n"+
        "       'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');\n"+
        "   } else XLSX.writeFile(wb,'file.xlsx');\n\n"+
        "4. WHATSAPP LINKS\n"+
        "   BEFORE: https://wa.me/?text=...\n"+
        "   AFTER:  whatsapp://send?text=...\n\n"+
        "5. GOOGLE SHEETS OAUTH\n"+
        "   BEFORE: window.open(url,'oauth','width=600')\n"+
        "   AFTER:  window.location.href = url\n\n"+
        "6. SUPPORTED LINKS\n"+
        "   tel:+919876543210 — phone dialer\n"+
        "   whatsapp://send?phone=91... — WhatsApp\n"+
        "   mailto:email@example.com — Gmail\n"+
        "   sms:+919876543210 — messaging\n"+
        "   geo:15.123,76.456 — Google Maps\n\n"+
        "7. JS VARIABLES (auto-injected)\n"+
        "   window.__IS_APK__ = true\n"+
        "   window.__APK_VERSION__ = '1.0'\n"+
        "   window.__THEME__ = 'dark'/'light'\n"+
        "   window.__BATTERY__ = {level, low}\n"+
        "   AndroidDownload.save(base64, filename, mime)\n"+
        "   AndroidNotification.show(title, body)\n\n"+
        "8. DON'T USE\n"+
        "   ✗ signInWithPopup\n"+
        "   ✗ window.open() for OAuth\n"+
        "   ✗ blob: URLs for downloads\n"+
        "   ✗ Browser Push API\n"+
        "   ✗ https://wa.me/\n"+
        "   ✗ XLSX.writeFile()\n";}

    // ═══ LAUNCH WEBVIEW ═══
    private void launchApp(JSONObject app){
        inWebView=true;root.removeAllViews();String url=app.optString("url","about:blank");
        String bg=app.optString("bg","#0f172a"),stat=app.optString("status","#0f172a"),accent=app.optString("accent","#6366f1");curAppName=app.optString("name","App");
        if(Build.VERSION.SDK_INT>=21){try{getWindow().setStatusBarColor(Color.parseColor(stat));}catch(Exception e){}try{getWindow().setNavigationBarColor(Color.parseColor(stat));}catch(Exception e){}}
        webContainer=new FrameLayout(this);try{webContainer.setBackgroundColor(Color.parseColor(bg));}catch(Exception e){webContainer.setBackgroundColor(Color.parseColor("#0f172a"));}
        root.addView(webContainer,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        // Splash
        if(sSplashMs>0){final FrameLayout spl=new FrameLayout(this);try{spl.setBackgroundColor(Color.parseColor(bg));}catch(Exception e){}spl.setClickable(true);
            LinearLayout sc=new LinearLayout(this);sc.setOrientation(LinearLayout.VERTICAL);sc.setGravity(Gravity.CENTER);
            TextView sn=new TextView(this);sn.setText(curAppName);sn.setTextColor(Color.WHITE);sn.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);sn.setTypeface(Typeface.DEFAULT_BOLD);sn.setGravity(Gravity.CENTER);sc.addView(sn);
            TextView sl=new TextView(this);sl.setText("Loading...");sl.setTextColor(Color.argb(60,255,255,255));sl.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);sl.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);slp.topMargin=(int)(8*dp);sl.setLayoutParams(slp);sc.addView(sl);
            FrameLayout.LayoutParams sclp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);sclp.gravity=Gravity.CENTER;spl.addView(sc,sclp);
            webContainer.addView(spl,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
            spl.postDelayed(new Runnable(){@Override public void run(){if(Build.VERSION.SDK_INT>=12)spl.animate().alpha(0f).setDuration(400).withEndAction(new Runnable(){@Override public void run(){try{webContainer.removeView(spl);}catch(Exception e){}}}).start();else try{webContainer.removeView(spl);}catch(Exception e){}}},sSplashMs);}
        webView=new WebView(this);try{webView.setBackgroundColor(Color.parseColor(bg));}catch(Exception e){}
        webContainer.addView(webView,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        CookieManager cm=CookieManager.getInstance();cm.setAcceptCookie(true);if(Build.VERSION.SDK_INT>=21)cm.setAcceptThirdPartyCookies(webView,true);
        setupWV(webView);
        if(sNotif)webView.addJavascriptInterface(new Object(){@JavascriptInterface public void show(String t,String b){showN(t,b);}},"AndroidNotification");
        if(sDownBridge)webView.addJavascriptInterface(new Object(){@JavascriptInterface public void save(final String b64,final String fn,final String m){
            runOnUiThread(new Runnable(){@Override public void run(){try{byte[] d=Base64.decode(b64,Base64.DEFAULT);
                File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);if(!dir.exists())dir.mkdirs();
                File f=new File(dir,fn);if(f.exists()){String n=fn.contains(".")?fn.substring(0,fn.lastIndexOf('.')):fn;String e=fn.contains(".")?fn.substring(fn.lastIndexOf('.')):"";;f=new File(dir,n+"_"+System.currentTimeMillis()+e);}
                FileOutputStream fos=new FileOutputStream(f);fos.write(d);fos.close();sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(f)));
                showDlNotif(f,m);
                if(sToast)Toast.makeText(MainActivity.this,"Downloaded: "+f.getName(),Toast.LENGTH_SHORT).show();
            }catch(Exception e){if(sToast)Toast.makeText(MainActivity.this,"Download failed",Toast.LENGTH_SHORT).show();}}});}},"AndroidDownload");
        final String acRgba;try{int cc=Color.parseColor(accent);acRgba=Color.red(cc)+","+Color.green(cc)+","+Color.blue(cc)+",0.85";}catch(Exception e){throw new RuntimeException(e);}
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onCreateWindow(WebView v,boolean d,boolean u,Message m){popupWebView=new WebView(MainActivity.this);setupWV(popupWebView);
                if(Build.VERSION.SDK_INT>=21)CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView,true);popupWebView.setWebViewClient(new WebViewClient());
                popupWebView.setWebChromeClient(new WebChromeClient(){@Override public void onCloseWindow(WebView w){clPop();}});
                webContainer.addView(popupWebView,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
                WebView.WebViewTransport t=(WebView.WebViewTransport)m.obj;t.setWebView(popupWebView);m.sendToTarget();return true;}
            @Override public void onCloseWindow(WebView w){clPop();}
            @Override public boolean onShowFileChooser(WebView wv,ValueCallback<Uri[]> cb,FileChooserParams p){
                if(!sCamera){cb.onReceiveValue(null);return true;}
                if(fuCb!=null)fuCb.onReceiveValue(null);fuCb=cb;String[] a=p.getAcceptTypes();boolean cam=false;
                if(a!=null)for(String x:a)if(x!=null&&x.startsWith("image"))cam=true;
                if(cam&&Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},CP);return true;}
                try{startActivityForResult(p.createIntent(),FC);}catch(Exception e){fuCb=null;return false;}return true;}
            @Override public void onGeolocationPermissionsShowPrompt(String o,GeolocationPermissions.Callback cb){
                if(!sGPS){cb.invoke(o,false,false);return;}
                if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LP);cb.invoke(o,true,true);}
        });
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,String u){if(u.startsWith("tel:")||u.startsWith("whatsapp:")||u.startsWith("mailto:")||u.startsWith("sms:")||u.startsWith("geo:")||u.startsWith("intent:")){
                try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception e){}return true;}return false;}
            @Override public void onPageFinished(WebView v,String u){super.onPageFinished(v,u);
                if(sCacheClear)v.evaluateJavascript("(function(){if(window._cc)return;window._cc=true;if('caches' in window){caches.keys().then(function(n){n.forEach(function(k){caches.delete(k)})})}if('serviceWorker' in navigator){navigator.serviceWorker.getRegistrations().then(function(r){r.forEach(function(w){w.unregister()})})}})();",null);
                if(sDownBridge)v.evaluateJavascript("(function(){if(window._dlP)return;window._dlP=true;var oc=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){var h=this.href||'',dl=this.download||'file';if(h.startsWith('blob:')){try{var x=new XMLHttpRequest();x.open('GET',h,true);x.responseType='blob';x.onload=function(){var r=new FileReader();r.onloadend=function(){var b=r.result.split(',')[1];var m=x.response.type||'application/octet-stream';try{AndroidDownload.save(b,dl,m)}catch(e){oc.call(this)}};r.readAsDataURL(x.response)};x.send();return}catch(e){}}if(h.startsWith('data:')){try{var p=h.split(',');var m=p[0].split(':')[1].split(';')[0];var b=p[1];AndroidDownload.save(b,dl,m);return}catch(e){}}oc.call(this)};window.__downloadFile=function(b,n,m){try{AndroidDownload.save(b,n,m||'application/octet-stream')}catch(e){}}})();",null);
                if(sNotif)v.evaluateJavascript("(function(){if(window._p)return;window._p=true;window.Notification=function(t,o){try{AndroidNotification.show(t,(o&&o.body)||'')}catch(e){}};window.Notification.permission='granted';window.Notification.requestPermission=function(c){if(c)c('granted');return Promise.resolve('granted')}})();",null);
                if(sRefreshBtn)v.evaluateJavascript("(function(){if(document.getElementById('_r'))return;var sz="+sRefreshSize+";var s=document.createElement('style');s.textContent='#_r{position:fixed;top:18px;right:30%;width:'+sz+'px;height:'+sz+'px;border-radius:50%;background:rgba("+acRgba+");border:2px solid rgba("+acRgba+");color:#fff;font-size:'+(sz*0.42)+'px;display:flex;align-items:center;justify-content:center;box-shadow:0 2px 10px rgba(0,0,0,0.4);z-index:99999;cursor:pointer;opacity:0.85}#_r:active{transform:scale(0.85);opacity:1}';document.head.appendChild(s);var b=document.createElement('div');b.id='_r';b.innerHTML='\\u21BB';b.onclick=function(){b.innerHTML='\\u23F3';b.style.opacity='0.5';setTimeout(function(){location.reload()},100)};document.body.appendChild(b)})();",null);
                v.evaluateJavascript("(function(){window.__IS_APK__=true;window.__APK_VERSION__='1.0';window.__APK_PACKAGE__='com.krr.apptester';try{window.dispatchEvent(new CustomEvent('apkReady',{detail:{version:'1.0',pkg:'com.krr.apptester'}}))}catch(x){}})();",null);
                if(sDarkSync){boolean dk=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
                    v.evaluateJavascript("(function(){window.__THEME__='"+(dk?"dark":"light")+"';document.documentElement.setAttribute('data-apk-theme','"+(dk?"dark":"light")+"');try{window.dispatchEvent(new CustomEvent('themeSync',{detail:{theme:'"+(dk?"dark":"light")+"'}}))}catch(e){}})();",null);}
                if(sBatSaver){Intent bat=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(bat!=null){int l=bat.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);int s2=bat.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
                    int p=(int)((l/(float)s2)*100);boolean lo=p<=20;v.evaluateJavascript("(function(){window.__BATTERY__={level:"+p+",low:"+lo+"};if("+lo+"){document.documentElement.setAttribute('data-battery-saver','true')}})();",null);}}
            }
            @Override public void onReceivedError(WebView v,int c,String d,String u){v.loadData("<html><body style='background:#0f172a;color:#94a3b8;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center'><div><h2 style='color:#f97316'>No Connection</h2><p>Check internet</p><button onclick='location.reload()' style='background:#f97316;color:#fff;border:none;padding:12px 24px;border-radius:8px;font-size:16px;margin-top:16px'>Retry</button></div></body></html>","text/html","utf-8");}
        });
        webView.setDownloadListener(new DownloadListener(){@Override public void onDownloadStart(String u,String ua,String cd,String m,long l){
            try{DownloadManager.Request r=new DownloadManager.Request(Uri.parse(u));r.setMimeType(m);String fn=URLUtil.guessFileName(u,cd,m);r.setTitle(fn);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fn);r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                ((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);if(sToast)Toast.makeText(MainActivity.this,"Downloading: "+fn,Toast.LENGTH_SHORT).show();
            }catch(Exception e){if(sToast)Toast.makeText(MainActivity.this,"Download failed",Toast.LENGTH_SHORT).show();}}});
        if(sKeepScreen)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        webView.loadUrl(url);
    }

    // ═══ APP STORAGE ═══
    private JSONArray getApps(){try{return new JSONArray(getSharedPreferences(P,0).getString(AK,"[]"));}catch(Exception e){return new JSONArray();}}
    private void saveApps(JSONArray a){getSharedPreferences(P,0).edit().putString(AK,a.toString()).apply();}
    private void delApp(int i){JSONArray a=getApps();JSONArray n=new JSONArray();for(int j=0;j<a.length();j++)if(j!=i)try{n.put(a.get(j));}catch(Exception e){}saveApps(n);}
    private void showAddEdit(final int idx){
        final JSONObject ex;if(idx>=0){try{ex=getApps().getJSONObject(idx);}catch(Exception e){return;}}else ex=null;
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding((int)(20*dp),(int)(16*dp),(int)(20*dp),(int)(8*dp));
        final EditText ni=mkInput(f,"App Name",ex!=null?ex.optString("name",""):"");
        final EditText ui=mkInput(f,"URL (https://...)",ex!=null?ex.optString("url",""):"");
        final EditText bi=mkInput(f,"Background (#0f172a)",ex!=null?ex.optString("bg","#0f172a"):"#0f172a");
        final EditText si=mkInput(f,"Status bar (#0f172a)",ex!=null?ex.optString("status","#0f172a"):"#0f172a");
        final EditText ai=mkInput(f,"Accent (#6366f1)",ex!=null?ex.optString("accent","#6366f1"):"#6366f1");
        new AlertDialog.Builder(this).setTitle(idx>=0?"Edit App":"Add New App").setView(f)
            .setPositiveButton("Save",new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface d,int w){
                String n=ni.getText().toString().trim(),u=ui.getText().toString().trim();if(n.isEmpty()||u.isEmpty()){Toast.makeText(MainActivity.this,"Name & URL required",Toast.LENGTH_SHORT).show();return;}
                if(!u.startsWith("http"))u="https://"+u;try{JSONObject o=new JSONObject();o.put("name",n);o.put("url",u);o.put("bg",bi.getText().toString().trim());
                    o.put("status",si.getText().toString().trim());o.put("accent",ai.getText().toString().trim());
                    if(idx>=0){JSONArray a=getApps();a.put(idx,o);saveApps(a);}else{JSONArray a=getApps();a.put(o);saveApps(a);}showTab(0);}catch(Exception e){}}})
            .setNegativeButton("Cancel",null).show();
    }

    // ═══ HELPERS ═══

    private void showDlNotif(File file, String mimeType) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Intent oi = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= 24) {
                oi.setDataAndType(Uri.parse("content://downloads/all_downloads/" + file.getName()), mimeType);
                oi.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                oi.setDataAndType(Uri.fromFile(file), mimeType);
            }
            oi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), oi, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                try { b = (Notification.Builder) Notification.Builder.class.getConstructor(Context.class, String.class).newInstance(this, CH); }
                catch (Exception e) { b = new Notification.Builder(this); }
            } else { b = new Notification.Builder(this); }
            b.setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("Download complete").setContentText(file.getName()).setContentIntent(pi).setAutoCancel(true);
            nm.notify((int)System.currentTimeMillis(), b.build());
        } catch (Exception e) {}
    }

    private void setupWV(WebView w){WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);s.setAllowContentAccess(true);s.setLoadWithOverviewMode(true);s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(false);s.setJavaScriptCanOpenWindowsAutomatically(true);s.setSupportMultipleWindows(true);
        s.setBuiltInZoomControls(false);s.setDisplayZoomControls(false);s.setAppCacheEnabled(true);s.setGeolocationEnabled(sGPS);
        if(sChromeUA){String ua=s.getUserAgentString().replace("; wv","").replaceAll("Version/[\\d.]+\\s","");s.setUserAgentString(ua+" Chrome/125.0.6422.165");}}
    private void showN(String t,String b){try{NotificationManager nm=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Intent i=new Intent(this,MainActivity.class);i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder nb;if(Build.VERSION.SDK_INT>=26)nb=(Notification.Builder)Notification.Builder.class.getConstructor(Context.class,String.class).newInstance(this,CH);
        else nb=new Notification.Builder(this);nb.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(t).setContentText(b).setContentIntent(pi).setAutoCancel(true);
        try{nb.setStyle(new Notification.BigTextStyle().bigText(b));}catch(Exception e){}nm.notify(notifId++,nb.build());}catch(Exception e){}}
    private void createCh(){if(Build.VERSION.SDK_INT>=26){try{Class c=Class.forName("android.app.NotificationChannel");
        Object ch=c.getConstructor(String.class,CharSequence.class,int.class).newInstance(CH,"App Alerts",4);
        c.getMethod("setDescription",String.class).invoke(ch,"Alerts");NotificationManager nm=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.getClass().getMethod("createNotificationChannel",c).invoke(nm,ch);}catch(Exception e){}}}
    private void clPop(){if(popupWebView!=null){webContainer.removeView(popupWebView);popupWebView.destroy();popupWebView=null;}}
    private void tv(LinearLayout c,String t,int sp,int grav,String col,int mt){TextView v=new TextView(this);v.setText(t);try{v.setTextColor(Color.parseColor(col));}catch(Exception e){}
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp);if(sp>=14)v.setTypeface(Typeface.DEFAULT_BOLD);v.setGravity(grav);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=mt;v.setLayoutParams(p);c.addView(v);}
    private void addSep(LinearLayout c,int mt){View v=new View(this);v.setBackgroundColor(Color.parseColor("#1e293b"));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,(int)(1*dp));p.topMargin=mt;v.setLayoutParams(p);c.addView(v);}
    private View mkBtn(String t,String bg,int tc,View.OnClickListener cl){Button b=new Button(this);b.setText(t);b.setTextColor(tc);b.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);
        b.setTypeface(Typeface.DEFAULT_BOLD);b.setAllCaps(false);GradientDrawable g=new GradientDrawable();g.setCornerRadius(8*dp);try{g.setColor(Color.parseColor(bg));}catch(Exception e){}
        b.setBackground(g);b.setPadding((int)(16*dp),(int)(8*dp),(int)(16*dp),(int)(8*dp));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.rightMargin=(int)(8*dp);b.setLayoutParams(p);b.setOnClickListener(cl);return b;}
    private Button styledBtn(String t,String bg){Button b=new Button(this);b.setText(t);b.setTextColor(Color.WHITE);b.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);b.setTypeface(Typeface.DEFAULT_BOLD);b.setAllCaps(false);
        GradientDrawable g=new GradientDrawable();g.setCornerRadius(8*dp);try{g.setColor(Color.parseColor(bg));}catch(Exception e){}b.setBackground(g);
        b.setPadding((int)(16*dp),(int)(10*dp),(int)(16*dp),(int)(10*dp));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.rightMargin=(int)(8*dp);b.setLayoutParams(p);return b;}
    private EditText mkInput(LinearLayout c,String hint,String val){EditText e=new EditText(this);e.setHint(hint);e.setText(val);e.setTextColor(Color.BLACK);e.setHintTextColor(Color.GRAY);c.addView(e);return e;}

    @Override protected void onActivityResult(int rq,int rs,Intent d){super.onActivityResult(rq,rs,d);
        if(rq==AC){if(rs==RESULT_OK)initApp();else{Toast.makeText(this,"Auth required",Toast.LENGTH_SHORT).show();finish();}}
        if(rq==FC&&fuCb!=null){Uri[] r=null;if(rs==RESULT_OK&&d!=null){String s=d.getDataString();if(s!=null)r=new Uri[]{Uri.parse(s)};}fuCb.onReceiveValue(r);fuCb=null;}}
    @Override public void onRequestPermissionsResult(int rq,String[] p,int[] r){super.onRequestPermissionsResult(rq,p,r);
        if(rq==CP){if(fuCb!=null){fuCb.onReceiveValue(null);fuCb=null;}}if(rq==LP){}}
    @Override public boolean onKeyDown(int k,KeyEvent e){if(k==KeyEvent.KEYCODE_BACK){if(inWebView){if(popupWebView!=null){clPop();return true;}
        if(webView!=null&&webView.canGoBack()){webView.goBack();return true;}if(webView!=null){webView.destroy();webView=null;}
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);showTab(0);return true;}
        if(System.currentTimeMillis()-backPressedTime<2000){finish();return true;}backPressedTime=System.currentTimeMillis();
        Toast.makeText(this,"Press back again to exit",Toast.LENGTH_SHORT).show();return true;}return super.onKeyDown(k,e);}
    @Override protected void onResume(){super.onResume();if(webView!=null&&sDarkSync){webView.onResume();boolean dk=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        webView.evaluateJavascript("(function(){window.__THEME__='"+(dk?"dark":"light")+"';document.documentElement.setAttribute('data-apk-theme','"+(dk?"dark":"light")+"')})();",null);}}
    @Override protected void onPause(){if(webView!=null)webView.onPause();super.onPause();}
    @Override protected void onDestroy(){if(batRx!=null)try{unregisterReceiver(batRx);}catch(Exception e){}super.onDestroy();}
}
