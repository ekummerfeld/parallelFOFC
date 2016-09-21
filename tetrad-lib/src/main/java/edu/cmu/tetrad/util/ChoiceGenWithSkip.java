///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.util;


import edu.cmu.tetrad.util.ChoiceGenerator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static edu.cmu.tetrad.util.ProbUtils.lngamma;
import static java.lang.Math.exp;
import static java.lang.Math.round;

/**
 * Generates (nonrecursively) all of the combinations of a choose b, where a, b
 * are nonnegative integers and a >= b.  The values of a and b are given in the
 * constructor, and the sequence of choices is obtained by repeatedly calling
 * the next() method.  When the sequence is finished, null is returned.</p> </p>
 * <p>A valid combination for the sequence of combinations for a choose b
 * generated by this class is an array x[] of b integers i, 0 <= i < a, such
 * that x[j] < x[j + 1] for each j from 0 to b - 1.
 * <p>
 * To see what this class does, try calling ChoiceGenerator.testPrint(5, 3), for
 * instance.
 *
 * Method for skipping ahead to later sections of the ChoiceGenerator made by Erich Kummerfeld
 * IMPORTANT NOTE!!!!! This class is made primarily for parallelizing FOFC so that it can run on
 * large data sets with e.g. 20 thousand variables. As such it is only appropriate for those settings
 * It also uses some sub methods that are particular to that application, such as special ways of
 * calculating n choose 2 and n choose 3 specifically.
 *
 * @author Joseph Ramsey
 * @author Erich Kummerfeld
 */
@SuppressWarnings({"WeakerAccess"})
public final class ChoiceGenWithSkip {

    /**
     * The number of objects being selected from.
     */
    private int a;

    /**
     * The number of objects in the desired selection.
     */
    private int b;

    /**
     * The difference between a and b (should be nonnegative).
     */
    private int diff;

    /**
     * The internally stored choice.
     */
    private int[] choiceLocal;

    /**
     * The choice that is returned. Used, since the returned array can be
     * modified by the user.
     */
    private int[] choiceReturned;

    /**
     * Indicates whether the next() method has been called since the last
     * initialization.
     */
    private boolean begun;

    /**
     * Constructs a new choice generator for a choose b. Once this
     * initialization has been performed, successive calls to next() will
     * produce the series of combinations.  To begin a new series at any time,
     * call this init method again with new values for a and b.
     *
     * @param a the number of objects being selected from.
     * @param b the number of objects in the desired selection.
     */
    public ChoiceGenWithSkip(int a, int b) {
        if ((a < 0) || (b < 0) || (a < b)) {
            throw new IllegalArgumentException(
                    "For 'a choose b', a and b must be " +
                            "nonnegative with a >= b: " + "a = " + a +
                            ", b = " + b);
        }

        this.a = a;
        this.b = b;
        choiceLocal = new int[b];
        choiceReturned = new int[b];
        diff = a - b;

        // Initialize the choice array with successive integers [0 1 2 ...].
        // Set the value at the last index one less than it would be in such
        // a series, ([0 1 2 ... b - 2]) so that on the first call to next()
        // the first combination ([0 1 2 ... b - 1]) is returned correctly.
        for (int i = 0; i < b - 1; i++) {
            choiceLocal[i] = i;
        }

        if (b > 0) {
            choiceLocal[b - 1] = b - 2;
        }

        begun = false;
    }

    /**
     * @return the next combination in the series, or null if the series is
     * finished.
     */
    public synchronized int[] next() {
        int i = getB();

        // Scan from the right for the first index whose value is less than
        // its expected maximum (i + diff) and perform the fill() operation
        // at that index.
        while (--i > -1) {
            if (this.choiceLocal[i] < i + this.diff) {
                fill(i);
                begun = true;
                System.arraycopy(choiceLocal, 0, choiceReturned, 0, b);
                return choiceReturned;
            }
        }

        if (this.begun) {
            return null;
        } else {
            begun = true;
            System.arraycopy(choiceLocal, 0, choiceReturned, 0, b);
            return choiceReturned;
        }
    }

