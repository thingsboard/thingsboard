/**
 * Backend fake minimo para pre-visualizar a UI logada (sidenav, home) sem precisar
 * de PostgreSQL/Java rodando. NAO usar em producao - dados sao falsos e nenhuma
 * autenticacao real e feita.
 *
 * Uso:
 *   1) node dev-tools/mock-backend-preview.js   (sobe na porta 8080)
 *   2) ng serve (proxy.conf.js ja aponta /api para localhost:8080)
 *   3) Abrir http://localhost:4200, abrir o Console (F12) e colar o snippet
 *      impresso no terminal (localStorage.setItem('jwt_token', ...)).
 *
 * Como funciona: o AuthGuard do ThingsBoard so exige uma chamada real ao backend
 * quando o JWT local ainda nao expirou E o usuario tem userId (GET /api/user/:id)
 * e depois GET /api/system/params. Nao ha verificacao de assinatura do JWT no
 * cliente (angular-jwt so decodifica o payload), entao um token "fake" com claims
 * plausiveis e uma expiracao futura e suficiente para passar pelo guard.
 */
const http = require('http');
const crypto = require('crypto');

const TENANT_ID = crypto.randomUUID();
const USER_ID = crypto.randomUUID();
const NULL_UUID = '13814000-1dd2-11b2-8080-808080808080'; // customerId "vazio" padrao do ThingsBoard

function b64url(obj) {
  return Buffer.from(JSON.stringify(obj)).toString('base64')
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

const header = { alg: 'HS256', typ: 'JWT' };
const nowSec = Math.floor(Date.now() / 1000);
const expSec = nowSec + 10 * 365 * 24 * 60 * 60; // 10 anos
const payload = {
  sub: 'twmonitor-preview@tecwise.com.br',
  scopes: ['TENANT_ADMIN'],
  userId: USER_ID,
  firstName: 'Preview',
  lastName: 'TWmonitor',
  enabled: true,
  isPublic: false,
  tenantId: TENANT_ID,
  customerId: NULL_UUID,
  iat: nowSec,
  exp: expSec
};
const jwt = `${b64url(header)}.${b64url(payload)}.fakesignature`;
const expMs = String(expSec * 1000);

const userObj = {
  id: { id: USER_ID, entityType: 'USER' },
  createdTime: Date.now(),
  name: 'preview@tecwise.com.br',
  tenantId: { id: TENANT_ID, entityType: 'TENANT' },
  customerId: { id: NULL_UUID, entityType: 'CUSTOMER' },
  email: 'preview@tecwise.com.br',
  phone: '',
  authority: 'TENANT_ADMIN',
  firstName: 'Preview',
  lastName: 'TWmonitor',
  additionalInfo: {
    userCredentialsEnabled: true,
    userActivated: true,
    description: '',
    defaultDashboardId: null,
    defaultDashboardFullscreen: false,
    homeDashboardId: null,
    homeDashboardHideToolbar: false,
    unitSystem: 'METRIC',
    lang: 'pt_BR'
  }
};

const sysParams = {
  userTokenAccessEnabled: false,
  allowedDashboardIds: [],
  edgesSupportEnabled: false,
  hasRepository: false,
  tbelEnabled: true,
  persistDeviceStateToTelemetry: false,
  mobileQrEnabled: false,
  userSettings: { openedMenuSections: [], notDisplayed: [] },
  maxResourceSize: 52428800,
  maxDebugModeDurationMinutes: 15,
  maxDataPointsPerRollingArg: 100,
  maxArgumentsPerCF: 10,
  minAllowedDeduplicationIntervalInSecForCF: 1,
  minAllowedAggregationIntervalInSecForCF: 1,
  minAllowedScheduledUpdateIntervalInSecForCF: 60,
  maxRelationLevelPerCfArgument: 3,
  maxRelatedEntitiesToReturnPerCfArgument: 100,
  intermediateAggregationIntervalInSecForCF: 60,
  trendzSettings: {},
  allowKeyFiltersOrConditions: true,
  nullsOrderStrategy: 'NONE',
  edqsEnabled: false,
  iotHubBaseUrl: '',
  maxDatapointsLimit: 50000
};

function send(res, status, body) {
  res.writeHead(status, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*'
  });
  res.end(JSON.stringify(body));
}

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];
  console.log(req.method, url);

  if (url === `/api/user/${USER_ID}` && req.method === 'GET') return send(res, 200, userObj);
  if (url === '/api/system/params' && req.method === 'GET') return send(res, 200, sysParams);
  if (url === '/api/auth/user' && req.method === 'GET') return send(res, 200, userObj);
  if (url.startsWith('/api/notification')) {
    return send(res, 200, { data: [], totalPages: 0, totalElements: 0, hasNext: false, totalUnread: 0 });
  }
  // Fallback permissivo: qualquer outra chamada recebe 200 {} em vez de erro de rede,
  // para nao travar o carregamento da tela (widgets/dados reais nao vao funcionar).
  return send(res, 200, {});
});

server.listen(8080, '0.0.0.0', () => {
  const snippet = `localStorage.setItem('jwt_token', '${jwt}'); localStorage.setItem('jwt_token_expiration', '${expMs}'); localStorage.removeItem('refresh_token'); localStorage.removeItem('refresh_token_expiration'); location.href = '/home';`;
  console.log('Mock backend rodando em http://localhost:8080 (proxy.conf.js do ng serve ja aponta pra ca)');
  console.log('');
  console.log('Cole isto no Console do navegador (F12) em http://localhost:4200 :');
  console.log('');
  console.log(snippet);
});
