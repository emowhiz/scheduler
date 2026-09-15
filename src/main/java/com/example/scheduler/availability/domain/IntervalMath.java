package com.example.scheduler.availability.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class IntervalMath {

    public static List<Interval> merge(List<Interval> intervals) {
        if (intervals.isEmpty()) {
            return List.of();
        }
        List<Interval> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparing(Interval::start));

        List<Interval> merged = new ArrayList<>();
        Instant currentStart = sorted.getFirst().start();
        Instant currentEnd = sorted.getFirst().end();
        for (int i = 1; i < sorted.size(); i++) {
            Interval next = sorted.get(i);
            if (!next.start().isAfter(currentEnd)) {
                if (next.end().isAfter(currentEnd)) {
                    currentEnd = next.end();
                }
            } else {
                merged.add(new Interval(currentStart, currentEnd));
                currentStart = next.start();
                currentEnd = next.end();
            }
        }
        merged.add(new Interval(currentStart, currentEnd));
        return merged;
    }


    public static List<Interval> subtract(List<Interval> base, List<Interval> toRemove) {
        List<Interval> mergedBase = merge(base);
        List<Interval> mergedRemove = merge(toRemove);
        if (mergedRemove.isEmpty()) {
            return mergedBase;
        }

        List<Interval> result = new ArrayList<>();
        for (Interval b : mergedBase) {
            Instant cursor = b.start();
            for (Interval r : mergedRemove) {
                if (!r.end().isAfter(cursor)) {
                    continue;
                }
                if (!r.start().isBefore(b.end())) {
                    break;
                }
                if (r.start().isAfter(cursor)) {
                    result.add(new Interval(cursor, r.start()));
                }
                if (r.end().isAfter(cursor)) {
                    cursor = r.end();
                }
            }
            if (cursor.isBefore(b.end())) {
                result.add(new Interval(cursor, b.end()));
            }
        }
        return result;
    }


    public static List<Interval> intersect(List<Interval> a, List<Interval> b) {
        List<Interval> ma = merge(a);
        List<Interval> mb = merge(b);
        List<Interval> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < ma.size() && j < mb.size()) {
            Interval x = ma.get(i);
            Interval y = mb.get(j);
            intersectPair(x, y).ifPresent(result::add);
            if (x.end().isBefore(y.end())) {
                i++;
            } else {
                j++;
            }
        }
        return result;
    }


    public static List<Interval> intersectAll(List<List<Interval>> lists) {
        if (lists.isEmpty()) {
            return List.of();
        }
        List<Interval> acc = merge(lists.getFirst());
        for (int i = 1; i < lists.size() && !acc.isEmpty(); i++) {
            acc = intersect(acc, lists.get(i));
        }
        return acc;
    }

    public static List<Interval> atLeast(List<Interval> intervals, Duration minDuration) {
        return intervals.stream()
                .filter(i -> !Duration.between(i.start(), i.end()).minus(minDuration).isNegative())
                .toList();
    }


    public static List<Interval> clip(List<Interval> intervals, Interval window) {
        List<Interval> result = new ArrayList<>();
        for (Interval i : intervals) {
            intersectPair(i, window).ifPresent(result::add);
        }
        return result;
    }

    private static Optional<Interval> intersectPair(Interval x, Interval y) {
        Instant start = maxInstant(x.start(), y.start());
        Instant end = minInstant(x.end(), y.end());
        return start.isBefore(end) ? Optional.of(new Interval(start, end)) : Optional.empty();
    }

    private static Instant maxInstant(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant minInstant(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
