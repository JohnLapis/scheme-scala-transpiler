import scala.language.implicitConversions

package object SchemeHelpers {
  // This is the type of all Scheme variables. That's necessary to implement its
  // dynamic typing.
  type Sch = Int | Number | Boolean
  type ExactNum = Number

  given Conversion[Sch, Number] = _.asInstanceOf
  given Conversion[Sch, Int] = _.asInstanceOf
  given Conversion[Sch, Boolean] = (x: Sch) => x match
    case x: Boolean if x == false => false
    case _ => true

  val predicates = List(isBoolean, isChar, isNull, isPair, isProcedure, isSymbol, isBytevector, isEofObject, isNumber, isPort, isString, isVector)

  def typesAreEqual(obj1: Sch, obj2: Sch): Boolean =
    (for predicate <- predicates
     yield predicate(obj1) && predicate(obj2)).reduce(_ || _)

  def isChar(obj: Sch): Boolean = ???
  def isNull(obj: Sch): Boolean = ???
  def isPair(obj: Sch): Boolean = ???
  def isProcedure(obj: Sch): Boolean = ???
  def isSymbol(obj: Sch): Boolean = ???
  def isBytevector(obj: Sch): Boolean = ???
  def isEofObject(obj: Sch): Boolean = ???
  def isNumber(obj: Sch): Boolean = ???
  def isPort(obj: Sch): Boolean = ???
  def isString(obj: Sch): Boolean = ???
  def isVector(obj: Sch): Boolean = ???

  // Section: Equivalence predicates

  def isEqv(obj1: Sch, obj2: Sch): Boolean = (obj1, obj2) match {
    case (obj1: Boolean, obj2: Boolean) => obj1 == obj2
    // TODO case (obj1: Symbol, obj2: Boolean) => symbolEq(obj1, obj2)
    // TODO case (obj1: ExactNum, obj2: ExactNum) => numsEq(obj1, obj2)
    // TODO case (obj1: InexactNum, obj2: InexactNum) => numsEq(obj1, obj2)
    // TODO case (obj1: Char, obj2: Char) => charEq(obj1, obj2)
    // TODO case pairs, vectors, bytevectors, records, strings
    // TODO case procedures
    // TODO case (obj1: Any, obj2: Any) if !typesAreEqual(obj1, obj2) => false
    // case (_, _) => unspecified
  }

  def isEq(obj1: Sch, obj2: Sch): Boolean =
    if ! isEqv(obj1, obj2) then
      false
    else
      ???

  def isEqual(obj1: Sch, obj2: Sch): Boolean =
    if !typesAreEqual(obj1, obj2) then
      false
    else
      obj1 match {
        // TODO case obj1: Pair | Vector | String | Bytevector => ??? //recurse
        case obj1: List[Any] if obj1.isEmpty => isEqv(obj1, obj2)
        // TODO case obj1: Boolean | Symbol | Number | Char | Port | Procedure => isEqv(obj1, obj2)
        case _ => isEqv(obj1, obj2)
      }

  // Section: Numbers

  def numsEq(nums: Number*): Boolean = compareNumbers(nums, _ == _)
  // def numsLt(nums: Number*): Boolean = compareNumbers(nums, _ < _)
  // def numsLe(nums: Number*): Boolean = compareNumbers(nums, _ <= _)
  // def numsGt(nums: Number*): Boolean = compareNumbers(nums, _ > _)
  // def numsGe(nums: Number*): Boolean = compareNumbers(nums, _ >= _)
  private def compareNumbers(
    nums: Seq[Number],
    operator: (Number, Number) => Boolean
  ): Boolean =
    if nums.length <= 1 then
      true
    else
      operator(nums(0), nums(1)) && compareNumbers(nums.drop(1), operator)

  // Section: Booleans

  def isBoolean(obj: Sch): Boolean = obj match {
    case obj: Boolean => true
    case _ => false
  }

  def isBoolean(objs: Sch*): Boolean =
    objs.map(isBoolean).reduce(_ && _)
      && objs.reduce(_ && _) == true // All objs are true
      || objs.reduce(_ || _) == false // All objs are false
}
