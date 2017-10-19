package com.github.epochcoder.imagedownloader.core;

import com.github.epochcoder.imagedownloader.util.FileUtil;
import com.github.epochcoder.imagedownloader.util.StringUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Simple image scraper
 * @author Willie Scholtz
 */
public class ImageDownloader {
    /**
     * the class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ImageDownloader.class);

    /**
     * the amount of threads to run in
     */
    private static final int THREADS = Runtime.getRuntime().availableProcessors();

    /**
     * the timeout for retrieving a document
     */
    private static final int TIMEOUT = 5000;

    /**
     * the digit template pattern
     */
    private static final String DIGIT_PATTERN = "\\$\\[digit\\]";

    /**
     * the selector for selecting images in the document object
     */
    private String cssSelector = "img";
    /**
     * the template that should be used to match images after the base URL
     * <tt>${digit}</tt> will be replaced with the current range
     */
    private String urlTemplate = "$[digit]";
    /**
     * the count at which the pattern will stop matching and figuring out URL's
     */
    private int failureCount = 10;
    /**
     * the range at which to start downloading,
     * the end will be auto-determined according to failureCount
     */
    private int startRange = 0;
    /**
     * should we pad zero to nine?
     */
    private boolean paddedDigits = false;
    /**
     * the base URL at which to start downloads
     */
    private final String baseUrl;
    /**
     * the thread service to use
     */
    private final ExecutorService service = Executors.newFixedThreadPool(THREADS);


    /**
     * starts this image downloader with the specified arguments
     * @param baseURL the base URL at which to start downloads
     * @param urlTemplate the template that should be used to match images after the base URL
     * <tt>${digit}</tt> will be replaced with the current range.
     * @param selector the CSS selector to use for matching images
     * @param startRange the range at which to start downloading (the digit in the pattern)
     * @param failureCount the count at which the pattern will stop matching and figuring out URL's
     */
    public ImageDownloader(String baseURL, String urlTemplate, String selector,
            int startRange, int failureCount) {
        this.baseUrl = baseURL;
        this.urlTemplate = urlTemplate;

        if (!StringUtil.isNull(selector)) {
            this.cssSelector = selector;
        }

        if (failureCount > 0) {
            this.failureCount = failureCount;
        }

        if (startRange > 0) {
            this.startRange = startRange;
        }
    }

    /**
     * tries to determine the next URL bases on the supplied template and current range
     * @param nextRange the next range
     * @return the auto-determined next possible URL
     */
    private String determineNextURL(final int nextRange) {
        String next = String.valueOf(nextRange);
        if (this.paddedDigits && nextRange < 10) {
            next = "0" + nextRange;
        }

        return this.urlTemplate.replaceAll(DIGIT_PATTERN, next);
    }

    /**
     * Gets all URL references by using the set up patterns and ranges
     * @param information used to keep track of progress
     * @return a non-null list of URL objects;
     */
    public List<URL> searchForImages(final DownloadInformation information) {
        final String uniqueId = "retrieve-info";
        final List<String> visitedLinks = new ArrayList<>();
        final List<URL> resources = new ArrayList<>();

        boolean hasTemplate = !StringUtil.isNull(this.urlTemplate);
        int start = this.startRange;
        int failCount = 0;

        information.onStart(uniqueId, -1);
        while (failCount <= this.failureCount) {
            final String next = hasTemplate ? this.determineNextURL(start++) : null;
            try {
                final String cUrl = this.baseUrl + (next == null ? "" : next);
                if (!visitedLinks.contains(cUrl)) {
                    final Document document = Jsoup.connect(cUrl)
                            .userAgent("ImageScraper")
                            .timeout(TIMEOUT).get();

                    // add to visited irrigardeless of failure
                    visitedLinks.add(cUrl);

                    if (document != null) {
                        LOG.trace("Got document from url[{}], parsing...", this.baseUrl + next);

                        final Elements elements = document.select(this.cssSelector);
                        if (elements != null && !elements.isEmpty()) {
                            LOG.trace("Found elements, looking for images");
                            for (Element image : elements) {
                                if ("img".equals(image.tagName())) {
                                    final String src = image.absUrl("src");
                                    if (!StringUtil.isNull(src)) {
                                        LOG.trace("Found image source[{}], adding to list...", src);
                                        final URL url = new URL(src);
                                        if (!resources.contains(url)) {
                                            resources.add(url);
                                            information.onStatusChange(
                                                    uniqueId, (start - 1), -1, src);
                                        }

                                        // reset fail count, we had success
                                        failCount = 0;
                                    } else {
                                        LOG.warn("Found image[{}], but it had no source, increasing error count [{}/{}]",
                                                image, ++failCount, this.failureCount);
                                    }
                                } else {
                                    LOG.warn("Found image[{}], but it was not an image, increasing error count [{}/{}]",
                                            image, ++failCount, this.failureCount);
                                }
                            }
                        } else {
                            // no elements for selector, could be empty page, anything really, increase failcount
                            LOG.warn("Could find images using selector[{}] on document[{}], increasing error count [{}/{}]",
                                    this.cssSelector, cUrl, ++failCount, this.failureCount);
                        }
                    } else {
                        // no document, increase failcount
                        LOG.warn("Could not open document/timeout for [{}], increasing error count [{}/{}]",
                                cUrl, ++failCount, this.failureCount);
                    }
                } else {
                    // already seen, increase failcount
                    LOG.warn("Already seen url[{}], increasing error count [{}/{}]",
                            cUrl, ++failCount, this.failureCount);
                }
            } catch (IOException e) {
                information.onException(uniqueId, e);
                LOG.warn("Could not open/read stream to [{}], increasing error count [{}/{}]",
                        this.baseUrl + next, ++failCount, this.failureCount);
            }
        }

        LOG.debug("Got resources to download\n{}", resources);
        information.onComplete(uniqueId);

        return Collections.unmodifiableList(resources);
    }

