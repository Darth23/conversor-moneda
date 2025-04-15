import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;


public class ConversorMonedas {

    private static class ConversionRecord {
        String monedaOrigen;
        String monedaDestino;
        double cantidad;
        double resultado;
        LocalDateTime timestamp;

        ConversionRecord(String origen, String destino, double cant, double res) {
            this.monedaOrigen = origen;
            this.monedaDestino = destino;
            this.cantidad = cant;
            this.resultado = res;
            this.timestamp = LocalDateTime.now();
        }
    }


    private static final String API_KEY = "tu-api-key";
    private static final String URL_BASE = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/";
    private static Set<String> monedasDisponibles;
    private static final Gson GSON = new Gson();

    private static final Map<String, String> NOMBRES_MONEDAS = new HashMap<>();
    private static final List<ConversionRecord> HISTORIAL = new ArrayList<>(); // Almacena historial
    private static final List<String[]> CONVERSIONES_RAPIDAS = List.of( // Lista de conversiones rápidas
            new String[]{"USD", "MXN"},
            new String[]{"ARS", "MXN"},
            new String[]{"BRL", "CAD"},
            new String[]{"CNY", "USD"},
            new String[]{"COP", "ARS"}
    );


    static {
        NOMBRES_MONEDAS.put("AED", "AED - Emiratos Árabes Unidos (Dirham)");
        NOMBRES_MONEDAS.put("ARS", "ARS - Argentina (Peso Argentino)");
        NOMBRES_MONEDAS.put("AUD", "AUD - Australia (Dólar Australiano)");
        NOMBRES_MONEDAS.put("BGN", "BGN - Bulgaria (Lev)");
        NOMBRES_MONEDAS.put("BRL", "BRL - Brasil (Real Brasileño)");
        NOMBRES_MONEDAS.put("CAD", "CAD - Canadá (Dólar Canadiense)");
        NOMBRES_MONEDAS.put("CHF", "CHF - Suiza (Franco Suizo)");
        NOMBRES_MONEDAS.put("CLP", "CLP - Chile (Peso Chileno)");
        NOMBRES_MONEDAS.put("CNY", "CNY - China (Yuan)");
        NOMBRES_MONEDAS.put("COP", "COP - Colombia (Peso Colombiano)");
        NOMBRES_MONEDAS.put("CZK", "CZK - República Checa (Corona Checa)");
        NOMBRES_MONEDAS.put("DKK", "DKK - Dinamarca (Corona Danesa)");
        NOMBRES_MONEDAS.put("EGP", "EGP - Egipto (Libra Egipcia)");
        NOMBRES_MONEDAS.put("EUR", "EUR - Zona Euro (Euro)");
        NOMBRES_MONEDAS.put("GBP", "GBP - Reino Unido (Libra Esterlina)");
        NOMBRES_MONEDAS.put("HKD", "HKD - Hong Kong (Dólar de Hong Kong)");
        NOMBRES_MONEDAS.put("HRK", "HRK - Croacia (Kuna)");
        NOMBRES_MONEDAS.put("HUF", "HUF - Hungría (Forinto Húngaro)");
        NOMBRES_MONEDAS.put("IDR", "IDR - Indonesia (Rupia Indonesia)");
        NOMBRES_MONEDAS.put("ILS", "ILS - Israel (Nuevo Shéquel)");
        NOMBRES_MONEDAS.put("INR", "INR - India (Rupia India)");
        NOMBRES_MONEDAS.put("JPY", "JPY - Japón (Yen Japonés)");
        NOMBRES_MONEDAS.put("KRW", "KRW - Corea del Sur (Won Surcoreano)");
        NOMBRES_MONEDAS.put("MXN", "MXN - México (Peso Mexicano)");
        NOMBRES_MONEDAS.put("MYR", "MYR - Malasia (Ringgit)");
        NOMBRES_MONEDAS.put("NOK", "NOK - Noruega (Corona Noruega)");
        NOMBRES_MONEDAS.put("NZD", "NZD - Nueva Zelanda (Dólar Neozelandés)");
        NOMBRES_MONEDAS.put("PEN", "PEN - Perú (Sol Peruano)");
        NOMBRES_MONEDAS.put("PHP", "PHP - Filipinas (Peso Filipino)");
        NOMBRES_MONEDAS.put("PLN", "PLN - Polonia (Zloty)");
        NOMBRES_MONEDAS.put("RON", "RON - Rumania (Leu Rumano)");
        NOMBRES_MONEDAS.put("RUB", "RUB - Rusia (Rublo Ruso)");
        NOMBRES_MONEDAS.put("SAR", "SAR - Arabia Saudita (Riyal Saudí)");
        NOMBRES_MONEDAS.put("SEK", "SEK - Suecia (Corona Sueca)");
        NOMBRES_MONEDAS.put("SGD", "SGD - Singapur (Dólar de Singapur)");
        NOMBRES_MONEDAS.put("THB", "THB - Tailandia (Baht Tailandés)");
        NOMBRES_MONEDAS.put("TRY", "TRY - Turquía (Lira Turca)");
        NOMBRES_MONEDAS.put("TWD", "TWD - Taiwán (Dólar Taiwanés)");
        NOMBRES_MONEDAS.put("UAH", "UAH - Ucrania (Grivna)");
        NOMBRES_MONEDAS.put("USD", "USD - Estados Unidos (Dólar Estadounidense)");
        NOMBRES_MONEDAS.put("UYU", "UYU - Uruguay (Peso Uruguayo)");
        NOMBRES_MONEDAS.put("VND", "VND - Vietnam (Dong Vietnamita)");
        NOMBRES_MONEDAS.put("ZAR", "ZAR - Sudáfrica (Rand Sudafricano)");
    }
    // Clases para mapear la respuesta JSON
    private static class ApiResponse {
        String result;
        @SerializedName("conversion_rates")
        Map<String, Double> conversionRates;

