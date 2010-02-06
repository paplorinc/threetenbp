/*
 * Copyright (c) 2007-2010, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package javax.time;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.time.calendar.format.CalendricalParseException;

/**
 * A duration between two instants on the time-line.
 * <p>
 * The Java Time Framework models time as a series of instantaneous events,
 * known as instants, along a single time-line. This class represents the
 * duration between two of those instants.
 * <p>
 * A physical instant is an instantaneous event.
 * However, for practicality the API and this class uses a precision of nanoseconds.
 * <p>
 * A physical duration could be of infinite length.
 * However, for practicality the API and this class limits the length to the
 * number of seconds that can be held in a <code>long</code>.
 * <p>
 * The nanosecond part is stored as an amount between 0 and 999,999,999 that
 * is added to the length of the duration in seconds. For example, the negative
 * duration of <code>PT-0.1S</code> is represented as -1 second and 900,000,000 nanoseconds.
 * <p>
 * In strict scientific terms, the unit of "seconds" only has a precise meaning
 * when applied to an instant. This is because it is the instant that defines the
 * time scale used, not the duration. For example, the simplified UTC time scale
 * used by {@link Instant} ignore leap seconds, which alters the effective length
 * of a second. By comparison, the TAI time scale follows the international scientific
 * definition of a second exactly. For most applications, this subtlety will be irrelevant.
 * <p>
 * Duration is immutable and thread-safe.
 *
 * @author Michael Nascimento Santos
 * @author Stephen Colebourne
 */
public final class Duration implements Comparable<Duration>, Serializable {

    /**
     * Constant for a duration of zero.
     */
    public static final Duration ZERO = new Duration(0, 0);
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Constant for nanos per second.
     */
    private static final int NANOS_PER_SECOND = 1000000000;

    /**
     * The number of seconds in the duration.
     */
    private final long seconds;
    /**
     * The number of nanoseconds in the duration, expressed as a fraction of the
     * number of seconds. This is always positive, and never exceeds 999,999,999.
     */
    private final int nanos;

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of <code>Duration</code> from a number of seconds.
     *
     * @param seconds  the number of seconds
     * @return the created Duration, never null
     */
    public static Duration seconds(long seconds) {
        return create(seconds, 0);
    }

    /**
     * Obtains an instance of <code>Duration</code> from a number of seconds
     * and an adjustment in nanoseconds.
     * <p>
     * This methods allows an arbitrary number of nanoseconds to be passed in.
     * The factory will alter the values of the second and nanosecond in order
     * to ensure that the stored nanosecond is in the range 0 to 999,999,999.
     * For example, the following will result in the exactly the same duration:
     * <pre>
     *  Duration.duration(3, 1);
     *  Duration.duration(4, -999999999);
     *  Duration.duration(2, 1000000001);
     * </pre>
     *
     * @param seconds  the number of seconds
     * @param nanoAdjustment  the nanosecond adjustment to the number of seconds, positive or negative
     * @return the created Duration, never null
     * @throws ArithmeticException if the adjustment causes the seconds to exceed the capacity of Duration
     */
    public static Duration seconds(long seconds, long nanoAdjustment) {
        long secs = MathUtils.safeAdd(seconds, nanoAdjustment / NANOS_PER_SECOND);
        int nos = (int) (nanoAdjustment % NANOS_PER_SECOND);
        if (nos < 0) {
            nos += NANOS_PER_SECOND;
            secs = MathUtils.safeDecrement(secs);
        }
        return create(secs, nos);
    }

