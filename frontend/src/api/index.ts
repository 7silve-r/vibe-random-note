declare const __UPLOAD_PREFIX__: string;

export class ApiError extends Error {
  constructor(
    message: string,
    public status = 0,
  ) {
    super(message);
  }
}
const base = import.meta.env.VITE_API_BASE || '/backend';
export function token() {
  return sessionStorage.getItem('vibe-token') || '';
}
export function media(url?: string) {
  if (!url) return '';
  if (url.startsWith(typeof __UPLOAD_PREFIX__ === 'string' ? __UPLOAD_PREFIX__ : '/uploads/'))
    return base + url;
  return /^(https?:|blob:)/.test(url) ? url : '';
}
export async function api<T = any>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (token()) headers.Authorization = `Bearer ${token()}`;
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json';
  let response: Response;
  try {
    response = await fetch(base + path, {
      method,
      headers,
      body: body instanceof FormData ? body : body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(body instanceof FormData ? 120000 : 20000),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'TimeoutError')
      throw new ApiError(
        body instanceof FormData ? '上传超时，请稍后检查文件是否已保存' : '请求超时，请稍后重试',
      );
    throw new ApiError('暂时连接不上服务，请稍后重试');
  }
  const result = await response.json().catch(() => null);
  if (!response.ok || result?.code !== 200) {
    if (response.status === 401) window.dispatchEvent(new Event('vibe:unauthorized'));
    throw new ApiError(result?.message || `请求未完成（${response.status}）`, response.status);
  }
  return result.data as T;
}
export function query(values: Record<string, unknown>) {
  return new URLSearchParams(
    Object.entries(values)
      .filter(([, v]) => v !== '' && v !== undefined && v !== null)
      .map(([k, v]) => [k, String(v)]),
  ).toString();
}
export async function upload(
  path: string,
  file: File,
  method = 'PUT',
  values: Record<string, string> = {},
) {
  const audio = /\/songs\/[^/]+\/audio$/.test(path);
  const lyric = /\/songs\/[^/]+\/lyric$/.test(path);
  const limit = lyric ? 60000 : (audio ? 50 : 5) * 1024 * 1024;
  if (file.size > limit)
    throw new ApiError(
      lyric ? '歌词不能超过60KB' : audio ? '音频不能超过50MB' : '图片不能超过5MB',
      413,
    );
  if (lyric && !/\.(lrc|txt)$/i.test(file.name)) throw new ApiError('请选择 LRC 或 TXT 歌词文件');
  const data = new FormData();
  data.append('file', file);
  Object.entries(values).forEach(([k, v]) => data.append(k, v));
  return api(path, method, data);
}
