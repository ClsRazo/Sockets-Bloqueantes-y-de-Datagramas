package com.mycompany.practica2;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Cliente {
    private static final String IP = "127.0.0.1";
    private static final int PUERTO = 12345;

    public static void main(String[] args) {
        try {
            Scanner lectura = new Scanner(System.in);
            System.out.println("Ingrese el tamaño del paquete: ");
            int tamPaquete = lectura.nextInt();
            System.out.println("Ingrese el tamaño de la ventana: ");
            int tamVentana = lectura.nextInt();

            DatagramSocket cS = new DatagramSocket();
            InetAddress sA = InetAddress.getByName(IP);

            //Se envia el tamaño del paquete y ventana al servidor
            enviarTams(cS, sA, PUERTO, tamPaquete, tamVentana);

            //Se espera la confirmación del servidor de recibido
            byte[] datoRecibido = new byte[1];
            DatagramPacket packRecibido = new DatagramPacket(datoRecibido, datoRecibido.length);
            cS.receive(packRecibido);

            if(datoRecibido[0] == 1){
                System.out.println("Conexión establecida con el servidor...");
                System.out.println("Confirmación recibida.");
            } else {
                System.out.println("Error en la confirmación.");
                cS.close();
                lectura.close();
                return;
            }

            //Se envia el archivo al servidor
            enviarArchivo(cS, sA, PUERTO, tamPaquete, tamVentana);

            cS.close();
            lectura.close();

            System.out.println("Archivo enviado al servidor.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void enviarTams(DatagramSocket cS, InetAddress aS, int puertoSer, int tamPaquete, int tamVentana) throws IOException {
        //Se envia el tamaño del paquete y ventana al servidor como cadena, pero en bytes
        byte[] tamVentanaD = (tamPaquete + "," + tamVentana).getBytes();
        DatagramPacket tamVentanaP = new DatagramPacket(tamVentanaD, tamVentanaD.length, aS, puertoSer);
        cS.send(tamVentanaP);
    }

    private static void enviarArchivo(DatagramSocket cS, InetAddress aS, int puertoSer, int tamPaquete, int tamVentana) throws IOException {

        File archivo = new File("src/main/java/com/mycompany/practica2/Cliente.java");
        // File archivo = new File("C:/Users/Asus/Desktop/ANEXO-INT1-2024-2.pdf");
        
        //Se envia el tamaño del archivo al servidor
        String tamArchivoStr = String.valueOf(archivo.length());
        byte[] tamArchivoBytes = tamArchivoStr.getBytes();
        DatagramPacket packEnviar = new DatagramPacket(tamArchivoBytes, tamArchivoBytes.length, aS, puertoSer);
        cS.send(packEnviar);
        
        // Se envía el nombre y extensión
        String nombreArch = archivo.getName();
        byte[] nombreArchBytes = nombreArch.getBytes();
        DatagramPacket packNombre = new DatagramPacket(nombreArchBytes, nombreArchBytes.length, aS, puertoSer);
        cS.send(packNombre);
        

        //Se envia el archivo al servidor
        byte[] buffer = new byte[tamPaquete];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivo));

        //Variables para contar bytes leidos y el numero de paquete actual
        int bytesLectura = 0;
        int paqueteActual = 0;

        //Ciclo para enviar todo el archivo al servidor
        while(true){
            //Se reinicia el conteo del paquete actual
            paqueteActual = 0;
            int acksRecibidos = 0; // Último acknowledge consecutivo recibido
            
            while(acksRecibidos != tamVentana) {
                //Ciclo para enviar la ventana de paquetes
                for(int i = acksRecibidos; i < tamVentana; i++) {
                    //Se lee el paquete "n"
                    bytesLectura = bis.read(buffer);
                    //Para confirmar
                    System.out.println("Bytes leidos: " + bytesLectura);

                    //Si se llegó al final del archivo, se sale del ciclo
                    if(bytesLectura == -1) break;

                    //Si no, se aunmenta el contador de paquete actual
                    paqueteActual++;

                    //Se envia el paquete al servidor
                    packEnviar = new DatagramPacket(buffer, bytesLectura, aS, puertoSer);
                    cS.send(packEnviar);

                    //Se envia el numero de paquete al servidor (Dentro de la ventana)
                    String numPaquete = String.valueOf(paqueteActual);
                    byte[] bytesNumPacket = numPaquete.getBytes();
                    packEnviar = new DatagramPacket(bytesNumPacket, bytesNumPacket.length, aS, puertoSer);
                    cS.send(packEnviar);

                    System.out.println("Enviando paquete: " + numPaquete + " con un tamaño de: " + bytesLectura + " bytes");

                    byte[] tamAckBytes = new byte[1024];
                    DatagramPacket tamAckPack = new DatagramPacket(tamAckBytes, tamAckBytes.length);
                    cS.receive(tamAckPack);
                    String ackStr = new String(tamAckPack.getData(), 0, tamAckPack.getLength());
                    int ack = Integer.parseInt(ackStr);

                    System.out.println("Acknowledge de: " + ack + " recibido");
                    
                    // System.out.println("XDXDXD1");

                    if(ack == paqueteActual) acksRecibidos++;

                    // System.out.println("XDXDXD2");
                }
                
//                for(int i = 0; i < tamVentana; i++) {
//                    byte[] tamAckBytes = new byte[1024];
//                    DatagramPacket tamAckPack = new DatagramPacket(tamAckBytes, tamAckBytes.length);
//                    cS.receive(tamAckPack);
//                    String ackStr = new String(tamAckPack.getData(), 0, tamAckPack.getLength());
//                    int ack = Integer.parseInt(ackStr);
//                    
//                    if(ack == acksRecibidos + 1) acksRecibidos++;
//                    else break;                    
//                }

                if(bytesLectura == -1) break;
            }

            //Verificamos si se llegó al final del archivo
            if(bytesLectura == -1) break;

        }

        bis.close();
    }
}

