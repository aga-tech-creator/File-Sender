import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

public final class TCPHandler {
        private TCPHandler() {
        }

        public static void send(String ipAddress, int portNumber, File f, JProgressBar progressBar) {
                Socket clientSocket = null;
                try {
                        int bytes = 0;
                        int packages = (int) (f.length() / 60000) + 1;
                        progressBar.setMinimum(0);
                        progressBar.setMaximum(packages);
                        clientSocket = new Socket(ipAddress, portNumber);

                        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

                        out.writeInt(packages);
                        out.writeUTF(f.getName());

                        FileInputStream fileInputStream = new FileInputStream(f);

                        out.writeLong(f.length());
                        byte[] buffer = new byte[60000];
                        while ((bytes = fileInputStream.read(buffer)) != -1) {
                                out.write(buffer, 0, bytes);
                                out.flush();
                                progressBar.setValue(progressBar.getValue() + 1);
                        }

                        fileInputStream.close();

                        JOptionPane.showMessageDialog(null, "Archive sent.", "Done", JOptionPane.INFORMATION_MESSAGE);

                } catch (UnknownHostException e) {
                        JOptionPane.showMessageDialog(null, "Unknown host.", "Host Error",
                                        JOptionPane.ERROR_MESSAGE);
                } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Cannot create connection.", "Connection Error",
                                        JOptionPane.ERROR_MESSAGE);
                } finally {
                        if (clientSocket != null) {
                                try {
                                        clientSocket.close();
                                } catch (IOException e) {
                                        JOptionPane.showMessageDialog(null, "Cannot close connection.",
                                                        "Connection Error",
                                                        JOptionPane.ERROR_MESSAGE);
                                }
                        }
                }
        }

        public static void receive(int portNumber, JProgressBar progressBar) {
                ServerSocket listenSocket = null;
                try {
                        int bytes = 0;
                        String filename = "";
                        int packages = 0;
                        progressBar.setMinimum(0);
                        listenSocket = new ServerSocket(portNumber);
                        listenSocket.setSoTimeout(15000);
                        Socket clientSocket = listenSocket.accept();
                        DataInputStream in = new DataInputStream(clientSocket.getInputStream());

                        packages = in.readInt();
                        filename = in.readUTF();
                        progressBar.setMaximum(packages);

                        FileOutputStream outToFile = new FileOutputStream(filename);

                        // Soma os bytes dos pacotes para indicar se foi enviado um de 60000 bytes.
                        int sumBytesToPack = 0;
                        long size = in.readLong();
                        byte[] buffer = new byte[60000];
                        while (size > 0 && (bytes = in.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                                outToFile.write(buffer, 0, bytes);
                                size -= bytes;
                                sumBytesToPack += bytes;
                                if (sumBytesToPack >= 60000) {
                                        sumBytesToPack = sumBytesToPack - 60000;
                                        progressBar.setValue(progressBar.getValue() + 1);
                                }
                        }
                        outToFile.close();
                        progressBar.setValue(packages);

                        JOptionPane.showMessageDialog(null, "Archive received.", "Done",
                                        JOptionPane.INFORMATION_MESSAGE);

                } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Cannot create connection.", "Connection Error",
                                        JOptionPane.ERROR_MESSAGE);
                } finally {
                        try {
                                listenSocket.close();
                        } catch (IOException e) {
                                JOptionPane.showMessageDialog(null, "Cannot close connection.", "Connection Error",
                                                JOptionPane.ERROR_MESSAGE);
                        }
                }
        }
}
