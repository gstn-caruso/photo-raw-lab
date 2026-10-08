require 'minitest/autorun'
require_relative 'release_version'

class ReleaseVersionTest < Minitest::Test
  def test_features_increment_minor_and_reset_patch
    assert_equal '0.2.0', ReleaseVersion.next('0.1.2', ["feat(mosaic): browse RAWs"])
  end

  def test_fixes_and_performance_increment_patch
    assert_equal '1.2.4', ReleaseVersion.next('1.2.3', ['fix: decode', 'perf: render'])
  end

  def test_breaking_header_or_footer_increments_major
    assert_equal '2.0.0', ReleaseVersion.next('1.2.3', ['docs!: replace public CLI'])
    assert_equal '2.0.0', ReleaseVersion.next('1.2.3', ["refactor: change API\n\nBREAKING CHANGE: old API removed"])
  end

  def test_non_product_changes_do_not_release
    assert_nil ReleaseVersion.next('1.2.3', ['docs: usage', 'chore: cleanup', 'ci: workflow', 'test: cases'])
  end

  def test_highest_bump_wins_and_empty_history_does_not_release
    assert_equal '1.3.0', ReleaseVersion.next('1.2.3', ['fix: one', 'feat: two'])
    assert_nil ReleaseVersion.next('1.2.3', [])
  end
end
