import java.util.*;

public class ArbolBMas {

    int M; // grado del arbol
           // maximo de claves = M-1 | minimo de claves = (M/2)-1
           // maximo de hijos  = M   | minimo de hijos  = M/2

    static class Nodo {
        ArrayList<Integer> claves = new ArrayList<>();
        ArrayList<Nodo> hijos = new ArrayList<>(); // vacio si es hoja
        Nodo siguienteHoja;                        // solo se usa si esHoja == true
        boolean esHoja;
        Nodo(boolean esHoja) { this.esHoja = esHoja; }
    }

    static class Promocion {
        int clave;
        Nodo derecho;
        Promocion(int clave, Nodo derecho) { this.clave = clave; this.derecho = derecho; }
    }

    static class ResultadoBusqueda {
        int clave;
        int nivel;
        String tipo;
    }

    Nodo raiz;

    ArbolBMas(int m) {
        this.M = m;
        this.raiz = new Nodo(true);
    }

    int minClaves() {
    return (int) Math.ceil(M / 2.0) - 1;
}

    // Punto de insercion simple (cuenta de claves menores). Sirve para leaf insert y,
    // como no hay duplicados, tambien para decidir el hijo por donde bajar al insertar.
    static int puntoInsercion(List<Integer> claves, int clave) {
        int r = Collections.binarySearch(claves, clave);
        if (r >= 0) return r;
        return -(r + 1);
    }

    // Hijo por donde bajar en busqueda/eliminacion: si la clave coincide EXACTO con un
    // separador, en B+ eso significa "vive en el subarbol de la derecha" por convencion.
    static int hijoParaRuta(List<Integer> claves, int clave) {
        int r = Collections.binarySearch(claves, clave);
        if (r >= 0) return r + 1;
        return -(r + 1);
    }

    // ---------------------------- INSERCION ----------------------------

    void insertar(int clave) {
        if (buscarClave(clave)) {
            System.out.println("La clave " + clave + " ya existe, no se inserta duplicada.");
            return;
        }
        Promocion promocion = insertarRec(raiz, clave);
        if (promocion != null) {
            Nodo nuevaRaiz = new Nodo(false);
            nuevaRaiz.claves.add(promocion.clave);
            nuevaRaiz.hijos.add(raiz);
            nuevaRaiz.hijos.add(promocion.derecho);
            raiz = nuevaRaiz;
        }
        System.out.println("Clave " + clave + " insertada.");
    }

    Promocion insertarRec(Nodo n, int clave) {
        if (n.esHoja) {
            int idx = puntoInsercion(n.claves, clave);
            n.claves.add(idx, clave);
            if (n.claves.size() == M) return dividirHoja(n);
            return null;
        }
        int idx = puntoInsercion(n.claves, clave);
        Nodo hijo = n.hijos.get(idx);
        Promocion promocion = insertarRec(hijo, clave);
        if (promocion != null) {
            n.claves.add(idx, promocion.clave);
            n.hijos.add(idx + 1, promocion.derecho);
            if (n.claves.size() == M) return dividirInterno(n);
        }
        return null;
    }

    // Division de un nodo interno con M claves: la de la posicion (M-1)/2 SUBE y se quita
    // (igual regla de mediana / menor-de-los-dos-medios que en el arbol B)
    Promocion dividirInterno(Nodo n) {
        int medio = (M - 1) / 2;
        int claveMedia = n.claves.get(medio);

        Nodo nuevo = new Nodo(false);
        nuevo.claves.addAll(n.claves.subList(medio + 1, n.claves.size()));
        nuevo.hijos.addAll(n.hijos.subList(medio + 1, n.hijos.size()));

        List<Integer> izquierda = new ArrayList<>(n.claves.subList(0, medio));
        n.claves.clear();
        n.claves.addAll(izquierda);
        List<Nodo> hijosIzq = new ArrayList<>(n.hijos.subList(0, medio + 1));
        n.hijos.clear();
        n.hijos.addAll(hijosIzq);

        return new Promocion(claveMedia, nuevo);
    }

