package com.debs.grid;

import static org.junit.Assert.*;

import org.junit.Test;

import com.debs.grid.LL2XY.XY;

/**
 * @author Sameer
 *
 */
public class LL2XYTest {

   private Double sLat1 = 40.756775;
   private Double sLong1 = -73.989937;

   private Double sLat2 = 40.77063;
   private Double sLong2 = -73.86525;
   
   private Double d = 10528.239220812493;
   
   @Test
   public void test() {
      LL2XY ll2xy = new LL2XY();
      XY xy1 = ll2xy.computeLL2XY(sLat1, sLong1);
      XY xy2 = ll2xy.computeLL2XY(sLat2, sLong2);

      Double distance = ll2xy.getEuclideanDistance(xy1, xy2);
      assertEquals(d,distance);
   }
}

