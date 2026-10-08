require 'open3'

module ReleaseVersion
  def self.next(current, messages)
    bump = messages.map do |message|
      header = message.lines.first.to_s
      if header.match?(/\A\w+(\([^\n]+\))?!:/) || message.match?(/^BREAKING[ -]CHANGE:/)
        3
      elsif header.match?(/\Afeat(\([^\n]+\))?:/)
        2
      elsif header.match?(/\A(fix|perf)(\([^\n]+\))?:/)
        1
      else
        0
      end
    end.max.to_i
    return nil if bump.zero?

    major, minor, patch = current.split('.').map { |part| Integer(part) }
    case bump
    when 3 then "#{major + 1}.0.0"
    when 2 then "#{major}.#{minor + 1}.0"
    when 1 then "#{major}.#{minor}.#{patch + 1}"
    end
  end
end

if $PROGRAM_NAME == __FILE__
  tag, status = Open3.capture2('git', 'describe', '--tags', '--match', 'v[0-9]*', '--abbrev=0')
  abort 'A previous semantic release tag is required' unless status.success?
  tag = tag.strip
  abort "Invalid release tag: #{tag}" unless tag.match?(/\Av\d+\.\d+\.\d+\z/)
  history, status = Open3.capture2('git', 'log', '--format=%B%x00', "#{tag}..HEAD")
  abort 'Cannot read release history' unless status.success?
  version = ReleaseVersion.next(tag.delete_prefix('v'), history.split("\0").map(&:strip))
  puts "release=#{!version.nil?}"
  puts "version=#{version}"
end
