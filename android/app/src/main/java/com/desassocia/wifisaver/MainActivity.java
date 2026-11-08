package com.desassocia.wifisaver;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;
import android.view.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.net.Uri;
import android.view.ViewGroup;
import java.io.File;
import java.util.UUID;

public class MainActivity extends Activity {
    private WebView appView, browser;
    private FrameLayout root, browserPanel;
    private EditText address;
    private TextView status;
    private final int green = Color.rgb(57,242,139);
    private File offlineDir;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        offlineDir = new File(getFilesDir(), "offline");
        offlineDir.mkdirs();
        root = new FrameLayout(this);
        appView = new WebView(this);
        configure(appView);
        appView.addJavascriptInterface(new Bridge(), "Android");
        appView.loadUrl("file:///android_asset/www/index.html");
        root.addView(appView, new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
    }

    private void configure(WebView view) {
        WebSettings s=view.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setGeolocationEnabled(false);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        view.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin,false,false);
            }
            @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
            @Override public void onProgressChanged(WebView v,int p) {
                if(status!=null) status.setText(p<100 ? "Carregando "+p+"%" : "Localização bloqueada");
            }
        });
        view.setWebViewClient(new WebViewClient());
    }

    private GradientDrawable panelBg(int color) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(14); d.setStroke(1,Color.rgb(45,75,55)); return d;
    }
    private TextView makeButton(String text) {
        TextView t=new TextView(this); t.setText(text); t.setTextColor(green); t.setTextSize(12); t.setTypeface(null,Typeface.BOLD);
        t.setGravity(Gravity.CENTER); t.setPadding(10,12,10,12); t.setBackground(panelBg(Color.rgb(15,27,19))); return t;
    }
    private void openBrowser(String url) {
        if(url==null || url.trim().isEmpty()) return;
        String u=url.trim();
        if(!u.matches("(?i)^https?://.*")) u="https://"+u;
        Uri parsed=Uri.parse(u);
        if(!"http".equals(parsed.getScheme()) && !"https".equals(parsed.getScheme()) && !"file".equals(parsed.getScheme())) return;
        if("file".equals(parsed.getScheme()) && !u.startsWith(offlineDir.getAbsolutePath())) return;
        if(browserPanel!=null) root.removeView(browserPanel);
        browserPanel=new FrameLayout(this);
        browserPanel.setBackgroundColor(Color.rgb(7,12,9));
        LinearLayout page=new LinearLayout(this); page.setOrientation(1); page.setPadding(8,8,8,8);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=makeButton("‹"), reload=makeButton("↻"), save=makeButton("↓ Offline"), close=makeButton("×");
        address=new EditText(this); address.setSingleLine(true); address.setText(u); address.setTextColor(Color.WHITE);
        address.setTextSize(12); address.setInputType(InputType.TYPE_TEXT_VARIATION_URI); address.setPadding(8,4,8,4);
        address.setBackground(panelBg(Color.rgb(16,25,19)));
        bar.addView(back);
        bar.addView(address,new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(reload); bar.addView(save); bar.addView(close);
        status=new TextView(this); status.setText("Localização bloqueada neste navegador"); status.setTextColor(green); status.setTextSize(10); status.setPadding(5,7,5,7);
        browser=new WebView(this); configure(browser);
        address.setOnEditorActionListener((v,a,event)->{String x=address.getText().toString().trim();if(!x.matches("(?i)^https?://.*"))x="https://"+x;browser.loadUrl(x);return true;});
        back.setOnClickListener(v->{if(browser.canGoBack())browser.goBack();});
        reload.setOnClickListener(v->browser.reload());
        close.setOnClickListener(v->{root.removeView(browserPanel);browserPanel=null;});
        save.setOnClickListener(v->savePage());
        browser.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String u){address.setText(u);status.setText("Localização bloqueada • "+(v.getTitle()==null?"Página":v.getTitle()));}
        });
        page.addView(bar); page.addView(status); page.addView(browser,new LinearLayout.LayoutParams(-1,0,1));
        browserPanel.addView(page,new FrameLayout.LayoutParams(-1,-1));
        root.addView(browserPanel,new FrameLayout.LayoutParams(-1,-1));
        browser.loadUrl(u);
    }
    private void savePage() {
        if(browser==null || browser.getUrl()==null) return;
        String name=UUID.randomUUID().toString()+".mht";
        File target=new File(offlineDir,name);
        browser.saveWebArchive(target.getAbsolutePath(),false,path->{
            if(path!=null) appView.evaluateJavascript("window.onArchiveSaved && window.onArchiveSaved("+quote(name)+","+quote(browser.getTitle())+","+quote(browser.getUrl())+")",null);
            else runOnUiThread(()->Toast.makeText(this,"Não foi possível salvar esta página.",Toast.LENGTH_SHORT).show());
        });
    }
    private String quote(String s) {
        if(s==null) return "null";
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","") + "\"";
    }
    private class Bridge {
        @JavascriptInterface public void openBrowser(String url) { runOnUiThread(()->MainActivity.this.openBrowser(url)); }
        @JavascriptInterface public void openArchive(String name) {
            runOnUiThread(()->{
                if(name==null || !name.matches("[a-f0-9-]+\\.mht")) return;
                File file=new File(offlineDir,name);
                if(file.exists()) MainActivity.this.openBrowser("file://"+file.getAbsolutePath());
                else Toast.makeText(MainActivity.this,"Página offline não encontrada.",Toast.LENGTH_SHORT).show();
            });
        }
    }
    @Override public void onBackPressed() {
        if(browserPanel!=null) { if(browser!=null && browser.canGoBack()) browser.goBack(); else {root.removeView(browserPanel);browserPanel=null;} }
        else if(appView!=null && appView.canGoBack()) appView.goBack();
        else super.onBackPressed();
    }
}