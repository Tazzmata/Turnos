package es.ruben.quientrabaja;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Cálculo de turnos. No depende de Android para poder probarlo por separado. */
public final class Turnos {
    public static final int NUM = 6;

    public final String[] nombres;
    public final String patron;
    public final String ref;
    public final int[] dias;
    public final String horaM;
    public final String horaT;
    public final String horaN;
    private final long refDia;

    /** Franja en curso: día en que empezó y letra M, T o N. */
    public static final class Franja {
        public final long dia;
        public final char letra;
        public final boolean ayer;

        Franja(long dia, char letra, boolean ayer) {
            this.dia = dia;
            this.letra = letra;
            this.ayer = ayer;
        }
    }

    private Turnos(String[] nombres, String patron, String ref, int[] dias,
                   String horaM, String horaT, String horaN) {
        this.nombres = nombres;
        this.patron = patron;
        this.ref = ref;
        this.dias = dias;
        this.horaM = horaM;
        this.horaT = horaT;
        this.horaN = horaN;
        this.refDia = LocalDate.parse(ref).toEpochDay();
    }

    public static Turnos porDefecto() {
        return new Turnos(
                new String[] {"Turno 1", "Turno 2", "Turno 3", "Turno 4", "Turno 5", "Turno 6"},
                "MMTTNNLLLLLL", "2026-01-01", new int[] {0, 2, 4, 6, 8, 10},
                "06:00", "14:00", "22:00");
    }

    /** Valida con las mismas reglas que la app web. Lanza IllegalArgumentException con el motivo. */
    public static Turnos validar(String[] nombres, String patron, String ref, int[] dias,
                                 String horaM, String horaT, String horaN) {
        if (nombres == null || nombres.length != NUM) {
            throw new IllegalArgumentException("Deben configurarse 6 turnos.");
        }
        String p = patron == null ? "" : patron.toUpperCase().replaceAll("\\s", "");
        if (!p.matches("[MTNL]+")) {
            throw new IllegalArgumentException("La cadencia solo admite las letras M, T, N y L.");
        }
        try {
            LocalDate.parse(ref == null ? "" : ref);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha de referencia no es válida.");
        }
        if (dias == null || dias.length != NUM) {
            throw new IllegalArgumentException("Falta indicar el día del ciclo de algún turno.");
        }
        String[] limpios = new String[NUM];
        for (int i = 0; i < NUM; i++) {
            String nombre = nombres[i] == null ? "" : nombres[i].trim();
            if (nombre.isEmpty()) {
                throw new IllegalArgumentException("Todos los turnos deben tener nombre.");
            }
            if (dias[i] < 0 || dias[i] >= p.length()) {
                throw new IllegalArgumentException("El día del ciclo de " + nombre + " no es válido.");
            }
            limpios[i] = nombre.length() > 30 ? nombre.substring(0, 30) : nombre;
        }
        String formato = "([01]\\d|2[0-3]):[0-5]\\d";
        if (horaM == null || horaT == null || horaN == null
                || !horaM.matches(formato) || !horaT.matches(formato) || !horaN.matches(formato)) {
            throw new IllegalArgumentException("Revisa las horas de entrada.");
        }
        if (horaM.compareTo(horaT) >= 0 || horaT.compareTo(horaN) >= 0) {
            throw new IllegalArgumentException("Las horas deben ir en orden: mañana, tarde y noche.");
        }
        return new Turnos(limpios, p, ref, dias.clone(), horaM, horaT, horaN);
    }

    public char letra(int turno, long dia) {
        long pos = Math.floorMod(dias[turno] + dia - refDia, (long) patron.length());
        return patron.charAt((int) pos);
    }

    public List<Integer> quienes(long dia, char letra) {
        List<Integer> r = new ArrayList<>();
        for (int i = 0; i < NUM; i++) {
            if (letra(i, dia) == letra) {
                r.add(i);
            }
        }
        return r;
    }

    public String listaNombres(List<Integer> lista, String siVacia) {
        if (lista.isEmpty()) {
            return siVacia;
        }
        StringBuilder s = new StringBuilder();
        for (int k = 0; k < lista.size(); k++) {
            if (k > 0) {
                s.append(k == lista.size() - 1 ? " y " : ", ");
            }
            s.append(nombres[lista.get(k)]);
        }
        return s.toString();
    }

    public static int minutos(String hora) {
        return Integer.parseInt(hora.substring(0, 2)) * 60 + Integer.parseInt(hora.substring(3, 5));
    }

    public Franja franjaActual(long hoy, int minutosAhora) {
        if (minutosAhora >= minutos(horaN)) {
            return new Franja(hoy, 'N', false);
        }
        if (minutosAhora >= minutos(horaT)) {
            return new Franja(hoy, 'T', false);
        }
        if (minutosAhora >= minutos(horaM)) {
            return new Franja(hoy, 'M', false);
        }
        return new Franja(hoy - 1, 'N', true);
    }

    /** Minutos del día en que el widget debe redibujarse; 1440 es la medianoche siguiente. */
    public int siguienteCambio(int minutosAhora) {
        int[] marcas = {minutos(horaM), minutos(horaT), minutos(horaN), 1440};
        for (int m : marcas) {
            if (m > minutosAhora) {
                return m;
            }
        }
        return 1440;
    }

    public static String nombreFranja(char letra) {
        switch (letra) {
            case 'M': return "Mañana";
            case 'T': return "Tarde";
            case 'N': return "Noche";
            default: return "Libre";
        }
    }
}