    /**
     * Obtains an instance of <code>Duration</code> from a number of seconds.
     *
     * @param seconds  the number of seconds
     * @return the created Duration, never null
     * @throws ArithmeticException if the input seconds exceeds the capacity of a duration
     */
    public static Duration seconds(BigDecimal seconds) {
        Instant.checkNotNull(seconds, "Seconds must not be null");
        return nanos(seconds.movePointRight(9).toBigIntegerExact());
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of <code>Duration</code> from a number of milliseconds.
     *
     * @param millis  the number of milliseconds
     * @return the created Duration, never null
     */
    public static Duration millis(long millis) {
        long secs = millis / 1000;
        int mos = (int) (millis % 1000);
        if (mos < 0) {
            mos += 1000;
            secs--;
        }
        return create(secs, mos * 1000000);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of <code>Duration</code> from a number of nanoseconds.
     * <p>
     * This factory will split the supplied nanosecond amount to ensure that the
     * stored nanosecond is in the range 0 to 999,999,999.
     *
     * @param nanos  the number of nanoseconds
     * @return the created Duration, never null
     */
    public static Duration nanos(long nanos) {
        long secs = nanos / NANOS_PER_SECOND;
        int nos = (int) (nanos % NANOS_PER_SECOND);
        if (nos < 0) {
            nos += NANOS_PER_SECOND;
            secs--;
        }
        return create(secs, nos);
    }

    /**
     * Obtains an instance of <code>Duration</code> from a number of nanoseconds.
     * <p>
     * This factory will split the supplied nanosecond amount to ensure that the
     * stored nanosecond is in the range 0 to 999,999,999.
     *
     * @param nanos  the number of nanoseconds, not null
     * @return the created Duration, never null
     * @throws ArithmeticException if the input nanoseconds exceeds the capacity of Duration
     */
    public static Duration nanos(BigInteger nanos) {
        Instant.checkNotNull(nanos, "Nanos must not be null");
        BigInteger[] divRem = nanos.divideAndRemainder(Instant.BILLION);
        if (divRem[0].bitLength() > 63) {
            throw new ArithmeticException("Exceeds capacity of Duration: " + nanos);
        }
        return seconds(divRem[0].longValue(), divRem[1].intValue());
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of <code>Duration</code> from a number of standard length minutes.
     * <p>
     * This factory uses the standard definition of a minute, where each
     * minute is 60 seconds.
     *
     * @param minutes  the number of minutes
     * @return the created Duration, never null
     * @throws ArithmeticException if the input minutes exceeds the capacity of Duration
     */
    public static Duration standardMinutes(long minutes) {
        return create(MathUtils.safeMultiply(minutes, 60), 0);
    }

    /**
     * Obtains an instance of <code>Duration</code> from a number of standard length hours.
     * <p>
     * This factory uses the standard definition of an hour, where each
     * hour is 3600 seconds.
     *
     * @param hours  the number of hours
     * @return the created Duration, never null
     * @throws ArithmeticException if the input hours exceeds the capacity of Duration
     */
    public static Duration standardHours(long hours) {
        return create(MathUtils.safeMultiply(hours, 3600), 0);
    }

    /**
     * Obtains an instance of <code>Duration</code> from a number of standard length days.
     * <p>
     * This factory uses the standard definition of a day, where each
     * day is 86400 seconds which implies a 24 hour day.
     *
     * @param days  the number of days
     * @return the created Duration, never null
     * @throws ArithmeticException if the input days exceeds the capacity of Duration
     */
    public static Duration standardDays(long days) {
        return create(MathUtils.safeMultiply(days, 86400), 0);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of <code>Duration</code> representing the duration between two instants.
     * This method will return a negative duration if the end is before the start.
     *
     * @param startInclusive  the start instant, inclusive, not null
     * @param endExclusive  the end instant, exclusive, not null
     * @return the created Duration, never null
     * @throws ArithmeticException if the duration exceeds the capacity of Duration
     */
    public static Duration durationBetween(InstantProvider startInclusive, InstantProvider endExclusive) {
        Instant start = Instant.instant(startInclusive);
        Instant end = Instant.instant(endExclusive);
        long secs = MathUtils.safeSubtract(end.getEpochSeconds(), start.getEpochSeconds());
        int nanos = end.getNanoOfSecond() - start.getNanoOfSecond();
        if (nanos < 0) {
            nanos += NANOS_PER_SECOND;
            secs = MathUtils.safeDecrement(secs);
        }
        return create(secs, nanos);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of <code>Duration</code> from a string.
     * <p>
     * This will parse the string produced by <code>toString()</code> which is
     * the ISO8601 format <code>PTnS</code> where <code>n</code> is
     * the number of seconds with optional decimal part.
     * The number must consist of ASCII numerals.
     * There must only be a negative sign at the start of the number and it can
     * only be present if the value is less then zero.
     * There must be at least one digit before any decimal point.
     * There must be between 1 and 9 inclusive digits after any decimal point.
     * The letters (P, T and S) will be accepted in upper or lower case.
     * The decimal point may be either a dot or a comma.
     *
     * @param text  the text to parse, not null
     * @return the created Duration, never null
     * @throws IllegalArgumentException if the text cannot be parsed to a Duration
     */
    public static Duration parse(final String text) {
        Instant.checkNotNull(text, "Text to parse must not be null");
        int len = text.length();
        if (len < 4 ||
                (text.charAt(0) != 'P' && text.charAt(0) != 'p') ||
                (text.charAt(1) != 'T' && text.charAt(1) != 't') ||
                (text.charAt(len - 1) != 'S' && text.charAt(len - 1) != 's') ||
                (len == 5 && text.charAt(2) == '-' && text.charAt(3) == '0')) {
            throw new CalendricalParseException("Duration could not be parsed: " + text, text, 0);
        }
        String numberText = text.substring(2, len - 1).replace(',', '.');
        int dot = numberText.indexOf('.');
        try {
            if (dot == -1) {
                // no decimal places
                return create(Long.parseLong(numberText), 0);
            }
            // decimal places
            boolean negative = false;
            if (numberText.charAt(0) == '-') {
                negative = true;
            }
            long secs = Long.parseLong(numberText.substring(0, dot));
            numberText = numberText.substring(dot + 1);
            len = numberText.length();
            if (len == 0 || len > 9 || numberText.charAt(0) == '-') {
                throw new CalendricalParseException("Duration could not be parsed: " + text, text, 2);
            }
            int nanos = Integer.parseInt(numberText);
            switch (len) {
                case 1:
                    nanos *= 100000000;
                    break;
                case 2:
                    nanos *= 10000000;
                    break;
                case 3:
                    nanos *= 1000000;
                    break;
                case 4:
                    nanos *= 100000;
                    break;
                case 5:
                    nanos *= 10000;
                    break;
                case 6:
                    nanos *= 1000;
                    break;
                case 7:
                    nanos *= 100;
                    break;
                case 8:
                    nanos *= 10;
                    break;
            }
            return negative ? seconds(secs, -nanos) : create(secs, nanos);
            
        } catch (ArithmeticException ex) {
            throw new CalendricalParseException("Duration could not be parsed: " + text, text, 2, ex);
        } catch (NumberFormatException ex) {
            throw new CalendricalParseException("Duration could not be parsed: " + text, text, 2, ex);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Creates an instance of Duration using seconds and nanoseconds.
     *
     * @param seconds  the length of the duration in seconds
     * @param nanoAdjustment  the nanosecond adjustment within the second, from 0 to 999,999,999
     */
    private static Duration create(long seconds, int nanoAdjustment) {
        if ((seconds | nanoAdjustment) == 0) {
            return ZERO;
        }
        return new Duration(seconds, nanoAdjustment);
    }

    /**
     * Constructs an instance of Duration using seconds and nanoseconds.
     *
     * @param seconds  the length of the duration in seconds
     * @param nanos  the nanoseconds within the second, from 0 to 999,999,999
     */
    private Duration(long seconds, int nanos) {
        super();
        this.seconds = seconds;
        this.nanos = nanos;
    }

    /**
     * Resolves singletons.
     *
     * @return the resolved instance
     */
    private Object readResolve() {
        return (seconds| nanos) == 0 ? ZERO : this;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this duration is zero length.
     *
     * @return true if this duration is zero length
     */
    public boolean isZero() {
        return (seconds | nanos) == 0;
    }

    /**
     * Checks if this duration is positive, excluding zero.
     * <p>
     * Periods are allowed to be negative, so this method checks if this period is positive.
     *
     * @return true if this duration is positive and not zero
     */
    public boolean isPositive() {
        return seconds >= 0 && ((seconds | nanos) != 0);
    }

    /**
     * Checks if this duration is positive or zero.
     * <p>
     * Periods are allowed to be negative, so this method checks if this period is positive.
     *
     * @return true if this duration is positive or zero
     */
    public boolean isPositiveOrZero() {
        return seconds >= 0;
    }

    /**
     * Checks if this duration is negative, excluding zero.
     * <p>
     * Periods are allowed to be negative, so this method checks if this period is negative.
     *
     * @return true if this duration is negative and not zero
     */
    public boolean isNegative() {
        return seconds < 0;
    }

    /**
     * Checks if this duration is negative or zero.
     * <p>
     * Periods are allowed to be negative, so this method checks if this period is negative.
     *
     * @return true if this duration is negative or zero
     */
    public boolean isNegativeOrZero() {
        return seconds < 0 || ((seconds | nanos) == 0);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the number of seconds in this duration.
     * <p>
     * The length of the duration is expressed using two fields - seconds and
     * nanoseconds. The nanoseconds is held as a value from 0 to 999,999,999
     * and is an adjustment to the length.
     * <p>
     * A duration can be negative, and this is expressed by the negative sign
     * of the value returned from this method. A duration of -1 nanosecond is
     * stored as -1 seconds plus 999,999,999 nanoseconds.
     *
     * @return the length of the duration in seconds
     * @see #getNanosInSecond()
     */
    public long getSeconds() {
        return seconds;
    }

    /**
     * Gets the number of nanoseconds within the second in this duration.
     * <p>
     * The length of the duration is expressed using two fields - seconds and
     * nanoseconds. The nanoseconds is held as a value from 0 to 999,999,999
     * and is an adjustment to the length.
     * <p>
     * A duration can be negative, and this is expressed by the negative sign
     * of the value returned from {@link #getSeconds()}. A duration of
     * -1 nanosecond is stored as -1 seconds plus 999,999,999 nanoseconds.
     *
     * @return the nanoseconds within the second, from 0 to 999,999,999
     * @see #getSeconds()
     */
    public int getNanosInSecond() {
        return nanos;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this Duration with the specified duration added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param duration  the duration to add, not null
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration plus(Duration duration) {
        long secsToAdd = duration.seconds;
        int nanosToAdd = duration.nanos;
        if (secsToAdd == 0 && nanosToAdd == 0) {
            return this;
        }
        long secs = MathUtils.safeAdd(seconds, secsToAdd);
        int nos = nanos + nanosToAdd;  // safe
        if (nos >= NANOS_PER_SECOND) {
            nos -= NANOS_PER_SECOND;
            secs = MathUtils.safeIncrement(secs);
        }
        return create(secs, nos);
     }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this Duration with the specified number of seconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToAdd  the seconds to add
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration plusSeconds(long secondsToAdd) {
        if (secondsToAdd == 0) {
            return this;
        }
        long secs = MathUtils.safeAdd(seconds, secondsToAdd);
        return create(secs, nanos);
    }

    /**
     * Returns a copy of this Duration with the specified number of milliseconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param millisToAdd  the milliseconds to add
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration plusMillis(long millisToAdd) {
        if (millisToAdd == 0) {
            return this;
        }
        long secondsToAdd = millisToAdd / 1000;
        // add: 0 to 999,000,000, subtract: 0 to -999,000,000
        int nos = ((int) (millisToAdd % 1000)) * 1000000;
        // add: 0 to 0 to 1998,999,999, subtract: -999,000,000 to 999,999,999
        nos += nanos;
        if (nos < 0) {
            nos += NANOS_PER_SECOND;  // subtract: 1,000,000 to 999,999,999
            secondsToAdd--;
        } else if (nos >= NANOS_PER_SECOND) {
            nos -= NANOS_PER_SECOND;  // add: 1 to 998,999,999
            secondsToAdd++;
        }
        return create(MathUtils.safeAdd(seconds, secondsToAdd) , nos);
    }

    /**
     * Returns a copy of this Duration with the specified number of nanoseconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanosToAdd  the nanoseconds to add
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration plusNanos(long nanosToAdd) {
        if (nanosToAdd == 0) {
            return this;
        }
        long secondsToAdd = nanosToAdd / NANOS_PER_SECOND;
        // add: 0 to 999,999,999, subtract: 0 to -999,999,999
        int nos = (int) (nanosToAdd % NANOS_PER_SECOND);
        // add: 0 to 0 to 1999,999,998, subtract: -999,999,999 to 999,999,999
        nos += nanos;
        if (nos < 0) {
            nos += NANOS_PER_SECOND;  // subtract: 1 to 999,999,999
            secondsToAdd--;
        } else if (nos >= NANOS_PER_SECOND) {
            nos -= NANOS_PER_SECOND;  // add: 1 to 999,999,999
            secondsToAdd++;
        }
        return create(MathUtils.safeAdd(seconds, secondsToAdd) , nos);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this Duration with the specified duration subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param duration  the duration to subtract, not null
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration minus(Duration duration) {
        long secsToSubtract = duration.seconds;
        int nanosToSubtract = duration.nanos;
        if (secsToSubtract == 0 && nanosToSubtract == 0) {
            return this;
        }
        long secs = MathUtils.safeSubtract(seconds, secsToSubtract);
        int nos = nanos - nanosToSubtract;  // safe
        if (nos < 0) {
            nos += NANOS_PER_SECOND;
            secs = MathUtils.safeDecrement(secs);
        }
        return create(secs, nos);
     }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this Duration with the specified number of seconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToSubtract the seconds to subtract
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration minusSeconds(long secondsToSubtract) {
        if (secondsToSubtract == 0) {
            return this;
        }
        long secs = MathUtils.safeSubtract(seconds, secondsToSubtract);
        return create(secs, nanos);
    }

    /**
     * Returns a copy of this Duration with the specified number of milliseconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param millisToSubtract  the milliseconds to subtract
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration minusMillis(long millisToSubtract) {
        if (millisToSubtract == 0) {
            return this;
        }
        long secondsToSubtract = millisToSubtract / 1000;
        int nos = ((int) (millisToSubtract % 1000)) * 1000000;
        nos = nanos - nos;
        if (nos < 0) {
            nos += NANOS_PER_SECOND;
            secondsToSubtract++;
        } else if (nos >= NANOS_PER_SECOND) {
            nos -= NANOS_PER_SECOND;
            secondsToSubtract--;
        }
        return create(MathUtils.safeSubtract(seconds, secondsToSubtract), nos);
    }

    /**
     * Returns a copy of this Duration with the specified number of nanoseconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanosToSubtract  the nanoseconds to subtract
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration minusNanos(long nanosToSubtract) {
        if (nanosToSubtract == 0) {
            return this;
        }
        long secondsToSubtract = nanosToSubtract / NANOS_PER_SECOND;
        int nos = (int) (nanosToSubtract % NANOS_PER_SECOND);
        nos = nanos - nos;
        if (nos < 0) {
            nos += NANOS_PER_SECOND;
            secondsToSubtract++;
        } else if (nos >= NANOS_PER_SECOND) {
            nos -= NANOS_PER_SECOND;
            secondsToSubtract--;
        }
        return create(MathUtils.safeSubtract(seconds, secondsToSubtract), nos);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this Duration multiplied by the scalar.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param multiplicand  the value to multiply the duration by
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 0) {
            return ZERO;
        }
        if (multiplicand == 1) {
            return this;
        }
        BigInteger nanos = toNanos();
        nanos = nanos.multiply(BigInteger.valueOf(multiplicand));
        BigInteger[] divRem = nanos.divideAndRemainder(Instant.BILLION);
        if (divRem[0].bitLength() > 63) {
            throw new ArithmeticException("Multiplication result exceeds capacity of Duration: " + this + " * " + multiplicand);
        }
        return seconds(divRem[0].longValue(), divRem[1].intValue());
     }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this Duration divided by the specified value.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param divisor  the value to divide the duration by
     * @return a new updated Duration, never null
     * @throws ArithmeticException if the result exceeds the storage capacity
     */
    public Duration dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        if (divisor == 1) {
            return this;
        }
        BigInteger nanos = toNanos();
        nanos = nanos.divide(BigInteger.valueOf(divisor));
        BigInteger[] divRem = nanos.divideAndRemainder(Instant.BILLION);
        return seconds(divRem[0].longValue(), divRem[1].intValue());
     }

    // TODO: negated
    // TODO: abs
    
    //-----------------------------------------------------------------------
    /**
     * Compares this Duration to another.
     *
     * @param otherDuration  the other duration to compare to, not null
     * @return the comparator value, negative if less, positive if greater
     * @throws NullPointerException if otherDuration is null
     */
    public int compareTo(Duration otherDuration) {
        int cmp = MathUtils.safeCompare(seconds, otherDuration.seconds);
        if (cmp != 0) {
            return cmp;
        }
        return MathUtils.safeCompare(nanos, otherDuration.nanos);
    }

    /**
     * Is this Duration longer than the specified one.
     *
     * @param otherDuration  the other duration to compare to, not null
     * @return true if this duration is longer than the specified duration
     * @throws NullPointerException if otherDuration is null
     */
    public boolean isLongerThan(Duration otherDuration) {
        return compareTo(otherDuration) > 0;
    }

    /**
     * Is this Duration shorter than the specified one.
     *
     * @param otherDuration  the other duration to compare to, not null
     * @return true if this duration is shorter than the specified duration
     * @throws NullPointerException if otherDuration is null
     */
    public boolean isShorterThan(Duration otherDuration) {
        return compareTo(otherDuration) < 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the length of this duration in seconds expressed as a <code>BigDecimal</code>.
     *
     * @return the length of the duration in seconds, with a scale of 9
     */
    public BigDecimal toSeconds() {
        return BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
    }

    /**
     * Returns the length of this duration in nanoseconds expressed as a <code>BigInteger</code>.
     *
     * @return the length of the duration in nanoseconds
     */
    public BigInteger toNanos() {
        return BigInteger.valueOf(seconds).multiply(Instant.BILLION).add(BigInteger.valueOf(nanos));
    }

    /**
     * Returns the length of this duration in nanoseconds expressed as a <code>long</code>.
     * <p>
     * If the duration is too large to fit in a long nanoseconds, then an
     * exception is thrown.
     *
     * @return the length of the duration in nanoseconds
     * @throws ArithmeticException if the length exceeds the capacity of a long
     */
    public long toNanosLong() {
        long millis = MathUtils.safeMultiply(seconds, 1000000000);
        millis = MathUtils.safeAdd(millis, nanos);
        return millis;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the length of this duration in milliseconds.
     * <p>
     * <code>Duration</code> uses a precision of nanoseconds.
     * The conversion will drop any excess precision information as though the
     * amount in nanoseconds was subject to integer division by one million.
     * <p>
     * <code>Duration</code> can store lengths that are too large to be represented
     * by a millisecond value. In this scenario, this method will throw an exception.
     *
     * @return the length of the duration in milliseconds
     * @throws ArithmeticException if the length exceeds the capacity of a long
     */
    public long toMillisLong() {
        long millis = MathUtils.safeMultiply(seconds, 1000);
        millis = MathUtils.safeAdd(millis, nanos / 1000000);
        return millis;
    }

    //-----------------------------------------------------------------------
    /**
     * Is this Duration equal to that specified.
     *
     * @param otherDuration  the other duration, null returns false
     * @return true if the other duration is equal to this one
     */
    @Override
    public boolean equals(Object otherDuration) {
        if (this == otherDuration) {
            return true;
        }
        if (otherDuration instanceof Duration) {
            Duration other = (Duration) otherDuration;
            return this.seconds == other.seconds &&
                   this.nanos == other.nanos;
        }
        return false;
    }

    /**
     * A hash code for this Duration.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return ((int) (seconds ^ (seconds >>> 32))) + (51 * nanos);
    }

    //-----------------------------------------------------------------------
    /**
     * A string representation of this Duration using ISO-8601 seconds based
     * representation.
     * <p>
     * The format of the returned string will be <code>PTnS</code> where n is
     * the seconds and fractional seconds of the duration.
     *
     * @return an ISO-8601 representation of this Duration
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(24);
        buf.append("PT");
        if (seconds < 0 && nanos > 0) {
            if (seconds == -1) {
                buf.append("-0");
            } else {
                buf.append(seconds + 1);
            }
        } else {
            buf.append(seconds);
        }
        if (nanos > 0) {
            int pos = buf.length();
            if (seconds < 0) {
                buf.append(2 * NANOS_PER_SECOND - nanos);
            } else {
                buf.append(nanos + NANOS_PER_SECOND);
            }
            while (buf.charAt(buf.length() - 1) == '0') {
                buf.setLength(buf.length() - 1);
            }
            buf.setCharAt(pos, '.');
        }
        buf.append('S');
        return buf.toString();
    }

}
