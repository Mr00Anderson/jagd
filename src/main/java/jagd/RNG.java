package jagd;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.NumberUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static com.badlogic.gdx.utils.NumberUtils.intBitsToFloat;

/**
 * A wrapper class for working with random number generators in a more friendly way.
 * <br>
 * Includes methods for getting values between two numbers and for getting
 * random elements from a collection or array. There are methods to shuffle
 * a collection and to get a random ordering. There's methods for getting
 * curved distributions, inclusive float and double generation, and more.
 * <br>
 * This implements {@link Random} but does not synchronize any methods,
 * because that slows down that JDK class quite a bit. You should be able
 * to plug in an RNG where existing code expects a Random, but the RNG API
 * has more features.
 * 
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 * @author Tommy Ettinger
 * @author smelC
 */
public class RNG extends Random implements Serializable {

    /**
     * The state of this RNG as a single long. This can be set to any long without losing statistical quality.
     */
    public long state;

    protected double nextNextGaussian;
    protected boolean haveNextNextGaussian = false;
    
    private static final long serialVersionUID = 2352426757973945105L;


    /**
     * Default constructor; uses a random seed.
     */
    public RNG() {
        this(determine(NumberUtils.doubleToLongBits(Math.random())) ^
                determine(NumberUtils.doubleToLongBits(Math.random())));
    }

    /**
     * Uses the given seed verbatim.
     * @param seed any long
     */
    public RNG(long seed) {
        state = seed;
    }

    /**
     * String-seeded constructor; uses {@link #determine(long)} called on String.hashCode() as a seed for this RNG, or 0
     * if seedString is null.
     * @param seedString any String; if null this will use the seed 0
     */
    public RNG(String seedString) {
        this(seedString == null ? 0L : determine(seedString.hashCode()));
    }

    /**
     * Static randomizing method that takes its state as a parameter; state is expected to change between calls to this.
     * It is recommended that you use {@code RNG.determine(++state)} or {@code RNG.determine(--state)} to
     * produce a sequence of different numbers, but you can also use {@code RNG.determine(state += 12345L)} or
     * any odd-number increment. All longs are accepted by this method, and all longs can be produced; passing 0 here
     * will return 0.
     * @param state any long; subsequent calls should change by an odd number, such as with {@code ++state}
     * @return any long
     */
    public static long determine(long state)
    {
        return (state = ((state = (state ^ (state << 41 | state >>> 23) ^ (state << 17 | state >>> 47)) * 0x369DEA0F31A53F85L) ^ state >>> 37 ^ state >>> 25) * 0xDB4F0B9175AE2165L) ^ state >>> 28;
    }

    /**
     * Static randomizing method that takes its state as a parameter and limits output to an int between 0 (inclusive)
     * and bound (exclusive); state is expected to change between calls to this. It is recommended that you use
     * {@code RNG.determineBounded(++state, bound)} or {@code RNG.determineBounded(--state, bound)} to
     * produce a sequence of different numbers, but you can also use
     * {@code RNG.determineBounded(state += 12345L, bound)} or any odd-number increment. All longs are accepted
     * by this method, but not all ints between 0 and bound are guaranteed to be produced with equal likelihood (for any
     * odd-number values for bound, this isn't possible for most generators). The bound can be negative.
     * @param state any long; subsequent calls should change by an odd number, such as with {@code ++state}
     * @param bound the outer exclusive bound, as an int
     * @return an int between 0 (inclusive) and bound (exclusive)
     */
    public static int determineBounded(long state, final int bound)
    {
        return (int)((bound * (((state = ((state = (state ^ (state << 41 | state >>> 23) ^ (state << 17 | state >>> 47)) * 0x369DEA0F31A53F85L) ^ state >>> 37 ^ state >>> 25) * 0xDB4F0B9175AE2165L) ^ state >>> 28) & 0xFFFFFFFFL)) >> 32);
    }

    /**
     * Returns a random float that is deterministic based on state; if state is the same on two calls to this, this will
     * return the same float. This is expected to be called with a changing variable, e.g.
     * {@code RNG.determineFloat(++state)}, where the increment for state should be odd but otherwise doesn't really
     * matter. This should tolerate just about any increment as long as it is odd. The period is 2 to the 64 if you
     * increment or decrement by 1, but there are only 2 to the 30 possible floats between 0 and 1, and this can only
     * generate 2 to the 24 of them.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code RNG.determineFloat(++state)} is recommended to go forwards or
     *              {@code RNG.determineFloat(--state)} to generate numbers in reverse order
     * @return a pseudo-random float between 0f (inclusive) and 1f (exclusive), determined by {@code state}
     */
    public static float determineFloat(long state) {
        return ((state = ((state = (state ^ (state << 41 | state >>> 23) ^ (state << 17 | state >>> 47)) * 0x369DEA0F31A53F85L) ^ state >>> 37 ^ state >>> 25) * 0xDB4F0B9175AE2165L) >>> 40) * 0x1p-24f;
    }