    /**
     * This static method will print the series of combinations for a choose b
     * to System.out.
     *
     * @param a the number of objects being selected from.
     * @param b the number of objects in the desired selection.
     */
    @SuppressWarnings({"SameParameterValue"})
    public static void testPrint(int a, int b) {
        ChoiceGenerator cg = new ChoiceGenerator(a, b);
        int[] choice;

        System.out.println();
        System.out.println(
                "Printing combinations for " + a + " choose " + b + ":");
        System.out.println();

        while ((choice = cg.next()) != null) {
            if (choice.length == 0) {
                System.out.println("zero-length array");
            } else {
                for (int aChoice : choice) {
                    System.out.print(aChoice + "\t");
                }

                System.out.println();
            }
        }

        System.out.println();
    }

    /**
     * @return Ibid.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public int getA() {
        return this.a;
    }

    /**
     * @return Ibid.
     */
    public int getB() {
        return this.b;
    }

    /**
     * Fills the 'choice' array, from index 'index' to the end of the array,
     * with successive integers starting with choice[index] + 1.
     *
     * @param index the index to begin this incrementing operation.
     */
    private void fill(int index) {
        this.choiceLocal[index]++;

        for (int i = index + 1; i < getB(); i++) {
            this.choiceLocal[i] = this.choiceLocal[i - 1] + 1;
        }
    }

    public static int getNumCombinations(int a, int b) {
        return (int) round(exp(lngamma(a + 1) - lngamma(b + 1) - lngamma((a - b) + 1)));
    }

    //getNumCombinations can't handle extremely large a, e.g. 20000 choose 3 will overflow the int
    //So I'm doing this calculation with BigDecimal, right now for special cases b=2 and b=3
    //-Erich Kummerfeld
    public static BigDecimal getBigNumComb(int a, int b){
        BigDecimal n = new BigDecimal(a);
        if ( !(b==2||b==3)){
            throw new IllegalArgumentException("getBigNumComb only works for b=2 or b=3");
        }
        if (b==2){
            //just calculate the closed form for n choose 2
            return n.multiply(n.subtract(new BigDecimal(1))).multiply(new BigDecimal(0.5));
        }
        if (b==3){
            return n.multiply(n.subtract(new BigDecimal(1)))
                    .multiply(n.subtract(new BigDecimal(2)))
                    .divide(new BigDecimal(6));
        }
        //if we've reached this point without returning a value, something weird happened
        throw new IllegalArgumentException("something weird happened??");
    }

    /**
     * This method skips ahead to the choiceLocal that would occur a fixed number of next() calls later.
     * When a choose b becomes large, using this method is much cheaper than performing skip() many times.
     *
     * IMPORTANT NOTE!!!! right now, this only works for the specific case where b=3 and a>6.
     */
    public void skip(BigDecimal n) {
        //check that b=3 and a>6
        if (b!=3 || a<7){
            throw new IllegalArgumentException("this method requires that b=3 and n>6");
        }
        //having the num combinations is useful, since I'm going to be counting backwards
        BigDecimal numComb = getBigNumComb(a,b);
        //System.out.println("numComb: "+numComb);
        //some sanity checks that the method input is neither too small nor too large
        if (n.compareTo(new BigDecimal(1))==-1 || n.compareTo(numComb)==1){
            throw new IllegalArgumentException("n must be positive and smaller than a choose b");
        }
        //calculate the first int in the choiceLocal array
        int firstint = calcFirstInt(n, numComb);
        choiceLocal[0]=firstint;
        //same for the second int
        int secondint = calcSecondInt(n,numComb);
        choiceLocal[1]=secondint;
        //third int
        int thirdint = calcThirdInt(n,numComb);
        choiceLocal[2]=thirdint;
    }

    //need a public methods for setting and getting the value of choiceLocal
    public void setChoiceLocal(int[] newchoice){
        //check that newchoice is the correct size
        if (newchoice.length != choiceLocal.length){
            throw new IllegalArgumentException("input must match length of choiceLocal");
        }
        choiceLocal=newchoice;
    }
    public int[] getChoiceLocal(){
        return choiceLocal;
    }


