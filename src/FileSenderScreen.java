import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class FileSenderScreen extends JFrame {
    int width, height;
    String title;
    JFrame mainScreen;
    JButton testConnectioButton;
    JRadioButton tcp, udp;
    ButtonGroup radioGroup;
    JLabel protocolText, ipText, ipPortText;
    JTextField ipField, fileField, ipPortField;
    JFileChooser fChooser;
    JProgressBar progressBar;

    public FileSenderScreen(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void start() {
        this.mainScreen = new JFrame();
        this.mainScreen.setLayout(null);
        this.mainScreen.setTitle(this.title);
        this.mainScreen.setSize(this.width, this.height);
        this.mainScreen.setLocation(500, 300);
        this.mainScreen.setResizable(false);
        this.mainScreen.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.layoutProtocolConfig();
        this.layoutIpConfig();
        this.layoutAppConfig();
        this.progressBarConfig();
        this.mainScreen.setVisible(true);
    }

    private void layoutProtocolConfig() {
        protocolText = new JLabel("Choose Connection");
        tcp = new JRadioButton("TCP", true);
        udp = new JRadioButton("UDP", false);
        radioGroup = new ButtonGroup();
        radioGroup.add(tcp);
        radioGroup.add(udp);

        this.mainScreen.add(protocolText);
        this.mainScreen.add(tcp);
        this.mainScreen.add(udp);

        protocolText.setBounds(175, 60, 140, 14);
        tcp.setBounds(175, 80, 50, 14);
        udp.setBounds(175, 100, 50, 14);
    }

    private void layoutIpConfig() {
        ipText = new JLabel("IP address");
        ipPortText = new JLabel("Port number");
        ipPortField = new JTextField();
        ipField = new JTextField();

        this.mainScreen.add(ipPortField);
        this.mainScreen.add(ipPortText);
        this.mainScreen.add(ipText);
        this.mainScreen.add(ipField);

        ipText.setBounds(425, 60, 100, 14);
        ipField.setBounds(425, 90, 130, 16);
        ipPortText.setBounds(575, 60, 100, 14);
        ipPortField.setBounds(575, 90, 50, 16);
    }

    private void progressBarConfig() {
        progressBar = new JProgressBar();
        progressBar.setForeground(Color.GREEN);
        progressBar.setStringPainted(true);

        this.mainScreen.add(progressBar);

        progressBar.setBounds(50, 505, 700, 30);
    }

    private void layoutAppConfig() {
        fChooser = new JFileChooser();

        // Change the name "Cancel" to "Delete".
        UIManager.put("FileChooser.cancelButtonText", "Receive");
        SwingUtilities.updateComponentTreeUI(fChooser);

        // Change the name "Send" to "Delete".
        fChooser.setApproveButtonText("Send");
        fChooser.setBorder(BorderFactory.createLineBorder(Color.darkGray));

        this.mainScreen.add(fChooser);

        fChooser.setBounds(50, 135, 700, 350);

        fChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File f;

                if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {

                    f = fChooser.getSelectedFile();

                    if (!ipField.getText().equals("") && !ipPortField.getText().equals("")) {
                        if (tcp.isSelected()) {
                            new Thread() {
                                @Override
                                public void run() {
                                    TCPHandler.send(ipField.getText(), Integer.parseInt(ipPortField.getText()), f,
                                            progressBar);
                                    progressBar.setValue(0);
                                }
                            }.start();

                        } else if (udp.isSelected()) {
                            new Thread() {
                                @Override
                                public void run() {
                                    UDPHandler.send(ipField.getText(), Integer.parseInt(ipPortField.getText()), f,
                                            progressBar);
                                    progressBar.setValue(0);
                                }
                            }.start();
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Blank IP/Port field.", "Input/Output Error",
                                JOptionPane.ERROR_MESSAGE);
                    }

                } else if (e.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
                    if (tcp.isSelected()) {
                        JFrame portToListenScreen = new JFrame();
                        String portToListen = JOptionPane.showInputDialog(portToListenScreen, "Port to listen");
                        if (!(portToListen == null || (portToListen != null && ("".equals(portToListen))))) {
                            new Thread() {
                                @Override
                                public void run() {
                                    TCPHandler.receive(Integer.parseInt(portToListen), progressBar);
                                    progressBar.setValue(0);
                                }
                            }.start();
                        }
                    } else if (udp.isSelected()) {
                        JFrame portToListenScreen = new JFrame();
                        String portToListen = JOptionPane.showInputDialog(portToListenScreen, "Port to listen");
                        if (!(portToListen == null || (portToListen != null && ("".equals(portToListen))))) {
                            new Thread() {
                                @Override
                                public void run() {
                                    UDPHandler.receive(Integer.parseInt(portToListen), progressBar);
                                    progressBar.setValue(0);
                                }
                            }.start();
                        }
                    }
                }
            }
        });
    }
}