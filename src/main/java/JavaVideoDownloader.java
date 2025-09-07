import com.formdev.flatlaf.FlatLightLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class YouTubeDownloader {
    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new FlatLightLaf());
        JFrame frame = new JFrame("Social Media Video Downloader");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(550, 300);
        frame.setLocation(
                (screenSize.width - frame.getWidth()) / 2,
                (screenSize.height - frame.getHeight()) / 2
        );
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        try {
            Image icon = ImageIO.read(Objects.requireNonNull(YouTubeDownloader.class.getResource("/project_icon.png")));
            frame.setIconImage(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final File[] downloadFolder = {new File(System.getProperty("user.home"), "Downloads/JavaVideoDownloader")};
        if (!downloadFolder[0].exists()) {
            downloadFolder[0].mkdirs();
        }

        // TOP
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        String textarea_placeholder = "Paste or Enter links to social media here...";
        textArea.setForeground(Color.GRAY);
        textArea.setText(textarea_placeholder);
        textArea.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (textArea.getText().equals(textarea_placeholder)) {
                    textArea.setText("");
                    textArea.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (textArea.getText().isEmpty()) {
                    textArea.setForeground(Color.GRAY);
                    textArea.setText(textarea_placeholder);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 180));
        frame.add(scrollPane, BorderLayout.NORTH);

        // MIDDLE
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(500, 20)); // тонкий
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.add(progressBar);
        frame.add(progressPanel, BorderLayout.CENTER);

        // BOTTOM
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String[] formats = {
                "(dual) Video+Audio/YT music",
                "TikTok, Instagram",
                //"X.com (twitter)", //UNDER BUG FIXING
                "Video",
                "Audio/YT music"
        };

        JComboBox<String> formatBox = new JComboBox<>(formats);
        formatBox.setMaximumSize(new Dimension(200, 25));

        JButton downloadButton = new JButton("Download");
        downloadButton.setPreferredSize(new Dimension(320, 30));

        ImageIcon thumbIcon = new ImageIcon(
                Objects.requireNonNull(YouTubeDownloader.class.getResource("/thumbnail_icon.png"))
        );
        Image scaled = thumbIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JButton thumbnailButton = new JButton(new ImageIcon(scaled));
        thumbnailButton.setToolTipText("Download thumbnail");

        bottomPanel.add(formatBox);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(downloadButton);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(thumbnailButton);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        downloadButton.addActionListener(_ -> {
            String input = textArea.getText().trim();
            if (input.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter at least one video URL!");
                return;
            }

            String[] urls = input.split("\\r?\\n");
            List<String> videoUrls = new ArrayList<>();
            for (String url : urls) {
                if (!url.trim().isEmpty()) {
                    videoUrls.add(url.trim());
                }
            }

            if (videoUrls.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No valid URLs found!");
                return;
            }

            downloadButton.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setString("Starting download...");

            new Thread(() -> {
                try {
                    File tempDir = new File(System.getProperty("java.io.tmpdir"), "ytDownloader");
                    tempDir.mkdirs();

                    File ytDlpExe = extractResource("/yt-dlp.exe", tempDir);
                    File ffmpegDir = new File(tempDir, "ffmpeg/bin");
                    ffmpegDir.mkdirs();
                    File ffmpegExe = extractResource("/ffmpeg/bin/ffmpeg.exe", tempDir);

                    String selectedFormat = (String) formatBox.getSelectedItem();

                    for (int i = 0; i < videoUrls.size(); i++) {
                        String videoUrl = videoUrls.get(i);

                        List<String> command = new ArrayList<>();
                        command.add(ytDlpExe.getAbsolutePath());

                        switch (selectedFormat) {
                            case "(dual) Video+Audio/YT music":
                                command.add("-f");
                                command.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]");
                                command.add("--merge-output-format");
                                command.add("mp4");
                                command.add("--ffmpeg-location");
                                command.add(ffmpegExe.getAbsolutePath());
                                break;
                            case "Video":
                                command.add("-f");
                                command.add("bestvideo");
                                break;
                            case "Audio/YT music":
                                command.add("-f");
                                command.add("bestaudio");
                                command.add("--extract-audio");
                                command.add("--audio-format");
                                command.add("mp3");
                                break;
                            case "TikTok, Instagram":
                                command.add("-f");
                                command.add("best[ext=mp4]");
                                break;
//                            case "X.com (twitter)":
//                                command.add("-f");
//                                command.add("best");
//                                command.add("--merge-output-format");
//                                command.add("mp4");
//                                command.add("--remux-video");
//                                command.add("mp4");
//                                command.add("--ffmpeg-location");
//                                command.add(ffmpegExe.getAbsolutePath());
//                                break;
                            case null:
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + selectedFormat);
                        }

                        command.add("-o");
                        command.add(downloadFolder[0].getAbsolutePath() + "/%(title)s.%(ext)s");
                        command.add(videoUrl);

                        ProcessBuilder pb = new ProcessBuilder(command);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d)%");

                        int videoIndex = i + 1;
                        while ((line = reader.readLine()) != null) {
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                int progress = (int) Float.parseFloat(matcher.group(1));
                                int totalProgress = (int) (((i + progress / 100.0) / videoUrls.size()) * 100);

                                SwingUtilities.invokeLater(() -> {
                                    progressBar.setValue(totalProgress);
                                    progressBar.setString("Video " + videoIndex + " of " + videoUrls.size() + " - " + progress + "%");
                                });
                            } else {
                                String finalLine = line;
                                SwingUtilities.invokeLater(() -> progressBar.setString("Video " + videoIndex + " of " + videoUrls.size() + " - " + finalLine.trim()));
                            }
                        }

                        process.waitFor();
                    }

                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(100);
                        progressBar.setString("All downloads completed!");
                        downloadButton.setEnabled(true);
                    });

                } catch (IOException | InterruptedException ex) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Error: " + ex.getMessage());
                        downloadButton.setEnabled(true);
                    });
                }
            }).start();
        });

        thumbnailButton.addActionListener(_ -> {
            String[] urls = textArea.getText().split("\\r?\\n");
            List<String> links = new ArrayList<>();
            for (String u : urls) {
                if (!u.trim().isEmpty()) {
                    links.add(u.trim());
                }
            }

            if (links.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter at least one video URL!");
                return;
            }

            thumbnailButton.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setString("Downloading thumbnails...");

            File folderToUse = downloadFolder[0];

            new Thread(() -> {
                try {
                    File tempDir = new File(System.getProperty("java.io.tmpdir"), "ytDownloader");
                    tempDir.mkdirs();

                    File ytDlpExe = extractResource("/yt-dlp.exe", tempDir);

                    int done = 0;
                    for (String videoUrl : links) {
                        List<String> command = new ArrayList<>();
                        command.add(ytDlpExe.getAbsolutePath());
                        command.add("--write-thumbnail");
                        command.add("--convert-thumbnails");
                        command.add("png");
                        command.add("-o");
                        command.add(folderToUse.getAbsolutePath() + "/%(title)s.%(ext)s");
                        command.add(videoUrl);

                        ProcessBuilder pb = new ProcessBuilder(command);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();
                        process.waitFor();

                        done++;
                        int progress = (int) (((double) done / links.size()) * 100);
                        int finalDone = done;
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(progress);
                            progressBar.setString("Downloaded " + finalDone + "/" + links.size() + " thumbnails");
                        });
                    }

                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(100);
                        progressBar.setString("All thumbnails downloaded!");
                        thumbnailButton.setEnabled(true);
                    });

                } catch (IOException | InterruptedException ex) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Error: " + ex.getMessage());
                        thumbnailButton.setEnabled(true);
                    });
                }
            }).start();
        });
    }

    private static File extractResource(String resourcePath, File outputDir) throws IOException {
        InputStream in = YouTubeDownloader.class.getResourceAsStream(resourcePath);
        if (in == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
        File outFile = new File(outputDir, new File(resourcePath).getName());
        Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        in.close();
        outFile.deleteOnExit();
        return outFile;
    }
}