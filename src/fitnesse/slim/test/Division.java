package fitnesse.slim.test;

public class Division {
  private double numerator;
  private double denominator;

  public void setNumerator(double numerator) {
    this.numerator = numerator;
  }

  public void setDenominator(double denominator) {
    this.denominator = denominator;
  }

  public double quotient() {
    return numerator / denominator;
  }
}
