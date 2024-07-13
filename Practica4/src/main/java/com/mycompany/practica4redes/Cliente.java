package com.mycompany.practica4redes;

import java.io.IOException;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        //Pedir al usuario la URL, la cantidad de hilos y el nivel de anidamiento
        System.out.print("Ingresa la URL: ");
        String url = scanner.nextLine();

        System.out.print("Ingresa la cantidad de hilos: ");
        int numHilos = scanner.nextInt();

        System.out.print("Ingresa el nivel de anidamiento: ");
        int nivelAnidamiento = scanner.nextInt();

        //Directorio donde se guardará la descarga
        String directorio = "CarpetaDescargada";

        //Iniciar la descarga desde la clase WGet
        WGet wget = new WGet();
        try {
            wget.descargar(url, directorio, numHilos, nivelAnidamiento);
            System.out.println("\n\n\t-----------------DESCARGA COMPLETADA-----------------\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        scanner.close();
    }
}
