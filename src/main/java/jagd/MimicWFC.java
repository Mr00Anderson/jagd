/*
The MIT License(MIT)
Copyright(c) mxgmn 2016, modified by Tommy Ettinger 2018
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
The software is provided "as is", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose and noninfringement. In no event shall the authors or copyright holders be liable for any claim, damages or other liability, whether in an action of contract, tort or otherwise, arising from, out of or in connection with the software or the use or other dealings in the software.
*/

package jagd;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ObjectIntMap;

/**
 * A port of WaveFunctionCollapse by ExUtumno/mxgmn; takes a single sample of a grid to imitate and produces one or more
 * grids of requested sizes that have a similar layout of cells to the sample. Samples are given as {@code int[][]}
 * where an int is usually an index into an array, list, {@link IndexedSet}, {@link IndexedMap}, or some similar
 * indexed collection of items (such as char values or colors) that would be used instead of an int directly. The
 * original WaveFunctionCollapse code, <a href="https://github.com/mxgmn/WaveFunctionCollapse">here</a>, used colors in
 * bitmap images, but this uses 2D int arrays that can stand as substitutes for colors or chars.
 * <br>
 * Created by Tommy Ettinger on 3/28/2018. Port of https://github.com/mxgmn/WaveFunctionCollapse
 * Brought into Jagd on 2/2/2019 by Tommy Ettinger.
 */
public class MimicWFC {
    private boolean[][] wave;

    private int[][][] propagator;
    private int[][][] compatible;
    private int[] observed;

    private int[] stack;
    private int stacksize;

    public RNG random;
    private int FMX, FMY, totalOptions;
    private boolean periodic;

    private IntArray baseWeights;
    private double[] weightLogWeights;

    private int[] sumsOfOnes;
    private double sumOfWeights, sumOfWeightLogWeights, startingEntropy;
    private double[] sumsOfWeights, sumsOfWeightLogWeights, entropies;


    private int order;
    private Array<IntArray> patterns;
    private IntIntMap choices, revChoices;
    private Integer surround;

    /**
     * Constructs a MimicWFC that will imitate a given 2D int array. The order should usually be 2, the width and height
     * do not need to correspond to the dimensions of itemGrid, and unless you have a particular usage in mind,
     * periodicInput and periodicOutput are usually both false. You should usually use 1 for symmetry, and some
     * kinds of items this could mimic might not work at all with other symmetry values.
     * @param itemGrid the grid to imitate, as a 2D int array that should be rectangular
     * @param order the size of the nearby area to analyze per cell; usually 2, but may be higher for some itemGrids
     * @param width the width of the output area to generate
     * @param height the height of the output area to generate
     * @param periodicInput usually false, but may be set to true if the input already tiles seamlessly
     * @param periodicOutput true if the output should tile seamlessly on x and y, or false otherwise (true can be slow)
     * @param symmetry must be greater than 0, and is usually 1, but can be as high as 8
     */
    public MimicWFC(int[][] itemGrid, int order, int width, int height, boolean periodicInput, boolean periodicOutput, int symmetry)
    {
        this(itemGrid, order, width, height, periodicInput, periodicOutput, symmetry, null);
    }
    /**
     * Constructs a MimicWFC that will imitate a given 2D int array. The order should usually be 2, the width and height
     * do not need to correspond to the dimensions of itemGrid, and unless you have a particular usage in mind,
     * periodicInput and periodicOutput are usually both false. You should usually use 1 for symmetry, and some
     * kinds of items this could mimic might not work at all with other symmetry values. If surround is true (but in
     * most usage it should be false), the square of items in the upper left corner of itemGrid (which will be order
     * wide and order high) will be duplicated over the whole border of the result. This allows maps to be always
     * islands if the corner is water, and allows textures for artwork to always have a frame. If using surround, the
     * block of items in the corner must be allowed to have itself as a neighbor in all directions (e.g. when order is
     * 2, a 2x2 block of identical items that can connect without issues, such as a solid color or something referring
     * to a seamlessly tiling texture).
     * @param itemGrid the grid to imitate, as a 2D int array that should be rectangular
     * @param order the size of the nearby area to analyze per cell; usually 2, but may be higher for some itemGrids
     * @param width the width of the output area to generate
     * @param height the height of the output area to generate
     * @param periodicInput usually false, but may be set to true if the input already tiles seamlessly
     * @param periodicOutput true if the output should tile seamlessly on x and y, or false otherwise (true can be slow)
     * @param symmetry must be greater than 0, and is usually 1, but can be as high as 8
     * @param surround if true, the item in the bottom right corner will enclose the output (effectively forcing
     *                 periodic output); the map will be generated normally if false
     */
    public MimicWFC(int[][] itemGrid, int order, int width, int height, boolean periodicInput, boolean periodicOutput, int symmetry, boolean surround)
    {
        this(itemGrid, order, width, height, periodicInput, periodicOutput, symmetry, surround ? itemGrid[0][0] : null);
    }

