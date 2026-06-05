let counter = 0;

export function generateRequestId(): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 10);
  counter = (counter + 1) % 10000;
  return `REQ${timestamp}${random}${counter.toString().padStart(4, '0')}`;
}

export function generateBusinessNo(prefix: string = 'BIZ'): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = (now.getMonth() + 1).toString().padStart(2, '0');
  const day = now.getDate().toString().padStart(2, '0');
  const hours = now.getHours().toString().padStart(2, '0');
  const minutes = now.getMinutes().toString().padStart(2, '0');
  const seconds = now.getSeconds().toString().padStart(2, '0');
  const milliseconds = now.getMilliseconds().toString().padStart(3, '0');
  const random = Math.random().toString(36).substring(2, 6).toUpperCase();
  return `${prefix}${year}${month}${day}${hours}${minutes}${seconds}${milliseconds}${random}`;
}

export function generateAccountNo(): string {
  const prefix = '6222';
  const timestamp = Date.now().toString().slice(-10);
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
  return `${prefix}${timestamp}${random}`;
}

export function generateTransactionNo(): string {
  return generateBusinessNo('TXN');
}

export function generateEntryNo(): string {
  return generateBusinessNo('ETY');
}

export function generateUuid(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
