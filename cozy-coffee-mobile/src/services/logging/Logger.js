const REDACTED = '[REDACTED]'
const SENSITIVE_KEY = /(token|authorization|phone|address|session[_-]?key|openid|payment|card|secret)/i

function redact(value, seen = new WeakSet()) {
  if (value == null || typeof value !== 'object') return value
  if (seen.has(value)) return '[Circular]'
  seen.add(value)

  if (Array.isArray(value)) return value.map(item => redact(item, seen))

  return Object.fromEntries(Object.entries(value).map(([key, item]) => [
    key,
    SENSITIVE_KEY.test(key) ? REDACTED : redact(item, seen)
  ]))
}

function emit(level, event, context) {
  const method = console[level] || console.log
  method(`[CozyCoffee] ${event}`, redact(context || {}))
}

export const Logger = {
  info(event, context) {
    emit('info', event, context)
  },
  warn(event, context) {
    emit('warn', event, context)
  },
  error(event, context) {
    emit('error', event, context)
  }
}

export { redact }
