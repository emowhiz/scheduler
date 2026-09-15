package com.example.scheduler.availability;

import com.example.scheduler.availability.domain.Interval;
import com.example.scheduler.availability.domain.IntervalMath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntervalMathTest {

    private static Instant hour(int hour) {
        return Instant.parse(String.format("2026-01-01T%02d:00:00Z", hour));
    }

    private static Interval interval(int startHour, int endHour) {
        return new Interval(hour(startHour), hour(endHour));
    }

    @Nested
    class IntervalConstruction {

        @Test
        void rejectsNonPositiveRange() {
            assertThatThrownBy(() -> new Interval(hour(10), hour(10)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Merge {

        @Test
        void combinesOverlappingIntervals() {
            List<Interval> merged = IntervalMath.merge(List.of(interval(9, 11), interval(10, 12)));
            assertThat(merged).containsExactly(interval(9, 12));
        }

        @Test
        void combinesTouchingIntervals() {
            List<Interval> merged = IntervalMath.merge(List.of(interval(9, 10), interval(10, 11)));
            assertThat(merged).containsExactly(interval(9, 11));
        }

        @Test
        void keepsDisjointIntervalsSeparate() {
            List<Interval> merged = IntervalMath.merge(List.of(interval(9, 10), interval(11, 12)));
            assertThat(merged).containsExactly(interval(9, 10), interval(11, 12));
        }

        @Test
        void sortsUnorderedInput() {
            List<Interval> merged = IntervalMath.merge(List.of(interval(11, 12), interval(9, 10)));
            assertThat(merged).containsExactly(interval(9, 10), interval(11, 12));
        }

        @Test
        void emptyInputYieldsEmptyOutput() {
            assertThat(IntervalMath.merge(List.of())).isEmpty();
        }
    }

    @Nested
    class Subtract {

        @Test
        void removesMiddleOfAnInterval() {
            List<Interval> result = IntervalMath.subtract(List.of(interval(9, 17)), List.of(interval(12, 13)));
            assertThat(result).containsExactly(interval(9, 12), interval(13, 17));
        }

        @Test
        void removesFromTheStart() {
            List<Interval> result = IntervalMath.subtract(List.of(interval(9, 17)), List.of(interval(9, 10)));
            assertThat(result).containsExactly(interval(10, 17));
        }

        @Test
        void removesFromTheEnd() {
            List<Interval> result = IntervalMath.subtract(List.of(interval(9, 17)), List.of(interval(16, 17)));
            assertThat(result).containsExactly(interval(9, 16));
        }

        @Test
        void removesEntireInterval() {
            List<Interval> result = IntervalMath.subtract(List.of(interval(9, 10)), List.of(interval(8, 11)));
            assertThat(result).isEmpty();
        }

        @Test
        void multipleRemovalsAcrossMultipleBaseIntervals() {
            List<Interval> result = IntervalMath.subtract(
                    List.of(interval(9, 12), interval(13, 18)),
                    List.of(interval(10, 11), interval(14, 15), interval(17, 19)));
            assertThat(result)
                    .containsExactly(interval(9, 10), interval(11, 12), interval(13, 14), interval(15, 17));
        }

        @Test
        void noOverlapReturnsBaseUnchanged() {
            List<Interval> result = IntervalMath.subtract(List.of(interval(9, 10)), List.of(interval(11, 12)));
            assertThat(result).containsExactly(interval(9, 10));
        }

        @Test
        void emptyRemoveListReturnsBaseUnchanged() {
            List<Interval> result = IntervalMath.subtract(List.of(interval(9, 10)), List.of());
            assertThat(result).containsExactly(interval(9, 10));
        }
    }

    @Nested
    class Intersect {

        @Test
        void findsOverlapAcrossTwoUsers() {
            List<Interval> result = IntervalMath.intersect(List.of(interval(9, 12)), List.of(interval(11, 14)));
            assertThat(result).containsExactly(interval(11, 12));
        }

        @Test
        void noOverlapYieldsEmpty() {
            List<Interval> result = IntervalMath.intersect(List.of(interval(9, 10)), List.of(interval(11, 12)));
            assertThat(result).isEmpty();
        }

        @Test
        void touchingIntervalsYieldNoOverlap() {
            // half-open intervals: [9,10) and [10,11) share no instant
            List<Interval> result = IntervalMath.intersect(List.of(interval(9, 10)), List.of(interval(10, 11)));
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class IntersectAll {

        @Test
        void acrossThreeUsers() {
            List<Interval> result = IntervalMath.intersectAll(List.of(
                    List.of(interval(9, 17)),
                    List.of(interval(10, 15)),
                    List.of(interval(11, 12), interval(13, 16))));
            assertThat(result).containsExactly(interval(11, 12), interval(13, 15));
        }

        @Test
        void emptyListsYieldsEmpty() {
            assertThat(IntervalMath.intersectAll(List.of())).isEmpty();
        }
    }

    @Nested
    class AtLeast {

        @Test
        void filtersOutShortWindows() {
            Interval thirtyMinutes = new Interval(hour(9), hour(9).plusSeconds(1800));
            List<Interval> filtered =
                    IntervalMath.atLeast(List.of(thirtyMinutes, interval(10, 12)), Duration.ofHours(1));
            assertThat(filtered).containsExactly(interval(10, 12));
        }

        @Test
        void keepsIntervalsExactlyAtMinDuration() {
            List<Interval> filtered = IntervalMath.atLeast(List.of(interval(9, 10)), Duration.ofHours(1));
            assertThat(filtered).containsExactly(interval(9, 10));
        }
    }

    @Nested
    class Clip {

        @Test
        void boundsIntervalsToWindow() {
            List<Interval> result = IntervalMath.clip(List.of(interval(8, 20)), interval(9, 17));
            assertThat(result).containsExactly(interval(9, 17));
        }

        @Test
        void dropsIntervalsEntirelyOutsideWindow() {
            List<Interval> result = IntervalMath.clip(List.of(interval(1, 2), interval(20, 22)), interval(9, 17));
            assertThat(result).isEmpty();
        }
    }
}