    /**
     * Returns a random double that is deterministic based on state; if state is the same on two calls to this, this
     * will return the same float. This is expected to be called with a changing variable, e.g.
     * {@code RNG.determineDouble(++state)}, where the increment for state should be odd but otherwise doesn't really
     * matter. This should tolerate just about any increment, as long as it is odd. The period is 2 to the 64 if you
     * increment or decrement by 1, but there are only 2 to the 62 possible doubles between 0 and 1, and this can only
     * generate 2 to the 53 of them.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code RNG.determineDouble(++state)} is recommended to go forwards or
     *              {@code RNG.determineDouble(--state)} to generate numbers in reverse order
     * @return a pseudo-random double between 0.0 (inclusive) and 1.0 (exclusive), determined by {@code state}
     */
    public static double determineDouble(long state) {
        return (((state = ((state = (state ^ (state << 41 | state >>> 23) ^ (state << 17 | state >>> 47)) * 0x369DEA0F31A53F85L) ^ state >>> 37 ^ state >>> 25) * 0xDB4F0B9175AE2165L) ^ state >>> 28) & 0x1FFFFFFFFFFFFFL) * 0x1p-53;
    }


    /**
     * Returns a double from an even distribution from min (inclusive) to max
     * (exclusive).
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found double
     */
    public double between(double min, double max) {
        return min + (max - min) * nextDouble();
    }

    /**
     * Returns a float from an even distribution from min (inclusive) to max
     * (exclusive).
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found float
     */
    public float between(float min, float max) {
        return min + (max - min) * nextFloat();
    }

    /**
     * Returns a double from an even distribution from min (inclusive) to max (inclusive).
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (inclusive)
     * @return the found double
     */
    public double betweenInclusive(double min, double max) {
        return min + (max - min) * nextDoubleInclusive();
    }

    /**
     * Returns a float from an even distribution from min (inclusive) to max (inclusive).
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (inclusive)
     * @return the found float
     */
    public float betweenInclusive(float min, float max) {
        return min + (max - min) * nextFloatInclusive();
    }

    /**
     * Returns a value between min (inclusive) and max (exclusive).
     * <p>
     * The inclusive and exclusive behavior is to match the behavior of the
     * similar method that deals with floating point values.
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found value
     */
    public int between(int min, int max) {
        return nextInt(max - min) + min;
    }

    /**
     * Returns a value between min (inclusive) and max (exclusive).
     * <p>
     * The inclusive and exclusive behavior is to match the behavior of the
     * similar method that deals with floating point values.
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found value
     */
    public long between(long min, long max) {
        return nextLong(max - min) + min;
    }

    /**
     * Returns a value between min (inclusive) and max (inclusive).
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found value
     */
    public int betweenInclusive(int min, int max) {
        return nextInt(max + 1 - min) + min;
    }

    /**
     * Returns a value between min (inclusive) and max (inclusive).
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found value
     */
    public long betweenInclusive(long min, long max) {
        return nextLong(max + 1 - min) + min;
    }

    /**
     * Returns the average of a number of randomly selected numbers from the
     * provided range, with min being inclusive and max being exclusive. It will
     * sample the number of times passed in as the third parameter.
     * <p>
     * The inclusive and exclusive behavior is to match the behavior of the
     * similar method that deals with floating point values.
     * <p>
     * This can be used to weight RNG calls to the average between min and max.
     *
     * @param min     the minimum bound on the return value (inclusive)
     * @param max     the maximum bound on the return value (exclusive)
     * @param samples the number of samples to take
     * @return the found value
     */
    public int betweenWeighted(int min, int max, int samples) {
        int sum = 0;
        for (int i = 0; i < samples; i++) {
            sum += between(min, max);
        }

        return Math.round((float) sum / samples);
    }

    /**
     * Returns a random element from the provided array and maintains object
     * type.
     *
     * @param <T>   the type of the returned object
     * @param array the array to get an element from
     * @return the randomly selected element
     */
    public <T> T getRandomElement(T[] array) {
        if (array.length < 1) {
            return null;
        }
        return array[nextInt(array.length)];
    }

    /**
     * Returns a random element from the provided list. If the list is empty
     * then null is returned.
     *
     * @param <T>  the type of the returned object
     * @param list the list to get an element from
     * @return the randomly selected element
     */
    public <T> T getRandomElement(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(nextInt(list.size()));
    }

    /**
     * Returns a random element from the provided Collection, which should have predictable iteration order if you want
     * predictable behavior for identical RNG seeds, though it will get a random element just fine for any Collection
     * (just not predictably in all cases). If you give this a Set, it should be a LinkedHashSet or some form of sorted
     * Set like TreeSet if you want predictable results. Any List or Queue should be fine. Map does not implement
     * Collection, thank you very much Java library designers, so you can't actually pass a Map to this, though you can
     * pass the keys or values. If coll is empty, returns null.
     * <br>
     * Requires iterating through a random amount of coll's elements, so performance depends on the size of coll but is
     * likely to be decent, as long as iteration isn't unusually slow.
     *
     * @param <T>  the type of the returned object
     * @param coll the Collection to get an element from; remember, Map does not implement Collection
     * @return the randomly selected element
     */
    public <T> T getRandomElement(Collection<T> coll) {
        int n;
        if ((n = coll.size()) <= 0) {
            return null;
        }
        n = nextInt(n);
        T t = null;
        Iterator<T> it = coll.iterator();
        while (n-- >= 0 && it.hasNext())
            t = it.next();
        return t;
    }


    /**
     * Mutates the array arr by switching the contents at pos1 and pos2.
     * @param arr an array of T; must not be null
     * @param pos1 an index into arr; must be at least 0 and no greater than arr.length
     * @param pos2 an index into arr; must be at least 0 and no greater than arr.length
     */
    private static <T> void swap(T[] arr, int pos1, int pos2) {
        final T tmp = arr[pos1];
        arr[pos1] = arr[pos2];
        arr[pos2] = tmp;
    }

