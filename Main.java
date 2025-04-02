import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;

class Buffer {
    private final LinkedList<Integer> list = new LinkedList<>();
    private final int capacity;
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore items = new Semaphore(0);
    private final Semaphore space;

    public Buffer(int capacity) {
        this.capacity = capacity;
        this.space = new Semaphore(capacity);
    }
    
    public void put(int num) throws InterruptedException {
        space.acquire(); // Espera si el buffer está lleno
        mutex.acquire();
        list.add(num);
        String bufferVisual = getBufferVisual(list.size() - 1, "\u001B[32m"); // Verde para añadir
        if (num != -1) {
            System.out.println("\u001B[32m--> [PRODUCTOR] Puso: " + num + " | Buffer: " + bufferVisual + "\u001B[0m");
        }
        mutex.release();
        items.release();
    }
    
    public int takeIf(Predicate<Integer> predicate) throws InterruptedException {
        while (true) {
            items.acquire(); // Espera si el buffer está vacío
            mutex.acquire();
            Iterator<Integer> iter = list.iterator();
            int index = 0;
            int removedIndex = -1;
            int num = -1;
            while (iter.hasNext()) {
                num = iter.next();
                if (predicate.test(num)) {
                    iter.remove();
                    removedIndex = index;
                    break;
                }
                index++;
            }
            if (removedIndex != -1) {
                String bufferVisual = getBufferVisual(removedIndex, "\u001B[31m"); // Rojo para quitar
                System.out.println("\u001B[31m<-- [CONSUMIDOR] Tomó: " + num + " | Buffer: " + bufferVisual + "\u001B[0m");
                mutex.release();
                space.release();
                return num;
            }
            mutex.release();
            items.release();
        }
    }

    private String getBufferVisual(int highlightIndex, String color) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < capacity; i++) {
            if (i == highlightIndex) {
                sb.append(color); // Aplicar color al elemento modificado
            }
            sb.append("[");
            if (i < list.size()) {
                sb.append(list.get(i));
            } else {
                sb.append(" ");
            }
            sb.append("]");
            if (i == highlightIndex) {
                sb.append("\u001B[0m"); // Resetear color
            }
        }
        // Añadir un indicador visual del estado del buffer
        sb.append(" ");
        if (list.size() == capacity) {
            sb.append("\u001B[41m LLENO \u001B[0m");
        } else if (list.size() == 0) {
            sb.append("\u001B[44m VACÍO \u001B[0m");
        } else {
            sb.append("\u001B[42m " + list.size() + "/" + capacity + " \u001B[0m");
        }
        return sb.toString();
    }

    // Método para mostrar el estado actual del buffer en cualquier momento
    public void mostrarEstadoBuffer() throws InterruptedException {
        mutex.acquire();
        String bufferVisual = getBufferVisual(-1, "");
        System.out.println("\u001B[36m[ESTADO] Buffer actual: " + bufferVisual + "\u001B[0m");
        mutex.release();
    }
}

