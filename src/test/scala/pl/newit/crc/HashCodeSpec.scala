package pl.newit.crc

import java.nio.ByteOrder.LITTLE_ENDIAN

import akka.util.ByteString
import org.scalacheck.Prop.{forAll, BooleanOperators}
import org.scalacheck.Properties

object HashCodeSpec extends Properties("HashCode") {
  property("fromShort") = forAll { value: Short ⇒
    HashCode.fromShort(value) == HashCode(ByteString.newBuilder.putShort(value)(LITTLE_ENDIAN).result())
  }

  property("fromInt") = forAll { value: Int ⇒
    HashCode.fromInt(value) == HashCode(ByteString.newBuilder.putInt(value)(LITTLE_ENDIAN).result())
  }

  property("length") = forAll { bytes: Array[Byte] ⇒
    (bytes.length > 0)  ==> (HashCode(ByteString(bytes)).length == bytes.length)
  }

  property("toShort") = forAll { value: Short ⇒
    HashCode.fromShort(value).toShort == value
  }

  property("toInt") = forAll { value: Int ⇒
    HashCode.fromInt(value).toInt == value
  }
}