    /**
     * Mutates the array arr by switching the contents at pos1 and pos2.
     * @param arr an array of T; must not be null
     * @param pos1 an index into arr; must be at least 0 and no greater than arr.length
     * @param pos2 an index into arr; must be at least 0 and no greater than arr.length
     */
    private static void swap(int[] arr, int pos1, int pos2) {
        final int tmp = arr[pos1];
        arr[pos1] = arr[pos2];
        arr[pos2] = tmp;
    }

    /**
     * Shuffle an array using the Fisher-Yates algorithm and returns a shuffled copy.
     * GWT-compatible since GWT 2.8.0, which is the default if you use libGDX 1.9.5 or higher.
     * <br>
     * <a href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Wikipedia has more on this algorithm</a>.
     *
     * @param elements an array of T; will not be modified
     * @param <T>      can be any non-primitive type.
     * @return a shuffled copy of elements
     */
    public <T> T[] shuffle(final T[] elements) {
        final int size = elements.length;
        final T[] array = Arrays.copyOf(elements, size);
        for (int i = size; i > 1; i--) {
            swap(array, i - 1, nextInt(i));
        }
        return array;
    }

    /**
     * Shuffles an array in-place using the Fisher-Yates algorithm.
     * If you don't want the array modified, use {@link #shuffle(Object[], Object[])}.
     * <br>
     * <a href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Wikipedia has more on this algorithm</a>.
     *
     * @param elements an array of T; <b>will</b> be modified
     * @param <T>      can be any non-primitive type.
     * @return elements after shuffling it in-place
     */
    public <T> T[] shuffleInPlace(T[] elements) {
        final int size = elements.length;
        for (int i = size; i > 1; i--) {
            swap(elements, i - 1, nextInt(i));
        }
        return elements;
    }

    /**
     * Shuffles an IntArray in-place using the Fisher-Yates algorithm.
     * <br>
     * <a href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Wikipedia has more on this algorithm</a>.
     *
     * @param array an IntArray to shuffle; <b>will</b> be modified
     */
    public void shuffleInPlace(IntArray array) {
        final int size = array.size;
        final int[] elements = array.items;
        for (int i = size; i > 1; i--) {
            swap(elements, i - 1, nextInt(i));
        }
    }

    /**
     * Shuffle an array using the Fisher-Yates algorithm. DO NOT give the same array for both elements and
     * dest, since the prior contents of dest are rearranged before elements is used, and if they refer to the same
     * array, then you can end up with bizarre bugs where one previously-unique item shows up dozens of times. If
     * possible, create a new array with the same length as elements and pass it in as dest; the returned value can be
     * assigned to whatever you want and will have the same items as the newly-formed array.
     * <br>
     * <a href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Wikipedia has more on this algorithm</a>.
     *
     * @param elements an array of T; will not be modified
     * @param <T>      can be any non-primitive type.
     * @param dest     Where to put the shuffle. If it does not have the same length as {@code elements}, this will use the
     *                 randomPortion method of this class to fill the smaller dest. MUST NOT be the same array as elements!
     * @return {@code dest} after modifications
     */
    public <T> T[] shuffle(T[] elements, T[] dest) {
        if (dest.length != elements.length)
            return randomPortion(elements, dest);
        System.arraycopy(elements, 0, dest, 0, elements.length);
        shuffleInPlace(dest);
        return dest;
    }

    /**
     * Shuffles a {@link Collection} of T using the Fisher-Yates algorithm and returns an ArrayList of T.
     * <br>
     * <a href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Wikipedia has more on this algorithm</a>.
     * @param elements a Collection of T; will not be modified
     * @param <T>      can be any non-primitive type.
     * @return a shuffled ArrayList containing the whole of elements in pseudo-random order.
     */
    public <T> ArrayList<T> shuffle(Collection<T> elements) {
        return shuffle(elements, null);
    }

    /**
     * Shuffles a {@link Collection} of T using the Fisher-Yates algorithm. The result
     * is allocated if {@code buf} is null or if {@code buf} isn't empty,
     * otherwise {@code elements} is poured into {@code buf}.
     * <br>
     * <a href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Wikipedia has more on this algorithm</a>.
     * @param elements a Collection of T; will not be modified
     * @param <T>      can be any non-primitive type.
     * @return a shuffled ArrayList containing the whole of elements in pseudo-random order.
     */
    public <T> ArrayList<T> shuffle(Collection<T> elements, /*@Nullable*/ ArrayList<T> buf) {
        final ArrayList<T> al;
        if (buf == null || !buf.isEmpty())
            al = new ArrayList<T>(elements);
        else {
            al = buf;
            al.addAll(elements);
        }
        final int n = al.size();
        for (int i = n; i > 1; i--) {
            Collections.swap(al, nextInt(i), i - 1);
        }
        return al;
    }
    /**
     * Shuffles a Collection of T items in-place using the Fisher-Yates algorithm.
     * This only shuffles List data structures.
     * If you don't want the array modified, use {@link #shuffle(Collection)}, which returns a List as well.
     * <br>
     * <a href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Wikipedia has more on this algorithm</a>.
     *
     * @param elements a Collection of T; <b>will</b> be modified
     * @param <T>      can be any non-primitive type.
     * @return elements after shuffling it in-place
     */
    public <T> List<T> shuffleInPlace(List<T> elements) {
        final int n = elements.size();
        for (int i = n; i > 1; i--) {
            Collections.swap(elements, nextInt(i), i - 1);
        }
        return elements;
    }