    /**
     * Constructs a MimicWFC that will imitate a given 2D int array. The order should usually be 2, the width and height
     * do not need to correspond to the dimensions of itemGrid, and unless you have a particular usage in mind,
     * periodicInput, periodicOutput, and ground are usually all false. You should usually use 1 for symmetry, and some
     * kinds of items this could mimic might not work at all with other symmetry values. If surround is true, the square
     * of items in the bottom right corner of itemGrid (which will be order wide and order high) will be duplicated over
     * the whole border of the result. This allows maps to be always islands if the corner is water, and allows textures
     * for artwork to always have a frame. If using surround, the block of items in the corner must be allowed to have
     * itself as a neighbor in all directions (e.g. when order is 2, a 2x2 block of identical items that can connect
     * without issues, such as a solid color or something referring to a seamlessly tiling texture). If surround is
     * non-null, then it must appear somewhere in itemGrid so neighbor cells can be placed next to the border (otherwise
     * the map will be purely the border item).
     * @param itemGrid the grid to imitate, as a 2D int array that should be rectangular
     * @param order the size of the nearby area to analyze per cell; usually 2, but may be higher for some itemGrids
     * @param width the width of the output area to generate
     * @param height the height of the output area to generate
     * @param periodicInput usually false, but may be set to true if the input already tiles seamlessly
     * @param periodicOutput true if the output should tile seamlessly on x and y, or false otherwise (true can be slow)
     * @param symmetry must be greater than 0, and is usually 1, but can be as high as 8
     * @param surround if null, the map will be generated normally; if any int value, this will wrap the map's borders
     *                 in that int value (which is often in the same range of int used in itemGrid). Note that 0 will
     *                 wrap the map in the value 0, which may be different from what is intended.
     */
    public MimicWFC(int[][] itemGrid, int order, int width, int height, boolean periodicInput, boolean periodicOutput,
                    int symmetry, Integer surround)
    {
        FMX = width;
        FMY = height;

        this.order = order;
        periodic = periodicOutput;

        int SMX = itemGrid.length, SMY = itemGrid[0].length;
        choices = new IntIntMap(SMX * SMY);
        revChoices = new IntIntMap(SMX * SMY);
        int[][] sample = new int[SMX][SMY];
        for (int y = 0; y < SMY; y++) {
            for (int x = 0; x < SMX; x++)
            {
                int color = itemGrid[x][y];
                int i = choices.get(color, -1);
                if(i == -1) {
                    i = choices.size;
                    choices.put(color, i);
                    revChoices.put(i, color);
                }
                sample[x][y] = i;
            }
        }

        final int yLimit = (periodicInput ? SMY : SMY - order + 1);
        final int xLimit = (periodicInput ? SMX : SMX - order + 1);
        ObjectIntMap<IntArray> weights = new ObjectIntMap<IntArray>(yLimit * xLimit);
        IntArray[] ps = new IntArray[8];
        for (int y = 0; y < yLimit; y++) {
            for (int x = 0; x < xLimit; x++) {

                ps[0] = patternFromSample(x, y, sample, SMX, SMY);
                ps[1] = reflect(ps[0]);
                ps[2] = rotate(ps[0]);
                ps[3] = reflect(ps[2]);
                ps[4] = rotate(ps[2]);
                ps[5] = reflect(ps[4]);
                ps[6] = rotate(ps[4]);
                ps[7] = reflect(ps[6]);

                for (int k = 0; k < symmetry; k++) {
                    IntArray ind = ps[k];
                    weights.getAndIncrement(ind, 0, 1);
//                    Integer wt = weights.get(ind);
//                    if (wt != null) weights.put(ind, wt + 1);
//                    else {
//                        weights.put(ind, 1);
//                    }
                }
            }
        }
        IntArray pat = null;
        if(surround != null)
        {
            pat = new IntArray(order * order);
            int surr = choices.get(surround, 0);
            for (int dy = 0; dy < order; dy++) {
                for (int dx = 0; dx < order; dx++) {
                    pat.add(surr);
                }
            }
            weights.getAndIncrement(pat, 0, 2);
        }
        else
            this.surround = null;

        totalOptions = weights.size;
        patterns = weights.keys().toArray();//new int[totalOptions][];
        baseWeights = weights.values().toArray();// new double[totalOptions];
        if(pat != null)
        {
            this.surround = patterns.indexOf(pat, false);
            baseWeights.set(this.surround, 1);
        }


        propagator = new int[4][][];
        IntArray list = new IntArray(totalOptions);
        for (int d = 0; d < 4; d++)
        {
            propagator[d] = new int[totalOptions][];
            for (int t = 0; t < totalOptions; t++)
            {
                list.clear();
                for (int t2 = 0; t2 < totalOptions; t2++)
                {
                    if (agrees(patterns.get(t), patterns.get(t2), DX[d], DY[d]))
                        list.add(t2);
                }
                propagator[d][t] = list.toArray();
            }
        }
    }

//    private long index(byte[] p, long C)
//    {
//        long result = 0, power = 1;
//        for (int i = 0; i < p.length; i++)
//        {
//            result += p[p.length - 1 - i] * power;
//            power *= C;
//        }
//        return result;
//    }
//
//    private byte[] patternFromIndex(long ind, long power, long C)
//    {
//        long residue = ind;
//        byte[] result = new byte[order * order];
//
//        for (int i = 0; i < result.length; i++)
//        {
//            power /= C;
//            int count = 0;
//
//            while (residue >= power)
//            {
//                residue -= power;
//                count++;
//            }
//
//            result[i] = (byte)count;
//        }
//
//        return result;
//    }

//    int[] pattern (Func<int, int, byte> f)
//    {
//        int[] result = new int[order * order];
//        for (int y = 0; y < order; y++) {
//            for (int x = 0; x < order; x++){
//                result[x + y * order] = f(x, y);
//            }
//        }
//        return result;
//    }

