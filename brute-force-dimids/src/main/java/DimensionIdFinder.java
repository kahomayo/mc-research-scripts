import com.google.common.hash.Hashing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class DimensionIdFinder implements AutoCloseable {

    static class Results {
        /** Indicates if finished because of exception */
        boolean finishedExceptionally;
        /** The first batch which was not executed */
        long nextBatch;
        /** The set of found results */
        Map<Integer, String> foundStrings;

        public Results(boolean finishedExceptionally, long nextBatch, Map<Integer, String> foundStrings) {
            this.finishedExceptionally = finishedExceptionally;
            this.nextBatch = nextBatch;
            this.foundStrings = foundStrings;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Results results = (Results) o;
            return finishedExceptionally == results.finishedExceptionally &&
                    nextBatch == results.nextBatch &&
                    foundStrings.equals(results.foundStrings);
        }

        @Override
        public int hashCode() {
            return Objects.hash(finishedExceptionally, nextBatch, foundStrings);
        }

        @Override
        public String toString() {
            return "Results{" +
                    "finishedExceptionally=" + finishedExceptionally +
                    ", nextBatch=" + nextBatch +
                    ", foundStrings=" + foundStrings +
                    '}';
        }
    }

    private final static int BATCH_SIZE = 1_000_000;

    private final ConcurrentLinkedQueue<Map<Integer, String>> resultsQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong nextBatch;
    private final ExecutorService hashingExecutor;
    private final ExecutorService accumulatingExecutor;
    private final int nThreads;

    public DimensionIdFinder(long nextBatch, int nThreads) {
        this.nThreads = nThreads;
        this.nextBatch = new AtomicLong(nextBatch);
        hashingExecutor = Executors.newFixedThreadPool(nThreads);
        accumulatingExecutor = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<Results> findResults(int min, int max, AtomicBoolean stopToken, boolean shouldLog) {
        IntPredicate isRelevant = x -> min <= x && x <= max;
        Predicate<Map<Integer, String>> isDone = m -> IntStream.rangeClosed(min, max).allMatch(m::containsKey);
        var futures = new CompletableFuture<?>[nThreads];
        for (int i = 0; i < nThreads; ++i) {
            futures[i] = (CompletableFuture.runAsync(() -> processBatches(isRelevant, stopToken), hashingExecutor));
        }
        CompletableFuture<Results> acc = CompletableFuture.supplyAsync(() -> accumulateResults(isDone, stopToken, shouldLog), accumulatingExecutor);
        return CompletableFuture.allOf(futures).thenCombine(acc, (unused, results) -> results);
    }

    private Results accumulateResults(Predicate<Map<Integer, String>> isDone, AtomicBoolean stopToken, boolean shouldLog) {
        try {
            var accumulatedResults = new HashMap<Integer, String>();
            while (!stopToken.get()) {
                var value = resultsQueue.poll();
                if (value == null) {
                    Thread.sleep(1_000);
                    continue;
                }
                merge(accumulatedResults, value, shouldLog);
                if (isDone.test(accumulatedResults)) {
                    stopToken.set(true);
                }
            }
            return new Results(false, nextBatch.get(), accumulatedResults);
        } catch (Exception e) {
            e.printStackTrace();
            return new Results(true, nextBatch.get(), new HashMap<>());
        }
    }

    private void processBatches(IntPredicate isInteresting, AtomicBoolean stopToken) {
        while (!stopToken.get()) {
            long batch = nextBatch.getAndAdd(1);
            var ids = findIdsFrom(batch, isInteresting);
            resultsQueue.offer(ids);
        }
    }

    @Override
    public void close() throws InterruptedException {
        hashingExecutor.shutdown();
        accumulatingExecutor.shutdown();
        if (!hashingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            hashingExecutor.shutdownNow();
        }
        if (!accumulatingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            accumulatingExecutor.shutdownNow();
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int min = 70;
        int max = 70;
        int nThreads = Runtime.getRuntime().availableProcessors();
        boolean shouldLog = true;
        try (DimensionIdFinder m = new DimensionIdFinder(0, nThreads)) {
            var stopToken = new AtomicBoolean(false);
            // Stop either when the user sends anything on stdin or when you have calculated everything.
            // This is super janky.
            var readTask = CompletableFuture.runAsync(() -> {
                try {
                    System.in.read();
                } catch (IOException e) {
                    // whatever, just shut down
                }
                stopToken.set(true);
            });
            m.findResults(min, max, stopToken, shouldLog)
                    .thenAccept(System.out::println)
                    .thenAccept(x -> readTask.cancel(true))
                    .get();

        }
    }

    private static String toAlphabeticRadix(long num) {
        char[] str = Long.toString(num, 26).toCharArray();
        for (int i = 0; i < str.length; i++) {
            str[i] += str[i] > '9' ? 10 : 49;
        }
        return new String(str);
    }

    private static Map<Integer, String> findIdsFrom(final long batch, IntPredicate isInteresting) {
        Map<Integer, String> results = new HashMap<>();
        for (long i = batch * BATCH_SIZE; i < (batch + 1) * BATCH_SIZE; ++i) {
            String string = toAlphabeticRadix(i);
            @SuppressWarnings("UnstableApiUsage") int result = Hashing.sha256()
                    .hashString(string + ":why_so_salty#LazyCrypto", StandardCharsets.UTF_8)
                    .asInt() & Integer.MAX_VALUE;
            if (isInteresting.test(result)) {
                results.put(result, string);
            }
        }
        return results;
    }

    private static void logResult(int key, String value) {
        System.out.printf("[%d] = %s\n", key, value);
    }

    private static void merge(HashMap<Integer, String> acc, Map<Integer, String> value, boolean log) {
        value.entrySet().stream().filter(e -> !acc.containsKey(e.getKey())).forEach(e -> {
            if (log) {
                logResult(e.getKey(), e.getValue());
            }
            acc.put(e.getKey(), e.getValue());
        });
    }

}
