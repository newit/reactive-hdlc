package pl.newit.crc

import java.nio.ByteOrder.LITTLE_ENDIAN

import akka.util.ByteString

object HashCode {
  /** Creates a 16-bit HashCode representation of the given short value. */
  def fromShort(value: Int): HashCode = apply(
    ByteString(Array(
      (value >>> 0).toByte,
      (value >>> 8).toByte)
    )
  )

  /** Creates a 32-bit HashCode representation of the given int value. */
  def fromInt(value: Int): HashCode = apply(
    ByteString(Array(
      (value >>> 0).toByte,
      (value >>> 8).toByte,
      (value >>> 16).toByte,
      (value >>> 24).toByte)
    )
  )
}

/** An immutable hash code of arbitrary bit length. */
case class HashCode(bytes: ByteString) {
  require(bytes.length > 0, "bytes.length must be > 0")

  /** Returns the number of bytes in this hash code. */
  def length: Int = bytes.length

  /** Returns the first two bytes of this hashcode's bytes, converted to an short value in little-endian order. */
  def toShort: Short = {
    require(length >= 2, "asShort() requires >= 2 bytes (it only has 1 byte).")
    bytes.iterator.getShort(LITTLE_ENDIAN)
  }

  /** Returns the first four bytes of this hashcode's bytes, converted to an int value in little-endian order. */
  def toInt: Int = {
    require(length >= 4, s"asInt() requires >= 4 bytes (it only has $length bytes).")
    bytes.iterator.getInt(LITTLE_ENDIAN)
  }
}
