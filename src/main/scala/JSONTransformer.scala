import spray.json._

object JSONTransformer {

  private val intToString = (0 until 1000).map(_.toString).toArray

  private def toString(x: Int): String = if(x < intToString.length) intToString(x) else x.toString

  def unflatten(value: Seq[(String, JsValue)]): JsValue = {
    def toValue(s: Map[String, JsValue]): JsValue = {
      def toArray: Option[JsArray] = Some(JsArray((0 until s.size).map { i =>
        s.get(toString(i)) match {
          case Some(value) => value
          case None => return None
        }
      }.toVector))
      toArray.getOrElse(JsObject(s))
    }
    def unflatten0(value: Seq[(Array[String], JsValue)], depth: Int): JsValue = {
      val minDepth = value.minBy(_._1.length)._1.length
      if(minDepth > depth) {
        val data = value
          .groupBy { case (k, v) => k(depth) }
          .map { case (k, v) => k -> unflatten0(v, depth + 1)}
        toValue(data)
      } else {
        require(value.length == 1)
        value.head._2
      }
    }
    val t = value.map { case (k, v) => k.split('.').filterNot(_.isEmpty) -> v}
    unflatten0(t, 0)
  }

  def flatten(value: JsValue): Seq[(String, JsValue)] = {

    val builder = Array.newBuilder[(String, JsValue)]

    def concat(a: String, b: String) =
      if(a.isEmpty) b
      else if(b.isEmpty) a
      else a + "." + b

    def flatten0(value: JsValue, prefix: String) : Unit = {
      value match {
        case JsObject(fields) =>
          val keys = fields.keys.toArray.sorted
          for(key <- keys)
            flatten0(fields(key), concat(prefix, key))
        case JsArray(elements) =>
          for(i <- 0 until elements.length)
            flatten0(elements(i), concat(prefix, toString(i)))
        case _ =>
          builder += prefix -> value
      }
    }

    flatten0(value, "")
    builder.result()
  }
}
