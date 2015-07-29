package pl.newit.crc

/** Computes a hash code based on the data provided. */
trait HashFunction extends (TraversableOnce[Byte] ⇒ HashCode) {
  /** Returns the number of bytes that each hash code produced by this hash function has. */
  def length: Int
}

object Crc16 extends HashFunction {
  private[crc] lazy val table: Array[Int] =
    (0 until 256).map(i ⇒
      (0 until 8).foldLeft(0) {
        case (crc, j) if ((crc ^ (i >>> j)) & 0x0001) > 0 ⇒ (crc >>> 1) ^ 0xA001
        case (crc, _)                                     ⇒ crc >>> 1
      }
    )(collection.breakOut)


  override def length = 2

  override def apply(bytes: TraversableOnce[Byte]) = HashCode.fromShort(
    bytes.foldLeft(0)((crc, b) ⇒
      (crc >>> 8) ^ table((crc ^ (0x00FF & b)) & 0xFF)
    )
  )
}