    /**
     * Generates a random permutation of the range from 0 (inclusive) to length (exclusive).
     * Useful for passing to IndexedMap or IndexedSet's reorder() methods.
     *
     * @param length the size of the ordering to produce
     * @return a random ordering containing all ints from 0 to length (exclusive)
     */
    public int[] randomOrdering(int length) {
        if (length <= 0)
            return new int[0];
        return randomOrdering(length, new int[length]);
    }

    /**
     * Generates a random permutation of the range from 0 (inclusive) to length (exclusive) and stores it in
     * the dest parameter, avoiding allocations.
     * Useful for passing to IndexedMap or IndexedSet's reorder() methods.
     *
     * @param length the size of the ordering to produce
     * @param dest   the destination array; will be modified
     * @return dest, filled with a random ordering containing all ints from 0 to length (exclusive)
     */
    public int[] randomOrdering(int length, int[] dest) {
        if (dest == null) return null;

        final int n = Math.min(length, dest.length);
        for (int i = 0; i < n; i++) {
            dest[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            final int r = nextInt(i+1),
                    t = dest[r];
            dest[r] = dest[i];
            dest[i] = t;
        }
        return dest;
    }

    /**
     * Gets a random portion of a Collection and returns it as a new List. Will only use a given position in the given
     * Collection at most once; does this by shuffling a copy of the Collection and getting a section of it.
     *
     * @param data  a Collection of T; will not be modified.
     * @param count the non-negative number of elements to randomly take from data
     * @param <T>   can be any non-primitive type
     * @return a List of T that has length equal to the smaller of count or data.length
     */
    public <T> List<T> randomPortion(Collection<T> data, int count) {
        return shuffle(data).subList(0, Math.min(count, data.size()));
    }

    /**
     * Gets a random subrange of the non-negative ints from start (inclusive) to end (exclusive), using count elements.
     * May return an empty array if the parameters are invalid (end is less than/equal to start, or start is negative).
     *
     * @param start the start of the range of numbers to potentially use (inclusive)
     * @param end   the end of the range of numbers to potentially use (exclusive)
     * @param count the total number of elements to use; will be less if the range is smaller than count
     * @return an int array that contains at most one of each number in the range
     */
    public int[] randomRange(int start, int end, int count) {
        if (end <= start || start < 0)
            return new int[0];

        int n = end - start;
        final int[] data = new int[n];

        for (int e = start, i = 0; e < end; e++) {
            data[i++] = e;
        }

        for (int i = 0; i < n - 1; i++) {
            final int r = i + nextInt(n - i), t = data[r];
            data[r] = data[i];
            data[i] = t;
        }
        final int[] array = new int[Math.min(count, n)];
        System.arraycopy(data, 0, array, 0, Math.min(count, n));
        return array;
    }

    /**
     * Generates a random float with a curved distribution that centers on 0 (where it has a bias) and can (rarely)
     * approach -1f and 1f, but not go beyond those bounds. This is similar to {@link #nextGaussian()} in that it uses
     * a curved distribution, but it is not the same. The distribution for the values is similar to Irwin-Hall, and is
     * frequently near 0 but not too-rarely near -1f or 1f. It cannot produce values greater than or equal to 1f, or
     * less than -1f, but it can produce -1f.
     * @return a deterministic float between -1f (inclusive) and 1f (exclusive), that is very likely to be close to 0f
     */
    public float nextCurvedFloat() {
        final long start = nextLong();
        return   (intBitsToFloat((int)start >>> 9 | 0x3F000000)
                + intBitsToFloat((int) (start >>> 41) | 0x3F000000)
                + intBitsToFloat(((int)(start ^ ~start >>> 20) & 0x007FFFFF) | 0x3F000000)
                + intBitsToFloat(((int) (~start ^ start >>> 30) & 0x007FFFFF) | 0x3F000000)
                - 3f);
    }

    /**
     * Gets a pseudo-random double from the Gaussian distribution, which will usually be between -1.0 and 1.0 but is not
     * always in that range. If you do want values to always be between -1 and 1 and a float is OK, consider using
     * {@link #nextCurvedFloat()}, which is a different distribution that is less sharply-curved towards 0 and
     * terminates at -1 and 1.
     * @return a value from the Gaussian distribution
     */
    @Override
    public double nextGaussian() {
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        } else {
            double v1, v2, s;
            do {
                v1 = 2 * nextDouble() - 1; // between -1 and 1
                v2 = 2 * nextDouble() - 1; // between -1 and 1
                s = v1 * v1 + v2 * v2;
            } while (s >= 1 || s == 0);
            double multiplier = Math.sqrt(-2 * Math.log(s) / s);
            nextNextGaussian = v2 * multiplier;
            haveNextNextGaussian = true;
            return v1 * multiplier;
        }
    }

    /**
     * Gets a random double between 0.0 inclusive and 1.0 exclusive.
     * This returns a maximum of 0.9999999999999999 because that is the largest double value that is less than 1.0 .
     *
     * @return a double between 0.0 (inclusive) and 0.9999999999999999 (inclusive)
     */
    @Override
    public double nextDouble() {
        return (nextLong() & 0x1fffffffffffffL) * 0x1p-53;
    }

    /**
     * This returns a random double between 0.0 (inclusive) and outer (exclusive). The value for outer can be positive
     * or negative. Because of how math on doubles works, there are at most 2 to the 53 values this can return for any
     * given outer bound, and very large values for outer will not necessarily produce all numbers you might expect.
     *
     * @param outer the outer exclusive bound as a double; can be negative or positive
     * @return a double between 0.0 (inclusive) and outer (exclusive)
     */
    public double nextDouble(final double outer) {
        return (nextLong() & 0x1fffffffffffffL) * 0x1p-53 * outer;
    }
    