public class Main {
    public static boolean isPrime(int n) {
        if (n <= 1) return false;
        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) return false;
        }
        return true;
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String filePath = null;

        while (true) {
            System.out.println("\n\u001B[1;34m======= SISTEMA PRODUCTOR-CONSUMIDOR =======\u001B[0m");
            System.out.println("1. Especificar ruta del archivo");
            System.out.println("2. Ejecutar programa");
            System.out.println("3. Salir");
            System.out.print("Seleccione una opción: ");
            int opcion = scanner.nextInt();
            scanner.nextLine();

            switch (opcion) {
                case 1:
                    System.out.print("Ingrese la ruta del archivo: ");
                    filePath = scanner.nextLine();
                    System.out.println("Ruta configurada: " + filePath);
                    break;
                case 2:
                    if (filePath == null) {
                        System.out.println("Primero seleccione un archivo (Opción 1)");
                        break;
                    }
                    List<Integer> numeros = cargarNumerosDesdeArchivo(filePath);
                    if (numeros != null) {
                        ejecutarProceso(numeros);
                    }
                    break;
                case 3:
                    System.out.println("Saliendo...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }
    
    private static List<Integer> cargarNumerosDesdeArchivo(String filePath) {
        List<Integer> numeros = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                try {
                    int num = Integer.parseInt(linea.trim());
                    numeros.add(num);
                } catch (NumberFormatException e) {
                    System.out.println("Error: '" + linea + "' no es un número válido");
                    return null;
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo archivo: " + e.getMessage());
            return null;
        }
        return numeros;
    }
    
    private static void ejecutarProceso(List<Integer> numeros) {
        Buffer buffer = new Buffer(5);
        
        // Mostrar animación de inicio
        mostrarAnimacionInicio();
        
        Thread productor = new Thread(() -> {
            try {
                // Añadir un pequeño retraso antes de empezar
                Thread.sleep(500);
                System.out.println("\u001B[32m[PRODUCTOR] Iniciando producción...\u001B[0m");
                Thread.sleep(500);
                
                for (int num : numeros) {
                    // Mostrar que el productor está trabajando
                    System.out.println("\u001B[32m[PRODUCTOR] Preparando número: " + num + "...\u001B[0m");
                    Thread.sleep(300); // Delay para simular trabajo
                    
                    buffer.put(num);
                    Thread.sleep(500); // Delay mayor para ver mejor el proceso
                }
                
                System.out.println("\u001B[32m[PRODUCTOR] Terminando producción (enviando señales de terminación)...\u001B[0m");
                // Envía señal de terminación a cada consumidor
                for (int i = 0; i < 3; i++) {
                    buffer.put(-1);
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        
        Thread[] consumidores = new Thread[3];
        consumidores[0] = new Thread(() -> consumir(buffer, n -> n == -1 || n % 2 == 0, "PARES"));
        consumidores[1] = new Thread(() -> consumir(buffer, n -> n == -1 || n % 2 != 0, "IMPARES"));
        consumidores[2] = new Thread(() -> consumir(buffer, n -> n == -1 || isPrime(n), "PRIMOS"));

        // Thread para mostrar periódicamente el estado del buffer
        Thread monitorBuffer = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(2000); // Cada 2 segundos
                    buffer.mostrarEstadoBuffer();
                }
            } catch (InterruptedException e) {
                // Thread interrumpido, terminar
            }
        });
        
        productor.start();
        for (Thread t : consumidores) t.start();
        monitorBuffer.start();

        try {
            productor.join();
            for (Thread t : consumidores) t.join();
            // Detener el monitor una vez que terminen los demás threads
            monitorBuffer.interrupt();
            monitorBuffer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Mostrar animación de finalización
        mostrarAnimacionFin();
    }
    
    private static void consumir(Buffer buffer, Predicate<Integer> condicion, String nombre) {
        int suma = 0;
        String color = "";
        String emoji = "";
        switch (nombre) {
            case "PARES": color = "\u001B[36m"; emoji = "^_^"; break;   // Cyan
            case "IMPARES": color = "\u001B[35m"; emoji = "._."; break; // Magenta
            case "PRIMOS": color = "\u001B[33m"; emoji = "~_~"; break;  // Amarillo
        }
        try {
            System.out.printf("%s%s [%s] Iniciando consumidor...\u001B[0m\n", emoji, color, nombre);
            Thread.sleep(300);
            
            while (true) {
                System.out.printf("%s%s [%s] Esperando producto...\u001B[0m\n", emoji, color, nombre);
                int num = buffer.takeIf(condicion);
                if (num == -1) {
                    System.out.printf("%s%s [%s] Recibió señal de terminación\u001B[0m\n", emoji, color, nombre);
                    buffer.put(-1); // Vuelve a poner la señal para otro consumidor
                    break;
                }
                suma += num;
                System.out.printf("%s%s [%s] Consumió: %d | Suma: %d\u001B[0m\n", 
                                emoji, color, nombre, num, suma);
                Thread.sleep(400); // Pausa para visualizar mejor
            }
            System.out.printf("%s%s [%s] Suma final: %d\u001B[0m\n", 
                            emoji, color, nombre, suma);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private static void mostrarAnimacionInicio() {
        try {
            System.out.println("\u001B[1;33m");
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║   INICIANDO SISTEMA PRODUCTOR-CONSUMIDOR   ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println("\u001B[0m");
            
            // Animación de carga
            System.out.print("Preparando buffer ");
            for (int i = 0; i < 10; i++) {
                System.out.print("■");
                Thread.sleep(100);
            }
            System.out.println(" ¡Listo!");
            
            // Leyenda
            System.out.println("\u001B[1;36m==== LEYENDA ====\u001B[0m");
            System.out.println("\u001B[32m--> [PRODUCTOR]\u001B[0m - Añade elementos al buffer");
            System.out.println("\u001B[31m<-- [CONSUMIDOR]\u001B[0m - Quita elementos del buffer");
            System.out.println("\u001B[36m^_^ [PARES]\u001B[0m - Consume números pares");
            System.out.println("\u001B[35m._. [IMPARES]\u001B[0m - Consume números impares");
            System.out.println("\u001B[33m~_~ [PRIMOS]\u001B[0m - Consume números primos");
            
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private static void mostrarAnimacionFin() {
        try {
            System.out.println("\u001B[1;33m");
            System.out.println("╔════════════════════════════════╗");
            System.out.println("║   PROCESO COMPLETADO CON ÉXITO   ║"); 
            System.out.println("╚════════════════════════════════╝");
            System.out.println("\u001B[0m");
            
            // Animación final
            for (int i = 0; i < 3; i++) {
                System.out.print("\r\u001B[32mFinalizando recursos ");
                Thread.sleep(300);
                System.out.print("\r\u001B[32mFinalizando recursos *");
                Thread.sleep(300);
                System.out.print("\r\u001B[32mFinalizando recursos **");
                Thread.sleep(300);
                System.out.print("\r\u001B[32mFinalizando recursos ***");
                Thread.sleep(300);
            }
            System.out.println("\r\u001B[32mRecursos liberados correctamente! :))         \u001B[0m");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}