package es.ruben.quientrabaja;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

/** Muestra la misma interfaz que la app web, servida desde el propio APK. */
public class MainActivity extends Activity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        WebView web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setBlockNetworkLoads(true);
        web.addJavascriptInterface(new Puente(getApplicationContext()), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    /** Lo que la página puede pedir al teléfono. */
    static final class Puente {
        private final Context contexto;

        Puente(Context contexto) {
            this.contexto = contexto;
        }

        @JavascriptInterface
        public String getConfig() {
            try {
                return Almacen.aJson(Almacen.leer(contexto), Almacen.guardado(contexto)).toString();
            } catch (JSONException e) {
                return error(e.getMessage());
            }
        }

        @JavascriptInterface
        public String saveConfig(String json) {
            try {
                Turnos t = Almacen.guardar(contexto, json);
                TurnosWidget.actualizarTodos(contexto);
                return Almacen.aJson(t, true).toString();
            } catch (JSONException | IllegalArgumentException e) {
                return error(e.getMessage());
            }
        }

        @JavascriptInterface
        public int getMiTurno() {
            return Almacen.miTurno(contexto);
        }

        @JavascriptInterface
        public void setMiTurno(int turno) {
            Almacen.guardarMiTurno(contexto, turno);
            TurnosWidget.actualizarTodos(contexto);
        }

        private static String error(String mensaje) {
            try {
                return new JSONObject().put("error", mensaje).toString();
            } catch (JSONException e) {
                return "{\"error\":\"No se pudo guardar.\"}";
            }
        }
    }
}
