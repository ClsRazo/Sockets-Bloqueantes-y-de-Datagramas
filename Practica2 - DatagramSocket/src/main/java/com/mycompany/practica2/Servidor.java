package com.mycompany.practica2;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Servidor {
    private static final String IP = "127.0.0.1";
    private static final int PUERTO = 12345;

    public static void main(String[] args) {
        try {
            DatagramSocket serverSocket = new DatagramSocket(PUERTO);

            //Se recibe el tamaño del paquete y ventana desde el cliente
            byte[] tamVentanaD = new byte[1024];
            DatagramPacket tamVentanaP = new DatagramPacket(tamVentanaD, tamVentanaD.length);
            serverSocket.receive(tamVentanaP);
            String[] tamVentanaInfo = new String(tamVentanaP.getData(), 0, tamVentanaP.getLength()).split(",");
            int tamPaquete = Integer.parseInt(tamVentanaInfo[0]);
            int tamVentana = Integer.parseInt(tamVentanaInfo[1]);
            System.out.println("Tamaño del paquete: " + tamPaquete);
            System.out.println("Tamaño de la ventana: " + tamVentana);

            //Se envia la confirmación al cliente
            serverSocket.send(new DatagramPacket(new byte[]{1}, 1, tamVentanaP.getAddress(), tamVentanaP.getPort()));

            //Recibir archivo desde el cliente
            recibirArchivo(serverSocket, tamPaquete, tamVentana);


            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void recibirArchivo(DatagramSocket serverSocket, int tamPaquete, int tamVentana) throws IOException {
        byte[] tamArchivoBytes = new byte[1024];
        DatagramPacket tamArchivoPack = new DatagramPacket(tamArchivoBytes, tamArchivoBytes.length);
        serverSocket.receive(tamArchivoPack);
        String tamArchivoStr = new String(tamArchivoPack.getData(), 0, tamArchivoPack.getLength());
        int tamArchivo = Integer.parseInt(tamArchivoStr);
        System.out.println("Tamaño del archivo: " + tamArchivo);
        
        byte[] nombreArchivoBytes = new byte[1024];
        DatagramPacket nombreArchivoPack = new DatagramPacket(nombreArchivoBytes, nombreArchivoBytes.length);
        serverSocket.receive(nombreArchivoPack);
        String nombreArchivo = new String(nombreArchivoPack.getData(), 0, nombreArchivoPack.getLength());
        int numCopia = 0;
        String extension = "";
        
        int fi = nombreArchivo.lastIndexOf('.');
        int p = Math.max(nombreArchivo.lastIndexOf('/'), nombreArchivo.lastIndexOf('\\'));

        if (fi > p) {
            extension = nombreArchivo.substring(fi+1);
        }
        
        nombreArchivo = nombreArchivo.substring(0,nombreArchivo.length() - (extension.length() + 1)); // quitamos extension
        nombreArchivo += " - Copia " + numCopia; // Agregamos apendice
        nombreArchivo += "." + extension; // regresamos extension
        System.out.println("Nombre del archivo: " + nombreArchivo);
        
        //Crear el archivo
        File archivo = new File(nombreArchivo);
        while(archivo.exists()) {
            numCopia++;
            if(numCopia > 9) {
                System.out.println("Demasiadas copias con el mismo nombre");
                return;
            }
            nombreArchivo = nombreArchivo.substring(0,nombreArchivo.length() - (extension.length() + 2)); // quitamos numero de copia
            nombreArchivo += numCopia + "." + extension;
            archivo = new File(nombreArchivo);
        }

        ArrayList<byte[]> PacksVentana = new ArrayList<>();
        int bytesRecibidos = 0, bytesLeidos = 0, i = 1;

        while(bytesLeidos < tamArchivo){
            PacksVentana = recibirVentana(tamVentana, serverSocket, tamPaquete);
            bytesRecibidos = calcularTamanioDatos(PacksVentana);
            // bytesRecibidos = tamPaquete * PacksVentana.size();
            //Checar si se puede recibir una ventana completa
            if((bytesLeidos + (tamPaquete * tamVentana)) < tamArchivo){
                //Si sí, checamos si lo que se leyo en 'recibirVentana' es del tamaño de una ventana
                System.out.println("Ventana completa");
                if(bytesRecibidos == tamPaquete * tamVentana){
                    System.out.println("Ventana correcta");
                    //Si sí, se añaden los bytes recibidos a los bytes leidos y se avanza a la siguiente ventana
                    bytesLeidos += bytesRecibidos;
                    //Escribimos los bytes en el archivo
                    escribirEnArchivo(archivo, PacksVentana);
                    i++;
                //Si no, se solicita el reenvio de la ventana ya que no llegó completa
                //Se vuelve a realizar el ciclo
                }else{
                    System.out.println("Ventana incorrecta, reenviar ventana: " + i);
                }
            //Si no se puede recibir una ventana completa, significa que es la última ventana de menor tamaño
            }else{
                System.out.println("Ultima ventana");
                int tamVentanaRestante = tamArchivo - bytesLeidos;
                //Se checa que los bytes recibidos sean del tamaño de la ventana restante
                if(bytesRecibidos == tamVentanaRestante){
                    System.out.println("Ventana correcta");
                    //Si sí, se añaden los bytes recibidos a los bytes leidos
                    bytesLeidos += bytesRecibidos;
                    //Escribimos los bytes en el archivo
                    escribirEnArchivo(archivo, PacksVentana);
                    i++;
                //Si no, se solicita el reenvio de la ventana ya que no llegó completa
                //Se vuelve a realizar el ciclo
                }else{
                    System.out.println("Ventana incorrecta, reenviar ventana: " + i);
                }
            }
        }

        System.out.println("Archivo recibido correctamente.");
    }

    private static ArrayList<byte[]> recibirVentana(int tamVentana, DatagramSocket serverSocket, int tamPaquete) throws IOException {
        byte[] buffer = new byte[tamPaquete];

        ArrayList<byte[]> PacksVentana = new ArrayList<>();
        int numPack = 0;

        DatagramPacket pack = new DatagramPacket(buffer, buffer.length);

        while(true){
            byte[] numPackBytes = new byte[1024];

            DatagramPacket numPackDP = new DatagramPacket(numPackBytes, numPackBytes.length);
            serverSocket.receive(numPackDP);
            numPack = Integer.parseInt(new String(numPackDP.getData(), 0, numPackDP.getLength()));

            pack = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(pack);
            
            // PacksVentana.add(pack.getData());

            if(new String(pack.getData(), 0, pack.getLength()).equals("FIN")){
                System.out.println("Fin de paquetes recibidos");
                break;
            }else{
                PacksVentana.add(Arrays.copyOf(pack.getData(), pack.getLength()));
                System.out.println("Paquete: " + numPack + " recibido");
            }
            
        }

        //Checamos que todos los paquetes se hayan recibido
        // for(int i = 0; i < PacksVentana.size(); i++){
        // }

        //Enviar ACKs
        enviarACKs(pack, PacksVentana, serverSocket);
        return PacksVentana;
    }

    private static void enviarACKs(DatagramPacket pack, ArrayList<byte[]> PacksVentana, DatagramSocket serverSocket) throws IOException {
        JSONObject ackJSON = new JSONObject();
        
        if(PacksVentana.isEmpty()){
            ackJSON.put("numero_ack", 0);
            ackJSON.put("contenido", "VACIO");
            String ackString = ackJSON.toString();

            byte[] ackBytes = ackString.getBytes();

            DatagramPacket ackPack = new DatagramPacket(ackBytes, ackBytes.length, pack.getAddress(), pack.getPort());
            serverSocket.send(ackPack);

            System.out.println("No hay paquetes para enviar ACKs, enviando ACK 0");

            return;
        }else{
            for(int i = 1; i <= PacksVentana.size(); i++){
                ackJSON.put("numero_ack", i);
                ackJSON.put("contenido", "ACK");

                String ackString = ackJSON.toString();

                byte[] ackBytes = ackString.getBytes();

                DatagramPacket ackPack = new DatagramPacket(ackBytes, ackBytes.length, pack.getAddress(), pack.getPort());
                serverSocket.send(ackPack);

                System.out.println("ACK " + i + " enviado.");
            }

            //Enviar fin de ACKs
            ackJSON.put("numero_ack", PacksVentana.size() + 1);
            ackJSON.put("contenido", "FIN");

            String ackString = ackJSON.toString();

            byte[] ackBytes = ackString.getBytes();

            DatagramPacket ackPack = new DatagramPacket(ackBytes, ackBytes.length, pack.getAddress(), pack.getPort());
            serverSocket.send(ackPack);
        }
    }

    public static void escribirEnArchivo(File archivo, ArrayList<byte[]> PacksVentana) {
        try (FileOutputStream fos = new FileOutputStream(archivo, true)) {
            for (byte[] datos : PacksVentana) {
                fos.write(datos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int calcularTamanioDatos(ArrayList<byte[]> PacksVentana) {
        int tamanioTotal = 0;
        for (byte[] datos : PacksVentana) {
            tamanioTotal += datos.length;
        }
        return tamanioTotal;
    }
}

