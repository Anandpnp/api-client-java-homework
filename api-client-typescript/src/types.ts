import type createClient from 'openapi-fetch';
import type { paths } from './schema.js';

export type Client = ReturnType<typeof createClient<paths>>;
