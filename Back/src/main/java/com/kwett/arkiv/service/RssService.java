package com.kwett.arkiv.service;

import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.downloader.response.Response;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
public class RssService {

    private final String videoIdsFile;
    private final String rssUrl;
    private final Set<String> knownVideoIds = new HashSet<>();
    private static final Logger logger = LoggerFactory.getLogger(RssService.class);
    private final String downloadFolder;

    public RssService(
            @Value("${video.ids.file}") String videoIdsFile,
            @Value("${rss.url}") String rssUrl,
            @Value("${download.folder}") String downloadFolder){
        this.videoIdsFile = videoIdsFile;
        this.rssUrl = rssUrl;
        this.downloadFolder = downloadFolder;

        logger.info("RSS URL: {}", rssUrl);
        logger.info("Video IDs File: {}", videoIdsFile);

        try {
            if (!Files.exists(Paths.get(videoIdsFile))) {
                Files.createFile(Paths.get(videoIdsFile));
                logger.info("Fichier d'ID connu créé : {}", videoIdsFile);
            }

            try (Stream<String> lines = Files.lines(Paths.get(videoIdsFile))) {
                lines.forEach(knownVideoIds::add);
            }
        } catch (IOException e) {
            logger.warn("Erreur lors de la gestion du fichier d'ID connu : ", e);
        }
    }

    @Scheduled(fixedRateString = "${rss.check.interval}")
    public void checkForNewVideos() {
        try {
            URI feedSource = new URI(rssUrl);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedSource.toURL()));

            for (SyndEntry entry : feed.getEntries()) {
                String videoId = entry.getUri();

                if (!knownVideoIds.contains(videoId)) {
                    knownVideoIds.add(videoId);
                    logger.info("Nouvelle vidéo détectée : Titre : {} | Lien : {}", entry.getTitle(), videoId);
                    downloadVideo(videoId);
                    saveKnownVideos();
                }

            }
        } catch (IOException e) {
            logger.error("Erreur d'entrée/sortie lors du traitement du flux RSS : ", e);
        } catch (FeedException e) {
            logger.error("Erreur de parsing du flux RSS : ", e);
        } catch (Exception e) {
            logger.error("Erreur inattendue : ", e);
        }
    }

    private void saveKnownVideos() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(videoIdsFile))) {
            for (String videoId : knownVideoIds) {
                writer.write(videoId);
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error("Erreur lors de la sauvegarde des vidéos connues : ", e);
        }
    }

    public void downloadVideo(String videoUrl) {
        try {
            YoutubeDownloader downloader = new YoutubeDownloader();

            String videoId;

            if (videoUrl.contains("v=")) {
                videoId = videoUrl.split("v=")[1];
                if (videoId.contains("&")) {
                    videoId = videoId.split("&")[0];
                }
            } else if (videoUrl.startsWith("yt:video:")) {
                videoId = videoUrl.replace("yt:video:", "");
            } else {
                logger.error("URL vidéo invalide : {}", videoUrl);
                return;
            }

            RequestVideoInfo requestInfo = new RequestVideoInfo(videoId);
            Response<VideoInfo> responseInfo = downloader.getVideoInfo(requestInfo);
            VideoInfo videoInfo = responseInfo.data();


            VideoDetails details = videoInfo.details();

            System.out.println("Titre : " + details.title());
            System.out.println("Durée : " + details.lengthSeconds() + " secondes");

            if (details.isLive()) {
                logger.warn("La vidéo est un live stream et ne peut pas être téléchargée pour le moment.");
                return;
            }


            VideoWithAudioFormat format = (VideoWithAudioFormat) videoInfo.bestVideoWithAudioFormat();

            if (format == null) {
                logger.error("Aucun format vidéo avec audio disponible pour : {}", details.title());
                return;
            }

            File downloadsDir = new File(downloadFolder);
            if (!downloadsDir.exists()) {
                boolean created = downloadsDir.mkdirs();
                if (!created) {
                    logger.error("Impossible de créer le dossier : {}", downloadsDir.getAbsolutePath());
                    throw new IOException("Erreur lors de la création du dossier " + downloadsDir.getAbsolutePath());
                }
            }

            RequestVideoFileDownload requestDownload = new RequestVideoFileDownload(format)
                    .saveTo(downloadsDir)
                    .renameTo(details.title())
                    .overwriteIfExists(true);

            Response<File> responseDownload = downloader.downloadVideoFile(requestDownload);
            File downloadedFile = responseDownload.data();
            requestDownload.callback(new YoutubeProgressCallback<>() {
                @Override
                public void onDownloading(int progress) {
                    logger.info("Téléchargement en cours : {}% pour {}", progress, details.title());
                }

                @Override
                public void onFinished(File file) {
                    logger.info("Téléchargement terminé : {}", file.getAbsolutePath());
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.error("Erreur lors du téléchargement : ", throwable);
                }
            });

            System.out.println("Téléchargement terminé : " + downloadedFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("erreur :", e);
        }
    }
}



