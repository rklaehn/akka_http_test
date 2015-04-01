import org.junit.Test
import spray.json._
import org.junit.Assert._

class JSONTransformerTest {

  def roundtripTest(tree: JsValue): Unit = {
    val flat = JSONTransformer.flatten(tree)
    val tree1 = JSONTransformer.unflatten(flat)
    assertEquals(tree, tree1)
  }

  @Test
  def testScalar(): Unit = {
    roundtripTest("""1""".parseJson)
    roundtripTest(""""xyz"""".parseJson)
  }

  @Test
  def testSimple(): Unit = {
    val tree = """{"a":1}""".parseJson
    roundtripTest(tree)
  }

  @Test
  def testNested(): Unit = {
    val tree = """{"a":{"b":1}}""".parseJson
    roundtripTest(tree)
  }

  @Test
  def testArray1(): Unit = {
    val tree = """[1,2,3]""".parseJson
    roundtripTest(tree)
  }

  @Test
  def testArray2(): Unit = {
    val tree = """[1,{"x":2},null]""".parseJson
    roundtripTest(tree)
  }
}
