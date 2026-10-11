/**
 * 邮箱规则。
 *
 * <p>必须与后端 {@code com.notemind.common.validation.EmailPolicy} 保持一致，
 * 改这里就要同时改那里。写法也刻意和后端一致，便于对照。
 *
 * <p>只做形状检查，用来拦住打字错误和乱码。真正能确认邮箱可用的是发验证邮件。
 */

export const EMAIL_MAX_LENGTH = 254

/** 常见的长后缀（超过 6 个字母的真实 TLD）。需要时往这里加 */
const LONG_TLDS =
  'photography|technology|solutions|consulting|foundation|management|engineering' +
  '|construction|community|education|healthcare|university|productions|enterprises' +
  '|marketing|accounting|directory|equipment|furniture|graphics|holdings|lighting' +
  '|mortgage|memorial|training|ventures|vacation|properties|industries|assistance' +
  '|consultant|production|contracting|entertainment|insurance|financial|apartments' +
  '|international'

/** 本地部分：点用于分隔，所以不能出现连续的点、也不能以点开头或结尾 */
const LOCAL = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"

/** 域名的一段：不能以连字符开头或结尾 */
const LABEL = '[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?'

/** 后缀必须是 2-6 个字母，或在长后缀白名单里 */
const TLD = `(?:[A-Za-z]{2,6}|${LONG_TLDS})`

/** 开头的 lookahead 限制本地部分不超过 64 字符 */
const EMAIL_PATTERN = new RegExp(`^(?=[^@]{1,64}@)${LOCAL}@(?:${LABEL}\\.)+${TLD}$`)

/** 校验邮箱，合法时返回空串 */
export function validateEmail(email: string): string {
  if (!email) {
    return '请输入邮箱'
  }
  if (email.length > EMAIL_MAX_LENGTH) {
    return `邮箱长度不能超过 ${EMAIL_MAX_LENGTH} 个字符`
  }
  if (!EMAIL_PATTERN.test(email)) {
    return '邮箱格式不正确，请填写真实可用的邮箱'
  }
  return ''
}
