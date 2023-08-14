import scala.language.implicitConversions

package object SchemeHelpers {
  // This is the type of all Scheme variables. That's necessary to implement its
  // dynamic typing.
  type Sch = Int | Number | Boolean

  given Conversion[Sch, Number] = _.asInstanceOf
  given Conversion[Sch, Int] = _.asInstanceOf
  given Conversion[Sch, Boolean] = (x: Sch) => x match
    case x: Boolean if x == false => false
    case _ => true

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
}
