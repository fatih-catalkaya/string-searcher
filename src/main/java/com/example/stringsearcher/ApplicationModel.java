package com.example.stringsearcher;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class ApplicationModel {
    // Properties
    private final BooleanProperty searchRunning;
    private final IntegerProperty threadCount;
    private final LongProperty searchRuntime;
    private final StringProperty runstatusProperty;
    private final Property<File> scanFileProperty;
    private final StringProperty searchTextProperty;
    private final ListProperty<String> searchResultProperty;


    // Objects needed for the runtime timer
    private final ScheduledThreadPoolExecutor runtimeTimer;
    private final Runnable updateRuntimeTimerTask;
    private ScheduledFuture<?> runtimeFuture;


    // Objects needed for the execution of the searcher threads
    private ExecutorService executor;
    private final List<CompletableFuture<Void>> futures;


    public ApplicationModel() {
        this.searchRunning = new SimpleBooleanProperty(false);
        this.threadCount = new SimpleIntegerProperty(1);
        this.searchRuntime = new SimpleLongProperty(0L);
        this.scanFileProperty = new SimpleObjectProperty<>(null);
        this.searchTextProperty = new SimpleStringProperty("");
        this.searchResultProperty = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<>()));
        this.runstatusProperty = new SimpleStringProperty("STOPPED");

        this.runtimeTimer = new ScheduledThreadPoolExecutor(1);
        this.updateRuntimeTimerTask =
                () -> Platform.runLater(() -> searchRuntime.setValue(searchRuntime.getValue()+1));

        futures = new ArrayList<>();
    }


    /**
     * This function starts the runtime timer
     */
    private void startRuntimeTimer() {
        this.searchRuntime.setValue(0L);
        this.runtimeFuture = this.runtimeTimer.scheduleAtFixedRate(this.updateRuntimeTimerTask, 0, 1, TimeUnit.SECONDS);
    }


    /**
     * This function stops the runtime timer
     */
    private void stopRuntimeTimer(){
        this.runtimeFuture.cancel(true);
        this.runtimeTimer.remove(this.updateRuntimeTimerTask);
    }


    /**
     * This function stops the search and updates the UI accordingly
     */
    public void stopSearch(){
        executor.shutdownNow();
        stopRuntimeTimer();
        searchRunning.set(false);
        updateStatusText("STOPPED");
    }


    /**
     * This function starts the search.
     * Takes care of UI, spawning of search threads
     * and awaits asynchronously the result
     */
    public void startSearch() {
        searchResultProperty.get().clear();
        searchRunning.set(true);
        startRuntimeTimer();
        updateStatusText("LOADING");
        final int numThreads = threadCount.get();
        final String searchString = searchTextProperty.get().toLowerCase();

        // Use one more thread since the controller will also allocate one thread
        executor = Executors.newFixedThreadPool(numThreads+1);

        CompletableFuture.supplyAsync(() -> {
            // In this stage, we load all strings from the input file
            // We use a CompletableFuture, so UI does not block
            try {
                final List<String> stringsToSearch = loadStrings(scanFileProperty.getValue());
                Collections.shuffle(stringsToSearch); // Here we shuffle the list of strings, as required
                return stringsToSearch;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor).whenCompleteAsync((list, ex) -> {
            // Here we check if all Strings could be successfully imported
            if(ex != null){
                errorStage(ex);
                throw new RuntimeException(ex);
            }
            else{
                spawnSearcherStage(list, numThreads, searchString);
            }
        }, executor).thenRun(this::finishedStage);
    }


    /**
     * This function spawns all threads that search the list of Strings
     * @param list List with strings
     * @param numThreads number of threads to spawn
     * @param searchString the String to search for
     */
    private void spawnSearcherStage(final List<String> list, final int numThreads, final String searchString){
        updateStatusText("SEARCHING");

        // Determine the number of items one thread is going to scan
        int sizeForOneThread = (int) Math.ceil( ((double)list.size()) / ((double)numThreads) );
        for(int i = 0; i < numThreads; i++){
            // One thread is going to scan the indices in the interval [startIdx; stopIdx]
            final int startIdx = i * sizeForOneThread;
            final int stopIdx = Math.min( ((i+1) * sizeForOneThread + 1), list.size());

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for(int j = startIdx; j < stopIdx; j++){
                    final String currentString = list.get(j);
                    if(currentString.toLowerCase().contains(searchString)){
                        addItemToResultList(currentString);
                    }
                }
            }, executor);
            futures.add(future);
        }
    }


    /**
     * Updates UI in case of an import failure
     * @param ex the exception that occurred
     */
    private void errorStage(Throwable ex){
        ex.printStackTrace();
        Platform.runLater(() -> {
            updateStatusText("UNABLE TO LOAD STRINGS");
            searchRunning.set(false);
            stopRuntimeTimer();
        });
    }

    /**
     * Waits for all searcher threads to finish scanning
     * Then updates UI.
     */
    private void finishedStage() {
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() ->
            Platform.runLater(() -> {
                Platform.runLater(this::stopRuntimeTimer);
                runstatusProperty.setValue("FINISHED");
                searchRunning.set(false);
            }
        ));
    }


    /**
     * Loads all lines of a file into a list of strings
     * @param file File to read from
     * @return List with String per line
     * @throws IOException exception
     */
    private List<String> loadStrings(File file) throws IOException {
        List<String> stringList = new ArrayList<>();
        BufferedReader r = new BufferedReader(new FileReader(file));
        String buffer;
        while((buffer = r.readLine()) != null){
            stringList.add(buffer);
        }
        r.close();
        return stringList;
    }


    /**
     * Adds an item to the list of results
     * Ensures, that update is performed on UI thread
     * @param item The item to add
     */
    private void addItemToResultList(final String item){
        Platform.runLater(() -> {
            synchronized (searchResultProperty){
                searchResultProperty.get().add(item);
            }
        });
    }


    /**
     * Updates the runstatus text
     * Ensures, that update is performed on UI thread
     * @param status The new status to set
     */
    private void updateStatusText(final String status){
        Platform.runLater(() -> runstatusProperty.setValue(status));
    }


    // region Getters
    public BooleanProperty searchRunningProperty() {
        return searchRunning;
    }

    public IntegerProperty threadCountProperty() {
        return threadCount;
    }

    public LongProperty searchRuntimeProperty() {
        return searchRuntime;
    }


    public Property<File> scanFilePropertyProperty() {
        return scanFileProperty;
    }

    public StringProperty searchTextPropertyProperty() {
        return searchTextProperty;
    }

    public ListProperty<String> searchResultPropertyProperty() {
        return searchResultProperty;
    }

    public StringProperty runstatusProperty(){
        return runstatusProperty;
    }
    // endregion
}