        boolean isSuccess() {
            return "success".equals(result);
        }
    }

    public static void main(String[] args) {
        try {
            cargarMonedasDisponibles();
            mostrarMenuPrincipal();
        } catch (IOException e) {
            System.out.println("Error inicial: " + e.getMessage());
        }
    }

    private static void mostrarMenuPrincipal() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.println("\n=== CONVERSOR DE MONEDAS ===");
                System.out.println("1. Realizar conversión manual");
                System.out.println("2. Conversiones rápidas");
                System.out.println("3. Ver lista de monedas");
                System.out.println("4. Ver historial de conversiones");
                System.out.println("5. Salir");
                System.out.print("Seleccione una opción: ");
                String opcion = reader.readLine();
                switch (opcion) {
                    case "1":
                        realizarConversion(reader);
                        break;
                    case "2":
                        mostrarConversionesRapidas(reader);
                        break;
                    case "3":
                        mostrarMonedasDisponibles();
                        break;
                    case "4":
                        mostrarHistorial();
                        break;
                    case "5":
                        System.out.println("¡Hasta luego!");
                        return;
                    default:
                        System.out.println("Opción no válida");
                }

                if (!continuarPrograma(reader)) {
                    System.out.println("¡Hasta luego!");
                    return;
                }
            }
        }
    }

    private static void realizarConversion(BufferedReader reader) throws IOException {
        try {
            String monedaOrigen = obtenerMoneda(reader, "origen");
            String monedaDestino = obtenerMoneda(reader, "destino");

            System.out.print("Cantidad a convertir: ");
            double cantidad = Double.parseDouble(reader.readLine());

            double tasa = obtenerTasaConversion(monedaOrigen, monedaDestino);
            double resultado = cantidad * tasa;

            System.out.printf("\n%.2f %s = %.2f %s\n",
                    cantidad,
                    obtenerNombreCompleto(monedaOrigen),
                    resultado,
                    obtenerNombreCompleto(monedaDestino));

        } catch (NumberFormatException e) {
            System.out.println("Cantidad no válida");
        }
    }

    private static void mostrarConversionesRapidas(BufferedReader reader) throws IOException {
        System.out.println("\n=== CONVERSIONES RÁPIDAS ===");
        for (int i = 0; i < CONVERSIONES_RAPIDAS.size(); i++) {
            String[] conversion = CONVERSIONES_RAPIDAS.get(i);
            System.out.printf("%d. %s a %s\n", i+1,
                    obtenerNombreCompleto(conversion[0]),
                    obtenerNombreCompleto(conversion[1]));
        }
        System.out.print("Seleccione una conversión (1-5): ");
        int seleccion = Integer.parseInt(reader.readLine()) - 1;

        if (seleccion < 0 || seleccion >= CONVERSIONES_RAPIDAS.size()) {
            System.out.println("Opción inválida");
            return;
        }

        String[] conversion = CONVERSIONES_RAPIDAS.get(seleccion);
        System.out.print("Cantidad a convertir: ");
        double cantidad = Double.parseDouble(reader.readLine());

        double tasa = obtenerTasaConversion(conversion[0], conversion[1]);
        double resultado = cantidad * tasa;

        // Registrar en historial
        HISTORIAL.add(new ConversionRecord(conversion[0], conversion[1], cantidad, resultado));

        System.out.printf("\n%.2f %s = %.2f %s\n",
                cantidad,
                obtenerNombreCompleto(conversion[0]),
                resultado,
                obtenerNombreCompleto(conversion[1]));
    }

    private static void mostrarHistorial() {
        if (HISTORIAL.isEmpty()) {
            System.out.println("\nNo hay conversiones registradas");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        System.out.println("\n=== HISTORIAL DE CONVERSIONES ===");
        for (ConversionRecord record : HISTORIAL) {
            System.out.printf("[%s] %.2f %s → %.2f %s\n",
                    record.timestamp.format(formatter),
                    record.cantidad,
                    obtenerNombreCompleto(record.monedaOrigen),
                    record.resultado,
                    obtenerNombreCompleto(record.monedaDestino));
        }
    }


    private static boolean continuarPrograma(BufferedReader reader) throws IOException {
        while (true) {
            System.out.print("\n¿Desea realizar otra operación? (s/n): ");
            String respuesta = reader.readLine().toLowerCase();

            if (respuesta.equals("s") || respuesta.equals("si")) {
                return true;
            } else if (respuesta.equals("n") || respuesta.equals("no")) {
                return false;
            }
            System.out.println("Respuesta no válida");
        }
    }


    private static void cargarMonedasDisponibles() throws IOException {
        ApiResponse response = obtenerDatosAPI("USD");
        if (response == null || !response.isSuccess()) {
            throw new IOException("Error al cargar monedas disponibles");
        }
        if (response.conversionRates == null || response.conversionRates.isEmpty()) {
            throw new IOException("No se encontraron tasas de conversión");
        }
        monedasDisponibles = response.conversionRates.keySet();
    }

    private static String obtenerMoneda(BufferedReader reader, String tipo) throws IOException {
        while (true) {
            System.out.printf("\nIngrese moneda %s (3 letras) o 'lista' para ver opciones: ", tipo);
            String input = reader.readLine().toUpperCase();

            if (input.equalsIgnoreCase("lista")) {
                mostrarMonedasDisponibles();
            } else if (monedasDisponibles.contains(input)) {
                return input;
            } else {
                System.out.println("Moneda no válida. Intente nuevamente.");
            }
        }
    }


    private static String obtenerNombreCompleto(String codigoMoneda) {
        return NOMBRES_MONEDAS.getOrDefault(codigoMoneda, codigoMoneda);
    }

    private static void mostrarMonedasDisponibles() {
        System.out.println("\nMonedas disponibles:");
        List<String> monedasOrdenadas = new ArrayList<>(monedasDisponibles);
        Collections.sort(monedasOrdenadas);

        for (int i = 0; i < monedasOrdenadas.size(); i++) {
            String codigo = monedasOrdenadas.get(i);
            String descripcion = NOMBRES_MONEDAS.getOrDefault(codigo, codigo);
            System.out.printf("%-35s", descripcion);  // Formato de columnas
            if (i % 2 == 1) System.out.println();    // 2 columnas por línea
        }
        System.out.println("\n");
    }

    private static double obtenerTasaConversion(String desde, String hacia) throws IOException {
        ApiResponse response = obtenerDatosAPI(desde);
        if (response != null && response.isSuccess()) {
            return response.conversionRates.get(hacia);
        }
        throw new IOException("No se pudo obtener la tasa de conversión");
    }

    private static ApiResponse obtenerDatosAPI(String monedaBase) throws IOException {
        try {
            URL url = new URL(URL_BASE + monedaBase);
            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("GET");

            if (conexion.getResponseCode() != 200) {
                throw new IOException("Error en la conexión: " + conexion.getResponseCode());
            }

            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(conexion.getInputStream()))) {
                return GSON.fromJson(lector, ApiResponse.class);
            }
        } catch (Exception e) {
            throw new IOException("Error al procesar la respuesta: " + e.getMessage());
        }
    }

}
