package es.ruben.quientrabaja;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Widget de la pantalla de inicio. Se redibuja al cambiar de franja y a medianoche. */
public class TurnosWidget extends AppWidgetProvider {
    private static final String ACCION_REFRESCAR = "es.ruben.quientrabaja.REFRESCAR";

    @Override
    public void onReceive(Context contexto, Intent intent) {
        super.onReceive(contexto, intent);
        String accion = intent.getAction();
        if (ACCION_REFRESCAR.equals(accion)
                || Intent.ACTION_BOOT_COMPLETED.equals(accion)
                || Intent.ACTION_TIME_CHANGED.equals(accion)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(accion)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(accion)) {
            actualizarTodos(contexto);
        }
    }

    @Override
    public void onUpdate(Context contexto, AppWidgetManager gestor, int[] ids) {
        actualizarTodos(contexto);
    }

    @Override
    public void onDisabled(Context contexto) {
        AlarmManager alarmas = contexto.getSystemService(AlarmManager.class);
        if (alarmas != null) {
            alarmas.cancel(intentRefrescar(contexto));
        }
    }

    static void actualizarTodos(Context contexto) {
        AppWidgetManager gestor = AppWidgetManager.getInstance(contexto);
        int[] ids = gestor.getAppWidgetIds(new ComponentName(contexto, TurnosWidget.class));
        if (ids.length == 0) {
            return;
        }
        Turnos t = Almacen.leer(contexto);
        LocalDate hoy = LocalDate.now();
        LocalTime hora = LocalTime.now();
        int minutos = hora.getHour() * 60 + hora.getMinute();
        RemoteViews vista = construir(contexto, t, hoy, minutos);
        for (int id : ids) {
            gestor.updateAppWidget(id, vista);
        }
        programar(contexto, t, hoy, minutos);
    }

    private static RemoteViews construir(Context c, Turnos t, LocalDate fecha, int minutos) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_turnos);
        long hoy = fecha.toEpochDay();
        Turnos.Franja f = t.franjaActual(hoy, minutos);

        v.setTextViewText(R.id.txtFecha, textoFecha(fecha));
        v.setTextViewText(R.id.txtAhora, "Ahora: "
                + t.listaNombres(t.quienes(f.dia, f.letra), "sin asignar")
                + (f.ayer ? " (noche de ayer)" : ""));

        char activa = f.ayer ? ' ' : f.letra;
        fila(v, t, hoy, 'M', activa, R.id.etiquetaM, R.id.txtM);
        fila(v, t, hoy, 'T', activa, R.id.etiquetaT, R.id.txtT);
        fila(v, t, hoy, 'N', activa, R.id.etiquetaN, R.id.txtN);

        Turnos.Servicio s = t.coord.actual(hoy, minutos);
        v.setTextViewText(R.id.etiquetaC, s.activo ? "▶ Coord." : "Coord.");
        v.setTextViewText(R.id.txtC, t.coord.listaNombres(t.coord.quienes(s.dia))
                + (s.activo ? " (hasta " + t.coord.salida + ")" : " (desde " + t.coord.entrada + ")"));

        v.setTextViewText(R.id.txtLibran, "Libran: " + t.listaNombres(t.quienes(hoy, 'L'), "nadie"));

        int mio = Almacen.miTurno(c);
        if (mio >= 0 && mio < Turnos.NUM) {
            v.setTextViewText(R.id.txtMio, "Tú (" + t.nombres[mio] + "): "
                    + Turnos.nombreFranja(t.letra(mio, hoy)));
            v.setViewVisibility(R.id.txtMio, View.VISIBLE);
        } else {
            v.setViewVisibility(R.id.txtMio, View.GONE);
        }

        v.setViewVisibility(R.id.txtEjemplo, Almacen.guardado(c) ? View.GONE : View.VISIBLE);

        Intent abrir = new Intent(c, MainActivity.class);
        abrir.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        v.setOnClickPendingIntent(R.id.raiz, PendingIntent.getActivity(c, 0, abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }

    private static void fila(RemoteViews v, Turnos t, long hoy, char letra, char activa,
                             int idEtiqueta, int idNombre) {
        String etiqueta = Turnos.nombreFranja(letra);
        v.setTextViewText(idEtiqueta, letra == activa ? "▶ " + etiqueta : etiqueta);
        v.setTextViewText(idNombre, t.listaNombres(t.quienes(hoy, letra), "Sin asignar"));
    }

    private static String textoFecha(LocalDate fecha) {
        String s = fecha.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", new Locale("es", "ES")));
        return s.substring(0, 1).toUpperCase(new Locale("es", "ES")) + s.substring(1);
    }

    private static void programar(Context c, Turnos t, LocalDate hoy, int minutos) {
        AlarmManager alarmas = c.getSystemService(AlarmManager.class);
        if (alarmas == null) {
            return;
        }
        int marca = t.siguienteCambio(minutos);
        LocalDate dia = marca >= 1440 ? hoy.plusDays(1) : hoy;
        LocalTime hora = marca >= 1440 ? LocalTime.MIDNIGHT : LocalTime.of(marca / 60, marca % 60);
        long cuando = dia.atTime(hora).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        alarmas.set(AlarmManager.RTC, cuando, intentRefrescar(c));
    }

    private static PendingIntent intentRefrescar(Context c) {
        Intent i = new Intent(c, TurnosWidget.class);
        i.setAction(ACCION_REFRESCAR);
        return PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
