import com.formdev.flatlaf.FlatLightLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

class YouTubeDownloader {
    private static final String APPDATA = System.getenv("APPDATA");
    private static final File YTDLP_DIR = new File(APPDATA, "yt-dlp-app");
    private static final File YTDLP_EXE = new File(YTDLP_DIR, "yt-dlp.exe");
    private static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";

    private static final File FFMPEG_DIR = new File(YTDLP_DIR, "ffmpeg");
    private static final File FFMPEG_EXE = new File(FFMPEG_DIR, "ffmpeg.exe");
    private static final String FFMPEG_ZIP_URL = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";

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
        if (!downloadFolder[0].exists()) downloadFolder[0].mkdirs();

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
        progressBar.setPreferredSize(new Dimension(500, 20));
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.add(progressBar);
        frame.add(progressPanel, BorderLayout.CENTER);

        // BOTTOM
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String[] formats = {
                "(dual) Video+Audio/YT music",
                "TikTok, Instagram, X.com",
                "Video only",
                "Audio/YT music"};

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
            if (input.isEmpty()) { JOptionPane.showMessageDialog(frame, "Please enter at least one video URL!"); return; }

            String[] urls = input.split("\\r?\\n");
            List<String> videoUrls = new ArrayList<>();
            for (String url : urls) if (!url.trim().isEmpty()) videoUrls.add(url.trim());
            if (videoUrls.isEmpty()) { JOptionPane.showMessageDialog(frame, "No valid URLs found!"); return; }

            downloadButton.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setString("Starting download...");

            new Thread(() -> {
                try {
                    checkAndDownloadYTDLP();
                    checkAndDownloadFFMPEG();
                    String selectedFormat = (String) formatBox.getSelectedItem();

                    for (int i = 0; i < videoUrls.size(); i++) {
                        String videoUrl = videoUrls.get(i);
                        List<String> command = new ArrayList<>();
                        command.add(YTDLP_EXE.getAbsolutePath());

                        switch (selectedFormat) {
                            case "(dual) Video+Audio/YT music":
                                command.add("-f");
                                command.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]");
                                command.add("--merge-output-format");
                                command.add("mp4");
                                command.add("--ffmpeg-location");
                                command.add(FFMPEG_EXE.getAbsolutePath());
                                break;
                            case "Video only":
                                command.add("-f");
                                command.add("bestvideo[ext=mp4]");
                                break;
                            case "Audio/YT music":
                                command.add("-f");
                                command.add("bestaudio");
                                command.add("--extract-audio");
                                command.add("--audio-format");
                                command.add("mp3");
                                command.add("--ffmpeg-location");
                                command.add(FFMPEG_EXE.getAbsolutePath());
                                break;
                            case "TikTok, Instagram, X.com":
                                command.add("-f");
                                command.add("best[ext=mp4]");
                                break;
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
                    SwingUtilities.invokeLater(() -> { progressBar.setValue(100); progressBar.setString("All downloads completed!"); downloadButton.setEnabled(true); });
                } catch (IOException | InterruptedException ex) {
                    SwingUtilities.invokeLater(() -> { progressBar.setString("Error: " + ex.getMessage()); downloadButton.setEnabled(true); });
                }
            }).start();
        });
    }

    private static void checkAndDownloadYTDLP() throws IOException {
        if (!YTDLP_DIR.exists()) YTDLP_DIR.mkdirs();
        if (!YTDLP_EXE.exists()) {
            System.out.println("yt-dlp not found, downloading latest version...");
            try (InputStream in = new URL(YTDLP_URL).openStream();
                 FileOutputStream out = new FileOutputStream(YTDLP_EXE)) {
                in.transferTo(out);
            }
            YTDLP_EXE.setExecutable(true);
        }
    }

    private static void checkAndDownloadFFMPEG() throws IOException {
        if (!FFMPEG_DIR.exists()) FFMPEG_DIR.mkdirs();

        // Очистим старые распакованные папки, чтобы не оставалось мусора
        cleanupOldFfmpegExtracts();

        if (FFMPEG_EXE.exists()) {
            // уже есть - в порядке
            return;
        }

        System.out.println("ffmpeg not found, downloading zip...");

        File zipFile = new File(FFMPEG_DIR, "ffmpeg.zip");
        try (InputStream in = new URL(FFMPEG_ZIP_URL).openStream();
             FileOutputStream out = new FileOutputStream(zipFile)) {
            in.transferTo(out);
        }

        // Попытка извлечь только ffmpeg.exe из архива
        extractFfmpegFromZip(zipFile, FFMPEG_EXE);

        // удаляем zip (не нужен)
        zipFile.delete();

        if (!FFMPEG_EXE.exists()) {
            throw new IOException("Не найден ffmpeg.exe внутри архива.");
        }
        // делаем исполняемым
        FFMPEG_EXE.setExecutable(true, false);

        System.out.println("ffmpeg installed to: " + FFMPEG_EXE.getAbsolutePath());
    }

    private static void extractFfmpegFromZip(File zipFile, File outFile) throws IOException {
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            ZipEntry candidate = null;

            // Сначала попробуем найти наиболее корректный путь: */bin/ffmpeg.exe
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName().replace('\\','/').toLowerCase();
                if (!e.isDirectory() && name.endsWith("/bin/ffmpeg.exe")) {
                    candidate = e;
                    break;
                }
            }

            // Если не нашли /bin/ffmpeg.exe, пройдёмся ещё раз и найдём любой ffmpeg.exe
            if (candidate == null) {
                Enumeration<? extends ZipEntry> entries2 = zf.entries();
                while (entries2.hasMoreElements()) {
                    ZipEntry e = entries2.nextElement();
                    String name = e.getName().replace('\\','/').toLowerCase();
                    if (!e.isDirectory() && name.endsWith("ffmpeg.exe")) {
                        candidate = e;
                        break;
                    }
                }
            }

            if (candidate == null) {
                throw new IOException("В архиве не найден ffmpeg.exe");
            }

            // Запишем только этот файл в целевую папку (перепишем, если был)
            try (InputStream is = zf.getInputStream(candidate);
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
            }
        }
    }

    // Удаляем старые подпапки/файлы, которые могли появиться от некорректной распаковки
    private static void cleanupOldFfmpegExtracts() {
        File dir = FFMPEG_DIR;
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            // Оставляем только ffmpeg.exe и ffmpeg.zip (zip будет перезаписан)
            if (f.getName().equalsIgnoreCase("ffmpeg.exe") || f.getName().equalsIgnoreCase("ffmpeg.zip")) {
                continue;
            }
            // рекурсивно удаляем всё остальное (без ошибки, если не удалось)
            deleteRecursivelyQuiet(f);
        }
    }

    private static void deleteRecursivelyQuiet(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursivelyQuiet(c);
        }
        try { f.delete(); } catch (Exception ignored) {}
    }
}