    /**
     * downloads the given resources in threads using the class executor.
     * @param resources the resources to download.
     * @param systemPath the path where to save them.
     * @param information the download information object
     * @return a set of the tasks busy executing
     */
    public final Set<Future<?>> downloadResources(List<URL> resources, String systemPath,
            final DownloadInformation information) {
        final Map<Integer, List<URL>> resourceThreads = new HashMap<>();
        final Set<Future<?>> tasks = new HashSet<>(THREADS);
        final File path = new File(systemPath);

        if (resources != null && !resources.isEmpty()) {
            final int size = resources.size();
            if (size > THREADS) {
                // split the resource list up into bundles
                final int split = size / THREADS;
                final int rest = size % THREADS;

                int from = 0;
                int to = split;

                for (int i = 0; i < THREADS; i++) {
                    resourceThreads.put(i, new ArrayList<>(
                            resources.subList(from, to)));
                    from += split;
                    to += split;
                }

                if (rest > 0) {
                    // just allocate the rest of the urls to the first bundle
                    resourceThreads.get(0).addAll(new ArrayList<>(resources
                            .subList(resources.size() - rest, resources.size())));
                }
            } else {
                // add all resources
                resourceThreads.put(0, resources);
            }

            // now start processing
            LOG.trace("Starting to process/download [{}]", resourceThreads);

            for (Iterator<Map.Entry<Integer, List<URL>>> resourceIterator =
                    resourceThreads.entrySet().iterator(); resourceIterator.hasNext();) {
                final Map.Entry<Integer, List<URL>> resourceEntry = resourceIterator.next();
                final List<URL> urls = resourceEntry.getValue();
                final String uniqueId = "downloader-" + resourceEntry.getKey();

                // submit the dowloads
                tasks.add(this.service.submit(new Runnable() {
                    @Override
                    public void run() {
                        information.onStart(uniqueId, urls.size());
                        for (int i = 0; i < urls.size(); i++) {
                            URL url = urls.get(i);
                            information.onStatusChange(uniqueId, i + 1, urls.size(),
                                    url.toString());

                            try {
                                download(url, path);
                            } catch (IOException | URISyntaxException e) {
                                information.onException(uniqueId, e);
                            }
                        }

                        information.onComplete(uniqueId);
                    }
                }));
            }
        }

        return tasks;
    }

    /**
     * downloads the specified URL to the supplied system path
     * @param url the URL to download
     * @param path the root system path to save at
     * @throws Exception
     */
    private static long download(URL url, File path) throws URISyntaxException, IOException  {
        final String uriPath = url.toURI().toString();
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        final InputStream is = conn.getInputStream();

        File file = new File(path, uriPath
                .substring(uriPath.lastIndexOf('/')));
        FileUtil.delete(file);
        FileUtil.write(file, new BufferedInputStream(is), false);
        return file.length();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.service.shutdownNow();
    }

    /**
     * @param paddedDigits the paddedDigits to set
     */
    public void setPaddedDigits(boolean paddedDigits) {
        this.paddedDigits = paddedDigits;
    }
}