    // Division de una hoja con M claves: la de la posicion (M-1)/2 se COPIA hacia el padre
    // (se queda en la hoja derecha ademas de subir)
    Promocion dividirHoja(Nodo hoja) {
        int medio = (M - 1) / 2;
        int claveCopia = hoja.claves.get(medio);

        Nodo nueva = new Nodo(true);
        nueva.claves.addAll(hoja.claves.subList(medio, hoja.claves.size()));

        List<Integer> izquierda = new ArrayList<>(hoja.claves.subList(0, medio));
        hoja.claves.clear();
        hoja.claves.addAll(izquierda);

        nueva.siguienteHoja = hoja.siguienteHoja;
        hoja.siguienteHoja = nueva;

        return new Promocion(claveCopia, nueva);
    }

    // ---------------------------- BUSQUEDA ----------------------------

    boolean buscarClave(int clave) {
        Nodo n = raiz;
        while (!n.esHoja) {
            n = n.hijos.get(hijoParaRuta(n.claves, clave));
        }
        return Collections.binarySearch(n.claves, clave) >= 0;
    }

    ResultadoBusqueda buscar(int clave) {
        Nodo n = raiz;
        int nivel = 0;
        while (!n.esHoja) {
            n = n.hijos.get(hijoParaRuta(n.claves, clave));
            nivel++;
        }
        int idx = Collections.binarySearch(n.claves, clave);
        if (idx < 0) return null;

        ResultadoBusqueda r = new ResultadoBusqueda();
        r.clave = n.claves.get(idx);
        r.nivel = nivel;
        if (n == raiz) {
            r.tipo = "raiz (que tambien es hoja)";
        } else {
            r.tipo = "hoja";
        }
        return r;
    }

    // ---------------------------- ELIMINACION ----------------------------
    // Los datos reales solo viven en las hojas, asi que ahi es donde se borra de verdad.
    // Si una hoja (o un nodo interno) queda por debajo del minimo, primero se intenta pedir
    // prestado a un hermano; si ninguno puede prestar, se fusionan.

    void eliminar(int clave) {
        boolean existia = buscarClave(clave);
        if (existia) {
            eliminarRec(raiz, clave);
            if (!raiz.esHoja && raiz.claves.isEmpty()) {
                raiz = raiz.hijos.get(0);
            }
            System.out.println("Clave " + clave + " eliminada.");
        } else {
            System.out.println("Clave " + clave + " no existia.");
        }
    }

    boolean eliminarRec(Nodo n, int clave) {
        if (n.esHoja) {
            int idx = Collections.binarySearch(n.claves, clave);
            if (idx < 0) return false;
            n.claves.remove(idx);
            return true;
        }
        int idx = hijoParaRuta(n.claves, clave);
        boolean seElimino = eliminarRec(n.hijos.get(idx), clave);
        if (seElimino) rellenar(n, idx);
        return seElimino;
    }

    void rellenar(Nodo n, int idx) {
        Nodo hijo = n.hijos.get(idx);
        if (hijo.claves.size() >= minClaves()) return;

        boolean tieneIzq = idx > 0;
        boolean tieneDer = idx < n.hijos.size() - 1;

        if (tieneIzq && n.hijos.get(idx - 1).claves.size() > minClaves()) {
            prestarDeIzquierda(n, idx);
        } else if (tieneDer && n.hijos.get(idx + 1).claves.size() > minClaves()) {
            prestarDeDerecha(n, idx);
        } else if (tieneIzq) {
            fusionar(n, idx - 1);
        } else {
            fusionar(n, idx);
        }
    }

    void prestarDeIzquierda(Nodo n, int idx) {
        Nodo hijo = n.hijos.get(idx);
        Nodo izq = n.hijos.get(idx - 1);
        if (hijo.esHoja) {
            int prestada = izq.claves.remove(izq.claves.size() - 1);
            hijo.claves.add(0, prestada);
            // el separador debe reflejar la nueva clave minima de la hoja actual
            n.claves.set(idx - 1, hijo.claves.get(0));
        } else {
            hijo.claves.add(0, n.claves.get(idx - 1));
            hijo.hijos.add(0, izq.hijos.remove(izq.hijos.size() - 1));
            n.claves.set(idx - 1, izq.claves.remove(izq.claves.size() - 1));
        }
    }

