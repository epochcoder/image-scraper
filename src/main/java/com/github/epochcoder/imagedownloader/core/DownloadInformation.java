package com.github.epochcoder.imagedownloader.core;

/**
 * Interface for helping with information
 * about the current downloads
 * @author Willie Scholtz
 */
public interface DownloadInformation {
    /**
     * called when the downloading starts with the total amount of files to be scraped
     * @param uniqueId the unique id of the current downloader
     * @param total the total amount of files
     */
    void onStart(final String uniqueId, int total);

    /**
     * called when an exception occurs while downloading
     * @param uniqueId the unique id of the current downloader
     * @param exception the exception
     */
    void onException(final String uniqueId, Throwable exception);

    /**
     * called when the status of the downloading for this information changes
     * @param uniqueId the unique id of the current downloader
     * @param current the current range of files
     * @param total the total amount of files
     * @param currUrl the current URL being downloaded
     */
    void onStatusChange(final String uniqueId, int current, int total, String currUrl);

    /**
     * called when this information sequence has finished downloading
     * @param uniqueId the unique id of the current downloader
     */
    void onComplete(final String uniqueId);
}