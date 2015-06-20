package utils

/**
 * Created by tomas on 20-06-15.
 */

object SeqUtils {
  def average[T](ts: Iterable[T])(implicit num: Numeric[T]) = {
    num.toDouble(ts.sum) / ts.size
  }
}