import { CURRENCY_MAP } from './constants';

const SCALE = 2;
const FACTOR = 100;

export function yuanToFen(amount: number | string | null | undefined): number {
  if (amount === null || amount === undefined || amount === '') {
    return 0;
  }
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(num)) {
    return 0;
  }
  return Math.round(num * FACTOR);
}

export function fenToYuan(amount: number | string | null | undefined): number {
  if (amount === null || amount === undefined || amount === '') {
    return 0;
  }
  const num = typeof amount === 'string' ? parseInt(amount, 10) : amount;
  if (isNaN(num)) {
    return 0;
  }
  return Math.round((num / FACTOR) * Math.pow(10, SCALE)) / Math.pow(10, SCALE);
}

export function formatAmount(amount: number | string | null | undefined, decimals: number = SCALE): string {
  const num = fenToYuan(amount);
  return num.toFixed(decimals);
}

export function formatCurrency(
  amount: number | string | null | undefined,
  currency: string = 'CNY',
  decimals: number = SCALE
): string {
  const formattedAmount = formatAmount(amount, decimals);
  const currencySymbol = getCurrencySymbol(currency);
  return `${currencySymbol} ${formattedAmount}`;
}

export function getCurrencySymbol(currency: string): string {
  const symbolMap: Record<string, string> = {
    CNY: '¥',
    USD: '$',
    EUR: '€',
    GBP: '£',
    JPY: '¥',
  };
  return symbolMap[currency] || currency;
}

export function getCurrencyName(currency: string): string {
  return CURRENCY_MAP[currency] || currency;
}

export function validateAmount(amount: number | string | null | undefined): { valid: boolean; message?: string } {
  if (amount === null || amount === undefined || amount === '') {
    return { valid: false, message: '金额不能为空' };
  }
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(num)) {
    return { valid: false, message: '金额格式不正确' };
  }
  if (num < 0) {
    return { valid: false, message: '金额不能为负数' };
  }
  const fen = yuanToFen(num);
  if (fen > 999999999999999) {
    return { valid: false, message: '金额超出最大限制' };
  }
  return { valid: true };
}

export function addAmount(a: number, b: number): number {
  return Math.round((a + b) * Math.pow(10, SCALE)) / Math.pow(10, SCALE);
}

export function subtractAmount(a: number, b: number): number {
  return Math.round((a - b) * Math.pow(10, SCALE)) / Math.pow(10, SCALE);
}

export function multiplyAmount(a: number, b: number): number {
  return Math.round(a * b * Math.pow(10, SCALE)) / Math.pow(10, SCALE);
}

export function divideAmount(a: number, b: number): number {
  if (b === 0) {
    return 0;
  }
  return Math.round((a / b) * Math.pow(10, SCALE)) / Math.pow(10, SCALE);
}