    private IntArray patternFromSample(int x, int y, int[][] sample, int SMX, int SMY) {
        IntArray result = new IntArray(order * order);
        for (int dy = 0; dy < order; dy++) {
            for (int dx = 0; dx < order; dx++) {
                result.add(sample[(x + dx) % SMX][(y + dy) % SMY]);
            }
        }
        return result;
    }
    private IntArray rotate(IntArray p)
    {
        IntArray result = new IntArray(order * order);
        for (int y = 0; y < order; y++) {
            for (int x = 0; x < order; x++){
                result.add(p.get(order - 1 - y + x * order));
            }
        }
        return result;
    }
    private IntArray reflect(IntArray p)
    {
        IntArray result = new IntArray(order * order);
        for (int y = 0; y < order; y++) {
            for (int x = 0; x < order; x++){
                result.add(p.get(order - 1 - x + y * order));
            }
        }
        return result;
    }
    private boolean agrees(IntArray p1, IntArray p2, int dx, int dy)
    {
        int xmin = dx < 0 ? 0 : dx, xmax = dx < 0 ? dx + order : order,
                ymin = dy < 0 ? 0 : dy, ymax = dy < 0 ? dy + order : order;
        for (int y = ymin; y < ymax; y++) {
            for (int x = xmin; x < xmax; x++) {
                if (p1.get(x + order * y) != p2.get(x - dx + order * (y - dy)))
                    return false;
            }
        }
        return true;
    }

