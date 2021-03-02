package provingground.interface

import ammonite.ops._

object Tuts {
  implicit val wd: Path = pwd

  def tutdir : Path = pwd / "mantle" / "src" / "main" / "tut"

  def gitHash: String =
    read(resource / "gitlog.txt") 
    // %%("git", "rev-parse", "HEAD").out.lines.head


  lazy  val gitrep: String =
  s"""
     |
     |#### git commit hash when running tutorial: $gitHash
     |
 """.stripMargin


  def mkTut(f: String): String = {
    val top =
      ""

    val spl = f.split("```tut").map(_.split("```").toVector).toVector

    val tutcode: String =
      spl.tail
        .map(_(0))
        .mkString(top, "// tutEnd", "")

    pprint.log(tutcode)

    val output = MDocService.replResult(tutcode).getOrElse(throw new Exception("no repl result"))

    pprint.log(output)

    val tutChunks =
      output
        .split("repl.prompt\\(\\) = \"scala> \"")(1)
        .split("""// tutEnd""")
        .map((s) => s"""```scala
           |${s.trim.dropRight(6).trim}
           |```
           |
       |
     """.stripMargin)
        .toVector

    pprint.log(tutChunks.size)

    val tailTextChunks: Vector[String] =
      spl.tail.map((v) => v.applyOrElse[Int, String](1, (_) => ""))

    val textTail: Vector[String] =
      tutChunks
        .zip(tailTextChunks)
        .flatMap { case (a, b) => Vector(a, b) }

    val allChunks: Vector[String] = spl.head.head +: textTail

   
    allChunks.mkString("", "\n", gitrep)
  }

  def outdir: Path = pwd / "docs" / "tuts"

}
