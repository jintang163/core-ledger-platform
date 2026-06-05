export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export const REQUEST_TIMEOUT = 30000;

export const PAGE_SIZE = 10;

export const MAX_PAGE_SIZE = 100;

export const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

export const DATE_FORMAT = 'YYYY-MM-DD';

export const TIME_FORMAT = 'HH:mm:ss';

export const MIN_BALANCE = 0;

export const MAX_BALANCE = 999999999999999;

export const REQUEST_ID_HEADER = 'requestId';

export const TRACE_ID_HEADER = 'traceId';

export const USER_ID_HEADER = 'userId';

export const CURRENCY_CNY = 'CNY';

export const CURRENCY_USD = 'USD';

export const CURRENCY_EUR = 'EUR';

export const CURRENCY_MAP: Record<string, string> = {
  CNY: '人民币',
  USD: '美元',
  EUR: '欧元',
};

export const FREEZE_TYPE_NORMAL = 1;

export const FREEZE_TYPE_JUDICIAL = 2;

export const FREEZE_TYPE_MAP: Record<number, string> = {
  1: '正常冻结',
  2: '司法冻结',
};
