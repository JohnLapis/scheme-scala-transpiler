import scala.language.implicitConversions

// This is the type of all Scheme variables. That's necessary to implement its
// dynamic typing.
type Sch = Boolean | Int | String | Char

// Scheme doesn't convert types implicitly (except for conversions to boolean
// type), so if x doesn't match the expected type, the program should throw an
// error. That's why `@unchecked` is used.


// Version 1
// def collapseSchemeType_v1[T](x: Sch): T = (x: @unchecked) match {case x: T => x}

// Version 2
def collapseSchemeType[T](x: Sch): T = x.asInstanceOf[T]

// given Conversion[Sch, Int] = collapseSchemeType[Int]
given Conversion[Sch, Int] = _.asInstanceOf
// given Conversion[Sch, Char] = collapseSchemeType[Char]
given Conversion[Sch, String] = collapseSchemeType[String]
given Conversion[Sch, Boolean] = (x: Sch) => x match
  case x: Boolean if x == false => false
  case _ => true



def _main(args: Array[String]) = {
  var sch: Sch = 5
  val int: Int = sch
  println(sch * sch)
  println(int * int)

  sch = "true"
  val str: String = sch
  println(sch + sch)
  println(str + str)

  sch = true
  val bool: Boolean = sch
  println(sch && sch)
  println(bool && bool)
}

def main(args: Array[String]) = {
  println(
    if true then
      println()
      5
    else
        6
  )
}