    /**
     * Gets a random float between 0.0f inclusive and 1.0f exclusive.
     * This returns a maximum of 0.99999994 because that is the largest float value that is less than 1.0f .
     *
     * @return a float between 0f (inclusive) and 0.99999994f (inclusive)
     */
    @Override
    public float nextFloat() {
        return (nextLong() & 0xFFFFFF) * 0x1p-24f; 
    }

    /**
     * This returns a random float between 0.0f (inclusive) and outer (exclusive). The value for outer can be positive
     * or negative. Because of how math on floats works, there are at most 2 to the 24 values this can return for any
     * given outer bound, and very large values for outer will not necessarily produce all numbers you might expect.
     *
     * @param outer the outer exclusive bound as a float; can be negative or positive
     * @return a float between 0f (inclusive) and outer (exclusive)
     */
    public float nextFloat(final float outer) {
        return (nextLong() & 0xFFFFFF) * 0x1p-24f * outer;
    }

    /**
     * Gets a random double between 0.0 inclusive and 1.0 inclusive.
     *
     * @return a double between 0.0 (inclusive) and 1.0 (inclusive)
     */
    public double nextDoubleInclusive()
    {
        return (nextLong() & 0x1fffffffffffffL) * 0x1.0000000000001p-53;
    }

    /**
     * This returns a random double between 0.0 (inclusive) and outer (inclusive). The value for outer can be positive
     * or negative. Because of how math on doubles works, there are at most 2 to the 53 values this can return for any
     * given outer bound, and very large values for outer will not necessarily produce all numbers you might expect.
     *
     * @param outer the outer inclusive bound as a double; can be negative or positive
     * @return a double between 0.0 (inclusive) and outer (inclusive)
     */
    public double nextDoubleInclusive(final double outer) {
        return (nextLong() & 0x1fffffffffffffL) * 0x1.0000000000001p-53 * outer;
    }

    /**
     * Gets a random float between 0.0f inclusive and 1.0f inclusive.
     *
     * @return a float between 0f (inclusive) and 1f (inclusive)
     */
    public float nextFloatInclusive() {
        return (nextLong() & 0xFFFFFF) * 0x1.000002p-24f;
    }

    /**
     * This returns a random float between 0.0f (inclusive) and outer (inclusive). The value for outer can be positive
     * or negative. Because of how math on floats works, there are at most 2 to the 24 values this can return for any
     * given outer bound, and very large values for outer will not necessarily produce all numbers you might expect.
     *
     * @param outer the outer inclusive bound as a float; can be negative or positive
     * @return a float between 0f (inclusive) and outer (inclusive)
     */
    public float nextFloatInclusive(final float outer) {
        return (nextLong() & 0xFFFFFF) * 0x1.000002p-24f * outer;
    }

    /**
     * Get a random bit of state, interpreted as true or false with approximately equal likelihood.
     * You can also consider calling {@link #next(int)} with 1 as the argument to get either 0 or 1.
     * @return a random boolean.
     */
    @Override
    public boolean nextBoolean() {
        return nextLong() < 0L;
    }

    /**
     * Get a random long between Long.MIN_VALUE to Long.MAX_VALUE (both inclusive).
     *
     * @return a 64-bit random long.
     */
    @Override
    public long nextLong() {
        long z = state += 0x9E3779B97F4A7C15L;
        z = (z ^ z >>> 30) * 0xBF58476D1CE4E5B9L;
        z = (z ^ z >>> 27) * 0x94D049BB133111EBL;
        return z ^ z >>> 31;
    }

    /**
     * Given the output of a call to {@link #nextLong()} as {@code out}, this finds the state of the RNG that produced
     * that output. If you set the state of an RNG with {@link #setSeed(long)} to the result of this method and then
     * call {@link #nextLong()} on it, you should get back {@code out}.
     * @param out a long as produced by {@link #nextLong()}, without changes
     * @return the state of the RNG that will produce the given long
     */
    public static long inverseNextLong(long out)
    {
        out ^= out >>> 31 ^ out >>> 62;
        out *= 0x319642B2D24D8EC3L;
        out ^= out >>> 27 ^ out >>> 54;
        out *= 0x96DE1B173F119089L;
        out ^= out >>> 30 ^ out >>> 60;
        return out - 0x9E3779B97F4A7C15L;
        //0x96DE1B173F119089L 0x319642B2D24D8EC3L 0xF1DE83E19937733DL
    }

    /**
     * Returns the number of steps (where a step is equal to one call to most random number methods in this class)
     * needed to go from receiving out1 from an RNG's {@link #nextLong()} method to receiving out2 from another call.
     * This number can be used with {@link #skip(long)} to move an RNG forward or backward by the desired distance.
     * @param out1 a long as produced by {@link #nextLong()}, without changes
     * @param out2 a long as produced by {@link #nextLong()}, without changes
     * @return the number of calls to {@link #nextLong()} that would be required to go from producing out1 to producing out2; can be positive or negative
     */
    public static long distance(long out1, long out2)
    {
        return inverseNextLong(out2) * 0xF1DE83E19937733DL - inverseNextLong(out1) * 0xF1DE83E19937733DL;
    }
    
