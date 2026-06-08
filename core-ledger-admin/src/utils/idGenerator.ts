export function generateRequestId(): string {
  return `REQ_${Date.now()}_${Math.random().toString(36).substring(2, 10).toUpperCase()}`
}

export function formatAmount(amount: number | string, decimals = 2): string {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

export function formatCurrency(amount: number | string, currency = 'CNY'): string {
  const symbols: Record<string, string> = {
    CNY: '¥',
    USD: '$',
    EUR: '€'
  }
  return `${symbols[currency] || ''}${formatAmount(amount)}`
}
