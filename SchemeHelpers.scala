package object SchemeHelpers {
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
