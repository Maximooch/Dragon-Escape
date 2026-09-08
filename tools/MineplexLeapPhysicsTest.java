package me.radoje17.dragonescape.kits;

public final class MineplexLeapPhysicsTest {
   private static final double EPSILON = 1.0E-9;

   public static void main(String[] args) {
      referenceKitConstantsMatchMineplex();
      horizontalDirectionKeepsMineplexVerticalAdds();
      upwardDirectionIsCappedBeforeGroundBoost();
      airborneUpwardDirectionStopsAtHeightLimit();
      downwardDirectionIsNormalizedBeforeAddition();
      zeroDirectionProducesZeroVelocity();
      System.out.println("Mineplex leap physics checks passed");
   }

   private static void referenceKitConstantsMatchMineplex() {
      if (MineplexLeapPhysics.MAX_USES != 4) {
         throw new AssertionError("expected four Mineplex leap uses");
      }
      if (MineplexLeapPhysics.DEFAULT_COOLDOWN_SECONDS != 8) {
         throw new AssertionError("expected an eight-second Mineplex leap cooldown");
      }
   }

   private static void horizontalDirectionKeepsMineplexVerticalAdds() {
      assertVector(MineplexLeapPhysics.calculate(1.0, 0.0, 0.0, true), 1.0, 0.4, 0.0, "grounded horizontal");
      assertVector(MineplexLeapPhysics.calculate(1.0, 0.0, 0.0, false), 1.0, 0.2, 0.0, "airborne horizontal");
   }

   private static void upwardDirectionIsCappedBeforeGroundBoost() {
      assertVector(MineplexLeapPhysics.calculate(0.0, 1.0, 0.0, true), 0.0, 1.2, 0.0, "grounded upward");
   }

   private static void airborneUpwardDirectionStopsAtHeightLimit() {
      assertVector(MineplexLeapPhysics.calculate(0.0, 1.0, 0.0, false), 0.0, 1.0, 0.0, "airborne upward");
   }

   private static void downwardDirectionIsNormalizedBeforeAddition() {
      double invSqrt2 = 1.0 / Math.sqrt(2.0);
      assertVector(
         MineplexLeapPhysics.calculate(invSqrt2, -invSqrt2, 0.0, false),
         invSqrt2,
         -invSqrt2 + 0.2,
         0.0,
         "airborne downward"
      );
   }

   private static void zeroDirectionProducesZeroVelocity() {
      assertVector(MineplexLeapPhysics.calculate(0.0, 0.0, 0.0, true), 0.0, 0.0, 0.0, "zero direction");
   }

   private static void assertVector(double[] actual, double x, double y, double z, String name) {
      if (actual.length != 3) {
         throw new AssertionError(name + ": expected three components, got " + actual.length);
      }
      assertClose(actual[0], x, name + " x");
      assertClose(actual[1], y, name + " y");
      assertClose(actual[2], z, name + " z");
   }

   private static void assertClose(double actual, double expected, String name) {
      if (Math.abs(actual - expected) > EPSILON) {
         throw new AssertionError(name + ": expected " + expected + ", got " + actual);
      }
   }
}
