package object SchemeHelpers {
  def numEquals(nums: Number*): Boolean = _numEquals(nums)
  private def _numEquals(nums: Seq[Number]): Boolean =
  if nums.length <= 1 then
    true
  else
    nums(0) == nums(1) && _numEquals(nums.drop(1))
}
