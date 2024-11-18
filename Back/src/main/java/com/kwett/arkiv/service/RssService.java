package com.kwett.arkiv.service;

import com.kwett.arkiv.repository.FileRepository;
import com.kwett.arkiv.model.FileInfo;
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RssService {

    private final String rssUrl;
    private final Set<String> knownVideoIds = new HashSet<>();
    private static final Logger logger = LoggerFactory.getLogger(RssService.class);
    private final String downloadFolder;
    private final FileRepository fileRepository;

    public RssService(
        @Value("${rss.url}") String rssUrl,
        @Value("${download.folder}") String downloadFolder,
        FileRepository fileRepository){
        this.fileRepository = fileRepository;
        this.rssUrl = rssUrl;
        this.downloadFolder = downloadFolder;

        List<FileInfo> existingFiles = fileRepository.findAll();
        for (FileInfo file : existingFiles) {
            knownVideoIds.add(file.getName());
        }

        logger.info("RSS URL: {}", rssUrl);
    }

    @Scheduled(fixedRateString = "${rss.check.interval}")
    public void checkForNewVideos() {
        try {
            URI feedSource = new URI(rssUrl);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedSource.toURL()));

            for (SyndEntry entry : feed.getEntries()) {
                String videoId = entry.getUri();
                String videoTitle = entry.getTitle();
                LocalDateTime publishedDate = LocalDateTime.now();

                if (!knownVideoIds.contains(videoId)) {
                    knownVideoIds.add(videoId);
                    logger.info("Nouvelle vidéo détectée : Titre : {} | Lien : {}", entry.getTitle(), videoId);
                    boolean downloaded = downloadVideo(videoId);
                    if (downloaded) {
                        logger.info("{} est telechargé avec succes", entry.getTitle());
                        saveKnownVideos(videoTitle, publishedDate);
                    }
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

    private void saveKnownVideos(String videoTitle, LocalDateTime publishedDate ) {

        logger.info("video title : {}", videoTitle);
        logger.info("length : {}", fileRepository.findByName(videoTitle));


        if (fileRepository.findByName(videoTitle).isEmpty()) {
            FileInfo newFile = new FileInfo();
            newFile.setName(videoTitle);
            newFile.setSize("Inconnu");
            newFile.setDate(publishedDate);
            fileRepository.save(newFile);

            logger.info("Vidéo ajoutée en base de données : {}", videoTitle);
        }
    }

    public boolean downloadVideo(String videoUrl) {
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
                return false;
            }

            RequestVideoInfo requestInfo = new RequestVideoInfo(videoId);
            Response<VideoInfo> responseInfo = downloader.getVideoInfo(requestInfo);
            VideoInfo videoInfo = responseInfo.data();


            VideoDetails details = videoInfo.details();

            System.out.println("Titre : " + details.title());
            System.out.println("Durée : " + details.lengthSeconds() + " secondes");

            if (details.isLive()) {
                logger.warn("La vidéo est un live stream et ne peut pas être téléchargée pour le moment.");
                return false;
            }


            VideoWithAudioFormat format = (VideoWithAudioFormat) videoInfo.bestVideoWithAudioFormat();

            if (format == null) {
                logger.error("Aucun format vidéo avec audio disponible pour : {}", details.title());
                return false;
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
    return true;
    }
}