    private void init()
    {
        wave = new boolean[FMX * FMY][];
        compatible = new int[wave.length][][];
        for (int i = 0; i < wave.length; i++)
        {
            wave[i] = new boolean[totalOptions];
            compatible[i] = new int[totalOptions][];
            for (int t = 0; t < totalOptions; t++) compatible[i][t] = new int[4];
        }

        weightLogWeights = new double[totalOptions];
        sumOfWeights = 0;
        sumOfWeightLogWeights = 0;

        for (int t = 0; t < totalOptions; t++)
        {
            final double bw = baseWeights.get(t);
            weightLogWeights[t] = bw * Math.log(bw);
            sumOfWeights += bw;
            sumOfWeightLogWeights += weightLogWeights[t];
        }

        startingEntropy = Math.log(sumOfWeights) - sumOfWeightLogWeights / sumOfWeights;

        sumsOfOnes = new int[FMX * FMY];
        sumsOfWeights = new double[FMX * FMY];
        sumsOfWeightLogWeights = new double[FMX * FMY];
        entropies = new double[FMX * FMY];

        stack = new int[wave.length * totalOptions << 1];
        stacksize = 0;
    }

    private Boolean observe()
    {
        double min = 1E+3;
        int argmin = -1;

        for (int i = 0; i < wave.length; i++)
        {
            if (onBoundary(i % FMX, i / FMX)) continue;

            int amount = sumsOfOnes[i];
            if (amount == 0) return false;

            double entropy = entropies[i];
            if (amount > 1 && entropy <= min)
            {
                double noise = 1E-6 * random.nextDouble();
                if (entropy + noise < min)
                {
                    min = entropy + noise;
                    argmin = i;
                }
            }
        }

        if (argmin == -1)
        {
            observed = new int[FMX * FMY];
            for (int i = 0; i < wave.length; i++) {
                for (int t = 0; t < totalOptions; t++) {
                    if (wave[i][t]) {
                        observed[i] = t;
                        break;
                    }
                }
            }
            return true;
        }

        double[] distribution = new double[totalOptions];
        double sum = 0.0, x = 0.0;
        for (int t = 0; t < totalOptions; t++)
        {
            sum += (distribution[t] = wave[argmin][t] ? baseWeights.get(t) : 0);
        }
        int r = 0;
        sum *= random.nextDouble();
        for (; r < totalOptions; r++) {
            if((x += distribution[r]) > sum)
                break;
        }

        boolean[] w = wave[argmin];
        for (int t = 0; t < totalOptions; t++){
            if (w[t] != (t == r))
                ban(argmin, t);
        }

        return null;
    }

    private void propagate()
    {
        while (stacksize > 0)
        {
            int i1 = stack[stacksize - 2], e2 = stack[stacksize - 1];
            stacksize -= 2;
            int x1 = i1 % FMX, y1 = i1 / FMX;

            for (int d = 0; d < 4; d++)
            {
                int dx = DX[d], dy = DY[d];
                int x2 = x1 + dx, y2 = y1 + dy;
                if (onBoundary(x2, y2)) continue;

                if (x2 < 0) x2 += FMX;
                else if (x2 >= FMX) x2 -= FMX;
                if (y2 < 0) y2 += FMY;
                else if (y2 >= FMY) y2 -= FMY;

                int i2 = x2 + y2 * FMX;
                int[] p = propagator[d][e2];
                int[][] compat = compatible[i2];

                for (int l = 0; l < p.length; l++)
                {
                    int t2 = p[l];
                    int[] comp = compat[t2];

                    comp[d]--;
                    if (comp[d] == 0) ban(i2, t2);
                }
            }
        }
    }

