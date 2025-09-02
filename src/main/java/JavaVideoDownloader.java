import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class YouTubeDownloader {
    public static void main(String[] args) {
        JFrame frame = new JFrame("YouTube Video Downloader");
        frame.setSize(515, 220);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width - 500) / 2, (screenSize.height - 220) / 2);
        frame.setLayout(null);
        try {
            Image icon = ImageIO.read(Objects.requireNonNull(YouTubeDownloader.class.getResource("/5295-youtube-i_102568.png")));
            frame.setIconImage(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JLabel label = new JLabel("Enter video URL:");
        label.setBounds(10, 10, 200, 25);
        frame.add(label);

        JTextField textField = new JTextField();
        textField.setBounds(10, 40, 480, 25);
        frame.add(textField);

        JButton folderButton = new JButton("Select Folder");
        folderButton.setBounds(10, 75, 150, 25);
        frame.add(folderButton);

        JLabel folderLabel = new JLabel("Download folder: Default Downloads");
        folderLabel.setBounds(170, 75, 320, 25);
        frame.add(folderLabel);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setBounds(10, 110, 480, 25);
        progressBar.setStringPainted(true);
        frame.add(progressBar);

        String[] formats = {"(dual) Video+Audio", "Video", "Audio"};
        JComboBox<String> formatBox = new JComboBox<>(formats);
        formatBox.setBounds(140, 145, 150, 25);
        frame.add(formatBox);

        JButton downloadButton = new JButton("Download");
        downloadButton.setBounds(10, 145, 120, 25);
        frame.add(downloadButton);

        final File[] downloadFolder = {new File(System.getProperty("user.home"), "Downloads")};

        folderButton.addActionListener(_ -> {
            JFileChooser chooser = new JFileChooser(downloadFolder[0]);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = chooser.showOpenDialog(frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                downloadFolder[0] = chooser.getSelectedFile();
                folderLabel.setText("Download folder: " + downloadFolder[0].getAbsolutePath());
            }
        });

        downloadButton.addActionListener(_ -> {
            String videoUrl = textField.getText().trim();
            if (videoUrl.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter a video URL!");
                return;
            }

            downloadButton.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setString("Starting download...");

            File folderToUse = downloadFolder[0];

            new Thread(() -> {
                try {
                    File tempDir = new File(System.getProperty("java.io.tmpdir"), "ytDownloader");
                    tempDir.mkdirs();

                    File ytDlpExe = extractResource("/yt-dlp.exe", tempDir);
                    File ffmpegDir = new File(tempDir, "ffmpeg/bin");
                    ffmpegDir.mkdirs();
                    File ffmpegExe = extractResource("/ffmpeg/bin/ffmpeg.exe", tempDir);

                    String selectedFormat = (String) formatBox.getSelectedItem();

                    List<String> command = new ArrayList<>();
                    command.add(ytDlpExe.getAbsolutePath());

                    switch (selectedFormat) {
                        case "(dual) Video+Audio":
                            command.add("-f");
                            command.add("bestvideo+bestaudio");
                            command.add("--merge-output-format");
                            command.add("mp4");
                            command.add("--ffmpeg-location");
                            command.add(ffmpegExe.getAbsolutePath());
                            break;
                        case "Video":
                            command.add("-f");
                            command.add("bestvideo");
                            break;
                        case "Audio":
                            command.add("-f");
                            command.add("bestaudio");
                            command.add("--extract-audio");
                            command.add("--audio-format");
                            command.add("mp3"); // also can be used "wav"/"aac" format
                            break;
                        case null:
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + selectedFormat);
                    }

                    command.add("-o");
                    command.add(folderToUse.getAbsolutePath() + "/%(title)s.%(ext)s");
                    command.add(videoUrl);

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();


                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d)%"); // ищем процент
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            int progress = (int) Float.parseFloat(matcher.group(1));
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(progress);
                                progressBar.setString(progress + "%");
                            });
                        } else {
                            String finalLine = line;
                            SwingUtilities.invokeLater(() -> progressBar.setString(finalLine.trim()));
                        }
                    }

                    int exitCode = process.waitFor();
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(100);
                        progressBar.setString(exitCode == 0 ? "Download completed!" : "Download failed!");
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

        frame.setVisible(true);
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
