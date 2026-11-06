package net.wowdev.ecommerce.payments.commons;

public class Utils {

  public static boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
    Throwable current = throwable;
    while (current != null) {
      if (causeType.isInstance(current)) {
        return true;
      }
      if (current.getCause() == current) {
        break;
      }
      current = current.getCause();
    }
    return false;
  }
}
