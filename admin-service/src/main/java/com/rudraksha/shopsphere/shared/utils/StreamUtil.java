package com.rudraksha.shopsphere.shared.utils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StreamUtil {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private StreamUtil() {
    }

    public static <T> Collector<T, ?, Optional<T>> toSingleResult() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.isEmpty()) {
                        return Optional.empty();
                    }
                    if (list.size() > 1) {
                        throw new IllegalStateException("Expected single result but found " + list.size());
                    }
                    return Optional.of(list.get(0));
                }
        );
    }

    public static <T> Collector<T, ?, T> toSingleResultOrThrow() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.isEmpty()) {
                        throw new IllegalStateException("Expected single result but found none");
                    }
                    if (list.size() > 1) {
                        throw new IllegalStateException("Expected single result but found " + list.size());
                    }
                    return list.get(0);
                }
        );
    }

    public static <T> void batchProcess(Collection<T> items, Consumer<List<T>> processor) {
        batchProcess(items, DEFAULT_BATCH_SIZE, processor);
    }

    public static <T> void batchProcess(Collection<T> items, int batchSize, Consumer<List<T>> processor) {
        if (items == null || items.isEmpty()) {
            return;
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be at least 1");
        }
        if (processor == null) {
            throw new IllegalArgumentException("Processor cannot be null");
        }

        List<T> batch = new ArrayList<>(batchSize);
        for (T item : items) {
            batch.add(item);
            if (batch.size() >= batchSize) {
                processor.accept(new ArrayList<>(batch));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            processor.accept(batch);
        }
    }

    public static <T, R> List<R> batchProcess(Collection<T> items, int batchSize, Function<List<T>, List<R>> processor) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be at least 1");
        }
        if (processor == null) {
            throw new IllegalArgumentException("Processor cannot be null");
        }

        List<R> results = new ArrayList<>();
        List<T> batch = new ArrayList<>(batchSize);

        for (T item : items) {
            batch.add(item);
            if (batch.size() >= batchSize) {
                results.addAll(processor.apply(new ArrayList<>(batch)));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            results.addAll(processor.apply(batch));
        }
        return results;
    }

    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        if (size < 1) {
            throw new IllegalArgumentException("Partition size must be at least 1");
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T>... streams) {
        if (streams == null || streams.length == 0) {
            return Stream.empty();
        }
        Stream<T> result = streams[0];
        for (int i = 1; i < streams.length; i++) {
            result = Stream.concat(result, streams[i]);
        }
        return result;
    }

    public static <T> List<T> filterToList(Collection<T> items, Predicate<T> predicate) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public static <T, R> List<R> mapToList(Collection<T> items, Function<T, R> mapper) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    public static <T, R> List<R> flatMapToList(Collection<T> items, Function<T, Collection<R>> mapper) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .flatMap(item -> mapper.apply(item).stream())
                .collect(Collectors.toList());
    }

    public static <T> Optional<T> findFirst(Collection<T> items, Predicate<T> predicate) {
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        return items.stream()
                .filter(predicate)
                .findFirst();
    }

    public static <T> boolean anyMatch(Collection<T> items, Predicate<T> predicate) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        return items.stream().anyMatch(predicate);
    }

    public static <T> boolean allMatch(Collection<T> items, Predicate<T> predicate) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        return items.stream().allMatch(predicate);
    }

    public static <T> boolean noneMatch(Collection<T> items, Predicate<T> predicate) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        return items.stream().noneMatch(predicate);
    }
}