    /**
     * Exclusive on bound (which must be positive), with an inner bound of 0.
     * If bound is negative or 0 this always returns 0.
     * <br>
     * Credit for this method goes to <a href="https://oroboro.com/large-random-in-range/">Rafael Baptista's blog</a>
     * for the original idea, and the JDK10 Math class' usage of Karatsuba multiplication for the current algorithm. 
     * This method is drastically faster than the previous implementation when the bound varies often (roughly 4x
     * faster, possibly more). It also always gets exactly one random long, so by default it advances the state as much
     * as {@link #nextLong()}; subclasses can generate two ints instead of one long if they prefer.
     *
     * @param bound the outer exclusive bound; should be positive, otherwise this always returns 0L
     * @return a random long between 0 (inclusive) and bound (exclusive)
     */
    public long nextLong(long bound) {
        long rand = nextLong();
        if (bound <= 0) return 0;
        final long randLow = rand & 0xFFFFFFFFL;
        final long boundLow = bound & 0xFFFFFFFFL;
        rand >>>= 32;
        bound >>>= 32;
        final long a = rand * bound;
        final long b = randLow * boundLow;
        return (((b >>> 32) + (rand + randLow) * (bound + boundLow) - a - b) >>> 32) + a;
    }
    
    /**
     * Returns a random non-negative integer between 0 (inclusive) and the given bound (exclusive),
     * or 0 if the bound is 0. The bound can be negative, which will produce 0 or a negative result.
     * Uses an aggressively optimized technique that has some bias, but mostly for values of
     * bound over 1 billion. This method is considered "hasty" since it should be faster than
     * the standard method of generating ints until one is within a threshold, and though it gives
     * up some statistical quality to do so, it has the benefit of guaranteeing that it only generates
     * one random long.
     * <br>
     * Credit goes to Daniel Lemire, http://lemire.me/blog/2016/06/27/a-fast-alternative-to-the-modulo-reduction/
     *
     * @param bound the outer bound (exclusive), can be negative or positive
     * @return the found number
     */
    @Override
    public int nextInt(final int bound) {
        return (int) ((bound * (nextLong() & 0x7FFFFFFFL)) >> 31);
    }

