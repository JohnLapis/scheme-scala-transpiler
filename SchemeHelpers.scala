import scala.language.implicitConversions
import math.{sin, cos}

package object SchemeHelpers {
  case class Rational(numerator: Int, denominator: Int)
  case class Polar(radius: Real, angle: Real)
  case class Complex(real: Real, complex: Real)

  extension (n: SchNumber)
    def unary_- = n match {
      case n: Int => -n
      case n: Rational => -n
      case n: Real => -n
      case n: Polar => -n
      case n: Complex => -n
    }

  extension (i: Int)
    def +(x: SchNumber): SchNumber = x match {
      case x: Int => i + x
      case x: Rational => Rational(i * x.denominator + x.numerator,
                                   x.denominator)
      case x: Real => i + x
      case x: Polar => i + x.asInstanceOf[Complex]
      case x: Complex => Complex(i + x.real, x.complex)
    }
    def -(x: SchNumber): SchNumber = i + (-x)

  extension (r: Rational)
    def inverse: Rational = Rational(r.denominator, r.numerator)
    def +(s: Rational): Rational =
      Rational(r.numerator * s.denominator + s.numerator * r.denominator,
               r.denominator * s.denominator)
    def unary_- = Rational(-r.numerator, r.denominator)
    // def -(s: Rational): Rational = r + (-s)
    def *(s: Rational): Rational =
      Rational(r.numerator * s.numerator, r.denominator * s.denominator)
    def /(s: Rational): Rational = r * s.inverse

  extension (z: Polar)
    def unary_- = Polar(-z.radius, z.angle)

  extension (z: Complex)
    def conjugate: Complex = Complex(z.real, -z.complex)
    def +(w: Complex): Complex = Complex(z.real + w.real, z.complex + w.complex)
    def unary_- = Complex(-z.real, -z.complex)
    // def -(w: Complex): Complex = z + (-w)
    def *(w: Complex): Complex =
      Complex(z.real * w.real - z.complex * z.complex,
              z.real * w.complex + z.complex * w.real)
    def /(w: Complex): Complex =
      if w.complex == 0 then
        Complex(z.real / w.real, z.complex / w.real)
      else
        (z * w.conjugate) / (w * w.conjugate)

  type Real = Double // This includes infinities and NaN.
  type SchNumber = Complex | Polar | Real | Rational | Int
  // type Sch = (SchNumber | Boolean | Char | Null | Pair | Procedure | Symbol
  //               | Bytevector | EofObject | Port | String | Vector)
  type Sch = SchNumber | Boolean

  // given Conversion[SchNumber, Int] = _.asInstanceOf[Int]
  // given Conversion[SchNumber, Rational] = _.asInstanceOf[Rational]
  // given Conversion[SchNumber, Real] = _.asInstanceOf[Real]
  // given Conversion[SchNumber, Polar] = _.asInstanceOf[Polar]
  // given Conversion[SchNumber, Complex] = _.asInstanceOf[Complex]

  given Conversion[Sch, SchNumber] = _.asInstanceOf
  given Conversion[Sch, Boolean] = (x: Sch) => x match
    case x: Boolean if x == false => false
    case _ => true

  given Conversion[Int, Rational] = Rational(_, 1)
  given Conversion[Rational, Real] =
    rat => rat.numerator.asInstanceOf[Real] / rat.denominator.asInstanceOf[Real]
  given Conversion[Real, Double] = _.asInstanceOf
  given Conversion[Double, Complex] = Complex(_, 0)
  given Conversion[Polar, Complex] =
    z => Complex(z.radius * cos(z.angle), z.radius * sin(z.angle))

  // val predicates = List(isBoolean, isChar, isNull, isPair, isProcedure, isSymbol, isBytevector, isEofObject, isNumber, isPort, isString, isVector)
  val predicates = List(isBoolean, isNumber)

  def typesAreEqual(obj1: Sch, obj2: Sch): Boolean =
    (for predicate <- predicates
     yield predicate(obj1) && predicate(obj2)).reduce(_ || _)

  def isChar(obj: Sch): Boolean = ???
  def isNull(obj: Sch): Boolean = obj == null
  def isPair(obj: Sch): Boolean = ???
  def isProcedure(obj: Sch): Boolean = ???
  def isSymbol(obj: Sch): Boolean = ???
  def isBytevector(obj: Sch): Boolean = ???
  def isEofObject(obj: Sch): Boolean = ???
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

  def isInteger(obj: Sch): Boolean = obj match {
    case obj: Int => true
    case Rational(_, denominator) => denominator == 1
    case _ => false
  }
  def isRational(obj: Sch): Boolean = obj match {
    case obj: Rational => true
    case _ => isInteger(obj)
  }
  def isReal(obj: Sch): Boolean = obj match {
    case obj: Real => true
    case Complex(_, complex) => complex == 0
    case _ => isRational(obj)
  }
  def isComplex(obj: Sch): Boolean = obj match {
    case obj: Complex => true
    case _ => isReal(obj)
  }
  def isNumber(obj: Sch): Boolean = isComplex(obj)

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
