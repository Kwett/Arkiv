package com.kwett.arkiv.service;

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

import java.io.BufferedWriter;
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

    public RssService(
            @Value("${video.ids.file}") String videoIdsFile,
            @Value("${rss.url}") String rssUrl) {
        this.videoIdsFile = videoIdsFile;
        this.rssUrl = rssUrl;

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
                    logger.info("Nouvelle vidéo détectée : Titre : {} | Lien : {}", entry.getTitle(), entry.getLink());
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
}