    public boolean run(long seed, int limit)
    {
        if (wave == null) init();

        clear();
        random = new RNG(seed);

        for (int l = 0; l < limit || limit == 0; l++)
        {
            Boolean result = observe();
            if (result != null) return result;
            propagate();
        }

        return false;
    }

    public boolean run(RNG rng, int limit)
    {
        if (wave == null) init();

        clear();
        random = rng;
        for (int l = 0; l < limit || limit == 0; l++)
        {
            Boolean result = observe();
            if (result != null) return result;
            propagate();
        }

        return false;
    }

    private void ban(int i, int t)
    {
        wave[i][t] = false;

        int[] comp = compatible[i][t];
        for (int d = 0; d < 4; d++) comp[d] = 0;
        stack[stacksize++] = i;
        stack[stacksize++] = t;

        double sum = sumsOfWeights[i];
        entropies[i] += sumsOfWeightLogWeights[i] / sum - Math.log(sum);

        sumsOfOnes[i] -= 1;
        sumsOfWeights[i] -= baseWeights.get(t);
        sumsOfWeightLogWeights[i] -= weightLogWeights[t];

        sum = sumsOfWeights[i];
        entropies[i] -= sumsOfWeightLogWeights[i] / sum - Math.log(sum);
    }


    private boolean onBoundary(int x, int y) {
        return !periodic && (x + order > FMX || y + order > FMY || x < 0 || y < 0);
    }

    public int[][] result()
    {
        int[][] result = new int[FMX][FMY];

        if (observed != null)
        {
            for (int y = 0; y < FMY; y++)
            {
                int dy = y < FMY - order + 1 ? 0 : order - 1;
                for (int x = 0; x < FMX; x++)
                {
                    int dx = x < FMX - order + 1 ? 0 : order - 1;
                    result[x][y] = revChoices.get(patterns.get(observed[x - dx + (y - dy) * FMX]).get(dx + dy * order), 0);
                }
            }
        }
        return result;
    }

    private void clear()
    {
        for (int i = 0; i < wave.length; i++)
        {
            for (int t = 0; t < totalOptions; t++)
            {
                wave[i][t] = true;
                for (int d = 0; d < 4; d++) compatible[i][t][d] = propagator[OPPOSITE[d]][t].length;
            }

            sumsOfOnes[i] = baseWeights.size;
            sumsOfWeights[i] = sumOfWeights;
            sumsOfWeightLogWeights[i] = sumOfWeightLogWeights;
            entropies[i] = startingEntropy;
        }


        if (surround != null)
        {
            final int g = surround;
            for (int x = 0; x < FMX; x++)
            {
                for (int t = 0; t < totalOptions; t++) {
                    if (t != g)
                    {
                        ban(x, t);
                    }
                    if (t != g)
                    {
                        ban(x + (FMY - 1) * FMX, t);
                    }
                }
            }
            for (int y = 1; y < FMY - 1; y++)
            {
                for (int t = 0; t < totalOptions; t++) {
                    if (t != g)
                    {
                        ban(y * FMX, t);
                    }
                    if(t != g)
                    {
                        ban(FMX - 1 + y * FMX, t);
                    }
                }
            }
            propagate();
        }
    }
    private static int[] DX = { -1, 0, 1, 0 };
    private static int[] DY = { 0, 1, 0, -1 };
    private static int[] OPPOSITE = { 2, 3, 0, 1 };
}
