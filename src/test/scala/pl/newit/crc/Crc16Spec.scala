package pl.newit.crc

import org.scalacheck.{Prop, Properties}

class Crc16Spec extends Properties("Crc16") {
  property("apply") = Prop {
    Crc16(Array[Byte]()) == HashCode.fromShort(0) &&
      Crc16("Scala".getBytes) == HashCode.fromShort(0x7E27)
  }
}
