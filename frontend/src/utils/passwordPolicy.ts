/**
 * 密码规则。
 *
 * <p>必须与后端 {@code com.notemind.common.validation.PasswordPolicy} 保持一致，
 * 改这里就要同时改那里，否则会出现"前端过了后端拒"。
 *
 * <p>规则：8-64 位 ASCII 可见字符（不含空格）。限定 ASCII 的原因见后端那个类的注释。
 */

export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 64

/** ASCII 可见字符 0x21-0x7E，排除空格 */
const ALLOWED_CHARS = /^[\x21-\x7E]+$/

/**
 * 校验密码，合法时返回空串。
 *
 * <p>返回具体原因而不是布尔值 —— 调用方要把原因显示给用户，
 * 只返回 false 的话每个调用点都得再判断一次"到底哪里不合格"。
 */
export function validatePassword(password: string): string {
  if (!password) {
    return '请输入密码'
  }
  if (password.length < PASSWORD_MIN_LENGTH) {
    return `密码至少 ${PASSWORD_MIN_LENGTH} 位（当前 ${password.length} 位）`
  }
  if (password.length > PASSWORD_MAX_LENGTH) {
    return `密码不能超过 ${PASSWORD_MAX_LENGTH} 位（当前 ${password.length} 位）`
  }
  if (!ALLOWED_CHARS.test(password)) {
    return '密码只能用英文字母、数字和常见符号，不能有空格、中文或全角字符'
  }
  return ''
}