    /**
     * Advances or rolls back the RNG's state without actually generating each number. Skips forward
     * or backward a number of steps specified by advance, where a step is equal to one call to {@link #nextLong()} (or
     * {@link #nextInt()} or {@link #nextInt(int)}, but not not {@link #nextLong(long)}) and returns the random number
     * produced at that step (you can get the state with {@link #getSeed()}).
     *
     * @param advance Number of future generations to skip over; can be negative to backtrack, 0 gets the most-recently-generated number
     * @return the random long generated after skipping forward or backwards by {@code advance} numbers
     */
    public long skip(long advance) {
        long z = (state += 0x9E3779B97F4A7C15L * advance);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
    
    /**
     * Generates random bytes and places them into the given byte array, modifying it in-place.
     * The number of random bytes produced is equal to the length of the byte array. Unlike the
     * method in java.util.Random, this generates 8 bytes at a time, which can be more efficient
     * with many RandomnessSource types than the JDK's method that generates 4 bytes at a time.
     * <br>
     * Adapted from code in the JavaDocs of {@link Random#nextBytes(byte[])}.
     * <br>
     * @param  bytes the byte array to fill with random bytes; cannot be null, will be modified
     * @throws NullPointerException if the byte array is null
     */
    @Override
    public void nextBytes(final byte[] bytes) {
        for (int i = 0; i < bytes.length; )
            for (long r = nextLong(), n = Math.min(bytes.length - i, 8); n-- > 0; r >>>= 8)
                bytes[i++] = (byte) r;
    }

    /**
     * Get a random integer between Integer.MIN_VALUE to Integer.MAX_VALUE (both inclusive).
     *
     * @return a 32-bit random int.
     */
    @Override
    public int nextInt() {
        return next(32);
    }

    /**
     * Get up to 32 bits (inclusive) of random state from the RandomnessSource.
     *
     * @param bits 1 to 32
     * @return a random number that fits in the specified number of bits.
     */
    @Override
    public int next(int bits) {
        long z = state += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return (int)(z ^ (z >>> 31)) >>> (32 - bits);
    }

    /**
     * Returns the exact current state being used by this generator; if you set this back into another RNG using
     * {@link #setSeed(long)}, then it will return the same sequence of numbers this RNG will. 
     * @return the state of this RNG as a long
     */
    public long getSeed() {
        return state;
    }

    /**
     * Sets the current state of the generator, which will be used without changes on the next call to a pseudo-random
     * number method such as {@link #nextLong()}. Does not do anything with the state of the {@link Random} class this
     * extends.
     * @param state
     */
    @Override
    public void setSeed(final long state) {
        this.state = state;
    }
    

    /**
     * Creates a copy of this RNG; it will generate the same random numbers, given the same calls in order, as this RNG
     * at the point copy() is called. The copy will not share references with this RNG.
     *
     * @return a copy of this RNG
     */
    public RNG copy() {
        return new RNG(state);
    }

    /**
     * Generates a random 64-bit long with a number of '1' bits (Hamming weight) approximately equal to bitCount.
     * For example, calling this with a parameter of 32 will be equivalent to calling nextLong() on this object's
     * RandomnessSource (it doesn't consider overridden nextLong() methods, where present, on subclasses of RNG).
     * Calling this with a parameter of 16 will have on average 16 of the 64 bits in the returned long set to '1',
     * distributed pseudo-randomly, while a parameter of 47 will have on average 47 bits set. This can be useful for
     * certain code that uses bits to represent data but needs a different ratio of set bits to unset bits than 1:1.
     * <br>
     * Implementors should limit any overriding method to calling and returning super(), potentially storing any extra
     * information they need to internally, but should not change the result. This works based on a delicate balance of
     * the RandomnessSource producing bits with an even 50% chance of being set, regardless of position, and RNG
     * subclasses that alter the odds won't work as expected here, particularly if those subclasses use doubles
     * internally (which almost always produce less than 64 random bits). You should definitely avoid using certain
     * RandomnessSources that aren't properly pseudo-random, such as any QRNG class (SobolQRNG and VanDerCorputQRNG,
     * pretty much), since these won't fill all 64 bits with equal likelihood.
     *
     * @param bitCount an int, only considered if between 0 and 64, that is the average number of bits to set
     * @return a 64-bit long that, on average, should have bitCount bits set to 1, potentially anywhere in the long
     */
    public long approximateBits(int bitCount) {
        if (bitCount <= 0)
            return 0L;
        if (bitCount >= 64)
            return -1L;
        if (bitCount == 32)
            return nextLong();
        boolean high = bitCount > 32;
        int altered = (high ? 64 - bitCount : bitCount), lsb = altered & ~(altered - 1); // lowestOneBit, GWT-compatible
        long data = nextLong();
        for (int i = lsb << 1; i <= 16; i <<= 1) {
            if ((altered & i) == 0)
                data &= nextLong();
            else
                data |= nextLong();
        }
        return high ? ~(nextLong() & data) : (nextLong() & data);
    }

    /**
     * Gets a somewhat-random long with exactly 32 bits set; in each pair of bits starting at bit 0 and bit 1, then bit
     * 2 and bit 3, up to bit 62 and bit 3, one bit will be 1 and one bit will be 0 in each pair.
     * <br>
     * Not exactly general-use; meant for generating data for GreasedRegion.
     * @return a random long with 32 "1" bits, distributed so exactly one bit is "1" for each pair of bits
     */
    public long randomInterleave() {
        long bits = nextLong() & 0xFFFFFFFFL, ib = ~bits & 0xFFFFFFFFL;
        bits |= (bits << 16);
        ib |= (ib << 16);
        bits &= 0x0000FFFF0000FFFFL;
        ib &= 0x0000FFFF0000FFFFL;
        bits |= (bits << 8);
        ib |= (ib << 8);
        bits &= 0x00FF00FF00FF00FFL;
        ib &= 0x00FF00FF00FF00FFL;
        bits |= (bits << 4);
        ib |= (ib << 4);
        bits &= 0x0F0F0F0F0F0F0F0FL;
        ib &= 0x0F0F0F0F0F0F0F0FL;
        bits |= (bits << 2);
        ib |= (ib << 2);
        bits &= 0x3333333333333333L;
        ib &= 0x3333333333333333L;
        bits |= (bits << 1);
        ib |= (ib << 1);
        bits &= 0x5555555555555555L;
        ib &= 0x5555555555555555L;
        return (bits | (ib << 1));
    }

    /**
     * Gets the minimum random long between 0 and {@code bound} generated out of {@code trials} generated numbers.
     * Useful for when numbers should have a strong bias toward zero, but all possible values are between 0, inclusive,
     * and bound, exclusive.
     * @param bound the outer exclusive bound
     * @param trials how many numbers to generate and get the minimum of
     * @return the minimum generated long between 0 and bound out of the specified amount of trials
     */
    public long minLongOf(final long bound, final int trials)
    {
        long value = nextLong(bound);
        for (int i = 1; i < trials; i++) {
            value = Math.min(value, nextLong(bound));
        }
        return value;
    }
    /**
     * Gets the maximum random long between 0 and {@code bound} generated out of {@code trials} generated numbers.
     * Useful for when numbers should have a strong bias away from zero, but all possible values are between 0,
     * inclusive, and bound, exclusive.
     * @param bound the outer exclusive bound
     * @param trials how many numbers to generate and get the maximum of
     * @return the maximum generated long between 0 and bound out of the specified amount of trials
     */
    public long maxLongOf(final long bound, final int trials)
    {
        long value = nextLong(bound);
        for (int i = 1; i < trials; i++) {
            value = Math.max(value, nextLong(bound));
        }
        return value;
    }

    /**
     * Gets the minimum random int between 0 and {@code bound} generated out of {@code trials} generated numbers.
     * Useful for when numbers should have a strong bias toward zero, but all possible values are between 0, inclusive,
     * and bound, exclusive.
     * @param bound the outer exclusive bound
     * @param trials how many numbers to generate and get the minimum of
     * @return the minimum generated int between 0 and bound out of the specified amount of trials
     */
    public int minIntOf(final int bound, final int trials)
    {
        int value = nextInt(bound);
        for (int i = 1; i < trials; i++) {
            value = Math.min(value, nextInt(bound));
        }
        return value;
    }
    /**
     * Gets the maximum random int between 0 and {@code bound} generated out of {@code trials} generated numbers.
     * Useful for when numbers should have a strong bias away from zero, but all possible values are between 0,
     * inclusive, and bound, exclusive.
     * @param bound the outer exclusive bound
     * @param trials how many numbers to generate and get the maximum of
     * @return the maximum generated int between 0 and bound out of the specified amount of trials
     */
    public int maxIntOf(final int bound, final int trials)
    {
        int value = nextInt(bound);
        for (int i = 1; i < trials; i++) {
            value = Math.max(value, nextInt(bound));
        }
        return value;
    }

    /**
     * Gets the minimum random double between 0 and {@code bound} generated out of {@code trials} generated numbers.
     * Useful for when numbers should have a strong bias toward zero, but all possible values are between 0, inclusive,
     * and bound, exclusive.
     * @param bound the outer exclusive bound
     * @param trials how many numbers to generate and get the minimum of
     * @return the minimum generated double between 0 and bound out of the specified amount of trials
     */
    public double minDoubleOf(final double bound, final int trials)
    {
        double value = nextDouble(bound);
        for (int i = 1; i < trials; i++) {
            value = Math.min(value, nextDouble(bound));
        }
        return value;
    }

    /**
     * Gets the maximum random double between 0 and {@code bound} generated out of {@code trials} generated numbers.
     * Useful for when numbers should have a strong bias away from zero, but all possible values are between 0,
     * inclusive, and bound, exclusive.
     * @param bound the outer exclusive bound
     * @param trials how many numbers to generate and get the maximum of
     * @return the maximum generated double between 0 and bound out of the specified amount of trials
     */
    public double maxDoubleOf(final double bound, final int trials)
    {
        double value = nextDouble(bound);
        for (int i = 1; i < trials; i++) {
            value = Math.max(value, nextDouble(bound));
        }
        return value;
    }
    /**
     * Gets the minimum random float between 0 and {@code bound} generated out of {@code trials} generated numbers.
     * Useful for when numbers should have a strong bias toward zero, but all possible values are between 0, inclusive,
     * and bound, exclusive.
     * @param bound the outer exclusive bound
     * @param trials how many numbers to generate and get the minimum of
     * @return the minimum generated float between 0 and bound out of the specified amount of trials
     */
    public float minFloatOf(final float bound, final int trials)
    {
        float value = nextFloat(bound);
        for (int i = 1; i < trials; i++) {
            value = Math.min(value, nextFloat(bound));
        }
        return value;
    }

    /**
     * Gets the maximum random float between 0 and {@code bound} generated out of {@code trials} generated numbers.
     * Useful for when numbers should have a strong bias away from zero, but all possible values are between 0,
     * inclusive, and bound, exclusive.
     * @param bound the outer exclusive bound
     * @param trials how many numbers to generate and get the maximum of
     * @return the maximum generated float between 0 and bound out of the specified amount of trials
     */
    public float maxFloatOf(final float bound, final int trials)
    {
        float value = nextFloat(bound);
        for (int i = 1; i < trials; i++) {
            value = Math.max(value, nextFloat(bound));
        }
        return value;
    }


    @Override
    public String toString() {
        return "RNG with state " + state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RNG)) return false;