    void prestarDeDerecha(Nodo n, int idx) {
        Nodo hijo = n.hijos.get(idx);
        Nodo der = n.hijos.get(idx + 1);
        if (hijo.esHoja) {
            int prestada = der.claves.remove(0);
            hijo.claves.add(prestada);
            // el separador debe reflejar la nueva clave minima del hermano derecho
            n.claves.set(idx, der.claves.get(0));
        } else {
            hijo.claves.add(n.claves.get(idx));
            hijo.hijos.add(der.hijos.remove(0));
            n.claves.set(idx, der.claves.remove(0));
        }
    }

    void fusionar(Nodo n, int idx) {
        Nodo izq = n.hijos.get(idx);
        Nodo der = n.hijos.get(idx + 1);
        if (izq.esHoja) {
            izq.claves.addAll(der.claves);
            izq.siguienteHoja = der.siguienteHoja;
            n.claves.remove(idx); // el separador era solo una copia, se descarta
            n.hijos.remove(idx + 1);
        } else {
            izq.claves.add(n.claves.remove(idx)); // aqui si es un separador real, baja
            izq.claves.addAll(der.claves);
            izq.hijos.addAll(der.hijos);
            n.hijos.remove(idx + 1);
        }
    }

    // ---------------------------- VISUALIZACION ----------------------------

    void mostrar() {
        Nodo n = raiz;
        while (!n.esHoja) n = n.hijos.get(0);
        System.out.print("Arbol B+ (hojas enlazadas): ");
        while (n != null) {
            for (int c : n.claves) System.out.print(c + " ");
            n = n.siguienteHoja;
        }
        System.out.println();
    }

    void mostrarEstructura() {
        System.out.println("Estructura por niveles:");
        mostrarEstructura(raiz, 0);
    }

    void mostrarEstructura(Nodo n, int nivel) {
        if (n == null) return;
        String sangria = "  ".repeat(nivel);
        String tipo;
        if (n == raiz && n.esHoja) tipo = "raiz/hoja";
        else if (n == raiz) tipo = "raiz";
        else if (n.esHoja) tipo = "hoja";
        else tipo = "interno";
        System.out.println(sangria + "[nivel " + nivel + ", " + tipo + "] " + n.claves);
        if (!n.esHoja) {
            for (Nodo hijo : n.hijos) mostrarEstructura(hijo, nivel + 1);
        }
    }

    // ---------------------------- ENTRADA VALIDADA ----------------------------

    static int leerEntero(Scanner sc, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = sc.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.println("Dato invalido, debe ser un numero entero. Intente de nuevo.");
            }
        }
    }

    static int leerEnteroConMinimo(Scanner sc, String mensaje, int minimo) {
        while (true) {
            int valor = leerEntero(sc, mensaje);
            if (valor >= minimo) return valor;
            System.out.println("El valor debe ser mayor o igual a " + minimo + ". Intente de nuevo.");
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int m = leerEnteroConMinimo(sc, "Ingrese el grado M del arbol (M >= 3): ", 3);

        ArbolBMas arbol = new ArbolBMas(m);
        System.out.println("Cada nodo (menos la raiz) tendra entre " + arbol.minClaves() + " y " + (m - 1)
        + " claves, y entre " + (int) Math.ceil(m / 2.0) + " y " + m + " hijos.");

        int opcion;
        do {
            System.out.println("\n1. Insertar  2. Eliminar  3. Buscar  4. Mostrar  5. Ver estructura  0. Salir");
            opcion = leerEntero(sc, "Opcion: ");
            switch (opcion) {
                case 1:
                    int c1 = leerEntero(sc, "Clave (int): ");
                    arbol.insertar(c1);
                    arbol.mostrar();
                    break;
                case 2:
                    int c2 = leerEntero(sc, "Clave a eliminar: ");
                    arbol.eliminar(c2);
                    arbol.mostrar();
                    break;
                case 3:
                    int c3 = leerEntero(sc, "Clave a buscar: ");
                    ResultadoBusqueda encontrada = arbol.buscar(c3);
                    if (encontrada != null) {
                        System.out.println("Encontrada -> clave: " + encontrada.clave
                                + ", nivel: " + encontrada.nivel
                                + ", tipo de nodo: " + encontrada.tipo);
                    } else {
                        System.out.println("No encontrada.");
                    }
                    break;
                case 4:
                    arbol.mostrar();
                    break;
                case 5:
                    arbol.mostrarEstructura();
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opcion invalida, intente de nuevo.");
            }
        } while (opcion != 0);
        sc.close();
    }
}
