package org.tagUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.io.File;

public class Window extends JFrame {
    private JPanel jPanel;
    private JTextField directoryField;
    private JButton cleanNewMusicButton;
    private JButton cleanOldMusicButton;
    private JButton selectDirectoryButton;
    private File folder;

    private static final Logger logger = LogManager.getLogger(App.class);

    public Window(String title) {
        super(title);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(jPanel);
        var jFileChooser = new JFileChooser(System.getProperty("user.home"));
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        this.getContentPane().add(selectDirectoryButton);
        this.pack();

        selectDirectoryButton.addActionListener(e -> {
            var status = jFileChooser.showSaveDialog(null);
            if (status == JFileChooser.APPROVE_OPTION) {
                folder = jFileChooser.getSelectedFile();
                directoryField.setText(folder.getAbsolutePath());
                logger.info("You chose to open this directory: " + folder.getAbsolutePath());
            }
        });

        cleanNewMusicButton.addActionListener(e -> {
            NewMusic.loopDirectory(folder);
            finishProcess();
        });

        cleanOldMusicButton.addActionListener(e -> {
            OldMusic.loopDirectory(folder);
            finishProcess();
        });
    }

    private void finishProcess() {
        folder = null;
        directoryField.setText(null);
        //todo text
    }
}
