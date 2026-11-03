package es.ruben.quientrabaja;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Guarda la configuración en el propio teléfono. */
final class Almacen {
    private static final String ARCHIVO = "quien_trabaja";
    private static final String CONFIG = "config";
    private static final String MI_TURNO = "mi_turno";

    private Almacen() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    static boolean guardado(Context c) {
        return prefs(c).contains(CONFIG);
    }

    static Turnos leer(Context c) {
        String texto = prefs(c).getString(CONFIG, null);
        if (texto == null) {
            return Turnos.porDefecto();
        }
        try {
            return desdeJson(new JSONObject(texto));
        } catch (JSONException | IllegalArgumentException e) {
            return Turnos.porDefecto();
        }
    }

    /** Valida y guarda. Devuelve la configuración limpia. */
    static Turnos guardar(Context c, String json) throws JSONException {
        Turnos t = desdeJson(new JSONObject(json));
        prefs(c).edit().putString(CONFIG, aJson(t, true).toString()).commit();
        return t;
    }

    static int miTurno(Context c) {
        return prefs(c).getInt(MI_TURNO, -1);
    }

    static void guardarMiTurno(Context c, int turno) {
        prefs(c).edit().putInt(MI_TURNO, turno).commit();
    }

    static Turnos desdeJson(JSONObject o) throws JSONException {
        JSONArray jn = o.getJSONArray("turnos");
        JSONArray jd = o.getJSONArray("dias");
        String[] nombres = new String[jn.length()];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = jn.getString(i);
        }
        int[] dias = new int[jd.length()];
        for (int i = 0; i < dias.length; i++) {
            dias[i] = jd.getInt(i);
        }
        JSONObject h = o.getJSONObject("horas");
        return Turnos.validar(nombres, o.getString("patron"), o.getString("ref"), dias,
                h.getString("M"), h.getString("T"), h.getString("N"), coordDesdeJson(o.optJSONObject("coord")));
    }

    private static Turnos.Coord coordDesdeJson(JSONObject c) throws JSONException {
        if (c == null) {
            return Turnos.coordPorDefecto();
        }
        JSONArray jn = c.getJSONArray("nombres");
        JSONArray jd = c.getJSONArray("dias");
        String[] nombres = new String[jn.length()];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = jn.getString(i);
        }
        int[] dias = new int[jd.length()];
        for (int i = 0; i < dias.length; i++) {
            dias[i] = jd.getInt(i);
        }
        return Turnos.validarCoord(nombres, dias, c.getString("ref"), c.getString("entrada"), c.getString("salida"));
    }

    private static JSONObject coordAJson(Turnos.Coord c) throws JSONException {
        JSONArray jn = new JSONArray();
        JSONArray jd = new JSONArray();
        for (int i = 0; i < c.nombres.length; i++) {
            jn.put(c.nombres[i]);
            jd.put(c.dias[i]);
        }
        JSONObject o = new JSONObject();
        o.put("nombres", jn);
        o.put("dias", jd);
        o.put("ref", c.ref);
        o.put("entrada", c.entrada);
        o.put("salida", c.salida);
        return o;
    }

    static JSONObject aJson(Turnos t, boolean guardado) throws JSONException {
        JSONArray jn = new JSONArray();
        JSONArray jd = new JSONArray();
        for (int i = 0; i < Turnos.NUM; i++) {
            jn.put(t.nombres[i]);
            jd.put(t.dias[i]);
        }
        JSONObject h = new JSONObject();
        h.put("M", t.horaM);
        h.put("T", t.horaT);
        h.put("N", t.horaN);
        JSONObject o = new JSONObject();
        o.put("turnos", jn);
        o.put("dias", jd);
        o.put("patron", t.patron);
        o.put("ref", t.ref);
        o.put("horas", h);
        o.put("coord", coordAJson(t.coord));
        o.put("guardado", guardado);
        return o;
    }
}
