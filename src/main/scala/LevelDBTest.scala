import java.io.File

import io.BlobBuilder
import org.iq80.leveldb._
import org.iq80.leveldb.impl.Iq80DBFactory.{factory => javaFactory}
import org.fusesource.leveldbjni.JniDBFactory.{factory => cppFactory}

object LevelDBTest extends App {
  val maxi = 10000
  val maxj = 10000

  def test(factory: org.iq80.leveldb.DBFactory, name: String, useBatch: Boolean): Unit = {

    val options = new Options()
      .createIfMissing(true)
      .paranoidChecks(false)
      .verifyChecksums(false)
      .errorIfExists(true)
      .blockSize(1 << 16)
      .writeBufferSize(1024 * 1024 * 64)
      .compressionType(CompressionType.SNAPPY)

    println(options.cacheSize())
    println(options.blockSize())
    println(options.writeBufferSize())

    val db = factory.open(new File(name), options)
    def mkKey(i: Int, j: Int): Array[Byte] = {
      val builder = BlobBuilder(8)
      builder.putInt(i)
      builder.putInt(j)
      builder.result
    }

    def mkValue(i: Int, j: Int): Array[Byte] = {
      val builder = BlobBuilder(1024)
      for(x <- 0 until 1024 / 4)
        builder.putInt(j)
      builder.result
    }

    val t0 = System.nanoTime()
    for (i <- 0 until maxi) {
      if (useBatch) {
        val batch = db.createWriteBatch()
        for (j <- 0 until maxj) {
          val key = mkKey(i, j)
          val value = mkValue(i, j)
          batch.put(key, value)
        }
        db.write(batch)
        batch.close()
      } else {
        for (j <- 0 until maxj) {
          val key = mkKey(i, j)
          val value = mkValue(i, j)
          db.put(key, value)
        }
    }
      val dt = (System.nanoTime() - t0) / 1.0e9
      val samples = 1L * i * maxj
      val sps = samples / dt
      println(s"samples=$samples")
      println(s"size=${samples*(8L + 1024)}")
      println(s"sps=$sps")
    }
    println("Compacting!")
    // db.compactRange(mkKey(0,0), mkKey(maxi, maxj))
    db.close()
    println("Done!")
  }

  test(cppFactory, "/tmp/cpp", true)
  test(javaFactory, "/tmp/java", true)
}