    //these methods are used by skip to calculate the values of the new choice
    private int calcFirstInt(BigDecimal n, BigDecimal numComb){
        //used for the BigFunctions calculations
        final int scale = 100;
        //k is the count to reach n from the end of the choicegen
        BigDecimal k = numComb.subtract(n);
        //System.out.println("k: "+k);
        //the next calculation is done in two parts, since q is used twice
        //this is an approximate solution for solving k=(n choose 3) for n
        /*double test1=Math.pow((243*Math.pow(k,2)-1),0.5); //testing purposes only!
        System.out.println("test1: "+test1);
        double test2=(Math.pow(3,.5)*test1+27*k);
        System.out.println("test2: "+test2);
        double test3=Math.pow(3,.5)*test1;
        System.out.println("test3: "+test3);
        double test27timesk=27*k;
        System.out.println("test27timesk: "+test27timesk);
        */

        BigDecimal testbd = BigFunctions.pow(
                (
                (new BigDecimal(243))
                        .multiply(BigFunctions.pow(k,new BigDecimal(2),scale))
                )
                        .subtract(new BigDecimal(1))
                ,new BigDecimal(0.5),scale);

        BigDecimal q = BigFunctions.pow(
                (
                        (new BigDecimal(Math.pow(3,.5))).multiply(testbd)
                )
                        .add(k.multiply(new BigDecimal(27)))
                ,new BigDecimal(1.0/3.0),scale);

        BigDecimal nCountBD = ((new BigDecimal(Math.pow(3,-(2.0/3.0)))).multiply(q))
                .add(
                        (new BigDecimal(Math.pow(3,-(1.0/3.0)),new MathContext(16)))
                                .divide(q,scale,RoundingMode.CEILING)
                ).add(new BigDecimal(1));
        //double q2=q.doubleValue();
        //double test1=Math.pow((243*Math.pow(k,2)-1),0.5);
        //double q = Math.pow((Math.pow(3,.5)*test1+27*k),1.0/3.0);
        //double q = Math.pow((Math.pow(3,.5)*Math.pow((243*Math.pow(k,2)-1),0.5)+27*k),1.0/3.0);
        //System.out.println("q: "+q);
        //double nCountDouble = Math.pow(3,-(2.0/3.0))*q2+Math.pow(3,-(1.0/3.0))/q2+1;
        //System.out.println("nCountDouble: "+nCountDouble);
        /**
         * double q = Math.pow((1.7321*Math.pow((243*Math.pow(k,2)-1),0.50000)+27*k),0.33333);
         double nCountDouble = 0.48075*q+0.69336/q+1;
         *
         */
        int nCount = nCountBD.setScale(0, RoundingMode.CEILING).intValue();
        //System.out.println("nCount: "+nCount);
        //int nCount = (int) Math.ceil(nCountDouble); //ceil to compensate for flooring and backcounting
        //nCount = Math.max(nCount,3); //this is just to make sure nCount is at least 2
        //we count downwards from a to get our value
        int value = a-nCount;
        return value;
    }
    private int calcSecondInt(BigDecimal n, BigDecimal numComb){
        final int scale = 100;
        BigDecimal k = numComb.subtract(n);
        //use firstInt to determine the parameter of our n choose 2 sub-choice
        int a2 = a-choiceLocal[0]-1;
        //the sub problem is a2-1 choose 2
        //int numComb2 = getNumCombinations(a2-1,2);
        BigDecimal countTo = getBigNumComb(a2,3);
        BigDecimal k2=k.subtract(countTo); //the backwards count inside the a2-1 choose 2 portion
        //System.out.println(k2);
        //k2 = Math.max(k2,1); //just incase there was a small rounding error, make sure k2 is at least 0

        BigDecimal nCountBD = (new BigDecimal(0.5)).multiply(
                        BigFunctions.pow(
                                (new BigDecimal(8).multiply(k2)).add(new BigDecimal(1))
                                ,new BigDecimal(0.5),scale)
                        .add(new BigDecimal(1))
        );
        //double nCountDouble = 0.5*(Math.sqrt(8*k2+1)+1);
        //System.out.println(nCountDouble);
        //int nCount = (int) Math.ceil(nCountDouble);
        int nCount = nCountBD.setScale(0, RoundingMode.CEILING).intValue();
        int value = a-nCount;
        return value;
    }
    private int calcThirdInt(BigDecimal n, BigDecimal numComb){
        BigDecimal k = numComb.subtract(n);
        int a2 = a-choiceLocal[0]-1;
        BigDecimal firstCount = getBigNumComb(a2,3);
        int a3 = a-choiceLocal[1]-1;
        BigDecimal secondCount = getBigNumComb(a3,2);
        BigDecimal k3 = k.subtract(firstCount).subtract(secondCount);
        //k3 = Math.max(k3,1);
        int value = a-k3.intValue();
        return value;
    }
}




