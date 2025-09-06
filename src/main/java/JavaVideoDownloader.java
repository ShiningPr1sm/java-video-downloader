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
        frame.setLocation((screenSize.width - 500) / 2, (screenSize.height - 240) / 2);
        frame.setLayout(null);
        frame.setResizable(false);

        try {
            Image icon = ImageIO.read(Objects.requireNonNull(YouTubeDownloader.class.getResource("/youtube_icon.png")));
            frame.setIconImage(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JLabel label = new JLabel("Enter media URL:");
        label.setBounds(10, 10, 200, 25);
        frame.add(label);

        JTextArea textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBounds(10, 50, 480, 60);
        frame.add(scrollPane);

        JButton folderButton = new JButton("Select Folder");
        folderButton.setBounds(10, 75, 150, 25);
        frame.add(folderButton);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setBounds(10, 110, 480, 25);
        progressBar.setStringPainted(true);
        frame.add(progressBar);

        ImageIcon icon = new ImageIcon(Objects.requireNonNull(YouTubeDownloader.class.getResource("/thumbnail_icon.png")));
        Image scaled = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);

        JButton thumbnailButton = new JButton();
        thumbnailButton.setIcon(new ImageIcon(scaled));
        thumbnailButton.setBounds(515 - 56, 10, 30, 30);
        thumbnailButton.setToolTipText("Download thumbnail");

        frame.add(thumbnailButton);
        String[] formats = {"(dual) Video+Audio/YT music",
                "TikTok, Instagram",
                /*"X.com (twitter)",*/
                "Video",
                "Audio/YT music"};
        JComboBox<String> formatBox = new JComboBox<>(formats);
        formatBox.setBounds(140, 145, 200, 25);
        frame.add(formatBox);

        JButton downloadButton = new JButton("Download");
        downloadButton.setBounds(10, 145, 120, 25);
        frame.add(downloadButton);

        final File[] downloadFolder = {new File(System.getProperty("user.home"), "Downloads/JavaVideoDownloader")};
        if (!downloadFolder[0].exists()) {
            downloadFolder[0].mkdirs();
        }

        folderButton.addActionListener(_ -> {
            JFileChooser chooser = new JFileChooser(downloadFolder[0]);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = chooser.showOpenDialog(frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                downloadFolder[0] = chooser.getSelectedFile();
            }
        });

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