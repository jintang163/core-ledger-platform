import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { message } from 'antd';
import { DATETIME_FORMAT, DATE_FORMAT } from './constants';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

export function formatDateTime(
  date: string | number | Date | dayjs.Dayjs | null | undefined,
  format: string = DATETIME_FORMAT
): string {
  if (!date) {
    return '-';
  }
  return dayjs(date).format(format);
}

export function formatDate(
  date: string | number | Date | dayjs.Dayjs | null | undefined,
  format: string = DATE_FORMAT
): string {
  return formatDateTime(date, format);
}

export function formatRelativeTime(
  date: string | number | Date | dayjs.Dayjs | null | undefined
): string {
  if (!date) {
    return '-';
  }
  return dayjs(date).fromNow();
}

export function formatRelativeTimeToNow(
  date: string | number | Date | dayjs.Dayjs | null | undefined
): string {
  if (!date) {
    return '-';
  }
  const now = dayjs();
  const target = dayjs(date);
  const diffDays = now.diff(target, 'day');

  if (diffDays === 0) {
    const diffHours = now.diff(target, 'hour');
    if (diffHours === 0) {
      const diffMinutes = now.diff(target, 'minute');
      if (diffMinutes === 0) {
        return '刚刚';
      }
      return `${diffMinutes}分钟前`;
    }
    return `${diffHours}小时前`;
  } else if (diffDays === 1) {
    return '昨天';
  } else if (diffDays < 7) {
    return `${diffDays}天前`;
  } else {
    return formatDate(date);
  }
}

export function formatThousands(num: number | string | null | undefined): string {
  if (num === null || num === undefined || num === '') {
    return '-';
  }
  const n = typeof num === 'string' ? parseFloat(num) : num;
  if (isNaN(n)) {
    return '-';
  }
  return n.toLocaleString('zh-CN');
}

export function formatPercent(
  num: number | string | null | undefined,
  decimals: number = 2
): string {
  if (num === null || num === undefined || num === '') {
    return '-';
  }
  const n = typeof num === 'string' ? parseFloat(num) : num;
  if (isNaN(n)) {
    return '-';
  }
  return `${(n * 100).toFixed(decimals)}%`;
}

export function maskString(str: string | null | undefined, start: number = 3, end: number = 4): string {
  if (!str) {
    return '-';
  }
  if (str.length <= start + end) {
    return str;
  }
  const prefix = str.substring(0, start);
  const suffix = str.substring(str.length - end);
  const mask = '*'.repeat(str.length - start - end);
  return `${prefix}${mask}${suffix}`;
}

export function maskPhone(phone: string | null | undefined): string {
  return maskString(phone, 3, 4);
}

export function maskIdCard(idCard: string | null | undefined): string {
  return maskString(idCard, 6, 4);
}

export function maskBankCard(cardNo: string | null | undefined): string {
  if (!cardNo) {
    return '-';
  }
  const cleanNo = cardNo.replace(/\s/g, '');
  if (cleanNo.length <= 8) {
    return cleanNo;
  }
  const prefix = cleanNo.substring(0, 4);
  const suffix = cleanNo.substring(cleanNo.length - 4);
  const mask = '*'.repeat(cleanNo.length - 8);
  return `${prefix} ${mask.replace(/(.{4})/g, '$1 ').trim()} ${suffix}`.trim();
}

export async function copyToClipboard(text: string): Promise<boolean> {
  if (!text) {
    message.warning('没有可复制的内容');
    return false;
  }
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
    } else {
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    }
    message.success('复制成功');
    return true;
  } catch (error) {
    console.error('复制失败:', error);
    message.error('复制失败');
    return false;
  }
}

export function truncateText(text: string | null | undefined, maxLength: number = 20): string {
  if (!text) {
    return '-';
  }
  if (text.length <= maxLength) {
    return text;
  }
  return `${text.substring(0, maxLength)}...`;
}

export function capitalizeFirstLetter(str: string | null | undefined): string {
  if (!str) {
    return '';
  }
  return str.charAt(0).toUpperCase() + str.slice(1);
}

export function toCamelCase(str: string | null | undefined): string {
  if (!str) {
    return '';
  }
  return str.replace(/[-_](\w)/g, (_, c) => c.toUpperCase());
}

export function toSnakeCase(str: string | null | undefined): string {
  if (!str) {
    return '';
  }
  return str.replace(/([A-Z])/g, '_$1').toLowerCase().replace(/^_/, '');
}
