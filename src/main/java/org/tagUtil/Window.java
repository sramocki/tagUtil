package org.tagUtil;

import org.apache.commons.collections4.Trie;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tagUtil.type.NewMusic;
import org.tagUtil.type.OldMusic;
import org.tagUtil.util.Lookup;

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

    private static Trie<String, String> composerTrie;

    private static final Logger logger = LogManager.getLogger(App.class);

    public Window(String title) {
        super(title);

        composerTrie = Lookup.setupTrie();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(jPanel);
        var jFileChooser = new JFileChooser(System.getProperty("user.home"));
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        this.getContentPane().add(selectDirectoryButton);
        this.pack();

        selectDirectoryButton.addActionListener(e -> {
            var status = jFileChooser.showSaveDialog(null);
            if (status == JFileChooser.APPROVE_OPTION) {
                var folder = jFileChooser.getSelectedFile();
                directoryField.setText(folder.getAbsolutePath());
                logger.info("You chose to open this directory: " + folder.getAbsolutePath());
            }
        });

        cleanNewMusicButton.addActionListener(e -> {
            NewMusic.loopDirectory(new File(directoryField.getText()));
            finishProcess();
        });

        cleanOldMusicButton.addActionListener(e -> {
            OldMusic.loopDirectory(new File(directoryField.getText()));
            finishProcess();
        });
    }

    private void finishProcess() {
        directoryField.setText(null);
    }

    public static Trie<String, String> getComposerTrie() {
        return composerTrie;
    }
}
