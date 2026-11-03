package es.ruben.quientrabaja;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Cálculo de turnos. No depende de Android para poder probarlo por separado. */
public final class Turnos {
    public static final int NUM = 6;
    public static final int CICLO_COORD = 5;
    public static final int MAX_COORD = 10;

    public final String[] nombres;
    public final String patron;
    public final String ref;
    public final int[] dias;
    public final String horaM;
    public final String horaT;
    public final String horaN;
    public final Coord coord;
    private final long refDia;

    /** Coordinadores de servicio: trabajan 1 día y libran 4. */
    public static final class Coord {
        public final String[] nombres;
        public final int[] dias;
        public final String ref;
        public final String entrada;
        public final String salida;
        private final long refDia;

        Coord(String[] nombres, int[] dias, String ref, String entrada, String salida) {
            this.nombres = nombres;
            this.dias = dias;
            this.ref = ref;
            this.entrada = entrada;
            this.salida = salida;
            this.refDia = LocalDate.parse(ref).toEpochDay();
        }

        public boolean trabaja(int i, long dia) {
            return Math.floorMod(dias[i] + dia - refDia, (long) CICLO_COORD) == 0;
        }

        public List<Integer> quienes(long dia) {
            List<Integer> r = new ArrayList<>();
            for (int i = 0; i < nombres.length; i++) {
                if (trabaja(i, dia)) {
                    r.add(i);
                }
            }
            return r;
        }

        public String listaNombres(List<Integer> lista) {
            if (lista.isEmpty()) {
                return "Sin coordinador";
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

        /** Servicio en curso o, si no hay ninguno, el siguiente. */
        public Servicio actual(long hoy, int minutosAhora) {
            int ent = minutos(entrada);
            int sal = minutos(salida);
            if (ent > sal) {
                if (minutosAhora >= ent) {
                    return new Servicio(hoy, true);
                }
                if (minutosAhora < sal) {
                    return new Servicio(hoy - 1, true);
                }
                return new Servicio(hoy, false);
            }
            if (minutosAhora >= ent && minutosAhora < sal) {
                return new Servicio(hoy, true);
            }
            return new Servicio(minutosAhora < ent ? hoy : hoy + 1, false);
        }
    }

    /** Día en que empieza el servicio y si está en curso. */
    public static final class Servicio {
        public final long dia;
        public final boolean activo;

        Servicio(long dia, boolean activo) {
            this.dia = dia;
            this.activo = activo;
        }
    }

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
                   String horaM, String horaT, String horaN, Coord coord) {
        this.nombres = nombres;
        this.patron = patron;
        this.ref = ref;
        this.dias = dias;
        this.horaM = horaM;
        this.horaT = horaT;
        this.horaN = horaN;
        this.coord = coord;
        this.refDia = LocalDate.parse(ref).toEpochDay();
    }

    public static Turnos porDefecto() {
        return new Turnos(
                new String[] {"Turno 1", "Turno 2", "Turno 3", "Turno 4", "Turno 5", "Turno 6"},
                "MMTTNNLLLLLL", "2026-01-01", new int[] {0, 2, 4, 6, 8, 10},
                "06:00", "14:00", "22:00", coordPorDefecto());
    }

    public static Coord coordPorDefecto() {
        return new Coord(
                new String[] {"Coordinador 1", "Coordinador 2", "Coordinador 3", "Coordinador 4", "Coordinador 5"},
                new int[] {0, 1, 2, 3, 4}, "2026-01-01", "18:00", "07:00");
    }

    public static Coord validarCoord(String[] nombres, int[] dias, String ref, String entrada, String salida) {
        if (nombres == null || dias == null || nombres.length != dias.length) {
            throw new IllegalArgumentException("Revisa los coordinadores.");
        }
        if (nombres.length < 1 || nombres.length > MAX_COORD) {
            throw new IllegalArgumentException("Debe haber entre 1 y " + MAX_COORD + " coordinadores.");
        }
        try {
            LocalDate.parse(ref == null ? "" : ref);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha de referencia de los coordinadores no es válida.");
        }
        String formato = "([01]\\d|2[0-3]):[0-5]\\d";
        if (entrada == null || salida == null || !entrada.matches(formato) || !salida.matches(formato)
                || entrada.equals(salida)) {
            throw new IllegalArgumentException("Revisa el horario de los coordinadores.");
        }
        String[] limpios = new String[nombres.length];
        for (int i = 0; i < nombres.length; i++) {
            String nombre = nombres[i] == null ? "" : nombres[i].trim();
            if (nombre.isEmpty()) {
                throw new IllegalArgumentException("Todos los coordinadores deben tener nombre.");
            }
            if (dias[i] < 0 || dias[i] >= CICLO_COORD) {
                throw new IllegalArgumentException("El ciclo de " + nombre + " no es válido.");
            }
            limpios[i] = nombre.length() > 30 ? nombre.substring(0, 30) : nombre;
        }
        return new Coord(limpios, dias.clone(), ref, entrada, salida);
    }

    /** Valida con las mismas reglas que la app web. Lanza IllegalArgumentException con el motivo. */
    public static Turnos validar(String[] nombres, String patron, String ref, int[] dias,
                                 String horaM, String horaT, String horaN, Coord coord) {
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
        return new Turnos(limpios, p, ref, dias.clone(), horaM, horaT, horaN,
                coord == null ? coordPorDefecto() : coord);
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
        int[] marcas = {minutos(horaM), minutos(horaT), minutos(horaN),
                minutos(coord.entrada), minutos(coord.salida)};
        int siguiente = 1440;
        for (int m : marcas) {
            if (m > minutosAhora && m < siguiente) {
                siguiente = m;
            }
        }
        return siguiente;
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
