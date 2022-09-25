import java.net.*;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import java.io.*;

class FileSizeLimitException extends Exception {
    public FileSizeLimitException() {
        super();
    }
}

public final class UDPHandler {
    private UDPHandler() {
    }

    public static void send(String receiverIpAddress, int receiverPortNumber, File f, JProgressBar progressBar) {
        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket();
            InetAddress aHost = InetAddress.getByName(receiverIpAddress);
            String fileName = f.getName();
            byte[] fileNameBytes = new byte[1024];
            int packages = (int) (f.length() / 59997);

            if (packages >= 65536)
                throw new FileSizeLimitException();

            fileNameBytes[0] = (byte) ((int) ((f.length() % 59997) >> 8));
            fileNameBytes[1] = (byte) ((int) (f.length() % 59997));
            fileNameBytes[2] = (byte) ((int) (packages >> 8));
            fileNameBytes[3] = (byte) ((int) packages);
            System.arraycopy(fileName.getBytes(), 0, fileNameBytes, 4, fileName.getBytes().length);
            DatagramPacket request = new DatagramPacket(fileNameBytes, fileName.getBytes().length + 4, aHost,
                    receiverPortNumber);

            aSocket.send(request);

            byte[] fileByteArray = readFileToByteArray(f);

            sendFile(aSocket, fileByteArray, aHost, receiverPortNumber, progressBar);

            JOptionPane.showMessageDialog(null, "Archive sent.", "Done", JOptionPane.INFORMATION_MESSAGE);

        } catch (SocketException e) {
            JOptionPane.showMessageDialog(null, "Cannot create connection.", "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SocketTimeoutException e) {
            JOptionPane.showMessageDialog(null, "Time was exceeded!",
                    "Timeout Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Input/Output is not correct.", "Input/Output Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (FileSizeLimitException e) {
            JOptionPane.showMessageDialog(null, "File size limit exceeded.", "Limit Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
    }

    public static void receive(int receiverPortNumber, JProgressBar progressBar) {
        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket(receiverPortNumber);
            byte[] requestFileName = new byte[1024];
            // create socket at agreed port
            DatagramPacket request = new DatagramPacket(requestFileName, requestFileName.length);

            // Set timeout receive in ms
            aSocket.setSoTimeout(15000);

            aSocket.receive(request);
            String fileName = new String(request.getData(), 4, request.getLength() - 4);
            File f = new File(fileName);
            FileOutputStream outToFile = new FileOutputStream(f);

            int lastPackLengthBytes = ((request.getData()[0] & 0xFF) << 8) + (request.getData()[1] & 0xFF);
            int packages = (((request.getData()[2] & 0xFF) << 8) + (request.getData()[3] & 0xFF) + 1);

            receiveFile(outToFile, aSocket, lastPackLengthBytes, packages, progressBar);

            JOptionPane.showMessageDialog(null, "Archive received.", "Done", JOptionPane.INFORMATION_MESSAGE);

        } catch (SocketTimeoutException e) {
            JOptionPane.showMessageDialog(null, "Time was exceeded!",
                    "Timeout Error", JOptionPane.ERROR_MESSAGE);
        } catch (SocketException e) {
            JOptionPane.showMessageDialog(null, "Cannot create connection.", "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Input/Output is not correct.", "Input/Output Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
    }

    private static void sendFile(DatagramSocket aSocket, byte[] fileByteArray, InetAddress aHost,
            int receiverPortNumber, JProgressBar progressBar) throws IOException {
        int order = 0;
        boolean eofFlag;
        int checkSend = 0;
        boolean checkRec;
        byte[] message = new byte[60000];
        int packages = ((int) (fileByteArray.length / 59997) + 1);
        progressBar.setMinimum(0);
        progressBar.setMaximum(packages);

        for (int i = 0; i < fileByteArray.length; i += 59997) {
            order++;
            message[0] = (byte) (order >> 8);
            message[1] = (byte) (order);

            if ((i + 59997) >= fileByteArray.length) {
                eofFlag = true;
                message[2] = (byte) (1);
            } else {
                eofFlag = false;
                message[2] = (byte) (0);
            }

            if (!eofFlag) {
                System.arraycopy(fileByteArray, i, message, 3, 59997);
            } else {
                System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length % 59997);
            }

            DatagramPacket sendPacket = new DatagramPacket(message, message.length, aHost, receiverPortNumber);

            attemptToSendPacket(aSocket, sendPacket);

            int timeOutCount = 0;
            while (true) {
                byte[] check = new byte[2];
                DatagramPacket checkPack = new DatagramPacket(check, check.length);

                try {
                    aSocket.setSoTimeout(50);
                    aSocket.receive(checkPack);
                    checkSend = ((check[0] & 0xFF) << 8) + (check[1] & 0xFF);
                    checkRec = true;
                } catch (IOException e) {
                    timeOutCount++;
                    checkRec = false;
                }

                if (timeOutCount > 10)
                    throw new SocketTimeoutException();

                if ((checkSend == order) && (checkRec)) {
                    break;
                } else {
                    attemptToSendPacket(aSocket, sendPacket);
                }
            }

            progressBar.setValue(progressBar.getValue() + 1);
        }
    }

    private static void receiveFile(FileOutputStream outToFile, DatagramSocket aSocket, int lastPackLengthBytes,
            int packages, JProgressBar progressBar)
            throws IOException {
        boolean eofFlag;
        int order = 0;
        int foundLast = 0;
        byte[] message = new byte[60000];
        byte[] fileByteArray = new byte[59997];
        DatagramPacket receivedPack = new DatagramPacket(message, message.length);
        progressBar.setMinimum(0);
        progressBar.setMaximum(packages);

        while (true) {
            aSocket.receive(receivedPack);
            message = receivedPack.getData();
            InetAddress address = receivedPack.getAddress();
            int port = receivedPack.getPort();

            order = ((message[0] & 0xFF) << 8) + (message[1] & 0xFF);

            eofFlag = (message[2] & 0xFF) == 1;

            if (order == foundLast + 1) {
                foundLast = order;
                System.arraycopy(message, 3, fileByteArray, 0, 59997);
                if (eofFlag == true) {
                    outToFile.write(fileByteArray, 0, lastPackLengthBytes);
                    progressBar.setValue(progressBar.getValue() + 1);
                } else {
                    outToFile.write(fileByteArray, 0, fileByteArray.length);
                    progressBar.setValue(progressBar.getValue() + 1);
                }
                sendCheckPacket(foundLast, aSocket, address, port);
            } else {
                sendCheckPacket(foundLast, aSocket, address, port);
            }

            if (eofFlag) {
                outToFile.close();
                break;
            }
        }
    }

    private static byte[] readFileToByteArray(File f) {
        FileInputStream fis = null;
        byte[] byteArrayFile = new byte[(int) f.length()];

        try {
            fis = new FileInputStream(f);
            fis.read(byteArrayFile);
            fis.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
        return byteArrayFile;
    }

    private static void sendCheckPacket(int foundLast, DatagramSocket aSocket, InetAddress ipAddress, int portNumber)
            throws IOException {
        byte[] bCheckPacket = new byte[2];
        bCheckPacket[0] = (byte) (foundLast >> 8);
        bCheckPacket[1] = (byte) (foundLast);

        DatagramPacket checkPacket = new DatagramPacket(bCheckPacket, bCheckPacket.length, ipAddress, portNumber);
        attemptToSendPacket(aSocket, checkPacket);
    }

    private static void attemptToSendPacket(DatagramSocket aSocket, DatagramPacket packet)
            throws SocketTimeoutException {
        int attempt;
        for (attempt = 1; attempt <= 5; attempt++) {
            try {
                aSocket.send(packet);
                break;
            } catch (IOException e) {
            }
        }

        if (attempt > 5)
            throw new SocketTimeoutException();

    }
}