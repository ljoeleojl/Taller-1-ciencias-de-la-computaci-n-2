import java.util.*;

public class ArbolB {

    int M; // grado del arbol
           // maximo de claves = M-1 | minimo de claves = (M/2)-1
           // maximo de hijos  = M   | minimo de hijos  = M/2

    static class Nodo {
        ArrayList<Integer> claves = new ArrayList<>();
        ArrayList<Nodo> hijos = new ArrayList<>(); // vacio si es hoja (size = claves.size()+1 si no)
        boolean esHoja;
        Nodo(boolean esHoja) { this.esHoja = esHoja; }
    }

    // Lo que "sube" cuando un nodo se divide: la clave mediana y el nuevo nodo derecho
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

    ArbolB(int m) {
        this.M = m;
        this.raiz = new Nodo(true);
    }

    int minClaves() {
    return (int) Math.ceil(M / 2.0) - 1; 
}

    // Punto donde clave deberia insertarse / hijo por donde bajar, via busqueda binaria
    static int puntoInsercion(List<Integer> claves, int clave) {
        int r = Collections.binarySearch(claves, clave);
        if (r >= 0) return r; // ya existe (solo relevante en busqueda/eliminacion)
        return -(r + 1);
    }

    // ---------------------------- INSERCION ----------------------------