        RNG rng = (RNG) o;
        return state == rng.state;
    }

    @Override
    public int hashCode() {
        return (int)(state ^ state >>> 32);
    }

    /**
     * Gets a random portion of data (an array), assigns that portion to output (an array) so that it fills as much as
     * it can, and then returns output. Will only use a given position in the given data at most once; does this by
     * generating random indices for data's elements, but only as much as needed, assigning the copied section to output
     * and not modifying data.
     * <br>
     * Based on http://stackoverflow.com/a/21460179 , credit to Vincent van der Weele; modifications were made to avoid
     * copying or creating a new generic array (a problem on GWT).
     *
     * @param data   an array of T; will not be modified.
     * @param output an array of T that will be overwritten; should always be instantiated with the portion length
     * @param <T>    can be any non-primitive type.
     * @return an array of T that has length equal to output's length and may contain unchanged elements (null if output
     * was empty) if data is shorter than output
     */
    public <T> T[] randomPortion(T[] data, T[] output) {
        int length = data.length;
        final int n = Math.min(length, output.length);
        int[] mapping = new int[n];
        for (int i = 0; i < n; i++) {
            mapping[i] = i;
        }
        for (int i = 0; i < n; i++) {
            int r = nextInt(length);
            output[i] = data[mapping[r]];
            mapping[r] = mapping[--length];
        }

        return output;
    }

    /**
     * Gets a random GridPoint2 that has x between 0 (inclusive) and width (exclusive) and y between 0 (inclusive)
     * and height (exclusive). This makes one call to randomLong to generate (more than) 31 random bits for
     * each axis, and should be very fast. If width and height are very large, greater than 100,000 for either,
     * this particular method may show bias toward certain positions due to the "hasty" technique used to reduce
     * the random numbers to the given size, but because most maps in tile-based games are relatively small, this
     * technique should be fine. This allows negative values for width and/or height if you want x and/or y to be
     * negative; the coordinates will still be between 0 and width or height.
     * <br>
     * Credit goes to Daniel Lemire, http://lemire.me/blog/2016/06/27/a-fast-alternative-to-the-modulo-reduction/
     *
     * @param width  the upper bound (exclusive) for x coordinates
     * @param height the upper bound (exclusive) for y coordinates
     * @return a random GridPoint2 between (0,0) inclusive and (width,height) exclusive
     */
    public GridPoint2 nextPoint(int width, int height) {
        final long n = nextLong();
        return new GridPoint2((int) ((width * (n >>> 33)) >> 31), (int) ((height * (n & 0x7FFFFFFFL)) >> 31));
    }

//    public static void main(String[] args)
//    {
//        RNG rng = new RNG(10L);
//        long out1 = rng.nextLong();
//        long inv1 = inverseNextLong(out1);
//        rng.nextLong();
//        long out2 = rng.nextLong();
//        long inv2 = inverseNextLong(out2);
//        rng.nextLong();
//        rng.setSeed(inv2);
//        System.out.printf("Inverse 1: 0x%016X, Inverse 2: 0x%016X, distance: 0x%016X, equal: %b\n", inv1, inv2,distance(out1, out2), out2 == rng.nextLong());
//    }
}