    void insertar(int clave) {
        if (buscarClave(clave) != null) {
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
        int idx = puntoInsercion(n.claves, clave);
        if (n.esHoja) {
            n.claves.add(idx, clave);
            if (n.claves.size() == M) return dividir(n);
            return null;
        }
        Nodo hijo = n.hijos.get(idx);
        Promocion promocion = insertarRec(hijo, clave);
        if (promocion != null) {
            n.claves.add(idx, promocion.clave);
            n.hijos.add(idx + 1, promocion.derecho);
            if (n.claves.size() == M) return dividir(n);
        }
        return null;
    }

    // Divide un nodo que quedo con M claves. La que sube es la de la posicion (M-1)/2:
    //  - mediana exacta si la cantidad de claves (M) es impar.
    //  - la menor de las dos claves centrales si M es par.
    Promocion dividir(Nodo n) {
        int medio = (M - 1) / 2;
        int claveMedia = n.claves.get(medio);

        Nodo nuevo = new Nodo(n.esHoja);
        nuevo.claves.addAll(n.claves.subList(medio + 1, n.claves.size()));
        if (!n.esHoja) {
            nuevo.hijos.addAll(n.hijos.subList(medio + 1, n.hijos.size()));
        }

        List<Integer> izquierda = new ArrayList<>(n.claves.subList(0, medio));
        n.claves.clear();
        n.claves.addAll(izquierda);
        if (!n.esHoja) {
            List<Nodo> hijosIzq = new ArrayList<>(n.hijos.subList(0, medio + 1));
            n.hijos.clear();
            n.hijos.addAll(hijosIzq);
        }

        return new Promocion(claveMedia, nuevo);
    }

    // ---------------------------- BUSQUEDA ----------------------------

    Integer buscarClave(int clave) {
        Nodo n = raiz;
        while (n != null) {
            int idx = Collections.binarySearch(n.claves, clave);
            if (idx >= 0) return n.claves.get(idx);
            if (n.esHoja) return null;
            n = n.hijos.get(-(idx + 1));
        }
        return null;
    }

    ResultadoBusqueda buscar(int clave) {
        return buscar(raiz, clave, 0);
    }

    ResultadoBusqueda buscar(Nodo n, int clave, int nivel) {
        if (n == null) return null;
        int idx = Collections.binarySearch(n.claves, clave);
        if (idx >= 0) {
            ResultadoBusqueda r = new ResultadoBusqueda();
            r.clave = n.claves.get(idx);
            r.nivel = nivel;
            if (n == raiz && n.esHoja) {
                r.tipo = "raiz (que tambien es hoja)";
            } else if (n == raiz) {
                r.tipo = "raiz";
            } else if (n.esHoja) {
                r.tipo = "hoja";
            } else {
                r.tipo = "nodo interno";
            }
            return r;
        }
        if (n.esHoja) return null;
        return buscar(n.hijos.get(-(idx + 1)), clave, nivel + 1);
    }

    // ---------------------------- ELIMINACION ----------------------------
    // Politica del SUCESOR: al borrar una clave interna se reemplaza con la clave mas
    // pequena del subarbol derecho (el mas a la izquierda de ese subarbol).
    // Si tras eliminar un nodo queda por debajo del minimo, primero intenta pedir
    // prestado a un hermano; si ninguno puede prestar, se fusionan.

    void eliminar(int clave) {
        boolean existia = buscarClave(clave) != null;
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
        int idx = Collections.binarySearch(n.claves, clave);
        if (idx >= 0) {
            if (n.esHoja) {
                n.claves.remove(idx);
            } else {
                Nodo hijoDerecho = n.hijos.get(idx + 1);
                int sucesor = minimo(hijoDerecho);
                n.claves.set(idx, sucesor);
                eliminarRec(hijoDerecho, sucesor);
                rellenar(n, idx + 1);
            }
            return true;
        } else {
            if (n.esHoja) return false;
            int hijoIdx = -(idx + 1);
            boolean seElimino = eliminarRec(n.hijos.get(hijoIdx), clave);
            if (seElimino) rellenar(n, hijoIdx);
            return seElimino;
        }
    }

    // Clave mas pequena de un subarbol: bajar siempre por el hijo mas a la izquierda
    int minimo(Nodo n) {
        while (!n.esHoja) n = n.hijos.get(0);
        return n.claves.get(0);
    }

    // Revisa que n.hijos[idx] no haya quedado por debajo del minimo; si es asi,
    // primero intenta prestamo de un hermano y, si ninguno puede prestar, fusiona.
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

    // Rota: el separador baja al hijo, la ultima clave del hermano izquierdo sube al separador
    void prestarDeIzquierda(Nodo n, int idx) {
        Nodo hijo = n.hijos.get(idx);
        Nodo izq = n.hijos.get(idx - 1);
        hijo.claves.add(0, n.claves.get(idx - 1));
        if (!hijo.esHoja) {
            hijo.hijos.add(0, izq.hijos.remove(izq.hijos.size() - 1));
        }
        n.claves.set(idx - 1, izq.claves.remove(izq.claves.size() - 1));
    }

    // Rota: el separador baja al hijo, la primera clave del hermano derecho sube al separador
    void prestarDeDerecha(Nodo n, int idx) {
        Nodo hijo = n.hijos.get(idx);
        Nodo der = n.hijos.get(idx + 1);
        hijo.claves.add(n.claves.get(idx));
        if (!hijo.esHoja) {
            hijo.hijos.add(der.hijos.remove(0));
        }
        n.claves.set(idx, der.claves.remove(0));
    }

    // Fusiona hijos[idx] y hijos[idx+1] bajando el separador n.claves[idx] entre ambos
    void fusionar(Nodo n, int idx) {
        Nodo izq = n.hijos.get(idx);
        Nodo der = n.hijos.get(idx + 1);
        izq.claves.add(n.claves.remove(idx));
        izq.claves.addAll(der.claves);
        if (!izq.esHoja) {
            izq.hijos.addAll(der.hijos);
        }
        n.hijos.remove(idx + 1);
    }

    // ---------------------------- VISUALIZACION ----------------------------

    void mostrar() {
        System.out.print("Arbol B (in-order): ");
        mostrar(raiz);
        System.out.println();
    }

    void mostrar(Nodo n) {
        if (n == null) return;
        for (int i = 0; i < n.claves.size(); i++) {
            if (!n.esHoja) mostrar(n.hijos.get(i));
            System.out.print(n.claves.get(i) + " ");
        }
        if (!n.esHoja) mostrar(n.hijos.get(n.claves.size()));
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

        ArbolB arbol = new ArbolB(m);
        System.out.println("Cada nodo (menos la raiz) tendra entre " + arbol.minClaves() + " y " + (m - 1)
                + " claves, y entre " + (int) Math.ceil(m / 2.0)  + " y " + m + " hijos.");

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
